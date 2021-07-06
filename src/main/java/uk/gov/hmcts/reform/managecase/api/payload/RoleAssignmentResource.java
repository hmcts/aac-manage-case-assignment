package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleAssignmentResource implements Serializable  {

    private static final long serialVersionUID = -8765493031023866825L;

    private String id;
    private String actorIdType;
    private String actorId;
    private String roleType;
    private String roleName;
    private String classification;
    private String grantType;
    private String roleCategory;
    private Boolean readOnly;
    private Instant beginTime;
    private Instant endTime;
    private Instant created;
    private List<String> authorisations;
    private RoleAssignmentAttributesResource attributes;
}
