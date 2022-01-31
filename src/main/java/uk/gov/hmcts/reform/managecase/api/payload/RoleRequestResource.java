package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Builder
@Value
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleRequestResource {

    String id; // this will be generated by RAS while saving request entity.
    String clientId; // this will be retrieved by RAS using s2s token
    String authenticatedUserId; // this will be retrieved by RAS from user-token.
    String correlationId; // If it is empty then need to be generated by RAS.
    String assignerId;
    String requestType;
    String process;
    String reference;
    boolean replaceExisting;
    String roleAssignmentId;
    String status; // this will be set by RAS default = created
    @JsonFormat(timezone = "UTC", shape = JsonFormat.Shape.STRING)
    Instant created; // this will be set by RAS
    String log; // this will be set RAS based on drool validation rule name on individual assignments.
    boolean byPassOrgDroolRule;

}
