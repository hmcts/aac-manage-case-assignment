package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private Map<String, Object> $inc;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> $set;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Object> $find;

    @JsonIgnore
    public Map<String, Object> getIncOperation() {
        return $inc;
    }

    @JsonIgnore
    public void setIncOperation(Map<String, Object> $inc) {
        this.$inc = $inc;
    }

    @JsonIgnore
    public Map<String, Object> getSetOperation() {
        return $set;
    }

    @JsonIgnore
    public void setSetOperation(Map<String, Object> $set) {
        this.$set = $set;
    }

    @JsonIgnore
    public Map<String, Object> getFindOperation() {
        return $find;
    }

    @JsonIgnore
    public void setFindOperation(Map<String, Object> $find) {
        this.$find = $find;
    }
}
