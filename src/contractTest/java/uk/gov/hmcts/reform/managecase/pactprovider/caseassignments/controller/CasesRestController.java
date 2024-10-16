package uk.gov.hmcts.reform.managecase.pactprovider.caseassignments.controller;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.GetCaseAssignmentsResponse;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class CasesRestController {

    public static final String ASSIGN_ACCESS_MESSAGE =
        "Roles %s from the organisation policies successfully assigned to the assignee.";
    public static final String GET_ASSIGNMENTS_MESSAGE = "Case-User-Role assignments returned successfully";

    private final CaseAssignmentService caseAssignmentService;

    private final ModelMapper mapper;

    public CasesRestController(final CaseAssignmentService caseAssignmentService, final ModelMapper mapper) {
        this.caseAssignmentService = caseAssignmentService;
        this.mapper = mapper;
    }

    /*
     * Handle Pact State "Case assignments exist for case Ids".
     */
    @Operation(description = "Case assignments exist for case Ids",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @GetMapping(path = "/case-assignments", produces = APPLICATION_JSON_VALUE)
    public GetCaseAssignmentsResponse getCaseAssignments(@RequestParam("case_ids") @Valid
            @NotEmpty(message = "case_ids must be a non-empty list of proper case ids.") List<String> caseIds) {

        List<CaseAssignedUsers> caseAssignedUsers = caseAssignmentService.getCaseAssignments(caseIds);
        return new GetCaseAssignmentsResponse(GET_ASSIGNMENTS_MESSAGE, caseAssignedUsers);
    }


    /*
     * Handle Pact State "Assign a user to a case".
     */
    @Operation(description = "Assign a user to a case",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", description = "Created",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @PostMapping(path = "/case-assignments", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseAssignmentResponse> assignAccessWithinOrganisation(
            @Valid @RequestBody CaseAssignmentRequest requestPayload,
            @RequestParam(name = "use_user_token", required = false) Optional<Boolean> useUserToken) {

        CaseAssignment caseAssignment = mapper.map(requestPayload, CaseAssignment.class);
        List<String> roles = caseAssignmentService.assignCaseAccess(caseAssignment, useUserToken.orElse(false));
        CaseAssignmentResponse caseAssignmentResponse = new CaseAssignmentResponse(String.format(ASSIGN_ACCESS_MESSAGE,
            StringUtils.join(roles, ',')));

        return ResponseEntity.status(HttpStatus.CREATED).body(caseAssignmentResponse);
    }
}
