package com.aero.control.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.aero.control.R;
import com.aero.control.helpers.legacy.LegacyOverclockController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies system tuning settings from preferences to kernel sysfs interfaces. Handles
 * CPU frequency, governors, GPU settings, memory parameters, and other kernel tunables
 * both on boot and when manually triggered.
 */
public class settingsHelper {
    private static final String MISC_SETTINGS_STORAGE = "miscSettingsStorage";
    private static final String PREF_CPU_BIG_MAX_FREQ = "big_max_frequency";
    private static final String PREF_CPU_BIG_MIN_FREQ = "big_min_frequency";
    private static final String PREF_CLUSTER_KEY_PREFIX = "cpu_cluster_";
    private static final String PREF_CLUSTER_MAX_FREQ_SUFFIX = "_max_frequency";
    private static final String PREF_CLUSTER_MIN_FREQ_SUFFIX = "_min_frequency";
    private static final String PREF_CLUSTER_GOVERNOR_SUFFIX = "_governor";
    private static final String PREF_CPU_COMMANDS = "cpu_commands";
    private static final String PREF_CPU_MAX_FREQ = "max_frequency";
    private static final String PREF_CPU_MIN_FREQ = "min_frequency";
    private static final String PREF_CURRENT_GOV_AVAILABLE = "set_governor";
    private static final String PREF_CURRENT_GPU_GOV_AVAILABLE = "set_gpu_governor";
    private static final String PREF_DISPLAY_COLOR = "display_control";
    private static final String PREF_RGB_VALUES = "rgbValues";
    private static final String PREF_DOUBLETAP2WAKE = "doubletaptowake";
    private static final String PREF_DYANMIC_FSYNC = "dynFsync";
    private static final String PREF_ENTROPY_SETTINGS = "entropy_settings";
    private static final String PREF_ENTROPY_READ = "entropy_read_wakeup";
    private static final String PREF_ENTROPY_WRITE = "entropy_write_wakeup";
    private static final String PREF_FSYNC = "fsync";
    private static final String PREF_GOV_IO_FILE = "io_scheduler_list";
    private static final String PREF_GPU_CONTROL_ACTIVE = "gpu_control_enable";
    private static final String PREF_GPU_FREQ_MAX = "gpu_max_freq";
    private static final String PREF_KSM = "ksm";
    private static final String PREF_READAHEAD = "read_ahead";
    private static final String PREF_SWEEP2WAKE = "sweeptowake";
    private static final String PREF_TCP_CONGESTION = "tcp_congestion";
    private static final String PREF_TIMER_DELAY = "boot_delay";
    private static final String PREF_WRITEBACK = "writeback";
    private static final String PREF_LED_CHARGING = "led_charging";
    private static final String PREF_MULTI_TOUCH = "multi_touch";
    private static final String PREF_BUTTON_BRIGHTNESS = "button_brightness";
    private String mGPUGov;
    private String mHotplugPath;
    private SharedPreferences mMiscSettings;
    private SharedPreferences prefs;
    private static final HardwareAccessService hardware =
            HardwareServices.hardware();
    private static final ArrayList<HardwareRollback> defaultHardwareProfile = new ArrayList<>();
    private static final GenericHelper genHelper = new GenericHelper();

    private interface HardwareAction {
        SysfsResult<?> apply();
    }

    private interface KernelValue {
        SysfsResult<String> read();
        SysfsResult<String> write(String value);
    }

    private static final class HardwareRollback {
        private final String description;
        private final HardwareAction action;

        private HardwareRollback(String description, HardwareAction action) {
            this.description = description;
            this.action = action;
        }
    }

