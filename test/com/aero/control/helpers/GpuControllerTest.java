package com.aero.control.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class GpuControllerTest {
    @Test
    public void parsesNumericAndKernelBooleanToggleValues() {
        assertEquals(Boolean.TRUE, GpuController.parseToggleState("1"));
        assertEquals(Boolean.TRUE, GpuController.parseToggleState("Y"));
        assertEquals(Boolean.TRUE, GpuController.parseToggleState("yes"));
        assertEquals(Boolean.TRUE, GpuController.parseToggleState("true"));
        assertEquals(Boolean.FALSE, GpuController.parseToggleState("0"));
        assertEquals(Boolean.FALSE, GpuController.parseToggleState("N"));
        assertEquals(Boolean.FALSE, GpuController.parseToggleState("no"));
        assertEquals(Boolean.FALSE, GpuController.parseToggleState("false"));
        assertNull(GpuController.parseToggleState("unknown"));
    }
}
