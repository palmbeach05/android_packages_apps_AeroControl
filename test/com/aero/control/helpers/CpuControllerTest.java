package com.aero.control.helpers;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class CpuControllerTest {
    @Test
    public void includesConfiguredFrequenciesMissingFromKernelCapabilities() {
        assertArrayEquals(new String[] {"1800000", "1200000", "300000"},
                CpuController.includeConfiguredFrequencies(
                        new String[] {"1800000", "1200000"},
                        "300000", "1800000"));
    }

    @Test
    public void ignoresInvalidConfiguredFrequencies() {
        assertArrayEquals(new String[] {"1200000"},
                CpuController.includeConfiguredFrequencies(
                        new String[] {"1200000", "Unavailable", "-300000"},
                        "not-a-frequency", "0"));
    }
}
