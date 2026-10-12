package com.aero.control.helpers.legacy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.aero.control.helpers.RootShellSession;
import com.aero.control.helpers.SysfsResult;

/**
 * Legacy, device-family-specific overclock support: patches kernel function
 * addresses (read from {@code /proc/kallsyms}) into the {@code
 * /proc/overclock/*} procfs nodes exposed by one specific device family's
 * kernel. This has nothing to do with the generic root-shell session and is
 * only still here for backwards compatibility with that hardware.
 */
public final class LegacyOverclockController {
    public static final String PREF_FREQUENCIES = "legacy_cpu_frequencies_khz";
    public static final String PREF_VOLTAGES = "legacy_cpu_voltage_selections";
    private static final String VOLTAGE_TABLE = "/proc/overclock/mpu_opps";
    private static final String MAX_VOLTAGE = "/proc/overclock/max_vsel";
    private static final String FREQUENCY_TABLE = "/proc/overclock/freq_table";
    private static final String MAX_RATE = "/proc/overclock/max_rate";
    private static final String MIN_FREQUENCY =
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
    private static final String OMAP_ADDRESS =
            "/proc/overclock/omap2_clk_init_cpufreq_table_addr";
    private static final String STATS_ADDRESS =
            "/proc/overclock/cpufreq_stats_update_addr";
    private static final String SYMBOLS = "/proc/kallsyms";

    private final RootShellSession session;

    /** Parsed numeric table from the pre-controller command-string preference. */
    public static final class SavedFrequencyTable {
        private final int[] frequenciesKHz;
        private final int[] voltages;

        private SavedFrequencyTable(int[] frequenciesKHz, int[] voltages) {
            this.frequenciesKHz = frequenciesKHz.clone();
            this.voltages = voltages.clone();
        }

        public int[] getFrequenciesKHz() {
            return frequenciesKHz.clone();
        }

        public int[] getVoltages() {
            return voltages.clone();
        }
    }

    public LegacyOverclockController(RootShellSession session) {
        this.session = session;
    }

    /** Reads the voltage values from the legacy overclock table. */
    public SysfsResult<int[]> readVoltageSelections() {
        String output = session.execute("cat " + RootShellSession.escapeShellArg(VOLTAGE_TABLE));
        if (output == null || output.length() == 0) {
            return SysfsResult.failure("Unable to read the legacy voltage table");
        }

        List<Integer> selections = new ArrayList<>();
        int offset = 0;
        while ((offset = output.indexOf(" vsel=", offset)) >= 0) {
            int start = offset + 6;
            int end = start;
            while (end < output.length() && Character.isDigit(output.charAt(end))) {
                end++;
            }
            if (end == start) {
                return SysfsResult.failure("The legacy voltage table contains an invalid value");
            }
            try {
                selections.add(Integer.parseInt(output.substring(start, end)));
            } catch (NumberFormatException e) {
                return SysfsResult.failure("The legacy voltage table contains an invalid value");
            }
            offset = end;
        }
        if (selections.isEmpty()) {
            return SysfsResult.failure("The legacy voltage table contains no values");
        }
        int[] values = new int[selections.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = selections.get(i);
        }
        return SysfsResult.success(values);
    }

    /** Applies the device-specific frequency/voltage table through the shared root session. */
    public SysfsResult<Boolean> writeFrequencyTable(int[] frequenciesKHz, int[] voltages) {
        SysfsResult<Boolean> validation = validateFrequencyTable(frequenciesKHz, voltages);
        if (!validation.isSuccess()) return validation;

        int maximumVoltage = voltages[0];
        int minimumFrequency = frequenciesKHz[0];
        int maximumFrequency = frequenciesKHz[0];
        for (int i = 1; i < frequenciesKHz.length; i++) {
            maximumVoltage = Math.max(maximumVoltage, voltages[i]);
            minimumFrequency = Math.min(minimumFrequency, frequenciesKHz[i]);
            maximumFrequency = Math.max(maximumFrequency, frequenciesKHz[i]);
        }
        StringBuilder commands = new StringBuilder("status=0\n");
        appendWrite(commands, MAX_VOLTAGE, Integer.toString(maximumVoltage));
        int remaining = frequenciesKHz.length;
        for (int i = 0; i < frequenciesKHz.length; i++) {
            int frequency = frequenciesKHz[i];
            appendWrite(commands, VOLTAGE_TABLE,
                    remaining + " " + frequency + "000 " + voltages[i]);
            appendWrite(commands, FREQUENCY_TABLE, i + " " + frequency);
            remaining--;
        }
        appendWrite(commands, MAX_RATE, Integer.toString(maximumFrequency));
        appendWrite(commands, MIN_FREQUENCY, Integer.toString(minimumFrequency));
        commands.append("if [ \"$status\" -eq 0 ]; then printf 'AERO_OK'; ")
                .append("else printf 'AERO_ERROR'; fi");
        String result = session.execute(commands.toString());
        if (!"AERO_OK".equals(result)) {
            return SysfsResult.failure("Unable to apply the legacy frequency table");
        }
        return SysfsResult.success(Boolean.TRUE);
    }

