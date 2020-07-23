package uk.gov.hmcts.reform.managecase.api.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentResponse;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Validated
@ConditionalOnProperty(value = "mca.conditional-apis.case-assignments.enabled", havingValue = "true")
public class CaseAssignmentController {

    public static final String MESSAGE = "Role %s from the organisation policy successfully assigned to the assignee.";

    private final CaseAssignmentService caseAssignmentService;
    private final ModelMapper mapper;

    public CaseAssignmentController(CaseAssignmentService caseAssignmentService, ModelMapper mapper) {
        this.caseAssignmentService = caseAssignmentService;
        this.mapper = mapper;
    }

    @PostMapping(path = "/case-assignments", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Assign Access within Organisation", notes = "Assign Access within Organisation")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = "Role from the organisation policy successfully assigned to the assignee."
        ),
        @ApiResponse(
            code = 400,
            message = "One of the following reasons.\n"
                + "1. Case ID can not be empty \n"
                + "2. Case type ID can not be empty \n"
                + "3. Assignee IDAM ID can not be empty \n"
                + "4. Intended assignee has to be in the same organisation as that of the invoker. \n"
                + "5. Case ID has to be one for which a case role is represented by the invoker's organisation. \n"
                + "6. Case ID has to be for an existing case accessible by the invoker. \n"
                + "7. Intended assignee has to be a solicitor enabled in the jurisdiction of the case. \n",
            examples = @Example({
                    @ExampleProperty(
                            value = "{\"message\": \"Intended assignee has to be in the same organisation of invoker\","
                                    + " \"status\": \"BAD_REQUEST\" }",
                            mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = "Authentication failure due to invalid / expired tokens (IDAM / S2S)."
        ),
        @ApiResponse(
            code = 403,
            message = "One of the following reasons.\n"
                + "1) UnAuthorised S2S service \n"
                + "2) The user is neither a case access administrator nor a solicitor with access to the jurisdiction"
        )
    })
    public CaseAssignmentResponse assignAccessWithinOrganisation(
            @Valid @RequestBody CaseAssignmentRequest requestPayload) {
        CaseAssignment caseAssignment = mapper.map(requestPayload, CaseAssignment.class);
        String role = caseAssignmentService.assignCaseAccess(caseAssignment);
        return new CaseAssignmentResponse(String.format(MESSAGE, role));
    }
}
