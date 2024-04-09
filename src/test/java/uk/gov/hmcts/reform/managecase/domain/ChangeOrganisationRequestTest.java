package uk.gov.hmcts.reform.managecase.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest.ChangeOrganisationRequestBuilder;

import jakarta.validation.ValidationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;

class ChangeOrganisationRequestTest {

    private ChangeOrganisationRequestBuilder requestBuilder;
    private ChangeOrganisationRequest request;

    @BeforeEach
    void setUp() {

        DynamicListElement dynamicListElement = DynamicListElement.builder().code("code").label("label").build();
        DynamicList dynamicList = DynamicList.builder()
            .value(dynamicListElement)
            .listItems(List.of(dynamicListElement))
            .build();
        requestBuilder = ChangeOrganisationRequest.builder()
            .caseRoleId(dynamicList)
            .approvalStatus("0")
            .requestTimestamp(LocalDateTime.now())
            .organisationToAdd(Organisation.builder().organisationID("OrgID").build());
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
