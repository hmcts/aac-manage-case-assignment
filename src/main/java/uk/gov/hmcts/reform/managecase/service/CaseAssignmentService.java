package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.RequestedCaseUnassignment;
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
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
public class CaseAssignmentService {
    private static final Logger LOG = LoggerFactory.getLogger(CaseAssignmentService.class);

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final IdamRepository idamRepository;
    private final JacksonUtils jacksonUtils;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseAssignmentService(PrdRepository prdRepository,
                                 @Qualifier("defaultDataStoreRepository") DataStoreRepository dataStoreRepository,
                                 IdamRepository idamRepository,
                                 JacksonUtils jacksonUtils,
                                 SecurityUtils securityUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.idamRepository = idamRepository;
        this.jacksonUtils = jacksonUtils;
        this.securityUtils = securityUtils;
    }

    @SuppressWarnings("PMD")
    public List<String> assignCaseAccess(CaseAssignment assignment, boolean useUserToken) {
        CaseDetails caseDetails = getCaseDetails(assignment, useUserToken);

        FindUsersByOrganisationResponse usersByOrg = prdRepository.findUsersByOrganisation();
        if (!isAssigneePresent(usersByOrg.getUsers(), assignment.getAssigneeId())) {
            throw new ValidationException(ValidationError.ASSIGNEE_ORGANISATION_ERROR);
        }

        List<String> assigneeRoles = getAssigneeRoles(assignment.getAssigneeId());

        if (!securityUtils.hasSolicitorAndJurisdictionRoles(assigneeRoles, caseDetails.getJurisdiction())) {
            throw new ValidationException(ValidationError.ASSIGNEE_ROLE_ERROR);
        }

        List<String> policyRoles = findInvokerOrgPolicyRoles(caseDetails, usersByOrg.getOrganisationIdentifier());
        if (policyRoles.isEmpty()) {
            throw new ValidationException(ValidationError.ORGANISATION_POLICY_ERROR);
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

    public void unassignCaseAccess(List<RequestedCaseUnassignment> unassignments) {
        FindUsersByOrganisationResponse usersByOrg = getUsersByOrganisation(unassignments);

        final List<CaseUserRole> missingRoleInformation = getMissingRoleInformation(unassignments);

        List<CaseUserRole> caseUserRolesToUnassign = expandUnassignmentsList(unassignments, missingRoleInformation);

        // NB: no processing required if empty (i.e. no roles found to delete)
        if (!caseUserRolesToUnassign.isEmpty()) {
            dataStoreRepository.removeCaseUserRoles(caseUserRolesToUnassign, usersByOrg.getOrganisationIdentifier());
        }
    }

    private List<CaseUserRole> getMissingRoleInformation(List<RequestedCaseUnassignment> unassignments) {
        // get list of all CaseId-Assignees that are missing list of roles
        List<RequestedCaseUnassignment> unassignmentsMissingRoles = unassignments.stream()
            // filter out those with case roles specified
            .filter(unassignment -> !unassignment.hasCaseRoles())
            .collect(toList());

        // load missing role information up front in single data store call
        final List<CaseUserRole> missingRoleInformation = new ArrayList<>();
        if (!unassignmentsMissingRoles.isEmpty()) {
            List<String> caseIds =  unassignmentsMissingRoles.stream()
                .map(RequestedCaseUnassignment::getCaseId)
                .distinct()
                .collect(toList());
            List<String> userIds =  unassignmentsMissingRoles.stream()
                .map(RequestedCaseUnassignment::getAssigneeId)
                .distinct()
                .collect(toList());
            missingRoleInformation.addAll(dataStoreRepository.getCaseAssignments(caseIds, userIds));
        }
        return missingRoleInformation;
    }

    private FindUsersByOrganisationResponse getUsersByOrganisation(List<RequestedCaseUnassignment> unassignments) {
        // load all users up front ...
        FindUsersByOrganisationResponse usersByOrg = prdRepository.findUsersByOrganisation();
        // ... and validate that all assignees can found in user list
        unassignments.stream()
            .map(RequestedCaseUnassignment::getAssigneeId)
            .distinct()
            .forEach(assignee -> {
                if (!isAssigneePresent(usersByOrg.getUsers(), assignee)) {
                    throw new ValidationException(ValidationError.UNASSIGNEE_ORGANISATION_ERROR);
                }
            });

        return usersByOrg;
    }

    private List<CaseUserRole> expandUnassignmentsList(List<RequestedCaseUnassignment> unassignments,
                                                       List<CaseUserRole> missingRoleInformation) {

        return unassignments.stream()
            .map(unassignment -> {
                // if roles defined :: expand into list of CaseUserRole objects
                if (unassignment.hasCaseRoles()) {
                    return unassignment.getCaseRoles().stream()
                        .map(role -> new CaseUserRole(unassignment.getCaseId(), unassignment.getAssigneeId(), role))
                        .collect(toList());
                    // otherwise :: expand using missingRoleInformation
                } else {
                    return missingRoleInformation.stream()
                        .filter(caseUserRole -> caseUserRole.getCaseId().equals(unassignment.getCaseId())
                            && caseUserRole.getUserId().equals(unassignment.getAssigneeId()))
                        .collect(toList());
                }
            })
            .flatMap(List::stream)
            .distinct()
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

    private CaseDetails getCaseDetails(CaseAssignment assignment, boolean useUserToken) {
        if (useUserToken) {
            LOG.debug("GET CaseDetails called using user token");
            return dataStoreRepository.findCaseByCaseIdUsingExternalApi(assignment.getCaseId());
        } else {
            LOG.debug("GET CaseDetails called using system user");
            return dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(assignment.getCaseId());
        }
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

    public List<String> getAssigneeRoles(String assigneeId) {
        String systemUserToken = idamRepository.getCaaSystemUserAccessToken();
        return idamRepository.getUserByUserId(assigneeId, systemUserToken).getRoles();
    }
}
