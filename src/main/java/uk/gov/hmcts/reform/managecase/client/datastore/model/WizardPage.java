package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WizardPage implements Serializable {

    private static final long serialVersionUID = -7977574952967533450L;

    @JsonProperty("id")
    private String id;

    @JsonProperty("label")
    private String label;

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("wizard_page_fields")
    private List<WizardPageField> wizardPageFields;

    @JsonProperty("show_condition")
    private String showCondition;

    @JsonProperty("callback_url_mid_event")
    private String callBackURLMidEvent;

    @JsonProperty("retries_timeout_mid_event")
    private List<Integer> retriesTimeoutMidEvent;
}
