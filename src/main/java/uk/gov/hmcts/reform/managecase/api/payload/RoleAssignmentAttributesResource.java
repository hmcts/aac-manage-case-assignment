package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentAttributesResource {
    private static final long serialVersionUID = -8907666789404292869L;

    Optional<String> jurisdiction;
    Optional<String> caseType;
    Optional<String> caseId;
    Optional<String> region;
    Optional<String> location;
    Optional<String> contractType;
}
