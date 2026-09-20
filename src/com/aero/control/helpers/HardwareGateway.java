package com.aero.control.helpers;

/** Narrow entry point for domain controllers used by UI and services. */
public final class HardwareGateway {
    private final SysfsRepository repository;
    private final CpuController cpuController;
    private final GpuController gpuController;
    private final DisplayColorController displayColorController;

    HardwareGateway(RootShellSession session) {
        this.repository = new SysfsRepository(session);
        this.cpuController = new CpuController(repository);
        this.gpuController = new GpuController(repository);
        this.displayColorController = new DisplayColorController(repository);
    }

    /** Returns the shared sysfs repository used by the hardware controllers. */
    public SysfsRepository sysfs() {
        return repository;
    }

    /** Returns the controller for CPU hardware operations. */
    public CpuController cpu() {
        return cpuController;
    }

    /** Returns the controller for GPU hardware operations. */
    public GpuController gpu() {
        return gpuController;
    }

    /** Returns the controller for display-color hardware operations. */
    public DisplayColorController displayColor() {
        return displayColorController;
    }
}
