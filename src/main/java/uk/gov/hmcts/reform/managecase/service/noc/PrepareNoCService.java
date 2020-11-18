package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_TYPE_CANNOT_BE_BLANK;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.JURISDICTION_CANNOT_BE_BLANK;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.MISSING_COR_CASE_ROLE_ON_THE_CASE_DATA;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.MISSING_COR_ON_THE_CASE_DATA;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORGANISATION_ID_IN_ANY_ORG_POLICY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORGANISATION_POLICY_ON_CASE_DATA;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ONGOING_NOC_REQUEST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY;

@Service
public class PrepareNoCService {
    protected static final String COR_FIELD_NAME = "ChangeOrganisationRequest";
    protected static final String COR_CASE_ROLE_ID = "CaseRoleId";
    protected static final String COR_REQUEST_TIMESTAMP = "RequestTimestamp";
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private final PrdRepository prdRepository;
    private final SecurityUtils securityUtils;
    private final JacksonUtils jacksonUtils;
    private final DefinitionStoreRepository definitionStoreRepository;

    @Autowired
    public PrepareNoCService(PrdRepository prdRepository,
                             SecurityUtils securityUtils,
                             JacksonUtils jacksonUtils,
                             DefinitionStoreRepository definitionStoreRepository) {
        this.prdRepository = prdRepository;
        this.securityUtils = securityUtils;
        this.jacksonUtils = jacksonUtils;
        this.definitionStoreRepository = definitionStoreRepository;
    }

    public Map<String, JsonNode> prepareNoCRequest(CaseDetails caseDetails) {

        String jurisdiction = caseDetails.getJurisdiction();
        String caseType = caseDetails.getCaseTypeId();

        validate(isBlank(jurisdiction), JURISDICTION_CANNOT_BE_BLANK);
        validate(isBlank(caseType), CASE_TYPE_CANNOT_BE_BLANK);

        List<OrganisationPolicy> orgPolicies = findPolicies(caseDetails);
        validate(orgPolicies.isEmpty(), NO_ORGANISATION_POLICY_ON_CASE_DATA);

        Map<String, JsonNode> data = caseDetails.getData();
        // TODO: Fetch the definition to find the ChangeOrganisationRequest field name
        // TODO: or if https://tools.hmcts.net/jira/browse/RDM-10262 delivered use the COR_FIELD_NAME.
        validateChangeOrganisationRequest(data, COR_FIELD_NAME);

        // check that there isn't an ongoing NoCRequest - if so this new NoCRequest must be rejected
        String caseRoleId = getCaseRoleId(data, COR_FIELD_NAME);
        validate(isNotBlank(caseRoleId), ONGOING_NOC_REQUEST);

        List<String> caseRoles;

        if (isInvokingUserSolicitor(jurisdiction)) {
            // Prepare the list of CaseRoles on the case for which the user might wish to choose to cease representation
            String organisationIdentifier = findTheOrganisationIdOfTheInvokerUsingPrd();
            caseRoles = findInvokerOrgPolicyRoles(orgPolicies, organisationIdentifier);
            validate(
                caseRoles.isEmpty(),
                NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY
            );
        } else {
            // Otherwise (should be a caseworker) prepare a list of CaseRoles corresponding to those
            // in the OrgansiationPolicy with a non-null Organisation Id
            caseRoles = findOrgPolicyRolesWithNonNullOrganisationId(orgPolicies);
            validate(
                caseRoles.isEmpty(),
                NO_ORGANISATION_ID_IN_ANY_ORG_POLICY
            );
        }

        List<CaseRole> caseRolesDefinition = getCaseRolesDefinitions(jurisdiction, caseType, caseRoles);

        updateChangeOrganisationRequestCaseRoleId(data, caseRolesDefinition);
        updateChangeOrganisationRequestRequestTimestamp(data);

        return data;
    }

