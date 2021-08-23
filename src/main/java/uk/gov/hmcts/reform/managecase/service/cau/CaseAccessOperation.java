package uk.gov.hmcts.reform.managecase.service.cau;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;
import uk.gov.hmcts.reform.managecase.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.reform.managecase.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.reform.managecase.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.data.caseaccess.GlobalCaseRole.CREATOR;

@Service
public class CaseAccessOperation {

    public static final String ORGS_ASSIGNED_USERS_PATH = "orgs_assigned_users.";

    private final RoleAssignmentService roleAssignmentService;
    private final CaseDetailsRepository caseDetailsRepository;
    private final SupplementaryDataRepository supplementaryDataRepository;

    public CaseAccessOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER) final
                                CaseDetailsRepository caseDetailsRepository,
                                @Qualifier("default") SupplementaryDataRepository supplementaryDataRepository,
                                RoleAssignmentService roleAssignmentService) {
        this.roleAssignmentService = roleAssignmentService;
        this.caseDetailsRepository = caseDetailsRepository;
        this.supplementaryDataRepository = supplementaryDataRepository;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseReferences, List<String> userIds) {
        final var caseIds = caseReferences.stream().map(String::valueOf).collect(Collectors.toList());
        return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);
    }

    private List<CaseAssignedUserRole> findCaseUserRoles(
        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
    ) {
        List<CaseDetailsExtended> caseDetailsList = new ArrayList<>(cauRolesByCaseDetails.keySet());
        List<String> userIds = getUserIdsFromMap(cauRolesByCaseDetails);

        final var caseIds = caseDetailsList.stream()
            .map(CaseDetailsExtended::getReferenceAsString).collect(Collectors.toList());
        return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

    }

    @Transactional
    public void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {

        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails =
            getMapOfCaseAssignedUserRolesByCaseDetails(caseUserRoles);

        // Ignore case user role mappings that DO NOT exist
        // NB: also required so they don't effect revoked user counts.
        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> filteredCauRolesByCaseDetails =
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
                                                                      .caseId(caseDetails.getReferenceAsString())
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

        removeUserCounts.forEach((caseReference, orgNewUserCountMap) ->
                                     orgNewUserCountMap.forEach((organisationId, removeUserCount) ->
                                                                    supplementaryDataRepository
                                                                        .incrementSupplementaryData(caseReference,
                                                                        ORGS_ASSIGNED_USERS_PATH
                                                                            + organisationId,
                                                                        Math.negateExact(removeUserCount)
                                                                    )
                                     )
        );
    }

    private Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> findAndFilterOnExistingCauRoles(
        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
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
                        entry.getKey().getReferenceAsString(),
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
        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails
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

    private Map<CaseDetailsExtended,
        List<CaseAssignedUserRoleWithOrganisation>> getMapOfCaseAssignedUserRolesByCaseDetails(
        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles
    ) {

        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseCaseDetails
            = new HashMap<>();

        List<Long> caseReferences = caseUserRoles.stream()
            .map(CaseAssignedUserRoleWithOrganisation::getCaseDataId)
            .distinct()
            .map(Long::parseLong)
            .collect(Collectors.toCollection(ArrayList::new));

        // create map of case references to case details
        Map<Long, CaseDetailsExtended> caseDetailsByReferences = getCaseDetailsList(caseReferences).stream()
            .collect(Collectors.toMap(CaseDetailsExtended::getReference, caseDetailsExtended -> caseDetailsExtended));

        // group roles by case reference
        Map<String, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseReference = caseUserRoles.stream()
            .collect(Collectors.groupingBy(CaseAssignedUserRoleWithOrganisation::getCaseDataId));

        // merge both maps to check we have found all cases
        cauRolesByCaseReference.forEach((key, roles) -> {
            final Long caseReference = Long.parseLong(key);
            if (caseDetailsByReferences.containsKey(caseReference)) {
                cauRolesByCaseCaseDetails.put(caseDetailsByReferences.get(caseReference), roles);
            } else {
                throw new CaseNotFoundException(key);
            }
        });

        return cauRolesByCaseCaseDetails;
    }

    private List<CaseDetailsExtended> getCaseDetailsList(List<Long> caseReferences) {
        return caseReferences.stream()
            .map(caseReference -> {
                Optional<CaseDetailsExtended> caseDetails = caseDetailsRepository.findByReference(
                    null,
                    caseReference
                );
                return caseDetails.orElse(null);
            }).filter(Objects::nonNull)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, Map<String, Long>> getNewUserCountByCaseAndOrganisation(
        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> cauRolesByCaseDetails,
        List<CaseAssignedUserRole> existingCaseUserRoles
    ) {
        Map<String, Map<String, Long>> result = new HashMap<>();

        Map<CaseDetailsExtended, List<CaseAssignedUserRoleWithOrganisation>> caseUserRolesWhichHaveAnOrgId =
            cauRolesByCaseDetails.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                    // filter out no organisation_id and [CREATOR] case role
                    .filter(caseUserRole ->
                                StringUtils.isNoneBlank(caseUserRole.getOrganisationId())
                                    && !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR.getRole()))
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
                .filter(caseUserRole -> !caseUserRole.getCaseRole().equalsIgnoreCase(CREATOR.getRole()))
                .collect(Collectors.groupingBy(
                    caseUserRole -> Long.parseLong(caseUserRole.getCaseDataId()),
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        caseUserRole -> caseUserRole.stream()
                            .map(CaseAssignedUserRole::getUserId)
                            .distinct().collect(Collectors.toList())
                    )));

        // for each case: count new Case-User relationships by Organisation
        caseUserRolesWhichHaveAnOrgId.forEach((caseDetails, requestedAssignments) -> {
            List<String> existingUsersForCase
                = existingCaseUserRelationships.getOrDefault(caseDetails.getReference(), new ArrayList<>());

            Map<String, Long> relationshipCounts = requestedAssignments.stream()
                // filter out any existing relationships
                .filter(cauRole -> !existingUsersForCase.contains(cauRole.getUserId()))
                // count unique users for each organisation
                .collect(Collectors.groupingBy(
                    CaseAssignedUserRoleWithOrganisation::getOrganisationId,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        cauRolesForOrganisation -> cauRolesForOrganisation.stream()
                            .map(CaseAssignedUserRoleWithOrganisation::getUserId).distinct().count())));

            // skip if no organisations have any relationships
            if (!relationshipCounts.isEmpty()) {
                result.put(caseDetails.getReferenceAsString(), relationshipCounts);
            }
        });

        return result;
    }

}