    static SysfsResult<Boolean> validateFrequencyTable(int[] frequenciesKHz, int[] voltages) {
        if (frequenciesKHz == null || voltages == null
                || frequenciesKHz.length == 0 || frequenciesKHz.length != voltages.length) {
            return SysfsResult.failure("Frequency and voltage tables must have matching values");
        }
        for (int i = 0; i < frequenciesKHz.length; i++) {
            if (frequenciesKHz[i] <= 0) {
                return SysfsResult.failure("CPU frequencies must be positive");
            }
            if (voltages[i] < 16 || voltages[i] > 79) {
                return SysfsResult.failure("Voltage selections must be between 16 and 79");
            }
            for (int j = 0; j < i; j++) {
                if (frequenciesKHz[i] == frequenciesKHz[j]) {
                    return SysfsResult.failure("CPU frequencies must not be duplicated");
                }
            }
        }
        return SysfsResult.success(Boolean.TRUE);
    }

    /**
     * Migrates and applies command tables saved by earlier releases without executing
     * their persisted shell strings.
     */
    public SysfsResult<Boolean> applySavedCommands(String[] savedCommands) {
        SysfsResult<SavedFrequencyTable> parsed = parseSavedCommands(savedCommands);
        if (!parsed.isSuccess()) return SysfsResult.failure(parsed.getError());
        return writeFrequencyTable(parsed.getValue().getFrequenciesKHz(),
                parsed.getValue().getVoltages());
    }

