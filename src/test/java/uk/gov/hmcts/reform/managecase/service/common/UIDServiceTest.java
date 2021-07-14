package uk.gov.hmcts.reform.managecase.service.common;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UIDServiceTest {

    private UIDService uidService = new UIDService();

    @Test
    public void shouldReturnFalseForNullUID() {
        String uid = null;

        assertFalse(uidService.validateUID(uid));
    }

    @Test
    public void shouldReturnFalseForInvalidLengthUID() {
        String uid = "1";

        assertFalse(uidService.validateUID(uid));
    }

    @Test
    public void shouldReturnFalseForInvalidContentUID() {
        String uid = "abcdefghijklmnop";

        assertFalse(uidService.validateUID(uid));
    }

    @Test
    public void shouldGenerateValid16digitUID() {
        String uid = uidService.generateUID();

        assertTrue(uid.length() == 16);
    }

    @Test
    public void shouldValidateGeneratedUID() {
        String uid = uidService.generateUID();

        assertTrue(uidService.validateUID(uid));
    }

}
