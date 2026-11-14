package com.aero.control.helpers;

import java.util.Arrays;

/** Reads and selects kernel-advertised TCP congestion-control algorithms. */
public final class NetworkController {
    private final ProcfsRepository repository;

    NetworkController(ProcfsRepository repository) {
        this.repository = repository;
    }

    public SysfsResult<String[]> readAvailableCongestionControls() {
        return repository.readValues(new ProcfsNode(
                FilePath.MISC_TCP_CONGESTION_AVAILABLE), ReadMode.RAW);
    }

    public SysfsResult<String> readCongestionControl() {
        return repository.readValue(new ProcfsNode(FilePath.MISC_TCP_CONGESTION_CURRENT));
    }

    public SysfsResult<String> writeCongestionControl(String value) {
        SysfsResult<String[]> available = readAvailableCongestionControls();
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        return repository.writeValue(new ProcfsNode(
                FilePath.MISC_TCP_CONGESTION_CURRENT,
                Arrays.asList(available.getValue())), value);
    }
}
