package com.aero.control.helpers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class BootImageOperationsTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private File kernel() throws IOException {
        File file = folder.newFile();
        RandomAccessFile image = new RandomAccessFile(file, "rw");
        try {
            image.setLength(128);
            image.seek(36);
            image.writeInt(Integer.reverseBytes(0x016f2818));
            image.writeInt(0);
            image.writeInt(Integer.reverseBytes(128));
        } finally {
            image.close();
        }
        return file;
    }

    private File boot() throws IOException {
        File file = folder.newFile();
        RandomAccessFile image = new RandomAccessFile(file, "rw");
        try {
            image.setLength(4096);
            image.writeBytes("ANDROID!");
            image.writeInt(Integer.reverseBytes(128));
            image.seek(36);
            image.writeInt(Integer.reverseBytes(2048));
        } finally {
            image.close();
        }
        return file;
    }

    @Test public void rejectsInvalidBackupsWithoutChangingDestination() throws Exception {
        File target = folder.newFile("zImage");
        Files.write(target.toPath(), new byte[] {1, 2, 3});
        Shell commands = new Shell();
        BootImageOperations operations = new BootImageOperations(commands);
        File empty = folder.newFile();
        File invalid = folder.newFile();
        Files.write(invalid.toPath(), new byte[128]);
        File unreadable = kernel();
        assertTrue(unreadable.setReadable(false, false));
        try {
            for (File backup : Arrays.asList(new File(folder.getRoot(), "missing"),
                    empty, invalid, unreadable, folder.getRoot())) {
                assertFalse(operations.restoreKernelImage(backup.getPath(), target.getPath()).isSuccess());
                assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(target.toPath()));
            }
        } finally {
            unreadable.setReadable(true);
        }
        assertEquals(0, commands.calls);
    }

    @Test public void stagingFailuresPreserveOriginalAndCleanOnlyOwnFiles() throws Exception {
        File source = kernel();
        File target = folder.newFile("zImage");
        File unrelated = folder.newFile("zImage.restore-unrelated");
        for (String failure : Arrays.asList("privateStorage", "permissions", "mkdir", "copy", "incomplete", "verify", "header", "chmod", "chown", "rename")) {
            Files.write(target.toPath(), new byte[] {1, 2, 3});
            Shell commands = new Shell();
            commands.failure = failure;
            assertFalse(failure, new BootImageOperations(commands)
                    .restoreKernelImage(source.getPath(), target.getPath()).isSuccess());
            assertArrayEquals(failure, new byte[] {1, 2, 3}, Files.readAllBytes(target.toPath()));
            assertTrue(unrelated.exists());
            assertEquals(3, folder.getRoot().list().length);
            assertEquals(1, folder.getRoot().listFiles(new java.io.FilenameFilter() {
                public boolean accept(File directory, String name) {
                    return name.startsWith("zImage.restore-");
                }
            }).length);
        }
    }

    @Test public void successfulReplacementCleansStagingFile() throws Exception {
        File source = kernel();
        File target = folder.newFile("zImage with \'quotes\'");
        Shell commands = new Shell();
        assertTrue(new BootImageOperations(commands)
                .restoreKernelImage(source.getPath(), target.getPath()).isSuccess());
        assertArrayEquals(Files.readAllBytes(source.toPath()), Files.readAllBytes(target.toPath()));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test public void bootPreflightRejectsInvalidAndWrongSizedBackups() throws Exception {
        Shell commands = new Shell();
        BootImageOperations operations = new BootImageOperations(commands);
        assertFalse(operations.restoreBootPartition(folder.newFile().getPath(), "/fake/boot").isSuccess());
        File source = boot();
        commands.capacity = 2048;
        assertFalse(operations.restoreBootPartition(source.getPath(), "/fake/boot").isSuccess());
        commands.capacity = -1;
        assertFalse(operations.restoreBootPartition(source.getPath(), "/fake/boot").isSuccess());
        assertEquals(0, commands.calls);
        RandomAccessFile image = new RandomAccessFile(source, "rw");
        image.setLength(2048);
        image.close();
        assertFalse(BootImageOperations.isValidBackup(source.getPath(), false));
    }

    @Test public void regularFileCannotBeUsedAsBootPartition() throws Exception {
        File source = boot();
        File target = folder.newFile("fake-boot");
        Files.write(target.toPath(), new byte[] {4, 5, 6});
        assertFalse(new BootImageOperations(new Shell())
                .restoreBootPartition(source.getPath(), target.getPath()).isSuccess());
        assertArrayEquals(new byte[] {4, 5, 6}, Files.readAllBytes(target.toPath()));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test public void legacyHashToolCanVerifyReplacement() throws Exception {
        File source = kernel();
        File target = folder.newFile("zImage");
        Shell commands = new Shell();
        commands.failure = "legacyHash";
        assertTrue(new BootImageOperations(commands)
                .restoreKernelImage(source.getPath(), target.getPath()).isSuccess());
        assertArrayEquals(Files.readAllBytes(source.toPath()), Files.readAllBytes(target.toPath()));
    }

    @Test public void bootWriteFailureAndTimeoutArePropagated() throws Exception {
        final File source = boot();
        for (OperationResult result : Arrays.asList(OperationResult.failed("dd failed"),
                OperationResult.timedOut("timeout"), OperationResult.ok())) {
            Shell commands = new Shell();
            commands.bootResult = result;
            assertSame(result, new BootImageOperations(commands)
                    .restoreBootPartition(source.getPath(), "/fake/boot"));
            assertTrue(commands.lastCommand.contains("[ -b '/fake/boot' ]"));
            assertTrue(commands.lastCommand.contains("[ -w '/fake/boot' ]"));
            assertFalse(commands.lastCommand.contains("chmod"));
            assertFalse(commands.lastCommand.contains("dd if='" + source.getPath() + "'"));
            assertTrue(commands.lastCommand.contains("trap"));
            assertArrayEquals(Files.readAllBytes(source.toPath()), commands.written);
            File[] snapshots = folder.getRoot().listFiles(new java.io.FilenameFilter() {
                public boolean accept(File directory, String name) {
                    return name.startsWith("boot-restore-");
                }
            });
            assertEquals(result.getStatus() == OperationResult.Status.TIMED_OUT ? 1 : 0,
                    snapshots.length);
            // Simulate the timed-out writer finishing its own trap later.
            for (File snapshot : snapshots) assertTrue(snapshot.delete());
        }
    }

    @Test public void changedSourceIsValidatedAfterPreflight() throws Exception {
        for (final boolean kernel : Arrays.asList(true, false)) {
            final File source = kernel ? kernel() : boot();
            File target = folder.newFile();
            Files.write(target.toPath(), new byte[] {7});
            Shell commands = new Shell();
            commands.beforeSnapshot = new Runnable() {
                public void run() {
                    try { Files.write(source.toPath(), new byte[kernel ? 128 : 4096]); }
                    catch (IOException e) { throw new AssertionError(e); }
                }
            };
            BootImageOperations operations = new BootImageOperations(commands);
            assertFalse((kernel ? operations.restoreKernelImage(source.getPath(), target.getPath())
                    : operations.restoreBootPartition(source.getPath(), "/fake/boot")).isSuccess());
            assertEquals(0, commands.calls);
            assertArrayEquals(new byte[] {7}, Files.readAllBytes(target.toPath()));
        }
    }

    @Test public void bootSnapshotSurvivesExternalSourceReplacement() throws Exception {
        final File source = boot();
        byte[] expected = Files.readAllBytes(source.toPath());
        Shell commands = new Shell();
        commands.bootResult = OperationResult.ok();
        commands.beforeWrite = new Runnable() {
            public void run() {
                try {
                    assertTrue(source.delete());
                    Files.write(source.toPath(), new byte[] {9});
                } catch (IOException e) { throw new AssertionError(e); }
            }
        };
        assertTrue(new BootImageOperations(commands)
                .restoreBootPartition(source.getPath(), "/fake/boot").isSuccess());
        assertArrayEquals(expected, commands.written);
    }

    @Test public void resizedSnapshotNeverReachesPartitionWrite() throws Exception {
        final File source = boot();
        Shell commands = new Shell();
        commands.beforeSnapshot = new Runnable() {
            public void run() {
                try {
                    RandomAccessFile image = new RandomAccessFile(source, "rw");
                    try { image.setLength(8192); } finally { image.close(); }
                } catch (IOException e) { throw new AssertionError(e); }
            }
        };
        assertFalse(new BootImageOperations(commands)
                .restoreBootPartition(source.getPath(), "/fake/boot").isSuccess());
        assertEquals(0, commands.calls);
        assertEquals(1, folder.getRoot().list().length);
    }

    @Test public void bootStagingFailureNeverWrites() throws Exception {
        Shell commands = new Shell();
        commands.failure = "privateStorage";
        assertFalse(new BootImageOperations(commands)
                .restoreBootPartition(boot().getPath(), "/fake/boot").isSuccess());
        assertEquals(0, commands.calls);
    }

    /** Runs only regular-file commands in a temporary directory, with fault injection. */
    private class Shell implements BootImageOperations.Commands {
        String failure = "";
        Runnable beforeSnapshot;
        Runnable beforeWrite;
        byte[] written;
        public File privateDirectory() {
            if (beforeSnapshot != null) beforeSnapshot.run();
            return "privateStorage".equals(failure) ? new File(folder.getRoot(), "missing")
                    : folder.getRoot();
        }
        String lastCommand;
        int calls;
        long capacity = 4096;
        OperationResult bootResult;

        public long partitionCapacity(String partition) { return capacity; }
        public String execute(String command, long timeoutMs) { throw new AssertionError(command); }
        public OperationResult executeOperation(String command, long timeoutMs) {
            calls++;
            lastCommand = command;
            if (bootResult != null) {
                // Read the exact dd input, but never execute a partition write.
                java.util.regex.Matcher input = java.util.regex.Pattern.compile("dd if='([^']+)'")
                        .matcher(command);
                assertTrue(input.find());
                File staged = new File(input.group(1));
                try {
                    if (beforeWrite != null) beforeWrite.run();
                    written = Files.readAllBytes(staged.toPath());
                    if (bootResult.getStatus() != OperationResult.Status.TIMED_OUT) staged.delete();
                } catch (IOException e) { throw new AssertionError(e); }
                return bootResult;
            }
            assertFalse(command.contains("of=\'/dev/"));
            if ("legacyHash".equals(failure)) {
                command = command.replace("command -v md5sum >/dev/null 2>&1", "false");
            }
            String functions = "ls() { printf 'drwxr-xr-x 1 0 0 0 directory\\n'; }; md5() { md5sum \"$@\"; }; chown() { return 0; }; ";
            if ("permissions".equals(failure)) functions += "ls() { printf 'drwxrwxrwx 1 0 0 0 directory\\n'; }; ";
            if ("mkdir".equals(failure)) functions += "mkdir() { return 1; }; ";
            if ("header".equals(failure)) functions += "dd() { printf invalid; }; ";
            if ("copy".equals(failure)) functions += "cp() { return 1; }; ";
            if ("incomplete".equals(failure)) functions += "cp() { printf x > \"$2\"; }; ";
            if ("verify".equals(failure)) functions += "md5sum() { return 1; }; ";
            if ("chmod".equals(failure)) functions += "chmod() { return 1; }; ";
            if ("chown".equals(failure)) functions += "chown() { return 1; }; ";
            if ("rename".equals(failure)) functions += "mv() { return 1; }; ";
            try {
                Process process = new ProcessBuilder("sh", "-c", functions + command)
                        .redirectErrorStream(true).start();
                while (process.getInputStream().read() != -1) { }
                return process.waitFor() == 0 ? OperationResult.ok() : OperationResult.failed(failure);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }
}
