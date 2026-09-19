package com.aero.control.helpers;

/** CPU frequency operations exposed through the typed sysfs repository. */
public final class CpuController {
    private final SysfsRepository repository;

    /** Creates a controller backed by the supplied sysfs repository. */
    CpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    /** Returns the frequencies supported by a CPU in the requested read mode. */
    public SysfsResult<String[]> readAvailableFrequencies(int cpu, ReadMode mode) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_FREQ_SUFFIX), mode);
    }

    /** Returns the configured maximum frequency for a CPU. */
    public SysfsResult<String> readMaxFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ));
    }

    /** Writes and verifies the maximum frequency for a CPU. */
    public SysfsResult<String> writeMaxFrequency(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ), value);
    }

    /** Returns the configured minimum frequency for a CPU. */
    public SysfsResult<String> readMinFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ));
    }

    /** Writes and verifies the minimum frequency for a CPU. */
    public SysfsResult<String> writeMinFrequency(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ), value);
    }

    /** Returns the governors supported by a CPU. */
    public SysfsResult<String[]> readAvailableGovernors(int cpu) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_GOV_SUFFIX), ReadMode.RAW);
    }

    /** Returns the configured governor for a CPU. */
    public SysfsResult<String> readGovernor(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE));
    }

    /** Writes and verifies the governor for a CPU. */
    public SysfsResult<String> writeGovernor(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE), value);
    }
}
