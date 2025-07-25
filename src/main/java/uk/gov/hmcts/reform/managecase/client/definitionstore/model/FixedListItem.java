package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@SuppressWarnings({"PMD.UncommentedEmptyConstructor"})
public class FixedListItem implements Orderable {

    private String code;
    private String label;
    private Integer order;

    public FixedListItem() {
    }

    public FixedListItem(String code, String label, Integer order) {
        this.code = code;
        this.label = label;
        this.order = order;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty("order")
    @Override
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
