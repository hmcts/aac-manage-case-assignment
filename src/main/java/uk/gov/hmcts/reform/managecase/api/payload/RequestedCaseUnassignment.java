package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.LuhnCheck;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ApiModel("Requested Unassignment From Case")
public class RequestedCaseUnassignment {

    @JsonProperty("case_id")
    @NotEmpty(message = ValidationError.CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = ValidationError.CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = ValidationError.CASE_ID_INVALID)
    @ApiModelProperty(value = "Case ID to Unassign Access To", required = true, example = "1583841721773828")
    private String caseId;

    @JsonProperty("assignee_id")
    @NotEmpty(message = ValidationError.ASSIGNEE_ID_EMPTY)
    @ApiModelProperty(value = "IDAM ID of the User to Unassign", required = true, example = "ecb5edf4-2f5f-4031-a0ec")
    private String assigneeId;

    @JsonProperty("case_roles")
    @JsonSetter(nulls = AS_EMPTY)
    @ApiModelProperty(
        value = "Case Roles to Unassign",
        allowEmptyValue = true,
        example = "[ \"[Claimant]\", \"[Defendant]\" ]"
    )
    private List<@Pattern(regexp = "^\\[.+]$", message = ValidationError.CASE_ROLE_FORMAT_INVALID)
            String> caseRoles;

    @JsonIgnore
    public boolean hasCaseRoles() {
        return this.caseRoles != null && !this.caseRoles.isEmpty();
    }
}
