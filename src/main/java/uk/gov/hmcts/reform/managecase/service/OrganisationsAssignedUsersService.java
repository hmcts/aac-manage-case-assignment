package uk.gov.hmcts.reform.managecase.service;

import feign.FeignException;
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
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.OrganisationsAssignedUsersCountData;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;
import uk.gov.hmcts.reform.managecase.util.OrganisationPolicyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ORGS_ASSIGNED_USERS_PATH;

@Service
public class OrganisationsAssignedUsersService {
    private static final Logger LOG = LoggerFactory.getLogger(OrganisationsAssignedUsersService.class);

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final OrganisationPolicyUtils organisationPolicyUtils;
    private final RoleAssignmentService roleAssignmentService;
    private final SecurityUtils securityUtils;
    private final DataStoreApiClient dataStoreApi;

    @Autowired
    public OrganisationsAssignedUsersService(PrdRepository prdRepository,
                                             @Qualifier("defaultDataStoreRepository")
                                             DataStoreRepository dataStoreRepository,
                                             OrganisationPolicyUtils organisationPolicyUtils,
                                             RoleAssignmentService roleAssignmentService,
                                             SecurityUtils securityUtils,
                                             DataStoreApiClient dataStoreApi) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.organisationPolicyUtils = organisationPolicyUtils;
        this.roleAssignmentService = roleAssignmentService;
        this.securityUtils = securityUtils;
        this.dataStoreApi = dataStoreApi;
    }

    public OrganisationsAssignedUsersCountData calculateOrganisationsAssignedUsersCountData(String caseId) {
        CaseDetails caseDetails = dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(caseId);
        List<OrganisationPolicy> policies = organisationPolicyUtils.findPolicies(caseDetails);

        Map<String, String> skippedOrgs = new HashMap<>();

        Set<String> orgIds = policies.stream()
            .filter(organisationPolicyUtils::checkIfPolicyHasOrganisationAssigned)
            .map(policy -> policy.getOrganisation().getOrganisationID())
            .collect(Collectors.toSet());

        Map<String, Set<String>> userIdsByOrg = new HashMap<>();
        orgIds.forEach(orgId -> {
            try {
                userIdsByOrg.put(orgId, findUsersByOrganisation(orgId));
            } catch (FeignException ex) {
                skippedOrgs.put(orgId, formatSkipMessageFromException(orgId, ex));
            }
        });

        return OrganisationsAssignedUsersCountData.builder()
            .caseId(caseId)
            .orgsAssignedUsers(generateOrgsAssignedUsersMap(caseId, policies, userIdsByOrg))
            .skippedOrgs(skippedOrgs)
            .build();
    }

    public void saveCountData(OrganisationsAssignedUsersCountData countData) {

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

            SupplementaryDataUpdates updateRequest = new SupplementaryDataUpdates();
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

    private boolean checkIfUserHasAnyGivenRole(List<CaseAssignedUserRole> cauRoles, String userId, List<String> roles) {
        return cauRoles.stream()
            .anyMatch(cauRole ->
                          userId.equalsIgnoreCase(cauRole.getUserId())
                              && roles.stream().anyMatch(role -> role.equalsIgnoreCase(cauRole.getCaseRole())));
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

    private Map<String, Long> generateOrgsAssignedUsersMap(String caseId,
                                                           List<OrganisationPolicy> policies,
                                                           Map<String, Set<String>> userIdsByOrg) {
        // find all users we are interested in
        Set<String> userIds = userIdsByOrg.entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toSet());
        Set<String> orgIds = userIdsByOrg.keySet();

        // NB: skip remaining process if no organisations or users found
        if (orgIds.isEmpty() || userIds.isEmpty()) {
            // just return zeros for each org (if any) as no users found for any
            return orgIds.stream()
                .collect(Collectors.toMap(orgId -> orgId, orgId -> 0L));

        } else {
            // find all active assigned roles for case
            List<CaseAssignedUserRole> allCauRoles
                = roleAssignmentService.findRoleAssignmentsByCasesAndUsers(List.of(caseId), new ArrayList<>(userIds));

            Map<String, Long> orgsAssignedUsers = new HashMap<>();

            // generate counts
            orgIds.forEach(orgId -> {
                Set<String> usersForOrg = userIdsByOrg.getOrDefault(orgId, Collections.emptySet());
                List<String> rolesForOrg = organisationPolicyUtils.findRolesForOrg(policies, orgId);
                long userCount = usersForOrg.stream()
                    .filter(userId -> checkIfUserHasAnyGivenRole(allCauRoles, userId, rolesForOrg))
                    .count();
                orgsAssignedUsers.put(orgId, userCount);
            });

            return orgsAssignedUsers;
        }
    }

}
