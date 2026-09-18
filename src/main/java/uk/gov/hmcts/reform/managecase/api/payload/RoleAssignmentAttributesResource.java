package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Optional;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Used Optional<Striing> in this class, this will help to have three different values for the object when
 * JSON de-serialisation is invoked.
 *
 * eeg:
 * When JSON don't have the jurisdiction field, then value of Optional<String> jurisdiction = null
 *
 * When JSON have the jurisdiction field, and the value of that field is null,
 * then value of Optional<String> jurisdiction = Optional.empty
 *
 * When JSON have the jurisdiction field, and the value of that field is XYZ,
 * then value of Optional<String> jurisdiction = "XYZ"
 *
 */
public class  RoleAssignmentAttributesResource implements Serializable {

    private static final long serialVersionUID = -490395457914850791L;

    @JsonProperty("jurisdiction")
    private Optional<String> jurisdiction;
    @JsonProperty("caseType")
    private Optional<String> caseType;
    @JsonProperty("caseId")
    private Optional<String> caseId;
    @JsonProperty("region")
    private Optional<String> region;
    @JsonProperty("location")
    private Optional<String> location;
    @JsonProperty("contractType")
    private Optional<String> contractType;
}
