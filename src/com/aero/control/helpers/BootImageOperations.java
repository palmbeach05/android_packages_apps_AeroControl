package com.aero.control.helpers;

/** Narrow privileged operations used to inspect and back up supported boot images. */
public final class BootImageOperations {
    private static final long OPERATION_TIMEOUT_MS = 60000L;
    private static final long CHECK_TIMEOUT_MS = 5000L;
    private final RootShellSession session;

    BootImageOperations(RootShellSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Root shell session is required");
        }
        this.session = session;
    }

    public boolean isReadableFile(String path) {
        if (!isAbsolutePath(path)) {
            return false;
        }
        String quotedPath = RootShellSession.escapeShellArg(path);
        String output = session.execute("[ -f " + quotedPath + " ] && [ -r "
                + quotedPath + " ] && printf '__AERO_FILE_READABLE__' || true",
                CHECK_TIMEOUT_MS);
        return output != null && output.contains("__AERO_FILE_READABLE__");
    }

    public boolean isReadableBlockDevice(String path) {
        if (!isAbsolutePath(path)) {
            return false;
        }
        String quotedPath = RootShellSession.escapeShellArg(path);
        String output = session.execute("[ -b " + quotedPath + " ] && [ -r "
                + quotedPath + " ] && printf '__AERO_BLOCK_READABLE__' || true",
                CHECK_TIMEOUT_MS);
        return output != null && output.contains("__AERO_BLOCK_READABLE__");
    }

    public OperationResult backupImage(String source, String destination) {
        if (!isAbsolutePath(source) || !isAbsolutePath(destination)) {
            return OperationResult.failed("Boot image path is invalid");
        }
        String quotedSource = RootShellSession.escapeShellArg(source);
        String quotedDestination = RootShellSession.escapeShellArg(destination);
        return session.executeOperation("dd if=" + quotedSource + " of="
                + quotedDestination + " && chmod 777 " + quotedDestination,
                OPERATION_TIMEOUT_MS);
    }

    public OperationResult restoreBootPartition(String source, String partition) {
        if (!isAbsolutePath(source) || !isAbsolutePath(partition)) {
            return OperationResult.failed("Boot image path is invalid");
        }
        String quotedSource = RootShellSession.escapeShellArg(source);
        String quotedPartition = RootShellSession.escapeShellArg(partition);
        return session.executeOperation("chmod 0777 " + quotedSource + " && dd if="
                + quotedSource + " of=" + quotedPartition, OPERATION_TIMEOUT_MS);
    }

    public OperationResult restoreKernelImage(String source) {
        if (!isAbsolutePath(source)) {
            return OperationResult.failed("Kernel image path is invalid");
        }
        String quotedSource = RootShellSession.escapeShellArg(source);
        String quotedDestination = RootShellSession.escapeShellArg(FilePath.zImage);
        String command = "rm -f /system/bootstrap/2nd-boot/zImage && cp "
                + quotedSource + " " + quotedDestination;
        return session.executeOperation(command, OPERATION_TIMEOUT_MS);
    }

    private static boolean isAbsolutePath(String path) {
        return path != null && path.startsWith("/") && path.indexOf('\0') < 0;
    }
}
