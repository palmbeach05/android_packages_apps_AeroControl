package com.aero.control.helpers;

/** Resolves the independent GPU governor menu and module-settings paths. */
public final class GpuGovernorPaths {
    public interface PathChecker {
        boolean exists(String path);
    }

    public static final class Availability {
        private final String governorMenuPath;
        private final boolean settingsPathAvailable;

        private Availability(String governorMenuPath, boolean settingsPathAvailable) {
            this.governorMenuPath = governorMenuPath;
            this.settingsPathAvailable = settingsPathAvailable;
        }

        public String getGovernorMenuPath() {
            return governorMenuPath;
        }

        public boolean isSettingsPathAvailable() {
            return settingsPathAvailable;
        }
    }

    private GpuGovernorPaths() {
    }

    public static Availability resolve(String[] governorMenuCandidates,
            String settingsPath, PathChecker checker) {
        if (governorMenuCandidates == null || settingsPath == null || checker == null) {
            throw new IllegalArgumentException("Governor paths and path checker are required");
        }
        String governorMenuPath = null;
        for (String path : governorMenuCandidates) {
            if (checker.exists(path)) {
                governorMenuPath = path;
            }
        }
        return new Availability(governorMenuPath,
                checker.exists(settingsPath));
    }
}
