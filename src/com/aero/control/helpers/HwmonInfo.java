package com.aero.control.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enumerates the fixed hwmon sysfs hierarchy ({@code /sys/class/hwmon} and
 * its {@code hwmonN} subdirectories), falling back to the shared root shell
 * only when the app cannot list the directory directly. Kept separate from
 * the generic {@link SysfsReader} because the allowlist regex and directory
 * layout here are specific to the hwmon subsystem.
 */
final class HwmonInfo {
    private static final Pattern HWMON_DIRECTORY_PATTERN =
            Pattern.compile("/sys/class/hwmon(?:/hwmon\\d+/?){0,1}");

    private final RootShellSession session;

    HwmonInfo(RootShellSession session) {
        this.session = session;
    }

    /**
     * Lists entries in the fixed hwmon sysfs hierarchy, falling back to the shared root
     * shell when the app cannot enumerate the directory directly.
     *
     * @param path /sys/class/hwmon or one of its hwmonN directories
     * @param files if true, list files; if false, list directories
     * @return sorted entry names, or an empty array when the directory cannot be listed
     */
    String[] getRootAwareHwmonDirInfo(String path, boolean files) {
        if (path == null || !HWMON_DIRECTORY_PATTERN.matcher(path).matches()) {
            return new String[0];
        }

        File directory = new File(path);
        File[] entries = directory.listFiles();
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
        String command = "for entry in " + RootShellSession.escapeShellArg(path) +
                "/*; do [ " + test + " \"$entry\" ] && printf '%s\\n' \"${entry##*/}\"; done";
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
}
