package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ToString
@Getter
@Setter
@SuppressWarnings({"PMD.TooManyFields", "PMD.MissingSerialVersionUID"})
public class CaseEventDefinition implements Serializable {

    private String id;
    private String name;
    private String description;
    @JsonProperty("order")
    private Integer displayOrder;
    @JsonProperty("case_fields")
    private List<CaseEventFieldDefinition> caseFields = new ArrayList<>();
    @JsonProperty("pre_states")
    private List<String> preStates = new ArrayList<>();
    @JsonProperty("post_state")
    private String postState;
    @JsonProperty("callback_url_about_to_start_event")
    private String callBackURLAboutToStartEvent;
    @JsonProperty("retries_timeout_about_to_start_event")
    private List<Integer> retriesTimeoutAboutToStartEvent;
    @JsonProperty("callback_url_about_to_submit_event")
    private String callBackURLAboutToSubmitEvent;
    @JsonProperty("retries_timeout_url_about_to_submit_event")
    private List<Integer> retriesTimeoutURLAboutToSubmitEvent;
    @JsonProperty("callback_url_submitted_event")
    private String callBackURLSubmittedEvent;
    @JsonProperty("retries_timeout_url_submitted_event")
    private List<Integer> retriesTimeoutURLSubmittedEvent;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("show_summary")
    private Boolean showSummary;
    @JsonProperty("show_event_notes")
    private Boolean showEventNotes;
    @JsonProperty("end_button_label")
    private String endButtonLabel;
    @JsonProperty("can_save_draft")
    private Boolean canSaveDraft;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;

    public Optional<CaseEventFieldDefinition> getCaseEventField(String caseFieldId) {
        return getCaseFields().stream()
            .filter(f -> f.getCaseFieldId().equals(caseFieldId))
            .findFirst();
    }
}
