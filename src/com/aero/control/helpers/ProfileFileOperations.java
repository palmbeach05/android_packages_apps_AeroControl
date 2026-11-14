package com.aero.control.helpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Narrow API for listing, renaming, and deleting saved profile preference files. */
public final class ProfileFileOperations {
    private static final String[] RESERVED_FILENAMES = {
            "com.aero.control_preferences.xml",
            "showcase_internal.xml",
            "app_rate_prefs.xml",
            "perAppProfileHandler.xml",
            "miscSettingsStorage.xml"
    };

    private final RootShellSession session;

    public ProfileFileOperations(RootShellSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Root shell session is required");
        }
        this.session = session;
    }

    public static boolean isValidProfileName(String name) {
        return name != null && name.equals(name.trim())
                && !".".equals(name) && !"..".equals(name)
                && name.matches("[A-Za-z0-9 _-]{1,64}");
    }

    public static boolean isValidProfileFilename(String filename) {
        if (filename == null || !filename.endsWith(".xml")) {
            return false;
        }
        for (String reserved : RESERVED_FILENAMES) {
            if (reserved.equals(filename)) {
                return false;
            }
        }
        return isValidProfileName(filename.substring(0, filename.length() - 4));
    }

    /** Lists profile files, falling back to root only when app-level access is denied. */
    public String[] listProfileFiles() throws IOException {
        File directory = new File(FilePath.sharedPrefsPath);
        String[] entries = directory.list();
        if (entries != null) {
            List<String> files = new ArrayList<>();
            for (String entry : entries) {
                if (isValidProfileFilename(entry) && new File(directory, entry).isFile()) {
                    files.add(entry);
                }
            }
            return files.toArray(new String[files.size()]);
        }

        String path = RootShellSession.escapeShellArg(FilePath.sharedPrefsPath);
        String output = this.session.execute("if [ -d " + path + " ]; then "
                + "for entry in " + path + "*; do "
                + "[ -f \"$entry\" ] && printf '%s\\n' \"${entry##*/}\"; "
                + "done; else printf '__PROFILE_DIRECTORY_UNAVAILABLE__'; fi");
        if (output == null || "__PROFILE_DIRECTORY_UNAVAILABLE__".equals(output)) {
            throw new IOException("Unable to list saved profile files");
        }
        List<String> files = new ArrayList<>();
        for (String entry : output.split("\\r?\\n")) {
            if (isValidProfileFilename(entry)) {
                files.add(entry);
            }
        }
        return files.toArray(new String[files.size()]);
    }

    public synchronized OperationResult deleteProfile(String profileName) {
        if (!isValidProfileName(profileName)
                || !isValidProfileFilename(profileName + ".xml")) {
            return OperationResult.failed("Invalid profile name");
        }
        return deleteFile(profileFile(profileName));
    }

    /** Removes the app's default settings file for the explicit profile-reset flow. */
    public synchronized OperationResult deleteCurrentSettings() {
        return deleteFile(new File(FilePath.sharedPrefsPath,
                "com.aero.control_preferences.xml"));
    }

    private OperationResult deleteFile(File profile) {
        if (!profile.exists() || profile.delete()) {
            return OperationResult.ok();
        }

        String path = RootShellSession.escapeShellArg(profile.getPath());
        String output = this.session.execute("rm " + path
                + " && [ ! -e " + path + " ] && printf '__PROFILE_OPERATION_OK__'");
        return "__PROFILE_OPERATION_OK__".equals(output)
                ? OperationResult.ok()
                : OperationResult.failed("Unable to delete saved profile");
    }

    public synchronized OperationResult renameProfile(String oldName, String newName) {
        if (!isValidProfileName(oldName)
                || !isValidProfileName(newName)
                || !isValidProfileFilename(oldName + ".xml")
                || !isValidProfileFilename(newName + ".xml")
                || oldName.equals(newName)) {
            return OperationResult.failed("Invalid profile name");
        }
        File source = profileFile(oldName);
        File destination = profileFile(newName);
        if (!source.isFile()) {
            return OperationResult.failed("Saved profile does not exist");
        }
        if (destination.exists()) {
            return OperationResult.failed("A profile with that name already exists");
        }
        if (source.renameTo(destination)) {
            return OperationResult.ok();
        }

        String sourcePath = RootShellSession.escapeShellArg(source.getPath());
        String destinationPath = RootShellSession.escapeShellArg(destination.getPath());
        String output = this.session.execute("[ ! -e " + destinationPath + " ] && mv "
                + sourcePath + " " + destinationPath
                + " && [ -f " + destinationPath + " ] && [ ! -e " + sourcePath
                + " ] && printf '__PROFILE_OPERATION_OK__'");
        return "__PROFILE_OPERATION_OK__".equals(output)
                ? OperationResult.ok()
                : OperationResult.failed("Unable to rename saved profile");
    }

    private static File profileFile(String profileName) {
        return new File(FilePath.sharedPrefsPath, profileName + ".xml");
    }
}
