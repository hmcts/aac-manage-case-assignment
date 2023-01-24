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
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationsAssignedUsersResetRequest;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationsAssignedUsersResetResponse;
import uk.gov.hmcts.reform.managecase.domain.OrganisationsAssignedUsersCountData;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.OrganisationsAssignedUsersService;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.ORGS_ASSIGNED_USERS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PROPERTY_CONTROLLER_ENABLED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_ID_LIST;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@RestController
@Validated
@RequestMapping(path = ORGS_ASSIGNED_USERS_PATH)
@ConditionalOnProperty(value = PROPERTY_CONTROLLER_ENABLED, havingValue = "true")
public class OrganisationsAssignedUsersController {
    private static final Logger LOG = LoggerFactory.getLogger(OrganisationsAssignedUsersController.class);

    public static final String PROPERTY_CONTROLLER_ENABLED
        = "mca.conditional-apis.organisations-assigned-users.enabled";
    public static final String PROPERTY_S2S_AUTHORISED_SERVICES
        = "ccd.s2s-authorised.services.organisations_assigned_users";

    @Value("#{'${" + PROPERTY_S2S_AUTHORISED_SERVICES + "}'.split(',')}")
    protected List<String> authorisedServicesForOrganisationsAssignedUsers;

    public static final String PARAM_CASE_ID = "case_id";
    public static final String PARAM_CASE_LIST = "case_ids";
    public static final String PARAM_DRY_RUN_FLAG = "dry_run";

    @SuppressWarnings({"squid:S1075"})
    public static final String ORGS_ASSIGNED_USERS_PATH = "/organisations-assigned-users";
    @SuppressWarnings({"squid:S1075"})
    public static final String RESET_FOR_A_CASE_PATH = "/reset-for-case";
    @SuppressWarnings({"squid:S1075"})
    public static final String RESET_FOR_MULTIPLE_CASES_PATH = "/reset-for-cases";

    private final OrganisationsAssignedUsersService organisationsAssignedUsersService;
    private final SecurityUtils securityUtils;

    @Autowired
    public OrganisationsAssignedUsersController(OrganisationsAssignedUsersService organisationsAssignedUsersService,
                                                SecurityUtils securityUtils) {
        this.organisationsAssignedUsersService = organisationsAssignedUsersService;
        this.securityUtils = securityUtils;
    }

    @PostMapping(path = RESET_FOR_A_CASE_PATH, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Reset the Organisations Assigned Users count information for a case",
        notes = "Reset the Organisations Assigned Users count information for a case."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The Organisations Assigned Users count information has been reset for case"
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
                        + "   \"message\": \"restOrganisationsAssignedUsersCountDataForACase.caseId\",\n"
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
    public OrganisationsAssignedUsersCountData restOrganisationsAssignedUsersCountDataForACase(
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

        return resetOrganisationsAssignedUsersCountForACase(dryRun, caseId);
    }

    @PostMapping(path = RESET_FOR_MULTIPLE_CASES_PATH, produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Reset the Organisations Assigned Users count information for a number of cases",
        notes = "Reset the Organisations Assigned Users count information for a number of cases."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The Organisations Assigned Users count information has been reset for the cases"
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
                        + "   \"message\": \"restOrganisationsAssignedUsersCountDataForMultipleCases.caseId\",\n"
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
    public OrganisationsAssignedUsersResetResponse restOrganisationsAssignedUsersCountDataForMultipleCases(
        @ApiParam(value = "Valid Service-to-Service JWT token for an approved micro-service", required = true)
        @RequestHeader(SERVICE_AUTHORIZATION) String clientS2SToken,
        @ApiParam(value = "List of cases to process", required = true)
        @Valid @RequestBody
        OrganisationsAssignedUsersResetRequest request
    ) {
        validateRequest(clientS2SToken);

        List<OrganisationsAssignedUsersCountData> countData = new ArrayList<>();

        request.getCaseIds().forEach(caseId -> {
            try {
                countData.add(resetOrganisationsAssignedUsersCountForACase(request.isDryRun(), caseId));
            } catch (Exception ex) {
                String message = String.format("Error resetting Organisation Count for case: %s", caseId);
                LOG.error(message, ex);
                countData.add(OrganisationsAssignedUsersCountData.builder()
                                  .caseId(caseId)
                                  .error(message + ": " + ex.getMessage())
                                  .build());
            }
        });

        return OrganisationsAssignedUsersResetResponse.builder()
            .countData(countData)
            .build();
    }

    private OrganisationsAssignedUsersCountData resetOrganisationsAssignedUsersCountForACase(boolean dryRun,
                                                                                             String caseId) {
        OrganisationsAssignedUsersCountData countData
            = organisationsAssignedUsersService.calculateOrganisationsAssignedUsersCountData(caseId);

        if (!dryRun && (countData.getOrgsAssignedUsers() != null) && !countData.getOrgsAssignedUsers().isEmpty()) {
            organisationsAssignedUsersService.saveCountData(countData);
        }

        return countData;
    }

    private void validateRequest(String clientS2SToken) {
        String clientServiceName = securityUtils.getServiceNameFromS2SToken(clientS2SToken);
        if (!this.authorisedServicesForOrganisationsAssignedUsers.contains(clientServiceName)) {
            throw new CaseRoleAccessException(CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION);
        }
    }
}
