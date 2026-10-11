package com.aero.control.helpers;

/** Process-wide owner for root-backed hardware and profile services. */
public final class HardwareServices {
    private HardwareServices() {
    }

    private static final class Holder {
        private static final RootShellSession SESSION = new RootShellSession();
        private static final HardwareGateway HARDWARE = new HardwareGateway(SESSION);
        private static final ProfileFileOperations PROFILE_FILES =
                new ProfileFileOperations(SESSION);
        private static final RootFileOperations ROOT_FILES =
                new RootFileOperations(SESSION);
        private static final BootImageOperations BOOT_IMAGES =
                new BootImageOperations(SESSION);
        private static final MemoryMaintenanceOperations MEMORY_MAINTENANCE =
                new MemoryMaintenanceOperations(SESSION);

        static {
            Thread worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    SESSION.openShell();
                }
            }, "AeroRootShell");
            worker.start();
        }
    }

    public static HardwareAccessService hardware() {
        return Holder.HARDWARE;
    }

    public static ProfileFileOperations profileFiles() {
        return Holder.PROFILE_FILES;
    }

    public static RootFileOperations rootFiles() {
        return Holder.ROOT_FILES;
    }

    public static BootImageOperations bootImages() {
        return Holder.BOOT_IMAGES;
    }

    public static MemoryMaintenanceOperations memoryMaintenance() {
        return Holder.MEMORY_MAINTENANCE;
    }

    static RootShellSession rootShellSession() {
        return Holder.SESSION;
    }
}
