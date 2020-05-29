package uk.gov.hmcts.reform.managecase.api.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentResponse;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import javax.validation.Valid;

import static uk.gov.hmcts.reform.managecase.api.controller.V1.Error.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.controller.V1.Error.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.controller.V1.MediaType.CASE_ASSIGNMENT_RESPONSE;

@RestController
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
            message = "Success"
        ),
        @ApiResponse(
            code = 400,
            message = CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = CASE_NOT_FOUND
        )
    })
    public CaseAssignmentResponse assignCaseAccess(@Valid @RequestBody CaseAssignmentRequest request) {
        CaseAssignment caseAssignment = mapper.map(request, CaseAssignment.class);
        String status = caseAssignmentService.assignCaseAccess(caseAssignment);
        return new CaseAssignmentResponse(status);
    }
}
