package com.aero.control.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** CPU frequency operations exposed through the typed sysfs repository. */
public final class CpuController {
    private static final String CPU_ONLINE_SUFFIX = "/online";
    private static final Pattern VOLTAGE_ROW =
            Pattern.compile("^.+?:\\s*(\\d+)\\s*(?:mV)?\\s*$");
    private final SysfsRepository repository;

    /** Creates a controller backed by the supplied sysfs repository. */
    CpuController(SysfsRepository repository) {
        this.repository = repository;
    }

    /** Reads the CPU voltage table as a space-separated list of millivolt values. */
    public SysfsResult<String> readVoltageValues() {
        SysfsResult<String[]> rows = readVoltageTable();
        if (!rows.isSuccess()) return SysfsResult.failure(rows.getError());
        List<Integer> values = parseVoltageTable(joinRows(rows.getValue()));
        return SysfsResult.success(joinIntegers(values));
    }

    /** Reads the formatted voltage-table rows used to label the voltage controls. */
    public SysfsResult<String[]> readVoltageTable() {
        SysfsResult<String> table = repository.readValue(new SysfsNode(FilePath.VOLTAGE_PATH));
        if (!table.isSuccess()) return SysfsResult.failure(table.getError());
        List<Integer> values = parseVoltageTable(table.getValue());
        if (values == null || values.isEmpty()) {
            return SysfsResult.<String[]>failure(
                    "CPU voltage table has an unsupported format");
        }
        return SysfsResult.success(table.getValue().split("\\r?\\n"));
    }

    /** Writes the CPU voltage table and verifies the readback values row by row. */
    public SysfsResult<String> writeVoltageValues(String value) {
        SysfsNode node = new SysfsNode(FilePath.VOLTAGE_PATH);
        SysfsResult<String> currentTable = repository.readValue(node);
        if (!currentTable.isSuccess()) return SysfsResult.failure(currentTable.getError());
        List<Integer> currentValues = parseVoltageTable(currentTable.getValue());
        List<Integer> requestedValues = parseVoltageValues(value);
        if (currentValues == null || requestedValues == null
                || currentValues.size() != requestedValues.size()) {
            return SysfsResult.failure("CPU voltage values do not match the available table");
        }
        final List<Integer> expectedValues = requestedValues;
        return repository.writeValue(node, joinIntegers(requestedValues),
                new SysfsRepository.ReadbackVerifier() {
                    @Override
                    public boolean matches(String requestedValue, String readback) {
                        List<Integer> actualValues = parseVoltageTable(readback);
                        return expectedValues.equals(actualValues);
                    }
                });
    }

