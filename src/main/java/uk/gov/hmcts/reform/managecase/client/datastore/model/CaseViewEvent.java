package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseViewEvent {
    private Long id;

    private LocalDateTime timestamp;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("user_last_name")
    private String userLastName;

    @JsonProperty("user_first_name")
    private String userFirstName;

    private String summary;

    private String comment;

    @JsonProperty("state_name")
    private String stateName;

    @JsonProperty("state_id")
    private String stateId;

    @JsonProperty("significant_item")
    private SignificantItem significantItem;

    public static CaseViewEvent createFrom(AuditEvent event) {
        CaseViewEvent caseEvent = new CaseViewEvent();
        caseEvent.setId(event.getId());
        caseEvent.setEventId(event.getEventId());
        caseEvent.setEventName(event.getEventName());
        caseEvent.setUserId(event.getUserId());
        caseEvent.setUserLastName(event.getUserLastName());
        caseEvent.setUserFirstName(event.getUserFirstName());
        caseEvent.setSummary(event.getSummary());
        caseEvent.setComment(event.getDescription());
        caseEvent.setTimestamp(event.getCreatedDate());
        caseEvent.setStateId(event.getStateId());
        caseEvent.setStateName(event.getStateName());
        caseEvent.setSignificantItem(event.getSignificantItem());
        return caseEvent;
    }
}
