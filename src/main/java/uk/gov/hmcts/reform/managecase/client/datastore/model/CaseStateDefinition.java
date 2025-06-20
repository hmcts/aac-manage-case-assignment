package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import java.io.Serializable;
import java.util.List;

@Schema
public class CaseStateDefinition implements Serializable {

    private static final long serialVersionUID = -4257574164546267919L;

    @JsonIgnore
    public static final String ANY = "*";

    private String id;
    private String name;
    private String description;
    private Integer displayOrder;
    private String titleDisplay;
    private List<AccessControlList> accessControlLists;

    @Schema(requiredMode = RequiredMode.REQUIRED)
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Schema
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Short name to display.
     **/
    @Schema(description = "Short name to display.")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @Schema(description = "Title label to be displayed for state")
    @JsonProperty("title_display")
    public String getTitleDisplay() {
        return titleDisplay;
    }

    public void setTitleDisplay(String titleDisplay) {
        this.titleDisplay = titleDisplay;
    }

    @Schema(description = "State Access Control Lists")
    @JsonProperty("acls")
    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
    }
}
