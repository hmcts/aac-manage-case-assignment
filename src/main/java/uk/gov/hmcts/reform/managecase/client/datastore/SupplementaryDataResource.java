package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SupplementaryDataResource {

    @JsonProperty("supplementary_data")
    private Map<String, Object> response;

    public SupplementaryDataResource(final SupplementaryData supplementaryDataUpdated) {
        this.response = supplementaryDataUpdated.getResponse();
    }
}
