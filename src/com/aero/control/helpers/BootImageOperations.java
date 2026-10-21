package com.aero.control.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/** Narrow privileged operations used to inspect and back up supported boot images. */
public final class BootImageOperations {
    private static final long OPERATION_TIMEOUT_MS = 60000L;
    private static final long CHECK_TIMEOUT_MS = 5000L;
    interface Commands {
        OperationResult executeOperation(String command, long timeoutMs);
        String execute(String command, long timeoutMs);
        long partitionCapacity(String partition) throws IOException;
        File privateDirectory();
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
            public File privateDirectory() {
                // App-private files location, also used by FilePath.OFFSET_STAT.
                return new File("/data/data/com.aero.control/files");
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
        File snapshot = null;
        boolean handedToShell = false;
        try {
            snapshot = snapshot(source, false);
            long capacity = session.partitionCapacity(partition);
            if (capacity <= 0 || snapshot.length() != capacity) {
                return OperationResult.failed("Backup does not match boot partition capacity");
            }
            // Timeout does not prove dd has exited. Only its shell may remove the input.
            handedToShell = true;
            return writeBootPartition(snapshot.getPath(), partition);
        } catch (IOException e) {
            return OperationResult.failed("Cannot stage backup or determine boot partition capacity");
        } catch (RuntimeException e) {
            return OperationResult.failed("Invalid capacity or staging storage");
        } finally {
            if (!handedToShell && snapshot != null) snapshot.delete();
        }
    }

    private File snapshot(String source, boolean kernel) throws IOException {
        File directory = session.privateDirectory();
        if (!directory.isDirectory()) throw new IOException("Private files directory unavailable");
        File staged = File.createTempFile("boot-restore-", ".img", directory);
        boolean valid = false;
        try {
            if (!staged.setReadable(false, false) || !staged.setWritable(false, false)
                    || !staged.setReadable(true, true) || !staged.setWritable(true, true)) {
                throw new IOException("Cannot restrict snapshot permissions");
            }
            String before = digest(new File(source), -1);
            FileInputStream input = new FileInputStream(source);
            try {
                FileOutputStream output = new FileOutputStream(staged);
                try {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                } finally {
                    output.close();
                }
            } finally {
                input.close();
            }
            if (!before.equals(digest(new File(source), -1))
                    || !before.equals(digest(staged, -1))
                    || !isValidBackup(staged.getPath(), kernel)) {
                throw new IOException("Staged backup is invalid or changed while copying");
            }
            valid = true;
            return staged;
        } finally {
            if (!valid) staged.delete();
        }
    }

    private static String digest(File file, int limit) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream input = new FileInputStream(file);
            try {
                byte[] buffer = new byte[8192];
                int remaining = limit;
                int count;
                while (remaining != 0 && (count = input.read(buffer, 0,
                        remaining < 0 ? buffer.length : Math.min(buffer.length, remaining))) != -1) {
                    digest.update(buffer, 0, count);
                    if (remaining > 0) remaining -= count;
                }
            } finally {
                input.close();
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) result.append(String.format("%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 unavailable", e);
        }
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
        return session.executeOperation("( trap "
                + RootShellSession.escapeShellArg("rm -f " + input)
                + " 0; trap 'exit 1' 1 2 15; [ -f " + input + " ] && [ -r " + input
                + " ] && [ -s " + input + " ] && [ -b " + target + " ] && [ -w "
                + target + " ] && dd if=" + input + " of=" + target + " )",
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
        File snapshot = null;
        boolean handedToShell = false;
        try {
            snapshot = snapshot(source, true);
            String input = RootShellSession.escapeShellArg(snapshot.getPath());
            String target = RootShellSession.escapeShellArg(destination);
            String directory = RootShellSession.escapeShellArg(destination + ".restore-"
                    + UUID.randomUUID().toString());
            String staging = directory + "/image";
            String cleanup = "rm -f " + staging + " " + input + "; rmdir " + directory;
            // A private child is insufficient if another user can rename its parent.
            // Check every ancestor for root ownership, symlinks and group/other writes.
            // ls -ldn and shell patterns are supported by legacy Android toolbox.
            String parent = RootShellSession.escapeShellArg(new File(destination).getParent());
            String command = "( umask 077; p=" + parent + "; while :; do "
                    + "[ ! -L \"$p\" ] || exit 1; metadata=$(ls -ldn \"$p\") || exit 1; "
                    + "set -- $metadata; [ \"$3\" = 0 ] || exit 1; "
                    + "case \"$1\" in d?????????) ;; *) exit 1;; esac; "
                    + "case \"$1\" in ?????w*|????????w*) exit 1;; esac; "
                    + "[ \"$p\" = / ] && break; p=${p%/*}; [ -n \"$p\" ] || p=/; done; "
                    + "mkdir " + directory + " || exit 1; trap "
                    + RootShellSession.escapeShellArg(cleanup) + " 0; trap 'exit 1' 1 2 15; "
                    + "set -C; : > " + staging + " || exit 1; set +C; "
                    + "if command -v md5sum >/dev/null 2>&1; then hash=md5sum; else hash=md5; fi; "
                    + "[ -f " + target + " ] && [ ! -L " + target + " ] && "
                    + "cp " + input + " " + staging + " && "
                    + "copied=$($hash " + staging + ") && [ \"${copied%% *}\" = "
                    + RootShellSession.escapeShellArg(digest(snapshot, -1)) + " ] && "
                    // The actual staged length and header must match the validated header.
                    // This applies the Java header predicate without requiring od/hexdump.
                    + "[ $(wc -c < " + staging + ") -eq " + snapshot.length() + " ] && "
                    + "header=$(dd if=" + staging + " bs=1 count=48 2>/dev/null | $hash) && "
                    + "[ \"${header%% *}\" = " + RootShellSession.escapeShellArg(digest(snapshot, 48))
                    + " ] && chown 0:0 " + staging + " && chmod 644 " + staging + " && "
                    + "[ -f " + target + " ] && [ ! -L " + target + " ] && "
                    + "mv -f " + staging + " " + target + " )";
            // Remove the private snapshot even when ancestor checks or mkdir fail.
            command = "( trap " + RootShellSession.escapeShellArg("rm -f " + input)
                    + " 0; trap 'exit 1' 1 2 15; " + command + " )";
            handedToShell = true;
            return session.executeOperation(command, OPERATION_TIMEOUT_MS);
        } catch (IOException e) {
            return OperationResult.failed("Cannot stage kernel backup");
        } finally {
            if (!handedToShell && snapshot != null) snapshot.delete();
        }
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
