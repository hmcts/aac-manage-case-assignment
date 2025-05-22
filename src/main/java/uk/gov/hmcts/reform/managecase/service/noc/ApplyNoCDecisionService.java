package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.FeignException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.ValidationException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.ContactInformation;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.AddressUK;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.PreviousOrganisation;
import uk.gov.hmcts.reform.managecase.domain.PreviousOrganisationCollectionItem;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NotifyService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.COR_MISSING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.COR_MISSING_ORGANISATIONS;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_DATA_PROVIDED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.UNKNOWN_NOC_APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CREATED_BY;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.LAST_NOC_REQUESTED_BY;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_ADD;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.PREVIOUS_ORGANISATIONS;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.REJECTED;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.PrematureDeclaration", "PMD.ExcessiveImports"})
@Slf4j
public class ApplyNoCDecisionService {

    private static final String JSON_PATH_SEPARATOR = "/";

    private final PrdRepository prdRepository;
    private final DataStoreRepository dataStoreRepository;
    private final NotifyService notifyService;
    private final JacksonUtils jacksonUtils;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApplyNoCDecisionService(PrdRepository prdRepository,
                                   @Qualifier("defaultDataStoreRepository") DataStoreRepository dataStoreRepository,
                                   NotifyService notifyService,
                                   JacksonUtils jacksonUtils,
                                   ObjectMapper objectMapper) {
        this.prdRepository = prdRepository;
        this.dataStoreRepository = dataStoreRepository;
        this.notifyService = notifyService;
        this.jacksonUtils = jacksonUtils;
        this.objectMapper = objectMapper;
    }

    public Map<String, JsonNode> applyNoCDecision(ApplyNoCDecisionRequest applyNoCDecisionRequest) {
        CaseDetails caseDetails = applyNoCDecisionRequest.getCaseDetails();
        Map<String, JsonNode> data = caseDetails.getData();

        if (data == null) {
            throw new ValidationException(NO_DATA_PROVIDED);
        }

        JsonNode changeOrganisationRequestField = caseDetails.findChangeOrganisationRequestNode()
            .orElseThrow(() -> new ValidationException(COR_MISSING));

        validateCorFieldOrganisations(changeOrganisationRequestField);
        String approvalStatus = getNonNullStringValue(changeOrganisationRequestField, APPROVAL_STATUS);
        String caseRoleId = getNonNullStringValue(changeOrganisationRequestField, CASE_ROLE_ID + ".value.code");

        if (PENDING.getValue().equals(approvalStatus)) {
            throw new ValidationException(NOC_REQUEST_NOT_CONSIDERED);
        } else if (REJECTED.getValue().equals(approvalStatus)) {
            nullifyNode(changeOrganisationRequestField, CASE_ROLE_ID);
            return data;
        } else if (!APPROVED.getValue().equals(approvalStatus)) {
            throw new ValidationException(UNKNOWN_NOC_APPROVAL_STATUS);
        }

        applyDecision(caseDetails, changeOrganisationRequestField, caseRoleId);

        nullifyNode(changeOrganisationRequestField, CASE_ROLE_ID);
        return data;
    }

    private void applyDecision(CaseDetails caseDetails, JsonNode changeOrganisationRequestField, String caseRoleId) {
        JsonNode orgPolicyNode = caseDetails.findOrganisationPolicyNodeForCaseRole(caseRoleId);

        JsonNode organisationToAddNode = changeOrganisationRequestField.get(ORGANISATION_TO_ADD);
        JsonNode organisationToRemoveNode = changeOrganisationRequestField.get(ORGANISATION_TO_REMOVE);
        Organisation organisationToAdd = objectMapper.convertValue(organisationToAddNode, Organisation.class);
        Organisation organisationToRemove = objectMapper.convertValue(organisationToRemoveNode, Organisation.class);
        JsonNode createdBy = changeOrganisationRequestField.get(CREATED_BY);

        List<CaseUserRole> existingCaseAssignments = dataStoreRepository
            .getCaseAssignments(singletonList(caseDetails.getId()), null);

        if (organisationToAdd == null || isNullOrEmpty(organisationToAdd.getOrganisationID())) {
            applyRemoveRepresentationDecision(existingCaseAssignments, caseRoleId, orgPolicyNode, organisationToRemove,
                caseDetails.getId());
        } else {
            applyAddOrReplaceRepresentationDecision(existingCaseAssignments, caseRoleId, orgPolicyNode,
                organisationToAddNode, organisationToAdd, organisationToRemove, caseDetails.getId(),
                createdBy);
        }

        setOrgPolicyPreviousOrganisations(caseDetails, organisationToAdd, organisationToRemove, orgPolicyNode);
    }

