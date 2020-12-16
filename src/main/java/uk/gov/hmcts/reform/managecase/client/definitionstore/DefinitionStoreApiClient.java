package uk.gov.hmcts.reform.managecase.client.definitionstore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClientConfig.CASE_ROLES;
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

    /**
     * Gets the caseRoles for a given caseType.
     *
     * @param userId Not used in the definition store. Can be any value.
     * @param jurisdiction Not used in the definition store. Can be any value.
     * @param caseTypeId Case id.
     *
     * @return List of case roles for a given CaseType.
     */
    @GetMapping(value = CASE_ROLES, consumes = APPLICATION_JSON_VALUE)
    List<CaseRole> caseRoles(@PathVariable("uid") String userId,
                             @PathVariable("jid") String jurisdiction,
                             @PathVariable("ctid") String caseTypeId);

}
