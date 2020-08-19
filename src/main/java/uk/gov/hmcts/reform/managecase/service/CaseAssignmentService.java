package uk.gov.hmcts.reform.managecase.service;

import static java.util.stream.Collectors.toList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFetchedException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.UserDetails;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

@Service
public class CaseAssignmentService {

    public static final String SOLICITOR_ROLE = "caseworker-%s-solicitor";

    public static final String CASE_COULD_NOT_BE_FETCHED = "Case could not be fetched";
    public static final String ASSIGNEE_ROLE_ERROR = "Intended assignee has to be a solicitor "
            + "enabled in the jurisdiction of the case.";
    public static final String ASSIGNEE_ORGA_ERROR = "Intended assignee has to be in the same organisation"
            + " as that of the invoker.";
    public static final String ORGA_POLICY_ERROR = "Case ID has to be one for which a case role is "
            + "represented by the invoker's organisation.";

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final IdamRepository idamRepository;
    private final JacksonUtils jacksonUtils;

    @Autowired
    public CaseAssignmentService(PrdRepository prdRepository,
                                 DataStoreRepository dataStoreRepository,
                                 IdamRepository idamRepository, JacksonUtils jacksonUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.idamRepository = idamRepository;
        this.jacksonUtils = jacksonUtils;
    }

    @SuppressWarnings("PMD")
    public List<String> assignCaseAccess(CaseAssignment assignment) {

        CaseDetails caseDetails = getCase(assignment);
        String solicitorRole = String.format(SOLICITOR_ROLE, caseDetails.getJurisdiction());

        FindUsersByOrganisationResponse usersByOrg = prdRepository.findUsersByOrganisation();
        if (!isAssigneePresent(usersByOrg.getUsers(), assignment.getAssigneeId())) {
            throw new ValidationException(ASSIGNEE_ORGA_ERROR);
        }

        List<String> assigneeRoles = getAssigneeRoles(assignment.getAssigneeId());
        if (!containsIgnoreCase(assigneeRoles, solicitorRole)) {
            throw new ValidationException(ASSIGNEE_ROLE_ERROR);
        }

        List<String> policyRoles = findInvokerOrgPolicyRoles(caseDetails, usersByOrg.getOrganisationIdentifier());
        if (policyRoles.isEmpty()) {
            throw new ValidationException(ORGA_POLICY_ERROR);
        }

        dataStoreRepository.assignCase(policyRoles, assignment.getCaseId(),
                                       assignment.getAssigneeId(), usersByOrg.getOrganisationIdentifier());
        return policyRoles;
    }

    @SuppressWarnings("PMD")
    public List<CaseAssignedUsers> getCaseAssignments(List<String> caseIds) {

        List<ProfessionalUser> professionalUsers = prdRepository.findUsersByOrganisation().getUsers();
        Map<String, ProfessionalUser> prdUsersMap = professionalUsers.stream()
                .collect(Collectors.toMap(ProfessionalUser::getUserIdentifier, user -> user));

        List<CaseUserRole> caseUserRoles =
                dataStoreRepository.getCaseAssignments(caseIds, new ArrayList<>(prdUsersMap.keySet()));
        Map<String, Map<String, List<String>>> rolesByUserIdAndByCaseId = caseUserRoles.stream()
                .collect(Collectors.groupingBy(CaseUserRole::getCaseId,
                        Collectors.groupingBy(CaseUserRole::getUserId,
                                Collectors.mapping(item -> item.getCaseRole(), toList()))));

        return rolesByUserIdAndByCaseId.entrySet().stream()
                .map(entry -> toCaseAssignedUsers(entry.getKey(), entry.getValue(), prdUsersMap))
                .collect(toList());
    }

    private CaseAssignedUsers toCaseAssignedUsers(String caseId, Map<String, List<String>> userRolesMap,
                                                  Map<String, ProfessionalUser> prdUsersMap) {
        return CaseAssignedUsers.builder()
                .caseId(caseId)
                .users(userRolesMap.entrySet().stream()
                        .map(entry -> toUserDetails(prdUsersMap.get(entry.getKey()), entry.getValue()))
                        .collect(toList()))
                .build();
    }

    private UserDetails toUserDetails(ProfessionalUser user, List<String> caseRoles) {
        return UserDetails.builder()
                .caseRoles(caseRoles)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .idamId(user.getUserIdentifier())
                .build();
    }

    private CaseDetails getCase(CaseAssignment input) {
        Optional<CaseDetails> caseOptional = dataStoreRepository.findCaseBy(input.getCaseTypeId(), input.getCaseId());
        return caseOptional.orElseThrow(() -> new CaseCouldNotBeFetchedException(CASE_COULD_NOT_BE_FETCHED));
    }

    private List<String> findInvokerOrgPolicyRoles(CaseDetails caseDetails, String organisation) {
        List<OrganisationPolicy> policies = findPolicies(caseDetails);
        return policies.stream()
                .filter(policy -> policy.getOrganisation() != null && organisation.equalsIgnoreCase(policy
                        .getOrganisation()
                        .getOrganisationID()))
                .map(policy -> policy.getOrgPolicyCaseAssignedRole())
                .collect(toList());
    }

    private List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }

    private boolean isAssigneePresent(List<ProfessionalUser> users, String assigneeId) {
        return users.stream().anyMatch(user -> assigneeId.equalsIgnoreCase(user.getUserIdentifier()));
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        return list.stream().anyMatch(value::equalsIgnoreCase);
    }

    private List<String> getAssigneeRoles(String assigneeId) {
        String systemUserToken = idamRepository.getSystemUserAccessToken();
        return idamRepository.searchUserById(assigneeId, systemUserToken).getRoles();
    }
}
