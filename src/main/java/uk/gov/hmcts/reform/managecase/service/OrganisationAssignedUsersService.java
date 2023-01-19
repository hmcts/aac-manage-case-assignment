package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ResourceNotFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ServiceException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdates;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
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
public class OrganisationAssignedUsersService {
    private static final Logger LOG = LoggerFactory.getLogger(OrganisationAssignedUsersService.class);

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;
    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;

    private final DataStoreApiClient dataStoreApi;

    @Autowired
    public OrganisationAssignedUsersService(PrdRepository prdRepository,
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

    public Map<String, Long> calculateOrganisationAssignedUsersCountOnCase(String caseId) {
        CaseDetails caseDetails = dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(caseId);
        List<OrganisationPolicy> policies = findPolicies(caseDetails);

        List<String> failedOrgIds = new ArrayList<>();

        Set<String> orgIds = policies.stream()
            .filter(policy -> policy.getOrganisation() != null)
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
            } catch (ResourceNotFoundException e) {
                failedOrgIds.add(orgId);
            }
        });

        // find all active assigned roles for case
        List<CaseAssignedUserRole> allCauRoles
            = roleAssignmentService.findRoleAssignmentsByCasesAndUsers(List.of(caseId), new ArrayList<>(userIds));

        // generate counts
        Map<String, Long> orgUserCounts = new HashMap<>();
        orgIds.forEach(orgId -> {
            if (failedOrgIds.contains(orgId)) {
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
                LOG.debug("resetOrgUserCountOnCase.orgCounts: {}: {}", orgId, userCount);
            }
        });

        return orgUserCounts;
    }

    public void saveOrganisationUserCount(String caseId, Map<String, Long> orgUserCounts) {
        SupplementaryDataUpdates updateRequest = new SupplementaryDataUpdates();

        Map<String, Object> formattedOrgToCountMap = new HashMap<>();
        for (Map.Entry<String, Long> orgUserCount : orgUserCounts.entrySet()) {

            // NB: skip zero count records
            if (orgUserCount.getValue() > 0) {
                formattedOrgToCountMap.put(
                    ORGS_ASSIGNED_USERS_PATH + orgUserCount.getKey(),
                    orgUserCount.getValue()
                );
            }
        }

        // NB: skip save if nothing to save
        if (!formattedOrgToCountMap.isEmpty()) {
            updateRequest.setSetMap(formattedOrgToCountMap);
            SupplementaryDataUpdateRequest request = new SupplementaryDataUpdateRequest();
            request.setSupplementaryDataUpdates(updateRequest);
            dataStoreApi.updateCaseSupplementaryData(
                securityUtils.getCaaSystemUserToken(),
                caseId,
                request
            );
        }
    }

    private Set<String> findUsersByOrganisation(String orgId) {
        try {
            return prdRepository.findUsersByOrganisation(orgId).getUsers().stream()
                .map(ProfessionalUser::getUserIdentifier)
                .collect(Collectors.toSet());

        } catch (FeignException e) {
            HttpStatus status = HttpStatus.resolve(e.status());
            String reasonPhrase = status == null ? e.getMessage() : status.getReasonPhrase();

            if (status == HttpStatus.NOT_FOUND) {
                String errorMessage = String.format("Organisation with ID '%s' can not be found.", orgId);
                throw new ResourceNotFoundException(errorMessage, e);
            } else {
                String errorMessage = String.format(
                    "Error encountered while retrieving organisation users for organisation ID '%s': %s",
                    orgId,
                    reasonPhrase
                );
                throw new ServiceException(errorMessage, e);
            }
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

    private boolean checkIfUserHasAnyGivenRole(List<CaseAssignedUserRole> cauRoles, String userId, List<String> roles) {
        return cauRoles.stream()
            .anyMatch(cauRole ->
                          userId.equalsIgnoreCase(cauRole.getUserId())
                              && roles.stream().anyMatch(role -> role.equalsIgnoreCase(cauRole.getCaseRole())));
    }

}
