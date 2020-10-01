package uk.gov.hmcts.reform.managecase.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

public class SystemUserAuthHeadersInterceptor implements RequestInterceptor {

    private final SecurityUtils securityUtils;

    public SystemUserAuthHeadersInterceptor(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(AUTHORIZATION, securityUtils.getSystemUserToken());
        template.header(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());
        template.header("experimental", "experimental");
    }
}
