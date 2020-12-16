package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileCaseState {
    private String id;
    private String name;
    private String description;
    @JsonProperty("title_display")
    private String titleDisplay;

    public ProfileCaseState() {
        // default constructor
    }

    public ProfileCaseState(String id, String name, String description, String titleDisplay) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.titleDisplay = titleDisplay;
    }
}
