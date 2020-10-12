package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class WizardPage implements Serializable {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("label")
    private String label = null;

    @JsonProperty("order")
    private Integer order = null;

    @JsonProperty("wizard_page_fields")
    private List<WizardPageField> wizardPageFields = new ArrayList<>();

    @JsonProperty("show_condition")
    private String showCondition;

    @JsonProperty("callback_url_mid_event")
    private String callBackURLMidEvent;

    @JsonProperty("retries_timeout_mid_event")
    private List<Integer> retriesTimeoutMidEvent;

    @JsonIgnore
    public Set<String> getWizardPageFieldNames() {
        Set<String> wizardPageFields = this.getWizardPageFields()
            .stream()
            .map(WizardPageField::getCaseFieldId)
            .collect(Collectors.toSet());

        Set<String> complexFieldOverRides = this.getWizardPageFields().stream()
            .map(WizardPageField::getComplexFieldOverrides)
            .flatMap(List::stream)
            .map(WizardPageComplexFieldOverride::getComplexFieldElementId)
            .collect(Collectors.toSet());
        wizardPageFields.addAll(complexFieldOverRides);
        return wizardPageFields;
    }
}
