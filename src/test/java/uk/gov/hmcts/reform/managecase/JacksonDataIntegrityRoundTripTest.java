package uk.gov.hmcts.reform.managecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributesResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleRequestResource;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonDataIntegrityRoundTripTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // This is the mapper used by the application's explicit Jackson 2 HTTP bridge.
        objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();
    }

    @Test
    void roleAssignmentRequestAndResponsePreserveAllFields() throws Exception {
        Instant created = Instant.parse("2025-01-02T03:04:05Z");
        RoleAssignmentResource role = RoleAssignmentResource.builder()
            .id("role-assignment-id")
            .actorIdType("IDAM")
            .actorId("actor-id")
            .roleType("CASE")
            .roleName("caseworker")
            .classification("PUBLIC")
            .grantType("STANDARD")
            .roleCategory("LEGAL_OPERATIONS")
            .readOnly(false)
            .beginTime(created)
            .endTime(created.plusSeconds(3600))
            .created(created)
            .authorisations(List.of("Read", "Update"))
            .attributes(RoleAssignmentAttributesResource.builder()
                .jurisdiction(Optional.of("PUBLICLAW"))
                .caseType(Optional.of("TestCase"))
                .caseId(Optional.of("case-id"))
                .region(Optional.of("1"))
                .location(Optional.of("London"))
                .contractType(Optional.of("Solicitor"))
                .build())
            .build();
        RoleRequestResource roleRequest = RoleRequestResource.builder()
            .id("request-id")
            .clientId("client-id")
            .authenticatedUserId("user-id")
            .correlationId("correlation-id")
            .assignerId("assigner-id")
            .requestType("CREATE")
            .process("case-assignment")
            .reference("reference")
            .replaceExisting(true)
            .roleAssignmentId("role-assignment-id")
            .status("CREATED")
            .created(created)
            .log("validation-rule")
            .byPassOrgDroolRule(false)
            .build();
        RoleAssignmentRequestResource request = RoleAssignmentRequestResource.builder()
            .roleRequest(roleRequest)
            .requestedRoles(List.of(role))
            .build();

        JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(request));
        JsonNode expected = objectMapper.readTree("""
            {
              "roleRequest": {
                "id": "request-id",
                "clientId": "client-id",
                "authenticatedUserId": "user-id",
                "correlationId": "correlation-id",
                "assignerId": "assigner-id",
                "requestType": "CREATE",
                "process": "case-assignment",
                "reference": "reference",
                "replaceExisting": true,
                "roleAssignmentId": "role-assignment-id",
                "status": "CREATED",
                "created": "2025-01-02T03:04:05Z",
                "log": "validation-rule",
                "byPassOrgDroolRule": false
              },
              "requestedRoles": [{
                "id": "role-assignment-id",
                "actorIdType": "IDAM",
                "actorId": "actor-id",
                "roleType": "CASE",
                "roleName": "caseworker",
                "classification": "PUBLIC",
                "grantType": "STANDARD",
                "roleCategory": "LEGAL_OPERATIONS",
                "readOnly": false,
                "beginTime": "2025-01-02T03:04:05Z",
                "endTime": "2025-01-02T04:04:05Z",
                "created": "2025-01-02T03:04:05Z",
                "authorisations": ["Read", "Update"],
                "attributes": {
                  "jurisdiction": "PUBLICLAW",
                  "caseType": "TestCase",
                  "caseId": "case-id",
                  "region": "1",
                  "location": "London",
                  "contractType": "Solicitor"
                }
              }]
            }
            """);

        assertThat(actual).isEqualTo(expected);

        RoleAssignmentRequestResource decoded = objectMapper.treeToValue(actual,
            RoleAssignmentRequestResource.class);
        assertThat(decoded).usingRecursiveComparison().isEqualTo(request);

        RoleAssignmentRequestResponse response = RoleAssignmentRequestResponse.builder()
            .roleAssignmentResponse(request)
            .build();
        JsonNode responseJson = objectMapper.readTree(objectMapper.writeValueAsString(response));
        assertThat(responseJson.get("roleAssignmentResponse")).isEqualTo(expected);
    }

    @Test
    void challengeQuestionPreservesSnakeCaseFieldsAndDerivedAnswers() throws Exception {
        JsonNode source = objectMapper.readTree("""
            {
              "case_type_id": "TestCase",
              "order": 1,
              "question_text": "What is the applicant's name?",
              "answer_field_type": {
                "id": "Text",
                "type": "Text",
                "min": null,
                "max": null,
                "regular_expression": null,
                "fixed_list_items": [],
                "complex_fields": [],
                "collection_field_type": null
              },
              "display_context_parameter": "Applicant",
              "challenge_question_id": "NoC",
              "answer_field": "${applicant.individual.fullname}|${applicant.company.name}:Applicant",
              "question_id": "QuestionId1",
              "ignore_null_fields": false
            }
            """);

        ChallengeQuestion decoded = objectMapper.treeToValue(source, ChallengeQuestion.class);

        assertThat(decoded.getCaseTypeId()).isEqualTo("TestCase");
        assertThat(decoded.getQuestionText()).isEqualTo("What is the applicant's name?");
        assertThat(decoded.getAnswerField()).isEqualTo(
            "${applicant.individual.fullname}|${applicant.company.name}:Applicant");
        assertThat(decoded.getAnswers()).hasSize(1);
        assertThat(decoded.getAnswers().get(0).getFieldIds())
            .containsExactly("applicant.individual.fullname", "applicant.company.name");
        assertThat(decoded.getAnswers().get(0).getCaseRoleId()).isEqualTo("Applicant");

        JsonNode roundTrip = objectMapper.readTree(objectMapper.writeValueAsString(decoded));
        assertThat(roundTrip).isEqualTo(source);
        assertThat(roundTrip.has("caseTypeId")).isFalse();
        assertThat(roundTrip.has("questionText")).isFalse();
    }

    @Test
    void changeOrganisationRequestPreservesUpperCamelCaseNullsAndTimestamp() throws Exception {
        DynamicListElement role = DynamicListElement.builder()
            .code("[Defendant]")
            .label("Defendant")
            .build();
        ChangeOrganisationRequest request = ChangeOrganisationRequest.builder()
            .organisationToAdd(Organisation.builder()
                .organisationID("ORG1")
                .organisationName("Organisation One")
                .build())
            .caseRoleId(DynamicList.builder().value(role).listItems(List.of(role)).build())
            .requestTimestamp(LocalDateTime.of(2025, 1, 2, 3, 4, 5))
            .approvalStatus("APPROVED")
            .createdBy("user-id")
            .build();

        JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(request));
        JsonNode expected = objectMapper.readTree("""
            {
              "OrganisationToAdd": {
                "OrganisationID": "ORG1",
                "OrganisationName": "Organisation One"
              },
              "OrganisationToRemove": null,
              "CaseRoleId": {
                "value": {"code": "[Defendant]", "label": "Defendant"},
                "list_items": [{"code": "[Defendant]", "label": "Defendant"}]
              },
              "RequestTimestamp": "2025-01-02T03:04:05",
              "ApprovalStatus": "APPROVED",
              "CreatedBy": "user-id"
            }
            """);

        assertThat(actual).isEqualTo(expected);
        ChangeOrganisationRequest decoded = objectMapper.treeToValue(actual, ChangeOrganisationRequest.class);
        assertThat(decoded.getOrganisationToAdd().getOrganisationID()).isEqualTo("ORG1");
        assertThat(decoded.getCaseRoleId().getValue().getCode()).isEqualTo("[Defendant]");
        assertThat(decoded.getRequestTimestamp()).isEqualTo(LocalDateTime.of(2025, 1, 2, 3, 4, 5));
        assertThat(decoded.getOrganisationToRemove()).isNull();
    }
}
