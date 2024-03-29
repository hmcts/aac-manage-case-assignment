package uk.gov.hmcts.reform.managecase.service.cau;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsAddRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseAccessOperation {

    public static final String CREATOR = "[CREATOR]";
    public static final String ORGS_ASSIGNED_USERS_PATH = "orgs_assigned_users.";

    private final RoleAssignmentService roleAssignmentService;
    private final DataStoreRepository dataStoreRepository;
    protected final SecurityUtils securityUtils;

    @Autowired
    public CaseAccessOperation(RoleAssignmentService roleAssignmentService, @Qualifier("defaultDataStoreRepository")
        DataStoreRepository dataStoreRepository,
                               SecurityUtils securityUtils) {
        this.roleAssignmentService = roleAssignmentService;
        this.dataStoreRepository = dataStoreRepository;
        this.securityUtils = securityUtils;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseReferences, List<String> userIds) {
        final var caseIds = caseReferences.stream().map(String::valueOf).collect(Collectors.toList());
        return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);
    }

    private List<CaseAssignedUserRole> findCaseUserRoles(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        List<CaseDetails> caseDetailsList = new ArrayList<>(cauRolesByCaseDetails.keySet());
        List<String> userIds = getUserIdsFromMap(cauRolesByCaseDetails);

        final var caseIds = caseDetailsList.stream()
            .map(CaseDetails::getId).collect(Collectors.toList());
        return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

    }

    public void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails =
            getMapOfCaseAssignedUserRolesByCaseDetails(caseUserRoles);

        // Ignore case user role mappings that DO NOT exist
        // NB: also required so they don't effect revoked user counts.
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> filteredCauRolesByCaseDetails =
            findAndFilterOnExistingCauRoles(cauRolesByCaseDetails);

        List<RoleAssignmentsDeleteRequest> deleteRequests = new ArrayList<>();

        // for each case
        filteredCauRolesByCaseDetails.forEach((caseDetails, requestedAssignments) -> {
            // group by user
            Map<String, List<String>> caseRolesByUserAndCase = requestedAssignments.stream()
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRoleWithOrganisation::getUserId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        roles -> roles.stream()
                            .map(CaseAssignedUserRoleWithOrganisation::getCaseRole)
                            .collect(Collectors.toList())
                    )
                ));

            // for each user in current case: add list of all case-roles to revoke to the delete requests
            caseRolesByUserAndCase.forEach((userId, roleNames) ->
                                               deleteRequests.add(RoleAssignmentsDeleteRequest.builder()
                                                                      .caseId(caseDetails.getId())
                                                                      .userId(userId)
                                                                      .roleNames(roleNames)
                                                                      .build()
                                               ));
        });

        // submit list of all delete requests from all cases
        roleAssignmentService.deleteRoleAssignments(deleteRequests);


        // determine counters after removal of requested mappings so that same function can be re-used
        // (i.e user still has an association to a case).
        Map<String, Map<String, Long>> removeUserCounts
            = getNewUserCountByCaseAndOrganisation(filteredCauRolesByCaseDetails, null);

        if (!removeUserCounts.isEmpty()) {
            dataStoreRepository.incrementCaseSupplementaryData(removeUserCounts);
        }
    }

    public void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {

        final var cauRolesByCaseDetails = getMapOfCaseAssignedUserRolesByCaseDetails(caseUserRoles);
        // load all existing case user roles upfront
        final var existingCaseUserRoles = findCaseUserRoles(cauRolesByCaseDetails);
        final var newUserCounts = getNewUserCountByCaseAndOrganisation(cauRolesByCaseDetails, existingCaseUserRoles);

        cauRolesByCaseDetails.forEach((caseDetails, requestedAssignments) -> {
            final var caseRolesByUserIdAndCase = requestedAssignments.stream()
                // filter out existing case user roles
                .filter(caseUserRole ->
                            existingCaseUserRoles.stream()
                                .noneMatch(cauRole -> caseUserRole.getCaseDataId().equals(cauRole.getCaseDataId())
                                    && caseUserRole.getUserId().equals(cauRole.getUserId())
                                    && caseUserRole.getCaseRole().equalsIgnoreCase(cauRole.getCaseRole())))
                // group by UserID
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRole::getUserId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        caseUserRole -> caseUserRole.stream()
                            .map(CaseAssignedUserRole::getCaseRole)
                            .collect(Collectors.toSet())
                    )
                ));

            final List<RoleAssignmentsAddRequest> addRequests = new ArrayList<>();
            caseRolesByUserIdAndCase.forEach((userId, caseRoles) ->

                                                 addRequests.add(RoleAssignmentsAddRequest.builder()
                                                                     .caseDetails(caseDetails)
                                                                     .userId(userId)
                                                                     .roleNames(new ArrayList<>(caseRoles))
                                                                     .build()
                                                 ));
            roleAssignmentService.createCaseRoleAssignments(addRequests);

        });

        if (!newUserCounts.isEmpty()) {
            dataStoreRepository.incrementCaseSupplementaryData(newUserCounts);
        }
    }

    private Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> findAndFilterOnExistingCauRoles(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        // find existing Case-User relationships and group by case reference
        Map<String, List<CaseAssignedUserRole>> existingCaseUserRolesByCaseReference =
            findCaseUserRoles(cauRolesByCaseDetails).stream()
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRole::getCaseDataId,
                    Collectors.toList()
                ));

        return cauRolesByCaseDetails.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> filterOnExistingCauRoles(
                    entry.getValue(),
                    existingCaseUserRolesByCaseReference.getOrDefault(
                        entry.getKey().getId(),
                        new ArrayList<>()
                    )
                )
            ));
    }

    private List<CaseAssignedUserRoleWithOrganisation> filterOnExistingCauRoles(
        List<CaseAssignedUserRoleWithOrganisation> inputCauRoles,
        List<CaseAssignedUserRole> existingCauRoles) {
        return inputCauRoles.stream()
            .filter(cauRole -> existingCauRoles.stream()
                .anyMatch(entity -> entity.getCaseRole().equalsIgnoreCase(cauRole.getCaseRole())
                    && entity.getUserId().equalsIgnoreCase(cauRole.getUserId())
                    && entity.getCaseDataId().equalsIgnoreCase(cauRole.getCaseDataId())))
            .collect(Collectors.toList());
    }

    private List<String> getUserIdsFromMap(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        return cauRolesByCaseDetails.values().stream()
            .map(cauRoles -> cauRoles.stream()
                .map(CaseAssignedUserRole::getUserId)
                .collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private Map<CaseDetails,
        List<CaseAssignedUserRoleWithOrganisation>> getMapOfCaseAssignedUserRolesByCaseDetails(
        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles
    ) {

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseCaseDetails
            = new HashMap<>();

        List<Long> caseReferences = caseUserRoles.stream()
            .map(CaseAssignedUserRoleWithOrganisation::getCaseDataId)
            .distinct()
            .map(Long::parseLong)
            .collect(Collectors.toCollection(ArrayList::new));

        // create map of case references to case details
        Map<Long, CaseDetails> caseDetailsByReferences = getCaseDetailsList(caseReferences).stream()
            .collect(Collectors.toMap(CaseDetails::getReferenceAsLong, caseDetails -> caseDetails));

        // group roles by case reference
        Map<String, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseReference = caseUserRoles.stream()
            .collect(Collectors.groupingBy(CaseAssignedUserRoleWithOrganisation::getCaseDataId));

        // merge both maps to check we have found all cases
        cauRolesByCaseReference.forEach((key, roles) -> {
            final Long caseReference = Long.parseLong(key);
            if (caseDetailsByReferences.containsKey(caseReference)) {
                cauRolesByCaseCaseDetails.put(caseDetailsByReferences.get(caseReference), roles);
            } else {
                throw new CaseCouldNotBeFoundException("No case found for reference: " + key);
            }
        });

        return cauRolesByCaseCaseDetails;
    }

    private List<CaseDetails> getCaseDetailsList(List<Long> caseReferences) {
        return caseReferences.stream()
            .map(caseReference -> dataStoreRepository.findCaseByCaseIdUsingExternalApi(
                caseReference.toString()
            )).filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Map<String, Long>> getNewUserCountByCaseAndOrganisation(
        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails,
        List<CaseAssignedUserRole> existingCaseUserRoles
    ) {
        Map<String, Map<String, Long>> result = new HashMap<>();

        Map<CaseDetails, List<CaseAssignedUserRoleWithOrganisation>> caseUserRolesWhichHaveAnOrgId =
            cauRolesByCaseDetails.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                    // filter out no organisation_id and [CREATOR] case role
                    .filter(caseUserRole ->
                                StringUtils.isNoneBlank(caseUserRole.getOrganisationId())
                                    && !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR))
                    .collect(Collectors.toList())))
                // filter cases that have no remaining roles
                .entrySet().stream().filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // if empty list this processing is not required
        if (caseUserRolesWhichHaveAnOrgId.isEmpty()) {
            return result; // exit with empty map
        }

        // if not preloaded the existing/current case roles get a fresh snapshot now
        if (existingCaseUserRoles == null) {
            existingCaseUserRoles = findCaseUserRoles(caseUserRolesWhichHaveAnOrgId);
        }

        // find existing Case-User relationships for all the relevant cases + users found
        Map<Long, List<String>> existingCaseUserRelationships =
            existingCaseUserRoles.stream()
                // filter out [CREATOR] case role
                .filter(caseUserRole -> !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR))
                .collect(Collectors.groupingBy(
                    caseUserRole -> Long.parseLong(caseUserRole.getCaseDataId()),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        caseUserRole -> caseUserRole.stream()
                            .map(CaseAssignedUserRole::getUserId)
                            .distinct().collect(Collectors.toList())
                    )
                ));

        // for each case: count new Case-User relationships by Organisation
        caseUserRolesWhichHaveAnOrgId.forEach((caseDetails, requestedAssignments) -> {
            List<String> existingUsersForCase
                = existingCaseUserRelationships.getOrDefault(caseDetails.getReferenceAsLong(), new ArrayList<>());

            Map<String, Long> relationshipCounts = requestedAssignments.stream()
                // filter out any existing relationships
                .filter(cauRole -> !existingUsersForCase.contains(cauRole.getUserId()))
                // count unique users for each organisation
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRoleWithOrganisation::getOrganisationId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        cauRolesForOrganisation -> cauRolesForOrganisation.stream()
                            .map(CaseAssignedUserRoleWithOrganisation::getUserId).distinct().count()
                    )
                ));

            // skip if no organisations have any relationships
            if (!relationshipCounts.isEmpty()) {
                result.put(caseDetails.getId(), relationshipCounts);
            }
        });

        return result;
    }

}
