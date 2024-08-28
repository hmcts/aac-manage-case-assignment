package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.LuhnCheck;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Schema(description = "Requested Unassignment From Case")
public class RequestedCaseUnassignment {

    @JsonProperty("case_id")
    @NotEmpty(message = ValidationError.CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = ValidationError.CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = ValidationError.CASE_ID_INVALID)
    @Schema(description = "Case ID to Unassign Access To", 
            requiredMode = RequiredMode.REQUIRED, example = "1583841721773828")
    private String caseId;

    @JsonProperty("assignee_id")
    @NotEmpty(message = ValidationError.ASSIGNEE_ID_EMPTY)
    @Schema(description = "IDAM ID of the User to Unassign", 
            requiredMode = RequiredMode.REQUIRED, example = "ecb5edf4-2f5f-4031-a0ec")
    private String assigneeId;

    @JsonProperty("case_roles")
    @JsonSetter(nulls = AS_EMPTY)
    @Schema(
        description = "Case Roles to Unassign",
        example = "[ \"[Claimant]\", \"[Defendant]\" ]"
    )
    private List<@Pattern(regexp = "^\\[.+]$", message = ValidationError.CASE_ROLE_FORMAT_INVALID)
            String> caseRoles;

    @JsonIgnore
    public boolean hasCaseRoles() {
        return this.caseRoles != null && !this.caseRoles.isEmpty();
    }
}
