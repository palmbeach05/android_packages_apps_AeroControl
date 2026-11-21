package com.aero.control.helpers;

import com.aero.control.helpers.legacy.LegacyOverclockController;

/** Narrow entry point for domain controllers used by UI and services. */
public final class HardwareGateway implements HardwareAccessService {
    private final SysfsRepository repository;
    private final ProcfsRepository procfsRepository;
    private final HwmonInfo hwmonInfo;
    private final TegraI2cInfo tegraI2cInfo;
    private final CpuController cpuController;
    private final GpuController gpuController;
    private final DisplayColorController displayColorController;
    private final LedController ledController;
    private final IoSchedulerController ioSchedulerController;
    private final NetworkController networkController;
    private final DynamicTunableController dynamicTunableController;
    private final HardwareMetricsReader hardwareMetricsReader;
    private final LegacyOverclockController legacyOverclockController;

    HardwareGateway(RootShellSession session) {
        this.repository = new SysfsRepository(session);
        this.procfsRepository = new ProcfsRepository(session);
        this.hwmonInfo = new HwmonInfo(session);
        this.tegraI2cInfo = new TegraI2cInfo(session);
        this.cpuController = new CpuController(repository);
        this.gpuController = new GpuController(repository);
        this.displayColorController = new DisplayColorController(repository);
        this.ledController = new LedController(session);
        this.ioSchedulerController = new IoSchedulerController(repository);
        this.networkController = new NetworkController(procfsRepository);
        this.dynamicTunableController =
                new DynamicTunableController(repository, procfsRepository);
        this.hardwareMetricsReader = new HardwareMetricsReader(session);
        this.legacyOverclockController = new LegacyOverclockController(session);
    }

    @Override
    public SysfsResult<String> read(SysfsNode node) {
        return repository.readValue(node);
    }

    @Override
    public SysfsResult<String[]> readValues(SysfsNode node, ReadMode mode) {
        return repository.readValues(node, mode);
    }

    @Override
    public SysfsResult<String[]> readLines(SysfsNode node) {
        return repository.readLines(node);
    }

    @Override
    public String[] readMetrics(String[] paths) {
        return hardwareMetricsReader.readFiles(paths);
    }

    @Override
    public SysfsResult<String> write(SysfsNode node, String value) {
        return repository.writeValue(node, value);
    }

    @Override
    public ProcfsRepository procfs() {
        return procfsRepository;
    }

    @Override
    public SysfsResult<String[]> listSysfsDirectory(
            SysfsDirectory directory, DirectoryEntryType entryType) {
        return repository.listDirectory(directory, entryType);
    }

    @Override
    public HwmonInfo hwmon() {
        return hwmonInfo;
    }

    @Override
    public TegraI2cInfo tegraI2c() {
        return tegraI2cInfo;
    }

    /** Returns the controller for CPU hardware operations. */
    @Override
    public CpuController cpu() {
        return cpuController;
    }

    /** Returns the controller for GPU hardware operations. */
    @Override
    public GpuController gpu() {
        return gpuController;
    }

    /** Returns the controller for device LED and button hardware operations. */
    @Override
    public LedController led() {
        return ledController;
    }

    /** Returns the controller for display-color hardware operations. */
    @Override
    public DisplayColorController displayColor() {
        return displayColorController;
    }

    @Override
    public IoSchedulerController ioScheduler() {
        return ioSchedulerController;
    }

    @Override
    public NetworkController network() {
        return networkController;
    }

    @Override
    public DynamicTunableController dynamicTunables() {
        return dynamicTunableController;
    }

    /** Returns the isolated legacy overclock operations for supported devices. */
    @Override
    public LegacyOverclockController legacyOverclock() {
        return legacyOverclockController;
    }
}
