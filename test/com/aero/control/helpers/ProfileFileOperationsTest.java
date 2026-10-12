package com.aero.control.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfileFileOperationsTest {
    @Test
    public void acceptsSimpleProfileNames() {
        assertTrue(ProfileFileOperations.isValidProfileName("Balanced profile_2"));
        assertTrue(ProfileFileOperations.isValidProfileFilename("Balanced profile_2.xml"));
    }

    @Test
    public void rejectsPathSyntaxAndInvalidCharacters() {
        assertFalse(ProfileFileOperations.isValidProfileName("../profile"));
        assertFalse(ProfileFileOperations.isValidProfileName("profile; rm -rf"));
        assertFalse(ProfileFileOperations.isValidProfileFilename("profile.txt"));
        assertFalse(ProfileFileOperations.isValidProfileName(null));
        assertFalse(ProfileFileOperations.isValidProfileFilename(null));
    }

    @Test
    public void rejectsApplicationPreferenceFilesAsProfiles() {
        assertFalse(ProfileFileOperations.isValidProfileFilename(
                "com.aero.control_preferences.xml"));
        assertFalse(ProfileFileOperations.isValidProfileFilename("miscSettingsStorage.xml"));
    }
}
