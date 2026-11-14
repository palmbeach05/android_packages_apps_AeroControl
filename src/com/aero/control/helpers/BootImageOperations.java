package com.aero.control.helpers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/** Narrow privileged operations used to inspect and back up supported boot images. */
public final class BootImageOperations {
    private static final long OPERATION_TIMEOUT_MS = 60000L;
    private static final long CHECK_TIMEOUT_MS = 5000L;
    interface Commands {
        OperationResult executeOperation(String command, long timeoutMs);
        String execute(String command, long timeoutMs);
        long partitionCapacity(String partition) throws IOException;
    }

    private final Commands session;

    BootImageOperations(final RootShellSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Root shell session is required");
        }
        this.session = new Commands() {
            public OperationResult executeOperation(String command, long timeoutMs) {
                return session.executeOperation(command, timeoutMs);
            }
            public long partitionCapacity(String partition) throws IOException {
                return readPartitionCapacity(partition);
            }
            public String execute(String command, long timeoutMs) {
                return session.execute(command, timeoutMs);
            }
        };
    }

    BootImageOperations(Commands session) {
        if (session == null) throw new IllegalArgumentException("Commands are required");
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

    /** Preflight cannot make a raw partition write atomic or power-loss safe. */
    public OperationResult restoreBootPartition(String source, String partition) {
        if (!isAbsolutePath(source) || !isAbsolutePath(partition)
                || !isValidBackup(source, false)) {
            return OperationResult.failed("Boot image backup is invalid or unreadable");
        }
        try {
            long capacity = session.partitionCapacity(partition);
            long bytes = new File(source).length();
            if (capacity <= 0 || bytes != capacity) {
                return OperationResult.failed("Backup does not match boot partition capacity");
            }
        } catch (IOException e) {
            return OperationResult.failed("Cannot determine boot partition capacity");
        } catch (RuntimeException e) {
            return OperationResult.failed("Invalid boot partition capacity");
        }
        return writeBootPartition(source, partition);
    }

    private static long readPartitionCapacity(String partition) throws IOException {
        // Resolve by-name symlinks; sysfs reports capacity in 512-byte sectors.
        String device = new File(partition).getCanonicalFile().getName();
        RandomAccessFile size = new RandomAccessFile("/sys/class/block/" + device + "/size", "r");
        try {
            long sectors = Long.parseLong(size.readLine().trim());
            return sectors > 0 && sectors <= Long.MAX_VALUE / 512 ? sectors * 512 : -1;
        } finally {
            size.close();
        }
    }

    private OperationResult writeBootPartition(String source, String partition) {
        String input = RootShellSession.escapeShellArg(source);
        String target = RootShellSession.escapeShellArg(partition);
        return session.executeOperation("[ -f " + input + " ] && [ -r " + input
                + " ] && [ -s " + input + " ] && [ -b " + target + " ] && [ -w "
                + target + " ] && dd if=" + input + " of=" + target,
                OPERATION_TIMEOUT_MS);
    }

    public OperationResult restoreKernelImage(String source) {
        return restoreKernelImage(source, FilePath.zImage);
    }

    OperationResult restoreKernelImage(String source, String destination) {
        if (!isAbsolutePath(source) || !isAbsolutePath(destination)
                || !isValidBackup(source, true)) {
            return OperationResult.failed("Kernel image backup is invalid or unreadable");
        }
        String input = RootShellSession.escapeShellArg(source);
        String target = RootShellSession.escapeShellArg(destination);
        String staging = RootShellSession.escapeShellArg(destination + ".restore-"
                + UUID.randomUUID().toString());
        // A subshell isolates umask and traps from the persistent root session.
        // noclobber reserves only this operation's file; failed reservation must not remove it.
        // Android toolbox supplies md5 on older releases, toybox supplies md5sum on newer ones.
        String command = "( umask 077; set -C; : > " + staging
                + " || exit 1; set +C; trap "
                + RootShellSession.escapeShellArg("rm -f " + staging)
                + " 0; trap 'exit 1' 1 2 15; "
                + "[ -f " + target + " ] && [ ! -L " + target + " ] && "
                + "[ -f " + input + " ] && [ -r " + input + " ] && [ -s " + input + " ] && "
                + "( if command -v md5sum >/dev/null 2>&1; then hash=md5sum; "
                + "else hash=md5; fi; "
                + "before=$($hash " + input + ") && "
                + "cp " + input + " " + staging + " && "
                + "after=$($hash " + input + ") && copied=$($hash " + staging + ") && "
                + "[ \"$before\" = \"$after\" ] && "
                + "digest=${before%% *} && stagedDigest=${copied%% *} && "
                + "[ ${#digest} -eq 32 ] && [ \"$digest\" = \"$stagedDigest\" ] && "
                + "[ -s " + staging + " ] && chown 0:0 " + staging
                + " && chmod 644 " + staging + " && "
                + "[ -f " + target + " ] && [ ! -L " + target + " ] && "
                + "mv -f " + staging + " " + target + " ) )";
        return session.executeOperation(command, OPERATION_TIMEOUT_MS);
    }

    /** Recognizes the ARM zImage header or the legacy Android boot header. */
    static boolean isValidBackup(String source, boolean kernel) {
        File file = new File(source);
        if (!file.isFile() || !file.canRead() || file.length() == 0) {
            return false;
        }
        try {
            RandomAccessFile image = new RandomAccessFile(file, "r");
            try {
                if (kernel) {
                    if (image.length() < 48) return false;
                    image.seek(36);
                    if (readUnsignedInt(image) != 0x016f2818L) return false;
                    long start = readUnsignedInt(image);
                    long end = readUnsignedInt(image);
                    return end > start && end - start <= image.length();
                }
                if (image.length() < 608) return false;
                byte[] magic = new byte[8];
                image.readFully(magic);
                if (!"ANDROID!".equals(new String(magic, "US-ASCII"))) return false;
                long kernelSize = readUnsignedInt(image);
                image.seek(16);
                long ramdiskSize = readUnsignedInt(image);
                image.seek(24);
                long secondSize = readUnsignedInt(image);
                image.seek(36);
                long pageSize = readUnsignedInt(image);
                if (kernelSize == 0 || pageSize < 608 || pageSize > 65536
                        || (pageSize & (pageSize - 1)) != 0) return false;
                long dtSize = readUnsignedInt(image);
                long required = pageSize + aligned(kernelSize, pageSize)
                        + aligned(ramdiskSize, pageSize) + aligned(secondSize, pageSize)
                        + aligned(dtSize, pageSize);
                return required <= image.length();
            } finally {
                image.close();
            }
        } catch (IOException e) {
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }

    private static long aligned(long value, long pageSize) {
        return ((value + pageSize - 1) / pageSize) * pageSize;
    }

    private static long readUnsignedInt(RandomAccessFile file) throws IOException {
        return ((long) Integer.reverseBytes(file.readInt())) & 0xffffffffL;
    }

    private static boolean isAbsolutePath(String path) {
        return path != null && path.startsWith("/") && path.indexOf('\0') < 0;
    }
}
