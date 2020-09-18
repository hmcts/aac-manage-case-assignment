package uk.gov.hmcts.reform.managecase.client.definitionstore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.INTERNAL_CASES;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.SEARCH_CASES;
import static uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClientConfig.CHALLENGE_QUESTIONS;

@FeignClient(
    name = "definition-store-api",
    url = "${ccd.definition-store.host}",
    configuration = DefinitionStoreApiClientConfig.class
)
public interface DefinitionStoreApiClient {

    @GetMapping(value = CHALLENGE_QUESTIONS, consumes = APPLICATION_JSON_VALUE)
    ChallengeQuestionsResult challengeQuestions(@RequestParam("ctid") String caseTypeId,
                                                @RequestParam("id") String challengeQuestionId);

}
