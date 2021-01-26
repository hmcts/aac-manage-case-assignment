package uk.gov.hmcts.reform.managecase.client.definitionstore;

import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.client.SystemUserAuthHeadersInterceptor;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class DefinitionStoreApiClientConfig {

    public static final String CHALLENGE_QUESTIONS
        = "api/display/challenge-questions/case-type/{ctid}/question-groups/{id}";

    public static final String CASE_ROLES
        = "api/data/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/roles";

    public static final String LATEST_VERSION = "api/data/case-type/{ctid}/version";

    @Bean
    public SystemUserAuthHeadersInterceptor systemUserAuthHeadersInterceptor(SecurityUtils securityUtils) {
        return new SystemUserAuthHeadersInterceptor(securityUtils);
    }
}
