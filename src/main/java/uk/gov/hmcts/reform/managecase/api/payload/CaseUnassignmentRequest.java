package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Case Unassignment Request")
public class CaseUnassignmentRequest {

    @JsonProperty("unassignments")
    @JsonSetter(nulls = AS_EMPTY)
    @NotEmpty(message = EMPTY_REQUESTED_UNASSIGNMENTS_LIST)
    @Schema(description = "Requested Unassignments", 
            requiredMode = RequiredMode.REQUIRED)
    private List<@Valid RequestedCaseUnassignment> unassignments;

}
