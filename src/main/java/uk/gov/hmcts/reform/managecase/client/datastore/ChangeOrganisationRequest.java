package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import java.time.LocalDateTime;

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
    private final LocalDateTime requestTimestamp;

    @Setter
    @JsonProperty("ApprovalStatus")
    private String approvalStatus;
}
