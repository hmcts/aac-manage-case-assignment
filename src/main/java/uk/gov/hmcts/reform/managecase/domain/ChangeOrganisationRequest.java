package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import javax.validation.ValidationException;
import java.time.LocalDateTime;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeOrganisationRequest {

    @JsonProperty("OrganisationToAdd")
    private final Organisation organisationToAdd;

    @JsonProperty("OrganisationToRemove")
    private final Organisation organisationToRemove;

    @JsonProperty("CaseRoleId")
    private final String caseRoleId;

    @JsonProperty("RequestTimestamp")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private final LocalDateTime requestTimestamp;

    @Setter
    @JsonProperty("ApprovalStatus")
    private String approvalStatus;

    public void validateChangeOrganisationRequest() {
        if (StringUtils.isBlank(this.getCaseRoleId())
            || StringUtils.isBlank(this.getApprovalStatus())
            || this.getRequestTimestamp() == null
            || (this.getOrganisationToAdd() == null && this.getOrganisationToRemove() == null)) {
            throw new ValidationException(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID);
        }
    }
}
