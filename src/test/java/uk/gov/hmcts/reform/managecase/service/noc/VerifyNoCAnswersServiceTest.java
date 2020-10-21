package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NoticeOfChangeService;

import javax.validation.ValidationException;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.JUnitAssertionsShouldIncludeMessage", "PMD.DataflowAnomalyAnalysis",
    "PMD.UseConcurrentHashMap"})
class VerifyNoCAnswersServiceTest {

    @InjectMocks
    private VerifyNoCAnswersService verifyNoCAnswersService;

    @Mock
    private NoticeOfChangeService noticeOfChangeService;

    @Mock
    private ChallengeAnswerValidator challengeAnswerValidator;

    @Mock
    private PrdRepository prdRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SearchResultViewItem searchResultViewItem;
    private ChallengeQuestionsResult challengeQuestionsResult;
    private CaseViewResource caseViewResource;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);

        searchResultViewItem = createCase();
        challengeQuestionsResult = new ChallengeQuestionsResult();
        caseViewResource = new CaseViewResource();
        NoCRequestDetails details = NoCRequestDetails.builder()
            .searchResultViewItem(searchResultViewItem)
            .challengeQuestionsResult(challengeQuestionsResult)
            .caseViewResource(caseViewResource)
            .build();

        when(noticeOfChangeService.challengeQuestions("1")).thenReturn(details);
    }

    @Test
    void shouldVerifyNoCAnswersSuccessfully() {
        mockPrdResponse("ORGID1");
        when(challengeAnswerValidator.getMatchingCaseRole(any(), any(), any())).thenReturn("[Defendant]");

        VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest("1", emptyList());

        NoCRequestDetails result = verifyNoCAnswersService.verifyNoCAnswers(request);

        assertAll(
            () -> assertThat(result.getOrganisationPolicy().getOrgPolicyCaseAssignedRole(), is("[Defendant]")),
            () -> assertThat(result.getOrganisationPolicy().getOrgPolicyReference(), is("DefendantPolicy")),
            () -> assertThat(result.getOrganisationPolicy().getOrganisation().getOrganisationID(), is("QUK822NA")),
            () -> assertThat(result.getOrganisationPolicy().getOrganisation().getOrganisationName(), is("SomeOrg")),
            () -> assertThat(result.getSearchResultViewItem(), is(searchResultViewItem)),
            () -> assertThat(result.getChallengeQuestionsResult(), is(challengeQuestionsResult)),
            () -> assertThat(result.getCaseViewResource(), is(caseViewResource))
        );
    }

    @Test
    void shouldErrorWhenIdentifiedCaseRoleDoesNotExistOnCase() {
        when(challengeAnswerValidator.getMatchingCaseRole(any(), any(), any()))
            .thenReturn("[OtherRole]");

        VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest("1", emptyList());

        ValidationException exception = assertThrows(ValidationException.class, () ->
            verifyNoCAnswersService.verifyNoCAnswers(request));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("No OrganisationPolicy exists on the case for the case role '[OtherRole]'"))
        );
    }

    @Test
    void shouldErrorWhenRequestingUserIsInSameOrganisationAsIdentifiedOrgPolicy() {
        mockPrdResponse("QUK822NA");
        when(challengeAnswerValidator.getMatchingCaseRole(any(), any(), any()))
            .thenReturn("[Defendant]");

        VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest("1", emptyList());

        ValidationException exception = assertThrows(ValidationException.class, () ->
            verifyNoCAnswersService.verifyNoCAnswers(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is("The requestor has answered questions uniquely identifying"
                + " a litigant that they are already representing"))
        );
    }

    private void mockPrdResponse(String organisationId) {
        FindUsersByOrganisationResponse prdOrgResponse =
            new FindUsersByOrganisationResponse(emptyList(), organisationId);
        when(prdRepository.findUsersByOrganisation()).thenReturn(prdOrgResponse);
    }

    private SearchResultViewItem createCase() throws JsonProcessingException {
        Map<String, JsonNode> fields = objectMapper.readValue(caseDataString(),
            new TypeReference<Map<String, JsonNode>>() {});
        return new SearchResultViewItem("1", fields, fields);
    }

    private String caseDataString() {
        return "{\n"
            + "    \"DateField\": null,\n"
            + "    \"TextField\": \"TextFieldValue\",\n"
            + "    \"EmailField\": \"aca72@gmail.com\",\n"
            + "    \"NumberField\": \"123\",\n"
            + "    \"OrganisationPolicyField1\": {\n"
            + "        \"Organisation\": {\n"
            + "            \"OrganisationID\": \"QUK822NA\",\n"
            + "            \"OrganisationName\": \"SomeOrg\"\n"
            + "        },\n"
            + "        \"OrgPolicyReference\": \"DefendantPolicy\",\n"
            + "        \"OrgPolicyCaseAssignedRole\": \"[Defendant]\"\n"
            + "    },\n"
            + "    \"OrganisationPolicyField2\": {\n"
            + "        \"Organisation\": {\n"
            + "            \"OrganisationID\": null,\n"
            + "            \"OrganisationName\": null\n"
            + "        },\n"
            + "        \"OrgPolicyReference\": \"ClaimantPolicy\",\n"
            + "        \"OrgPolicyCaseAssignedRole\": \"[Claimant]\"\n"
            + "    }\n"
            + "}";
    }
}