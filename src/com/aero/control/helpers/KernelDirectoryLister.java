package com.aero.control.helpers;

import java.util.regex.Pattern;

/** Root-aware directory listing restricted to kernel virtual filesystems. */
final class KernelDirectoryLister {
    private static final Pattern KERNEL_DIRECTORY =
            Pattern.compile("/(?:sys|proc)/[A-Za-z0-9_./,:+=@-]+");

    private KernelDirectoryLister() {
    }

    static SysfsResult<String[]> list(
            String path, DirectoryEntryType entryType, RootShellSession session) {
        if (path == null || entryType == null) {
            return SysfsResult.failure("Kernel directory and entry type are required");
        }
        if (!KERNEL_DIRECTORY.matcher(path).matches() || containsParentSegment(path)) {
            return SysfsResult.failure("Kernel directory path is invalid");
        }
        return RootAwareDirectory.listResult(path,
                entryType == DirectoryEntryType.FILES, KERNEL_DIRECTORY, session);
    }

    private static boolean containsParentSegment(String path) {
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) return true;
        }
        return false;
    }
}
