package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import java.util.List;

@Data
@Builder
@Schema(description = "Case Access Metadata of a Case Type")
public class CaseAccessMetadataResource {

    @JsonProperty("accessGrants")
    private List<GrantType> accessGrants;

    @JsonProperty("accessProcess")
    private String accessProcess;

}
