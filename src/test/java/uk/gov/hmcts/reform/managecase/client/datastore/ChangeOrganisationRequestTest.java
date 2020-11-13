package uk.gov.hmcts.reform.managecase.client.datastore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest.ChangeOrganisationRequestBuilder;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import javax.validation.ValidationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;

class ChangeOrganisationRequestTest {

    private ChangeOrganisationRequestBuilder requestBuilder;
    private ChangeOrganisationRequest request;

    @BeforeEach
    void setUp() {
        requestBuilder = ChangeOrganisationRequest.builder()
            .caseRoleId("someRole")
            .approvalStatus("0")
            .requestTimestamp(LocalDateTime.now())
            .organisationToAdd(new Organisation("OrgID", "OrgName"));
    }

    @Test
    void shouldValidateChangeOrganisationRequest() {
        request = requestBuilder.build();
        assertDoesNotThrow(() -> request.validateChangeOrganisationRequest());
    }

    @Test
    void shouldThrowValidationExceptionWhenCaseRoleIdIsNull() {
        request = requestBuilder.caseRoleId(null).build();

        assertThrows(ValidationException.class,
            () -> request.validateChangeOrganisationRequest(),
            CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenCaseRoleIdIsBlank() {
        request = requestBuilder.caseRoleId("").build();

        assertThrows(ValidationException.class,
            () -> request.validateChangeOrganisationRequest(),
            CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenApprovalStatusIsNull() {

        request = requestBuilder.approvalStatus(null).build();

        assertThrows(ValidationException.class,
            () -> request.validateChangeOrganisationRequest(),
            CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenRequestTimeStampIsNull() {

        request = requestBuilder.requestTimestamp(null).build();

        assertThrows(ValidationException.class,
            () -> request.validateChangeOrganisationRequest(),
            CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
        );
    }

    @Test
    void shouldThrowValidationExceptionWhenBothOrganisationFieldsAreNull() {

        request = requestBuilder.organisationToAdd(null).organisationToRemove(null).build();

        assertThrows(ValidationException.class,
            () -> request.validateChangeOrganisationRequest(),
            CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID
        );
    }
}