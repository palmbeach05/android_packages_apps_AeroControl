package com.aero.control.helpers;

/** A validated directory path within procfs. */
public final class ProcfsDirectory {
    private final String path;

    public ProcfsDirectory(String path) {
        if (path == null || !path.startsWith("/proc/")
                || containsParentSegment(path)) {
            throw new IllegalArgumentException("Procfs directories must be under /proc");
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
