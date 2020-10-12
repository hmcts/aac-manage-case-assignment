package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Optional;

@Data
public class WizardPageComplexFieldOverride implements Serializable {
    @JsonProperty("complex_field_element_id")
    private String complexFieldElementId;

    @JsonProperty("display_context")
    private String displayContext;

    @JsonProperty("label")
    private String label;

    @JsonProperty("hint_text")
    private String hintText;

    @JsonProperty("show_condition")
    private String showCondition;

    @JsonProperty("default_value")
    private String defaultValue;

    public DisplayContext displayContextType() {
        return Optional.ofNullable(getDisplayContext())
            .filter(dc -> !dc.equals("HIDDEN"))
            .map(DisplayContext::valueOf)
            .orElse(null);
    }
}
