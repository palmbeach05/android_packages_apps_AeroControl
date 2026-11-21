package com.aero.control.helpers;

/** A validated directory path within sysfs. */
public final class SysfsDirectory {
    private final String path;

    public SysfsDirectory(String path) {
        if (path == null || !path.startsWith("/sys/")
                || containsParentSegment(path)) {
            throw new IllegalArgumentException("Sysfs directories must be under /sys");
        }
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    private static boolean containsParentSegment(String path) {
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) return true;
        }
        return false;
    }
}
