package com.aero.control.helpers;

/** GPU frequency operations exposed through the typed sysfs repository. */
public final class GpuController {
    private final SysfsRepository repository;

    GpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String> readFrequency(String path) {
        return repository.readValue(new SysfsNode(path));
    }

    public SysfsResult<String> writeFrequency(String path, String value) {
        return repository.writeValue(new SysfsNode(path), value);
    }
}
