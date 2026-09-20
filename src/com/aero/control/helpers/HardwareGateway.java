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

    public SysfsRepository sysfs() {
        return repository;
    }

    public CpuController cpu() {
        return cpuController;
    }

    public GpuController gpu() {
        return gpuController;
    }

    public DisplayColorController displayColor() {
        return displayColorController;
    }
}
