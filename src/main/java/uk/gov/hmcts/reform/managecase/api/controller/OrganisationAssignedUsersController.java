package uk.gov.hmcts.reform.managecase.api.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.hibernate.validator.constraints.LuhnCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseRoleAccessException;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationAssignedUsersResetRequest;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationAssignedUsersResetResponse;
import uk.gov.hmcts.reform.managecase.domain.OrganisationAssignedUsersCountData;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.OrganisationAssignedUsersService;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.ORG_ASSIGNED_USERS_PATH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_ID_LIST;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@RestController
@Validated
@RequestMapping(path = ORG_ASSIGNED_USERS_PATH)
@ConditionalOnProperty(value = "mca.conditional-apis.organisation-counts.enabled", havingValue = "true")
public class OrganisationAssignedUsersController {
    private static final Logger LOG = LoggerFactory.getLogger(OrganisationAssignedUsersController.class);

    @Value("#{'${ccd.s2s-authorised.services.organisation_assigned_users}'.split(',')}")
    private List<String> authorisedServicesForOrganisationAssignedUsers;

    public static final String PARAM_CASE_ID = "case_id";
    public static final String PARAM_CASE_LIST = "case_ids";
    public static final String PARAM_DRY_RUN_FLAG = "dry_run";

    @SuppressWarnings({"squid:S1075"})
    public static final String ORG_ASSIGNED_USERS_PATH = "/organisation-assigned-users";
    @SuppressWarnings({"squid:S1075"})
    public static final String RESET_ORG_COUNT_PATH = "/reset-for-case";
    @SuppressWarnings({"squid:S1075"})
    public static final String RESET_ORG_COUNTS_PATH = "/reset-for-cases";

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
    @ApiOperation(
        value = "Reset organisation user counts for a case",
        notes = "Reset organisation user counts for a case."
    )
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
    public OrganisationAssignedUsersCountData restOrganisationCountForCase(
        @ApiParam(value = "Valid Service-to-Service JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION)
        String clientS2SToken,
        @ApiParam(value = "Case to process", required = true)
        @RequestParam(value = PARAM_CASE_ID)
        @Valid
        @NotEmpty(message = CASE_ID_EMPTY)
        @Size(min = 16, max = 16, message = CASE_ID_INVALID_LENGTH)
        @LuhnCheck(message = CASE_ID_INVALID, ignoreNonDigitCharacters = false)
        String caseId,
        @ApiParam(value = "Only perform a dry run", required = true)
        @RequestParam(name = PARAM_DRY_RUN_FLAG)
        boolean dryRun
    ) {
        validateRequest(clientS2SToken);

        return resetOrganisationAssignedUsersCountForCase(dryRun, caseId);
    }

    @PostMapping(path = RESET_ORG_COUNTS_PATH, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Reset organisation user counts for a number of cases",
        notes = "Reset organisation user counts for a number of cases."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Organisation user counts have been reset for the cases"
        ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + EMPTY_CASE_ID_LIST
                + "\n2) " + CASE_ID_EMPTY
                + "\n3) " + CASE_ID_INVALID_LENGTH
                + "\n4) " + CASE_ID_INVALID,
            response = ApiError.class,
            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"BAD_REQUEST\",\n"
                        + "   \"message\": \"restOrganisationCountForMultipleCases.caseId\",\n"
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
        )
    })
    public OrganisationAssignedUsersResetResponse restOrganisationCountForMultipleCases(
        @ApiParam(value = "Valid Service-to-Service JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken,
        @ApiParam(value = "List of cases to process", required = true)
        @Valid @RequestBody
        OrganisationAssignedUsersResetRequest request
    ) {
        validateRequest(clientS2SToken);

        List<OrganisationAssignedUsersCountData> results = new ArrayList<>();

        request.getCaseIds().forEach(caseId -> {
            try {
                results.add(resetOrganisationAssignedUsersCountForCase(request.isDryRun(), caseId));
            } catch (Exception ex) {
                String message = String.format("Error resetting Organisation Count for case: %s", caseId);
                LOG.error(message, ex);
                results.add(OrganisationAssignedUsersCountData.builder()
                                .caseId(caseId)
                                .error(message + ": " + ex.getMessage())
                                .build());
            }
        });

        return OrganisationAssignedUsersResetResponse.builder()
            .orgUserCounts(results)
            .build();
    }

    private OrganisationAssignedUsersCountData  resetOrganisationAssignedUsersCountForCase(boolean dryRun,
                                                                                           String caseId) {
        OrganisationAssignedUsersCountData countData
            = organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(caseId);

        if (!dryRun && (countData.getOrgsAssignedUsers() != null) && !countData.getOrgsAssignedUsers().isEmpty()) {
            organisationAssignedUsersService.saveOrganisationUserCount(countData);
        }

        return countData;
    }

    private void validateRequest(String clientS2SToken) {
        String clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        if (!this.authorisedServicesForOrganisationAssignedUsers.contains(clientServiceName)) {
            throw new CaseRoleAccessException(CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION);
        }
    }
}
