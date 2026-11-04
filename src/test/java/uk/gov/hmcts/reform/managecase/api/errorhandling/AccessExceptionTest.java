package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AccessExceptionTest {

    @Test
    void shouldCarryForbiddenStatusAndMessage() {
        AccessException exception = new AccessException("access denied");

        assertThat(exception).hasMessage("access denied");
        assertThat(AccessException.class.getAnnotation(ResponseStatus.class).code())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
