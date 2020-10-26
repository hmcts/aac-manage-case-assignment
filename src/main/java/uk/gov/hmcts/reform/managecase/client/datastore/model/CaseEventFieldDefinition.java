package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString
@ApiModel(description = "")
public class CaseEventFieldDefinition implements Serializable, CommonDCPModel {

    private static final long serialVersionUID = -4257574164546267919L;

    private String caseFieldId;
    private String displayContext;
    private String displayContextParameter;
    private String showCondition;
    private Boolean showSummaryChangeOption;
    private Integer showSummaryContentOption;
    private String label;
    private String hintText;
    private Boolean retainHiddenValue;
    private String defaultValue;
    private List<CaseEventFieldComplexDefinition> caseEventFieldComplexDefinitions = new ArrayList<>();

    @ApiModelProperty(required = true, value = "Foreign key to CaseField.id")
    @JsonProperty("case_field_id")
    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    @ApiModelProperty("whether this field is optional, mandatory or read only for this event")
    @JsonProperty("display_context")
    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    @ApiModelProperty("contain names of fields for list or table")
    @JsonProperty("display_context_parameter")
    @Override
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    @ApiModelProperty("Show Condition expression for this field")
    @JsonProperty("show_condition")
    public String getShowCondition() {
        return showCondition;
    }

    public void setShowCondition(String showCondition) {
        this.showCondition = showCondition;
    }

    /**
     * event case field hint text.
     **/
    @ApiModelProperty("")
    @JsonProperty("hint_text")
    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @ApiModelProperty("Show Summary Change Option")
    @JsonProperty("show_summary_change_option")
    public Boolean getShowSummaryChangeOption() {
        return showSummaryChangeOption;
    }

    public void setShowSummaryChangeOption(final Boolean showSummaryChangeOption) {
        this.showSummaryChangeOption = showSummaryChangeOption;
    }

    @ApiModelProperty("Show Summary Content Option")
    @JsonProperty("show_summary_content_option")
    public Integer getShowSummaryContentOption() {
        return showSummaryContentOption;
    }

    public void setShowSummaryContentOption(Integer showSummaryContentOption) {
        this.showSummaryContentOption = showSummaryContentOption;
    }

    /**
     * event case field label.
     **/
    @ApiModelProperty("")
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @ApiModelProperty("")
    @JsonProperty("case_fields_complex")
    public List<CaseEventFieldComplexDefinition> getCaseEventFieldComplexDefinitions() {
        return caseEventFieldComplexDefinitions;
    }

    public void setCaseEventFieldComplexDefinitions(List<CaseEventFieldComplexDefinition> eventComplexTypeEntities) {
        this.caseEventFieldComplexDefinitions = eventComplexTypeEntities;
    }

    @ApiModelProperty("Default value coming from the Event that overwrites complex fields.")
    @JsonProperty("defaultValue")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @ApiModelProperty("whether this field is data should be retained, "
        + "dependant on show_condition being populated")
    @JsonProperty("retain_hidden_value")
    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

}
