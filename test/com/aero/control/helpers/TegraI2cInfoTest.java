package com.aero.control.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TegraI2cInfoTest {
    @Test
    public void deniedOrIncompleteRootEnumerationIsFailure() {
        SysfsResult<String[]> denied = TegraI2cInfo.parseRootControllerListing(
                TegraI2cInfo.TEGRA_I2C_LIST_FAILURE_MARKER);
        SysfsResult<String[]> incomplete = TegraI2cInfo.parseRootControllerListing(
                TegraI2cInfo.TEGRA_I2C_LIST_START_MARKER);

        assertFalse(denied.isSuccess());
        assertFalse(incomplete.isSuccess());
    }

    @Test
    public void successfulEmptyRootEnumerationIsCacheable() {
        String output = TegraI2cInfo.TEGRA_I2C_LIST_START_MARKER + "\n"
                + TegraI2cInfo.TEGRA_I2C_LIST_END_MARKER;

        SysfsResult<String[]> result = TegraI2cInfo.parseRootControllerListing(output);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().length);
    }

    @Test
    public void successfulListingFiltersAndSortsControllerNames() {
        String output = TegraI2cInfo.TEGRA_I2C_LIST_START_MARKER
                + "\nserial8250\ntegra-i2c.10\ntegra-i2c.2\n"
                + TegraI2cInfo.TEGRA_I2C_LIST_END_MARKER;

        SysfsResult<String[]> result = TegraI2cInfo.parseRootControllerListing(output);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getValue().length);
        assertEquals("tegra-i2c.10", result.getValue()[0]);
        assertEquals("tegra-i2c.2", result.getValue()[1]);
    }
}
