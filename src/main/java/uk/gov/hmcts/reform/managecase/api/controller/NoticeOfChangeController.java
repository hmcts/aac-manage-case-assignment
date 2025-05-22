package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.hibernate.validator.constraints.LuhnCheck;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToStartCallbackRequest;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToStartCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionResponse;
import uk.gov.hmcts.reform.managecase.api.payload.CallbackRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.SubmitCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.service.noc.ApplyNoCDecisionService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeApprovalService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.PrepareNoCService;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHALLENGE_QUESTION_ANSWERS_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INVALID_CASE_ROLE_FIELD;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_DECISION_EVENT_UNIDENTIFIABLE;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;

@RestController
@Validated
@RequestMapping(path = "/noc")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class NoticeOfChangeController {

    @SuppressWarnings({"squid:S1075"})
    public static final String GET_NOC_QUESTIONS = "/noc-questions";
    public static final String VERIFY_NOC_ANSWERS = "/verify-noc-answers";
    public static final String APPLY_NOC_DECISION = "/apply-decision";
    public static final String NOC_PREPARE_PATH = "/noc-prepare";

    public static final String VERIFY_NOC_ANSWERS_MESSAGE = "Notice of Change answers verified successfully";

    public static final String REQUEST_NOTICE_OF_CHANGE_PATH = "/noc-requests";
    public static final String CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH = "/check-noc-approval";
    public static final String SET_ORGANISATION_TO_REMOVE_PATH = "/set-organisation-to-remove";

    public static final String REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE =
        "The Notice of Change request has been successfully submitted.";
    public static final String CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE = "Not yet approved";
    public static final String CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE = "Approval Applied";

    private final NoticeOfChangeQuestions noticeOfChangeQuestions;

    private final NoticeOfChangeApprovalService noticeOfChangeApprovalService;
    private final VerifyNoCAnswersService verifyNoCAnswersService;
    private final PrepareNoCService prepareNoCService;
    private final RequestNoticeOfChangeService requestNoticeOfChangeService;
    private final ApplyNoCDecisionService applyNoCDecisionService;
    private final JacksonUtils jacksonUtils;

    public NoticeOfChangeController(NoticeOfChangeQuestions noticeOfChangeQuestions,
                                    NoticeOfChangeApprovalService noticeOfChangeApprovalService,
                                    VerifyNoCAnswersService verifyNoCAnswersService,
                                    PrepareNoCService prepareNoCService,
                                    RequestNoticeOfChangeService requestNoticeOfChangeService,
                                    ApplyNoCDecisionService applyNoCDecisionService,
                                    JacksonUtils jacksonUtils) {
        this.noticeOfChangeQuestions = noticeOfChangeQuestions;
        this.verifyNoCAnswersService = verifyNoCAnswersService;
        this.prepareNoCService = prepareNoCService;
        this.requestNoticeOfChangeService = requestNoticeOfChangeService;
        this.applyNoCDecisionService = applyNoCDecisionService;
        this.noticeOfChangeApprovalService = noticeOfChangeApprovalService;
        this.jacksonUtils = jacksonUtils;
    }

    @GetMapping(path = GET_NOC_QUESTIONS, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Notice of Change questions", description = "Get Notice of Change questions")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(
        responseCode = "200",
        description = "Challenge questions returned successfully.",
        content = @Content(
            schema = @Schema(implementation = ChallengeQuestionsResult.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "questions":[{
                            "case_type_id":"caseType",
                            "order":1,
                            "question_text":"questionText",
                            "answer_field_type":{
                                "id":"Number",
                                "type":"Number",
                                "min":null,
                                "max":null,
                                "regular_expression":null,
                                "fixed_list_items":[],
                                "complex_fields":[],
                                "collection_field_type":null
                            },
                            "display_context_parameter":null,
                            "challenge_question_id":"NoC",
                            "answer_field":null,
                            "question_id":"QuestionId1"
                        }]
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "400",
        description = """
            One or more of the following reasons:
            1. Case ID can not be empty, 
            2. Case ID has to be a valid 16-digit Luhn number, 
            3. No NoC events available for this case type, 
            4. Multiple NoC Request events found for the user, 
            5. More than one change request found on the case, 
            6. Ongoing NoC request in progress 
            7. Insufficient privileges for notice of change request 
            8. No Organisation Policy for one or more of the roles available
             for the notice of change request 
            """,
        content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "message": "Case ID has to be a valid 16-digit Luhn number"
                        "code": "case-id-invalid"
                        "status": "BAD_REQUEST"
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "404",
        description = "Case could not be found",
        content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "message": "Case could not be found",
                        "status": "NOT_FOUND"
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public ChallengeQuestionsResult getNoticeOfChangeQuestions(@RequestParam("case_id")
                                                               @Valid
                                                               @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
                                                               @Size(min = 16, max = 16, message =
                                                                   NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
                                                               @LuhnCheck(message =
                                                                   NoCValidationError.NOC_CASE_ID_INVALID,
                                                                   ignoreNonDigitCharacters = false)
                                                                   String caseId) {
        return noticeOfChangeQuestions.getChallengeQuestions(caseId);
    }

    @PostMapping(path = VERIFY_NOC_ANSWERS, produces = APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Verify Notice of Change answers",
        description = "Use to validate the answers provided by a user wishing to raise a "
            + "Notice of Change Request to gain access to a case"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Notice of Change answers verified successfully.",
        content = @Content(
            schema = @Schema(implementation = VerifyNoCAnswersResponse.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "organisation": {
                            "OrganisationID": "QUK822NA",
                            "OrganisationName": "Some Org"
                        },
                        "status_message": "Notice of Change answers verified successfully"
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "400",
        description = """
            One or more of the following reasons:
            1. Any of the `400` errors returned by the `Get Notice of Change questions` operation
            2. Challenge question answers can not be empty
            3. The number of provided answers must match the number of questions - expected %s answers, received %s
            4. The answers did not match those for any litigant
            5. The answers did not uniquely identify a litigant
            6. No answer has been provided for question ID '%s'
            7. No OrganisationPolicy exists on the case for the case role '%s'
            8. The requestor has answered questions uniquely identifying a litigant that they are already representing
            """,
        content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "status": "BAD_REQUEST",
                        "message": "The number of provided answers must match the number of questions 
                            - expected 1 answers, received 2",
                        "errors": []
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "404",
        description = "Case could not be found",
        content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "message": "Case could not be found",
                        "status": "NOT_FOUND"
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public VerifyNoCAnswersResponse verifyNoticeOfChangeAnswers(
        @Valid @RequestBody VerifyNoCAnswersRequest verifyNoCAnswersRequest) {
        NoCRequestDetails result = verifyNoCAnswersService.verifyNoCAnswers(verifyNoCAnswersRequest);
        return result.toVerifyNoCAnswersResponse(VERIFY_NOC_ANSWERS_MESSAGE);
    }

    @PostMapping(path = APPLY_NOC_DECISION, produces = APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Apply Notice of Change decision",
        description = """
                Use to apply a Notice of Change decision on a case.
                Note that this operation acts as a callback and therefore it accepts a standard callback request,
                and similarly returns a standard callback response. As with normal callbacks, the response
                returns a 200 (success) status when valid parameters have been passed to the operation but
                processing errors have occurred.
                """)
    @ApiResponse(
        responseCode = "200",
        description = """
            Notice of Change decision applied successfully, or one of the following error conditions:
            - A decision has not yet been made on the pending Notice of Change request
            - Approval status is not recognised
            - Case must contain exactly one OrganisationPolicy with the case role
            - ChangeOrganisationRequest complex type is missing an expected field or value
            Note that the response will contain *either* `data` (for a success) or `errors` (for an error).
        """,
        content = @Content(
            schema = @Schema(implementation = ApplyNoCDecisionResponse.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                        {
                            "data": {
                                "TextField": "TextFieldValue",
                                "EmailField": "[email protected]",
                                "NumberField": "123",
                                "OrganisationPolicyField1": {
                                    "Organisation": {
                                        "OrganisationID": null,
                                        "OrganisationName": null
                                    },
                                    "OrgPolicyReference": "DefendantPolicy",
                                    "OrgPolicyCaseAssignedRole": "[Defendant]"
                                },
                                "OrganisationPolicyField2": {
                                    "Organisation": {
                                        "OrganisationID": "QUK822N",
                                        "OrganisationName": "SomeOrg"
                                    },
                                    "OrgPolicyReference": "ClaimantPolicy",
                                    "OrgPolicyCaseAssignedRole": "[Claimant]"
                                },
                                "ChangeOrganisationRequestField": {
                                    "Reason": null,
                                    "CaseRoleId": null,
                                    "NotesReason": null,
                                    "ApprovalStatus": null,
                                    "RequestTimestamp": null,
                                    "OrganisationToAdd": {
                                        "OrganisationID": null,
                                        "OrganisationName": null
                                    },
                                    "OrganisationToRemove": {
                                        "OrganisationID": null,
                                        "OrganisationName": null
                                    },
                                    "ApprovalRejectionTimestamp": null
                                }
                            }
                        }"""
                        )}))
    @ApiResponse(
        responseCode = "400",
        description = """
                One or more of the following reasons:
                - Request payload does not match the expected format
                """,
        content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "status": "BAD_REQUEST",
                        "message": "'case_details' are required.",
                        "errors": []
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public ApplyNoCDecisionResponse applyNoticeOfChangeDecision(
        @Valid @RequestBody ApplyNoCDecisionRequest applyNoCDecisionRequest) {
        ApplyNoCDecisionResponse result = new ApplyNoCDecisionResponse();
        try {
            result.setData(applyNoCDecisionService.applyNoCDecision(applyNoCDecisionRequest));
        } catch (ValidationException ex) {
            result.setErrors(singletonList(ex.getMessage()));
        }
        return result;
    }

    @PostMapping(path = NOC_PREPARE_PATH, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Prepare NoC request event", description = "Prepare NoC request event")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(
        responseCode = "200",
        description = "Data with a list of Case Roles attached to the ChangeOrganisationRequest.",
        content = @Content(
            schema = @Schema(implementation = AboutToStartCallbackResponse.class),
            mediaType = APPLICATION_JSON_VALUE
        ))
    @ApiResponse(
        responseCode = "400",
        description = "One or more of the following reasons:"
            + "\n1) " + ValidationError.JURISDICTION_CANNOT_BE_BLANK
            + "\n2) " + ValidationError.CASE_TYPE_ID_EMPTY
            + "\n3) " + ValidationError.NO_ORGANISATION_POLICY_ON_CASE_DATA
            + "\n4) " + ValidationError.NOC_REQUEST_ONGOING
            + "\n5) " + ValidationError.NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY
            + "\n6) " + ValidationError.NO_ORGANISATION_ID_IN_ANY_ORG_POLICY
            + "\n7) " + ValidationError.ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION
            + "\n8) " + ValidationError.INVALID_CASE_ROLE_FIELD
            + "\n9) " + ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID,
        content = @Content(
            schema = @Schema(implementation = ApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\n"
                    + "   \"status\": \"BAD_REQUEST\",\n"
                    + "   \"message\": \"" + ValidationError.NOC_REQUEST_ONGOING + "\",\n"
                    + "   \"errors\": [ ]\n"
                    + "}"
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public AboutToStartCallbackResponse prepareNoC(
        @Valid @RequestBody AboutToStartCallbackRequest aboutToStartCallbackRequest) {

        return AboutToStartCallbackResponse.builder()
            .data(prepareNoCService.prepareNoCRequest(aboutToStartCallbackRequest.getCaseDetails()))
            .state(aboutToStartCallbackRequest.getCaseDetails().getState())
            .securityClassification(aboutToStartCallbackRequest.getCaseDetails().getSecurityClassification())
            .dataClassification(aboutToStartCallbackRequest.getCaseDetails().getDataClassification())
            .build();
    }

    @PostMapping(path = REQUEST_NOTICE_OF_CHANGE_PATH, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Request Notice of Change Event", description = "Request Notice of Change Event")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(
        responseCode = "201",
        description = REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
    @ApiResponse(
        responseCode = "400",
        description = "One or more of the following reasons:"
            + "- Any of the `400` errors returned by the `Verify Notice of Change answers` operation\n"
            + "\n1) " + CASE_ID_INVALID
            + "\n2) " + CASE_ID_INVALID_LENGTH
            + "\n3) " + CASE_ID_EMPTY
            + "\n4) " + CHALLENGE_QUESTION_ANSWERS_EMPTY
            + "\n5) " + "Missing ChangeOrganisationRequest.CaseRoleID [APPLICANT] in the case definition",
        content = @Content(
            schema = @Schema(implementation = NoCApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\n"
                    + "   \"status\": \"BAD_REQUEST\",\n"
                    + "   \"message\": \"" + CASE_ID_INVALID + "\",\n"
                    + "   \"code\": \"" + "case-id-invalid" + "\",\n"
                    + "   \"errors\": [ ]\n"
                    + "}"
                    )}))
    @ApiResponse(
        responseCode = "404",
        description = "Case could not be found",
        content = @Content(
            schema = @Schema(implementation = NoCApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                        "message": "Case could not be found",
                        "status": "NOT_FOUND"
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public RequestNoticeOfChangeResponse requestNoticeOfChange(@Valid @RequestBody RequestNoticeOfChangeRequest
                                                                                        requestNoticeOfChangeRequest) {
        VerifyNoCAnswersRequest verifyNoCAnswersRequest
            = new VerifyNoCAnswersRequest(requestNoticeOfChangeRequest.getCaseId(),
                                          requestNoticeOfChangeRequest.getAnswers());
        NoCRequestDetails noCRequestDetails = verifyNoCAnswersService.verifyNoCAnswers(verifyNoCAnswersRequest);
        return requestNoticeOfChangeService.requestNoticeOfChange(noCRequestDetails);
    }

    @PostMapping(path = CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Check for Notice of Change Approval", description = "Check for Notice of Change Approval")
    @ApiResponse(
        responseCode = "200",
        description = StringUtils.EMPTY,
        content = @Content(
            schema = @Schema(implementation = SubmitCallbackResponse.class),
            mediaType = APPLICATION_JSON_VALUE
        ))
    @ApiResponse(
        responseCode = "400",
        description = "One or more of the following reasons:"
            + "\n1) " + CASE_ID_INVALID
            + "\n2) " + CASE_ID_INVALID_LENGTH
            + "\n3) " + CASE_ID_EMPTY
            + "\n4) " + CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
            + "\n5) " + NOC_DECISION_EVENT_UNIDENTIFIABLE,
        content = @Content(
            schema = @Schema(implementation = ApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\n"
                    + "   \"status\": \"BAD_REQUEST\",\n"
                    + "   \"message\": \"" + CASE_ID_EMPTY + "\",\n"
                    + "   \"errors\": [ ]\n"
                    + "}"
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public SubmitCallbackResponse checkNoticeOfChangeApproval(@Valid @RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        Optional<JsonNode> changeOrganisationRequestFieldJson = caseDetails.findCorNodeWithApprovalStatus();
        if (changeOrganisationRequestFieldJson.isEmpty()) {
            throw new ValidationException(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID);
        }

        ChangeOrganisationRequest changeOrganisationRequest =
            jacksonUtils.convertValue(changeOrganisationRequestFieldJson.get(), ChangeOrganisationRequest.class);
        changeOrganisationRequest.validateChangeOrganisationRequest();
        if (!changeOrganisationRequest.getApprovalStatus().equals(APPROVED.name())
            && !changeOrganisationRequest.getApprovalStatus().equals(APPROVED.getValue())) {
            return new SubmitCallbackResponse(CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE,
                CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE);
        }

        noticeOfChangeApprovalService.findAndTriggerNocDecisionEvent(caseDetails.getId());
        return new SubmitCallbackResponse(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE,
            CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE);
    }

    @PostMapping(path = SET_ORGANISATION_TO_REMOVE_PATH, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Set Organisation To Remove", description = "Set Organisation To Remove")
    @ApiResponse(
        responseCode = "200",
        description = StringUtils.EMPTY,
        content = @Content(
            schema = @Schema(implementation = AboutToSubmitCallbackResponse.class),
            mediaType = APPLICATION_JSON_VALUE
        ))
    @ApiResponse(
        responseCode = "400",
        description = "One or more of the following reasons:"
            + "\n1) " + CASE_ID_INVALID
            + "\n2) " + CASE_ID_INVALID_LENGTH
            + "\n3) " + CASE_ID_EMPTY
            + "\n4) " + CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
            + "\n5) " + INVALID_CASE_ROLE_FIELD,
        content = @Content(
            schema = @Schema(implementation = ApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\n"
                    + "   \"status\": \"BAD_REQUEST\",\n"
                    + "   \"message\": \"" + CASE_ID_EMPTY + "\",\n"
                    + "   \"errors\": [ ]\n"
                    + "}"
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public AboutToSubmitCallbackResponse setOrganisationToRemove(@Valid @RequestBody CallbackRequest callbackRequest) {
        CaseDetails caseDetails = callbackRequest.getCaseDetails();
        Optional<JsonNode> changeOrganisationRequestFieldJson = caseDetails.findChangeOrganisationRequestNode();

        if (changeOrganisationRequestFieldJson.isEmpty()) {
            throw new ValidationException(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID);
        }

        ChangeOrganisationRequest changeOrganisationRequest =
            jacksonUtils.convertValue(changeOrganisationRequestFieldJson.get(), ChangeOrganisationRequest.class);

        changeOrganisationRequest.validateChangeOrganisationRequest();

        if (changeOrganisationRequest.getOrganisationToRemove().getOrganisationID() != null
            || changeOrganisationRequest.getOrganisationToRemove().getOrganisationName() != null) {
            throw new ValidationException(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID);
        }
        String changeOrganisationKey = caseDetails.getKeyFromDataWithValue(changeOrganisationRequestFieldJson.get());

        return requestNoticeOfChangeService.setOrganisationToRemove(caseDetails,
                                                                    changeOrganisationRequest,
                                                                    changeOrganisationKey);
    }
}