    private void validateCorFieldOrganisations(JsonNode changeOrganisationRequestField) {
        if (!changeOrganisationRequestField.has(ORGANISATION_TO_ADD)
            || !changeOrganisationRequestField.has(ORGANISATION_TO_REMOVE)) {
            throw new ValidationException(COR_MISSING_ORGANISATIONS);
        }
    }

    @SuppressWarnings({"squid:S1075"})
    private void applyAddOrReplaceRepresentationDecision(List<CaseUserRole> existingCaseAssignments,
                                                         String caseRoleId,
                                                         JsonNode orgPolicyNode,
                                                         JsonNode organisationToAddNode,
                                                         Organisation organisationToAdd,
                                                         Organisation organisationToRemove,
                                                         String caseReference, JsonNode createdBy) {

        ((ObjectNode) orgPolicyNode).put(LAST_NOC_REQUESTED_BY, (createdBy == null) ? null : createdBy.asText());

        setOrgPolicyOrganisation(orgPolicyNode, organisationToAddNode);
        Pair<List<CaseUserRole>, List<ProfessionalUser>> newAssignedUsers = assignAccessToOrganisationUsers(
            existingCaseAssignments, organisationToAdd, caseRoleId, caseReference);

        if (organisationToRemove != null && !isNullOrEmpty(organisationToRemove.getOrganisationID())) {
            List<CaseUserRole> filteredCaseAssignments =
                filterCaseAssignments(existingCaseAssignments, newAssignedUsers.getLeft());
            removeOrganisationUsersAccess(caseReference, filteredCaseAssignments,
                organisationToRemove, caseRoleId);
        }
    }

    private List<CaseUserRole> filterCaseAssignments(List<CaseUserRole> existingCaseAssignments,
                                                     List<CaseUserRole> assignmentsToFilter) {
        List<String> userIdsToFilter = assignmentsToFilter.stream().map(CaseUserRole::getUserId).collect(toList());
        return existingCaseAssignments.stream()
            .filter(assignment -> !userIdsToFilter.contains(assignment.getUserId()))
            .collect(toList());
    }

    private void applyRemoveRepresentationDecision(List<CaseUserRole> existingCaseAssignments,
                                                   String caseRoleId,
                                                   JsonNode orgPolicyNode,
                                                   Organisation organisationToRemove,
                                                   String caseReference) {
        nullifyNode(orgPolicyNode.get(ORGANISATION));
        ((ObjectNode) orgPolicyNode).putNull(LAST_NOC_REQUESTED_BY);
        removeOrganisationUsersAccess(caseReference, existingCaseAssignments, organisationToRemove, caseRoleId);
    }

    private Pair<List<CaseUserRole>, List<ProfessionalUser>> assignAccessToOrganisationUsers(
        List<CaseUserRole> existingCaseAssignments,
        Organisation organisationToAdd,
        String caseRoleToBeAssigned,
        String caseReference) {
        String organisationId = organisationToAdd.getOrganisationID();
        Pair<List<CaseUserRole>, List<ProfessionalUser>> users =
            getUsersWithCaseAccess(existingCaseAssignments, organisationId);

        users.getRight().forEach(user -> dataStoreRepository.assignCase(
            singletonList(caseRoleToBeAssigned),
            caseReference,
            user.getUserIdentifier(),
            organisationId)
        );

        return users;
    }

    private void setOrgPolicyOrganisation(JsonNode orgPolicyNode, JsonNode organisationToAddNode) {
        ((ObjectNode) orgPolicyNode).set(ORGANISATION, organisationToAddNode.deepCopy());
    }

    private void removeOrganisationUsersAccess(String caseReference,
                                               List<CaseUserRole> existingCaseAssignments,
                                               Organisation organisationToRemove,
                                               String caseRoleId) {
        Pair<List<CaseUserRole>, List<ProfessionalUser>> users =
            getUsersWithCaseAccess(existingCaseAssignments, organisationToRemove.getOrganisationID(), caseRoleId);

        if (!users.getLeft().isEmpty()) {
            dataStoreRepository.removeCaseUserRoles(users.getLeft(), organisationToRemove.getOrganisationID());
            sendRemovalNotification(caseReference, users.getRight());
        }
    }

