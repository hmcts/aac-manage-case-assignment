provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location

  tags = var.common_tags
}

module "key-vault" {
  source              = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product             = var.product
  env                 = var.env
  tenant_id           = var.tenant_id
  object_id           = var.jenkins_AAD_objectId
  resource_group_name = azurerm_resource_group.rg.name

  # dcd_cc-dev group object ID
  product_group_object_id              = "38f9dea6-e861-4a50-9e73-21e64f563537"
  common_tags                          = var.common_tags
  create_managed_identity              = true
  additional_managed_identities_access = var.additional_managed_identities_access
}

resource "azurerm_key_vault_secret" "connection_string" {
  name         = "app-insights-connection-string"
  value        = azurerm_application_insights.appinsights.connection_string
  key_vault_id = module.vault.key_vault_id
}

module "application_insights" {
  source = "git@github.com:hmcts/terraform-module-application-insights?ref=4.x"

  env      = var.env
  product  = var.product
  location = var.location

  resource_group_name = azurerm_resource_group.rg.name

  common_tags = var.common_tags
}

moved {
  from = azurerm_application_insights.appinsights
  to   = module.application_insights.azurerm_application_insights.this
}

data "azurerm_key_vault" "s2s_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}
data "azurerm_key_vault_secret" "manage-case-s2s-vault-secret" {
  name         = "microservicekey-aac-manage-case-assignment"
  key_vault_id = data.azurerm_key_vault.s2s_vault.id
}

resource "azurerm_key_vault_secret" "aac-manage-case-s2s-secret" {
  name         = "aac-manage-case-s2s-secret"
  value        = data.azurerm_key_vault_secret.manage-case-s2s-vault-secret.value
  key_vault_id = module.key-vault.key_vault_id
}

