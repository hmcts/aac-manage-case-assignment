package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Case Assignment Request")
public class CaseAssignmentRequest {

    @JsonProperty("case_type_id")
    @NotEmpty(message = ValidationError.CASE_TYPE_ID_EMPTY)
    @Schema(description = "Case type ID of the requested case", 
            requiredMode = RequiredMode.REQUIRED, 
            example = "PROBATE-TEST")
    private String caseTypeId;

    @JsonProperty("case_id")
    @NotEmpty(message = ValidationError.CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = ValidationError.CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = ValidationError.CASE_ID_INVALID, ignoreNonDigitCharacters = false)
    @Schema(description = "Case ID to Assign Access To", 
            requiredMode = RequiredMode.REQUIRED, 
            example = "1583841721773828")
    private String caseId;

    @JsonProperty("assignee_id")
    @NotEmpty(message = ValidationError.ASSIGNEE_ID_EMPTY)
    @Schema(description = "IDAM ID of the Assign User", 
            requiredMode = RequiredMode.REQUIRED, 
            example = "ecb5edf4-2f5f-4031-a0ec")
    private String assigneeId;
}
