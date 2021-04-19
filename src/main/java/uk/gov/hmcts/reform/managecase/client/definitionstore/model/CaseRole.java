package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@ApiModel(description = "Case role of a Case Type")
public class CaseRole {

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;

}
