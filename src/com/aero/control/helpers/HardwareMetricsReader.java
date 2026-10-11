package com.aero.control.helpers;

/** Batches root-aware reads of validated sysfs and procfs metric nodes. */
public final class HardwareMetricsReader {
    private final RootShellSession session;

    HardwareMetricsReader(RootShellSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Root shell session is required");
        }
        this.session = session;
    }

    public String[] readFiles(String[] paths) {
        if (paths == null) {
            return null;
        }
        String[] values = new String[paths.length];
        if (paths.length == 0) {
            return values;
        }

        StringBuilder command = new StringBuilder("AERO_METRICS_OUTPUT=$(");
        for (int i = 0; i < paths.length; i++) {
            String startMarker = "AERO_FILE_" + i + "_START";
            String endMarker = "AERO_FILE_" + i + "_END";
            command.append("printf '%s\\n' ")
                    .append(RootShellSession.escapeShellArg(startMarker)).append("; ");
            String path = paths[i];
            if (isValidNodePath(path)) {
                command.append("cat ").append(RootShellSession.escapeShellArg(path))
                        .append(" 2>/dev/null");
            }
            command.append("; printf '\\n%s\\n' ")
                    .append(RootShellSession.escapeShellArg(endMarker)).append("; ");
        }
        command.append("); printf '%s' \"$AERO_METRICS_OUTPUT\"");

        String output = this.session.execute(command.toString());
        for (int i = 0; i < paths.length; i++) {
            String startMarker = "AERO_FILE_" + i + "_START";
            String endMarker = "AERO_FILE_" + i + "_END";
            if (output == null) {
                values[i] = SysfsReader.NO_DATA_FOUND;
                continue;
            }
            int start = output.indexOf(startMarker);
            int end = start < 0 ? -1
                    : output.indexOf(endMarker, start + startMarker.length());
            if (start < 0 || end < 0) {
                values[i] = SysfsReader.NO_DATA_FOUND;
                continue;
            }
            String value = output.substring(start + startMarker.length(), end).trim();
            values[i] = value.length() == 0 ? SysfsReader.NO_DATA_FOUND : value;
        }
        return values;
    }

    private static boolean isValidNodePath(String path) {
        if (path == null || (!path.startsWith("/sys/") && !path.startsWith("/proc/"))) {
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
