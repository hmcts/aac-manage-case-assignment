package uk.gov.hmcts.reform.managecase.client.datastore.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaseViewActionableEvent {
    private String id;
    private String name;
    private String description;
    private Integer order;
}
