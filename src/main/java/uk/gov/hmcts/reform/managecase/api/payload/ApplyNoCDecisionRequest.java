package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import jakarta.validation.constraints.NotNull;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_DETAILS_REQUIRED;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Apply Notice of Change Decision Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyNoCDecisionRequest {

    @JsonProperty("case_details")
    @NotNull(message = CASE_DETAILS_REQUIRED)
    @Schema(description = "Case details", requiredMode = RequiredMode.REQUIRED)
    private CaseDetails caseDetails;
}
