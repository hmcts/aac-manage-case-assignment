package uk.gov.hmcts.reform.managecase.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonObjectMapperConfigTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");
    private static final String DOWNSTREAM_DECODER_FACTORY =
        "src/main/java/uk/gov/hmcts/reform/managecase/client/DownstreamResponseDecoderFactory.java";

    private static final Pattern UNKNOWN_PROPERTY_FEATURE_REFERENCE = Pattern.compile("FAIL_ON_UNKNOWN_PROPERTIES");
    private static final Pattern UNKNOWN_PROPERTY_BYPASS_REFERENCE = Pattern.compile(
        "ignoreUnknown\\s*=|JsonAnySetter|DeserializationProblemHandler|handleUnknownProperty"
    );

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();
    }

    @Test
    void shouldRejectUnknownPropertiesByDefault() {
        assertThat(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isTrue();
    }

    @Test
    void shouldSerialiseProblemDetailWithoutNullPropertiesField() throws Exception {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Required parameter 'case_ids' is not present."
        );

        String json = objectMapper.writeValueAsString(problemDetail);

        assertThat(json).contains("\"status\":400");
        assertThat(json).contains("\"detail\":\"Required parameter 'case_ids' is not present.\"");
        assertThat(json).doesNotContain("properties");
    }

    @Test
    void shouldKeepAllObjectMapperBeansStrictInSpringContext() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            JacksonObjectMapperConfig.class)) {

            assertThat(context.getBeansOfType(ObjectMapper.class))
                .isNotEmpty()
                .allSatisfy((name, mapper) -> assertThat(mapper.isEnabled(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                    .as("ObjectMapper bean '%s' must reject unknown JSON properties", name)
                    .isTrue());
        }
    }

    @Test
    void shouldOnlyAllowUnknownPropertyFailureToBeDisabledForDownstreamProviderResponses() throws IOException {
        assertThat(findMainSourceMatches(UNKNOWN_PROPERTY_FEATURE_REFERENCE))
            .as("Only downstream provider response decoding may tolerate additive fields")
            .allSatisfy(match -> assertThat(match).startsWith(DOWNSTREAM_DECODER_FACTORY + ":"));
    }

    @Test
    void shouldNotAllowUnknownPropertyBypassesInMainSource() throws IOException {
        assertThat(findMainSourceMatches(UNKNOWN_PROPERTY_BYPASS_REFERENCE))
            .as("Unknown JSON property bypasses must not be added without an explicit security review")
            .isEmpty();
    }

    @Test
    void shouldRejectUnknownPropertiesOnApiRequestPayloads() {
        String json = """
            {
              "case_type_id": "PROBATE-TEST",
              "case_id": "1588234985453946",
              "assignee_id": "ecb5edf4-2f5f-4031-a0ec",
              "unexpected": "value"
            }
            """;

        assertThatThrownBy(() -> objectMapper.readValue(json, CaseAssignmentRequest.class))
            .isInstanceOf(UnrecognizedPropertyException.class)
            .hasMessageContaining("unexpected");
    }

    @Test
    void shouldRejectUnknownPropertiesOnChangeOrganisationRequest() {
        String json = """
            {
              "ApprovalStatus": "PENDING",
              "unexpected": "value"
            }
            """;

        assertThatThrownBy(() -> objectMapper.readValue(json, ChangeOrganisationRequest.class))
            .isInstanceOf(UnrecognizedPropertyException.class)
            .hasMessageContaining("unexpected");
    }

    @Test
    void shouldRejectUnknownPropertiesOnNestedCaseDetails() {
        String json = """
            {
              "id": "1588234985453946",
              "case_type_id": "FT_NoCCaseType",
              "supplementary_data": null,
              "callback_response_status_code": null,
              "unexpected": "value"
            }
            """;

        assertThatThrownBy(() -> objectMapper.readValue(json, CaseDetails.class))
            .isInstanceOf(UnrecognizedPropertyException.class)
            .hasMessageContaining("unexpected");
    }

    @Test
    void shouldDeserialiseRequestModelsWithoutBroadSetters() throws Exception {
        SubmittedChallengeAnswer answer = objectMapper.readValue("""
            {
              "question_id": "Question1",
              "value": "Answer"
            }
            """, SubmittedChallengeAnswer.class);

        RoleAssignmentResource roleAssignment = objectMapper.readValue("""
            {
              "roleName": "caseworker",
              "attributes": {
                "caseId": "1588234985453946"
              }
            }
            """, RoleAssignmentResource.class);

        assertThat(answer.getQuestionId()).isEqualTo("Question1");
        assertThat(answer.getValue()).isEqualTo("Answer");
        assertThat(roleAssignment.getRoleName()).isEqualTo("caseworker");
        assertThat(roleAssignment.getAttributes().getCaseId()).contains("1588234985453946");
    }

    @Test
    void shouldRejectUnknownPropertiesOnRoleAssignmentResource() {
        String json = """
            {
              "roleName": "caseworker",
              "unexpected": "value"
            }
            """;

        assertThatThrownBy(() -> objectMapper.readValue(json, RoleAssignmentResource.class))
            .isInstanceOf(UnrecognizedPropertyException.class)
            .hasMessageContaining("unexpected");
    }

    private List<String> findMainSourceMatches(Pattern pattern) throws IOException {
        List<String> matches = new ArrayList<>();

        try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(source);
                for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                    String line = lines.get(lineIndex);
                    if (pattern.matcher(line).find()) {
                        matches.add(source + ":" + (lineIndex + 1) + ": " + line.trim());
                    }
                }
            }
        }

        return matches;
    }
}
