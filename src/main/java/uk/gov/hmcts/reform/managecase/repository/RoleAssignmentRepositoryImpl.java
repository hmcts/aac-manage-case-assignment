package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ResourceNotFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ServiceException;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.List;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENTS_CLIENT_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENT_SERVICE_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.R_A_NOT_FOUND_FOR_CASE_AND_USER;

@Service
public class RoleAssignmentRepositoryImpl implements RoleAssignmentRepository {

    private final RestTemplate restTemplate;
    private final ApplicationParams applicationParams;
    private final SecurityUtils securityUtils;

    public RoleAssignmentRepositoryImpl(@Qualifier("restTemplate") final RestTemplate restTemplate,
                                        final ApplicationParams applicationParams,
                                        final SecurityUtils securityUtils) {
        this.restTemplate = restTemplate;
        this.applicationParams = applicationParams;
        this.securityUtils = securityUtils;
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
            && ((HttpClientErrorException) exception).getRawStatusCode() == HttpStatus.NOT_FOUND.value()) {
            return resourceNotFoundException;
        } else if (exception instanceof HttpClientErrorException
            && HttpStatus.valueOf(((HttpClientErrorException) exception).getRawStatusCode()).is4xxClientError()) {
            return new BadRequestException(String.format(ROLE_ASSIGNMENTS_CLIENT_ERROR, exception.getMessage()));
        } else {
            return new ServiceException(String.format(ROLE_ASSIGNMENT_SERVICE_ERROR, exception.getMessage()));
        }
    }
}
