package com.aero.control.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Handles import and export of profile backups on shared storage. */
public final class ProfileBackupRestoreOperations {
    private final File backupDirectory = new File(
            FilePath.EXTERNAL_PATH + "/com.aero.control/profiles");

    /** Lists valid profile backup filenames available for restore. */
    public String[] listAvailableBackups() throws IOException {
        if (!this.backupDirectory.exists()) {
            return new String[0];
        }
        String[] entries = this.backupDirectory.list();
        if (entries == null) {
            throw new IOException("Unable to list profile backups");
        }
        List<String> backups = new ArrayList<>();
        for (String entry : entries) {
            if (ProfileFileOperations.isValidProfileFilename(entry)
                    && new File(this.backupDirectory, entry).isFile()) {
                backups.add(entry);
            }
        }
        return backups.toArray(new String[backups.size()]);
    }

    public void restoreProfile(String filename) throws IOException {
        File source = backupFile(filename);
        File destination = new File(FilePath.sharedPrefsPath, filename);
        if (!source.isFile()) {
            throw new IOException("Profile backup does not exist");
        }
        copyFile(source, destination);
    }

    public void backupProfile(String profileName) throws IOException {
        if (!ProfileFileOperations.isValidProfileName(profileName)
                || !ProfileFileOperations.isValidProfileFilename(profileName + ".xml")) {
            throw new IOException("Invalid profile name");
        }
        String filename = profileName + ".xml";
        File source = new File(FilePath.sharedPrefsPath, filename);
        if (!source.isFile()) {
            throw new IOException("Saved profile does not exist");
        }
        if (!this.backupDirectory.exists() && !this.backupDirectory.mkdirs()) {
            throw new IOException("Unable to create the profile backup directory");
        }
        if (!this.backupDirectory.isDirectory()) {
            throw new IOException("Profile backup path is not a directory");
        }
        copyFile(source, new File(this.backupDirectory, filename));
    }

    private File backupFile(String filename) throws IOException {
        if (!ProfileFileOperations.isValidProfileFilename(filename)) {
            throw new IOException("Invalid profile backup filename");
        }
        return new File(this.backupDirectory, filename);
    }

    private static void copyFile(File source, File destination) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        IOException failure = null;
        try {
            input = new BufferedInputStream(new FileInputStream(source));
            output = new BufferedOutputStream(new FileOutputStream(destination));
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            output.flush();
        } catch (IOException e) {
            failure = e;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    if (failure == null) {
                        failure = e;
                    }
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    if (failure == null) {
                        failure = e;
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
