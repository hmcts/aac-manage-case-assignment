package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import uk.gov.hmcts.reform.managecase.client.datastore.model.AccessControlList;
import uk.gov.hmcts.reform.managecase.client.datastore.model.ComplexACL;

import java.util.ArrayList;
import java.util.List;

@ApiModel(description = "")
@SuppressWarnings({"PMD.TooManyFields"})
public class CaseField implements HasAcls, Orderable {

    private String id;
    private String caseTypeId;
    private String label;
    private String hintText;
    private FieldType fieldType;
    private Boolean hidden;
    private String securityClassification;
    private String liveFrom;
    private String liveUntil;
    private List<AccessControlList> acls = new ArrayList<>();
    private List<ComplexACL> complexACLs = new ArrayList<>();
    private Integer order;
    private String showCondition;
    private boolean metadata;
    private String displayContextParameter;
    private Boolean retainHiddenValue;

    /**
     * The id of the case field.
     **/
    @ApiModelProperty(required = true, value = "The id of the case field")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Foriegn key to the case type as fields should not work across.
     **/
    @ApiModelProperty("Foriegn key to the case type as fields should not work across")
    @JsonProperty("case_type_id")
    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    /**
     * hint text associated with the field.
     **/
    @ApiModelProperty("")
    @JsonProperty("hint_text")
    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    /**
     * label associated with the field.
     **/
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * return the type of the field i.e. YesOrNo, text, date etc.
     **/
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("field_type")
    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    /**
     * should the field be hidden.
     **/
    @ApiModelProperty("")
    @JsonProperty("hidden")
    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Gov security level of the data (official, top secret etc).
     **/
    @ApiModelProperty("Gov security level of the data (official, top secret etc)")
    @JsonProperty("security_classification")
    public String getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(String securityClassification) {
        this.securityClassification = securityClassification;
    }

    /**
     * date the field went live.
     **/
    @ApiModelProperty("")
    @JsonProperty("live_from")
    public String getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(String liveFrom) {
        this.liveFrom = liveFrom;
    }

    /**
     * date the field should be active until.
     **/
    @ApiModelProperty("")
    @JsonProperty("live_until")
    public String getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(String liveUntil) {
        this.liveUntil = liveUntil;
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
    @JsonProperty("order")
    @Override
    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @ApiModelProperty("")
    @JsonProperty("acls")
    public List<AccessControlList> getAcls() {
        return this.acls;
    }

    @Override
    public void setAcls(List<AccessControlList> acls) {
        this.acls = acls;
    }

    @ApiModelProperty("")
    @JsonProperty("complexACLs")
    public List<ComplexACL> getComplexACLs() {
        return complexACLs;
    }

    public void setComplexACLs(List<ComplexACL> complexACLs) {
        this.complexACLs = complexACLs;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    @ApiModelProperty("")
    @JsonProperty("display_context_parameter")
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    @ApiModelProperty("")
    @JsonProperty("retain_hidden_value")
    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }
}