    private List<CaseRole> getCaseRolesDefinitions(String jurisdiction, String caseType, List<String> caseRoles) {
        List<CaseRole> caseRolesDefinition = definitionStoreRepository.caseRoles("0", jurisdiction, caseType);
        List<String> caseRolesDefinitionIds = caseRolesDefinition.stream()
            .map(CaseRole::getId).collect(toList());

        caseRoles.forEach(cr -> {
            validate(
                !caseRolesDefinitionIds.contains(cr),
                format(ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION, cr)
            );
        });

        return caseRolesDefinition.stream()
            .filter(cr -> caseRoles.contains(cr.getId()))
            .collect(Collectors.toUnmodifiableList());
    }

    private void updateChangeOrganisationRequestCaseRoleId(Map<String, JsonNode> data,
                                                          List<CaseRole> caseRolesDefinition) {
        CaseRole firstCaseRole = caseRolesDefinition.get(0);
        ObjectNode root = JSON_NODE_FACTORY.objectNode();
        root.putObject("value").setAll(createObjectNode(firstCaseRole.getId(), firstCaseRole.getName()));
        root.putArray("list_items")
            .addAll(caseRolesDefinition.stream()
                        .map(caseRole -> createObjectNode(caseRole.getId(), caseRole.getName()))
                        .collect(toList()));

        JsonNode cor = data.get(COR_FIELD_NAME);
        ((ObjectNode) cor).set(COR_CASE_ROLE_ID, root);
    }

    /**
     * Ex.:
     * {
     * "code": "[Claimant]",
     * "label": "Claimant"
     * }
     */
    private ObjectNode createObjectNode(String code, String label) {
        return JSON_NODE_FACTORY.objectNode().put("code", code).put("label", label);
    }

    private void updateChangeOrganisationRequestRequestTimestamp(Map<String, JsonNode> data) {
        JsonNode cor = data.get(COR_FIELD_NAME);
        ((ObjectNode) cor).set(COR_REQUEST_TIMESTAMP, TextNode.valueOf(LocalDateTime.now().toString()));
    }

    private String findTheOrganisationIdOfTheInvokerUsingPrd() {
        FindUsersByOrganisationResponse usersByOrganisation = prdRepository.findUsersByOrganisation();
        return usersByOrganisation.getOrganisationIdentifier();
    }

    private void validateChangeOrganisationRequest(Map<String, JsonNode> data, String corFieldName) {
        validate(
            !data.containsKey(corFieldName),
            format(MISSING_COR_ON_THE_CASE_DATA, corFieldName)
        );
        if (!(data.get(corFieldName).getNodeType() == JsonNodeType.OBJECT)
            || data.get(corFieldName).findPath(COR_CASE_ROLE_ID).isMissingNode()) {
            throw new ValidationException(format(
                MISSING_COR_CASE_ROLE_ON_THE_CASE_DATA,
                corFieldName,
                COR_CASE_ROLE_ID
            ));
        }
    }

    private String getCaseRoleId(Map<String, JsonNode> data, String corFieldName) {
        return data.get(corFieldName).findPath(COR_CASE_ROLE_ID).textValue();
    }

    private List<String> findInvokerOrgPolicyRoles(List<OrganisationPolicy> policies, String organisationId) {
        return policies.stream()
            .filter(policy -> policy.getOrganisation() != null
                && organisationId.equalsIgnoreCase(policy.getOrganisation().getOrganisationID()))
            .map(OrganisationPolicy::getOrgPolicyCaseAssignedRole)
            .map(String::trim)
            .collect(toList());
    }

    private List<String> findOrgPolicyRolesWithNonNullOrganisationId(List<OrganisationPolicy> policies) {
        return policies.stream()
            .filter(policy -> policy.getOrganisation() != null && policy.getOrganisation().getOrganisationID() != null)
            .map(OrganisationPolicy::getOrgPolicyCaseAssignedRole)
            .map(String::trim)
            .collect(toList());
    }

    private List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }

    private boolean isInvokingUserSolicitor(String jurisdiction) {
        UserInfo userInfo = securityUtils.getUserInfo();
        List<String> roles = userInfo.getRoles();

        return securityUtils.hasSolicitorRole(roles, jurisdiction);
    }

    private void validate(boolean condition, String errorMessage) {
        if (condition) {
            throw new ValidationException(errorMessage);
        }
    }
}
