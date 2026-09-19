package com.aero.control.helpers;

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
        if (path == null || !allowedPath.matcher(path).matches()) {
            return new String[0];
        }

        File[] entries = new File(path).listFiles();
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
}
