package com.aero.control.helpers;

/** Display-color operations exposed through the typed sysfs repository. */
public final class DisplayColorController {
    private final SysfsRepository repository;

    /**
     * Creates a controller backed by the supplied sysfs repository.
     *
     * @param repository repository used for display-color operations
     */
    DisplayColorController(SysfsRepository repository) {
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
     * @return the verified write result, or an inspection/activation failure
     */
    public SysfsResult<String> writeColorValue(String value) {
        SysfsResult<String> result = repository.writeValue(
                new SysfsNode(FilePath.COLOR_CONTROL), value);
        if (!result.isSuccess()) {
            return result;
        }

        SysfsNode activationNode = new SysfsNode(FilePath.COLOR_CONTROL_BIT);
        SysfsResult<Boolean> inspectionResult = repository.exists(activationNode);
        if (!inspectionResult.isSuccess()) {
            return SysfsResult.failure(inspectionResult.getError());
        }
        if (!inspectionResult.getValue()) {
            return result;
        }
        SysfsResult<String> activationResult = repository.writeValue(
                activationNode, "1");
        return activationResult.isSuccess() ? result : activationResult;
    }
}
