package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentResource implements Serializable  {

    private static final long serialVersionUID = -8765493031023866825L;

    @JsonProperty("id")
    private String id;
    @JsonProperty("actorIdType")
    private String actorIdType;
    @JsonProperty("actorId")
    private String actorId;
    @JsonProperty("roleType")
    private String roleType;
    @JsonProperty("roleName")
    private String roleName;
    @JsonProperty("classification")
    private String classification;
    @JsonProperty("grantType")
    private String grantType;
    @JsonProperty("roleCategory")
    private String roleCategory;
    @JsonProperty("readOnly")
    private Boolean readOnly;
    @JsonProperty("beginTime")
    @JsonFormat(timezone = "UTC", shape = JsonFormat.Shape.STRING)
    private Instant beginTime;
    @JsonProperty("endTime")
    @JsonFormat(timezone = "UTC", shape = JsonFormat.Shape.STRING)
    private Instant endTime;
    @JsonProperty("created")
    @JsonFormat(timezone = "UTC", shape = JsonFormat.Shape.STRING)
    private Instant created;
    @JsonProperty("authorisations")
    private List<String> authorisations;
    @JsonProperty("attributes")
    private RoleAssignmentAttributesResource attributes;
}
