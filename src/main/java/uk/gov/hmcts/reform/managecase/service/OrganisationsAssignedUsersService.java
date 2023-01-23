package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdates;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.OrganisationsAssignedUsersCountData;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ORGS_ASSIGNED_USERS_PATH;

@Service
public class OrganisationsAssignedUsersService {
    private static final Logger LOG = LoggerFactory.getLogger(OrganisationsAssignedUsersService.class);

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;
    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;

    private final DataStoreApiClient dataStoreApi;

    @Autowired
    public OrganisationsAssignedUsersService(PrdRepository prdRepository,
                                             @Qualifier("defaultDataStoreRepository")
                                             DataStoreRepository dataStoreRepository,
                                             JacksonUtils jacksonUtils,
                                             RoleAssignmentService roleAssignmentService,
                                             SecurityUtils securityUtils,
                                             DataStoreApiClient dataStoreApi) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.jacksonUtils = jacksonUtils;
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;

        this.dataStoreApi = dataStoreApi;
    }

    public OrganisationsAssignedUsersCountData calculateOrganisationsAssignedUsersCountData(String caseId) {
        CaseDetails caseDetails = dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(caseId);
        List<OrganisationPolicy> policies = findPolicies(caseDetails);

        Map<String, String> failedOrgs = new HashMap<>();

        Set<String> orgIds = policies.stream()
            .filter(this::checkIfPolicyHasOrganisationAssigned)
            .map(policy -> policy.getOrganisation().getOrganisationID())
            .collect(Collectors.toSet());

        // find all users we are interested in
        Set<String> userIds = new HashSet<>();
        Map<String, Set<String>> userIdsByOrg = new HashMap<>();
        orgIds.forEach(orgId -> {
            try {
                Set<String> usersForOrg = findUsersByOrganisation(orgId);

                userIds.addAll(usersForOrg);
                userIdsByOrg.put(orgId, usersForOrg);
            } catch (FeignException ex) {
                failedOrgs.put(orgId, formatSkipMessageFromException(orgId, ex));
            }
        });

        // find all active assigned roles for case
        List<CaseAssignedUserRole> allCauRoles
            = roleAssignmentService.findRoleAssignmentsByCasesAndUsers(List.of(caseId), new ArrayList<>(userIds));

        // generate counts
        Map<String, Long> orgUserCounts = new HashMap<>();
        orgIds.forEach(orgId -> {
            if (failedOrgs.containsKey(orgId)) {
                LOG.warn("Skipping OrgID: {} for Case: {}", orgId, caseId);
            } else {
                Set<String> usersForOrg = userIdsByOrg.getOrDefault(orgId, Collections.emptySet());
                List<String> rolesForOrg = findRolesForOrg(policies, orgId);
                long userCount = 0;
                if (!usersForOrg.isEmpty() && !rolesForOrg.isEmpty()) {
                    userCount = usersForOrg.stream()
                        .filter(userId -> checkIfUserHasAnyGivenRole(allCauRoles, userId, rolesForOrg))
                        .count();
                }
                orgUserCounts.put(orgId, userCount);
            }
        });

        return OrganisationsAssignedUsersCountData.builder()
            .caseId(caseId)
            .orgsAssignedUsers(orgUserCounts)
            .skippedOrgs(failedOrgs)
            .build();
    }

    public void saveOrganisationUserCount(OrganisationsAssignedUsersCountData countData) {
        SupplementaryDataUpdates updateRequest = new SupplementaryDataUpdates();

        Map<String, Object> formattedOrgToCountMap = new HashMap<>();
        for (Map.Entry<String, Long> orgsAssignedUsers : countData.getOrgsAssignedUsers().entrySet()) {

            // NB: skip zero count records
            if (orgsAssignedUsers.getValue() > 0) {
                formattedOrgToCountMap.put(
                    ORGS_ASSIGNED_USERS_PATH + orgsAssignedUsers.getKey(),
                    orgsAssignedUsers.getValue()
                );
            }
        }

        // NB: skip save if nothing to save
        if (!formattedOrgToCountMap.isEmpty()) {

            LOG.info("Saving `orgs_assigned_users` data for case : {} : {} ",
                     countData.getCaseId(), formattedOrgToCountMap);

            updateRequest.setSetMap(formattedOrgToCountMap);
            SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();
            request.setSupplementaryDataUpdates(updateRequest);
            dataStoreApi.updateCaseSupplementaryData(
                securityUtils.getCaaSystemUserToken(),
                countData.getCaseId(),
                request
            );
        }
    }

    private Set<String> findUsersByOrganisation(String orgId) {
        return prdRepository.findUsersByOrganisation(orgId).getUsers().stream()
            .map(ProfessionalUser::getUserIdentifier)
            .collect(Collectors.toSet());
    }

    private String formatSkipMessageFromException(String orgId, FeignException ex) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        String reasonPhrase = status == null ? ex.getMessage() : status.getReasonPhrase();

        if (status == HttpStatus.NOT_FOUND) {
            String warningMessage = String.format("Organisation not found: ID '%s': %s", orgId, reasonPhrase);
            LOG.warn(warningMessage);
            return warningMessage;
        } else {
            String errorMessage = String.format(
                "Error encountered while retrieving organisation users for organisation ID '%s': %s",
                orgId,
                reasonPhrase
            );
            LOG.error(errorMessage, ex);
            return errorMessage;
        }
    }

    private List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }

    private List<String> findRolesForOrg(List<OrganisationPolicy> policies, String orgId) {
        return policies.stream()
            .filter(policy -> policy.getOrganisation() != null && orgId.equalsIgnoreCase(policy
                                                                                             .getOrganisation()
                                                                                             .getOrganisationID()))
            .map(OrganisationPolicy::getOrgPolicyCaseAssignedRole)
            .collect(toList());
    }

    private boolean checkIfPolicyHasOrganisationAssigned(OrganisationPolicy policy) {
        return policy != null
            && policy.getOrganisation() != null
            && StringUtils.isNotEmpty(policy.getOrganisation().getOrganisationID());
    }

    private boolean checkIfUserHasAnyGivenRole(List<CaseAssignedUserRole> cauRoles, String userId, List<String> roles) {
        return cauRoles.stream()
            .anyMatch(cauRole ->
                          userId.equalsIgnoreCase(cauRole.getUserId())
                              && roles.stream().anyMatch(role -> role.equalsIgnoreCase(cauRole.getCaseRole())));
    }

}
