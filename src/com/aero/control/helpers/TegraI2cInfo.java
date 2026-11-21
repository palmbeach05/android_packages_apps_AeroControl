package com.aero.control.helpers;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enumerates and reads the allowlisted Tegra-I2C sysfs hierarchy (used for
 * per-board temperature sensors on Tegra devices), falling back to the
 * shared root shell only when direct access is unavailable. This is
 * device-specific sysfs layout knowledge that has no reason to live inside
 * the generic shell-session helper.
 */
public final class TegraI2cInfo {
    static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = TegraI2cInfo.class.getName();
    private static final String TEGRA_I2C_PLATFORM_PATH = "/sys/devices/platform";
    private static final String TEGRA_I2C_LIST_MARKER = "__AERO_TEGRA_I2C_LIST__";

    private static final Pattern TEGRA_I2C_CONTROLLER_NAME_PATTERN =
            Pattern.compile("tegra-i2c\\.\\d+");
    private static final Pattern TEGRA_I2C_DIRECTORY_PATTERN = Pattern.compile(
            Pattern.quote(TEGRA_I2C_PLATFORM_PATH) + "(?:/tegra-i2c\\.(\\d+)"
                    + "(?:/i2c-\\1(?:/\\1-[0-9a-fA-F]{4})?)?)?");
    private static final Pattern TEGRA_I2C_DEVICE_DIRECTORY_PATTERN = Pattern.compile(
            "/sys/devices/platform/tegra-i2c\\.(\\d+)/i2c-\\1/"
                    + "\\1-[0-9a-fA-F]{4}");
    private static final Pattern TEGRA_I2C_TEMPERATURE_FILE_PATTERN = Pattern.compile(
            "/sys/devices/platform/tegra-i2c\\.(\\d+)/i2c-\\1/"
                    + "\\1-[0-9a-fA-F]{4}/temp\\d+_input");
    private static final Pattern TEGRA_I2C_METADATA_FILE_PATTERN = Pattern.compile(
            "/sys/devices/platform/tegra-i2c\\.(\\d+)/i2c-\\1/"
                    + "\\1-[0-9a-fA-F]{4}/(?:name|temp\\d+_label)");

    private final RootShellSession session;

    TegraI2cInfo(RootShellSession session) {
        this.session = session;
    }

    /**
     * Lists entries in the allowlisted Tegra I2C temperature hierarchy, using the
     * shared root shell only when direct directory enumeration is unavailable.
     *
     * @param path an allowlisted Tegra I2C hierarchy directory
     * @param files if true, list files; if false, list directories
     * @return sorted child names, or an empty array when access is rejected or fails
     */
    public String[] getRootAwareTegraI2cDirInfo(String path, boolean files) {
        if (files && (path == null || !TEGRA_I2C_DEVICE_DIRECTORY_PATTERN.matcher(path).matches())) {
            return new String[0];
        }
        if (path == null || !TEGRA_I2C_DIRECTORY_PATTERN.matcher(path).matches()) {
            return new String[0];
        }
        if (!files && TEGRA_I2C_PLATFORM_PATH.equals(path)) {
            SysfsResult<String[]> result = listPlatformControllers();
            if (!result.isSuccess()) {
                Log.w(LOG_TAG, result.getError());
                return new String[0];
            }
            return result.getValue();
        }
        return RootAwareDirectory.list(path, files, TEGRA_I2C_DIRECTORY_PATTERN, session);
    }

    public SysfsResult<String[]> listRootAwareTegraI2cControllers() {
        return listPlatformControllers();
    }

    private SysfsResult<String[]> listPlatformControllers() {
        File[] entries = null;
        try {
            entries = new File(TEGRA_I2C_PLATFORM_PATH).listFiles();
        } catch (SecurityException e) {
            Log.d(LOG_TAG, "Direct Tegra I2C directory listing denied", e);
        }
        if (entries != null) {
            List<String> controllers = new ArrayList<>();
            for (File entry : entries) {
                if (entry.isDirectory()
                        && TEGRA_I2C_CONTROLLER_NAME_PATTERN.matcher(entry.getName()).matches()) {
                    controllers.add(entry.getName());
                }
            }
            Collections.sort(controllers);
            return SysfsResult.success(controllers.toArray(new String[controllers.size()]));
        }

        String command = "if [ -d " + escapePlatformPath(TEGRA_I2C_PLATFORM_PATH)
                + " ]; then printf '%s\\n' " + TEGRA_I2C_LIST_MARKER
                + "; for entry in " + TEGRA_I2C_PLATFORM_PATH
                + "/tegra-i2c.*; do [ -d \"$entry\" ]"
                + " && printf '%s\\n' \"${entry##*/}\"; done; fi";
        String output = this.session.execute(command);
        if (output == null) {
            return SysfsResult.failure("Unable to list Tegra I2C controllers under "
                    + TEGRA_I2C_PLATFORM_PATH);
        }
        if (!output.contains(TEGRA_I2C_LIST_MARKER)) {
            return SysfsResult.failure("Unable to enumerate "
                    + TEGRA_I2C_PLATFORM_PATH);
        }
        List<String> controllers = new ArrayList<>();
        for (String controller : output.trim().split("\\r?\\n")) {
            if (TEGRA_I2C_LIST_MARKER.equals(controller)) {
                continue;
            }
            if (TEGRA_I2C_CONTROLLER_NAME_PATTERN.matcher(controller).matches()) {
                controllers.add(controller);
            }
        }
        Collections.sort(controllers);
        return SysfsResult.success(controllers.toArray(new String[controllers.size()]));
    }

    private static String escapePlatformPath(String path) {
        return RootShellSession.escapeShellArg(path);
    }

    /**
     * Reads an allowlisted Tegra I2C tempN_input node, using the shared root shell only
     * when a direct read fails.
     *
     * @param path an allowlisted Tegra I2C tempN_input path
     * @return the node contents, or "Unavailable" when access is rejected or fails
     */
    public String getRootAwareTegraI2cInfo(String path) {
        if (path == null || !TEGRA_I2C_TEMPERATURE_FILE_PATTERN.matcher(path).matches()) {
            return NO_DATA_FOUND;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path), 8192);
            try {
                String info = reader.readLine();
                return info == null ? NO_DATA_FOUND : info;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            // Fall through to the shared root shell.
        } catch (SecurityException e) {
            // Fall through to the shared root shell.
        }

        synchronized (session) {
            session.openShell();
            if (!session.isLoaded()) {
                return NO_DATA_FOUND;
            }
            session.addCommand("[ -f " + RootShellSession.escapeShellArg(path) + " ] && cat "
                    + RootShellSession.escapeShellArg(path));
            String output = session.getRootResult();
            return output == null || output.length() == 0 ? NO_DATA_FOUND : output;
        }
    }

    /**
     * Reads optional metadata from an allowlisted Tegra I2C device without using
     * the root shell when direct access is unavailable.
     *
     * @param path an allowlisted Tegra I2C name or tempN_label path
     * @return the trimmed node contents, or "Unavailable" when access is rejected or fails
     */
    public String getDirectTegraI2cInfo(String path) {
        if (path == null || !TEGRA_I2C_METADATA_FILE_PATTERN.matcher(path).matches()) {
            return NO_DATA_FOUND;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path), 8192);
            try {
                String info = reader.readLine();
                if (info == null || info.trim().length() == 0) {
                    return NO_DATA_FOUND;
                }
                return info.trim();
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            return NO_DATA_FOUND;
        } catch (SecurityException e) {
            return NO_DATA_FOUND;
        }
    }
}
