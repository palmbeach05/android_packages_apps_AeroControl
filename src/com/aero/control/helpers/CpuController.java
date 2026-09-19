package com.aero.control.helpers;

/** CPU frequency operations exposed through the typed sysfs repository. */
public final class CpuController {
    private final SysfsRepository repository;

    CpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String[]> readAvailableFrequencies(int cpu, ReadMode mode) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_FREQ_SUFFIX), mode);
    }

    public SysfsResult<String> readMaxFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ));
    }

    public SysfsResult<String> writeMaxFrequency(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ), value);
    }

    public SysfsResult<String> readMinFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ));
    }

    public SysfsResult<String> writeMinFrequency(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ), value);
    }

    public SysfsResult<String[]> readAvailableGovernors(int cpu) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_GOV_SUFFIX), ReadMode.RAW);
    }

    public SysfsResult<String> readGovernor(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE));
    }

    public SysfsResult<String> writeGovernor(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE), value);
    }
}
