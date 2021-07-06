package uk.gov.hmcts.reform.managecase.api.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.managecase.service.cau.CaseAssignedUserRolesOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/")
public class CaseAssignedUserRolesController {

    private final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    public CaseAssignedUserRolesController(CaseAssignedUserRolesOperation caseAssignedUserRolesOperation) {
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
    }

    @GetMapping(path = "/case-users")
    @ApiOperation(value = "Get Case-Assigned Users and Roles")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Get Case-Assigned Users and Roles",
            response = CaseAssignedUserRolesResource.class
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + ValidationError.CASE_ID_INVALID
                + "\n2) " + ValidationError.EMPTY_CASE_ID_LIST
                + "\n3) " + ValidationError.USER_ID_INVALID,
            response = ApiError.class
        ),
        @ApiResponse(
            code = 401,
            message = AuthError.AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = "One of the following reasons:"
                + "\n1) " + AuthError.OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED
                + "\n2) " + AuthError.UNAUTHORISED_S2S_SERVICE
        )
    })
    public ResponseEntity<CaseAssignedUserRolesResource> getCaseUserRoles(@RequestParam("case_ids")
                                                                              List<String> caseIds,
                                                                          @RequestParam(value = "user_ids",
                                                                              required = false)
                                                                              Optional<List<String>> optionalUserIds,
                                                                          @RequestParam(value = "organisation_id",
                                                                              required = false)
                                                                              String organisationId) {
        List<String> userIds = optionalUserIds.orElseGet(Lists::newArrayList);
        validateRequestParams(caseIds, userIds);
        List<CaseAssignedUserRole> caseAssignedUserRoles = this.caseAssignedUserRolesOperation.findCaseUserRoles(caseIds
                                                               .stream()
                                                               .map(Long::valueOf)
                                                               .collect(Collectors.toCollection(ArrayList::new)), userIds);
        return ResponseEntity.ok(new CaseAssignedUserRolesResource(caseAssignedUserRoles));
    }

    private void validateRequestParams(List<String> caseIds, List<String> userIds) {

        // validate request params
    }
}
