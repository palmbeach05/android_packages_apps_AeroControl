package com.aero.control.helpers;

import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
final class TegraI2cInfo {
    static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = TegraI2cInfo.class.getName();

    private static final Pattern TEGRA_I2C_DIRECTORY_PATTERN = Pattern.compile(
            "/sys/devices/platform(?:/tegra-i2c\\.(\\d+)"
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
    String[] getRootAwareTegraI2cDirInfo(String path, boolean files) {
        if (path == null
                || !TEGRA_I2C_DIRECTORY_PATTERN.matcher(path).matches()
                || (files && !TEGRA_I2C_DEVICE_DIRECTORY_PATTERN.matcher(path).matches())) {
            return new String[0];
        }

        File[] entries = null;
        try {
            entries = new File(path).listFiles();
        } catch (SecurityException e) {
            // Fall through to the shared root shell.
        }
        if (entries != null) {
            List<String> results = new ArrayList<>();
            for (File entry : entries) {
                if ((files && entry.isFile()) || (!files && entry.isDirectory())) {
                    results.add(entry.getName());
                }
            }
            Collections.sort(results);
            return results.toArray(new String[0]);
        }

        String test = files ? "-f" : "-d";
        String command = "for entry in " + RootShellSession.escapeShellArg(path)
                + "/*; do [ " + test + " \"$entry\" ]"
                + " && printf '%s\\n' \"${entry##*/}\"; done";
        synchronized (session) {
            session.openShell();
            if (!session.isLoaded()) {
                return new String[0];
            }
            session.addCommand(command);
            String output = session.getRootResult();
            if (output == null || output.length() == 0) {
                return new String[0];
            }
            String[] results = output.split("\\r?\\n");
            Arrays.sort(results);
            return results;
        }
    }

    /**
     * Reads an allowlisted Tegra I2C tempN_input node, using the shared root shell only
     * when a direct read fails.
     *
     * @param path an allowlisted Tegra I2C tempN_input path
     * @return the node contents, or "Unavailable" when access is rejected or fails
     */
    String getRootAwareTegraI2cInfo(String path) {
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
    String getDirectTegraI2cInfo(String path) {
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
