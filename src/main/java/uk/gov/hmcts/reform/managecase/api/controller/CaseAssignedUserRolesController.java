package uk.gov.hmcts.reform.managecase.api.controller;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.managecase.service.cau.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.reform.managecase.service.common.UIDService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_ID_LIST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.USER_ID_INVALID;

@RestController
@RequestMapping(path = "/")
public class CaseAssignedUserRolesController {

    private final CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    private final UIDService caseReferenceService;

    @Autowired
    public CaseAssignedUserRolesController(CaseAssignedUserRolesOperation caseAssignedUserRolesOperation,
                                           UIDService caseReferenceService) {
        this.caseAssignedUserRolesOperation = caseAssignedUserRolesOperation;
        this.caseReferenceService = caseReferenceService;

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
                + "\n1) " + CASE_ID_INVALID
                + "\n2) " + EMPTY_CASE_ID_LIST
                + "\n3) " + USER_ID_INVALID,
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
                                                                              Optional<List<String>> optionalUserIds) {
        List<String> userIds = optionalUserIds.orElseGet(Lists::newArrayList);
        validateRequestParams(caseIds, userIds);
        List<CaseAssignedUserRole> caseAssignedUserRoles = this.caseAssignedUserRolesOperation.findCaseUserRoles(caseIds
                                                               .stream()
                                                               .map(Long::valueOf)
                                                               .collect(Collectors.toCollection(ArrayList::new)),
                                                                userIds);
        return ResponseEntity.ok(new CaseAssignedUserRolesResource(caseAssignedUserRoles));
    }

    private void validateRequestParams(List<String> caseIds, List<String> userIds) {

        List<String> errorMessages =
            Lists.newArrayList("Invalid data provided for the following inputs to the request:");
        if (caseIds == null || caseIds.isEmpty()) {
            errorMessages.add(EMPTY_CASE_ID_LIST);
        } else {
            caseIds.forEach(caseId -> {
                if (!caseReferenceService.validateUID(caseId)) {
                    errorMessages.add(CASE_ID_INVALID);
                }
            });
        }

        userIds.forEach(userId -> {
            if (StringUtils.isAllBlank(userId)) {
                errorMessages.add(USER_ID_INVALID);
            }
        });

        if (errorMessages.size() > 1) {
            String message = String.join("\n", errorMessages);
            throw new BadRequestException(message);
        }
    }
}
