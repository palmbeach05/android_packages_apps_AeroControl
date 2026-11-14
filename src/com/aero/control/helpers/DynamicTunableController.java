package com.aero.control.helpers;

import java.util.ArrayList;
import java.util.List;

/** Discovers and applies individual dynamic sysfs/procfs tuning parameters. */
public final class DynamicTunableController {
    private static final String PARAMETER_NAME_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.-";

    private final SysfsRepository sysfs;
    private final ProcfsRepository procfs;

    DynamicTunableController(SysfsRepository sysfs, ProcfsRepository procfs) {
        this.sysfs = sysfs;
        this.procfs = procfs;
    }

    public SysfsResult<String[]> listSysfsParameters(SysfsDirectory directory) {
        if (directory == null) {
            return SysfsResult.failure("Sysfs directory is required");
        }
        return filterParameters(sysfs.listDirectory(directory, DirectoryEntryType.FILES));
    }

    public SysfsResult<String[]> listProcfsParameters(ProcfsDirectory directory) {
        if (directory == null) {
            return SysfsResult.failure("Procfs directory is required");
        }
        return filterParameters(procfs.listDirectory(directory, DirectoryEntryType.FILES));
    }

    public SysfsResult<String> readSysfsParameter(
            SysfsDirectory directory, String parameterName) {
        if (!isValidParameter(directory == null ? null : directory.getPath(), parameterName,
                "/sys/")) {
            return SysfsResult.failure("Sysfs parameter is invalid");
        }
        return sysfs.readValue(new SysfsNode(directory.getPath() + "/" + parameterName));
    }

    public SysfsResult<String> readProcfsParameter(
            ProcfsDirectory directory, String parameterName) {
        if (!isValidParameter(directory == null ? null : directory.getPath(), parameterName,
                "/proc/")) {
            return SysfsResult.failure("Procfs parameter is invalid");
        }
        return procfs.readValue(new ProcfsNode(directory.getPath() + "/" + parameterName));
    }

    public SysfsResult<String> writeSysfsParameter(
            SysfsDirectory directory, String parameterName, String value) {
        if (!isValidParameter(directory == null ? null : directory.getPath(), parameterName,
                "/sys/")) {
            return SysfsResult.failure("Sysfs parameter is invalid");
        }
        if (!isSafeValue(value)) {
            return SysfsResult.failure("Tunable value contains unsupported characters");
        }
        return sysfs.writeValue(
                new SysfsNode(directory.getPath() + "/" + parameterName), value);
    }

    public SysfsResult<String> writeProcfsParameter(
            ProcfsDirectory directory, String parameterName, String value) {
        if (!isValidParameter(directory == null ? null : directory.getPath(), parameterName,
                "/proc/")) {
            return SysfsResult.failure("Procfs parameter is invalid");
        }
        if (!isSafeValue(value)) {
            return SysfsResult.failure("Tunable value contains unsupported characters");
        }
        return procfs.writeValue(
                new ProcfsNode(directory.getPath() + "/" + parameterName), value);
    }

    private static SysfsResult<String[]> filterParameters(SysfsResult<String[]> listing) {
        if (!listing.isSuccess()) {
            return SysfsResult.failure(listing.getError());
        }
        List<String> parameters = new ArrayList<>();
        for (String entry : listing.getValue()) {
            if (isParameterName(entry) && !"uevent".equals(entry) && !"dev".equals(entry)) {
                parameters.add(entry);
            }
        }
        return SysfsResult.success(parameters.toArray(new String[parameters.size()]));
    }

    private static boolean isValidParameter(
            String directoryPath, String parameterName, String prefix) {
        return directoryPath != null && directoryPath.startsWith(prefix)
                && isParameterName(parameterName);
    }

    private static boolean isParameterName(String parameterName) {
        if (parameterName == null || parameterName.length() == 0) {
            return false;
        }
        for (int i = 0; i < parameterName.length(); i++) {
            if (PARAMETER_NAME_CHARACTERS.indexOf(parameterName.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSafeValue(String value) {
        if (value == null || value.length() == 0 || value.length() > 256) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character < 0x20 || character == 0x7f) {
                return false;
            }
        }
        return true;
    }
}
