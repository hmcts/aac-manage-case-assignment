package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(description = "")
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

    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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
