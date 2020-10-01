package uk.gov.hmcts.reform.managecase.api.controller;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.GetCaseAssignmentsResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersResponse;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.service.NoticeOfChangeService;

import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Validated
@ConditionalOnProperty(value = "mca.conditional-apis.case-assignments.enabled", havingValue = "true")
@RequestMapping(path = "/noc")
@Api(value = "/noc")
public class NoticeOfChangeController {

    @SuppressWarnings({"squid:S1075"})
    public static final String GET_NOC_QUESTIONS = "/noc-questions";
    public static final String VERIFY_NOC_ANSWERS = "/verify-noc-answers";

    public static final String VERIFY_NOC_ANSWERS_MESSAGE = "Notice of Change answers verified successfully";

    public static final String REQUEST_NOTICE_OF_CHANGE_PATH = "/noc-requests";
    public static final String REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE = "The Notice of Change request has been successfully submitted.";

    private final NoticeOfChangeService noticeOfChangeService;
    private final ModelMapper mapper;

    public NoticeOfChangeController(NoticeOfChangeService noticeOfChangeService, ModelMapper mapper) {
        this.noticeOfChangeService = noticeOfChangeService;
        this.mapper = mapper;
    }

    @GetMapping(path = GET_NOC_QUESTIONS, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get Notice of Change questions", notes = "Get Notice of Change questions")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(
                    code = 200,
                    message = "Case-User-Role assignments returned successfully.",
                    response = GetCaseAssignmentsResponse.class,
                    examples = @Example({
                            @ExampleProperty(
                                    value = "{\n"
                                            + "  \"status_message\": \"Case-User-Role assignments returned "
                                            + "successfully\",\n"
                                            + "  \"case_assignments\": [\n"
                                            + "    {\n"
                                            + "      \"case_id\": \"1588234985453946\",\n"
                                            + "      \"shared_with\": [\n"
                                            + "        {\n"
                                            + "          \"idam_id\": \"221a2877-e1ab-4dc4-a9ff-f9424ad58738\",\n"
                                            + "          \"first_name\": \"Bill\",\n"
                                            + "          \"last_name\": \"Roberts\",\n"
                                            + "          \"email\": \"bill.roberts@greatbrsolicitors.co.uk\",\n"
                                            + "          \"case_roles\": [\n"
                                            + "            \"[Claimant]\",\n"
                                            + "            \"[Defendant]\"\n"
                                            + "          ]\n"
                                            + "        }\n"
                                            + "      ]\n"
                                            + "    }    \n"
                                            + "  ]\n"
                                            + "}",
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
            @Valid @NotEmpty(message = "case_id must be not be empty") String caseId) {
        validateCaseIds(caseId);
        ChallengeQuestionsResult challengeQuestion = noticeOfChangeService.getChallengeQuestions(caseId);
        return challengeQuestion;
    }

    // TODO: Swagger
    @PostMapping(path = VERIFY_NOC_ANSWERS, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Verify Notice of Change answers")
    public VerifyNoCAnswersResponse verifyNoticeOfChangeAnswers(
        @Valid @RequestBody VerifyNoCAnswersRequest verifyNoCAnswersRequest) {
        NoCRequestDetails result = noticeOfChangeService.verifyNoticeOfChangeAnswers(verifyNoCAnswersRequest);
        return result.toVerifyNoCAnswersResponse(VERIFY_NOC_ANSWERS_MESSAGE);
    }

    private void validateCaseIds(String caseId) {
        if (!StringUtils.isNumeric(caseId)) {
            throw new ValidationException("Case ID should contain digits only");
        }
    }

    @GetMapping(path = REQUEST_NOTICE_OF_CHANGE_PATH, produces = APPLICATION_JSON_VALUE)
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
                + "\n1) " + ValidationError.CASE_ID_INVALID
                + "\n2) " + ValidationError.CASE_ID_INVALID_LENGTH
                + "\n3) " + ValidationError.CASE_ID_EMPTY,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"" + ValidationError.CASE_ID_EMPTY + "\",\n"
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
    public RequestNoticeOfChangeResponse requestNoticeOfChange(@RequestBody RequestNoticeOfChangeRequest noticeOfChangeRequest) {
        //noticeOfChangeService.callDansQuestionsMethod()
        final OrganisationPolicy organisationPolicy = null;
        final Organisation organisation = null;
        return noticeOfChangeService.requestNoticeOfChange(noticeOfChangeRequest.getCaseId(), organisation, organisationPolicy);
    }

}
