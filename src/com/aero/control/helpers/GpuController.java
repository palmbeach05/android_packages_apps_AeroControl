package com.aero.control.helpers;

import java.util.Arrays;

/** Normal GPU frequency, governor, and control operations. */
public final class GpuController {
    private static final String AVAILABLE_GOVERNORS = "available_governors";
    private static final String GOVERNOR = "governor";

    public enum Toggle {
        CONTROL(FilePath.GPU_CONTROL_ACTIVE),
        SWEEP_TO_WAKE(FilePath.SWEEP2WAKE),
        DOUBLE_TAP_TO_WAKE(FilePath.DOUBLETAP2WAKE);

        private final String path;

        Toggle(String path) {
            this.path = path;
        }
    }

    private final SysfsRepository repository;

    /**
     * Creates GPU operations backed by the supplied sysfs repository.
     *
     * @param repository repository used to discover, read, and write GPU nodes
     */
    GpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    /**
     * Reads the frequencies advertised by the first supported GPU frequency table.
     *
     * @param mode formatting to apply to each frequency
     * @return the available frequencies, or a failure when no table can be read
     */
    public SysfsResult<String[]> readAvailableFrequencies(ReadMode mode) {
        SysfsResult<SysfsNode> node = findNode(FilePath.GPU_FREQ_ARRAY, "GPU frequency table");
        return node.isSuccess() ? repository.readValues(node.getValue(), mode)
                : SysfsResult.<String[]>failure(node.getError());
    }

    /**
     * Reads the configured maximum frequency from the first supported GPU node.
     *
     * @return the current maximum frequency, or a discovery/read failure
     */
    public SysfsResult<String> readMaxFrequency() {
        return readValue(findNode(FilePath.GPU_FILES, "GPU maximum frequency"));
    }

    /**
     * Writes a maximum frequency to the first supported GPU node.
     *
     * @param value raw frequency value accepted by the kernel node
     * @return the written value, or a discovery/write failure
     */
    public SysfsResult<String> writeMaxFrequency(String value) {
        SysfsResult<String[]> available = readAvailableFrequencies(ReadMode.RAW);
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        if (!contains(available.getValue(), value)) {
            return SysfsResult.failure("Frequency is not supported by the GPU");
        }
        SysfsResult<SysfsNode> node = findNode(FilePath.GPU_FILES, "GPU maximum frequency");
        if (!node.isSuccess()) return SysfsResult.failure(node.getError());
        return repository.writeValue(new SysfsNode(node.getValue().getPath(),
                Arrays.asList(available.getValue())), value);
    }

    /**
     * Reads the governors advertised by the active GPU governor interface.
     *
     * @return available governor names, or a discovery/read failure
     */
    public SysfsResult<String[]> readAvailableGovernors() {
        SysfsResult<SysfsNode> node = findGovernorNode(AVAILABLE_GOVERNORS);
        if (node.isSuccess()) {
            SysfsResult<String[]> governors = repository.readValues(node.getValue(), ReadMode.RAW);
            if (governors.isSuccess()) return governors;
        }

        SysfsResult<String> governor = readValue(findGovernorNode(GOVERNOR));
        return governor.isSuccess()
                ? SysfsResult.success(new String[] { governor.getValue() })
                : SysfsResult.<String[]>failure(governor.getError());
    }

    /**
     * Reads the currently selected GPU governor.
     *
     * @return the current governor name, or a discovery/read failure
     */
    public SysfsResult<String> readGovernor() {
        return readValue(findGovernorNode(GOVERNOR));
    }

    /**
     * Selects the GPU governor on the first supported governor interface.
     *
     * @param value governor name accepted by the kernel node
     * @return the written governor name, or a discovery/write failure
     */
    public SysfsResult<String> writeGovernor(String value) {
        SysfsResult<String[]> available = readAvailableGovernors();
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        if (!contains(available.getValue(), value)) {
            return SysfsResult.failure("Governor is not supported by the GPU");
        }
        SysfsResult<SysfsNode> node = findGovernorNode(GOVERNOR);
        if (!node.isSuccess()) return SysfsResult.failure(node.getError());
        return repository.writeValue(new SysfsNode(node.getValue().getPath(),
                Arrays.asList(available.getValue())), value);
    }

    /**
     * Reads whether the optional GPU control switch is enabled.
     *
     * @return the parsed control state, or a failure for unreadable or invalid state
     */
    public SysfsResult<Boolean> readControlEnabled() {
        SysfsResult<String> result = repository.readValue(new SysfsNode(FilePath.GPU_CONTROL_ACTIVE));
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        if ("1".equals(result.getValue())) return SysfsResult.success(true);
        if ("0".equals(result.getValue())) return SysfsResult.success(false);
        return SysfsResult.failure("Unexpected GPU control state");
    }

