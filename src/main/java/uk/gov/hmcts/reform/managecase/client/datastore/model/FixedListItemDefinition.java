package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema
@SuppressWarnings({"PMD.UncommentedEmptyConstructor"})
public class FixedListItemDefinition implements Serializable {

    private static final long serialVersionUID = 6196146295016140921L;
    private String code;
    private String label;
    private String order;

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Schema
    @JsonProperty("order")
    public String getOrder() {
        return order;
    }

    public void setOrder(final String order) {
        this.order = order;
    }

    @Schema
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
