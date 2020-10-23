package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.HeaderGroupMetadata;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.GET_NOC_QUESTIONS;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternalES;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetChallengeQuestions;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap",
    "PMD.DataflowAnomalyAnalysis", "squid:S100", "squid:S1192"})
public class NoticeOfChangeControllerIT {

    private static final String CASE_ID = "1588234985453946";
    private static final String CASE_TYPE_ID = "caseType";
    private static final String JURISDICTION = "Jurisdiction";

    private static final String RAW_QUERY = "{\"query\":{\"bool\":{\"filter\":{\"term\":{\"reference\":%s}}}}}";
    private static final String ES_QUERY = String.format(RAW_QUERY, CASE_ID);
    private final Map<String, JsonNode> caseFields = new HashMap<>();
    private final List<SearchResultViewItem> viewItems = new ArrayList<>();
    private static final String ANSWER_FIELD_APPLICANT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant";


    @Nested
    @DisplayName("GET /noc/noc-questions")
    class GetNoticeOfChangeQuestions extends BaseTest {

        @Autowired
        private MockMvc mockMvc;

        @DisplayName("Successfully return NoC questions for case id")
        @Test
        void shouldGetNoCQuestions_forAValidRequest() throws Exception {
            CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
            caseViewActionableEvent.setId("NOC");
            CaseViewResource caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[] {caseViewActionableEvent});
            CaseViewType caseViewType = new CaseViewType();
            caseViewType.setId(CASE_TYPE_ID);
            CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
            caseViewJurisdiction.setId(JURISDICTION);
            caseViewType.setJurisdiction(caseViewJurisdiction);
            caseViewResource.setCaseType(caseViewType);
            stubGetCaseInternal(CASE_ID, caseViewResource);


            SearchResultViewHeader searchResultViewHeader = new SearchResultViewHeader();
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST);
            searchResultViewHeader.setCaseFieldTypeDefinition(fieldTypeDefinition);
            searchResultViewHeader.setCaseFieldId("changeOrg");
            SearchResultViewHeaderGroup correctHeader = new SearchResultViewHeaderGroup(
                new HeaderGroupMetadata(JURISDICTION, CASE_TYPE_ID),
                Arrays.asList(searchResultViewHeader),
                Arrays.asList("111", "222")
            );
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readValue("{\n"
                                                      + "  \"OrganisationPolicy1\": {\n"
                                                      + "    \"OrgPolicyCaseAssignedRole\": \"Applicant\",\n"
                                                      + "    \"OrgPolicyReference\": \"Reference\",\n"
                                                      + "    \"Organisation\": {\n"
                                                      + "      \"OrganisationID\": \"QUK822N\",\n"
                                                      + "      \"OrganisationName\": \"CCD Solicitors Limited\"\n"
                                                      + "    }\n"
                                                      + "  }\n"
                                                      + "}", JsonNode.class);

            caseFields.put(PREDEFINED_COMPLEX_ORGANISATION_POLICY, actualObj);
            SearchResultViewItem item = new SearchResultViewItem("CaseId", caseFields, caseFields);
            viewItems.add(item);
            List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
            headers.add(correctHeader);
            List<SearchResultViewItem> cases = new ArrayList<>();
            cases.add(item);
            Long total = 3L;
            CaseSearchResultView caseSearchResultView = new CaseSearchResultView(headers, cases, total);
            CaseSearchResultViewResource resource = new CaseSearchResultViewResource(caseSearchResultView);
            stubGetCaseInternalES(CASE_TYPE_ID, ES_QUERY, resource);

            FieldType fieldType = FieldType.builder()
                .regularExpression("regular expression")
                .max(null)
                .min(null)
                .id("Number")
                .type("Number")
                .build();
            ChallengeQuestion challengeQuestion = new ChallengeQuestion(CASE_TYPE_ID, 1,
                                                                        "questionText",
                                                                        fieldType,
                                                                        null,
                                                                        "NoC",
                                                                        ANSWER_FIELD_APPLICANT,
                                                                        "QuestionId1",
                                                                        null);
            ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                Arrays.asList(challengeQuestion));
            stubGetChallengeQuestions(CASE_TYPE_ID, "NoCChallenge", challengeQuestionsResult);

            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                                     .queryParam("case_id", CASE_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))

                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].case_type_id", is(CASE_TYPE_ID)))
                .andExpect(jsonPath("$.questions[0].order", is(1)))
                .andExpect(jsonPath("$.questions[0].question_text", is("questionText")))
                .andExpect(jsonPath("$.questions[0].challenge_question_id", is("NoC")));

        }

        @DisplayName("Must return 400 bad request response if caseIds are missing in GetAssignments request")
        @Test
        void shouldReturn400_whenCaseIdsAreNotPassedForGetNoCQuestionsApi() throws Exception {

            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                                     .queryParam("case_id", ""))
                .andExpect(status().isBadRequest());
        }

    }

}
