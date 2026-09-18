package com.aero.control.helpers;

import java.io.File;

/** LED and display-control operations exposed through the typed sysfs repository. */
public final class LedController {
    private final SysfsRepository repository;

    /**
     * Creates a controller backed by the supplied sysfs repository.
     *
     * @param repository repository used for display-color operations
     */
    LedController(SysfsRepository repository) {
        this.repository = repository;
    }

    /**
     * Reads the display-color node as a single value.
     *
     * @return the current color value, or a failure when the node cannot be read
     */
    public SysfsResult<String> readColorValue() {
        return repository.readValue(new SysfsNode(FilePath.COLOR_CONTROL));
    }

    /**
     * Reads the individual values exposed by the display-color node.
     *
     * @return the raw color components, or a failure when the node cannot be read
     */
    public SysfsResult<String[]> readColorValues() {
        return repository.readValues(new SysfsNode(FilePath.COLOR_CONTROL), ReadMode.RAW);
    }

    /**
     * Writes a display-color value and enables the color control when supported.
     *
     * @param value space-separated display-color components to write
     * @return the verified write result, or the activation failure when activation fails
     */
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
