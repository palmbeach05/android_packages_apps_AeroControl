package com.aero.control.helpers.legacy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import com.aero.control.helpers.RootShellSession;
import com.aero.control.helpers.SysfsResult;
import org.junit.Test;

public class LegacyOverclockControllerTest {
    private final LegacyOverclockController controller =
            new LegacyOverclockController(new RootShellSession());

    @Test
    public void acceptsUserSelectedFrequenciesAndVoltageSelectors() {
        assertTrue(LegacyOverclockController.validateFrequencyTable(
                new int[] {1800000, 350000}, new int[] {70, 25}).isSuccess());
    }

    @Test
    public void rejectsNonPositiveFrequencyBeforeShellExecution() {
        assertFalse(LegacyOverclockController.validateFrequencyTable(
                new int[] {0}, new int[] {50}).isSuccess());
    }

    @Test
    public void rejectsMismatchedFrequencyAndVoltageTables() {
        assertFalse(controller.writeFrequencyTable(
                new int[] {300000}, new int[0]).isSuccess());
    }

    @Test
    public void rejectsPersistedShellCommandsInsteadOfExecutingThem() {
        assertFalse(controller.applySavedCommands(new String[] {
                "touch /data/local/tmp/untrusted"
        }).isSuccess());
    }

    @Test
    public void parsesKnownLegacyTableIntoTypedValues() {
        SysfsResult<LegacyOverclockController.SavedFrequencyTable> result =
                controller.parseSavedCommands(new String[] {
                        "echo 50 > /proc/overclock/max_vsel",
                        "echo 1 300000000 50 > /proc/overclock/mpu_opps",
                        "echo 0 300000 > /proc/overclock/freq_table",
                        "echo 300000 > /proc/overclock/max_rate",
                        "echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"
                });

        assertTrue(result.isSuccess());
        assertArrayEquals(new int[] {300000},
                result.getValue().getFrequenciesKHz());
        assertArrayEquals(new int[] {50}, result.getValue().getVoltages());
    }

    @Test
    public void parsesFrequenciesAboveGenericCeilingForDeviceSpecificValidation() {
        SysfsResult<LegacyOverclockController.SavedFrequencyTable> result =
                controller.parseSavedCommands(new String[] {
                        "echo 50 > /proc/overclock/max_vsel",
                        "echo 1 1800000000 50 > /proc/overclock/mpu_opps",
                        "echo 0 1800000 > /proc/overclock/freq_table",
                        "echo 1800000 > /proc/overclock/max_rate",
                        "echo 1800000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"
                });

        assertTrue(result.isSuccess());
        assertArrayEquals(new int[] {1800000},
                result.getValue().getFrequenciesKHz());
    }

    @Test
    public void rejectsCommandInjectionInLegacyTable() {
        assertFalse(controller.parseSavedCommands(new String[] {
                "echo 50 > /proc/overclock/max_vsel; touch /data/local/tmp/untrusted"
        }).isSuccess());
    }

    @Test
    public void rejectsOutOfRangeValuesWhenParsingLegacyTable() {
        assertFalse(controller.parseSavedCommands(new String[] {
        "echo 80 > /proc/overclock/max_vsel",
                "echo 1 300000000 80 > /proc/overclock/mpu_opps",
                "echo 0 300000 > /proc/overclock/freq_table",
        "echo 300000 > /proc/overclock/max_rate",
        "echo 300000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"
        }).isSuccess());
    }
}
