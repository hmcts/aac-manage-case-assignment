package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NotifyService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_ADD;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;

@Service
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
        String caseReference = caseDetails.getReference();
        Map<String, JsonNode> data = caseDetails.getData();

        // TODO: Use ACA-71 method when available on branch (and double-check it errors when required)
        JsonNode changeOrganisationRequestField = data.get("ChangeOrganisationRequestField");

        String approvalStatus = getNonNullStringValue(changeOrganisationRequestField, APPROVAL_STATUS);
        String caseRoleId = getNonNullStringValue(changeOrganisationRequestField, CASE_ROLE_ID);
        validateCorFieldOrganisations(changeOrganisationRequestField);

        if (approvalStatus.equals("Not considered")) {
            throw new ValidationException("Pending request!");
        } else if (approvalStatus.equals("Rejected")) {
            nullifyNode(changeOrganisationRequestField);
            return data;
        } else if (!approvalStatus.equals("Approved")) {
            throw new ValidationException("Unknown approval status!");
        }

        JsonNode orgPolicyNode = caseDetails.findOrganisationPolicyNodeForCaseRole(caseRoleId)
                .orElseThrow(() -> new ValidationException("None found"));

        JsonNode organisationToAddNode = changeOrganisationRequestField.get(ORGANISATION_TO_ADD);
        JsonNode organisationToRemoveNode = changeOrganisationRequestField.get(ORGANISATION_TO_REMOVE);
        Organisation organisationToAdd = objectMapper.convertValue(organisationToAddNode, Organisation.class);
        Organisation organisationToRemove = objectMapper.convertValue(organisationToRemoveNode, Organisation.class);

        if (organisationToAdd == null || isNullOrEmpty(organisationToAdd.getOrganisationID())) {
            applyRemoveRepresentationDecision(caseReference, orgPolicyNode, organisationToRemove);
        } else {
            applyAddOrReplaceRepresentationDecision(caseReference, caseRoleId, orgPolicyNode,
                    organisationToAddNode, organisationToAdd, organisationToRemove);
        }

        nullifyNode(changeOrganisationRequestField);
        return data;
    }

    private void validateCorFieldOrganisations(JsonNode changeOrganisationRequestField) {
        if (!changeOrganisationRequestField.has(ORGANISATION_TO_ADD)
                || !changeOrganisationRequestField.has(ORGANISATION_TO_REMOVE)) {
            throw new ValidationException("Fields of type ChangeOrganisationRequest must include both an " +
                    "OrganisationToAdd and OrganisationToRemove field.");
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
            removeOrganisationUsersAccess(caseReference, organisationToRemove);
        }
    }

    private void applyRemoveRepresentationDecision(String caseReference, JsonNode orgPolicyNode, Organisation organisationToRemove) {
        nullifyNode(orgPolicyNode.get(ORGANISATION));
        removeOrganisationUsersAccess(caseReference, organisationToRemove);
    }

    private void assignAccessToOrganisationUsers(String caseReference, Organisation organisation, String caseRoleToBeAssigned) {
        FindUsersByOrganisationResponse response = prdRepository.findUsersByOrganisation(organisation.getOrganisationID());
        response.getUsers().forEach(user ->
                dataStoreRepository.assignCase(
                        singletonList(caseRoleToBeAssigned),
                        caseReference,
                        user.getUserIdentifier(),
                        response.getOrganisationIdentifier())
        );
    }

    private void setOrgPolicyOrganisation(JsonNode orgPolicyNode, JsonNode organisationToAddNode) {
        ((ObjectNode) orgPolicyNode).set(ORGANISATION, organisationToAddNode.deepCopy());
    }

    private void removeOrganisationUsersAccess(String caseReference, Organisation organisationToRemove) {
        Pair<List<CaseUserRole>, List<ProfessionalUser>> users =
            getUsersWithCaseAccess(caseReference, organisationToRemove.getOrganisationID());

        dataStoreRepository.removeCaseUserRoles(users.getLeft(), organisationToRemove.getOrganisationID());

        sendRemovalNotification(caseReference, users.getRight());
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

    private List<EmailNotificationRequestStatus> sendRemovalNotification(String caseReference, List<ProfessionalUser> users) {
        List<EmailNotificationRequest> emailNotificationRequests = users.stream()
            .map(professionalUser -> new EmailNotificationRequest(caseReference, professionalUser.getEmail()))
            .collect(toList());

        return notifyService.sendEmail(emailNotificationRequests);
    }

    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getUsersWithCaseAccess(String caseReference,
                                                                                    String organisationId) {
        List<CaseUserRole> existingCaseAssignments = dataStoreRepository
            .getCaseAssignments(singletonList(caseReference), null);

        FindUsersByOrganisationResponse usersByOrganisation = prdRepository.findUsersByOrganisation(organisationId);

        return getIntersection(existingCaseAssignments, usersByOrganisation.getUsers());
    }

    /**
     * Obtain the intersection of a list of case user role assignments and professional users.
     * Users are considered the same if their ID matches.
     * @param caseUserRoles case use role assignments
     * @param professionalUsers professional users
     * @return the intersection - the list of filtered case user role assignments and
     * professional users are both provided
     */
    private Pair<List<CaseUserRole>, List<ProfessionalUser>> getIntersection(List<CaseUserRole> caseUserRoles,
                                                                             List<ProfessionalUser> professionalUsers) {
        List<CaseUserRole> caseUserRolesIntersection = new ArrayList<>();
        List<ProfessionalUser> professionalUsersIntersection = new ArrayList<>();

        professionalUsers.forEach(professionalUser -> {
            Optional<CaseUserRole> caseUserRoleOptional = caseUserRoles.stream()
                .filter(cur -> cur.getUserId().equals(professionalUser.getUserIdentifier()))
                .findFirst();

            caseUserRoleOptional.ifPresent(caseUserRole -> {
                caseUserRolesIntersection.add(caseUserRole);
                professionalUsersIntersection.add(professionalUser);
            });
        });

        return new ImmutablePair<>(caseUserRolesIntersection, professionalUsersIntersection);
    }
}
