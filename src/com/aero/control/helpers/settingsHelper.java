package com.aero.control.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import com.aero.control.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
    private static final String PREF_DOUBLETAP2WAKE = "doubletaptowake";
    private static final String PREF_DYANMIC_FSYNC = "dynFsync";
    private static final String PREF_ENTROPY_SETTINGS = "entropy_settings";
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
    private String gpu_file;
    private String mGPUGov;
    private String mHotplugPath;
    private SharedPreferences mMiscSettings;
    private SharedPreferences prefs;
    private static final shellHelper shell = shellHelper.instance();
    private static final shellHelper shellPara = shellHelper.forceInstance();
    private static final ArrayList<String> defaultProfile = new ArrayList<>();
    private static final GenericHelper genHelper = new GenericHelper();

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
                if (settingsHelper.shell.setOverclockAddress()) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        Log.e("Aero", "Something went really wrong...", e);
                    }
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
     * Applies all system settings from preferences. This method reads CPU, GPU, memory,
     * and other kernel parameters from SharedPreferences and queues the corresponding
     * shell commands to apply them. Executes synchronously on the calling thread; callers
     * must invoke this from a worker thread when background execution is required.
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
        try {
            HashSet<String> hashcpu_cmd = (HashSet) this.prefs.getStringSet(PREF_CPU_COMMANDS, null);
            if (hashcpu_cmd != null) {
                for (String cmd : hashcpu_cmd) {
                    shell.queueWork(cmd);
                }
            }
        } catch (ClassCastException e3) {
            String cpu_cmd = this.prefs.getString(PREF_CPU_COMMANDS, null);
            if (cpu_cmd != null) {
                String[] array = cpu_cmd.substring(1, cpu_cmd.length() - 1).split(",");
                for (String cmd2 : array) {
                    shell.queueWork(cmd2);
                }
            }
        }
        try {
            HashSet<String> hashent_cmd = (HashSet) this.prefs.getStringSet(PREF_ENTROPY_SETTINGS, null);
            if (hashent_cmd != null) {
                for (String cmd3 : hashent_cmd) {
                    shell.queueWork(cmd3);
                }
            }
        } catch (ClassCastException e4) {
            String ent_cmd = this.prefs.getString(PREF_ENTROPY_SETTINGS, null);
            if (ent_cmd != null) {
                String[] array2 = ent_cmd.substring(1, ent_cmd.length() - 1).split(",");
                for (String cmd4 : array2) {
                    shell.queueWork(cmd4);
                }
            }
        }
        String voltage = this.prefs.getString("voltage_values", null);
        if (voltage != null && genHelper.isSafeShellValue(voltage)) {
            shell.queueWork("echo " + voltage + " > " + FilePath.VOLTAGE_PATH);
        }
        String gpu_gov = this.prefs.getString(PREF_CURRENT_GPU_GOV_AVAILABLE, null);
        String gpu_max = this.prefs.getString(PREF_GPU_FREQ_MAX, null);
        String display_color = this.prefs.getString(PREF_DISPLAY_COLOR, null);
        Boolean gpu_enb = getSaveBoolean(PREF_GPU_CONTROL_ACTIVE);
        Boolean sweep = getSaveBoolean(PREF_SWEEP2WAKE);
        Boolean doubletap = getSaveBoolean(PREF_DOUBLETAP2WAKE);
        String rgbValues = this.prefs.getString("rgbValues", null);
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
        ArrayList<String> governorSettings = new ArrayList<>();
        boolean governorApplied = false;
        List<CpuClusterHelper.Cluster> clusters = new CpuClusterHelper().getClusters();
        for (int i = 0; i < clusters.size(); i++) {
            CpuClusterHelper.Cluster cluster = clusters.get(i);
            List<Integer> members = cluster.getMembers();
            int representativeCpu = cluster.getRepresentativeCpu();
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
            if (freqMax != null && genHelper.isSafeShellValue(freqMax)) {
                String rollbackMax = shell.getInfo(FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CPU_MAX_FREQ);
                for (Integer cpu : members) {
                    boolean hasOnlineControl = genHelper.doesExist(FilePath.CPU_BASE_PATH + cpu + "/online");
                    if (hasOnlineControl) {
                        shell.queueWork("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                    }
                    shell.queueWork("chmod 0666 " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ);
                    if (Profile != null) {
                        if (hasOnlineControl) {
                            defaultProfile.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                        }
                        defaultProfile.add("echo " + rollbackMax + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ);
                    }
                    shell.queueWork("echo " + freqMax + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ);
                }
            }
            if (freqMin != null && genHelper.isSafeShellValue(freqMin)) {
                String rollbackMin = shell.getInfo(FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CPU_MIN_FREQ);
                for (Integer cpu : members) {
                    boolean hasOnlineControl = genHelper.doesExist(FilePath.CPU_BASE_PATH + cpu + "/online");
                    if (hasOnlineControl) {
                        shell.queueWork("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                    }
                    shell.queueWork("chmod 0666 " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ);
                    if (Profile != null) {
                        if (hasOnlineControl) {
                            defaultProfile.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                        }
                        defaultProfile.add("echo " + rollbackMin + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ);
                    }
                    shell.queueWork("echo " + freqMin + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ);
                }
            }
            if (governor != null && genHelper.isSafeShellValue(governor)) {
                governorApplied = true;
                String rollbackGovernor = shell.getInfo(FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CURRENT_GOV_AVAILABLE);
                for (Integer cpu : members) {
                    boolean hasOnlineControl = genHelper.doesExist(FilePath.CPU_BASE_PATH + cpu + "/online");
                    shell.queueWork("chmod 0666 " + FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE);
                    if (Profile != null) {
                        if (hasOnlineControl) {
                            defaultProfile.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                        }
                        defaultProfile.add("echo " + rollbackGovernor + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE);
                    }
                    if (hasOnlineControl) {
                        shell.queueWork("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                    }
                    shell.queueWork("echo " + governor + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE);
                }
            }
        }
        if (mem_ios != null && genHelper.isSafeShellValue(mem_ios)) {
            governorSettings.add("chmod 0666 /sys/block/mmcblk0/queue/scheduler");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfoString(shell.getInfo(FilePath.GOV_IO_FILE)) + " > " + FilePath.GOV_IO_FILE);
            }
            governorSettings.add("echo " + mem_ios + " > " + FilePath.GOV_IO_FILE);
        }
        if (mem_rah != null && genHelper.isSafeShellValue(mem_rah)) {
            shell.queueWork("chmod 0666 /sys/block/mmcblk0/queue/read_ahead_kb");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.READAHEAD_PARAMETER) + " > " + FilePath.READAHEAD_PARAMETER);
            }
            shell.queueWork("echo " + mem_rah + " > " + FilePath.READAHEAD_PARAMETER);
        }
        if (governorApplied || mem_ios != null) {
            shell.setRootInfo((String[]) governorSettings.toArray(new String[0]));
        }
        if (gpu_gov != null && genHelper.isSafeShellValue(gpu_gov)) {
            String[] arr$ = FilePath.GPU_GOV_ARRAY;
            for (String s : arr$) {
                if (genHelper.doesExist(s)) {
                    this.mGPUGov = s;
                }
            }
            shell.queueWork("chmod 0666 " + this.mGPUGov + "governor");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(this.mGPUGov + "governor") + " > " + this.mGPUGov + "governor");
            }
            shell.queueWork("echo " + gpu_gov + " > " + this.mGPUGov + "governor");
        }
        if (gpu_max != null) {
            String[] arr$2 = FilePath.GPU_FILES;
            int len$ = arr$2.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                String a = arr$2[i$];
                if (!genHelper.doesExist(a)) {
                    i$++;
                } else {
                    this.gpu_file = a;
                    break;
                }
            }
            if (this.gpu_file != null && genHelper.isSafeShellValue(gpu_max)) {
                shell.queueWork("chmod 0666 " + this.gpu_file);
                if (Profile != null) {
                    defaultProfile.add("echo " + shell.getInfo(this.gpu_file) + " > " + this.gpu_file);
                }
                shell.queueWork("echo " + gpu_max + " > " + this.gpu_file);
            }
        }
        if (genHelper.doesExist(FilePath.GPU_CONTROL_ACTIVE)) {
            shell.queueWork("chmod 0666 /sys/kernel/gpu_control/gpu_control_active");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.GPU_CONTROL_ACTIVE) + " > " + FilePath.GPU_CONTROL_ACTIVE);
            }
            shell.queueWork("echo " + (gpu_enb.booleanValue() ? "1" : "0") + " > " + FilePath.GPU_CONTROL_ACTIVE);
        }
        if (genHelper.doesExist(FilePath.SWEEP2WAKE)) {
            shell.queueWork("chmod 0666 /sys/android_touch/sweep2wake");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.SWEEP2WAKE) + " > " + FilePath.SWEEP2WAKE);
            }
            shell.queueWork("echo " + (sweep.booleanValue() ? "1" : "0") + " > " + FilePath.SWEEP2WAKE);
        }
        if (genHelper.doesExist(FilePath.DOUBLETAP2WAKE)) {
            shell.queueWork("chmod 0666 /sys/android_touch/doubletap2wake");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.DOUBLETAP2WAKE) + " > " + FilePath.DOUBLETAP2WAKE);
            }
            shell.queueWork("echo " + (doubletap.booleanValue() ? "1" : "0") + " > " + FilePath.DOUBLETAP2WAKE);
        }
        if (display_color != null && genHelper.isSafeShellValue(display_color)) {
            shell.queueWork("chmod 0666 /sys/class/misc/mDisplayControl/display_brightness_value");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.DISPLAY_COLOR) + " > " + FilePath.DISPLAY_COLOR);
            }
            shell.queueWork("echo " + display_color + " > " + FilePath.DISPLAY_COLOR);
        }
        if (rgbValues != null && genHelper.isSafeShellValue(rgbValues)) {
            shell.queueWork("chmod 0666 /sys/devices/platform/kcal_ctrl.0/kcal");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.COLOR_CONTROL) + " > " + FilePath.COLOR_CONTROL);
            }
            shell.queueWork("echo " + rgbValues + " > " + FilePath.COLOR_CONTROL);
        }
        if (genHelper.doesExist(FilePath.DYANMIC_FSYNC)) {
            shell.queueWork("chmod 0666 /sys/kernel/dyn_fsync/Dyn_fsync_active");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.DYANMIC_FSYNC) + " > " + FilePath.DYANMIC_FSYNC);
            }
            shell.queueWork("echo " + (mem_dfs.booleanValue() ? "1" : "0") + " > " + FilePath.DYANMIC_FSYNC);
        }
        if (genHelper.doesExist(FilePath.WRITEBACK)) {
            shell.queueWork("chmod 0666 /sys/devices/virtual/misc/writeback/writeback_enabled");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.WRITEBACK) + " > " + FilePath.WRITEBACK);
            }
            shell.queueWork("echo " + (mem_wrb.booleanValue() ? "1" : "0") + " > " + FilePath.WRITEBACK);
        }
        if (genHelper.doesExist(FilePath.FSYNC)) {
            shell.queueWork("chmod 0666 /sys/module/sync/parameters/fsync_enabled");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.FSYNC) + " > " + FilePath.FSYNC);
            }
            shell.queueWork("echo " + (mem_fsy.booleanValue() ? "Y" : "N") + " > " + FilePath.FSYNC);
        }
        if (genHelper.doesExist(FilePath.KSM_SETTINGS)) {
            shell.queueWork("chmod 0666 /sys/kernel/mm/ksm/run");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.KSM_SETTINGS) + " > " + FilePath.KSM_SETTINGS);
            }
            shell.queueWork("echo " + (mem_ksm.booleanValue() ? "1" : "0") + " > " + FilePath.KSM_SETTINGS);
        }
        if (misc_vib != null && genHelper.isSafeShellValue(misc_vib)) {
            shell.queueWork("chmod 0666 /sys/devices/virtual/timed_output/vibrator/vtg_level");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_VIBRATOR_CONTROL_FILE) + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILE);
            }
            shell.queueWork("echo " + misc_vib + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILE);
        }
        if (misc_amp != null && genHelper.isSafeShellValue(misc_amp)) {
            shell.queueWork("chmod 0666 /sys/devices/virtual/timed_output/vibrator/amp");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_VIBRATOR_CONTROL_FILEAMP) + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILEAMP);
            }
            shell.queueWork("echo " + misc_amp + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILEAMP);
        }
        if (misc_thm != null && genHelper.isSafeShellValue(misc_thm)) {
            shell.queueWork("chmod 0666 /sys/module/msm_thermal/parameters/temp_threshold");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_THERMAL_CONTROL_FILE) + " > " + FilePath.MISC_THERMAL_CONTROL_FILE);
            }
            shell.queueWork("echo " + misc_thm + " > " + FilePath.MISC_THERMAL_CONTROL_FILE);
        }
        if (misc_tcp != null && genHelper.isSafeShellValue(misc_tcp)) {
            shell.queueWork("chmod 0666 /proc/sys/net/ipv4/tcp_congestion_control");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_TCP_CONGESTION_CURRENT) + " > " + FilePath.MISC_TCP_CONGESTION_CURRENT);
            }
            shell.queueWork("echo " + misc_tcp + " > " + FilePath.MISC_TCP_CONGESTION_CURRENT);
        }
        if (misc_vol != null && genHelper.isSafeShellValue(misc_vol)) {
            shell.queueWork("chmod 0666 /sys/class/misc/soundcontrol/volume_boost");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_HEADSET_VOLUME_BOOST_FILE) + " > " + FilePath.MISC_HEADSET_VOLUME_BOOST_FILE);
            }
            shell.queueWork("echo " + misc_vol + " > " + FilePath.MISC_HEADSET_VOLUME_BOOST_FILE);
        }
        if (this.mMiscSettings != null) {
            Map<String, ?> misc_keys = this.mMiscSettings.getAll();
            Map<String, ?> aero_keys = this.prefs.getAll();
            for (Map.Entry<String, ?> a2 : misc_keys.entrySet()) {
                for (Map.Entry<String, ?> b : aero_keys.entrySet()) {
                    if (a2.getKey().equals(b.getKey()) && b.getValue() != null && genHelper.isSafeShellValue(String.valueOf(b.getValue()))) {
                        shell.queueWork("chmod 0666 " + b.getKey());
                        if (Profile != null) {
                            defaultProfile.add("echo " + shell.getInfo(b.getKey()) + " > " + b.getKey());
                        }
                        shell.queueWork("echo " + b.getValue() + " > " + b.getKey());
                    }
                }
            }
        }
        shell.execWork();
        shell.flushWork();
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
        String[] defaultValues = (String[]) defaultProfile.toArray(new String[0]);
        shell.setRootInfo(defaultValues);
        defaultProfile.clear();
    }

    private Boolean getSaveBoolean(String s) {
        try {
            return Boolean.valueOf(this.prefs.getString(s, "0").equals("1"));
        } catch (ClassCastException e) {
            return Boolean.valueOf(this.prefs.getBoolean(s, false));
        }
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

    private void setSubParameters(String mem_ios, String Profile, String gpu_gov) throws NullPointerException {
        shellPara.queueWork("sleep 1");
        String[] completeVMSettings = shellPara.getDirInfo(FilePath.DALVIK_TWEAK, true);
        String cpu_governor = shell.getInfo(FilePath.GOV_FILE);
        String[] completeGovernorSettingList = shellPara.getDirInfo(FilePath.CPU_GOV_BASE + cpu_governor, true);
        if (mem_ios != null) {
            String[] completeIOSchedulerSettings = shellPara.getDirInfo(FilePath.GOV_IO_PARAMETER, true);
            for (String b : completeIOSchedulerSettings) {
                String ioSettings = this.prefs.getString("/sys/block/mmcblk0/queue/iosched/" + b, null);
                if (ioSettings != null && genHelper.isSafeShellValue(ioSettings)) {
                    shellPara.queueWork("chmod 0666 /sys/block/mmcblk0/queue/iosched/" + b);
                    if (Profile != null) {
                        defaultProfile.add("echo " + shellPara.getInfo("/sys/block/mmcblk0/queue/iosched/" + b) + " > " + FilePath.GOV_IO_PARAMETER + "/" + b);
                    }
                    shellPara.queueWork("echo " + ioSettings + " > " + FilePath.GOV_IO_PARAMETER + "/" + b);
                }
            }
        }
        for (String c : completeVMSettings) {
            String vmSettings = this.prefs.getString("/proc/sys/vm/" + c, null);
            if (vmSettings != null && genHelper.isSafeShellValue(vmSettings)) {
                shellPara.queueWork("chmod 0666 /proc/sys/vm/" + c);
                if (Profile != null) {
                    defaultProfile.add("echo " + shellPara.getInfo("/proc/sys/vm/" + c) + " > " + FilePath.DALVIK_TWEAK + "/" + c);
                }
                shellPara.queueWork("echo " + vmSettings + " > " + FilePath.DALVIK_TWEAK + "/" + c);
            }
        }
        String[] arr$ = FilePath.HOTPLUG_PATH;
        for (String s : arr$) {
            if (genHelper.doesExist(s)) {
                this.mHotplugPath = s;
            }
        }
        if (genHelper.doesExist(this.mHotplugPath)) {
            String[] completeHotplugSettings = shellPara.getDirInfo(this.mHotplugPath, true);
            for (String d : completeHotplugSettings) {
                String hotplugSettings = this.prefs.getString(this.mHotplugPath + "/" + d, null);
                if (hotplugSettings != null && genHelper.isSafeShellValue(hotplugSettings)) {
                    shellPara.queueWork("chmod 0666 " + this.mHotplugPath + "/" + d);
                    if (Profile != null) {
                        defaultProfile.add("echo " + shellPara.getInfo(this.mHotplugPath + "/" + d) + " > " + this.mHotplugPath + "/" + d);
                    }
                    shellPara.queueWork("echo " + hotplugSettings + " > " + this.mHotplugPath + "/" + d);
                }
            }
        }
        if (genHelper.doesExist(FilePath.CPU_BOOST)) {
            String[] completeCPUBOOSTSettings = shellPara.getDirInfo(FilePath.CPU_BOOST, true);
            for (String d2 : completeCPUBOOSTSettings) {
                String cpuBoostSettings = this.prefs.getString("/sys/module/cpu_boost/parameters/" + d2, null);
                if (cpuBoostSettings != null && genHelper.isSafeShellValue(cpuBoostSettings)) {
                    shellPara.queueWork("chmod 0666 /sys/module/cpu_boost/parameters/" + d2);
                    if (Profile != null) {
                        defaultProfile.add("echo " + shellPara.getInfo("/sys/module/cpu_boost/parameters/" + d2) + " > " + FilePath.CPU_BOOST + "/" + d2);
                    }
                    shellPara.queueWork("echo " + cpuBoostSettings + " > " + FilePath.CPU_BOOST + "/" + d2);
                }
            }
        }
        if (genHelper.doesExist(FilePath.GPU_GOV_PATH)) {
            String[] completeGPUGovSettings = shellPara.getDirInfo(FilePath.GPU_GOV_PATH, true);
            for (String e : completeGPUGovSettings) {
                String gpugovSettings = this.prefs.getString("/sys/module/msm_kgsl_core/parameters/" + e, null);
                if (gpugovSettings != null && genHelper.isSafeShellValue(gpugovSettings)) {
                    shellPara.queueWork("chmod 0666 /sys/module/msm_kgsl_core/parameters/" + e);
                    if (Profile != null) {
                        defaultProfile.add("echo " + shellPara.getInfo("/sys/module/msm_kgsl_core/parameters/" + e) + " > " + FilePath.GPU_GOV_PATH + "/" + e);
                    }
                    shellPara.queueWork("echo " + gpugovSettings + " > " + FilePath.GPU_GOV_PATH + "/" + e);
                }
            }
        }
        if (completeGovernorSettingList != null) {
            for (String b2 : completeGovernorSettingList) {
                String governorSetting = this.prefs.getString(FilePath.CPU_GOV_BASE + cpu_governor + "/" + b2, null);
                if (governorSetting != null && genHelper.isSafeShellValue(governorSetting)) {
                    shellPara.queueWork("sleep 1");
                    shellPara.queueWork("chmod 0666 /sys/devices/system/cpu/cpufreq/" + cpu_governor + "/" + b2);
                    if (Profile != null) {
                        defaultProfile.add("sleep 1");
                        defaultProfile.add("echo " + shell.getInfo(FilePath.CPU_GOV_BASE + cpu_governor + "/" + b2) + " > " + FilePath.CPU_GOV_BASE + cpu_governor + "/" + b2);
                    }
                    shellPara.queueWork("echo " + governorSetting + " > " + FilePath.CPU_GOV_BASE + cpu_governor + "/" + b2);
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
            String[] completeGPUGovernorSetting = shell.getDirInfo(this.mGPUGov + gpu_gov, true);
            for (String b3 : completeGPUGovernorSetting) {
                String governorSetting2 = this.prefs.getString(this.mGPUGov + gpu_gov + "/" + b3, null);
                if (governorSetting2 != null && genHelper.isSafeShellValue(governorSetting2)) {
                    shellPara.queueWork("chmod 0666 " + this.mGPUGov + gpu_gov + "/" + b3);
                    if (Profile != null) {
                        defaultProfile.add("echo " + shellPara.getInfo(this.mGPUGov + gpu_gov + "/" + b3) + " > " + this.mGPUGov + gpu_gov + "/" + b3);
                    }
                    shellPara.queueWork("echo " + governorSetting2 + " > " + this.mGPUGov + gpu_gov + "/" + b3);
                }
            }
        }
        shellPara.execWork();
        shellPara.flushWork();
    }
}
