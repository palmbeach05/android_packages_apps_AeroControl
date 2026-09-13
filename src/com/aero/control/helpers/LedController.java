package com.aero.control.helpers;

/** LED and display-control operations exposed through the typed sysfs repository. */
public final class LedController {
    private final SysfsRepository repository;

    LedController(SysfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String> readColorValue() {
        return repository.readValue(new SysfsNode(FilePath.COLOR_CONTROL));
    }

    public SysfsResult<String> writeColorValue(String value) {
        return repository.writeValue(new SysfsNode(FilePath.COLOR_CONTROL), value);
    }
}
