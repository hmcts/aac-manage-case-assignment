// Infrastructural variables
variable "product" {
  type = "string"
}

variable "component" {
  type = "string"
}

variable "env" {
  type = "string"
  description = "(Required) The environment in which to deploy the application infrastructure."
}
