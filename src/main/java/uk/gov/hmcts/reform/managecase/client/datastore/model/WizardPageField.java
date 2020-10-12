package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class WizardPageField implements Serializable {

    @JsonProperty("case_field_id")
    private String caseFieldId = null;

    @JsonProperty("order")
    private Integer order = null;

    @JsonProperty("page_column_no")
    private Integer pageColumnNumber;

    @JsonProperty("complex_field_overrides")
    private List<WizardPageComplexFieldOverride> complexFieldOverrides = new ArrayList<>();

    @JsonIgnore
    public Optional<WizardPageComplexFieldOverride> getComplexFieldOverride(String fieldPath) {
        return getComplexFieldOverrides().stream()
            .filter(override -> fieldPath.equals(override.getComplexFieldElementId()))
            .findAny();
    }
}
