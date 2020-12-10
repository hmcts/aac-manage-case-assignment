package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.ContactInformation;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationAddress;
import uk.gov.hmcts.reform.managecase.domain.PreviousOrganisation;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NotifyService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.COR_MISSING_ORGANISATIONS;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_DATA_PROVIDED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.UNKNOWN_NOC_APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.REJECTED;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_ADD;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.PREVIOUS_ORGANISATIONS;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.PrematureDeclaration", "PMD.ExcessiveImports"})
@Slf4j
public class ApplyNoCDecisionService {

    private final PrdRepository prdRepository;
    private final DataStoreRepository dataStoreRepository;
    private final NotifyService notifyService;
    private final JacksonUtils jacksonUtils;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApplyNoCDecisionService(PrdRepository prdRepository,
                                   DataStoreRepository dataStoreRepository,
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

        // TODO: Use ACA-71 method when available on branch
        JsonNode changeOrganisationRequestField = data.get("ChangeOrganisationRequestField");

        String approvalStatus = getNonNullStringValue(changeOrganisationRequestField, APPROVAL_STATUS);
        String caseRoleId = getNonNullStringValue(changeOrganisationRequestField, CASE_ROLE_ID);
        validateCorFieldOrganisations(changeOrganisationRequestField);

        if (NOT_CONSIDERED.getCode().equals(approvalStatus)) {
            throw new ValidationException(NOC_REQUEST_NOT_CONSIDERED);
        } else if (REJECTED.getCode().equals(approvalStatus)) {
            nullifyNode(changeOrganisationRequestField);
            return data;
        } else if (!APPROVED.getCode().equals(approvalStatus)) {
            throw new ValidationException(UNKNOWN_NOC_APPROVAL_STATUS);
        }

        applyDecision(caseDetails, changeOrganisationRequestField, caseRoleId);

        nullifyNode(changeOrganisationRequestField);
        return data;
    }

    private void applyDecision(CaseDetails caseDetails, JsonNode changeOrganisationRequestField, String caseRoleId) {
        JsonNode orgPolicyNode = caseDetails.findOrganisationPolicyNodeForCaseRole(caseRoleId);

        JsonNode organisationToAddNode = changeOrganisationRequestField.get(ORGANISATION_TO_ADD);
        JsonNode organisationToRemoveNode = changeOrganisationRequestField.get(ORGANISATION_TO_REMOVE);
        Organisation organisationToAdd = objectMapper.convertValue(organisationToAddNode, Organisation.class);
        Organisation organisationToRemove = objectMapper.convertValue(organisationToRemoveNode, Organisation.class);

        if (organisationToAdd == null || isNullOrEmpty(organisationToAdd.getOrganisationID())) {
            applyRemoveRepresentationDecision(caseDetails.getReference(), orgPolicyNode, organisationToRemove);
        } else {
            applyAddOrReplaceRepresentationDecision(caseDetails.getReference(), caseRoleId, orgPolicyNode,
                    organisationToAddNode, organisationToAdd, organisationToRemove);
        }

        setOrgPolicyPreviousOrganisations(caseDetails, changeOrganisationRequestField, orgPolicyNode);
    }

    private void validateCorFieldOrganisations(JsonNode changeOrganisationRequestField) {
        if (!changeOrganisationRequestField.has(ORGANISATION_TO_ADD)
                || !changeOrganisationRequestField.has(ORGANISATION_TO_REMOVE)) {
            throw new ValidationException(COR_MISSING_ORGANISATIONS);
        }
    }

    private void applyAddOrReplaceRepresentationDecision(String caseReference,
                                                         String caseRoleId,
                                                         JsonNode orgPolicyNode,
                                                         JsonNode organisationToAddNode,
                                                         Organisation organisationToAdd,
                                                         Organisation organisationToRemove) {
        setOrgPolicyOrganisation(orgPolicyNode, organisationToAddNode);
        assignAccessToOrganisationUsers(caseReference, organisationToAdd, caseRoleId);

        if (organisationToRemove != null && !isNullOrEmpty(organisationToRemove.getOrganisationID())) {
            removeOrganisationUsersAccess(caseReference, organisationToRemove, caseRoleId);
        }
    }

    private void applyRemoveRepresentationDecision(String caseReference,
                                                   JsonNode orgPolicyNode,
                                                   Organisation organisationToRemove) {
        nullifyNode(orgPolicyNode.get(ORGANISATION));
        removeOrganisationUsersAccess(caseReference, organisationToRemove, null);
    }

    private void assignAccessToOrganisationUsers(String caseReference,
                                                 Organisation organisationToAdd,
                                                 String caseRoleToBeAssigned) {
        String organisationId = organisationToAdd.getOrganisationID();
        Pair<List<CaseUserRole>, List<ProfessionalUser>> users =
            getUsersWithCaseAccess(caseReference, organisationId, null);

        users.getRight().forEach(user -> dataStoreRepository.assignCase(
            singletonList(caseRoleToBeAssigned),
            caseReference,
            user.getUserIdentifier(),
            organisationId)
        );
    }

    private void setOrgPolicyOrganisation(JsonNode orgPolicyNode, JsonNode organisationToAddNode) {
        ((ObjectNode) orgPolicyNode).set(ORGANISATION, organisationToAddNode.deepCopy());
    }