    private String getNonNullStringValue(JsonNode node, String field) {
        String path = JSON_PATH_SEPARATOR + field.replace(".", JSON_PATH_SEPARATOR);
        JsonNode nodeAtPath = node.at(path);
        if (nodeAtPath.isMissingNode() || nodeAtPath.isNull()) {
            throw new ValidationException(String.format("A value is expected for '%s'", field));
        }
        return nodeAtPath.asText();
    }

    private void nullifyNode(JsonNode node, String... ignoreNestedFields) {
        jacksonUtils.nullifyObjectNode((ObjectNode) node, ignoreNestedFields);
    }

    private List<EmailNotificationRequestStatus> sendRemovalNotification(String caseReference,
                                                                         List<ProfessionalUser> users) {
        List<EmailNotificationRequest> emailNotificationRequests = users.stream()
            .map(professionalUser -> new EmailNotificationRequest(caseReference, professionalUser.getEmail()))
            .collect(toList());

        return notifyService.sendEmail(emailNotificationRequests);
    }

    private FindUsersByOrganisationResponse findUsersByOrganisation(String organisationId) {
        try {
            return prdRepository.findUsersByOrganisation(organisationId);
        } catch (FeignException e) {
            HttpStatus status = HttpStatus.resolve(e.status());
            String reasonPhrase = status == null ? e.getMessage() : status.getReasonPhrase();

            String errorMessage = status == HttpStatus.NOT_FOUND
                ? String.format("Organisation with ID '%s' can not be found.", organisationId)
                : String.format("Error encountered while retrieving organisation users for organisation ID '%s': %s",
                organisationId, reasonPhrase);

            throw new ValidationException(errorMessage, e);
        }
    }

    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getUsersWithCaseAccess(
        List<CaseUserRole> existingCaseAssignments,
        String organisationId) {

        FindUsersByOrganisationResponse usersByOrganisation = findUsersByOrganisation(organisationId);
        return getIntersection(existingCaseAssignments, usersByOrganisation.getUsers());
    }

    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getUsersWithCaseAccess(
        List<CaseUserRole> existingCaseAssignments,
        String organisationId,
        String caseRoleId) {

        FindUsersByOrganisationResponse usersByOrganisation = findUsersByOrganisation(organisationId);
        return getIntersection(existingCaseAssignments, usersByOrganisation.getUsers(), caseRoleId);
    }

    /**
     * Obtain the intersection of a list of case user role assignments and professional users.
     * Users are considered the same if their ID matches.
     * @param caseUserRoles case use role assignments
     * @param professionalUsers professional users
     * @return the intersection - the list of filtered case user role assignments and
     *         professional users for the intersection are both provided
     */
    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getIntersection(List<CaseUserRole> caseUserRoles,
                                                                             List<ProfessionalUser> professionalUsers) {
        List<CaseUserRole> caseUserRolesIntersection = new ArrayList<>();
        List<ProfessionalUser> professionalUsersIntersection = new ArrayList<>();

        professionalUsers.forEach(professionalUser -> {
            Optional<CaseUserRole> caseUserRoleOptional = caseUserRoles.stream()
                .filter(caseUserRole -> caseUserRole.getUserId().equals(professionalUser.getUserIdentifier()))
                .findFirst();

            caseUserRoleOptional.ifPresent(caseUserRole -> {
                caseUserRolesIntersection.add(caseUserRole);
                professionalUsersIntersection.add(professionalUser);
            });
        });

        return new ImmutablePair<>(caseUserRolesIntersection, professionalUsersIntersection);
    }

    /**
     * Obtain the intersection of a list of case user role assignments matching case role ID and professional users.
     * Users are considered the same if their ID and case role both match.
     * @param caseUserRoles case use role assignments
     * @param professionalUsers professional users
     * @param caseRoleId case role identifier
     * @return the intersection - the list of filtered case user role assignments and
     *         professional users for the intersection are both provided
     */
    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getIntersection(List<CaseUserRole> caseUserRoles,
                                                                             List<ProfessionalUser> professionalUsers,
                                                                             String caseRoleId) {
        List<CaseUserRole> caseUserRolesIntersection = new ArrayList<>();
        List<ProfessionalUser> professionalUsersIntersection = new ArrayList<>();

        professionalUsers.forEach(professionalUser -> {
            Optional<CaseUserRole> caseUserRoleOptional = caseUserRoles.stream()
                .filter(caseUserRole -> caseUserRole.getUserId().equals(professionalUser.getUserIdentifier())
                                        && caseUserRole.getCaseRole().equals(caseRoleId))
                .findFirst();

            caseUserRoleOptional.ifPresent(caseUserRole -> {
                caseUserRolesIntersection.add(caseUserRole);
                professionalUsersIntersection.add(professionalUser);
            });
        });

        return new ImmutablePair<>(caseUserRolesIntersection, professionalUsersIntersection);
    }

