package uk.gov.hmcts.reform.managecase.api.controller;

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
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersResponse;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.service.NoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.ApplyNoCDecisionService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Validated
@RequestMapping(path = "/noc")
public class NoticeOfChangeController {

    @SuppressWarnings({"squid:S1075"})
    public static final String GET_NOC_QUESTIONS = "/noc-questions";
    public static final String VERIFY_NOC_ANSWERS = "/verify-noc-answers";
    public static final String APPLY_NOC_DECISION = "/apply-decision";

    public static final String VERIFY_NOC_ANSWERS_MESSAGE = "Notice of Change answers verified successfully";

    private final NoticeOfChangeService noticeOfChangeService;
    private final VerifyNoCAnswersService verifyNoCAnswersService;
    private final ApplyNoCDecisionService applyNoCDecisionService;

    public NoticeOfChangeController(NoticeOfChangeService noticeOfChangeService,
                                    VerifyNoCAnswersService verifyNoCAnswersService,
                                    ApplyNoCDecisionService applyNoCDecisionService) {
        this.noticeOfChangeService = noticeOfChangeService;
        this.verifyNoCAnswersService = verifyNoCAnswersService;
        this.applyNoCDecisionService = applyNoCDecisionService;
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
                + "1. " + "case_id must be not be empty" + ", \n"
                + "2. " + "on going NoC request in progress, \n"
                + "3. " + "no NoC events available for this case id, \n",
            examples = @Example({
                @ExampleProperty(
                    value = "{\"message\": \"case_id must be not be empty\","
                        + " \"status\": \"BAD_REQUEST\" }",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 404,
            message = "case_id not found",
            examples = @Example({
                @ExampleProperty(
                    value = "{\"message\": \"case_id not found\","
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
                                                               @Valid @NotEmpty(message = "case_id must "
        + "be not be empty") String caseId) {
        validateCaseIds(caseId);
        return noticeOfChangeService.getChallengeQuestions(caseId);
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
                + "- The number of submitted answers does not match the number of questions\n"
                + "- No answer has been provided for an expected question ID\n"
                + "- The submitted answers do not match any litigant\n"
                + "- The submitted answers do not uniquely identify a litigant\n"
                + "- No organisation policy exists on the case for the identified case role\n"
                + "- The submitted answers identify a litigant that the requestor is already representing\n",
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
            + "Note that this operation acts as a callback and therefore it accepts a standard callback request "
            + "(although only `case_details` is required), and similarly returns a standard callback response. "
            + "As with normal callbacks, the response returns a 200 (success) status when valid parameters "
            + "have been passed to the operation but processing errors have occurred."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Notice of Change decision applied successfully, or one of the following error conditions:\n"
                + "- A decision has not yet been made on the pending Notice of Change request\n"
                + "- Approval status is not recognised\n"
                + "- Case must contain exactly one OrganisationPolicy with the case role\n"
                + "- ChangeOrganisationRequest complex type is missing an expected field or value",
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

    private void validateCaseIds(String caseId) {
        if (!StringUtils.isNumeric(caseId)) {
            throw new ValidationException("Case ID should contain digits only");
        }
    }
}
