package uk.gov.hmcts.reform.managecase.service.ras;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ResourceNotFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ServiceException;
import uk.gov.hmcts.reform.managecase.api.payload.MultipleQueryRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newConcurrentMap;
import static org.springframework.http.HttpHeaders.ETAG;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENTS_CLIENT_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENT_SERVICE_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.R_A_NOT_FOUND_FOR_CASE_AND_USER;

@Slf4j
@Service
public class RoleAssignmentServiceHelperImpl implements RoleAssignmentServiceHelper {

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;

    // UserId as a key, Pair<ETag, RoleAssignmentResponse> as a value
    private final Map<String, Pair<String, RoleAssignmentResponse>> roleAssignments = newConcurrentMap();
    private static final String GZIP_POSTFIX = "--gzip";
    public static final String ROLE_ASSIGNMENTS_NOT_FOUND =
        "No Role Assignments found for userId=%s when getting from Role Assignment Service because of %s";

    public RoleAssignmentServiceHelperImpl(@Qualifier("restTemplate") final RestTemplate restTemplate,
                                           final ApplicationParams applicationParams,
                                           final SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
    }

    @Override
    public void deleteRoleAssignmentsByQuery(List<RoleAssignmentQuery> queryRequests) {
        try {
            final HttpEntity<Object> requestEntity = new HttpEntity<>(
                MultipleQueryRequestResource.builder().queryRequests(queryRequests).build(),
                securityUtils.authorizationHeaders()
            );

            restTemplate.exchange(
                applicationParams.amDeleteByQueryRoleAssignmentsURL(),
                HttpMethod.POST,
                requestEntity,
                Void.class
            );

        } catch (HttpStatusCodeException e) {
            log.warn("Error while deleting Role Assignments", e);
            throw mapException(e, "deleting");
        }
    }

    @Override
    public RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest) {
        try {
            final HttpEntity<Object> requestEntity =
                new HttpEntity<>(assignmentRequest, securityUtils.authorizationHeaders());

            return restTemplate.exchange(
                applicationParams.roleAssignmentBaseURL(),
                HttpMethod.POST,
                requestEntity,
                RoleAssignmentRequestResponse.class
            ).getBody();

        } catch (HttpStatusCodeException e) {
            log.warn("Error while creating Role Assignments", e);
            throw mapException(e, "creating");
        }
    }

    @Override
    public RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        try {
            final var roleAssignmentQuery = new RoleAssignmentQuery(caseIds, userIds);
            final var requestEntity = new HttpEntity<>(roleAssignmentQuery, securityUtils.authorizationHeaders());
            return restTemplate.exchange(
                applicationParams.amQueryRoleAssignmentsURL(),
                HttpMethod.POST,
                requestEntity,
                RoleAssignmentResponse.class).getBody();

        } catch (Exception exception) {
            final var resourceNotFoundException = new ResourceNotFoundException(
                String.format(R_A_NOT_FOUND_FOR_CASE_AND_USER, userIds, caseIds, exception.getMessage())
            );
            throw mapException(exception, resourceNotFoundException);
        }
    }

    @Override
    public RoleAssignmentResponse getRoleAssignments(String userId) {
        try {
            HttpHeaders headers = securityUtils.authorizationHeaders();
            addETagHeader(userId, headers);

            final HttpEntity<Object> requestEntity = new HttpEntity<>(headers);

            return getRoleAssignmentResponse(userId, requestEntity);
        } catch (Exception e) {
            log.warn("Error while retrieving Role Assignments", e);
            if (e instanceof HttpClientErrorException
                && ((HttpClientErrorException) e).getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ResourceNotFoundException(String.format(ROLE_ASSIGNMENTS_NOT_FOUND,
                                                                  userId, e.getMessage()));
            } else {
                throw mapException(e, "getting");
            }
        }
    }

    private RuntimeException mapException(Exception exception, ResourceNotFoundException resourceNotFoundException) {

        if (exception instanceof HttpClientErrorException
            && ((HttpClientErrorException) exception).getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
            return resourceNotFoundException;
        } else {
            return mapException(exception, "getting");
        }
    }

    private RuntimeException mapException(Exception exception, String processDescription) {

        if (exception instanceof HttpClientErrorException
            && ((HttpClientErrorException) exception).getStatusCode().is4xxClientError()) {
            return new BadRequestException(
                String.format(ROLE_ASSIGNMENTS_CLIENT_ERROR, processDescription, exception.getMessage()));
        } else {
            return new ServiceException(
                String.format(ROLE_ASSIGNMENT_SERVICE_ERROR, processDescription, exception.getMessage()));
        }
    }

    private RoleAssignmentResponse getRoleAssignmentResponse(String userId, HttpEntity<Object> requestEntity)
        throws URISyntaxException {

        ResponseEntity<RoleAssignmentResponse> exchange = exchangeGet(userId, requestEntity);
        log.debug("GET RoleAssignments for user={} returned response status={}", userId, exchange.getStatusCode());

        if (exchange.getStatusCode() == HttpStatus.NOT_MODIFIED && roleAssignments.containsKey(userId)) {
            return roleAssignments.get(userId).getRight();
        }
        if (exchange.getHeaders().containsKey(ETAG) && exchange.getHeaders().getETag() != null) {
            log.debug("GET RoleAssignments response contains header ETag={}", exchange.getHeaders().getETag());
            if (thereAreRoleAssignmentsInTheBody(exchange)) {
                roleAssignments.put(userId, Pair.of(getETag(exchange.getHeaders().getETag()), exchange.getBody()));
            }
        }

        return exchange.getBody();
    }

    private void addETagHeader(String userId, HttpHeaders headers) {
        if (roleAssignments.containsKey(userId)) {
            Pair<String, RoleAssignmentResponse> stringRoleAssignmentResponsePair = roleAssignments.get(userId);
            headers.setIfNoneMatch(stringRoleAssignmentResponsePair.getKey());
        }
    }

    private ResponseEntity<RoleAssignmentResponse> exchangeGet(String userId, HttpEntity<Object> requestEntity)
        throws URISyntaxException {
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("uid", ApplicationParams.encode(userId.toLowerCase()));

        final String encodedUrl = UriComponentsBuilder.fromHttpUrl(applicationParams.amGetRoleAssignmentsURL())
            .buildAndExpand(queryParams).toUriString();

        return restTemplate.exchange(new URI(encodedUrl),
                                     HttpMethod.GET, requestEntity,
                                     RoleAssignmentResponse.class);
    }

    /**
     * 'Accept-Encoding: gzip' makes the response ETag header being suffixed with the '--gzip'.
     * This method is to drop this suffix before using the ETag.
     */
    private String getETag(String etag) {
        if (etag != null && etag.endsWith(GZIP_POSTFIX + "\"")) {
            return etag.substring(0, etag.length() - GZIP_POSTFIX.length() - 1) + "\"";
        }
        return etag;
    }

    private boolean thereAreRoleAssignmentsInTheBody(ResponseEntity<RoleAssignmentResponse> exchange) {
        RoleAssignmentResponse body = exchange.getBody();
        if (body == null) {
            return false;
        }

        List<RoleAssignmentResource> roleAssignmentsInBody = body.getRoleAssignments();
        if (roleAssignmentsInBody == null) {
            return false;
        }

        return !roleAssignmentsInBody.isEmpty();
    }

}
