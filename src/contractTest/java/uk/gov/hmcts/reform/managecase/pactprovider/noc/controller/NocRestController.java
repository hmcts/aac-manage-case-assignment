package uk.gov.hmcts.reform.managecase.pactprovider.noc.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.hibernate.validator.constraints.LuhnCheck;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

// **** WORK IN PROGRESS ****
// See NoticeOfChangeController.java

@RestController
@Validated
@RequestMapping(path = "/noc")
public class NocRestController {

    public static final String GET_NOC_QUESTIONS_METHOD_2 = "/noc-questions";               // Uses innjected mock
    public static final String GET_NOC_QUESTIONS_METHOD_1 = "/noc-questions-DEACTIVATED";   // Uses hard coded JSON

    private final NoticeOfChangeQuestions noticeOfChangeQuestions;

    public NocRestController(final NoticeOfChangeQuestions noticeOfChangeQuestions) {
        this.noticeOfChangeQuestions = noticeOfChangeQuestions;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * Handle Pact States (using METHOD TWO , injected mock) :-
     *   "a case with id 1234567890123456 exists"  (response 200)
     *   "a case with an error exists"             (response 400)
     */
    @GetMapping(path = GET_NOC_QUESTIONS_METHOD_2, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get Notice of Change questions", notes = "Get Notice of Change questions")
    @ResponseStatus(HttpStatus.OK)
    @io.swagger.annotations.ApiResponses({
        @io.swagger.annotations.ApiResponse(
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
            })),
        @io.swagger.annotations.ApiResponse(
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
                    value = "{\"message\": \"Case ID has to be a valid 16-digit Luhn number\","
                        + " \"code\": \"case-id-invalid\","
                        + " \"status\": \"BAD_REQUEST\" }",
                    mediaType = APPLICATION_JSON_VALUE)
            })),
        @io.swagger.annotations.ApiResponse(
            code = 404,
            message = "Case could not be found",
            examples = @Example({
                @ExampleProperty(
                    value = "{\"message\": \"Case could not be found\","
                        + " \"status\": \"NOT_FOUND\" }",
                    mediaType = APPLICATION_JSON_VALUE)
            })),
        @io.swagger.annotations.ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID),
        @io.swagger.annotations.ApiResponse(
            code = 403,
            message = AuthError.UNAUTHORISED_S2S_SERVICE)
    })
    public ResponseEntity<String> getNoticeOfChangeQuestionsMethod2(@RequestParam("case_id")
                                                                               @Valid
                                                                               @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
                                                                               @Size(min = 16, max = 16, message =
                                                                                   NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
                                                                               @LuhnCheck(message =
                                                                                   NoCValidationError.NOC_CASE_ID_INVALID,
                                                                                   ignoreNonDigitCharacters = false)
                                                                               String caseId) throws JsonProcessingException {
        System.out.println("********** JCDEBUG: METHOD TWO: caseId = " + caseId);

        ChallengeQuestionsResult challengeQuestionsResult = noticeOfChangeQuestions.getChallengeQuestions(caseId);
        if (challengeQuestionsResult != null) {
            System.out.println(objectMapper.writeValueAsString(challengeQuestionsResult));
            return ResponseEntity.status(HttpStatus.OK).body(objectMapper.writeValueAsString(challengeQuestionsResult));
        } else {
            String jsonString = "{\"status\":\"BAD_REQUEST\",\"message\":\"Case ID has to be a valid 16-digit Luhn number\",\"code\":\"case-id-invalid\"}";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonString);
        }
    }


    /*
     * Handle Pact States (using METHOD ONE , hard coded JSON) :-
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
    @GetMapping(path = GET_NOC_QUESTIONS_METHOD_1, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getNoticeOfChangeQuestionsMethod1(@RequestParam("case_id")
                                                             @Valid
                                                             @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
                                                             @Size(min = 16, max = 16, message =
                                                                     NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
                                                             @LuhnCheck(message =
                                                                     NoCValidationError.NOC_CASE_ID_INVALID,
                                                                     ignoreNonDigitCharacters = false)
                                                                     String caseId) {

        System.out.println("********** JCDEBUG: METHOD ONE: caseId = " + caseId);

        if ("1234567890123456".equals(caseId)) {
            System.out.println("**** JCDEBUG: Returning OK");
            String jsonString = "{\"questions\":[{\"case_type_id\":\"Probate\",\"order\":\"1\",\"question_text\":\"What is their Email?\",\"answer_field_type\":{\"id\":\"Email\",\"type\":\"Email\",\"min\":0,\"max\":10,\"regular_expression\":\"asdsa\",\"collection_field_type\":\"type\"},\"display_context_parameter\":\"1\",\"challenge_question_id\":\"NoC\",\"answer_field\":\"\",\"question_id\":\"QuestionId67745\"}]}";
            return ResponseEntity.status(HttpStatus.OK).body(jsonString);
        } else {
            System.out.println("**** JCDEBUG: Returning BAD_REQUEST");
            String jsonString = "{\"status\":\"BAD_REQUEST\",\"message\":\"Case ID has to be a valid 16-digit Luhn number\",\"code\":\"case-id-invalid\"}";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonString);
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
    @PostMapping(path = "/noc-requests", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> requestNoticeOfChange(@Valid @RequestBody RequestNoticeOfChangeRequest
                                                                requestNoticeOfChangeRequest) {

        String jsonString = "{\"approval_status\":\"APPROVED\",\"case_role\":\"[Claimant]\",\"status_message\":\"Notice of request has been successfully submitted.\"}";
        return ResponseEntity.status(HttpStatus.CREATED).body(jsonString);
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
    @PostMapping(path = "/verify-noc-answers", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> verifyNoticeOfChangeAnswers(@Valid @RequestBody VerifyNoCAnswersRequest verifyNoCAnswersRequest) {

        String jsonString = "{\"organisation\":{\"OrganisationID\":\"QUK822NA\",\"OrganisationName\":\"Some Org\"},\"status_message\":\"Notice of Change answers verified successfully\"}";
        return ResponseEntity.status(HttpStatus.CREATED).body(jsonString);
    }
}
