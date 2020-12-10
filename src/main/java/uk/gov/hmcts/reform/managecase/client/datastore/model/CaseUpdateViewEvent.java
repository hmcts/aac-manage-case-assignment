package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseUpdateViewEvent {

    private String id;
    private String name;
    private String description;
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_fields")
    private List<CaseViewField> caseFields;
    @JsonProperty("event_token")
    private String eventToken;
    @JsonProperty("wizard_pages")
    private List<WizardPage> wizardPages;
    @JsonProperty("show_summary")
    private Boolean showSummary;
    @JsonProperty("show_event_notes")
    private Boolean showEventNotes;
    @JsonProperty("end_button_label")
    private String endButtonLabel;
    @JsonProperty("can_save_draft")
    private Boolean canSaveDraft;
}
