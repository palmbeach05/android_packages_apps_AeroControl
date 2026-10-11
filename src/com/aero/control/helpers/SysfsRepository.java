package com.aero.control.helpers;

/** Central typed read/write boundary for sysfs nodes. */
public final class SysfsRepository {
    private static final String NO_DATA_FOUND = "Unavailable";

    public interface ReadbackVerifier {
        boolean matches(String requestedValue, String readback);
    }

    private final RootShellSession session;
    private final SysfsReader reader;

    /**
     * Creates a repository that performs reads and writes through a root shell session.
     *
     * @param session root shell session used to access sysfs nodes
     */
    public SysfsRepository(RootShellSession session) {
        this.session = session;
        this.reader = new SysfsReader(session);
    }

    /**
     * Reads and trims a single value from a sysfs node.
     *
     * @param node node to read
     * @return the node value, or a failure when the node cannot be read
     */
    public SysfsResult<String> readValue(SysfsNode node) {
        if (node == null) {
            return SysfsResult.failure("Node is required");
        }
        String value = reader.getContents(node.getPath());
        if (value == null || NO_DATA_FOUND.equals(value)) {
            return SysfsResult.failure("Unable to read " + node.getPath());
        }
        return SysfsResult.success(value.trim());
    }

    /** Reads all lines from a sysfs node. */
    public SysfsResult<String[]> readLines(SysfsNode node) {
        SysfsResult<String> result = readValue(node);
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        return SysfsResult.success(result.getValue().split("\\r?\\n"));
    }

    /** Reads a space-separated node value using an explicit decoding mode. */
    public SysfsResult<String[]> readValues(SysfsNode node, ReadMode mode) {
        if (node == null || mode == null) {
            return SysfsResult.failure("Node and read mode are required");
        }
        String contents = reader.getContents(node.getPath());
        String[] values = reader.parseArray(contents, mode);
        if (values.length == 0
                || (values.length == 1 && NO_DATA_FOUND.equals(values[0]))) {
            return SysfsResult.failure("Unable to read " + node.getPath());
        }
        return SysfsResult.success(values);
    }

    /** Lists files or directories under a validated sysfs directory. */
    public SysfsResult<String[]> listDirectory(
            SysfsDirectory directory, DirectoryEntryType entryType) {
        if (directory == null) {
            return SysfsResult.failure("Sysfs directory is required");
        }
        return KernelDirectoryLister.list(directory.getPath(), entryType, session);
    }

    /** Inspects whether a sysfs node exists through the root session. */
    public SysfsResult<Boolean> exists(SysfsNode node) {
        if (node == null) {
            return SysfsResult.failure("Node is required");
        }
        String path = RootShellSession.escapeShellArg(node.getPath());
        String inspection = session.execute("[ -e " + path + " ] && echo EXISTS || echo MISSING");
        if (inspection == null) {
            return SysfsResult.failure("Unable to inspect " + node.getPath());
        }
        inspection = inspection.trim();
        if ("EXISTS".equals(inspection)) return SysfsResult.success(true);
        if ("MISSING".equals(inspection)) return SysfsResult.success(false);
        return SysfsResult.failure("Unexpected inspection result for " + node.getPath());
    }

    /** Writes a value and only reports success after reading the value back. */
    public SysfsResult<String> writeValue(SysfsNode node, String value) {
        return writeValue(node, value, new ReadbackVerifier() {
            @Override
            public boolean matches(String requestedValue, String readback) {
                return requestedValue.equals(readback.trim());
            }
        });
    }

    /** Writes a value using a node-specific readback format verifier. */
    public SysfsResult<String> writeValue(
            SysfsNode node, String value, ReadbackVerifier verifier) {
        if (node == null || value == null || value.trim().length() == 0) {
            return SysfsResult.failure("Node and value are required");
        }
        if (verifier == null) {
            return SysfsResult.failure("Readback verifier is required");
        }
        String normalizedValue = value.trim();
        if (normalizedValue.indexOf('\n') >= 0 || normalizedValue.indexOf('\r') >= 0) {
            return SysfsResult.failure("Value must be a single line");
        }
        if (!node.accepts(normalizedValue)) {
            return SysfsResult.failure("Value is outside the node bounds");
        }

        String path = RootShellSession.escapeShellArg(node.getPath());
        String command = "printf %s "
                + RootShellSession.escapeShellArg(normalizedValue) + " > " + path
                + " && cat " + path;
        String readback = session.execute(command);
        if (readback == null || !verifier.matches(normalizedValue, readback)) {
            return SysfsResult.failure("Write verification failed for " + node.getPath());
        }
        return SysfsResult.success(readback.trim());
    }
}
