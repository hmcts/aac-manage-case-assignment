package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
@Schema
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyFields"})
public class CaseFieldDefinition implements Serializable, CommonField {

    @Serial
    private static final long serialVersionUID = -4257574164546267919L;

    private String id;
    @JsonProperty("case_type_id")
    private String caseTypeId;
    private String label;
    @JsonProperty("hint_text")
    private String hintText;
    @JsonProperty("field_type")
    private FieldTypeDefinition fieldTypeDefinition;
    private Boolean hidden;
    @JsonProperty("security_classification")
    private String securityLabel;
    @JsonProperty("live_from")
    private String liveFrom;
    @JsonProperty("live_until")
    private String liveUntil;
    private Integer order;
    @JsonProperty("show_condition")
    private String showConditon;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    @JsonProperty("complexACLs")
    private List<ComplexACL> complexACLs = new ArrayList<>();
    private boolean metadata;
    @JsonProperty("display_context")
    private String displayContext;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;
    @JsonProperty("retain_hidden_value")
    private Boolean retainHiddenValue;
    @JsonProperty("formatted_value")
    private transient Object formattedValue;
    @JsonProperty("default_value")
    private String defaultValue;

}
