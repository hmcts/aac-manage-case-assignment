package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.ValidationException;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeOrganisationRequest {

    @JsonProperty("OrganisationToAdd")
    private Organisation organisationToAdd;

    @Setter
    @JsonProperty("OrganisationToRemove")
    private Organisation organisationToRemove;

    @JsonProperty("CaseRoleId")
    private DynamicList caseRoleId;

    @JsonProperty("RequestTimestamp")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime requestTimestamp;

    @JsonProperty("ApprovalStatus")
    private String approvalStatus;

    @JsonProperty("CreatedBy")
    private String createdBy;

    public void validateChangeOrganisationRequest() {
        if (this.getCaseRoleId() == null
            || StringUtils.isBlank(this.getApprovalStatus())
            || this.getRequestTimestamp() == null
            || (this.getOrganisationToAdd() == null && this.getOrganisationToRemove() == null)) {
            throw new ValidationException(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID);
        }
    }
}
