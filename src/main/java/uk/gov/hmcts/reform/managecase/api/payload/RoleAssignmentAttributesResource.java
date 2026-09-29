package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
/*
  This class tracks whether a field was present in JSON to support three states during deserialisation:

  When JSON does not have the jurisdiction field, isJurisdictionDefined() returns false.

  When JSON have the jurisdiction field, and the value of that field is null,
  isJurisdictionDefined() returns true and getJurisdiction() returns null.

  When JSON have the jurisdiction field, and the value of that field is XYZ,
  isJurisdictionDefined() returns true and getJurisdiction() returns "XYZ".
 */
public class RoleAssignmentAttributesResource implements Serializable {

    @Serial
    private static final long serialVersionUID = -490395457914850791L;

    private String jurisdiction;
    private boolean jurisdictionDefined;
    private String caseType;
    private boolean caseTypeDefined;
    private String caseId;
    private boolean caseIdDefined;
    private String region;
    private boolean regionDefined;
    private String location;
    private boolean locationDefined;
    private String contractType;
    private boolean contractTypeDefined;

    public static RoleAssignmentAttributesResourceBuilder builder() {
        return new RoleAssignmentAttributesResourceBuilder();
    }

    @JsonProperty("jurisdiction")
    public String getJurisdiction() {
        return jurisdiction;
    }

    @JsonProperty("jurisdiction")
    public void setJurisdiction(String jurisdiction) {
        jurisdictionDefined = true;
        this.jurisdiction = jurisdiction;
    }

    @JsonProperty("caseType")
    public String getCaseType() {
        return caseType;
    }

    @JsonProperty("caseType")
    public void setCaseType(String caseType) {
        caseTypeDefined = true;
        this.caseType = caseType;
    }

    @JsonProperty("caseId")
    public String getCaseId() {
        return caseId;
    }

    @JsonProperty("caseId")
    public void setCaseId(String caseId) {
        caseIdDefined = true;
        this.caseId = caseId;
    }

    @JsonProperty("region")
    public String getRegion() {
        return region;
    }

    @JsonProperty("region")
    public void setRegion(String region) {
        regionDefined = true;
        this.region = region;
    }

    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    @JsonProperty("location")
    public void setLocation(String location) {
        locationDefined = true;
        this.location = location;
    }

    @JsonProperty("contractType")
    public String getContractType() {
        return contractType;
    }

    @JsonProperty("contractType")
    public void setContractType(String contractType) {
        contractTypeDefined = true;
        this.contractType = contractType;
    }

    @JsonIgnore
    public boolean isJurisdictionDefined() {
        return jurisdictionDefined;
    }

    @JsonIgnore
    public boolean isCaseTypeDefined() {
        return caseTypeDefined;
    }

    @JsonIgnore
    public boolean isCaseIdDefined() {
        return caseIdDefined;
    }

    @JsonIgnore
    public boolean isRegionDefined() {
        return regionDefined;
    }

    @JsonIgnore
    public boolean isLocationDefined() {
        return locationDefined;
    }

    @JsonIgnore
    public boolean isContractTypeDefined() {
        return contractTypeDefined;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RoleAssignmentAttributesResource that)) {
            return false;
        }

        return jurisdictionDefined == that.jurisdictionDefined
            && caseTypeDefined == that.caseTypeDefined
            && caseIdDefined == that.caseIdDefined
            && regionDefined == that.regionDefined
            && locationDefined == that.locationDefined
            && contractTypeDefined == that.contractTypeDefined
            && Objects.equals(jurisdiction, that.jurisdiction)
            && Objects.equals(caseType, that.caseType)
            && Objects.equals(caseId, that.caseId)
            && Objects.equals(region, that.region)
            && Objects.equals(location, that.location)
            && Objects.equals(contractType, that.contractType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jurisdiction, jurisdictionDefined, caseType, caseTypeDefined, caseId, caseIdDefined,
            region, regionDefined, location, locationDefined, contractType, contractTypeDefined);
    }

    @Override
    public String toString() {
        return "RoleAssignmentAttributesResource("
            + "jurisdiction=" + getJurisdiction()
            + ", caseType=" + getCaseType()
            + ", caseId=" + getCaseId()
            + ", region=" + getRegion()
            + ", location=" + getLocation()
            + ", contractType=" + getContractType()
            + ")";
    }

    public static class RoleAssignmentAttributesResourceBuilder {

        private final RoleAssignmentAttributesResource instance = new RoleAssignmentAttributesResource();

        public RoleAssignmentAttributesResourceBuilder jurisdiction(String jurisdiction) {
            instance.setJurisdiction(jurisdiction);
            return this;
        }

        public RoleAssignmentAttributesResourceBuilder caseType(String caseType) {
            instance.setCaseType(caseType);
            return this;
        }

        public RoleAssignmentAttributesResourceBuilder caseId(String caseId) {
            instance.setCaseId(caseId);
            return this;
        }

        public RoleAssignmentAttributesResourceBuilder region(String region) {
            instance.setRegion(region);
            return this;
        }

        public RoleAssignmentAttributesResourceBuilder location(String location) {
            instance.setLocation(location);
            return this;
        }

        public RoleAssignmentAttributesResourceBuilder contractType(String contractType) {
            instance.setContractType(contractType);
            return this;
        }

        public RoleAssignmentAttributesResource build() {
            return instance;
        }
    }
}
