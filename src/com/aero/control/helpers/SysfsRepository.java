package com.aero.control.helpers;

/** Central typed read/write boundary for sysfs nodes. */
public final class SysfsRepository {
    private static final String NO_DATA_FOUND = "Unavailable";

    private final RootShellSession session;
    private final SysfsReader reader;

    public SysfsRepository(RootShellSession session) {
        this.session = session;
        this.reader = new SysfsReader(session);
    }

    public SysfsResult<String> readValue(SysfsNode node) {
        if (node == null) {
            return SysfsResult.failure("Node is required");
        }
        String value = reader.getInfo(node.getPath());
        if (value == null || NO_DATA_FOUND.equals(value)) {
            return SysfsResult.failure("Unable to read " + node.getPath());
        }
        return SysfsResult.success(value.trim());
    }

    /** Reads a space-separated node value using an explicit decoding mode. */
    public SysfsResult<String[]> readValues(SysfsNode node, ReadMode mode) {
        if (node == null || mode == null) {
            return SysfsResult.failure("Node and read mode are required");
        }
        String[] values = reader.getInfoArray(node.getPath(), mode);
        if (values == null || values.length == 0
                || (values.length == 1 && NO_DATA_FOUND.equals(values[0]))) {
            return SysfsResult.failure("Unable to read " + node.getPath());
        }
        return SysfsResult.success(values);
    }

    /** Writes a value and only reports success after reading the value back. */
    public SysfsResult<String> writeValue(SysfsNode node, String value) {
        if (node == null || value == null || value.trim().length() == 0) {
            return SysfsResult.failure("Node and value are required");
        }
        String normalizedValue = value.trim();
        if (normalizedValue.indexOf('\n') >= 0 || normalizedValue.indexOf('\r') >= 0) {
            return SysfsResult.failure("Value must be a single line");
        }
        if (!node.accepts(normalizedValue)) {
            return SysfsResult.failure("Value is outside the node bounds");
        }

        String path = RootShellSession.escapeShellArg(node.getPath());
        String command = "chmod 0666 " + path + " && printf %s "
                + RootShellSession.escapeShellArg(normalizedValue) + " > " + path
                + " && cat " + path;
        synchronized (session) {
            session.openShell();
            if (!session.isLoaded()) {
                return SysfsResult.failure("Root shell is unavailable");
            }
            session.addCommand(command);
            String readback = session.getRootResult();
            if (readback == null || !normalizedValue.equals(readback.trim())) {
                return SysfsResult.failure("Write verification failed for " + node.getPath());
            }
            return SysfsResult.success(readback.trim());
        }
    }
}
