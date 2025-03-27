package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString
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

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Foreign key to CaseField.id")
    @JsonProperty("case_field_id")
    public String getCaseFieldId() {
        return caseFieldId;
    }

    public void setCaseFieldId(String caseFieldId) {
        this.caseFieldId = caseFieldId;
    }

    @Schema(description = "whether this field is optional, mandatory or read only for this event")
    @JsonProperty("display_context")
    public String getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(String displayContext) {
        this.displayContext = displayContext;
    }

    @Schema(description = "contain names of fields for list or table")
    @JsonProperty("display_context_parameter")
    @Override
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    @Schema(description = "Show Condition expression for this field")
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
    @Schema
    @JsonProperty("hint_text")
    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @Schema(description = "Show Summary Change Option")
    @JsonProperty("show_summary_change_option")
    public Boolean getShowSummaryChangeOption() {
        return showSummaryChangeOption;
    }

    public void setShowSummaryChangeOption(final Boolean showSummaryChangeOption) {
        this.showSummaryChangeOption = showSummaryChangeOption;
    }

    @Schema(description = "Show Summary Content Option")
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
    @Schema
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Schema
    @JsonProperty("case_fields_complex")
    public List<CaseEventFieldComplexDefinition> getCaseEventFieldComplexDefinitions() {
        return caseEventFieldComplexDefinitions;
    }

    public void setCaseEventFieldComplexDefinitions(List<CaseEventFieldComplexDefinition> eventComplexTypeEntities) {
        this.caseEventFieldComplexDefinitions = eventComplexTypeEntities;
    }

    @Schema(description = "Default value coming from the Event that overwrites complex fields.")
    @JsonProperty("defaultValue")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Schema(description = "whether this field is data should be retained, "
        + "dependant on show_condition being populated")
    @JsonProperty("retain_hidden_value")
    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

}
