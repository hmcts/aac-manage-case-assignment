package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

import java.io.Serializable;
import java.util.Date;

@Schema
public class Version implements Serializable {

    private static final long serialVersionUID = 6196146295016140921L;
    private Integer number;
    private Date liveFrom;
    private Date liveUntil;

    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Sequantial version number")
    @JsonProperty("number")
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    /**
     * Date and time from when this version is valid from.
     **/
    @Schema(requiredMode = RequiredMode.REQUIRED, description = "Date and time from when this version is valid from")
    @JsonProperty("live_from")
    public Date getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(Date liveFrom) {
        this.liveFrom = liveFrom;
    }

    /**
     * Date and time this version is to be retired.
     **/
    @Schema(description = "Date and time this version is to be retired")
    @JsonProperty("live_until")
    public Date getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(Date liveUntil) {
        this.liveUntil = liveUntil;
    }
}