    /** Parses the exact historic table grammar without executing any stored command text. */
    public SysfsResult<SavedFrequencyTable> parseSavedCommands(String[] savedCommands) {
        if (savedCommands == null || savedCommands.length == 0) {
            return SysfsResult.failure("No saved legacy overclock table was provided");
        }
        Map<Integer, Integer> frequencies = new TreeMap<>();
        Map<Integer, Integer> voltageFrequenciesByRemaining = new TreeMap<>();
        Map<Integer, Integer> voltagesByRemaining = new TreeMap<>();
        Integer maxVoltage = null;
        Integer maxRate = null;
        Integer minFrequency = null;
        int tableSize = 0;
        for (String savedCommand : savedCommands) {
            if (savedCommand == null) {
                return SysfsResult.failure("Saved legacy overclock table is invalid");
            }
            String[] parts = savedCommand.trim().split("\\s+");
            try {
                if (parts.length == 4 && "echo".equals(parts[0]) && ">".equals(parts[2])) {
                    int value = Integer.parseInt(parts[1]);
                    if (MAX_VOLTAGE.equals(parts[3])) {
                        if (maxVoltage != null) {
                            return SysfsResult.failure("Saved legacy voltage limit is duplicated");
                        }
                        maxVoltage = value;
                    } else if (MAX_RATE.equals(parts[3])) {
                        if (maxRate != null) {
                            return SysfsResult.failure("Saved legacy maximum rate is duplicated");
                        }
                        maxRate = value;
                    } else if (MIN_FREQUENCY.equals(parts[3])) {
                        if (minFrequency != null) {
                            return SysfsResult.failure("Saved legacy minimum frequency is duplicated");
                        }
                        minFrequency = value;
                    } else {
                        return SysfsResult.failure("Saved legacy overclock table has an unknown node");
                    }
                } else if (parts.length == 6 && "echo".equals(parts[0])
                        && ">".equals(parts[4]) && VOLTAGE_TABLE.equals(parts[5])) {
                    int remaining = Integer.parseInt(parts[1]);
                    String frequency = parts[2];
                    if (!frequency.endsWith("000")) {
                        return SysfsResult.failure("Saved legacy voltage table is invalid");
                    }
                    if (remaining <= 0) {
                        return SysfsResult.failure("Saved legacy voltage table is invalid");
                    }
                    tableSize = Math.max(tableSize, remaining);
                    if (voltagesByRemaining.put(remaining,
                            Integer.parseInt(parts[3])) != null) {
                        return SysfsResult.failure("Saved legacy voltage table is invalid");
                    }
                    voltageFrequenciesByRemaining.put(remaining, Integer.parseInt(
                            frequency.substring(0, frequency.length() - 3)));
                } else if (parts.length == 5 && "echo".equals(parts[0])
                        && ">".equals(parts[3]) && FREQUENCY_TABLE.equals(parts[4])) {
                    int index = Integer.parseInt(parts[1]);
                    if (frequencies.put(index, Integer.parseInt(parts[2])) != null) {
                        return SysfsResult.failure("Saved legacy frequency table is invalid");
                    }
                } else {
                    return SysfsResult.failure("Saved legacy overclock table is invalid");
                }
            } catch (NumberFormatException e) {
                return SysfsResult.failure("Saved legacy overclock table contains invalid numbers");
            }
        }
        if (frequencies.isEmpty() || frequencies.size() != voltagesByRemaining.size()
                || frequencies.size() != voltageFrequenciesByRemaining.size()
                || frequencies.size() != tableSize
                || maxVoltage == null || maxRate == null || minFrequency == null) {
            return SysfsResult.failure("Saved legacy overclock table is incomplete");
        }
        int[] frequencyValues = new int[frequencies.size()];
        int[] voltageValues = new int[voltagesByRemaining.size()];
        int index = 0;
        for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
            int remaining = tableSize - index;
            Integer savedVoltage = voltagesByRemaining.get(remaining);
            Integer savedVoltageFrequency = voltageFrequenciesByRemaining.get(remaining);
            if (entry.getKey() != index || savedVoltage == null
                    || !entry.getValue().equals(savedVoltageFrequency)) {
                return SysfsResult.failure("Saved legacy overclock table is incomplete");
            }
            if (entry.getValue() <= 0 || savedVoltage < 16 || savedVoltage > 79) {
                return SysfsResult.failure("Saved legacy overclock table contains out-of-range values");
            }
            frequencyValues[index] = entry.getValue();
            voltageValues[index] = savedVoltage;
            index++;
        }
        if (maxVoltage != voltageValues[0]
                || maxRate != frequencyValues[0]
                || minFrequency != frequencyValues[frequencyValues.length - 1]) {
            return SysfsResult.failure("Saved legacy overclock table is inconsistent");
        }
        return SysfsResult.success(new SavedFrequencyTable(frequencyValues, voltageValues));
    }

    /** Reads kallsyms and initializes the device-specific overclock address nodes. */
    public SysfsResult<Boolean> initialize() {
        String available = session.execute("if test -e "
                + RootShellSession.escapeShellArg(OMAP_ADDRESS) + " && test -e "
                + RootShellSession.escapeShellArg(STATS_ADDRESS)
                + "; then printf 'yes'; else printf 'no'; fi");
        if (!"yes".equals(available)) {
            return SysfsResult.failure("Legacy overclock address nodes are unavailable");
        }

        String omapAddress = readSymbolAddress("omap2_clk_init_cpufreq_table");
        String statsAddress = readSymbolAddress("cpufreq_stats_update");
        if (omapAddress == null || statsAddress == null) {
            return SysfsResult.failure("Unable to read legacy overclock symbols");
        }

        StringBuilder commands = new StringBuilder("status=0\n");
        appendWrite(commands, OMAP_ADDRESS, "0x" + omapAddress);
        appendWrite(commands, STATS_ADDRESS, "0x" + statsAddress);
        commands.append("if [ \"$status\" -eq 0 ]; then printf 'AERO_OK'; ")
                .append("else printf 'AERO_ERROR'; fi");
        if (!"AERO_OK".equals(session.execute(commands.toString()))) {
            return SysfsResult.failure("Unable to initialize legacy overclock addresses");
        }
        return SysfsResult.success(Boolean.TRUE);
    }

    private String readSymbolAddress(String symbol) {
        String output = session.execute("busybox egrep "
                + RootShellSession.escapeShellArg(symbol + "$") + " "
                + RootShellSession.escapeShellArg(SYMBOLS));
        if (output == null || output.length() < 8) {
            return null;
        }
        String address = output.substring(0, 8);
        for (int i = 0; i < address.length(); i++) {
            if (Character.digit(address.charAt(i), 16) < 0) {
                return null;
            }
        }
        return address;
    }

    private void appendWrite(StringBuilder commands, String path, String value) {
        commands.append("printf '%s\\n' ")
                .append(RootShellSession.escapeShellArg(value))
                .append(" > ")
                .append(RootShellSession.escapeShellArg(path))
                .append(" || status=1\n");
    }
}
