package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.LuhnCheck;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import java.util.List;

import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.PARAM_CASE_LIST;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.PARAM_DRY_RUN_FLAG;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_ID_LIST;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Organisation Assigned Users Reset Request")
public class OrganisationAssignedUsersResetRequest {

    @JsonProperty(PARAM_CASE_LIST)
    @Valid
    @NotEmpty(message = EMPTY_CASE_ID_LIST)
    @ApiModelProperty(value = "List of cases to process", required = true)
    private List<
            @Valid
            @NotEmpty(message = CASE_ID_EMPTY)
            @Size(min = 16, max = 16, message = CASE_ID_INVALID_LENGTH)
            @LuhnCheck(message = CASE_ID_INVALID, ignoreNonDigitCharacters = false)
            String> caseIds;

    @JsonProperty(PARAM_DRY_RUN_FLAG)
    @ApiModelProperty(value = "Only perform a dry run", required = true)
    private boolean dryRun;
}
