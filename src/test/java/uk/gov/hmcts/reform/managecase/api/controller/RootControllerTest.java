package uk.gov.hmcts.reform.managecase.api.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RootControllerTest {

    @Test
    void shouldReturnWelcomeMessage() {
        assertThat(new RootController().welcome().getBody()).isEqualTo("Welcome to manage-case-assignment ");
    }
}
