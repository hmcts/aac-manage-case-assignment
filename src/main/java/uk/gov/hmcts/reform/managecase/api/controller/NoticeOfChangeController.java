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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.service.NoticeOfChangeQuestions;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Validated
@RequestMapping(path = "/noc")
public class NoticeOfChangeController {

    @SuppressWarnings({"squid:S1075"})
    public static final String GET_NOC_QUESTIONS = "/noc-questions";

    private final NoticeOfChangeQuestions noticeOfChangeQuestions;

    public NoticeOfChangeController(NoticeOfChangeQuestions noticeOfChangeQuestions) {
        this.noticeOfChangeQuestions = noticeOfChangeQuestions;
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
                + "1. " + "case_id must not be empty" + ", \n"
                + "2. " + "ongoing NoC request in progress, \n"
                + "3. " + "no NoC events available for this case id, \n",
            examples = @Example({
                @ExampleProperty(
                    value = "{\"message\": \"case_id must not be empty\","
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
        + "not be empty") String caseId) {
        validateCaseIds(caseId);
        return noticeOfChangeQuestions.getChallengeQuestions(caseId);
    }

    private void validateCaseIds(String caseId) {
        if (!StringUtils.isNumeric(caseId)) {
            throw new ValidationException("Case ID should contain digits only");
        }
    }
}
