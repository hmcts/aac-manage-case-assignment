package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ApiModel("SubmitCallbackResponse")
public class SubmitCallbackResponse {

    @JsonProperty("confirmation_header")
    private String confirmationHeader;

    @JsonProperty("confirmation_body")
    private String confirmationBody;

}
