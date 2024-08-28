package uk.gov.hmcts.reform.managecase.api.controller;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;

import org.modelmapper.ModelMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.CaseUnassignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseUnassignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.GetCaseAssignmentsResponse;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Validated
@ConditionalOnProperty(value = "mca.conditional-apis.case-assignments.enabled", havingValue = "true")
public class CaseAssignmentController {

    @SuppressWarnings({"squid:S1075"})
    public static final String CASE_ASSIGNMENTS_PATH = "/case-assignments";

    public static final String ASSIGN_ACCESS_MESSAGE =
            "Roles %s from the organisation policies successfully assigned to the assignee.";
    public static final String UNASSIGN_ACCESS_MESSAGE = "Unassignment(s) performed successfully.";
    public static final String GET_ASSIGNMENTS_MESSAGE = "Case-User-Role assignments returned successfully";

    private final CaseAssignmentService caseAssignmentService;
    private final ModelMapper mapper;

    public CaseAssignmentController(CaseAssignmentService caseAssignmentService, ModelMapper mapper) {
        this.caseAssignmentService = caseAssignmentService;
        this.mapper = mapper;
    }

    @PostMapping(path = CASE_ASSIGNMENTS_PATH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Assign Access within Organisation", description = "Assign Access within Organisation")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(
        responseCode = "201",
        description = "Role from the organisation policy successfully assigned to the assignee.")
    @ApiResponse(
        responseCode = "400",
        description = "One or more of the following reasons:"
            + "\n1) " + ValidationError.CASE_ID_INVALID
            + "\n2) " + ValidationError.CASE_ID_INVALID_LENGTH
            + "\n3) " + ValidationError.CASE_ID_EMPTY
            + "\n4) " + ValidationError.CASE_TYPE_ID_EMPTY
            + "\n5) " + ValidationError.ASSIGNEE_ID_EMPTY
            + "\n6) " + ValidationError.ASSIGNEE_ORGANISATION_ERROR
            + "\n7) " + ValidationError.ORGANISATION_POLICY_ERROR
            + "\n8) " + ValidationError.ASSIGNEE_ROLE_ERROR,
        content = @Content(
            schema = @Schema(implementation = ApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\n"
                    + "   \"status\": \"BAD_REQUEST\",\n"
                    + "   \"message\": \"" + ValidationError.ASSIGNEE_ORGANISATION_ERROR + "\",\n"
                    + "   \"errors\": [ ]\n"
                    + "}"
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    @ApiResponse(
        responseCode = "404",
        description = ValidationError.CASE_NOT_FOUND)
    public CaseAssignmentResponse assignAccessWithinOrganisation(
            @Valid @RequestBody CaseAssignmentRequest requestPayload,
            @RequestParam(name = "use_user_token", defaultValue = "false") boolean useUserToken) {
        CaseAssignment caseAssignment = mapper.map(requestPayload, CaseAssignment.class);
        List<String> roles = caseAssignmentService.assignCaseAccess(caseAssignment, useUserToken);
        return new CaseAssignmentResponse(String.format(ASSIGN_ACCESS_MESSAGE, StringUtils.join(roles, ",")));
    }

    @GetMapping(path = CASE_ASSIGNMENTS_PATH, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Case Assignments in My Organisation", description = "Get Assignments in My Organisation")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(
        responseCode = "200",
        description = "Case-User-Role assignments returned successfully.",
        content = @Content(
            schema = @Schema(implementation = GetCaseAssignmentsResponse.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = """
                    {
                    "status_message": "Case-User-Role assignments returned successfully",
                      "case_assignments": [
                        {
                            "case_id": "1588234985453946",
                            "shared_with": [
                            {
                                "idam_id": "221a2877-e1ab-4dc4-a9ff-f9424ad58738",
                                "first_name": "Bill",
                                "last_name": "Roberts",
                                "email": "bill.roberts@greatbrsolicitors.co.uk",
                                "case_roles": [
                                    "[Claimant]",
                                    "[Defendant]"
                                ]
                            }
                            ]
                        }
                      ]
                    }"""
                    )}))
    @ApiResponse(
        responseCode = "400",
        description = "case_ids must be a non-empty list of proper case ids.",
        content = @Content(
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\"message\": \"case_ids must be a non-empty list of proper case ids\","
                    + " \"status\": \"BAD_REQUEST\" }"
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public GetCaseAssignmentsResponse getCaseAssignments(@RequestParam("case_ids")
            @Valid @NotEmpty(message = "case_ids must be a non-empty list of proper case ids.") List<String> caseIds) {
        validateCaseIds(caseIds);
        List<CaseAssignedUsers> caseAssignedUsers = caseAssignmentService.getCaseAssignments(caseIds);
        return new GetCaseAssignmentsResponse(GET_ASSIGNMENTS_MESSAGE, caseAssignedUsers);
    }

    @DeleteMapping(path = CASE_ASSIGNMENTS_PATH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Unassign Access within Organisation", description = "Unassign Access within Organisation")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponse(
        responseCode = "200",
        description = UNASSIGN_ACCESS_MESSAGE)
    @ApiResponse(
        responseCode = "400",
        description = "One or more of the following reasons:"
            + "\n1) " + ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST
            + "\n2) " + ValidationError.CASE_ID_INVALID
            + "\n3) " + ValidationError.CASE_ID_INVALID_LENGTH
            + "\n4) " + ValidationError.CASE_ID_EMPTY
            + "\n5) " + ValidationError.ASSIGNEE_ID_EMPTY
            + "\n6) " + ValidationError.CASE_ROLE_FORMAT_INVALID
            + "\n7) " + ValidationError.UNASSIGNEE_ORGANISATION_ERROR,
        content = @Content(
            schema = @Schema(implementation = ApiError.class),
            mediaType = APPLICATION_JSON_VALUE,
            examples = { @ExampleObject(
                    value = "{\n"
                    + "   \"status\": \"BAD_REQUEST\",\n"
                    + "   \"errors\": [\n"
                    + "      \"" + ValidationError.CASE_ID_INVALID + "\", \n"
                    + "      \"" + ValidationError.CASE_ROLE_FORMAT_INVALID + "\"\n"
                    + "   ]\n"
                    + "}"
                    )}))
    @ApiResponse(
        responseCode = "401",
        description = AuthError.AUTHENTICATION_TOKEN_INVALID)
    @ApiResponse(
        responseCode = "403",
        description = AuthError.UNAUTHORISED_S2S_SERVICE)
    public CaseUnassignmentResponse unassignAccessWithinOrganisation(
        @Valid @RequestBody CaseUnassignmentRequest requestPayload) {
        caseAssignmentService.unassignCaseAccess(requestPayload.getUnassignments());
        return new CaseUnassignmentResponse(UNASSIGN_ACCESS_MESSAGE);
    }

    private void validateCaseIds(List<String> caseIds) {
        caseIds.stream()
                .filter(caseId -> !StringUtils.isNumeric(caseId))
                .findAny()
                .ifPresent(caseId -> {
                    throw new ValidationException("Case ID should contain digits only");
                });

    }
}
