package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

@ApiModel(description = "")
public class CaseTypeTabField implements Serializable, CommonDCPModel {

    private static final long serialVersionUID = -4257574164546267919L;

    private CaseFieldDefinition caseFieldDefinition;
    private Integer displayOrder;
    private String showCondition;
    private String displayContextParameter;

    @ApiModelProperty("")
    @JsonProperty("case_field")
    public CaseFieldDefinition getCaseFieldDefinition() {
        return caseFieldDefinition;
    }

    public void setCaseFieldDefinition(final CaseFieldDefinition caseFieldDefinition) {
        this.caseFieldDefinition = caseFieldDefinition;
    }

    @ApiModelProperty("")
    @JsonProperty("order")
    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(final Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ApiModelProperty("")
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    @ApiModelProperty("")
    @JsonProperty("display_context_parameter")
    @Override
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }
}
