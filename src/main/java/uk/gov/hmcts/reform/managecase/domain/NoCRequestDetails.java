package uk.gov.hmcts.reform.managecase.domain;

import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

@Data
@Builder(builderClassName = "Builder")
public class NoCRequestDetails {

    private CaseViewResource caseViewResource;
    private OrganisationPolicy organisationPolicy;
    private SearchResultViewItem searchResultViewItem;
    private ChallengeQuestionsResult challengeQuestionsResult;

    public VerifyNoCAnswersResponse toVerifyNoCAnswersResponse(String status) {
        return new VerifyNoCAnswersResponse(status, getOrganisationPolicy().getOrganisation());
    }
}
