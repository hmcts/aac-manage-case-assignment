package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.client.SystemUserAuthHeadersInterceptor;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class DataStoreApiClientConfig {

    public static final String CASES_WITH_ID = "/cases/{caseId}";

    public static final String SEARCH_CASES = "/searchCases";
    public static final String INTERNAL_SEARCH_CASES = "/internal/searchCases";
    public static final String CASE_USERS = "/case-users";
    public static final String INTERNAL_CASES = "/internal/cases/{caseId}";
    public static final String START_EVENT_TRIGGER = "internal/" + CASES_WITH_ID + "/event-triggers/{eventId}";
    public static final String SUBMIT_EVENT_FOR_CASE = CASES_WITH_ID + "/events";

    @Bean
    public SystemUserAuthHeadersInterceptor systemUserAuthHeadersInterceptor(SecurityUtils securityUtils) {
        return new SystemUserAuthHeadersInterceptor(securityUtils);
    }
}
