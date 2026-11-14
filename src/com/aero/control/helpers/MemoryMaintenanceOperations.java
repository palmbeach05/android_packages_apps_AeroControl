package com.aero.control.helpers;

/** Narrow root operations for filesystem and low-memory status screens. */
public final class MemoryMaintenanceOperations {
    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static final long MAINTENANCE_TIMEOUT_MS = 60000L;
    private final RootShellSession session;

    MemoryMaintenanceOperations(RootShellSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Root shell session is required");
        }
        this.session = session;
    }

    public SysfsResult<String> readJournalInfo() {
        return readCommandOutput("tune2fs -l "
                + RootShellSession.escapeShellArg("/dev/block/mmcblk1p25"),
                DEFAULT_TIMEOUT_MS, "Unable to read filesystem journal information");
    }

    public SysfsResult<String> readLowMemoryStatus() {
        String propertyPattern = FilePath.LOW_MEM_PROPERTY.replace(".", "\\.");
        String file = RootShellSession.escapeShellArg(FilePath.LOW_MEM);
        String command = "value=$(grep '^" + propertyPattern + "=' " + file
                + " | head -n 1 | cut -d= -f2-); "
                + "if [ -n \"$value\" ]; then printf '__AERO_LOW_MEM__%s' \"$value\"; "
                + "else printf '__AERO_LOW_MEM_UNSUPPORTED__'; fi";
        String output = session.execute(command, DEFAULT_TIMEOUT_MS);
        if (output == null) {
            return SysfsResult.failure("Unable to read low-memory status");
        }
        String unsupported = "__AERO_LOW_MEM_UNSUPPORTED__";
        if (output.contains(unsupported)) {
            return SysfsResult.failure("Low-memory status is not supported");
        }
        String marker = "__AERO_LOW_MEM__";
        int markerIndex = output.indexOf(marker);
        if (markerIndex < 0) {
            return SysfsResult.failure("Unable to parse low-memory status");
        }
        return SysfsResult.success(output.substring(markerIndex + marker.length()).trim());
    }

    public SysfsResult<String> readMountTable() {
        return readCommandOutput("mount", DEFAULT_TIMEOUT_MS, "Unable to read mount table");
    }

    public OperationResult remountSystemReadWrite() {
        return session.executeOperation(
                "mount -o remount,rw -t ext3 /dev/block/mmcblk1p21 /system",
                DEFAULT_TIMEOUT_MS);
    }

    public OperationResult trimFilesystem(String mountPoint) {
        if (!"/system".equals(mountPoint) && !"/data".equals(mountPoint)
                && !"/cache".equals(mountPoint)) {
            return OperationResult.failed("Filesystem mount point is invalid");
        }
        return session.executeOperation("fstrim -v "
                + RootShellSession.escapeShellArg(mountPoint), MAINTENANCE_TIMEOUT_MS);
    }

    private SysfsResult<String> readCommandOutput(
            String command, long timeoutMs, String failureMessage) {
        String output = session.execute(command, timeoutMs);
        if (output == null) {
            return SysfsResult.failure(failureMessage);
        }
        return SysfsResult.success(output);
    }
}