    /**
     * Applies system settings from the specified profile in a background thread. Optionally
     * shows a toast notification when complete if running at boot time.
     *
     * @param context the context
     * @param Profile the profile name to load settings from, or null for default preferences
     * @param onboot whether this is being called during system boot
     */
    public void setSettings(final Context context, final String Profile, final boolean onboot) {
        new Thread(new Runnable() { // from class: com.aero.control.helpers.settingsHelper.1
            @Override // java.lang.Runnable
            public void run() {
                SysfsResult<Boolean> overclockInitialization =
                        hardware.legacyOverclock().initialize();
                if (overclockInitialization.isSuccess()) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.e("Aero", "Something went really wrong...", e);
                    }
                } else {
                    Log.i("Aero", "Legacy overclock initialization unavailable: "
                            + overclockInitialization.getError());
                }
                settingsHelper.this.doBackground(context, Profile, onboot);
                if (onboot) {
                    Handler h = new Handler(context.getMainLooper());
                    h.post(new Runnable() { // from class: com.aero.control.helpers.settingsHelper.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            Toast.makeText(context, ((Object) context.getText(R.string.app_name)) + ": " + ((Object) context.getText(R.string.pref_finishing_settings)), 1).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Applies system settings from preferences. Supported hardware settings use typed
     * controllers and verified sysfs writes; legacy command preferences are parsed into
     * typed values and never executed as stored shell text. Callers must invoke this from
     * a worker thread.
     *
     * @param context the context to access preferences and resources
     * @param Profile the profile name to load settings from, or null to use default preferences
     * @param onboot whether this is being called during system boot
     */
    public void doBackground(final Context context, String Profile, boolean onboot) {
        if (Profile == null) {
            this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        } else {
            this.prefs = context.getSharedPreferences(Profile, 0);
        }
        this.mMiscSettings = context.getSharedPreferences(MISC_SETTINGS_STORAGE, 0);
        if (onboot) {
            String timer_delay = this.prefs.getString(PREF_TIMER_DELAY, null);
            if (timer_delay != null) {
                int i = 0;
                try {
                    i = Integer.parseInt(timer_delay);
                } catch (NumberFormatException e) {
                }
                try {
                    Thread.sleep(i * 1000 * 60);
                } catch (InterruptedException e2) {
                }
            }
            Handler h = new Handler(context.getMainLooper());
            h.post(new Runnable() { // from class: com.aero.control.helpers.settingsHelper.2
                @Override // java.lang.Runnable
                public void run() {
                    Toast.makeText(context, ((Object) context.getText(R.string.app_name)) + ": " + ((Object) context.getText(R.string.pref_set_on_boot_settings)), 1).show();
                }
            });
        }
        String legacyMaxFreq = this.prefs.getString(PREF_CPU_MAX_FREQ, null);
        String legacyMinFreq = this.prefs.getString(PREF_CPU_MIN_FREQ, null);
        String legacyBigMaxFreq = this.prefs.getString(PREF_CPU_BIG_MAX_FREQ, null);
        String legacyBigMinFreq = this.prefs.getString(PREF_CPU_BIG_MIN_FREQ, null);
        String legacyGovernor = this.prefs.getString(PREF_CURRENT_GOV_AVAILABLE, null);
        boolean useDynamicClusterKeys = hasDynamicClusterKeys(this.prefs.getAll());
        applyLegacyCpuSettings();
        applyEntropySettings(Profile != null);
        String voltage = this.prefs.getString("voltage_values", null);
        if (voltage != null) {
            applyCpuVoltageValues(voltage, Profile != null);
        }
        String gpu_gov = this.prefs.getString(PREF_CURRENT_GPU_GOV_AVAILABLE, null);
        String gpu_max = this.prefs.getString(PREF_GPU_FREQ_MAX, null);
        String display_color = this.prefs.getString(PREF_DISPLAY_COLOR, null);
        Boolean gpu_enb = getSaveBoolean(PREF_GPU_CONTROL_ACTIVE);
        Boolean sweep = getSaveBoolean(PREF_SWEEP2WAKE);
        Boolean doubletap = getSaveBoolean(PREF_DOUBLETAP2WAKE);
        String rgbValues = this.prefs.getString(PREF_RGB_VALUES, null);
        String mem_ios = this.prefs.getString(PREF_GOV_IO_FILE, null);
        String mem_rah = this.prefs.getString(PREF_READAHEAD, null);
        Boolean mem_dfs = getSaveBoolean(PREF_DYANMIC_FSYNC);
        Boolean mem_wrb = getSaveBoolean(PREF_WRITEBACK);
        Boolean mem_fsy = getSaveBoolean(PREF_FSYNC);
        Boolean mem_ksm = getSaveBoolean(PREF_KSM);
        String misc_vib = this.prefs.getString(FilePath.MISC_VIBRATOR_CONTROL_FILE, null);
        String misc_amp = this.prefs.getString(FilePath.MISC_VIBRATOR_CONTROL_FILEAMP, null);
        String misc_thm = this.prefs.getString(FilePath.MISC_THERMAL_CONTROL_FILE, null);
        String misc_tcp = this.prefs.getString(PREF_TCP_CONGESTION, null);
        String misc_vol = this.prefs.getString(FilePath.MISC_HEADSET_VOLUME_BOOST_FILE, null);
        List<CpuClusterHelper.Cluster> clusters = new CpuClusterHelper().getClusters();
        for (int i = 0; i < clusters.size(); i++) {
            CpuClusterHelper.Cluster cluster = clusters.get(i);
            List<Integer> members = cluster.getMembers();
            String freqMax;
            String freqMin;
            String governor;
            if (useDynamicClusterKeys) {
                freqMax = this.prefs.getString(PREF_CLUSTER_KEY_PREFIX + i + PREF_CLUSTER_MAX_FREQ_SUFFIX, null);
                freqMin = this.prefs.getString(PREF_CLUSTER_KEY_PREFIX + i + PREF_CLUSTER_MIN_FREQ_SUFFIX, null);
                governor = this.prefs.getString(PREF_CLUSTER_KEY_PREFIX + i + PREF_CLUSTER_GOVERNOR_SUFFIX, null);
                // Fall back to legacy values for any missing dynamic keys
                boolean isBigCluster = false;
                for (Integer member : members) {
                    if (member >= 4) {
                        isBigCluster = true;
                        break;
                    }
                }
                if (freqMax == null) {
                    freqMax = isBigCluster && legacyBigMaxFreq != null ? legacyBigMaxFreq : legacyMaxFreq;
                }
                if (freqMin == null) {
                    freqMin = isBigCluster && legacyBigMinFreq != null ? legacyBigMinFreq : legacyMinFreq;
                }
                if (governor == null) {
                    governor = legacyGovernor;
                }
            } else {
                freqMax = legacyMaxFreq;
                freqMin = legacyMinFreq;
                governor = legacyGovernor;
                boolean isBigCluster = false;
                for (Integer member : members) {
                    if (member >= 4) {
                        isBigCluster = true;
                        break;
                    }
                }
                if (isBigCluster) {
                    if (legacyBigMaxFreq != null) {
                        freqMax = legacyBigMaxFreq;
                    }
                    if (legacyBigMinFreq != null) {
                        freqMin = legacyBigMinFreq;
                    }
                }
            }
            applyCpuFrequencyLimits(cluster, freqMin, freqMax, Profile != null);
            if (governor != null && genHelper.isSafeShellValue(governor)) {
                applyCpuGovernor(cluster, governor, Profile != null);
            } else if (governor != null) {
                Log.e("Aero", "Rejected unsafe CPU governor for cluster "
                        + cluster.getMemberRangeLabel());
            }
        }
        if (mem_ios != null) {
            applyIoScheduler(mem_ios, Profile != null);
        }
        if (mem_rah != null && genHelper.isSafeShellValue(mem_rah)) {
            applyKernelValue("Read-ahead", FilePath.READAHEAD_PARAMETER, mem_rah, Profile != null);
        }
        if (gpu_gov != null) {
            String[] arr$ = FilePath.GPU_GOV_ARRAY;
            for (String s : arr$) {
                if (genHelper.doesExist(s)) {
                    this.mGPUGov = s;
                }
            }
            applyGpuGovernor(gpu_gov, Profile != null);
        }
        if (gpu_max != null) {
            applyGpuMaximumFrequency(gpu_max, Profile != null);
        }
        applyGpuToggle(GpuController.Toggle.CONTROL, gpu_enb.booleanValue(), Profile != null);
        if (!onboot || this.prefs.contains(PREF_SWEEP2WAKE)) {
            applyGpuToggle(GpuController.Toggle.SWEEP_TO_WAKE,
                    sweep.booleanValue(), Profile != null);
        }
        if (!onboot || this.prefs.contains(PREF_DOUBLETAP2WAKE)) {
            applyGpuToggle(GpuController.Toggle.DOUBLE_TAP_TO_WAKE,
                    doubletap.booleanValue(), Profile != null);
        }
        if (display_color != null) {
            applyDisplayMode(display_color, Profile != null);
        }
        if (rgbValues != null) {
            applyDisplayColor(rgbValues, Profile != null);
        }
        applyLedProperty(PREF_LED_CHARGING, LedController.Property.CHARGING_MODE,
                Profile != null);
        applyLedProperty(PREF_MULTI_TOUCH, LedController.Property.MULTI_TOUCH_POINTS,
                Profile != null);
        applyLedProperty(PREF_BUTTON_BRIGHTNESS, LedController.Property.BUTTON_BRIGHTNESS,
                Profile != null);
        if (genHelper.doesExist(FilePath.DYANMIC_FSYNC)) {
            applyKernelValue("Dynamic fsync", FilePath.DYANMIC_FSYNC,
                    mem_dfs.booleanValue() ? "1" : "0", Profile != null);
        }
        if (genHelper.doesExist(FilePath.WRITEBACK)) {
            applyKernelValue("Writeback", FilePath.WRITEBACK,
                    mem_wrb.booleanValue() ? "1" : "0", Profile != null);
        }
        if (genHelper.doesExist(FilePath.FSYNC)) {
            applyKernelValue("File sync", FilePath.FSYNC,
                    mem_fsy.booleanValue() ? "Y" : "N", Profile != null);
        }
        if (genHelper.doesExist(FilePath.KSM_SETTINGS)) {
            applyKernelValue("KSM", FilePath.KSM_SETTINGS,
                    mem_ksm.booleanValue() ? "1" : "0", Profile != null);
        }
        if (misc_vib != null && genHelper.isSafeShellValue(misc_vib)) {
            applyKernelValue("Vibrator voltage level",
                    FilePath.MISC_VIBRATOR_CONTROL_FILE, misc_vib, Profile != null);
        }
        if (misc_amp != null && genHelper.isSafeShellValue(misc_amp)) {
            applyKernelValue("Vibrator amplitude",
                    FilePath.MISC_VIBRATOR_CONTROL_FILEAMP, misc_amp, Profile != null);
        }
        if (misc_thm != null && genHelper.isSafeShellValue(misc_thm)) {
            applyKernelValue("Thermal threshold",
                    FilePath.MISC_THERMAL_CONTROL_FILE, misc_thm, Profile != null);
        }
        if (misc_tcp != null) {
            applyTcpCongestionControl(misc_tcp, Profile != null);
        }
        if (misc_vol != null && genHelper.isSafeShellValue(misc_vol)) {
            applyKernelValue("Headset volume boost",
                    FilePath.MISC_HEADSET_VOLUME_BOOST_FILE, misc_vol, Profile != null);
        }
        if (this.mMiscSettings != null) {
            Map<String, ?> misc_keys = this.mMiscSettings.getAll();
            Map<String, ?> aero_keys = this.prefs.getAll();
            for (Map.Entry<String, ?> a2 : misc_keys.entrySet()) {
                for (Map.Entry<String, ?> b : aero_keys.entrySet()) {
                    if (a2.getKey().equals(b.getKey()) && b.getValue() != null && genHelper.isSafeShellValue(String.valueOf(b.getValue()))) {
                        applyKernelValue("Miscellaneous setting " + b.getKey(),
                                b.getKey(), String.valueOf(b.getValue()), Profile != null);
                    }
                }
            }
        }
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e5) {
            Log.e("Aero", "Something interrupted the main Thread, try again.", e5);
        }
        try {
            setSubParameters(mem_ios, Profile, gpu_gov);
        } catch (NullPointerException e6) {
            Log.e("Aero", "This shouldn't happen.. Maybe a race condition. ", e6);
        }
    }

    /**
     * Executes the rollback profile to restore default system settings that were saved
     * before applying a custom profile. Clears the rollback queue after execution.
     */
    public void executeDefault() {
        for (int i = defaultHardwareProfile.size() - 1; i >= 0; i--) {
            HardwareRollback rollback = defaultHardwareProfile.get(i);
            SysfsResult<?> rollbackResult = rollback.action.apply();
            logSysfsFailure("default profile " + rollback.description, rollbackResult);
        }
        defaultHardwareProfile.clear();
    }

    private boolean applyKernelValue(final String operation, final String path,
            final String value, boolean saveRollback) {
        if (path == null || !genHelper.isSafeShellValue(value)) {
            Log.e("Aero", "Rejected invalid value for " + operation);
            return false;
        }

        try {
            if (path.startsWith("/proc/")) {
                return applyKernelValue(operation, procfsAccess(new ProcfsNode(path)),
                        value, saveRollback);
            }
            return applyKernelValue(operation, sysfsAccess(new SysfsNode(path)),
                    value, saveRollback);
        } catch (IllegalArgumentException e) {
            Log.e("Aero", "Rejected kernel tuning path for " + operation + ": " + path, e);
            return false;
        }
    }

    private boolean applyKernelValue(final String operation, final KernelValue node,
            final String value, boolean saveRollback) {
        if (!genHelper.isSafeShellValue(value)) {
            Log.e("Aero", "Rejected invalid value for " + operation);
            return false;
        }
        SysfsResult<String> oldValue = node.read();
        if (saveRollback && oldValue.isSuccess()) {
            final String rollbackValue = oldValue.getValue();
            addHardwareRollback(operation, new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return node.write(rollbackValue);
                }
            });
        } else if (saveRollback) {
            Log.w("Aero", "Unable to capture rollback value for " + operation + ": "
                    + oldValue.getError());
        }
        SysfsResult<String> result = node.write(value);
        logSysfsFailure(operation, result);
        return result.isSuccess();
    }

    private KernelValue sysfsAccess(final SysfsNode node) {
        return new KernelValue() {
            @Override
            public SysfsResult<String> read() {
                return hardware.read(node);
            }

            @Override
            public SysfsResult<String> write(String value) {
                return hardware.write(node, value);
            }
        };
    }

    private KernelValue procfsAccess(final ProcfsNode node) {
        return new KernelValue() {
            @Override
            public SysfsResult<String> read() {
                return hardware.procfs().readValue(node);
            }

            @Override
            public SysfsResult<String> write(String value) {
                return hardware.procfs().writeValue(node, value);
            }
        };
    }

    private void applyCpuVoltageValues(String value, boolean saveRollback) {
        if (saveRollback) {
            SysfsResult<String> previous = hardware.cpu().readVoltageValues();
            if (previous.isSuccess()) {
                final String rollbackValue = previous.getValue();
                addHardwareRollback("CPU voltage table", new HardwareAction() {
                    @Override
                    public SysfsResult<?> apply() {
                        return hardware.cpu().writeVoltageValues(rollbackValue);
                    }
                });
            } else {
                Log.w("Aero", "Unable to capture CPU voltage rollback value: "
                        + previous.getError());
            }
        }
        SysfsResult<String> result = hardware.cpu().writeVoltageValues(value);
        logSysfsFailure("CPU voltage table", result);
    }

    private void applyIoScheduler(String value, boolean saveRollback) {
        if (saveRollback) {
            SysfsResult<String> previous = hardware.ioScheduler().readCurrentScheduler();
            if (previous.isSuccess()) {
                final String rollbackValue = previous.getValue();
                addHardwareRollback("I/O scheduler", new HardwareAction() {
                    @Override
                    public SysfsResult<?> apply() {
                        return hardware.ioScheduler().writeScheduler(rollbackValue);
                    }
                });
            } else {
                Log.w("Aero", "Unable to capture I/O scheduler rollback value: "
                        + previous.getError());
            }
        }
        SysfsResult<String> result = hardware.ioScheduler().writeScheduler(value);
        logSysfsFailure("I/O scheduler", result);
    }

    private void applyTcpCongestionControl(String value, boolean saveRollback) {
        if (saveRollback) {
            SysfsResult<String> previous = hardware.network().readCongestionControl();
            if (previous.isSuccess()) {
                final String rollbackValue = previous.getValue();
                addHardwareRollback("TCP congestion control", new HardwareAction() {
                    @Override
                    public SysfsResult<?> apply() {
                        return hardware.network().writeCongestionControl(rollbackValue);
                    }
                });
            } else {
                Log.w("Aero", "Unable to capture TCP congestion rollback value: "
                        + previous.getError());
            }
        }
        SysfsResult<String> result = hardware.network().writeCongestionControl(value);
        logSysfsFailure("TCP congestion control", result);
    }

    private void applyEntropySettings(boolean saveRollback) {
        String readValue = this.prefs.getString(PREF_ENTROPY_READ, null);
        String writeValue = this.prefs.getString(PREF_ENTROPY_WRITE, null);
        Object oldFormat = this.prefs.getAll().get(PREF_ENTROPY_SETTINGS);
        boolean hasTypedSettings = readValue != null || writeValue != null;
        if (hasTypedSettings && oldFormat == null) {
            return;
        }
        if (readValue == null && writeValue == null && oldFormat != null) {
            if (!isLegacyEntropyCommandFormat(oldFormat)) {
                return;
            }
            String[] commands = getLegacyEntropyCommands(oldFormat);
            if (commands == null) {
                Log.e("Aero", "Saved entropy settings have an unsupported format");
                discardLegacyEntropyCommands(oldFormat);
                return;
            }
            for (String command : commands) {
                String[] parts = command.trim().split("\\s+");
                if (parts.length != 4 || !"echo".equals(parts[0]) || !">".equals(parts[2])
                        || !parts[1].matches("\\d+")) {
                    Log.e("Aero", "Rejected malformed saved entropy setting");
                    discardLegacyEntropyCommands(oldFormat);
                    return;
                }
                if (FilePath.RANDOM_READ_WAKEUP.equals(parts[3])) {
                    if (readValue != null) {
                        Log.e("Aero", "Saved read entropy threshold is duplicated");
                        discardLegacyEntropyCommands(oldFormat);
                        return;
                    }
                    readValue = parts[1];
                } else if (FilePath.RANDOM_WRITE_WAKEUP.equals(parts[3])) {
                    if (writeValue != null) {
                        Log.e("Aero", "Saved write entropy threshold is duplicated");
                        discardLegacyEntropyCommands(oldFormat);
                        return;
                    }
                    writeValue = parts[1];
                } else {
                    Log.e("Aero", "Rejected saved entropy path: " + parts[3]);
                    discardLegacyEntropyCommands(oldFormat);
                    return;
                }
            }
            if (readValue == null || writeValue == null) {
                Log.e("Aero", "Saved entropy settings are incomplete");
                discardLegacyEntropyCommands(oldFormat);
                return;
            }
        }
        if (readValue == null && writeValue == null) return;
        if (!isNonNegativeInteger(readValue) || !isNonNegativeInteger(writeValue)) {
            Log.e("Aero", "Entropy thresholds must be non-negative integers");
            discardLegacyEntropyCommands(oldFormat);
            return;
        }

        ProcfsNode readNode = new ProcfsNode(
                FilePath.RANDOM_READ_WAKEUP, 0, Integer.MAX_VALUE);
        ProcfsNode writeNode = new ProcfsNode(
                FilePath.RANDOM_WRITE_WAKEUP, 0, Integer.MAX_VALUE);
        SysfsResult<String> previousReadValue = hardware.procfs().readValue(readNode);
        if (!previousReadValue.isSuccess()) {
            Log.e("Aero", "Unable to capture existing entropy read threshold: "
                    + previousReadValue.getError());
            return;
        }
        boolean readApplied = applyKernelValue("Entropy read threshold",
                procfsAccess(readNode), readValue, saveRollback);
        boolean writeApplied = false;
        if (readApplied) {
            writeApplied = applyKernelValue("Entropy write threshold",
                    procfsAccess(writeNode), writeValue, saveRollback);
            if (!writeApplied) {
                SysfsResult<String> rollbackResult =
                        hardware.procfs().writeValue(readNode, previousReadValue.getValue());
                logSysfsFailure("restore entropy read threshold", rollbackResult);
            }
        }
        if (readApplied && writeApplied && oldFormat != null
                && (isLegacyEntropyCommandFormat(oldFormat)
                || !this.prefs.contains(PREF_ENTROPY_READ))) {
            boolean migrated = this.prefs.edit()
                    .putString(PREF_ENTROPY_READ, readValue)
                    .putString(PREF_ENTROPY_WRITE, writeValue)
                    .putString(PREF_ENTROPY_SETTINGS, "enabled")
                    .commit();
            if (!migrated) {
                Log.e("Aero", "Failed to save migrated entropy settings");
            }
        }
    }

    private String[] getLegacyEntropyCommands(Object value) {
        if (value instanceof Set) {
            Set<?> commands = (Set<?>) value;
            String[] result = new String[commands.size()];
            int index = 0;
            for (Object command : commands) {
                if (!(command instanceof String)) return null;
                result[index++] = (String) command;
            }
            return result;
        }
        if (value instanceof String) {
            String serialized = ((String) value).trim();
            if (serialized.length() >= 2 && serialized.charAt(0) == '['
                    && serialized.charAt(serialized.length() - 1) == ']') {
                serialized = serialized.substring(1, serialized.length() - 1);
            }
            return serialized.length() == 0 ? new String[0] : serialized.split(",");
        }
        return null;
    }

    private void discardLegacyEntropyCommands(Object value) {
        if (!isLegacyEntropyCommandFormat(value)) return;
        if (!this.prefs.edit().remove(PREF_ENTROPY_SETTINGS).commit()) {
            Log.e("Aero", "Failed to remove invalid saved entropy commands");
        }
    }

    private boolean isLegacyEntropyCommandFormat(Object value) {
        return value instanceof Set
                || (value instanceof String
                && !"enabled".equals(((String) value).trim()));
    }

    private boolean isNonNegativeInteger(String value) {
        if (value == null || !value.matches("\\d+")) return false;
        try {
            return Integer.parseInt(value) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addHardwareRollback(String description, HardwareAction action) {
        defaultHardwareProfile.add(new HardwareRollback(description, action));
    }

    private void applyCpuFrequencyLimits(final CpuClusterHelper.Cluster cluster,
            String minimum, String maximum, boolean saveRollback) {
        if (minimum == null && maximum == null) return;
        if ((minimum != null && !genHelper.isSafeShellValue(minimum))
                || (maximum != null && !genHelper.isSafeShellValue(maximum))) {
            Log.e("Aero", "Rejected unsafe CPU frequency for cluster "
                    + cluster.getMemberRangeLabel());
            return;
        }
        final SysfsResult<String> oldMinimum = hardware.cpu().readMinFrequency(cluster);
        final SysfsResult<String> oldMaximum = hardware.cpu().readMaxFrequency(cluster);
        if (!oldMinimum.isSuccess() || !oldMaximum.isSuccess()) {
            Log.e("Aero", "Unable to read current CPU limits for cluster "
                    + cluster.getMemberRangeLabel() + ": "
                    + (!oldMinimum.isSuccess() ? oldMinimum.getError() : oldMaximum.getError()));
            return;
        }
        final String desiredMinimum = minimum == null ? oldMinimum.getValue() : minimum;
        final String desiredMaximum = maximum == null ? oldMaximum.getValue() : maximum;
        if (saveRollback) {
            final String rollbackMinimum = oldMinimum.getValue();
            final String rollbackMaximum = oldMaximum.getValue();
            addHardwareRollback("CPU limits for cluster " + cluster.getMemberRangeLabel(),
                    new HardwareAction() {
                        @Override
                        public SysfsResult<?> apply() {
                            return hardware.cpu().writeFrequencyLimits(
                                    cluster, rollbackMinimum, rollbackMaximum);
                        }
                    });
        }
        SysfsResult<Boolean> result = hardware.cpu().writeFrequencyLimits(
                cluster, desiredMinimum, desiredMaximum);
        logSysfsFailure("CPU limits for cluster " + cluster.getMemberRangeLabel(), result);
    }

    private void applyCpuGovernor(final CpuClusterHelper.Cluster cluster,
            final String governor, boolean saveRollback) {
        final SysfsResult<String> oldGovernor = hardware.cpu().readGovernor(cluster);
        if (!oldGovernor.isSuccess()) {
            Log.e("Aero", "Unable to read CPU governor for cluster "
                    + cluster.getMemberRangeLabel() + ": " + oldGovernor.getError());
        } else if (saveRollback) {
            final String rollbackGovernor = oldGovernor.getValue();
            addHardwareRollback("CPU governor for cluster " + cluster.getMemberRangeLabel(),
                    new HardwareAction() {
                        @Override
                        public SysfsResult<?> apply() {
                            return hardware.cpu().writeGovernor(cluster, rollbackGovernor);
                        }
                    });
        }
        SysfsResult<String> result = hardware.cpu().writeGovernor(cluster, governor);
        logSysfsFailure("CPU governor for cluster " + cluster.getMemberRangeLabel(), result);
    }

    private void applyGpuGovernor(final String governor, boolean saveRollback) {
        final SysfsResult<String> oldGovernor = hardware.gpu().readGovernor();
        if (oldGovernor.isSuccess() && saveRollback) {
            final String rollbackGovernor = oldGovernor.getValue();
            addHardwareRollback("GPU governor", new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return hardware.gpu().writeGovernor(rollbackGovernor);
                }
            });
        }
        SysfsResult<String> result = hardware.gpu().writeGovernor(governor);
        logSysfsFailure("GPU governor", result);
    }

    private void applyGpuMaximumFrequency(final String frequency, boolean saveRollback) {
        final SysfsResult<String> oldFrequency = hardware.gpu().readMaxFrequency();
        if (oldFrequency.isSuccess() && saveRollback) {
            final String rollbackFrequency = oldFrequency.getValue();
            addHardwareRollback("GPU maximum frequency", new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return hardware.gpu().writeMaxFrequency(rollbackFrequency);
                }
            });
        }
        SysfsResult<String> result = hardware.gpu().writeMaxFrequency(frequency);
        logSysfsFailure("GPU maximum frequency", result);
    }

