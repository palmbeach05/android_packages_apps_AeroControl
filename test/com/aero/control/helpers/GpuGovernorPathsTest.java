package com.aero.control.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class GpuGovernorPathsTest {
    private static final String SETTINGS_PATH = "settings";
    private static final String GOVERNOR_MENU_PATH = "governor";

    @Test
    public void detectsSettingsPathWithoutGovernorMenuPath() {
        GpuGovernorPaths.Availability availability = resolve(SETTINGS_PATH);

        assertTrue(availability.isSettingsPathAvailable());
        assertNull(availability.getGovernorMenuPath());
    }

    @Test
    public void detectsGovernorMenuPathWithoutSettingsPath() {
        GpuGovernorPaths.Availability availability = resolve(GOVERNOR_MENU_PATH);

        assertFalse(availability.isSettingsPathAvailable());
        assertEquals(GOVERNOR_MENU_PATH, availability.getGovernorMenuPath());
    }

    @Test
    public void detectsBothPathsIndependently() {
        GpuGovernorPaths.Availability availability = resolve(
                GOVERNOR_MENU_PATH, SETTINGS_PATH);

        assertTrue(availability.isSettingsPathAvailable());
        assertEquals(GOVERNOR_MENU_PATH, availability.getGovernorMenuPath());
    }

    @Test
    public void reportsNeitherPathAsAvailable() {
        GpuGovernorPaths.Availability availability = resolve();

        assertFalse(availability.isSettingsPathAvailable());
        assertNull(availability.getGovernorMenuPath());
    }

    private GpuGovernorPaths.Availability resolve(String... existingPaths) {
        final Set<String> existing = new HashSet<String>();
        for (String path : existingPaths) {
            existing.add(path);
        }
        return GpuGovernorPaths.resolve(new String[] {GOVERNOR_MENU_PATH},
                SETTINGS_PATH, new GpuGovernorPaths.PathChecker() {
                    @Override
                    public boolean exists(String path) {
                        return existing.contains(path);
                    }
                });
    }
}
