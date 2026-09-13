package com.aero.control.helpers;

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
        return RootAwareDirectory.list(path, files, HWMON_DIRECTORY_PATTERN, session);
    }
}
