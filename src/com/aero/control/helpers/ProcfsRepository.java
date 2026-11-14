package com.aero.control.helpers;

import java.io.BufferedReader;
import java.io.StringReader;

/** Typed access boundary for kernel controls exposed through procfs. */
public final class ProcfsRepository {
    private static final String NO_DATA_FOUND = "Unavailable";

    public interface ReadbackVerifier {
        boolean matches(String requestedValue, String readback);
    }

    private final RootShellSession session;
    private final SysfsReader reader;

    public ProcfsRepository(RootShellSession session) {
        this.session = session;
        this.reader = new SysfsReader(session);
    }

    /** Reads and trims a single procfs value. */
    public SysfsResult<String> readValue(ProcfsNode node) {
        if (node == null) return SysfsResult.failure("Procfs node is required");
        String value = reader.getContents(node.getPath());
        if (value == null || NO_DATA_FOUND.equals(value)) {
            return SysfsResult.failure("Unable to read " + node.getPath());
        }
        return SysfsResult.success(value.trim());
    }

    /** Reads all lines from a procfs node. */
    public SysfsResult<String[]> readLines(ProcfsNode node) {
        SysfsResult<String> result = readValue(node);
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        return SysfsResult.success(result.getValue().split("\\r?\\n"));
    }

    /** Parses a procfs meminfo snapshot after reading it through the typed boundary. */
    public SysfsResult<MemorySnapshot> readMemorySnapshot(ProcfsNode node) {
        SysfsResult<String> result = readValue(node);
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        MemorySnapshot snapshot = MemInfoParser.parse(
                new BufferedReader(new StringReader(result.getValue())));
        return snapshot.isAvailable()
                ? SysfsResult.success(snapshot)
                : SysfsResult.<MemorySnapshot>failure(
                        "Unable to parse memory statistics from " + node.getPath());
    }

    /** Reads a space-separated procfs value using an explicit decoding mode. */
    public SysfsResult<String[]> readValues(ProcfsNode node, ReadMode mode) {
        if (node == null || mode == null) {
            return SysfsResult.failure("Procfs node and read mode are required");
        }
        String contents = reader.getContents(node.getPath());
        String[] values = reader.parseArray(contents, mode);
        if (values.length == 0
                || (values.length == 1 && NO_DATA_FOUND.equals(values[0]))) {
            return SysfsResult.failure("Unable to read " + node.getPath());
        }
        return SysfsResult.success(values);
    }

    /** Lists files or directories under a validated procfs directory. */
    public SysfsResult<String[]> listDirectory(
            ProcfsDirectory directory, DirectoryEntryType entryType) {
        if (directory == null) {
            return SysfsResult.failure("Procfs directory is required");
        }
        return KernelDirectoryLister.list(directory.getPath(), entryType, session);
    }

    /** Writes a procfs value and verifies the kernel readback. */
    public SysfsResult<String> writeValue(ProcfsNode node, String value) {
        return writeValue(node, value, new ReadbackVerifier() {
            @Override
            public boolean matches(String requestedValue, String readback) {
                return requestedValue.equals(readback.trim());
            }
        });
    }

    /** Writes a procfs value using a node-specific readback verifier. */
    public SysfsResult<String> writeValue(
            ProcfsNode node, String value, ReadbackVerifier verifier) {
        if (node == null || value == null || value.trim().length() == 0) {
            return SysfsResult.failure("Procfs node and value are required");
        }
        if (verifier == null) {
            return SysfsResult.failure("Readback verifier is required");
        }
        String normalizedValue = value.trim();
        if (normalizedValue.indexOf('\n') >= 0 || normalizedValue.indexOf('\r') >= 0) {
            return SysfsResult.failure("Value must be a single line");
        }
        if (!node.accepts(normalizedValue)) {
            return SysfsResult.failure("Value is outside the procfs node bounds");
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