    private void setOrgPolicyPreviousOrganisations(final CaseDetails caseDetails,
                                                   final Organisation organisationToAdd,
                                                   final Organisation organisationToRemove,
                                                   final JsonNode orgPolicyNode) {
        if (organisationToRemove != null && !isNullOrEmpty(organisationToRemove.getOrganisationID())) {

            FindOrganisationResponse response = prdRepository
                .findOrganisationAddress(organisationToRemove.getOrganisationID());

            if (!response.getContactInformation().isEmpty()) {
                PreviousOrganisation previousOrganisation = createPreviousOrganisation(
                    caseDetails,
                    orgPolicyNode,
                    response);
                insertPreviousOrgNode(orgPolicyNode, previousOrganisation);
            }
        } else if (organisationToAdd != null && !isNullOrEmpty(organisationToAdd.getOrganisationID())) {
            PreviousOrganisation previousOrganisation = PreviousOrganisation
                .builder()
                .fromTimestamp(getFromTimeStamp(caseDetails, orgPolicyNode))
                .toTimestamp(LocalDateTime.now())
                .build();
            insertPreviousOrgNode(orgPolicyNode, previousOrganisation);
        }
    }

    private void insertPreviousOrgNode(JsonNode orgPolicyNode, PreviousOrganisation previousOrganisation) {
        PreviousOrganisationCollectionItem previousOrganisationCollectionItem =
            new PreviousOrganisationCollectionItem(null, previousOrganisation);

        ((ArrayNode) orgPolicyNode
            .withArray(PREVIOUS_ORGANISATIONS))
            .insert(0, objectMapper.valueToTree(previousOrganisationCollectionItem));
    }

    private PreviousOrganisation createPreviousOrganisation(final CaseDetails caseDetails,
                                                            final JsonNode orgPolicyNode,
                                                            final FindOrganisationResponse response) {
        if (response.getContactInformation().size() > 1) {
            log.warn("More than one address received in the response for the organisation {},"
                     + " using first address from the list.", response.getOrganisationIdentifier());
        }
        AddressUK organisationAddress = createOrganisationAddress(response.getContactInformation().get(0));
        return PreviousOrganisation
            .builder()
            .organisationName(response.getName())
            .organisationAddress(organisationAddress)
            .fromTimestamp(getFromTimeStamp(caseDetails, orgPolicyNode))
            .toTimestamp(LocalDateTime.now())
            .build();
    }

    private AddressUK createOrganisationAddress(final ContactInformation contactInformation) {
        return AddressUK.builder()
            .addressLine1(contactInformation.getAddressLine1())
            .addressLine2(contactInformation.getAddressLine2())
            .addressLine3(contactInformation.getAddressLine3())
            .country(contactInformation.getCountry())
            .county(contactInformation.getCounty())
            .postCode(contactInformation.getPostCode())
            .postTown(contactInformation.getTownCity())
            .build();
    }

    private LocalDateTime getFromTimeStamp(final CaseDetails caseDetails,
                                           final JsonNode orgPolicyNode) {
        LocalDateTime caseCreatedDate = caseDetails.getCreatedDate();
        try {
            JsonNode prevOrgsNode = orgPolicyNode.get(PREVIOUS_ORGANISATIONS);

            if (prevOrgsNode == null) {
                return caseCreatedDate;
            }
            List<PreviousOrganisationCollectionItem> previousOrganisations = objectMapper
                .readerFor(new TypeReference<List<PreviousOrganisationCollectionItem>>() {
                }).readValue(prevOrgsNode);
            if (previousOrganisations.isEmpty()) {
                return caseCreatedDate;
            }
            previousOrganisations.sort(
                Comparator.comparing(prevOrgCollectionItem -> prevOrgCollectionItem.getValue().getToTimestamp()));
            return previousOrganisations.get(previousOrganisations.size() - 1).getValue().getToTimestamp();
        } catch (IOException ie) {
            log.warn("Error encountered while reading PreviousOrganisations from an OrganisationPolicy: ", ie);
            return caseCreatedDate;
        }
    }
}
