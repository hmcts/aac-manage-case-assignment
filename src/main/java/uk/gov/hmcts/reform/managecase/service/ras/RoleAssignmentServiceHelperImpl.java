package uk.gov.hmcts.reform.managecase.service.ras;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ResourceNotFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ServiceException;
import uk.gov.hmcts.reform.managecase.api.payload.MultipleQueryRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.List;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENTS_CLIENT_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENT_SERVICE_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.R_A_NOT_FOUND_FOR_CASE_AND_USER;

@Slf4j
@Service
public class RoleAssignmentServiceHelperImpl implements RoleAssignmentServiceHelper {

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;

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
}
