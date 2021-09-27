package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SupplementaryDataUpdateRequest {

    @JsonProperty("supplementary_data_updates")
    private SupplementaryDataUpdates supplementaryDataUpdates;
}
