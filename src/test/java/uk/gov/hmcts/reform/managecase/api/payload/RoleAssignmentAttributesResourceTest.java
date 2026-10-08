package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("RoleAssignmentAttributesResourceTest")
class RoleAssignmentAttributesResourceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("should build role assignment attributes with all fields defined")
    void shouldBuildRoleAssignmentAttributesWithAllFieldsDefined() {
        RoleAssignmentAttributesResource result = RoleAssignmentAttributesResource.builder()
            .jurisdiction("DIVORCE")
            .caseType("FinancialRemedy")
            .caseId("111111")
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED")
            .build();

        assertAll(
            () -> assertThat(result.getJurisdiction()).isEqualTo("DIVORCE"),
            () -> assertThat(result.getCaseType()).isEqualTo("FinancialRemedy"),
            () -> assertThat(result.getCaseId()).isEqualTo("111111"),
            () -> assertThat(result.getRegion()).isEqualTo("Hampshire"),
            () -> assertThat(result.getLocation()).isEqualTo("Southampton"),
            () -> assertThat(result.getContractType()).isEqualTo("SALARIED"),
            () -> assertThat(result.isJurisdictionDefined()).isTrue(),
            () -> assertThat(result.isCaseTypeDefined()).isTrue(),
            () -> assertThat(result.isCaseIdDefined()).isTrue(),
            () -> assertThat(result.isRegionDefined()).isTrue(),
            () -> assertThat(result.isLocationDefined()).isTrue(),
            () -> assertThat(result.isContractTypeDefined()).isTrue()
        );
    }

    @Test
    @DisplayName("should track null builder values as defined")
    void shouldTrackNullBuilderValuesAsDefined() {
        RoleAssignmentAttributesResource result = RoleAssignmentAttributesResource.builder()
            .jurisdiction(null)
            .caseType(null)
            .caseId(null)
            .region(null)
            .location(null)
            .contractType(null)
            .build();

        assertAll(
            () -> assertThat(result.getJurisdiction()).isNull(),
            () -> assertThat(result.getCaseType()).isNull(),
            () -> assertThat(result.getCaseId()).isNull(),
            () -> assertThat(result.getRegion()).isNull(),
            () -> assertThat(result.getLocation()).isNull(),
            () -> assertThat(result.getContractType()).isNull(),
            () -> assertThat(result.isJurisdictionDefined()).isTrue(),
            () -> assertThat(result.isCaseTypeDefined()).isTrue(),
            () -> assertThat(result.isCaseIdDefined()).isTrue(),
            () -> assertThat(result.isRegionDefined()).isTrue(),
            () -> assertThat(result.isLocationDefined()).isTrue(),
            () -> assertThat(result.isContractTypeDefined()).isTrue()
        );
    }

    @Test
    @DisplayName("should leave fields undefined when JSON properties are absent")
    void shouldLeaveFieldsUndefinedWhenJsonPropertiesAreAbsent() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            "{}",
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction()).isNull(),
            () -> assertThat(result.getCaseType()).isNull(),
            () -> assertThat(result.getCaseId()).isNull(),
            () -> assertThat(result.getRegion()).isNull(),
            () -> assertThat(result.getLocation()).isNull(),
            () -> assertThat(result.getContractType()).isNull(),
            () -> assertThat(result.isJurisdictionDefined()).isFalse(),
            () -> assertThat(result.isCaseTypeDefined()).isFalse(),
            () -> assertThat(result.isCaseIdDefined()).isFalse(),
            () -> assertThat(result.isRegionDefined()).isFalse(),
            () -> assertThat(result.isLocationDefined()).isFalse(),
            () -> assertThat(result.isContractTypeDefined()).isFalse()
        );
    }

    @Test
    @DisplayName("should track explicit null JSON properties as defined")
    void shouldTrackExplicitNullJsonPropertiesAsDefined() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            """
                {
                    "jurisdiction":null,
                    "caseType":null,
                    "caseId":null,
                    "region":null,
                    "location":null,
                    "contractType":null
                }
                """,
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction()).isNull(),
            () -> assertThat(result.getCaseType()).isNull(),
            () -> assertThat(result.getCaseId()).isNull(),
            () -> assertThat(result.getRegion()).isNull(),
            () -> assertThat(result.getLocation()).isNull(),
            () -> assertThat(result.getContractType()).isNull(),
            () -> assertThat(result.isJurisdictionDefined()).isTrue(),
            () -> assertThat(result.isCaseTypeDefined()).isTrue(),
            () -> assertThat(result.isCaseIdDefined()).isTrue(),
            () -> assertThat(result.isRegionDefined()).isTrue(),
            () -> assertThat(result.isLocationDefined()).isTrue(),
            () -> assertThat(result.isContractTypeDefined()).isTrue()
        );
    }

    @Test
    @DisplayName("should deserialise JSON properties with values as defined")
    void shouldDeserialiseJsonPropertiesWithValuesAsDefined() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            """
                {
                    "jurisdiction":"DIVORCE",
                    "caseType":"FinancialRemedy",
                    "caseId":"111111",
                    "region":"Hampshire",
                    "location":"Southampton",
                    "contractType":"SALARIED"
                }
                """,
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction()).isEqualTo("DIVORCE"),
            () -> assertThat(result.getCaseType()).isEqualTo("FinancialRemedy"),
            () -> assertThat(result.getCaseId()).isEqualTo("111111"),
            () -> assertThat(result.getRegion()).isEqualTo("Hampshire"),
            () -> assertThat(result.getLocation()).isEqualTo("Southampton"),
            () -> assertThat(result.getContractType()).isEqualTo("SALARIED"),
            () -> assertThat(result.isJurisdictionDefined()).isTrue(),
            () -> assertThat(result.isCaseTypeDefined()).isTrue(),
            () -> assertThat(result.isCaseIdDefined()).isTrue(),
            () -> assertThat(result.isRegionDefined()).isTrue(),
            () -> assertThat(result.isLocationDefined()).isTrue(),
            () -> assertThat(result.isContractTypeDefined()).isTrue()
        );
    }

    @Test
    @DisplayName("should ignore unknown JSON properties")
    void shouldIgnoreUnknownJsonProperties() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            "{\"jurisdiction\":\"DIVORCE\",\"unknown\":\"value\"}",
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction()).isEqualTo("DIVORCE"),
            () -> assertThat(result.isJurisdictionDefined()).isTrue()
        );
    }

    @Test
    @DisplayName("should serialise only non null JSON properties")
    void shouldSerialiseOnlyNonNullJsonProperties() throws JsonProcessingException {
        RoleAssignmentAttributesResource attributes = RoleAssignmentAttributesResource.builder()
            .jurisdiction("DIVORCE")
            .caseId("111111")
            .contractType(null)
            .build();

        JsonNode result = OBJECT_MAPPER.readTree(OBJECT_MAPPER.writeValueAsString(attributes));

        assertAll(
            () -> assertThat(result.get("jurisdiction").asText()).isEqualTo("DIVORCE"),
            () -> assertThat(result.get("caseId").asText()).isEqualTo("111111"),
            () -> assertThat(result.has("contractType")).isFalse(),
            () -> assertThat(result.has("jurisdictionDefined")).isFalse(),
            () -> assertThat(result.has("caseIdDefined")).isFalse()
        );
    }

    @Test
    @DisplayName("should consider resources with same values and defined fields equal")
    void shouldConsiderResourcesWithSameValuesAndDefinedFieldsEqual() {
        RoleAssignmentAttributesResource first = populatedAttributes();
        RoleAssignmentAttributesResource second = populatedAttributes();

        assertAll(
            () -> assertThat(first).isEqualTo(second),
            () -> assertThat(first).hasSameHashCodeAs(second)
        );
    }

    @Test
    @DisplayName("should not consider undefined null field equal to explicitly defined null field")
    void shouldNotConsiderUndefinedNullFieldEqualToExplicitlyDefinedNullField() {
        RoleAssignmentAttributesResource undefinedJurisdiction = new RoleAssignmentAttributesResource();
        RoleAssignmentAttributesResource definedNullJurisdiction = RoleAssignmentAttributesResource.builder()
            .jurisdiction(null)
            .build();

        assertThat(undefinedJurisdiction).isNotEqualTo(definedNullJurisdiction);
    }

    @Test
    @DisplayName("should not consider resources with different values equal")
    void shouldNotConsiderResourcesWithDifferentValuesEqual() {
        RoleAssignmentAttributesResource first = populatedAttributes();
        RoleAssignmentAttributesResource second = RoleAssignmentAttributesResource.builder()
            .jurisdiction("PROBATE")
            .caseType("FinancialRemedy")
            .caseId("111111")
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED")
            .build();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("should return descriptive string with field values")
    void shouldReturnDescriptiveStringWithFieldValues() {
        RoleAssignmentAttributesResource attributes = populatedAttributes();

        String result = attributes.toString();

        assertThat(result).isEqualTo("RoleAssignmentAttributesResource("
            + "jurisdiction=DIVORCE, caseType=FinancialRemedy, caseId=111111, "
                                         + "region=Hampshire, location=Southampton, contractType=SALARIED)");
    }

    private RoleAssignmentAttributesResource populatedAttributes() {
        return RoleAssignmentAttributesResource.builder()
            .jurisdiction("DIVORCE")
            .caseType("FinancialRemedy")
            .caseId("111111")
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED")
            .build();
    }
}