    private static List<Integer> parseVoltageTable(String table) {
        if (table == null) return null;
        List<Integer> values = new ArrayList<>();
        String[] rows = table.split("\\r?\\n");
        for (String row : rows) {
            Matcher matcher = VOLTAGE_ROW.matcher(row.trim());
            if (!matcher.matches()) return null;
            try {
                values.add(Integer.valueOf(matcher.group(1)));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return values;
    }

    private static List<Integer> parseVoltageValues(String value) {
        if (value == null || value.trim().length() == 0) return null;
        String[] tokens = value.trim().split("\\s+");
        List<Integer> values = new ArrayList<>();
        for (String token : tokens) {
            if (!token.matches("\\d+")) return null;
            try {
                values.add(Integer.valueOf(token));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return values;
    }

    private static String joinIntegers(List<Integer> values) {
        StringBuilder result = new StringBuilder();
        for (Integer value : values) {
            if (result.length() > 0) result.append(' ');
            result.append(value);
        }
        return result.toString();
    }

    private static String joinRows(String[] rows) {
        StringBuilder result = new StringBuilder();
        for (String row : rows) {
            if (result.length() > 0) result.append('\n');
            result.append(row);
        }
        return result.toString();
    }

    /** Returns the frequencies supported by a CPU in the requested read mode. */
    public SysfsResult<String[]> readAvailableFrequencies(int cpu, ReadMode mode) {
        return repository.readValues(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_AVAILABLE_FREQ_SUFFIX), mode);
    }

    /** Reads the frequency capabilities exposed by an active CPU cluster policy. */
    public SysfsResult<String[]> readAvailableFrequencies(
            CpuClusterHelper.Cluster cluster, ReadMode mode) {
        SysfsResult<List<Integer>> targets = getFrequencyWriteTargets(cluster, true);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        return readAvailableFrequencies(targets.getValue().get(0), mode);
    }

    /** Returns the metric nodes associated with a cluster for batched read-only polling. */
    public String[] getFrequencyMetricPaths(CpuClusterHelper.Cluster cluster) {
        int cpu = cluster.getRepresentativeCpu();
        String cpuPath = FilePath.CPU_BASE_PATH + cpu;
        return new String[] {
                cpuPath + FilePath.CPU_AVAILABLE_FREQ_SUFFIX,
                cpuPath + FilePath.CPU_MIN_FREQ,
                cpuPath + FilePath.CPU_MAX_FREQ
        };
    }

    /** Returns the configured maximum frequency for a CPU. */
    public SysfsResult<String> readMaxFrequency(int cpu) {
        return repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ));
    }

    /** Reads the maximum frequency from an active policy node in a CPU cluster. */
    public SysfsResult<String> readMaxFrequency(CpuClusterHelper.Cluster cluster) {
        SysfsResult<List<Integer>> targets = getFrequencyWriteTargets(cluster, true);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        return readMaxFrequency(targets.getValue().get(0));
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

    /** Reads the minimum frequency from an active policy node in a CPU cluster. */
    public SysfsResult<String> readMinFrequency(CpuClusterHelper.Cluster cluster) {
        SysfsResult<List<Integer>> targets = getFrequencyWriteTargets(cluster, false);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        return readMinFrequency(targets.getValue().get(0));
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

    /** Reads the governor capabilities exposed by an active CPU cluster policy. */
    public SysfsResult<String[]> readAvailableGovernors(CpuClusterHelper.Cluster cluster) {
        SysfsResult<List<Integer>> targets = getGovernorWriteTargets(cluster);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        return readAvailableGovernors(targets.getValue().get(0));
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

    /** Reads the currently active governor for a cluster's available policy node. */
    public SysfsResult<String> readGovernor(CpuClusterHelper.Cluster cluster) {
        SysfsResult<List<Integer>> targets = getGovernorWriteTargets(cluster);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        return readGovernor(targets.getValue().get(0));
    }

    /** Writes a cluster governor only when it is among the kernel-advertised capabilities. */
    public SysfsResult<String> writeGovernor(
            CpuClusterHelper.Cluster cluster, String value) {
        SysfsResult<List<Integer>> targets = getGovernorWriteTargets(cluster);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        SysfsResult<String[]> available = readAvailableGovernors(targets.getValue().get(0));
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        if (!contains(available.getValue(), value)) {
            return SysfsResult.failure("Governor is not supported by this CPU cluster");
        }
        int cpu = targets.getValue().get(0);
        return repository.writeValue(new SysfsNode(FilePath.CPU_BASE_PATH + cpu
                + FilePath.CURRENT_GOV_AVAILABLE, Arrays.asList(available.getValue())), value);
    }

    /** Writes and verifies a cluster maximum from its advertised frequency capabilities. */
    public SysfsResult<String> writeMaxFrequency(
            CpuClusterHelper.Cluster cluster, String value) {
        return writeClusterFrequency(cluster, value, true);
    }

    /** Writes and verifies a cluster minimum from its advertised frequency capabilities. */
    public SysfsResult<String> writeMinFrequency(
            CpuClusterHelper.Cluster cluster, String value) {
        return writeClusterFrequency(cluster, value, false);
    }

    /**
     * Applies a minimum/maximum pair without transiently crossing the current limits.
     * Both requested values must be advertised by the kernel and the pair must be valid.
     */
    public SysfsResult<Boolean> writeFrequencyLimits(
            CpuClusterHelper.Cluster cluster, String minimum, String maximum) {
        SysfsResult<List<Integer>> targets = getFrequencyWriteTargets(cluster, true);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        int cpu = targets.getValue().get(0);
        SysfsResult<String[]> available = readAvailableFrequencies(cpu, ReadMode.RAW);
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        if (!contains(available.getValue(), minimum)
                || !contains(available.getValue(), maximum)) {
            return SysfsResult.failure("Frequency is not supported by this CPU cluster");
        }

        final int requestedMinimum;
        final int requestedMaximum;
        try {
            requestedMinimum = Integer.parseInt(minimum.trim());
            requestedMaximum = Integer.parseInt(maximum.trim());
        } catch (NumberFormatException e) {
            return SysfsResult.failure("CPU frequencies must be numeric");
        }
        if (requestedMinimum > requestedMaximum) {
            return SysfsResult.failure("CPU minimum frequency exceeds maximum frequency");
        }

        SysfsResult<String> currentMinimum = readMinFrequency(cpu);
        SysfsResult<String> currentMaximum = readMaxFrequency(cpu);
        if (!currentMinimum.isSuccess()) return SysfsResult.failure(currentMinimum.getError());
        if (!currentMaximum.isSuccess()) return SysfsResult.failure(currentMaximum.getError());

        final int oldMinimum;
        final int oldMaximum;
        try {
            oldMinimum = Integer.parseInt(currentMinimum.getValue().trim());
            oldMaximum = Integer.parseInt(currentMaximum.getValue().trim());
        } catch (NumberFormatException e) {
            return SysfsResult.failure("Current CPU frequency limits are invalid");
        }

        boolean writeMinimumFirst = requestedMaximum < oldMinimum
                || (requestedMinimum <= oldMaximum && requestedMaximum < oldMaximum);
        SysfsResult<String> first = writeMinimumFirst
                ? writeMinFrequency(cluster, minimum)
                : writeMaxFrequency(cluster, maximum);
        if (!first.isSuccess()) return SysfsResult.failure(first.getError());

        SysfsResult<String> second = writeMinimumFirst
                ? writeMaxFrequency(cluster, maximum)
                : writeMinFrequency(cluster, minimum);
        if (!second.isSuccess()) {
            return SysfsResult.failure("First CPU frequency limit changed, but the second "
                    + "write failed: " + second.getError());
        }
        return SysfsResult.success(Boolean.TRUE);
    }

    private SysfsResult<String> writeClusterFrequency(
            CpuClusterHelper.Cluster cluster, String value, boolean maximum) {
        SysfsResult<List<Integer>> targets = getFrequencyWriteTargets(cluster, maximum);
        if (!targets.isSuccess()) return SysfsResult.failure(targets.getError());
        int cpu = targets.getValue().get(0);
        SysfsResult<String[]> available = readAvailableFrequencies(cpu, ReadMode.RAW);
        if (!available.isSuccess()) return SysfsResult.failure(available.getError());
        if (!contains(available.getValue(), value)) {
            return SysfsResult.failure("Frequency is not supported by this CPU cluster");
        }

        String oppositeSuffix = maximum ? FilePath.CPU_MIN_FREQ : FilePath.CPU_MAX_FREQ;
        SysfsResult<String> opposite = repository.readValue(new SysfsNode(
                FilePath.CPU_BASE_PATH + cpu + oppositeSuffix));
        if (!opposite.isSuccess()) {
            return SysfsResult.failure(opposite.getError());
        }
        try {
            int requested = Integer.parseInt(value.trim());
            int current = Integer.parseInt(opposite.getValue().trim());
            if (maximum ? requested < current : requested > current) {
                return SysfsResult.failure("Frequency would cross the cluster's "
                        + (maximum ? "minimum" : "maximum"));
            }
        } catch (NumberFormatException e) {
            return SysfsResult.failure("CPU frequencies must be numeric");
        }

        String suffix = maximum ? FilePath.CPU_MAX_FREQ : FilePath.CPU_MIN_FREQ;
        return repository.writeValue(new SysfsNode(FilePath.CPU_BASE_PATH + cpu + suffix,
                Arrays.asList(available.getValue())), value);
    }

    private boolean contains(String[] values, String candidate) {
        if (candidate == null) return false;
        for (String value : values) {
            if (candidate.trim().equals(value.trim())) return true;
        }
        return false;
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

    /** Reports whether a CPU is online; CPUs without an online node are treated as online. */
    public SysfsResult<Boolean> readOnlineState(int cpu) {
        if (cpu < 0) {
            return SysfsResult.failure("CPU index must not be negative");
        }
        return isCpuOnline(cpu);
    }
}
