package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"PMD.MethodReturnsInternalArray",
    "PMD.UseVarargs",
    "PMD.ArrayIsStoredDirectly"})
public class CaseView extends AbstractCaseView {
    private ProfileCaseState state;
    private String[] channels;
    private CaseViewActionableEvent[] actionableEvents;
    private CaseViewEvent[] caseViewEvents;

    public ProfileCaseState getState() {
        return state;
    }

    public void setState(ProfileCaseState state) {
        this.state = state;
    }

    public String[] getChannels() {
        return channels;
    }

    public void setChannels(String[] channels) {
        this.channels = channels;
    }

    @JsonProperty("triggers")
    public CaseViewActionableEvent[] getActionableEvents() {
        return actionableEvents;
    }

    public void setActionableEvents(CaseViewActionableEvent[] actionableEvents) {
        this.actionableEvents = actionableEvents;
    }

    @JsonProperty("events")
    public CaseViewEvent[] getEvents() {
        return caseViewEvents;
    }

    public void setEvents(CaseViewEvent[] events) {
        this.caseViewEvents = events;
    }
}
