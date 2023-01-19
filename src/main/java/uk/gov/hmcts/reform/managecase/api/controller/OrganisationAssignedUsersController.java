package uk.gov.hmcts.reform.managecase.api.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.hibernate.validator.constraints.LuhnCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseRoleAccessException;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationAssignedUsersResetResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.OrganisationAssignedUsersService;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@RestController
@Validated
@RequestMapping(path = "/organisation-assigned-users")
@ConditionalOnProperty(value = "mca.conditional-apis.organisation-counts.enabled", havingValue = "true")
public class OrganisationAssignedUsersController {

    @Value("#{'${ccd.s2s-authorised.services.organisation_assigned_users}'.split(',')}")
    private List<String> authorisedServicesForOrganisationAssignedUsers;

    @SuppressWarnings({"squid:S1075"})
    public static final String RESET_ORG_COUNT_PATH = "/reset-for-case";

    private final OrganisationAssignedUsersService organisationAssignedUsersService;
    private final SecurityUtils securityUtils;

    @Autowired
    public OrganisationAssignedUsersController(OrganisationAssignedUsersService organisationAssignedUsersService,
                                               SecurityUtils securityUtils) {
        this.organisationAssignedUsersService = organisationAssignedUsersService;
        this.securityUtils = securityUtils;
    }

    @PostMapping(path = RESET_ORG_COUNT_PATH, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Get Case Assignments in My Organisation", notes = "Get Assignments in My Organisation")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Organisation user counts have been reset for case"
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + CASE_ID_EMPTY
                + "\n2) " + CASE_ID_INVALID_LENGTH
                + "\n3) " + CASE_ID_INVALID,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"restOrganisationCountForCase.caseId\",\n"
                        + "   \"errors\": [ \"" + CASE_ID_INVALID + "\" ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        ),
        @ApiResponse(
            code = 401,
            message = "Authentication failure due to invalid / expired tokens (IDAM / S2S)."
        ),
        @ApiResponse(
            code = 403,
            message = "One of the following reasons:\n"
                + "1. Unauthorised S2S service \n"
                + "2. " + CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION + "."
        ),
        @ApiResponse(
            code = 404,
            message = CASE_NOT_FOUND,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"NOT_FOUND\",\n"
                        + "   \"message\": \"" + CASE_NOT_FOUND + "\",\n"
                        + "   \"errors\": [ ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
        )
    })
    public OrganisationAssignedUsersResetResponse restOrganisationCountForCase(
        @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken,
        @RequestParam(value = "case_ids")
        @Valid
        @NotEmpty(message = CASE_ID_EMPTY)
        @Size(min = 16, max = 16, message = CASE_ID_INVALID_LENGTH)
        @LuhnCheck(message = CASE_ID_INVALID, ignoreNonDigitCharacters = false)
        String caseId,
        @RequestParam(name = "dry_run")
        boolean dryRun
    ) {
        validateRequest(clientS2SToken);

        Map<String, Long> orgUserCounts
            = organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(caseId);

        if (!dryRun && !orgUserCounts.isEmpty()) {
            organisationAssignedUsersService.saveOrganisationUserCount(caseId, orgUserCounts);
        }

        return OrganisationAssignedUsersResetResponse.builder()
            .orgUserCounts(orgUserCounts)
            .build();
    }

    private void validateRequest(String clientS2SToken) {
        String clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        if (!authorisedServicesForOrganisationAssignedUsers.contains(clientServiceName)) {
            throw new CaseRoleAccessException(CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION);
        }
    }
}
