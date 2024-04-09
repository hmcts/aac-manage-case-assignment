package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import jakarta.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_TYPE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INVALID_CASE_ROLE_FIELD;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.JURISDICTION_CANNOT_BE_BLANK;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_ONGOING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORGANISATION_ID_IN_ANY_ORG_POLICY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORGANISATION_POLICY_ON_CASE_DATA;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_REQUEST_TIMESTAMP;

@Service
@SuppressWarnings({"PMD.UseLocaleWithCaseConversions"})
public class PrepareNoCService {

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
        String caseTypeId = caseDetails.getCaseTypeId();

        validate(isBlank(jurisdiction), JURISDICTION_CANNOT_BE_BLANK);
        validate(isBlank(caseTypeId), CASE_TYPE_ID_EMPTY);

        List<OrganisationPolicy> orgPolicies = findPolicies(caseDetails);
        validate(orgPolicies.isEmpty(), NO_ORGANISATION_POLICY_ON_CASE_DATA);

        String changeOfRequestFieldName = caseDetails.findChangeOrganisationRequestFieldName()
            .orElseThrow(() -> new ValidationException(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID));

        validate(caseDetails.getData().get(changeOfRequestFieldName).path(CASE_ROLE_ID).isMissingNode(),
                 INVALID_CASE_ROLE_FIELD);
        Map<String, JsonNode> data = caseDetails.getData();

        // check that there isn't an ongoing NoCRequest - if so this new NoCRequest must be rejected
        validate(hasCaseRoleId(data, changeOfRequestFieldName), NOC_REQUEST_ONGOING);

        List<String> caseRoles = prepareCaseRoles(jurisdiction, orgPolicies);
        List<CaseRole> caseRolesDefinition = getCaseRolesDefinitions(jurisdiction, caseTypeId, caseRoles);

        updateChangeOrganisationRequestCaseRoleId(data, caseRolesDefinition, changeOfRequestFieldName);
        updateChangeOrganisationRequestRequestTimestamp(data, changeOfRequestFieldName);

        return data;
    }

    private List<String> prepareCaseRoles(String jurisdiction, List<OrganisationPolicy> orgPolicies) {
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
        return caseRoles;
    }

    private List<CaseRole> getCaseRolesDefinitions(String jurisdiction, String caseType, List<String> caseRoles) {
        List<CaseRole> caseRolesDefinition = definitionStoreRepository.caseRoles("0", jurisdiction, caseType);
        Map<String, CaseRole> collect = caseRolesDefinition.stream()
            .collect(Collectors.toMap(caseRole -> caseRole.getId().toUpperCase(), Function.identity()));

        List<CaseRole> foundCaseRoles = new ArrayList<>();
        caseRoles.forEach(cr -> {
            validate(!collect.containsKey(cr.toUpperCase()), format(ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION, cr));

            CaseRole caseRole = collect.get(cr.toUpperCase());
            foundCaseRoles.add(CaseRole.builder()
                .id(cr)
                .name(caseRole.getName())
                .description(caseRole.getDescription())
                .build());
        });

        return foundCaseRoles;
    }

    private void updateChangeOrganisationRequestCaseRoleId(Map<String, JsonNode> data,
                                                           List<CaseRole> caseRolesDefinition,
                                                           String changeOfRequestFieldName) {
        List<DynamicListElement> dynamicListElements = caseRolesDefinition.stream()
            .map(caseRole -> DynamicListElement.builder().code(caseRole.getId()).label(caseRole.getName()).build())
            .collect(toList());
        ObjectNode dynamicList = jacksonUtils.createDynamicList(dynamicListElements.get(0), dynamicListElements);

        JsonNode cor = data.get(changeOfRequestFieldName);
        ((ObjectNode) cor).set(CASE_ROLE_ID, dynamicList);
    }

    private void updateChangeOrganisationRequestRequestTimestamp(Map<String, JsonNode> data,
                                                                 String changeOfRequestFieldName) {
        JsonNode cor = data.get(changeOfRequestFieldName);
        ((ObjectNode) cor).set(ORGANISATION_REQUEST_TIMESTAMP, TextNode.valueOf(LocalDateTime.now().toString()));
    }

    private String findTheOrganisationIdOfTheInvokerUsingPrd() {
        FindUsersByOrganisationResponse usersByOrganisation = prdRepository.findUsersByOrganisation();
        return usersByOrganisation.getOrganisationIdentifier();
    }

    private boolean hasCaseRoleId(Map<String, JsonNode> data, String corFieldName) {
        return data.get(corFieldName).findPath(CASE_ROLE_ID).isObject();
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

        return securityUtils.hasSolicitorAndJurisdictionRoles(roles, jurisdiction);
    }

    private void validate(boolean condition, String errorMessage) {
        if (condition) {
            throw new ValidationException(errorMessage);
        }
    }
}
