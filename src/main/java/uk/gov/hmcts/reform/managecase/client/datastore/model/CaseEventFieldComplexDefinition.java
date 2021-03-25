package uk.gov.hmcts.reform.managecase.client.datastore.model;

import lombok.Builder;

import java.io.Serializable;

@Builder
@SuppressWarnings({"PMD.UncommentedEmptyConstructor"})
public class CaseEventFieldComplexDefinition implements Serializable, CommonDCPModel {

    private static final long serialVersionUID = -4257574164546267919L;

    private String reference;

    private Integer order;

    private String displayContextParameter;

    private String defaultValue;

    private Boolean retainHiddenValue;

    public CaseEventFieldComplexDefinition() {
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order,
                                           String defaultValue,
                                           Boolean retainHiddenValue) {
        this.reference = reference;
        this.order = order;
        this.defaultValue = defaultValue;
        this.retainHiddenValue = retainHiddenValue;
    }

    public CaseEventFieldComplexDefinition(String reference,
                                           Integer order,
                                           String displayContextParameter,
                                           String defaultValue, Boolean retainHiddenValue) {

        this.reference = reference;
        this.order = order;
        this.displayContextParameter = displayContextParameter;
        this.defaultValue = defaultValue;
        this.retainHiddenValue = retainHiddenValue;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public void setDisplayContextParameter(String displayContextParameter) {
        this.displayContextParameter = displayContextParameter;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getRetainHiddenValue() {
        return retainHiddenValue;
    }

    public void setRetainHiddenValue(Boolean retainHiddenValue) {
        this.retainHiddenValue = retainHiddenValue;
    }

}
