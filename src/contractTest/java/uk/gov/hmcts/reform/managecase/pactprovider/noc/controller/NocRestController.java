package uk.gov.hmcts.reform.managecase.pactprovider.noc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.hibernate.validator.constraints.LuhnCheck;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersResponse;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;
import org.json.JSONObject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class NocRestController {

    private static final String VERIFY_NOC_ANSWERS_MESSAGE = "Notice of Change answers verified successfully";

    private final NoticeOfChangeQuestions noticeOfChangeQuestions;

    private final VerifyNoCAnswersService verifyNoCAnswersService;

    private final RequestNoticeOfChangeService requestNoticeOfChangeService;

    public NocRestController(final NoticeOfChangeQuestions noticeOfChangeQuestions,
                             final VerifyNoCAnswersService verifyNoCAnswersService,
                             final RequestNoticeOfChangeService requestNoticeOfChangeService) {
        this.noticeOfChangeQuestions = noticeOfChangeQuestions;
        this.verifyNoCAnswersService = verifyNoCAnswersService;
        this.requestNoticeOfChangeService = requestNoticeOfChangeService;
    }

    /*
     * Handle Pact States :-
     *   "a case with id 1234567890123456 exists"  (response 200)
     *   "a case with an error exists"             (response 400)
     */
    @Operation(description = "a case with id 1234567890123456 exists  ,  and a case with an error exists",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            }),
        @ApiResponse(
            responseCode = "400", description = "Bad request",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @GetMapping(path = "/noc/noc-questions", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getNoticeOfChangeQuestions(@RequestParam("case_id")
                                                     @Valid
                                                     @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
                                                     @Size(min = 16, max = 16, message =
                                                         NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
                                                     @LuhnCheck(message =
                                                         NoCValidationError.NOC_CASE_ID_INVALID,
                                                         ignoreNonDigitCharacters = false)
                                                     String caseId) {
        ChallengeQuestionsResult challengeQuestionsResult = noticeOfChangeQuestions.getChallengeQuestions(caseId);
        if (challengeQuestionsResult != null) {
            return ResponseEntity.status(HttpStatus.OK).body(challengeQuestionsResult);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(badRequest().toString());
        }
    }


    /*
     * Handle Pact States :-
     *   "A valid submit NoC event is requested"      (response 201)
     *   "A NoC answer request with invalid case ID"  (response 400)
     */
    @Operation(description = "A valid submit NoC event is requested  ,  and A NoC answer request with invalid case ID",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", description = "Created",
            content = {
                @Content(mediaType = "application/json")
            }),
        @ApiResponse(
            responseCode = "400", description = "Bad request",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @PostMapping(path = "/noc/noc-requests", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity requestNoticeOfChange(@RequestBody RequestNoticeOfChangeRequest
                                                requestNoticeOfChangeRequest) {

        VerifyNoCAnswersRequest verifyNoCAnswersRequest = new VerifyNoCAnswersRequest(
            requestNoticeOfChangeRequest.getCaseId(),
            requestNoticeOfChangeRequest.getAnswers());
        NoCRequestDetails noCRequestDetails = verifyNoCAnswersService.verifyNoCAnswers(verifyNoCAnswersRequest);
        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse = requestNoticeOfChangeService
            .requestNoticeOfChange(noCRequestDetails);
        if (requestNoticeOfChangeResponse != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(requestNoticeOfChangeResponse);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(badRequest().toString());
        }
    }


    /*
     * Handle Pact States :-
     *   "A valid NoC answers verification request"  (response 201)
     *   "An invalid NoC answer request"             (response 400)
     */
    @Operation(description = "A valid NoC answers verification request  ,  and An invalid NoC answer request",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", description = "Created",
            content = {
                @Content(mediaType = "application/json")
            }),
        @ApiResponse(
            responseCode = "400", description = "Bad request",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @PostMapping(path = "/noc/verify-noc-answers", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity verifyNoticeOfChangeAnswers(@RequestBody VerifyNoCAnswersRequest verifyNoCAnswersRequest) {

        NoCRequestDetails result = verifyNoCAnswersService.verifyNoCAnswers(verifyNoCAnswersRequest);
        VerifyNoCAnswersResponse verifyNoCAnswersResponse = result.toVerifyNoCAnswersResponse(
            VERIFY_NOC_ANSWERS_MESSAGE);
        if (verifyNoCAnswersResponse != null) {
            return ResponseEntity.status(HttpStatus.OK).body(verifyNoCAnswersResponse);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(badRequest().toString());
        }
    }

    private JSONObject badRequest() {
        return new JSONObject() {{
                put("status", "");
                put("message", "");
                put("code", "");
                put("errors", new String[]{});
            }
        };
    }
}
