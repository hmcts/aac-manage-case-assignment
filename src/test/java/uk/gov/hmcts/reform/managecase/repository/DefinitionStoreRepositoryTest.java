package uk.gov.hmcts.reform.managecase.repository;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.HeaderGroupMetadata;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ES_QUERY;

class DefinitionStoreRepositoryTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "12345678";
    private static final String ANSWER_FIELD = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant,${respondent.individual.fullname}|${respondent.company.name}"
        + "|${respondent.soletrader.name}:Respondent";

    @Mock
    private DefinitionStoreApiClient definitionStoreApiClient;

    @InjectMocks
    private DefaultDefinitionStoreRepository repository;


    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    @DisplayName("Get Challenge questions")
    void shouldGetChallengeQuestions() {
        // ARRANGE
        FieldType fieldType = FieldType.builder()
            .max(null)
            .min(null)
            .id("Number")
            .type("Number")
            .build();
        ChallengeQuestion challengeQuestion = new ChallengeQuestion(CASE_TYPE_ID, 1,
                                                                    "QuestionText1",
                                                                    fieldType,
                                                                    null,
                                                                    "NoC",
                                                                    ANSWER_FIELD,
                                                                    "QuestionId1",
                                                                    null);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
            Arrays.asList(challengeQuestion));
        given(definitionStoreApiClient.challengeQuestions(anyString(), anyString())).willReturn(challengeQuestionsResult);

        // ACT
        ChallengeQuestionsResult result = repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(challengeQuestionsResult);

        verify(definitionStoreApiClient).challengeQuestions(eq(CASE_TYPE_ID), eq(CASE_ID));
    }

}
