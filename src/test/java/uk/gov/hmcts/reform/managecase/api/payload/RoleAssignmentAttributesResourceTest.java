package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("RoleAssignmentAttributesResourceTest")
@SuppressWarnings("PMD.TooManyMethods")
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
            () -> assertThat(result.getJurisdiction(), is("DIVORCE")),
            () -> assertThat(result.getCaseType(), is("FinancialRemedy")),
            () -> assertThat(result.getCaseId(), is("111111")),
            () -> assertThat(result.getRegion(), is("Hampshire")),
            () -> assertThat(result.getLocation(), is("Southampton")),
            () -> assertThat(result.getContractType(), is("SALARIED")),
            () -> assertThat(result.isJurisdictionDefined(), is(true)),
            () -> assertThat(result.isCaseTypeDefined(), is(true)),
            () -> assertThat(result.isCaseIdDefined(), is(true)),
            () -> assertThat(result.isRegionDefined(), is(true)),
            () -> assertThat(result.isLocationDefined(), is(true)),
            () -> assertThat(result.isContractTypeDefined(), is(true))
        );
    }

    @Test
    @DisplayName("should leave fields undefined when JSON properties are absent")
    void shouldLeaveFieldsUndefinedWhenJsonPropertiesAreAbsent() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue("{}",
            RoleAssignmentAttributesResource.class);

        assertAll(
            () -> assertThat(result.getJurisdiction(), is(nullValue())),
            () -> assertThat(result.getCaseType(), is(nullValue())),
            () -> assertThat(result.getCaseId(), is(nullValue())),
            () -> assertThat(result.getRegion(), is(nullValue())),
            () -> assertThat(result.getLocation(), is(nullValue())),
            () -> assertThat(result.getContractType(), is(nullValue())),
            () -> assertThat(result.isJurisdictionDefined(), is(false)),
            () -> assertThat(result.isCaseTypeDefined(), is(false)),
            () -> assertThat(result.isCaseIdDefined(), is(false)),
            () -> assertThat(result.isRegionDefined(), is(false)),
            () -> assertThat(result.isLocationDefined(), is(false)),
            () -> assertThat(result.isContractTypeDefined(), is(false))
        );
    }

    @Test
    @DisplayName("should track explicit null JSON properties as defined")
    void shouldTrackExplicitNullJsonPropertiesAsDefined() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            "{"
                + "\"jurisdiction\":null,"
                + "\"caseType\":null,"
                + "\"caseId\":null,"
                + "\"region\":null,"
                + "\"location\":null,"
                + "\"contractType\":null"
                + "}",
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction(), is(nullValue())),
            () -> assertThat(result.getCaseType(), is(nullValue())),
            () -> assertThat(result.getCaseId(), is(nullValue())),
            () -> assertThat(result.getRegion(), is(nullValue())),
            () -> assertThat(result.getLocation(), is(nullValue())),
            () -> assertThat(result.getContractType(), is(nullValue())),
            () -> assertThat(result.isJurisdictionDefined(), is(true)),
            () -> assertThat(result.isCaseTypeDefined(), is(true)),
            () -> assertThat(result.isCaseIdDefined(), is(true)),
            () -> assertThat(result.isRegionDefined(), is(true)),
            () -> assertThat(result.isLocationDefined(), is(true)),
            () -> assertThat(result.isContractTypeDefined(), is(true))
        );
    }

    @Test
    @DisplayName("should deserialise JSON properties with values as defined")
    void shouldDeserialiseJsonPropertiesWithValuesAsDefined() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            "{"
                + "\"jurisdiction\":\"DIVORCE\","
                + "\"caseType\":\"FinancialRemedy\","
                + "\"caseId\":\"111111\","
                + "\"region\":\"Hampshire\","
                + "\"location\":\"Southampton\","
                + "\"contractType\":\"SALARIED\""
                + "}",
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction(), is("DIVORCE")),
            () -> assertThat(result.getCaseType(), is("FinancialRemedy")),
            () -> assertThat(result.getCaseId(), is("111111")),
            () -> assertThat(result.getRegion(), is("Hampshire")),
            () -> assertThat(result.getLocation(), is("Southampton")),
            () -> assertThat(result.getContractType(), is("SALARIED")),
            () -> assertThat(result.isJurisdictionDefined(), is(true)),
            () -> assertThat(result.isCaseTypeDefined(), is(true)),
            () -> assertThat(result.isCaseIdDefined(), is(true)),
            () -> assertThat(result.isRegionDefined(), is(true)),
            () -> assertThat(result.isLocationDefined(), is(true)),
            () -> assertThat(result.isContractTypeDefined(), is(true))
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
            () -> assertThat(result.getJurisdiction(), is("DIVORCE")),
            () -> assertThat(result.isJurisdictionDefined(), is(true))
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
            () -> assertThat(result.get("jurisdiction").asText(), is("DIVORCE")),
            () -> assertThat(result.get("caseId").asText(), is("111111")),
            () -> assertThat(result.has("contractType"), is(false)),
            () -> assertThat(result.has("jurisdictionDefined"), is(false)),
            () -> assertThat(result.has("caseIdDefined"), is(false))
        );
    }

    @Test
    @DisplayName("should consider resources with same values and defined fields equal")
    void shouldConsiderResourcesWithSameValuesAndDefinedFieldsEqual() {
        RoleAssignmentAttributesResource first = populatedAttributes();
        RoleAssignmentAttributesResource second = populatedAttributes();

        assertAll(
            () -> assertThat(first, is(second)),
            () -> assertThat(first.hashCode(), is(second.hashCode()))
        );
    }

    @Test
    @DisplayName("should not consider undefined null field equal to explicitly defined null field")
    void shouldNotConsiderUndefinedNullFieldEqualToExplicitlyDefinedNullField() {
        RoleAssignmentAttributesResource undefinedJurisdiction = new RoleAssignmentAttributesResource();
        RoleAssignmentAttributesResource definedNullJurisdiction = RoleAssignmentAttributesResource.builder()
            .jurisdiction(null)
            .build();

        assertThat(undefinedJurisdiction, is(not(definedNullJurisdiction)));
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

        assertThat(first, is(not(second)));
    }

    @Test
    @DisplayName("should return descriptive string with field values")
    void shouldReturnDescriptiveStringWithFieldValues() {
        RoleAssignmentAttributesResource attributes = populatedAttributes();

        String result = attributes.toString();

        assertThat(result, is("RoleAssignmentAttributesResource("
            + "jurisdiction=DIVORCE, caseType=FinancialRemedy, caseId=111111, "
            + "region=Hampshire, location=Southampton, contractType=SALARIED)"));
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



