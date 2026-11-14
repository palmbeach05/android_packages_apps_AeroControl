package com.aero.control.helpers;

import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Shared direct-read/root-fallback directory enumeration for allowlisted paths. */
final class RootAwareDirectory {
    private RootAwareDirectory() {
    }

    static String[] list(String path, boolean files, Pattern allowedPath, RootShellSession session) {
        SysfsResult<String[]> result = listResult(path, files, allowedPath, session);
        if (!result.isSuccess()) {
            Log.w(RootAwareDirectory.class.getName(), result.getError());
            return new String[0];
        }
        return result.getValue();
    }

    static SysfsResult<String[]> listResult(
            String path, boolean files, Pattern allowedPath, RootShellSession session) {
        if (path == null || !allowedPath.matcher(path).matches()) {
            return SysfsResult.failure("Kernel directory path is invalid");
        }

        File[] entries = null;
        try {
            entries = new File(path).listFiles();
        } catch (SecurityException e) {
            Log.d(RootAwareDirectory.class.getName(),
                    "Direct directory listing denied for " + path, e);
        }
        if (entries != null) {
            List<String> results = new ArrayList<>();
            for (File entry : entries) {
                if ((files && entry.isFile()) || (!files && entry.isDirectory())) {
                    results.add(entry.getName());
                }
            }
            Collections.sort(results);
            return SysfsResult.success(results.toArray(new String[0]));
        }

        String test = files ? "-f" : "-d";
        String command = "for entry in " + RootShellSession.escapeShellArg(path)
                + "/*; do [ " + test + " \"$entry\" ]"
                + " && printf '%s\\n' \"${entry##*/}\"; done";
        String output = session.execute(command);
        if (output == null) {
            return SysfsResult.failure("Unable to list kernel directory " + path);
        }
        if (output.trim().length() == 0) return SysfsResult.success(new String[0]);
        String[] results = output.trim().split("\\r?\\n");
        Arrays.sort(results);
        return SysfsResult.success(results);
    }
}
