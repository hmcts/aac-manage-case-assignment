package uk.gov.hmcts.reform.managecase.api.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
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
import static org.springframework.http.ResponseEntity.status;

@Slf4j
@RestController
@RequestMapping("/ccd")
public class ProxyDataStoreController {

    // TODO: use and move it to application config
    public static List<String> whitelistedUrls = Arrays.asList("", "");
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

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
                                            @RequestHeader Map<String, String> requestHeaders)
        throws URISyntaxException {

        // TODO: do a regex check
        if (request.getRequestURI().startsWith("/ccd/searchCases")) {
            String systemUserToken = securityUtils.getSystemUserToken();

            MultiValueMap<String, String> headers = updateHeaders(requestHeaders, systemUserToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

            try {
                return restTemplate.exchange(prepareUri(request), method, requestEntity, String.class);
            } catch (HttpClientErrorException e) {
                return status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
            }

        } else {
            throw new DataStoreProxyUrlException("Invalid url");
        }
    }

    @RequestMapping(path = "/**", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> proxyRest(HttpMethod method, HttpServletRequest request)
        throws URISyntaxException {

        if (request.getRequestURI().contains("/ccd/health")) {
            return restTemplate.exchange(prepareUri(request), method, null, String.class);
        } else {
            throw new DataStoreProxyUrlException("Invalid url");
        }
    }

    private MultiValueMap<String, String> updateHeaders(Map<String, String> requestHeaders, String systemUserToken) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        requestHeaders.forEach(headers::add);

        headers.remove(AUTHORIZATION);
        headers.remove(SERVICE_AUTHORIZATION);

        headers.add(AUTHORIZATION, systemUserToken);
        headers.add(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());

        return headers;
    }

    private URI prepareUri(HttpServletRequest request) throws URISyntaxException {
        return new URI(applicationParams.getDateStoreHost() + request.getRequestURI().substring("/ccd".length()));
    }
}