    private void applyGpuToggle(final GpuController.Toggle toggle,
            final boolean enabled, boolean saveRollback) {
        SysfsResult<Boolean> available = hardware.gpu().isToggleAvailable(toggle);
        if (!available.isSuccess()) {
            Log.e("Aero", toggle.name() + " availability check failed: " + available.getError());
            return;
        }
        if (!available.getValue()) return;

        final SysfsResult<Boolean> previous = hardware.gpu().readToggle(toggle);
        if (previous.isSuccess() && saveRollback) {
            final boolean rollbackValue = previous.getValue();
            addHardwareRollback(toggle.name(), new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return hardware.gpu().writeToggle(toggle, rollbackValue);
                }
            });
        }
        SysfsResult<Boolean> result = hardware.gpu().writeToggle(toggle, enabled);
        logSysfsFailure(toggle.name(), result);
    }

    private void applyDisplayMode(final String mode, boolean saveRollback) {
        SysfsResult<Boolean> available = hardware.gpu().isDisplayModeAvailable();
        if (!available.isSuccess()) {
            Log.e("Aero", "Display mode availability check failed: " + available.getError());
            return;
        }
        if (!available.getValue()) return;

        final SysfsResult<String> previous = hardware.gpu().readDisplayMode();
        if (previous.isSuccess() && saveRollback) {
            final String rollbackMode = previous.getValue();
            addHardwareRollback("Display mode", new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return hardware.gpu().writeDisplayMode(rollbackMode);
                }
            });
        }
        SysfsResult<String> result = hardware.gpu().writeDisplayMode(mode);
        logSysfsFailure("Display mode", result);
    }

    private void applyDisplayColor(final String color, boolean saveRollback) {
        SysfsResult<Boolean> available = hardware.displayColor().isAvailable();
        if (!available.isSuccess()) {
            Log.e("Aero", "Display color availability check failed: " + available.getError());
            return;
        }
        if (!available.getValue()) return;

        final SysfsResult<String> previous = hardware.displayColor().readColorValue();
        if (previous.isSuccess() && saveRollback) {
            final String rollbackColor = previous.getValue();
            addHardwareRollback("Display color", new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return hardware.displayColor().writeColorValue(rollbackColor);
                }
            });
        }
        SysfsResult<String> result = hardware.displayColor().writeColorValue(color);
        logSysfsFailure("Display color", result);
    }

    private void applyLedProperty(final String preferenceKey,
            final LedController.Property property, boolean saveRollback) {
        String value = this.prefs.getString(preferenceKey, null);
        if (value == null) return;

        final SysfsResult<String> previous = hardware.led().read(property);
        if (previous.isSuccess() && saveRollback) {
            final String rollbackValue = previous.getValue();
            addHardwareRollback(preferenceKey, new HardwareAction() {
                @Override
                public SysfsResult<?> apply() {
                    return hardware.led().write(property, rollbackValue);
                }
            });
        }
        SysfsResult<String> result = hardware.led().write(property, value);
        logSysfsFailure(preferenceKey, result);
    }

    private void logSysfsFailure(String operation, SysfsResult<?> result) {
        if (!result.isSuccess()) {
            Log.e("Aero", operation + " failed: " + result.getError());
        }
    }

    /**
     * Retrieves a boolean preference value with fallback handling for legacy string-based
     * boolean storage.
     *
     * @param s the preference key
     * @return the boolean value, reading "1" as true and "0" as false from strings
     */
    private Boolean getSaveBoolean(String s) {
        try {
            return Boolean.valueOf(this.prefs.getString(s, "0").equals("1"));
        } catch (ClassCastException e) {
            return Boolean.valueOf(this.prefs.getBoolean(s, false));
        }
    }

    private void applyLegacyCpuSettings() {
        String savedFrequencies = this.prefs.getString(
                LegacyOverclockController.PREF_FREQUENCIES, null);
        String savedVoltages = this.prefs.getString(
                LegacyOverclockController.PREF_VOLTAGES, null);
        LegacyOverclockController controller =
                hardware.legacyOverclock();
        if (savedFrequencies != null || savedVoltages != null) {
            int[] frequencies = parseIntegerValues(savedFrequencies);
            int[] voltages = parseIntegerValues(savedVoltages);
            if (frequencies == null || voltages == null) {
                Log.e("Aero", "Saved legacy CPU frequency table is invalid");
                removeLegacyCpuCommands();
                return;
            }
            SysfsResult<Boolean> result = controller.writeFrequencyTable(frequencies, voltages);
            if (!result.isSuccess()) {
                Log.e("Aero", "Failed to apply saved legacy CPU table: " + result.getError());
            }
            removeLegacyCpuCommands();
            return;
        }

        Object savedCommands = this.prefs.getAll().get(PREF_CPU_COMMANDS);
        if (savedCommands == null) {
            return;
        }
        String[] commands;
        if (savedCommands instanceof Set) {
            Set<?> commandSet = (Set<?>) savedCommands;
            commands = new String[commandSet.size()];
            int index = 0;
            for (Object command : commandSet) {
                if (!(command instanceof String)) {
                    Log.e("Aero", "Saved legacy CPU commands contain an invalid value");
                    removeLegacyCpuCommands();
                    return;
                }
                commands[index++] = (String) command;
            }
        } else if (savedCommands instanceof String) {
            String serialized = ((String) savedCommands).trim();
            if (serialized.length() >= 2 && serialized.charAt(0) == '['
                    && serialized.charAt(serialized.length() - 1) == ']') {
                serialized = serialized.substring(1, serialized.length() - 1);
            }
            commands = serialized.length() == 0 ? new String[0] : serialized.split(",");
            for (int i = 0; i < commands.length; i++) {
                commands[i] = commands[i].trim();
            }
        } else {
            Log.e("Aero", "Saved legacy CPU settings have an unsupported format");
            removeLegacyCpuCommands();
            return;
        }

        SysfsResult<LegacyOverclockController.SavedFrequencyTable> parsed =
                controller.parseSavedCommands(commands);
        if (!parsed.isSuccess()) {
            Log.e("Aero", "Discarding invalid saved legacy CPU commands: "
                    + parsed.getError());
            removeLegacyCpuCommands();
            return;
        }
        LegacyOverclockController.SavedFrequencyTable table = parsed.getValue();
        int[] frequencies = table.getFrequenciesKHz();
        int[] voltages = table.getVoltages();
        SysfsResult<Boolean> result = controller.writeFrequencyTable(frequencies, voltages);
        if (!result.isSuccess()) {
            Log.e("Aero", "Failed to migrate saved legacy CPU table: " + result.getError());
            return;
        }
        boolean migrated = this.prefs.edit()
                .putString(LegacyOverclockController.PREF_FREQUENCIES,
                        serializeIntegerValues(frequencies))
                .putString(LegacyOverclockController.PREF_VOLTAGES,
                        serializeIntegerValues(voltages))
                .remove(PREF_CPU_COMMANDS)
                .commit();
        if (!migrated) {
            Log.e("Aero", "Failed to persist migrated legacy CPU table");
        }
    }

    private void removeLegacyCpuCommands() {
        if (!this.prefs.contains(PREF_CPU_COMMANDS)) return;
        if (!this.prefs.edit().remove(PREF_CPU_COMMANDS).commit()) {
            Log.e("Aero", "Failed to remove saved legacy CPU commands");
        }
    }

    private String serializeIntegerValues(int[] values) {
        StringBuilder serialized = new StringBuilder();
        for (int value : values) {
            if (serialized.length() > 0) serialized.append(',');
            serialized.append(value);
        }
        return serialized.toString();
    }

    private int[] parseIntegerValues(String serialized) {
        if (serialized == null || serialized.trim().length() == 0) {
            return null;
        }
        String[] values = serialized.split(",");
        int[] result = new int[values.length];
        try {
            for (int i = 0; i < values.length; i++) {
                result[i] = Integer.parseInt(values[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }

    /**
     * Checks whether the profile stores at least one dynamic per-cluster CPU
     * key (e.g. "cpu_cluster_0_max_frequency"). Profiles saved before the
     * introduction of CpuClusterHelper only contain the legacy fixed keys.
     */
    private boolean hasDynamicClusterKeys(Map<String, ?> allPrefs) {
        int clusterCount = new CpuClusterHelper().getClusters().size();
        for (String key : allPrefs.keySet()) {
            if (isDynamicClusterKey(key, clusterCount)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a preference key is a valid dynamic per-cluster CPU control key
     * (e.g., "cpu_cluster_0_max_frequency") and falls within the cluster count range.
     *
     * @param key the preference key to check
     * @param clusterCount the number of CPU clusters on this device
     * @return true if the key is a valid dynamic cluster key, false otherwise
     */
    private boolean isDynamicClusterKey(String key, int clusterCount) {
        if (key == null || !key.startsWith(PREF_CLUSTER_KEY_PREFIX)) {
            return false;
        }
        // Determine which suffix this key uses
        String suffix = null;
        if (key.endsWith(PREF_CLUSTER_MAX_FREQ_SUFFIX)) {
            suffix = PREF_CLUSTER_MAX_FREQ_SUFFIX;
        } else if (key.endsWith(PREF_CLUSTER_MIN_FREQ_SUFFIX)) {
            suffix = PREF_CLUSTER_MIN_FREQ_SUFFIX;
        } else if (key.endsWith(PREF_CLUSTER_GOVERNOR_SUFFIX)) {
            suffix = PREF_CLUSTER_GOVERNOR_SUFFIX;
        } else {
            return false;
        }
        // Extract the cluster index between prefix and suffix
        int prefixLen = PREF_CLUSTER_KEY_PREFIX.length();
        int suffixStart = key.length() - suffix.length();
        if (suffixStart <= prefixLen) {
            // No room for an index between prefix and suffix
            return false;
        }
        String indexStr = key.substring(prefixLen, suffixStart);
        // Validate that indexStr is a valid non-negative decimal integer
        if (indexStr.isEmpty()) {
            return false;
        }
        for (int i = 0; i < indexStr.length(); i++) {
            char c = indexStr.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        // Parse the index and ensure it's within range
        try {
            int clusterIndex = Integer.parseInt(indexStr);
            return clusterIndex >= 0 && clusterIndex < clusterCount;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Applies advanced settings for I/O scheduler parameters, VM/Dalvik settings, hotplug
     * control, CPU boost, GPU governor tuning, and per-governor CPU settings. Runs after
     * the primary settings have been applied.
     *
     * @param mem_ios the I/O scheduler name if I/O settings should be applied, null otherwise
     * @param Profile the profile name if building a rollback profile, null otherwise
     * @param gpu_gov the GPU governor name if GPU governor settings should be applied, null otherwise
     * @throws NullPointerException if essential paths are null
     */
    private void setSubParameters(String mem_ios, String Profile, String gpu_gov) throws NullPointerException {
        String[] completeVMSettings = listProcfsParameters(
                FilePath.DALVIK_TWEAK, "VM parameters");
        SysfsResult<String> currentGovernor = hardware.read(new SysfsNode(FilePath.GOV_FILE));
        String cpu_governor = null;
        if (currentGovernor.isSuccess() && genHelper.isSafeShellValue(currentGovernor.getValue())) {
            cpu_governor = currentGovernor.getValue();
        } else {
            Log.w("Aero", "Unable to read the active CPU governor: "
                    + currentGovernor.getError());
        }
        String[] completeGovernorSettingList = cpu_governor == null ? new String[0]
                : listSysfsParameters(FilePath.CPU_GOV_BASE + cpu_governor,
                        "CPU governor parameters");
        if (mem_ios != null) {
            String[] completeIOSchedulerSettings = listSysfsParameters(
                    FilePath.GOV_IO_PARAMETER, "I/O scheduler parameters");
            for (String b : completeIOSchedulerSettings) {
                String path = FilePath.GOV_IO_PARAMETER + "/" + b;
                String ioSettings = this.prefs.getString(path, null);
                if (ioSettings != null && genHelper.isSafeShellValue(ioSettings)) {
                    applyKernelValue("I/O scheduler parameter " + b,
                            path, ioSettings, Profile != null);
                }
            }
        }
        for (String c : completeVMSettings) {
            String path = FilePath.DALVIK_TWEAK + "/" + c;
            String vmSettings = this.prefs.getString(path, null);
            if (vmSettings != null && genHelper.isSafeShellValue(vmSettings)) {
                applyKernelValue("VM setting " + c, path, vmSettings, Profile != null);
            }
        }
        String[] arr$ = FilePath.HOTPLUG_PATH;
        for (String s : arr$) {
            if (genHelper.doesExist(s)) {
                this.mHotplugPath = s;
            }
        }
        if (genHelper.doesExist(this.mHotplugPath)) {
            String[] completeHotplugSettings = listSysfsParameters(
                    this.mHotplugPath, "CPU hotplug parameters");
            for (String d : completeHotplugSettings) {
                String path = this.mHotplugPath + "/" + d;
                String hotplugSettings = this.prefs.getString(path, null);
                if (hotplugSettings != null && genHelper.isSafeShellValue(hotplugSettings)) {
                    applyKernelValue("Hotplug setting " + d, path,
                            hotplugSettings, Profile != null);
                }
            }
        }
        if (genHelper.doesExist(FilePath.CPU_BOOST)) {
            String[] completeCPUBOOSTSettings = listSysfsParameters(
                    FilePath.CPU_BOOST, "CPU boost parameters");
            for (String d2 : completeCPUBOOSTSettings) {
                String path = FilePath.CPU_BOOST + "/" + d2;
                String cpuBoostSettings = this.prefs.getString(path, null);
                if (cpuBoostSettings != null && genHelper.isSafeShellValue(cpuBoostSettings)) {
                    applyKernelValue("CPU boost setting " + d2, path,
                            cpuBoostSettings, Profile != null);
                }
            }
        }
        if (genHelper.doesExist(FilePath.GPU_GOV_PATH)) {
            String[] completeGPUGovSettings = listSysfsParameters(
                    FilePath.GPU_GOV_PATH, "GPU governor parameters");
            for (String e : completeGPUGovSettings) {
                String path = FilePath.GPU_GOV_PATH + "/" + e;
                String gpugovSettings = this.prefs.getString(path, null);
                if (gpugovSettings != null && genHelper.isSafeShellValue(gpugovSettings)) {
                    applyKernelValue("GPU module setting " + e, path,
                            gpugovSettings, Profile != null);
                }
            }
        }
        if (cpu_governor != null) {
            for (String b2 : completeGovernorSettingList) {
                String governorSetting = this.prefs.getString(FilePath.CPU_GOV_BASE + cpu_governor + "/" + b2, null);
                if (governorSetting != null && genHelper.isSafeShellValue(governorSetting)) {
                    String path = FilePath.CPU_GOV_BASE + cpu_governor + "/" + b2;
                    applyKernelValue("CPU governor parameter " + b2,
                            path, governorSetting, Profile != null);
                }
            }
        }
        if (gpu_gov != null && genHelper.isSafeShellValue(gpu_gov)) {
            if (this.mGPUGov == null) {
                String[] arr$2 = FilePath.GPU_GOV_ARRAY;
                for (String s2 : arr$2) {
                    if (genHelper.doesExist(s2)) {
                        this.mGPUGov = s2;
                    }
                }
            }
            String[] completeGPUGovernorSetting = listSysfsParameters(
                    this.mGPUGov + gpu_gov, "Selected GPU governor parameters");
            for (String b3 : completeGPUGovernorSetting) {
                String path = this.mGPUGov + gpu_gov + "/" + b3;
                String governorSetting2 = this.prefs.getString(path, null);
                if (governorSetting2 != null && genHelper.isSafeShellValue(governorSetting2)) {
                    applyKernelValue("GPU governor parameter " + b3,
                            path, governorSetting2, Profile != null);
                }
            }
        }
    }

    private static String[] listSysfsParameters(String path, String description) {
        try {
            SysfsResult<String[]> result = hardware.dynamicTunables()
                    .listSysfsParameters(new SysfsDirectory(path));
            if (result.isSuccess()) {
                return result.getValue();
            }
            Log.w("Aero", "Unable to discover " + description + ": " + result.getError());
        } catch (IllegalArgumentException e) {
            Log.w("Aero", "Rejected path while discovering " + description, e);
        }
        return new String[0];
    }

    private static String[] listProcfsParameters(String path, String description) {
        try {
            SysfsResult<String[]> result = hardware.dynamicTunables()
                    .listProcfsParameters(new ProcfsDirectory(path));
            if (result.isSuccess()) {
                return result.getValue();
            }
            Log.w("Aero", "Unable to discover " + description + ": " + result.getError());
        } catch (IllegalArgumentException e) {
            Log.w("Aero", "Rejected path while discovering " + description, e);
        }
        return new String[0];
    }
}
