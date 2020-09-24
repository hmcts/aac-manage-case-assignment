package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseViewResource extends RepresentationModel {

    @JsonProperty("case_id")
    private String reference;

    @JsonProperty("case_type")
    private CaseViewType caseType;

    @JsonProperty("tabs")
    private CaseViewTab[] tabs;

    @JsonProperty("metadataFields")
    private List<CaseViewField> metadataFields;

    @JsonProperty("state")
    private ProfileCaseState state;

    @JsonProperty("triggers")
    private CaseViewActionableEvent[] caseViewActionableEvents;

    @JsonProperty("events")
    private CaseViewEvent[] caseViewEvents;

    private void copyProperties(CaseView caseView) {
        this.reference = caseView.getCaseId();
        this.caseType = caseView.getCaseType();
        this.tabs = caseView.getTabs();
        this.metadataFields = caseView.getMetadataFields();
        this.state = caseView.getState();
        this.caseViewActionableEvents = caseView.getActionableEvents();
        this.caseViewEvents = caseView.getEvents();
    }
}
