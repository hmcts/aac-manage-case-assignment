package uk.gov.hmcts.reform.managecase.service.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

public class UIDServiceTest {

    private UIDService uidService = new UIDService();

    @Test
    public void shouldReturnFalseForNullUID() {
        assertAll(
            () -> assertFalse(uidService.validateUID(null)),
            () -> assertFalse(uidService.validateUID("1")),
            () -> assertFalse(uidService.validateUID("abcdefghijklmnop")),
            () -> assertEquals(16, uidService.generateUID().length()),
            () -> assertTrue(uidService.validateUID(uidService.generateUID()))
        );
    }

}
