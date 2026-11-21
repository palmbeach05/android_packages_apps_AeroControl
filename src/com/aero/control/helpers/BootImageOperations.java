package com.aero.control.helpers;

import java.util.UUID;

/** Narrow privileged operations used to inspect and back up supported boot images. */
public final class BootImageOperations {
    private static final long OPERATION_TIMEOUT_MS = 60000L;
    private static final long CHECK_TIMEOUT_MS = 5000L;
    private static final String BOOT_RESTORE_STAGING_DIRECTORY = "/data/local/tmp";
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
        String stagedPath = BOOT_RESTORE_STAGING_DIRECTORY + "/aero-boot-restore-"
                + UUID.randomUUID().toString();
        String quotedStagedPath = RootShellSession.escapeShellArg(stagedPath);
        String stageCommand = "umask 077 && " + verifiedCopyCommand(
                quotedSource, quotedStagedPath)
                + " && chmod 0600 " + quotedStagedPath
                + " && dd if=" + quotedStagedPath + " of=" + quotedPartition;
        return session.executeOperation(withTemporaryCleanup(
                stageCommand, quotedStagedPath,
                "Boot image staging, validation, or restore failed"),
                OPERATION_TIMEOUT_MS);
    }

    public OperationResult restoreKernelImage(String source) {
        if (!isAbsolutePath(source)) {
            return OperationResult.failed("Kernel image path is invalid");
        }
        String quotedSource = RootShellSession.escapeShellArg(source);
        String quotedDestination = RootShellSession.escapeShellArg(FilePath.zImage);
        String stagedPath = FilePath.zImage + ".restore-" + UUID.randomUUID().toString();
        String quotedStagedPath = RootShellSession.escapeShellArg(stagedPath);
        String stageCommand = verifiedCopyCommand(quotedSource, quotedStagedPath)
                + " && mv -f " + quotedStagedPath + " " + quotedDestination;
        String command = withTemporaryCleanup(stageCommand, quotedStagedPath,
                "Kernel image staging, validation, or replacement failed");
        return session.executeOperation(command, OPERATION_TIMEOUT_MS);
    }

    private static String verifiedCopyCommand(String quotedSource, String quotedStagedPath) {
        return "[ -f " + quotedSource + " ] && [ -r " + quotedSource + " ] && [ -s "
                + quotedSource + " ] && cp " + quotedSource + " " + quotedStagedPath
                + " && [ -s " + quotedStagedPath + " ] && cmp -s "
                + quotedSource + " " + quotedStagedPath;
    }

    private static String withTemporaryCleanup(String command, String quotedStagedPath,
            String failureMessage) {
        return "(" + command + " || { printf '%s\\n' "
                + RootShellSession.escapeShellArg(failureMessage)
                + "; false; }; operation_status=$?; rm -f " + quotedStagedPath
                + "; cleanup_status=$?; if [ \"$operation_status\" -eq 0 ]; then "
                + "exit \"$cleanup_status\"; fi; exit \"$operation_status\")";
    }

    private static boolean isAbsolutePath(String path) {
        return path != null && path.startsWith("/") && path.indexOf('\0') < 0;
    }
}
