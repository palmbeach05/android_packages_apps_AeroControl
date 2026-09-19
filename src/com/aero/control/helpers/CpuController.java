package com.aero.control.helpers;

import java.util.ArrayList;
import java.util.List;

/** CPU frequency operations exposed through the typed sysfs repository. */
public final class CpuController {
    private static final String CPU_ONLINE_SUFFIX = "/online";
    private final SysfsRepository repository;

    /** Creates a controller backed by the supplied sysfs repository. */
    CpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    /** Returns the frequencies supported by a CPU in the requested read mode. */
    public SysfsResult<String[]> readAvailableFrequencies(int cpu, ReadMode mode) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_FREQ_SUFFIX), mode);
    }

    /** Returns the configured maximum frequency for a CPU. */
    public SysfsResult<String> readMaxFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ));
    }

    /** Writes and verifies the maximum frequency for a CPU. */
    public SysfsResult<String> writeMaxFrequency(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ), value);
    }

    /** Returns the configured minimum frequency for a CPU. */
    public SysfsResult<String> readMinFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ));
    }

    /** Writes and verifies the minimum frequency for a CPU. */
    public SysfsResult<String> writeMinFrequency(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ), value);
    }

    /** Returns the governors supported by a CPU. */
    public SysfsResult<String[]> readAvailableGovernors(int cpu) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_GOV_SUFFIX), ReadMode.RAW);
    }

    /** Returns the configured governor for a CPU. */
    public SysfsResult<String> readGovernor(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE));
    }

    /** Writes and verifies the governor for a CPU. */
    public SysfsResult<String> writeGovernor(int cpu, String value) {
        return repository.writeValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE), value);
    }

    /**
     * Returns an online CPU that exposes the requested frequency node for a cluster.
     * A cluster describes one shared cpufreq policy, so only its first usable member
     * is returned. This avoids touching offline members or bringing them online.
     */
    public SysfsResult<List<Integer>> getFrequencyWriteTargets(
            CpuClusterHelper.Cluster cluster, boolean isMax) {
        return getWriteTargets(cluster, isMax ? FilePath.CPU_MAX_FREQ : FilePath.CPU_MIN_FREQ);
    }

    /** Returns an online representative that exposes the cluster's governor node. */
    public SysfsResult<List<Integer>> getGovernorWriteTargets(CpuClusterHelper.Cluster cluster) {
        return getWriteTargets(cluster, FilePath.CURRENT_GOV_AVAILABLE);
    }

    private SysfsResult<List<Integer>> getWriteTargets(
            CpuClusterHelper.Cluster cluster, String nodeSuffix) {
        if (cluster == null) {
            return SysfsResult.failure("CPU cluster is required");
        }
        String lastError = null;
        for (Integer cpu : cluster.getMembers()) {
            SysfsResult<Boolean> onlineResult = isCpuOnline(cpu);
            if (!onlineResult.isSuccess()) {
                lastError = onlineResult.getError();
                continue;
            }
            if (!onlineResult.getValue()) {
                continue;
            }
            SysfsResult<Boolean> nodeResult = repository.exists(new SysfsNode(
                    FilePath.CPU_BASE_PATH + cpu + nodeSuffix));
            if (!nodeResult.isSuccess()) {
                lastError = nodeResult.getError();
                continue;
            }
            if (nodeResult.getValue()) {
                List<Integer> targets = new ArrayList<>();
                targets.add(cpu);
                return SysfsResult.success(targets);
            }
        }
        String detail = lastError == null ? "no online member exposes the requested node" : lastError;
        return SysfsResult.failure("CPU cluster " + cluster.getMemberRangeLabel()
                + " is unavailable: " + detail);
    }

    /**
     * Reports the kernel online state without changing it. CPUs without an online
     * node (notably CPU 0) are treated as permanently online.
     */
    private SysfsResult<Boolean> isCpuOnline(int cpu) {
        SysfsNode onlineNode = new SysfsNode(FilePath.CPU_BASE_PATH + cpu + CPU_ONLINE_SUFFIX);
        SysfsResult<Boolean> existsResult = repository.exists(onlineNode);
        if (!existsResult.isSuccess()) {
            return SysfsResult.failure(existsResult.getError());
        }
        if (!existsResult.getValue()) {
            return SysfsResult.success(true);
        }
        SysfsResult<String> valueResult = repository.readValue(onlineNode);
        if (!valueResult.isSuccess()) {
            return SysfsResult.failure(valueResult.getError());
        }
        String value = valueResult.getValue();
        if ("1".equals(value)) {
            return SysfsResult.success(true);
        }
        if ("0".equals(value)) {
            return SysfsResult.success(false);
        }
        return SysfsResult.failure("Unexpected online state for CPU " + cpu);
    }
}
