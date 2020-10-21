package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ApiModel(description = "")
@ToString
public class JurisdictionDefinition implements Serializable {

    private static final long serialVersionUID = 6196146295016140921L;

    private String id;
    private String name;
    private String description;
    private Date liveFrom;
    private Date liveUntil;

    private List<CaseTypeDefinition> caseTypeDefinitions = new ArrayList<>();

    @ApiModelProperty(required = true, value = "")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(required = true, value = "")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty("")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty("")
    @JsonProperty("live_from")
    public Date getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(Date liveFrom) {
        this.liveFrom = liveFrom;
    }

    @ApiModelProperty("")
    @JsonProperty("live_until")
    public Date getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(Date liveUntil) {
        this.liveUntil = liveUntil;
    }

    @ApiModelProperty("")
    @JsonProperty("case_types")
    public List<CaseTypeDefinition> getCaseTypeDefinitions() {
        return caseTypeDefinitions;
    }

    public void setCaseTypeDefinitions(List<CaseTypeDefinition> caseTypeDefinitions) {
        this.caseTypeDefinitions = caseTypeDefinitions;
    }

    public List<String> getCaseTypesIDs() {
        return this.getCaseTypeDefinitions().stream().map(CaseTypeDefinition::getId).collect(Collectors.toList());
    }
}
