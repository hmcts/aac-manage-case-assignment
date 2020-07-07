package uk.gov.hmcts.reform.managecase.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.DataStoreProxyUrlException;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.ResponseEntity.ok;
import static uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter.AUTHORISATION;
import static uk.gov.hmcts.reform.managecase.client.ProxySystemAuthHeadersInterceptor.SERVICE_AUTHORIZATION;

@Slf4j
@RestController
@RequestMapping("/ccd")
public class ProxyDataStoreController {

    // TODO: use and move it to application config
    public static List<String> whitelistedUrls = Arrays.asList("", "");

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ApplicationParams applicationParams;
    @Autowired
    private final SecurityUtils securityUtils;

    public ProxyDataStoreController(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @RequestMapping(path = "/**")
    @ResponseBody
    public ResponseEntity<String> proxyRest(@RequestBody String body, HttpMethod method, HttpServletRequest request,
                                            @RequestHeader Map<String, String> headers)
        throws URISyntaxException {

        // TODO: do a regex check
        if (request.getRequestURI().startsWith("/ccd/searchCases")) {
            String systemUserToken = securityUtils.getSystemUserToken();

            MultiValueMap<String, String> headers1 = new LinkedMultiValueMap<>();
            // @throws UnsupportedOperationException because adding headers is not supported
            headers.forEach(headers1::add);

            // TODO: add X-Forwarded: host - check in dm-store
            headers1.remove(AUTHORIZATION);
            headers1.remove(SERVICE_AUTHORIZATION);
//
            headers1.add(AUTHORIZATION, systemUserToken);
            headers1.add(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());

            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers1);

            URI uri = new URI(applicationParams.getDateStoreHost() + request.getRequestURI().substring("/ccd".length()));

//            return ok("Welcome to manage-case-assignment ");
            ResponseEntity<String> response = restTemplate.exchange(uri, method, requestEntity, String.class);
            return response;
        } else {
            throw new DataStoreProxyUrlException("Invalid url");
        }

    }

    @RequestMapping(path = "/**", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> proxyRest(HttpMethod method, HttpServletRequest request)
        throws URISyntaxException {

        if (request.getRequestURI().contains("/ccd/health")) {

            URI uri = new URI(applicationParams.getDateStoreHost() + request.getRequestURI().substring("/ccd".length()));

            return restTemplate.exchange(uri, method, null, String.class);
        } else {
            throw new DataStoreProxyUrlException("Invalid url");
        }

    }
}
