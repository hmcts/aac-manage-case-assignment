package uk.gov.hmcts.reform.managecase.client.datastore.model;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DisplayContextParameterTest {

    @Test
    void testDisplayContextParameter() {
        DisplayContextParameter displayContextParameter =
            new DisplayContextParameter(DisplayContextParameterType.LIST, "someValue");

        assertEquals(DisplayContextParameterType.LIST, displayContextParameter.getType());
        assertEquals("someValue", displayContextParameter.getValue());
    }

    @Test
    void testGetDisplayContextParametersFor_blankOrNull() {
        assertEquals(0, DisplayContextParameter.getDisplayContextParametersFor("").size());
        assertEquals(0, DisplayContextParameter.getDisplayContextParametersFor(null).size());
    }

    @Test
    void testGetDisplayContextParametersFor_single() {
        List<DisplayContextParameter> list =  DisplayContextParameter.getDisplayContextParametersFor(
            "#LIST(someValue)"
        );
        assertEquals(1, list.size());
        assertEquals(DisplayContextParameterType.LIST, list.get(0).getType());
        assertEquals("someValue", list.get(0).getValue());
    }

    @Test
    void testGetDisplayContextParametersFor_multiple() {
        List<DisplayContextParameter> list = DisplayContextParameter.getDisplayContextParametersFor(
            "#LIST(someValue), #LIST(anotherValue)"
        );
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(p -> p.getType() == DisplayContextParameterType.LIST 
            && p.getValue().equals("someValue")));
        assertTrue(list.stream().anyMatch(p -> p.getType() == DisplayContextParameterType.LIST 
            && p.getValue().equals("anotherValue")));
    }

}
