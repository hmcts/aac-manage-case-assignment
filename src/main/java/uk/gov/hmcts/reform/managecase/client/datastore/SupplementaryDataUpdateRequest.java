package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@ToString
@NoArgsConstructor
@Data
@SuppressWarnings({"checkstyle:ParameterName", "checkstyle:MemberName"})
public class SupplementaryDataUpdateRequest {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("$inc")
    private Map<String, Object> incrementalMap;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("$set")
    private Map<String, Object> setMap;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("$find")
    private Map<String, Object> findMap;

    @JsonIgnore
    public void setIncrementalMap(Map<String, Object> incrementalMap) {
        this.incrementalMap = incrementalMap;
    }
}
