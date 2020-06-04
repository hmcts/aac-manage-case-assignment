package uk.gov.hmcts.reform.managecase.api.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.modelmapper.ModelMapper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentResponse;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import javax.validation.Valid;

import static uk.gov.hmcts.reform.managecase.api.controller.V1.MediaType.CASE_ASSIGNMENT_RESPONSE;

@RestController
@Validated
public class CaseAssignmentController {

    private final CaseAssignmentService caseAssignmentService;
    private final ModelMapper mapper;

    public CaseAssignmentController(CaseAssignmentService caseAssignmentService, ModelMapper mapper) {
        this.caseAssignmentService = caseAssignmentService;
        this.mapper = mapper;
    }

    @PutMapping(path = "/case-assignments", produces = CASE_ASSIGNMENT_RESPONSE)
    @ApiOperation(value = "Assign access to a case", notes = "Assign Access within Organisation")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Role from the organisation policy successfully assigned to the assignee."
        ),
        @ApiResponse(
            code = 400,
            message = "One of the following reasons.\n"
                + "1. Case ID is not valid \n"
                + "2. Case type ID is not valid \n"
                + "3. Assignee IDAM ID is not valid \n"
                + "4. Intended assignee has to be in the same organisation as that of the invoker. \n"
                + "5. Case ID has to be one for which a case role is represented by the invoker's organisation.",
            examples = @Example({@ExampleProperty(
                value = "{\"errorCode\": \"E001\" }",
                mediaType = CASE_ASSIGNMENT_RESPONSE
            )})
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
    public CaseAssignmentResponse assignCaseAccess(@Valid @RequestBody CaseAssignmentRequest requestPayload) {
        CaseAssignment caseAssignment = mapper.map(requestPayload, CaseAssignment.class);
        String role = caseAssignmentService.assignCaseAccess(caseAssignment);
        return new CaseAssignmentResponse(role);
    }
}
