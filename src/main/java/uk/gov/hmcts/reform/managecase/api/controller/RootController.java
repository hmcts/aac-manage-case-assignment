package uk.gov.hmcts.reform.managecase.api.controller;

import org.hibernate.validator.constraints.LuhnCheck;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import static org.springframework.http.ResponseEntity.ok;

/**
 * Default endpoints per application.
 */
@RestController
public class RootController {

    /**
     * Root GET endpoint.
     *
     * <p>Azure application service has a hidden feature of making requests to root endpoint when
     * "Always On" is turned on.
     * This is the endpoint to deal with that and therefore silence the unnecessary 404s as a response code.
     *
     * @return Welcome message from the service.
     */
    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to manage-case-assignment ");
    }

    // Test endpoint with no parameters.
    @GetMapping("/jctest1")
    public ResponseEntity<String> jctest1() {
        return ok("JCTEST1 OK");
    }

    // Test endpoint with case_id parameter (No validation).
    @GetMapping("/jctest2")
    public ResponseEntity<String> jctest2(@RequestParam("case_id")
                                          String caseId) {
        return ok("JCTEST2 OK: caseId: " + caseId);
    }

    // Test endpoint with case_id parameter (and validation).
    @GetMapping("/jctest3")
    public ResponseEntity<String> jctest3(@RequestParam("case_id")
                                          @Valid
                                          @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
                                          @Size(min = 16, max = 16, message =
                                                  NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
                                          @LuhnCheck(message =
                                                  NoCValidationError.NOC_CASE_ID_INVALID,
                                                  ignoreNonDigitCharacters = false)
                                          String caseId) {
        return ok("JCTEST3 OK: caseId: " + caseId);
    }
}
