package com.aero.control.helpers;

import java.io.File;

/** LED and display-control operations exposed through the typed sysfs repository. */
public final class LedController {
    private final SysfsRepository repository;

    LedController(SysfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String> readColorValue() {
        return repository.readValue(new SysfsNode(FilePath.COLOR_CONTROL));
    }

    public SysfsResult<String[]> readColorValues() {
        return repository.readValues(new SysfsNode(FilePath.COLOR_CONTROL), ReadMode.RAW);
    }

    public SysfsResult<String> writeColorValue(String value) {
        SysfsResult<String> result = repository.writeValue(
                new SysfsNode(FilePath.COLOR_CONTROL), value);
        if (!result.isSuccess() || !new File(FilePath.COLOR_CONTROL_BIT).exists()) {
            return result;
        }
        SysfsResult<String> activationResult = repository.writeValue(
                new SysfsNode(FilePath.COLOR_CONTROL_BIT), "1");
        return activationResult.isSuccess() ? result : activationResult;
    }
}
