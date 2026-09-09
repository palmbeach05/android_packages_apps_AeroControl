package com.aero.control.fragments;

import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.AeroAdapter;
import com.aero.control.adapter.AeroData;
import com.aero.control.helpers.CpuClusterHelper;
import com.aero.control.helpers.FilePath;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Overview fragment displaying real-time system information including CPU frequencies
 * (per-core grid or list), kernel version, CPU governor, memory usage, and other
 * system metrics. Refreshes every 3 seconds and provides a first-run tutorial.
 */
public class AeroFragment extends Fragment {
    private static final String FILENAME = "firstrun";
    private static final int MAX_GRID_CORES = 8;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String SCALE_CPU_UTIL = "/cpufreq/cpu_utilization";
    private static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu";
    private static final String SCALE_PATH_NAME = "/cpufreq/scaling_cur_freq";
    private static final String THERMAL_ZONE_DIRECTORY = "/sys/devices/virtual/thermal";
    private static final Pattern THERMAL_ZONE_NAME_PATTERN = Pattern.compile("thermal_zone\\d+");
    private static final String THERMAL_ZONE_TYPE_FILE = "type";
    private static final String THERMAL_ZONE_TEMP_FILE = "temp";
    private static final String POWER_SUPPLY_DIRECTORY = "/sys/class/power_supply";
    private static final String POWER_SUPPLY_TYPE_FILE = "type";
    private static final String POWER_SUPPLY_TEMP_FILE = "temp";
    private static final String TEGRA_I2C_PLATFORM_DIRECTORY = "/sys/devices/platform";
    private static final Pattern TEGRA_I2C_CONTROLLER_PATTERN =
            Pattern.compile("tegra-i2c\\.(\\d+)");
    private static final Pattern TEGRA_I2C_BUS_PATTERN = Pattern.compile("i2c-(\\d+)");
    private static final Pattern TEGRA_I2C_DEVICE_PATTERN =
            Pattern.compile("(\\d+)-[0-9a-fA-F]{4}");
    private static final String HWMON_DIRECTORY = "/sys/class/hwmon";
    private static final String HWMON_NAME_FILE = "name";
    private static final Pattern HWMON_TEMP_INPUT_PATTERN = Pattern.compile("(temp\\d+)_input");
    private static final String HWMON_TEMP_LABEL_SUFFIX = "_label";
    private static final double MIN_CPU_TEMPERATURE_CELSIUS = -100.0d;
    private static final double MAX_CPU_TEMPERATURE_CELSIUS = 250.0d;
    private String gpu_file;
    private AeroAdapter mAdapter;
    private AeroData mPerformanceData;
    private AeroData mTemperatureData;
    private AeroData mSystemSection;
    private AeroData mPerformanceSection;
    private AeroData mTemperaturesSection;
    private AeroData mMemorySection;
    private AeroData mConfigurationSection;
    private AeroData mConfigurationData;
    private final CpuClusterHelper mCpuClusterHelper = new CpuClusterHelper();
    private AeroData mKernelData;
    private ListView mOverView;
    private AeroData mRAMData;
    private ShowcaseView mShowCase;
    private ViewGroup root;
    private List<AeroData> mOverviewData = new ArrayList<AeroData>();
    private int mActionBarHeight = 0;
    private boolean mVisible = true;
    private boolean mExecuted = false;
    private RefreshThread mRefreshThread = new RefreshThread();
    private Handler mRefreshHandler = new Handler() {
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1
                    && AeroFragment.this.isVisible()
                    && msg.obj instanceof OverviewSnapshot) {
                AeroFragment.this.applySnapshot((OverviewSnapshot) msg.obj);
            }
        }
    };

    private class RefreshThread extends Thread {
        private volatile boolean mInterrupt;

        private RefreshThread() {
            this.mInterrupt = false;
        }

        /**
         * Cancels the refresh thread by setting the interrupt flag and interrupting the thread.
         */
        public void cancel() {
            this.mInterrupt = true;
            interrupt();
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (!this.mInterrupt) {
                try {
                    OverviewSnapshot snapshot;
                    try {
                        snapshot = AeroFragment.this.collectOverviewData();
                    } catch (RuntimeException e) {
                        Log.e(AeroFragment.class.getName(),
                                "Failed to collect overview data", e);
                        snapshot = AeroFragment.this.createFallbackOverviewSnapshot();
                    }
                    Message message = AeroFragment.this.mRefreshHandler.obtainMessage(1, snapshot);
                    message.sendToTarget();
                    sleep(3000L);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mVisible = false;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mVisible = true;
    }

    @Override // android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mRefreshThread.cancel();
        this.mRefreshHandler.removeMessages(1);
        this.mAdapter = null;
        this.mOverView = null;
        this.root = null;
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.root = (ViewGroup) inflater.inflate(R.layout.overviewlist_item, (ViewGroup) null);
        this.mOverView = (ListView) this.root.findViewById(R.id.listView1);
        String[] arr$ = FilePath.GPU_FILES_RATE;
        int len$ = arr$.length;
        int i$ = 0;
        while (true) {
            if (i$ >= len$) {
                break;
            }
            String a = arr$[i$];
            if (!AeroActivity.genHelper.doesExist(a)) {
                i$++;
            } else {
                this.gpu_file = a;
                break;
            }
        }
        // Always create a fresh thread for each view instance
        this.mRefreshThread = new RefreshThread();
        this.mRefreshThread.start();
        this.mRefreshThread.setPriority(1);
        return this.root;
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_aero_fragment, R.string.showcase_aero_fragment_summary);
        }
    }

    /**
     * Builds the legacy joined single-line frequency string. Used as the
     * fallback format when a device reports more than {@link #MAX_GRID_CORES}
     * cores, since the fixed grid layout only supports up to 8 cells.
     */
    public final String getFreqPerCore() {
        String freq_string = "";
        int i = Runtime.getRuntime().availableProcessors();
        for (int k = 0; k < i; k++) {
            String complete_path = "/sys/devices/system/cpu/cpu" + k + SCALE_PATH_NAME;
            freq_string = freq_string + " " + AeroActivity.shell.toMHz(AeroActivity.shell.getInfo(complete_path));
        }
        String freq_string2 = freq_string.replace(NO_DATA_FOUND, " Offline ");
        String cpu_util_line = getCpuUtilizationLine();
        if (cpu_util_line == null) {
            return freq_string2;
        }
        return freq_string2 + "\n" + cpu_util_line;
    }

    /**
     * Builds one formatted string per core, for example "CPU0\n1200 MHz", for
     * rendering in the Overview frequency grid.
     */
    private List<String> getCoreFrequencyList() {
        int i = Runtime.getRuntime().availableProcessors();
        List<String> perCore = new ArrayList<>(i);
        for (int k = 0; k < i; k++) {
            String complete_path = "/sys/devices/system/cpu/cpu" + k + SCALE_PATH_NAME;
            String value = AeroActivity.shell.toMHz(AeroActivity.shell.getInfo(complete_path)).replace(NO_DATA_FOUND, "Offline");
            perCore.add("CPU" + k + "\n" + value);
        }
        return perCore;
    }

    /**
     * Builds the tab-padded, percentage-suffixed CPU-utilization line,
     * decoupled from the per-core frequency string. Returns null when the
     * utilization file does not exist.
     */
    private String getCpuUtilizationLine() {
        int i = Runtime.getRuntime().availableProcessors();
        if (!AeroActivity.genHelper.doesExist("/sys/devices/system/cpu/cpu0/cpufreq/cpu_utilization")) {
            return null;
        }
        String cpu_util = "";
        for (int j = 0; j < i; j++) {
            String complete_path2 = "/sys/devices/system/cpu/cpu" + j + SCALE_CPU_UTIL;
            String tmp = AeroActivity.shell.getInfo(complete_path2);
            if (!tmp.equals(NO_DATA_FOUND)) {
                try {
                    if (Integer.parseInt(tmp.trim()) < 10) {
                        tmp = " " + tmp;
                    }
                } catch (NumberFormatException e) {
                    tmp = "0";
                }
            }
            cpu_util = cpu_util + "\t\t\t" + tmp + "%";
        }
        return cpu_util.replace("Unavailable%", "--");
    }

    private List<RawTemperature> getTemperatures() {
        List<RawTemperature> readings = new ArrayList<>();
        boolean hasPowerSupplyBatteryTemperature = false;
        String[] powerSupplies = AeroActivity.shell.getDirInfo(POWER_SUPPLY_DIRECTORY, false);
        if (powerSupplies != null) {
            for (String powerSupply : powerSupplies) {
                String supplyPath = POWER_SUPPLY_DIRECTORY + "/" + powerSupply + "/";
                String type = AeroActivity.shell.getInfo(supplyPath + POWER_SUPPLY_TYPE_FILE);
                if (type == null || !type.trim().equalsIgnoreCase("Battery")) {
                    continue;
                }
                String temperature = formatTemperature(
                        AeroActivity.shell.getInfo(supplyPath + POWER_SUPPLY_TEMP_FILE), true);
                if (temperature != null) {
                    readings.add(new RawTemperature(
                            R.string.temperature_source_battery, "Battery", temperature));
                    hasPowerSupplyBatteryTemperature = true;
                    break;
                }
            }
        }
        readings.addAll(getTegraI2cTemperatures());
        readings.addAll(getHwmonTemperatures());
        String[] thermalZones = AeroActivity.shell.getDirInfo(THERMAL_ZONE_DIRECTORY, false);
        if (thermalZones != null) {
            for (String thermalZone : thermalZones) {
                if (!THERMAL_ZONE_NAME_PATTERN.matcher(thermalZone).matches()) {
                    continue;
                }
                String zonePath = THERMAL_ZONE_DIRECTORY + "/" + thermalZone + "/";
                String type = AeroActivity.shell.getInfo(zonePath + THERMAL_ZONE_TYPE_FILE);
                int labelResource = getTemperatureLabel(type);
                if (hasPowerSupplyBatteryTemperature
                        && labelResource == R.string.temperature_source_battery) {
                    continue;
                }
                String temperature = formatTemperature(
                        AeroActivity.shell.getInfo(zonePath + THERMAL_ZONE_TEMP_FILE), false);
                if (temperature != null) {
                    readings.add(new RawTemperature(
                            labelResource, safeSensorName(type, thermalZone), temperature));
                }
            }
        }
        return readings;
    }

    private List<RawTemperature> getTegraI2cTemperatures() {
        List<RawTemperature> readings = new ArrayList<>();
        String[] controllers = AeroActivity.shell.getDirInfo(
                TEGRA_I2C_PLATFORM_DIRECTORY, false);
        if (controllers == null) {
            return readings;
        }
        for (String controller : controllers) {
            Matcher controllerMatcher = TEGRA_I2C_CONTROLLER_PATTERN.matcher(controller);
            String controllerPath = TEGRA_I2C_PLATFORM_DIRECTORY + "/" + controller;
            if (!controllerMatcher.matches()) {
                Log.d(AeroFragment.class.getName(),
                        "Rejected Tegra I2C node path: " + controllerPath);
                continue;
            }
            String busNumber = controllerMatcher.group(1);
            String[] buses = AeroActivity.shell.getRootAwareTegraI2cDirInfo(
                    controllerPath, false);
            for (String bus : buses) {
                Matcher busMatcher = TEGRA_I2C_BUS_PATTERN.matcher(bus);
                String busPath = controllerPath + "/" + bus;
                if (!busMatcher.matches() || !busNumber.equals(busMatcher.group(1))) {
                    Log.d(AeroFragment.class.getName(),
                            "Rejected Tegra I2C node path: " + busPath);
                    continue;
                }
                String[] devices = AeroActivity.shell.getRootAwareTegraI2cDirInfo(
                        busPath, false);
                for (String device : devices) {
                    Matcher deviceMatcher = TEGRA_I2C_DEVICE_PATTERN.matcher(device);
                    String devicePath = busPath + "/" + device;
                    if (!deviceMatcher.matches()
                            || !busNumber.equals(deviceMatcher.group(1))) {
                        Log.d(AeroFragment.class.getName(),
                                "Rejected Tegra I2C node path: " + devicePath);
                        continue;
                    }
                    Log.d(AeroFragment.class.getName(),
                            "Discovered Tegra I2C temperature device: " + devicePath);
                    addTegraI2cDeviceTemperatures(
                            readings, busNumber, device, devicePath);
                }
            }
        }
        return readings;
    }

    private void addTegraI2cDeviceTemperatures(List<RawTemperature> readings,
            String busNumber, String device, String devicePath) {
        String[] files = AeroActivity.shell.getRootAwareTegraI2cDirInfo(devicePath, true);
        String deviceName = AeroActivity.shell.getDirectTegraI2cInfo(devicePath + "/name");
        boolean hasDeviceName = !deviceName.equalsIgnoreCase(NO_DATA_FOUND);
        for (String file : files) {
            String filePath = devicePath + "/" + file;
            Matcher inputMatcher = HWMON_TEMP_INPUT_PATTERN.matcher(file);
            if (!inputMatcher.matches()) {
                Log.d(AeroFragment.class.getName(),
                        "Rejected Tegra I2C node path: " + filePath);
                continue;
            }
            String temperature = formatTemperature(
                    AeroActivity.shell.getRootAwareTegraI2cInfo(filePath), false);
            if (temperature == null) {
                Log.d(AeroFragment.class.getName(),
                        "Rejected Tegra I2C node path: " + filePath);
                continue;
            }
            Log.d(AeroFragment.class.getName(),
                    "Accepted Tegra I2C node path: " + filePath);
            String inputName = inputMatcher.group(1);
            String sourceName = "i2c-" + busNumber + " " + device + " " + inputName;
            if (hasDeviceName) {
                String channelLabel = AeroActivity.shell.getDirectTegraI2cInfo(
                        devicePath + "/" + inputName + HWMON_TEMP_LABEL_SUFFIX);
                sourceName = channelLabel.equalsIgnoreCase(NO_DATA_FOUND)
                        ? deviceName + " " + inputName
                        : deviceName + ": " + channelLabel;
            }
            readings.add(new RawTemperature(R.string.temperature_source_other,
                    sourceName, temperature));
        }
    }

    private List<RawTemperature> getHwmonTemperatures() {
        List<RawTemperature> readings = new ArrayList<>();
        if (!new File(HWMON_DIRECTORY).exists()) {
            return readings;
        }
        String[] hwmonDevices = AeroActivity.shell.getRootAwareHwmonDirInfo(
                HWMON_DIRECTORY, false);
        if (hwmonDevices == null) {
            return readings;
        }
        for (String hwmonDevice : hwmonDevices) {
            String devicePath = HWMON_DIRECTORY + "/" + hwmonDevice + "/";
            String deviceName = safeSensorName(
                    AeroActivity.shell.getInfo(devicePath + HWMON_NAME_FILE), hwmonDevice);
            String[] deviceFiles = AeroActivity.shell.getRootAwareHwmonDirInfo(
                    devicePath, true);
            if (deviceFiles == null) {
                continue;
            }
            for (String deviceFile : deviceFiles) {
                Matcher inputMatcher = HWMON_TEMP_INPUT_PATTERN.matcher(deviceFile);
                if (!inputMatcher.matches()) {
                    continue;
                }
                String temperature = formatTemperature(
                        AeroActivity.shell.getInfo(devicePath + deviceFile), false);
                if (temperature == null) {
                    continue;
                }
                String inputName = inputMatcher.group(1);
                String label = AeroActivity.shell.getInfo(
                        devicePath + inputName + HWMON_TEMP_LABEL_SUFFIX);
                String sourceName = deviceName + " " + inputName;
                if (label != null
                        && label.trim().length() > 0
                        && !label.trim().equalsIgnoreCase(NO_DATA_FOUND)) {
                    sourceName = deviceName + ": " + label.trim();
                }
                readings.add(new RawTemperature(
                        R.string.temperature_source_other, sourceName, temperature));
            }
        }
        return readings;
    }

    private String safeSensorName(String type, String fallback) {
        if (type == null || type.trim().length() == 0 || type.equalsIgnoreCase(NO_DATA_FOUND)) {
            return fallback;
        }
        return type.trim();
    }

    private int getTemperatureLabel(String type) {
        if (type == null) return R.string.temperature_source_other;
        String normalizedType = type.trim().toLowerCase(Locale.US).replace('_', '-');
        if (normalizedType.contains("battery") || normalizedType.contains("batt")) return R.string.temperature_source_battery;
        if (normalizedType.contains("gpu")) return R.string.temperature_source_gpu;
        if (normalizedType.equals("soc") || normalizedType.contains("soc-therm")) return R.string.temperature_source_soc;
        if (normalizedType.contains("cpu") || normalizedType.contains("x86-pkg")) return R.string.temperature_source_cpu;
        return R.string.temperature_source_other;
    }

    private static final class RawTemperature {
        private final int labelResource;
        private final String source;
        private final String value;
        private RawTemperature(int labelResource, String source, String value) {
            this.labelResource = labelResource;
            this.source = source;
            this.value = value;
        }
    }

    private String formatTemperature(String rawTemperature, boolean batteryTenths) {
        if (rawTemperature == null) {
            return null;
        }
        String value = rawTemperature.trim();
        if (value.length() == 0 || value.equalsIgnoreCase(NO_DATA_FOUND)) {
            return null;
        }
        try {
            double celsius = Double.parseDouble(value);
            if (Double.isNaN(celsius) || Double.isInfinite(celsius)) {
                return null;
            }
            if (batteryTenths && Math.abs(celsius) < 1000.0d) {
                celsius /= 10.0d;
            } else if (Math.abs(celsius) >= 1000.0d) {
                celsius /= 1000.0d;
            }
            if (celsius < MIN_CPU_TEMPERATURE_CELSIUS || celsius > MAX_CPU_TEMPERATURE_CELSIUS) {
                return null;
            }
            return BigDecimal.valueOf(celsius).stripTrailingZeros().toPlainString() + " °C";
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final class OverviewSnapshot {
        private String kernel;
        private List<String> governors;
        private List<String> governorLabels;
        private String ioScheduler;
        private String frequencyContent;
        private List<String> coreFrequencies;
        private String gpuFrequency;
        private String memory;
        private List<RawTemperature> temperatures;
    }

    private OverviewSnapshot collectOverviewData() {
        OverviewSnapshot snapshot = new OverviewSnapshot();
        snapshot.kernel = AeroActivity.shell.getKernel();
        snapshot.governors = new ArrayList<>();
        snapshot.governorLabels = new ArrayList<>();
        List<CpuClusterHelper.Cluster> clusters = this.mCpuClusterHelper.getClusters();
        for (CpuClusterHelper.Cluster cluster : clusters) {
            snapshot.governors.add(AeroActivity.shell.getInfo(FilePath.CPU_BASE_PATH
                    + cluster.getRepresentativeCpu() + FilePath.CURRENT_GOV_AVAILABLE));
            snapshot.governorLabels.add(cluster.getMemberRangeLabel());
        }
        snapshot.ioScheduler = AeroActivity.shell.getInfoString(
                AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE));
        int coreCount = Runtime.getRuntime().availableProcessors();
        snapshot.coreFrequencies = coreCount <= MAX_GRID_CORES ? getCoreFrequencyList() : null;
        snapshot.frequencyContent = coreCount <= MAX_GRID_CORES ? getCpuUtilizationLine() : getFreqPerCore();
        String gpuFrequency = AeroActivity.shell.getInfo(this.gpu_file);
        if (gpuFrequency == null || gpuFrequency.length() <= 3 || gpuFrequency.equals(NO_DATA_FOUND)) {
            snapshot.gpuFrequency = NO_DATA_FOUND;
        } else {
            snapshot.gpuFrequency = AeroActivity.shell.toMHz(gpuFrequency.substring(0, gpuFrequency.length() - 3));
        }
        snapshot.memory = AeroActivity.shell.getMemory(FilePath.FILENAME_PROC_MEMINFO);
        snapshot.temperatures = getTemperatures();
        return snapshot;
    }

    private OverviewSnapshot createFallbackOverviewSnapshot() {
        OverviewSnapshot snapshot = new OverviewSnapshot();
        snapshot.kernel = NO_DATA_FOUND;
        snapshot.governors = new ArrayList<>();
        snapshot.governorLabels = new ArrayList<>();
        snapshot.ioScheduler = NO_DATA_FOUND;
        snapshot.frequencyContent = NO_DATA_FOUND;
        snapshot.coreFrequencies = null;
        snapshot.gpuFrequency = NO_DATA_FOUND;
        snapshot.memory = NO_DATA_FOUND;
        snapshot.temperatures = new ArrayList<>();
        return snapshot;
    }

    private void applySnapshot(OverviewSnapshot snapshot) {
        if (this.mKernelData == null) {
            this.mKernelData = AeroData.standardCard(getString(R.string.kernel_version), snapshot.kernel);
        } else {
            this.mKernelData.content = snapshot.kernel;
        }
        List<AeroData.ConfigurationReading> configurations =
                buildConfigurationReadings(snapshot);
        if (this.mConfigurationData == null) {
            this.mConfigurationData = AeroData.configurationCard(configurations);
        } else {
            this.mConfigurationData.configurations = configurations;
        }
        if (this.mPerformanceData == null) {
            this.mPerformanceData = AeroData.performanceCard(
                    getString(R.string.current_cpu_speed), snapshot.frequencyContent,
                    snapshot.coreFrequencies, getString(R.string.current_gpu_speed),
                    snapshot.gpuFrequency);
        } else {
            this.mPerformanceData.cpuFrequencyContent = snapshot.frequencyContent;
            this.mPerformanceData.coreFrequencies = snapshot.coreFrequencies;
            this.mPerformanceData.gpuFrequencyValue = snapshot.gpuFrequency;
        }
        if (this.mRAMData == null) {
            this.mRAMData = AeroData.standardCard(getString(R.string.available_memory), snapshot.memory);
        } else {
            this.mRAMData.content = snapshot.memory;
        }
        List<AeroData.TemperatureReading> temperatures = new ArrayList<>();
        for (RawTemperature reading : snapshot.temperatures) {
            String label = reading.labelResource == R.string.temperature_source_other
                    ? getString(reading.labelResource, reading.source) : getString(reading.labelResource);
            temperatures.add(new AeroData.TemperatureReading(label, reading.value));
        }
        if (temperatures.isEmpty()) {
            temperatures.add(new AeroData.TemperatureReading(
                    getString(R.string.temperature_status), getString(R.string.temperature_unavailable)));
        }
        if (this.mTemperatureData == null) {
            this.mTemperatureData = AeroData.temperatureCard(
                    getString(R.string.overview_section_temperatures), temperatures);
        } else {
            this.mTemperatureData.temperatures = temperatures;
        }

        rebuildOrderedOverview();
        if (this.mAdapter == null) {
            this.mAdapter = new AeroAdapter(getActivity(), R.layout.overviewlist_item, this.mOverviewData);
            this.mOverView.setAdapter((ListAdapter) this.mAdapter);
        } else {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void rebuildOrderedOverview() {
        if (this.mSystemSection == null) {
            this.mSystemSection = AeroData.section(getString(R.string.overview_section_system));
            this.mPerformanceSection = AeroData.section(getString(R.string.overview_section_performance));
            this.mTemperaturesSection = AeroData.section(getString(R.string.overview_section_temperatures));
            this.mMemorySection = AeroData.section(getString(R.string.overview_section_memory));
            this.mConfigurationSection = AeroData.section(getString(R.string.overview_section_configuration));
        }
        this.mOverviewData.clear();
        this.mOverviewData.add(this.mSystemSection);
        this.mOverviewData.add(this.mKernelData);
        this.mOverviewData.add(this.mPerformanceSection);
        this.mOverviewData.add(this.mPerformanceData);
        this.mOverviewData.add(this.mTemperaturesSection);
        this.mOverviewData.add(this.mTemperatureData);
        this.mOverviewData.add(this.mMemorySection);
        this.mOverviewData.add(this.mRAMData);
        this.mOverviewData.add(this.mConfigurationSection);
        this.mOverviewData.add(this.mConfigurationData);
    }

    private List<AeroData.ConfigurationReading> buildConfigurationReadings(
            OverviewSnapshot snapshot) {
        Map<String, String> distinctGovernors = new LinkedHashMap<>();
        for (int i = 0; i < snapshot.governors.size(); i++) {
            String governor = normalizeValue(snapshot.governors.get(i));
            if (governor != null && !distinctGovernors.containsKey(governor)) {
                String label = i < snapshot.governorLabels.size()
                        ? snapshot.governorLabels.get(i) : null;
                distinctGovernors.put(governor, label);
            }
        }
        if (distinctGovernors.isEmpty()) {
            distinctGovernors.put(NO_DATA_FOUND, null);
        }

        List<AeroData.ConfigurationReading> readings = new ArrayList<>();
        boolean showClusterLabels = distinctGovernors.size() > 1;
        for (Map.Entry<String, String> entry : distinctGovernors.entrySet()) {
            String label = showClusterLabels && entry.getValue() != null
                    ? getString(R.string.current_governor_cluster, entry.getValue())
                    : getString(R.string.overview_cpu_governor);
            readings.add(new AeroData.ConfigurationReading(
                    AeroData.ConfigurationReading.Kind.GOVERNOR,
                    label, entry.getKey()));
        }
        String scheduler = normalizeValue(snapshot.ioScheduler);
        readings.add(new AeroData.ConfigurationReading(
                AeroData.ConfigurationReading.Kind.IO_SCHEDULER,
                getString(R.string.current_io_governor),
                scheduler == null ? NO_DATA_FOUND : scheduler));
        return readings;
    }

    private String normalizeValue(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        if (normalized.length() == 0 || normalized.equalsIgnoreCase(NO_DATA_FOUND)) {
            return null;
        }
        return normalized;
    }

    /**
     * Displays the first-time tutorial showcase overlay for this fragment.
     *
     * @param header the resource ID for the showcase title
     * @param content the resource ID for the showcase content text
     */
    public void DrawFirstStart(int header, int content) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(FILENAME, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.AeroFragment.3
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                return new Point(100, AeroFragment.this.mActionBarHeight);
            }
        };
        this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

    /**
     * Sets file permissions for CPU frequency scaling files to allow user modification.
     * Requires root access to modify system file permissions.
     */
    public void setPermissions() {
        String[] commands = {"chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq", "chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"};
        AeroActivity.shell.setRootInfo(commands);
        this.mExecuted = true;
    }
}
