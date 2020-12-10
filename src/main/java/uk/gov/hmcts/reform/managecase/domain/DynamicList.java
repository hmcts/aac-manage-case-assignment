package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DynamicList {
    public static final String LIST_ITEMS = "list_items";
    public static final String VALUE = "value";

    @JsonProperty(VALUE)
    private DynamicListElement value;

    @JsonProperty(LIST_ITEMS)
    private List<DynamicListElement> listItems;
}
