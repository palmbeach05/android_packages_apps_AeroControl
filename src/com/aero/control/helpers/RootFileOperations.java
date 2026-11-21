package com.aero.control.helpers;

import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Root-aware directory listing for the generic filesystem browser. */
public final class RootFileOperations {
    private static final String LOG_TAG = RootFileOperations.class.getName();
    private static final String DIRECTORY_UNAVAILABLE = "__AERO_DIRECTORY_UNAVAILABLE__";
    private final RootShellSession session;

    RootFileOperations(RootShellSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Root shell session is required");
        }
        this.session = session;
    }

    /** Returns visible child paths for an absolute directory. */
    public SysfsResult<String[]> listDirectory(String path) {
        if (!isSafeDirectoryPath(path)) {
            return SysfsResult.failure("Directory path is invalid");
        }

        File directory = new File(path);
        File[] entries;
        try {
            entries = directory.listFiles();
        } catch (SecurityException e) {
            Log.d(LOG_TAG, "Direct directory listing denied for " + path, e);
            entries = null;
        }
        if (entries != null) {
            List<String> paths = new ArrayList<>();
            for (File entry : entries) {
                if (!entry.getName().startsWith(".")) {
                    paths.add(entry.getPath());
                }
            }
            return SysfsResult.success(paths.toArray(new String[paths.size()]));
        }

        String quotedPath = RootShellSession.escapeShellArg(path);
        String command = "if [ -d " + quotedPath + " ]; then for entry in "
                + quotedPath + "/*; do [ -e \"$entry\" ] && printf '%s\\n' \"$entry\"; "
                + "done; else printf '" + DIRECTORY_UNAVAILABLE + "'; fi";
        String output = session.execute(command);
        if (output == null || DIRECTORY_UNAVAILABLE.equals(output.trim())) {
            String message = "Unable to list directory " + path;
            Log.w(LOG_TAG, message);
            return SysfsResult.failure(message);
        }
        if (output.trim().length() == 0) {
            return SysfsResult.success(new String[0]);
        }
        String[] paths = output.trim().split("\\r?\\n");
        return SysfsResult.success(paths);
    }

    private static boolean isSafeDirectoryPath(String path) {
        if (path == null || !path.startsWith("/") || path.indexOf('\0') >= 0
                || path.indexOf('*') >= 0 || path.indexOf('?') >= 0
                || path.indexOf('[') >= 0 || path.indexOf(']') >= 0) {
            return false;
        }
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) {
                return false;
            }
        }
        return true;
    }
}
