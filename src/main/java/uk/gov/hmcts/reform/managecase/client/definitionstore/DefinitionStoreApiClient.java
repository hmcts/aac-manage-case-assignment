package uk.gov.hmcts.reform.managecase.client.definitionstore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClientConfig.CHALLENGE_QUESTIONS;

@FeignClient(
    name = "definition-store-api",
    url = "${ccd.definition-store.host}",
    configuration = DefinitionStoreApiClientConfig.class
)
public interface DefinitionStoreApiClient {

    @GetMapping(value = CHALLENGE_QUESTIONS, consumes = APPLICATION_JSON_VALUE)
    ChallengeQuestionsResult challengeQuestions(@PathVariable("ctid") String caseTypeId,
                                                @PathVariable("id") String challengeQuestionId);

}
