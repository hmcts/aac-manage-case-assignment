package uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ApiModel(description = "Definition of a case type in the context of the search")
public class SearchResultViewHeaderGroup {

    @NonNull
    @ApiModelProperty("Metadata for the case type")
    private HeaderGroupMetadata metadata;
    @NonNull
    @ApiModelProperty("Definition of the fields for the case type")
    private List<SearchResultViewHeader> fields;
    @NonNull
    @ApiModelProperty("Case references for the case type that are returned in the search")
    private List<String> cases;
}