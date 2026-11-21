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

    /** Checks whether the device exposes the calibration control node. */
    public SysfsResult<Boolean> isAvailable() {
        return repository.exists(new SysfsNode(FilePath.COLOR_CONTROL));
    }

    /**
     * Writes a display-color value and enables the color control when supported.
     *
     * @param value space-separated display-color components to write
     * @return the verified write result, or an inspection/activation failure
     */
    public SysfsResult<String> writeColorValue(String value) {
        if (!isValidColor(value)) {
            return SysfsResult.failure("Color must contain three values from 0 to 255");
        }
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

    private boolean isValidColor(String value) {
        if (value == null) return false;
        String[] components = value.trim().split("\\s+");
        if (components.length != 3) return false;
        for (String component : components) {
            try {
                int channel = Integer.parseInt(component);
                if (channel < 0 || channel > 255) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
