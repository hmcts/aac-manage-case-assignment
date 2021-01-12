package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
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

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
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
    @ApiOperation(value = "Get Notice of Change questions", notes = "Get Notice of Change questions")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Challenge questions returned successfully.",
            response = ChallengeQuestionsResult.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\"questions\":["
                        + "{\"case_type_id\":\"caseType\","
                        + "\"order\":1,"
                        + "\"question_text\":\"questionText\","
                        + "\"answer_field_type\""
                        + ":{\"id\":\"Number\","
                        + "\"type\":\"Number\","
                        + "\"min\":null,"
                        + "\"max\":null,"
                        + "\"regular_expression\":null,"
                        + "\"fixed_list_items\":[],"
                        + "\"complex_fields\":[],"
                        + "\"collection_field_type\":null"
                        + "},"
                        + "\"display_context_parameter\":null,"
                        + "\"challenge_question_id\":\"NoC\","
                        + "\"answer_field\":null,"
                        + "\"question_id\":\"QuestionId1\"}]}\n",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:\n"
                + "1. " + "Case ID can not be empty" + ", \n"
                + "2. " + "Case ID has to be a valid 16-digit Luhn number" + ", \n"
                + "3. " + "No NoC events available for this case type" + ", \n"
                + "4. " + "Multiple NoC Request events found for the user" + ", \n"
                + "5. " + "More than one change request found on the case" + ", \n"
                + "6. " + "Ongoing NoC request in progress \n"
                + "7. " + "Insufficient privileges for notice of change request \n"
                + "8. " + "No Organisation Policy for one or more of the roles available"
                + " for the notice of change request \n",
            examples = @Example({
                @ExampleProperty(
                    value = "{\"message\": \"Case ID can not be empty\","
                        + " \"status\": \"BAD_REQUEST\" }",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 404,
            message = "Case could not be found",
            examples = @Example({
                @ExampleProperty(
                    value = "{\"message\": \"Case could not be found\","
                        + " \"status\": \"NOT_FOUND\" }",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
    public ChallengeQuestionsResult getNoticeOfChangeQuestions(@RequestParam("case_id")
                                                               @Valid @NotEmpty(message = CASE_ID_EMPTY)
                                                                       String caseId) {
        validateCaseIds(caseId);
        return noticeOfChangeQuestions.getChallengeQuestions(caseId);
    }

    @PostMapping(path = VERIFY_NOC_ANSWERS, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(
        value = "Verify Notice of Change answers",
        notes = "Use to validate the answers provided by a user wishing to raise a "
            + "Notice of Change Request to gain access to a case"
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Notice of Change answers verified successfully.",
            response = VerifyNoCAnswersResponse.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "    \"organisation\": {\n"
                        + "        \"OrganisationID\": \"QUK822NA\",\n"
                        + "        \"OrganisationName\": \"Some Org\"\n"
                        + "    },\n"
                        + "    \"status_message\": \"Notice of Change answers verified successfully\"\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:\n"
                + "- Any of the `400` errors returned by the `Get Notice of Change questions` operation\n"
                + "- Challenge question answers can not be empty \n"
                + "- The number of provided answers must match the number of questions - "
                + "expected %s answers, received %s\n"
                + "- The answers did not match those for any litigant \n"
                + "- The answers did not uniquely identify a litigant \n"
                + "- No answer has been provided for question ID '%s' \n"
                + "- No OrganisationPolicy exists on the case for the case role '%s' \n"
                + "- The requestor has answered questions uniquely identifying a litigant that"
                + " they are already representing \n",
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "    \"status\": \"BAD_REQUEST\",\n"
                        + "    \"message\": \"The answers did not match those for any litigant\",\n"
                        + "    \"errors\": []\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
    public VerifyNoCAnswersResponse verifyNoticeOfChangeAnswers(
        @Valid @RequestBody VerifyNoCAnswersRequest verifyNoCAnswersRequest) {
        NoCRequestDetails result = verifyNoCAnswersService.verifyNoCAnswers(verifyNoCAnswersRequest);
        return result.toVerifyNoCAnswersResponse(VERIFY_NOC_ANSWERS_MESSAGE);
    }

    @PostMapping(path = APPLY_NOC_DECISION, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(
        value = "Apply Notice of Change decision",
        notes = "Use to apply a Notice of Change decision on a case.\n\n"
            + "Note that this operation acts as a callback and therefore it accepts a standard callback request, "
            + "and similarly returns a standard callback response. As with normal callbacks, the response "
            + "returns a 200 (success) status when valid parameters have been passed to the operation but "
            + "processing errors have occurred."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Notice of Change decision applied successfully, or one of the following error conditions:\n"
                + "- A decision has not yet been made on the pending Notice of Change request\n"
                + "- Approval status is not recognised\n"
                + "- Case must contain exactly one OrganisationPolicy with the case role\n"
                + "- ChangeOrganisationRequest complex type is missing an expected field or value\n\n"
                + "Note that the response will contain *either* `data` (for a success) or `errors` (for an error).",
            response = ApplyNoCDecisionResponse.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "    \"data\": {\n"
                        + "        \"TextField\": \"TextFieldValue\",\n"
                        + "        \"EmailField\": \"aca72@gmail.com\",\n"
                        + "        \"NumberField\": \"123\",\n"
                        + "        \"OrganisationPolicyField1\": {\n"
                        + "            \"Organisation\": {\n"
                        + "                \"OrganisationID\": null,\n"
                        + "                \"OrganisationName\": null\n"
                        + "            },\n"
                        + "            \"OrgPolicyReference\": \"DefendantPolicy\",\n"
                        + "            \"OrgPolicyCaseAssignedRole\": \"[Defendant]\"\n"
                        + "        },\n"
                        + "        \"OrganisationPolicyField2\": {\n"
                        + "            \"Organisation\": {\n"
                        + "                \"OrganisationID\": \"QUK822N\",\n"
                        + "                \"OrganisationName\": \"SomeOrg\"\n"
                        + "            },\n"
                        + "            \"OrgPolicyReference\": \"ClaimantPolicy\",\n"
                        + "            \"OrgPolicyCaseAssignedRole\": \"[Claimant]\"\n"
                        + "        },\n"
                        + "        \"ChangeOrganisationRequestField\": {\n"
                        + "            \"Reason\": null,\n"
                        + "            \"CaseRoleId\": null,\n"
                        + "            \"NotesReason\": null,\n"
                        + "            \"ApprovalStatus\": null,\n"
                        + "            \"RequestTimestamp\": null,\n"
                        + "            \"OrganisationToAdd\": {\n"
                        + "                \"OrganisationID\": null,\n"
                        + "                \"OrganisationName\": null\n"
                        + "            },\n"
                        + "            \"OrganisationToRemove\": {\n"
                        + "                \"OrganisationID\": null,\n"
                        + "                \"OrganisationName\": null\n"
                        + "            },\n"
                        + "            \"ApprovalRejectionTimestamp\": null\n"
                        + "        }\n"
                        + "    }\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:\n"
                + "- Request payload does not match the expected format\n",
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "    \"status\": \"BAD_REQUEST\",\n"
                        + "    \"message\": \"'case_details' are required.\",\n"
                        + "    \"errors\": []\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
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
    @ApiOperation(value = "Prepare NoC request event", notes = "Prepare NoC request event")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
        @ApiResponse(
            code = 200,
            response = AboutToStartCallbackResponse.class,
            message = "Data with a list of Case Roles attached to the ChangeOrganisationRequest."
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + ValidationError.JURISDICTION_CANNOT_BE_BLANK
                + "\n2) " + ValidationError.CASE_TYPE_ID_EMPTY
                + "\n3) " + ValidationError.NO_ORGANISATION_POLICY_ON_CASE_DATA
                + "\n4) " + ValidationError.NOC_REQUEST_ONGOING
                + "\n5) " + ValidationError.NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY
                + "\n6) " + ValidationError.NO_ORGANISATION_ID_IN_ANY_ORG_POLICY
                + "\n7) " + ValidationError.ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION
                + "\n8) " + ValidationError.INVALID_CASE_ROLE_FIELD
                + "\n9) " + ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"" + ValidationError.NOC_REQUEST_ONGOING + "\",\n"
                        + "   \"errors\": [ ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
    public AboutToStartCallbackResponse prepareNoC(
        @Valid @RequestBody AboutToStartCallbackRequest aboutToStartCallbackRequest) {

        return AboutToStartCallbackResponse.builder()
            .data(prepareNoCService.prepareNoCRequest(aboutToStartCallbackRequest.getCaseDetails()))
            .state(aboutToStartCallbackRequest.getCaseDetails().getState())
            .securityClassification(aboutToStartCallbackRequest.getCaseDetails().getSecurityClassification())
            .dataClassification(aboutToStartCallbackRequest.getCaseDetails().getDataClassification())
            .build();
    }

    private void validateCaseIds(String caseId) {
        if (!StringUtils.isNumeric(caseId)) {
            throw new ValidationException("Case ID should contain digits only");
        }
    }

    @PostMapping(path = REQUEST_NOTICE_OF_CHANGE_PATH, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Request Notice of Change Event", notes = "Request Notice of Change Event")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + CASE_ID_INVALID
                + "\n2) " + CASE_ID_INVALID_LENGTH
                + "\n3) " + CASE_ID_EMPTY,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"" + CASE_ID_EMPTY + "\",\n"
                        + "   \"errors\": [ ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
    public RequestNoticeOfChangeResponse requestNoticeOfChange(@Valid @RequestBody RequestNoticeOfChangeRequest
                                                                                        requestNoticeOfChangeRequest) {
        VerifyNoCAnswersRequest verifyNoCAnswersRequest
            = new VerifyNoCAnswersRequest(requestNoticeOfChangeRequest.getCaseId(),
                                          requestNoticeOfChangeRequest.getAnswers());
        NoCRequestDetails noCRequestDetails = verifyNoCAnswersService.verifyNoCAnswers(verifyNoCAnswersRequest);
        return requestNoticeOfChangeService.requestNoticeOfChange(noCRequestDetails);
    }

    @PostMapping(path = CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Check for Notice of Change Approval", notes = "Check for Notice of Change Approval")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = StringUtils.EMPTY,
            response = SubmitCallbackResponse.class
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + CASE_ID_INVALID
                + "\n2) " + CASE_ID_INVALID_LENGTH
                + "\n3) " + CASE_ID_EMPTY
                + "\n4) " + CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
                + "\n5) " + NOC_DECISION_EVENT_UNIDENTIFIABLE,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"" + CASE_ID_EMPTY + "\",\n"
                        + "   \"errors\": [ ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
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
    @ApiOperation(value = "Set Organisation To Remove", notes = "Set Organisation To Remove")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = StringUtils.EMPTY,
            response = AboutToSubmitCallbackResponse.class
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + CASE_ID_INVALID
                + "\n2) " + CASE_ID_INVALID_LENGTH
                + "\n3) " + CASE_ID_EMPTY
                + "\n4) " + CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
                + "\n5) " + INVALID_CASE_ROLE_FIELD,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"" + CASE_ID_EMPTY + "\",\n"
                        + "   \"errors\": [ ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
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