    /**
     * Enables or disables the optional GPU control switch.
     *
     * @param enabled desired control state
     * @return the applied state, or a write failure
     */
    public SysfsResult<Boolean> writeControlEnabled(boolean enabled) {
        SysfsResult<String> result = repository.writeValue(
                new SysfsNode(FilePath.GPU_CONTROL_ACTIVE), enabled ? "1" : "0");
        return result.isSuccess() ? SysfsResult.success(enabled)
                : SysfsResult.<Boolean>failure(result.getError());
    }

    /**
     * Checks whether the optional GPU control switch exists.
     *
     * @return whether the control node exists, or a repository failure
     */
    public SysfsResult<Boolean> isControlAvailable() {
        return repository.exists(new SysfsNode(FilePath.GPU_CONTROL_ACTIVE));
    }

    /** Checks whether a supported toggle node is exposed by this kernel. */
    public SysfsResult<Boolean> isToggleAvailable(Toggle toggle) {
        if (toggle == null) return SysfsResult.failure("Toggle is required");
        return repository.exists(new SysfsNode(toggle.path));
    }

    /** Reads a supported GPU or wake-gesture toggle without exposing its sysfs path. */
    public SysfsResult<Boolean> readToggle(Toggle toggle) {
        if (toggle == null) return SysfsResult.failure("Toggle is required");
        SysfsResult<String> result = repository.readValue(new SysfsNode(toggle.path));
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        if ("1".equals(result.getValue())) return SysfsResult.success(true);
        if ("0".equals(result.getValue())) return SysfsResult.success(false);
        return SysfsResult.failure("Unexpected state for " + toggle.name());
    }

    /** Writes and verifies a supported GPU or wake-gesture toggle. */
    public SysfsResult<Boolean> writeToggle(Toggle toggle, boolean enabled) {
        if (toggle == null) return SysfsResult.failure("Toggle is required");
        SysfsResult<String> result = repository.writeValue(
                new SysfsNode(toggle.path, 0, 1), enabled ? "1" : "0");
        return result.isSuccess() ? SysfsResult.success(enabled)
                : SysfsResult.<Boolean>failure(result.getError());
    }

    /** Writes and verifies one of the supported display modes. */
    public SysfsResult<String> writeDisplayMode(String value) {
        if (!"31".equals(value) && !"9".equals(value) && !"0".equals(value)) {
            return SysfsResult.failure("Display mode is not supported");
        }
        return repository.writeValue(new SysfsNode(FilePath.DISPLAY_COLOR, 0, 31), value);
    }

    /** Reads the selected display mode from its supported sysfs control. */
    public SysfsResult<String> readDisplayMode() {
        return repository.readValue(new SysfsNode(FilePath.DISPLAY_COLOR, 0, 31));
    }

    /** Checks whether the display-mode control is exposed by this kernel. */
    public SysfsResult<Boolean> isDisplayModeAvailable() {
        return repository.exists(new SysfsNode(FilePath.DISPLAY_COLOR, 0, 31));
    }

    /**
     * Finds the first supported governor node with the requested suffix.
     *
     * @param suffix governor node name appended to each supported base path
     * @return the discovered node, or a failure describing why none was available
     */
    private SysfsResult<SysfsNode> findGovernorNode(String suffix) {
        String[] paths = new String[FilePath.GPU_GOV_ARRAY.length];
        for (int i = 0; i < paths.length; i++) paths[i] = FilePath.GPU_GOV_ARRAY[i] + suffix;
        return findNode(paths, "GPU governor " + suffix);
    }

    /**
     * Finds the first existing node in a list of candidate paths.
     *
     * @param paths candidate sysfs paths in discovery order
     * @param description label included in the failure message
     * @return the first existing node, or a failure when none can be used
     */
    private SysfsResult<SysfsNode> findNode(String[] paths, String description) {
        String lastError = null;
        for (String path : paths) {
            SysfsResult<Boolean> result = repository.exists(new SysfsNode(path));
            if (result.isSuccess() && result.getValue()) return SysfsResult.success(new SysfsNode(path));
            if (!result.isSuccess()) lastError = result.getError();
        }
        return SysfsResult.failure(description + " is unavailable: "
                + (lastError == null ? "no supported node was found" : lastError));
    }

    /**
     * Reads a previously discovered node while preserving discovery failures.
     *
     * @param node node discovery result
     * @return the node value, or the discovery/read failure
     */
    private SysfsResult<String> readValue(SysfsResult<SysfsNode> node) {
        return node.isSuccess() ? repository.readValue(node.getValue())
                : SysfsResult.<String>failure(node.getError());
    }

    /**
     * Writes a previously discovered node while preserving discovery failures.
     *
     * @param node node discovery result
     * @param value value to write
     * @return the written value, or the discovery/write failure
     */
    private SysfsResult<String> writeValue(SysfsResult<SysfsNode> node, String value) {
        return node.isSuccess() ? repository.writeValue(node.getValue(), value)
                : SysfsResult.<String>failure(node.getError());
    }

    private boolean contains(String[] values, String candidate) {
        if (candidate == null) return false;
        for (String value : values) {
            if (candidate.trim().equals(value.trim())) return true;
        }
        return false;
    }
}
