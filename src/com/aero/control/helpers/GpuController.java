package com.aero.control.helpers;

/** Normal GPU frequency, governor, and control operations. */
public final class GpuController {
    private static final String AVAILABLE_GOVERNORS = "available_governors";
    private static final String GOVERNOR = "governor";
    private final SysfsRepository repository;

    GpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String[]> readAvailableFrequencies(ReadMode mode) {
        SysfsResult<SysfsNode> node = findNode(FilePath.GPU_FREQ_ARRAY, "GPU frequency table");
        return node.isSuccess() ? repository.readValues(node.getValue(), mode)
                : SysfsResult.<String[]>failure(node.getError());
    }

    public SysfsResult<String> readMaxFrequency() {
        return readValue(findNode(FilePath.GPU_FILES, "GPU maximum frequency"));
    }

    public SysfsResult<String> writeMaxFrequency(String value) {
        return writeValue(findNode(FilePath.GPU_FILES, "GPU maximum frequency"), value);
    }

    public SysfsResult<String[]> readAvailableGovernors() {
        SysfsResult<SysfsNode> node = findGovernorNode(AVAILABLE_GOVERNORS);
        if (!node.isSuccess()) {
            node = findGovernorNode(GOVERNOR);
        }
        return node.isSuccess() ? repository.readValues(node.getValue(), ReadMode.RAW)
                : SysfsResult.<String[]>failure(node.getError());
    }

    public SysfsResult<String> readGovernor() {
        return readValue(findGovernorNode(GOVERNOR));
    }

    public SysfsResult<String> writeGovernor(String value) {
        return writeValue(findGovernorNode(GOVERNOR), value);
    }

    public SysfsResult<Boolean> readControlEnabled() {
        SysfsResult<String> result = repository.readValue(new SysfsNode(FilePath.GPU_CONTROL_ACTIVE));
        if (!result.isSuccess()) return SysfsResult.failure(result.getError());
        if ("1".equals(result.getValue())) return SysfsResult.success(true);
        if ("0".equals(result.getValue())) return SysfsResult.success(false);
        return SysfsResult.failure("Unexpected GPU control state");
    }

    public SysfsResult<Boolean> writeControlEnabled(boolean enabled) {
        SysfsResult<String> result = repository.writeValue(
                new SysfsNode(FilePath.GPU_CONTROL_ACTIVE), enabled ? "1" : "0");
        return result.isSuccess() ? SysfsResult.success(enabled)
                : SysfsResult.<Boolean>failure(result.getError());
    }

    public SysfsResult<Boolean> isControlAvailable() {
        return repository.exists(new SysfsNode(FilePath.GPU_CONTROL_ACTIVE));
    }

    private SysfsResult<SysfsNode> findGovernorNode(String suffix) {
        String[] paths = new String[FilePath.GPU_GOV_ARRAY.length];
        for (int i = 0; i < paths.length; i++) paths[i] = FilePath.GPU_GOV_ARRAY[i] + suffix;
        return findNode(paths, "GPU governor " + suffix);
    }

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

    private SysfsResult<String> readValue(SysfsResult<SysfsNode> node) {
        return node.isSuccess() ? repository.readValue(node.getValue())
                : SysfsResult.<String>failure(node.getError());
    }

    private SysfsResult<String> writeValue(SysfsResult<SysfsNode> node, String value) {
        return node.isSuccess() ? repository.writeValue(node.getValue(), value)
                : SysfsResult.<String>failure(node.getError());
    }
}
