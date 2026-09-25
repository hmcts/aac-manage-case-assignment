package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.LABEL;

@ToString
@Getter
@Setter
@SuppressWarnings("unused")
public class CaseTypeDefinition implements Serializable {
    @Serial
    private static final long serialVersionUID = 5688786015302840008L;
    private String id;
    private String description;
    private Version version;
    private String name;
    @JsonProperty("jurisdiction")
    private JurisdictionDefinition jurisdictionDefinition;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    private List<CaseEventDefinition> events = new ArrayList<>();
    private List<CaseStateDefinition> states = new ArrayList<>();
    @JsonProperty("case_fields")
    private List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
    @JsonProperty("printable_document_url")
    private String printableDocumentsUrl;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    private final List<SearchAliasField> searchAliasFields = new ArrayList<>();

    @JsonIgnore
    public String getJurisdictionId() {
        return jurisdictionDefinition.getId();
    }

    public SecurityClassification getClassificationForField(String fieldId) {
        return SecurityClassification.valueOf(caseFieldDefinitions
            .stream()
            .filter(cf -> cf.getId().equals(fieldId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("CaseFieldId %s not found in CaseType %s", fieldId, name)))
            .getSecurityLabel());
    }

    public boolean hasDraftEnabledEvent() {
        return this.events
            .stream()
            .anyMatch(caseEvent -> caseEvent.getCanSaveDraft() != null && caseEvent.getCanSaveDraft());
    }

    public boolean hasEventId(String eventId) {
        return events.stream().anyMatch(event -> event.getId().equals(eventId));
    }

    public Optional<CaseEventDefinition> findCaseEvent(String eventId) {
        return events.stream()
            .filter(event -> event.getId().equalsIgnoreCase(eventId))
            .findFirst();
    }

    public void setSearchAliasFields(List<SearchAliasField> searchAliasFields) {
        if (searchAliasFields != null) {
            this.searchAliasFields.addAll(searchAliasFields);
        }
    }

    @JsonIgnore
    public boolean isCaseFieldACollection(String caseFieldId) {
        return getCaseField(caseFieldId).map(CaseFieldDefinition::isCollectionFieldType).orElse(false);
    }

    @JsonIgnore
    public Optional<CaseFieldDefinition> getCaseField(String caseFieldId) {
        return caseFieldDefinitions.stream().filter(caseField ->
                                                        caseField.getId().equalsIgnoreCase(caseFieldId)).findFirst();
    }

    @JsonIgnore
    public Optional<CaseFieldDefinition> getComplexSubfieldDefinitionByPath(String path) {
        return CaseFieldPathUtils.getFieldDefinitionByPath(this, path);
    }

    @JsonIgnore
    public Map<String, TextNode> getLabelsFromCaseFields() {
        return getCaseFieldDefinitions()
            .stream()
            .filter(caseField -> LABEL.equals(caseField.getFieldTypeDefinition().getType()))
            .collect(Collectors
                         .toMap(CaseFieldDefinition::getId, caseField -> JsonNodeFactory.instance
                             .textNode(caseField.getLabel())));
    }
}
