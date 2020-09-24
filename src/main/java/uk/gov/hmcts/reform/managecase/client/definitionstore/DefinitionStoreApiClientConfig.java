package uk.gov.hmcts.reform.managecase.client.definitionstore;

import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.client.SystemUserAuthHeadersInterceptor;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class DefinitionStoreApiClientConfig {

    public static final String CHALLENGE_QUESTIONS
        = "api/display/challenge-questions/case-type/{ctid}/question-groups/{id}";

    @Bean
    public SystemUserAuthHeadersInterceptor systemUserAuthHeadersInterceptor(SecurityUtils securityUtils) {
        return new SystemUserAuthHeadersInterceptor(securityUtils);
    }
}
