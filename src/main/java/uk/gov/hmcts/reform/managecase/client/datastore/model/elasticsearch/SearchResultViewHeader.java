package uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CommonViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultViewHeader implements CommonViewHeader {

    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_type")
    private FieldTypeDefinition caseFieldTypeDefinition;
    private String label;
    private Integer order;
    private boolean metadata;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;
}
