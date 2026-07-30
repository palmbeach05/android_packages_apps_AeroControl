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
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class settingsHelper {
    private static final String MISC_SETTINGS_STORAGE = "miscSettingsStorage";
    private static final String PREF_CPU_BIG_MAX_FREQ = "big_max_frequency";
    private static final String PREF_CPU_BIG_MIN_FREQ = "big_min_frequency";
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
    private static final int mNumCpus = Runtime.getRuntime().availableProcessors();
    private static final shellHelper shell = shellHelper.instance();
    private static final shellHelper shellPara = shellHelper.forceInstance();
    private static final ArrayList<String> defaultProfile = new ArrayList<>();
    private static final GenericHelper genHelper = new GenericHelper();

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
                            Toast.makeText(context, ((Object) context.getText(R.string.app_name)) + ": " + ((Object) context.getText(R.string.finishing_settings)), 1).show();
                        }
                    });
                }
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
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
                    Toast.makeText(context, ((Object) context.getText(R.string.app_name)) + ": " + ((Object) context.getText(R.string.setonboot_settings)), 1).show();
                }
            });
        }
        String cpu_max = this.prefs.getString(PREF_CPU_MAX_FREQ, null);
        String cpu_min = this.prefs.getString(PREF_CPU_MIN_FREQ, null);
        String cpu_big_max = this.prefs.getString(PREF_CPU_BIG_MAX_FREQ, null);
        String cpu_big_min = this.prefs.getString(PREF_CPU_BIG_MIN_FREQ, null);
        String cpu_gov = this.prefs.getString(PREF_CURRENT_GOV_AVAILABLE, null);
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
        if (voltage != null) {
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
        String max_freq = shell.getInfo("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
        String min_freq = shell.getInfo("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
        String max_big_freq = shell.getInfo("/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq");
        String min_big_freq = shell.getInfo("/sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq");
        int cores = mNumCpus;
        if (mNumCpus > 4) {
            cores = mNumCpus / 2;
            for (int k = 4; k < mNumCpus; k++) {
                if (cpu_big_max != null) {
                    shell.queueWork("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                    shell.queueWork("chmod 0666 /sys/devices/system/cpu/cpu" + k + FilePath.CPU_MAX_FREQ);
                    if (Profile != null) {
                        defaultProfile.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                        defaultProfile.add("echo " + max_big_freq + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MAX_FREQ);
                    }
                    shell.queueWork("echo " + cpu_big_max + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MAX_FREQ);
                }
                if (cpu_big_min != null) {
                    shell.queueWork("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                    shell.queueWork("chmod 0666 /sys/devices/system/cpu/cpu" + k + FilePath.CPU_MIN_FREQ);
                    if (Profile != null) {
                        defaultProfile.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                        defaultProfile.add("echo " + min_big_freq + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MIN_FREQ);
                    }
                    shell.queueWork("echo " + cpu_big_min + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MIN_FREQ);
                }
                if (cpu_gov != null) {
                    shell.queueWork("chmod 0666 /sys/devices/system/cpu/cpu" + k + FilePath.CURRENT_GOV_AVAILABLE);
                    if (Profile != null) {
                        defaultProfile.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                        defaultProfile.add("echo " + shell.getInfo(FilePath.CPU_BASE_PATH + k + FilePath.CURRENT_GOV_AVAILABLE) + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CURRENT_GOV_AVAILABLE);
                    }
                    shell.queueWork("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                    shell.queueWork("echo " + cpu_gov + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CURRENT_GOV_AVAILABLE);
                }
            }
        }
        for (int k2 = 0; k2 < cores; k2++) {
            if (cpu_max != null) {
                shell.queueWork("echo 1 > /sys/devices/system/cpu/cpu" + k2 + "/online");
                shell.queueWork("chmod 0666 /sys/devices/system/cpu/cpu" + k2 + FilePath.CPU_MAX_FREQ);
                if (Profile != null) {
                    defaultProfile.add("echo 1 > /sys/devices/system/cpu/cpu" + k2 + "/online");
                    defaultProfile.add("echo " + max_freq + " > " + FilePath.CPU_BASE_PATH + k2 + FilePath.CPU_MAX_FREQ);
                }
                shell.queueWork("echo " + cpu_max + " > " + FilePath.CPU_BASE_PATH + k2 + FilePath.CPU_MAX_FREQ);
            }
            if (cpu_min != null) {
                shell.queueWork("echo 1 > /sys/devices/system/cpu/cpu" + k2 + "/online");
                shell.queueWork("chmod 0666 /sys/devices/system/cpu/cpu" + k2 + FilePath.CPU_MIN_FREQ);
                if (Profile != null) {
                    defaultProfile.add("echo 1 > /sys/devices/system/cpu/cpu" + k2 + "/online");
                    defaultProfile.add("echo " + min_freq + " > " + FilePath.CPU_BASE_PATH + k2 + FilePath.CPU_MIN_FREQ);
                }
                shell.queueWork("echo " + cpu_min + " > " + FilePath.CPU_BASE_PATH + k2 + FilePath.CPU_MIN_FREQ);
            }
            if (cpu_gov != null) {
                shell.queueWork("chmod 0666 /sys/devices/system/cpu/cpu" + k2 + FilePath.CURRENT_GOV_AVAILABLE);
                if (Profile != null) {
                    defaultProfile.add("echo 1 > /sys/devices/system/cpu/cpu" + k2 + "/online");
                    defaultProfile.add("echo " + shell.getInfo(FilePath.CPU_BASE_PATH + k2 + FilePath.CURRENT_GOV_AVAILABLE) + " > " + FilePath.CPU_BASE_PATH + k2 + FilePath.CURRENT_GOV_AVAILABLE);
                }
                shell.queueWork("echo 1 > /sys/devices/system/cpu/cpu" + k2 + "/online");
                shell.queueWork("echo " + cpu_gov + " > " + FilePath.CPU_BASE_PATH + k2 + FilePath.CURRENT_GOV_AVAILABLE);
            }
        }
        if (mem_ios != null) {
            governorSettings.add("chmod 0666 /sys/block/mmcblk0/queue/scheduler");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfoString(shell.getInfo(FilePath.GOV_IO_FILE)) + " > " + FilePath.GOV_IO_FILE);
            }
            governorSettings.add("echo " + mem_ios + " > " + FilePath.GOV_IO_FILE);
        }
        if (mem_rah != null) {
            shell.queueWork("chmod 0666 /sys/block/mmcblk0/queue/read_ahead_kb");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.READAHEAD_PARAMETER) + " > " + FilePath.READAHEAD_PARAMETER);
            }
            shell.queueWork("echo " + mem_rah + " > " + FilePath.READAHEAD_PARAMETER);
        }
        if (cpu_gov != null || mem_ios != null) {
            shell.setRootInfo((String[]) governorSettings.toArray(new String[0]));
        }
        if (gpu_gov != null) {
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
            if (this.gpu_file != null) {
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
        if (display_color != null) {
            shell.queueWork("chmod 0666 /sys/class/misc/mDisplayControl/display_brightness_value");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.DISPLAY_COLOR) + " > " + FilePath.DISPLAY_COLOR);
            }
            shell.queueWork("echo " + display_color + " > " + FilePath.DISPLAY_COLOR);
        }
        if (rgbValues != null) {
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
        if (misc_vib != null) {
            shell.queueWork("chmod 0666 /sys/devices/virtual/timed_output/vibrator/vtg_level");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_VIBRATOR_CONTROL_FILE) + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILE);
            }
            shell.queueWork("echo " + misc_vib + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILE);
        }
        if (misc_amp != null) {
            shell.queueWork("chmod 0666 /sys/devices/virtual/timed_output/vibrator/amp");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_VIBRATOR_CONTROL_FILEAMP) + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILEAMP);
            }
            shell.queueWork("echo " + misc_amp + " > " + FilePath.MISC_VIBRATOR_CONTROL_FILEAMP);
        }
        if (misc_thm != null) {
            shell.queueWork("chmod 0666 /sys/module/msm_thermal/parameters/temp_threshold");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_THERMAL_CONTROL_FILE) + " > " + FilePath.MISC_THERMAL_CONTROL_FILE);
            }
            shell.queueWork("echo " + misc_thm + " > " + FilePath.MISC_THERMAL_CONTROL_FILE);
        }
        if (misc_tcp != null) {
            shell.queueWork("chmod 0666 /proc/sys/net/ipv4/tcp_congestion_control");
            if (Profile != null) {
                defaultProfile.add("echo " + shell.getInfo(FilePath.MISC_TCP_CONGESTION_CURRENT) + " > " + FilePath.MISC_TCP_CONGESTION_CURRENT);
            }
            shell.queueWork("echo " + misc_tcp + " > " + FilePath.MISC_TCP_CONGESTION_CURRENT);
        }
        if (misc_vol != null) {
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
                    if (a2.getKey().equals(b.getKey())) {
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

    private void setSubParameters(String mem_ios, String Profile, String gpu_gov) throws NullPointerException {
        shellPara.queueWork("sleep 1");
        String[] completeVMSettings = shellPara.getDirInfo(FilePath.DALVIK_TWEAK, true);
        String cpu_governor = shell.getInfo(FilePath.GOV_FILE);
        String[] completeGovernorSettingList = shellPara.getDirInfo(FilePath.CPU_GOV_BASE + cpu_governor, true);
        if (mem_ios != null) {
            String[] completeIOSchedulerSettings = shellPara.getDirInfo(FilePath.GOV_IO_PARAMETER, true);
            for (String b : completeIOSchedulerSettings) {
                String ioSettings = this.prefs.getString("/sys/block/mmcblk0/queue/iosched/" + b, null);
                if (ioSettings != null) {
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
            if (vmSettings != null) {
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
                if (hotplugSettings != null) {
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
                if (cpuBoostSettings != null) {
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
                if (gpugovSettings != null) {
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
                if (governorSetting != null) {
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
        if (gpu_gov != null) {
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
                if (governorSetting2 != null) {
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
