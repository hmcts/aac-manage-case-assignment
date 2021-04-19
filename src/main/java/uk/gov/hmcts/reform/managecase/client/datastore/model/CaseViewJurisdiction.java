package uk.gov.hmcts.reform.managecase.client.datastore.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseViewJurisdiction {
    private String id;
    private String name;
    private String description;

    public CaseViewJurisdiction() {
        // default constructor
    }

    private CaseViewJurisdiction(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static CaseViewJurisdiction createFrom(JurisdictionDefinition jurisdictionDefinition) {
        return new CaseViewJurisdiction(jurisdictionDefinition.getId(),
            jurisdictionDefinition.getName(),
            jurisdictionDefinition.getDescription());
    }
}
