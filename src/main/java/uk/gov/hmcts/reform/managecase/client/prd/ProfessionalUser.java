package uk.gov.hmcts.reform.managecase.client.prd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfessionalUser {

    @JsonProperty
    private String userIdentifier;
    @JsonProperty
    private String firstName;
    @JsonProperty
    private String lastName;
    @JsonProperty
    private String email;
    @JsonProperty
    private List<String> roles;
    @JsonProperty
    private String idamStatus;
    @JsonProperty
    private String idamStatusCode;
    @JsonProperty
    private String idamMessage;

}