    private void removeOrganisationUsersAccess(String caseReference,
                                               Organisation organisationToRemove,
                                               String ignoredRole) {
        Pair<List<CaseUserRole>, List<ProfessionalUser>> users =
            getUsersWithCaseAccess(caseReference, organisationToRemove.getOrganisationID(), ignoredRole);

        if (!users.getLeft().isEmpty()) {
            dataStoreRepository.removeCaseUserRoles(users.getLeft(), organisationToRemove.getOrganisationID());
            sendRemovalNotification(caseReference, users.getRight());
        }
    }

    private String getNonNullStringValue(JsonNode node, String key) {
        if (node.hasNonNull(key)) {
            return node.get(key).asText();
        } else {
            throw new ValidationException(String.format("A value is expected for '%s'", key));
        }
    }

    private void nullifyNode(JsonNode node) {
        jacksonUtils.nullifyObjectNode((ObjectNode) node);
    }

    private List<EmailNotificationRequestStatus> sendRemovalNotification(String caseReference,
                                                                         List<ProfessionalUser> users) {
        List<EmailNotificationRequest> emailNotificationRequests = users.stream()
            .map(professionalUser -> new EmailNotificationRequest(caseReference, professionalUser.getEmail()))
            .collect(toList());

        return notifyService.sendEmail(emailNotificationRequests);
    }

    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getUsersWithCaseAccess(String caseReference,
                                                                                    String organisationId,
                                                                                    String ignoredRole) {
        List<CaseUserRole> existingCaseAssignments = dataStoreRepository
            .getCaseAssignments(singletonList(caseReference), null)
            .stream()
            .filter(caseUserRole -> !caseUserRole.getCaseRole().equals(ignoredRole))
            .collect(toList());

        FindUsersByOrganisationResponse usersByOrganisation = prdRepository.findUsersByOrganisation(organisationId);

        return getIntersection(existingCaseAssignments, usersByOrganisation.getUsers());
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

    private void setOrgPolicyPreviousOrganisations(final CaseDetails caseDetails,
                                                   final JsonNode changeOrganisationRequestField,
                                                   final JsonNode orgPolicyNode) {
        JsonNode organisationToRemoveNode = changeOrganisationRequestField.get(ORGANISATION_TO_REMOVE);
        Organisation organisationToRemove = objectMapper.convertValue(organisationToRemoveNode, Organisation.class);
        if (organisationToRemove != null && !isNullOrEmpty(organisationToRemove.getOrganisationID())) {

            FindOrganisationResponse response = prdRepository
                .findOrganisationAddress(organisationToRemove.getOrganisationID());

            if (response.getContactInformation().size() > 0) {
                PreviousOrganisation previousOrganisation = createPreviousOrganisation(
                    caseDetails,
                    orgPolicyNode,
                    response);
                ((ArrayNode) orgPolicyNode
                    .withArray(PREVIOUS_ORGANISATIONS))
                    .insert(0, objectMapper.valueToTree(previousOrganisation));
            }
        }
    }

    private PreviousOrganisation createPreviousOrganisation(final CaseDetails caseDetails,
                                                            final JsonNode orgPolicyNode,
                                                            final FindOrganisationResponse response) {
        if (response.getContactInformation().size() > 1) {
            log.warn("More than one address received in the response for the organisation {},"
                         + " using first address from the list.", response.getOrganisationIdentifier());
        }
        OrganisationAddress organisationAddress =  createOrganisationAddress(response.getContactInformation().get(0));
        return PreviousOrganisation
            .builder()
            .organisationName(response.getName())
            .organisationAddresses(Lists.newArrayList(organisationAddress))
            .fromTimestamp(getFromTimeStamp(caseDetails, orgPolicyNode))
            .toTimestamp(LocalDateTime.now())
            .build();
    }

    private OrganisationAddress createOrganisationAddress(final ContactInformation contactInformation) {
        return OrganisationAddress.builder()
            .addressLine1(contactInformation.getAddressLine1())
            .addressLine2(contactInformation.getAddressLine2())
            .addressLine3(contactInformation.getAddressLine3())
            .country(contactInformation.getCountry())
            .county(contactInformation.getCounty())
            .postCode(contactInformation.getPostCode())
            .townCity(contactInformation.getTownCity())
            .build();
    }

    private LocalDateTime getFromTimeStamp(final CaseDetails caseDetails,
                                           final JsonNode orgPolicyNode) {
        try {
            JsonNode prevOrgsNode = orgPolicyNode.get(PREVIOUS_ORGANISATIONS);

            if (prevOrgsNode == null) {
                return caseDetails.getCreatedDate();
            }
            List<PreviousOrganisation> previousOrganisations = objectMapper
                .readerFor(new TypeReference<List<PreviousOrganisation>>() {}).readValue(prevOrgsNode);
            previousOrganisations.sort(Comparator.comparing(PreviousOrganisation::getToTimestamp));
            return previousOrganisations.get(previousOrganisations.size() - 1).getToTimestamp();
        } catch (IOException ie) {
            return LocalDateTime.now();
        }
    }
}
