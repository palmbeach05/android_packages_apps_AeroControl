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
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private static final int INVALID_THERMAL_ZONE_PRIORITY = Integer.MAX_VALUE;
    private static final double MIN_CPU_TEMPERATURE_CELSIUS = -100.0d;
    private static final double MAX_CPU_TEMPERATURE_CELSIUS = 250.0d;
    private String gpu_file;
    private AeroAdapter mAdapter;
    private AeroData mFrequencyData;
    private AeroData mGPUData;
    private final CpuClusterHelper mCpuClusterHelper = new CpuClusterHelper();
    private final List<AeroData> mGovernorData = new ArrayList<>();
    private AeroData mIOSchedulerData;
    private AeroData mKernelData;
    private ListView mOverView;
    private AeroData mRAMData;
    private ShowcaseView mShowCase;
    private ViewGroup root;
    private List<AeroData> mOverviewData = new ArrayList();
    private int mActionBarHeight = 0;
    private boolean mVisible = true;
    private boolean mExecuted = false;
    private RefreshThread mRefreshThread = new RefreshThread();
    private ThermalThread mThermalThread;
    private volatile String mCPUTemperature;
    private volatile String mCPUTemperaturePath;
    private Handler mRefreshHandler = new Handler() { // from class: com.aero.control.fragments.AeroFragment.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what >= 1 && AeroFragment.this.isVisible() && AeroFragment.this.mVisible) {
                AeroFragment.this.createList();
                AeroFragment.this.mVisible = true;
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
                    sleep(1000L);
                    AeroFragment.this.mRefreshHandler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    private class ThermalThread extends Thread {
        private volatile boolean mInterrupt;

        /**
         * Cancels the thermal thread by setting the interrupt flag and interrupting the thread.
         */
        public void cancel() {
            this.mInterrupt = true;
            interrupt();
        }

        /**
         * Continuously monitors CPU temperature by detecting the appropriate thermal zone path
         * and reading temperature values. Retries path detection every second until a valid
         * CPU thermal zone is found, then polls temperature every second and triggers UI
         * refresh when the temperature changes.
         */
        @Override
        public void run() {
            String temperaturePath = null;
            while (!this.mInterrupt) {
                if (temperaturePath == null) {
                    temperaturePath = findCPUTemperaturePath();
                    if (temperaturePath == null) {
                        try {
                            sleep(1000L);
                        } catch (InterruptedException e) {
                            return;
                        }
                        continue;
                    }
                    AeroFragment.this.mCPUTemperaturePath = temperaturePath;
                }
                String temperature = formatCPUTemperature(
                        AeroActivity.shell.getInfo(temperaturePath));
                if (this.mInterrupt) {
                    return;
                }
                if (temperature != null
                        && !temperature.equals(AeroFragment.this.mCPUTemperature)) {
                    AeroFragment.this.mCPUTemperature = temperature;
                    requestTemperatureRefresh(this);
                }
                try {
                    sleep(1000L);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    /**
     * Called when the fragment is paused. Sets the visibility flag to false
     * to prevent UI updates while the fragment is not visible.
     */
    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mVisible = false;
    }

    /**
     * Called when the fragment is resumed. Sets the visibility flag to true
     * to allow UI updates to proceed.
     */
    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mVisible = true;
    }

    /**
     * Called when the view is being destroyed. Cleans up background threads,
     * removes pending messages from the handler, and nullifies view references
     * to prevent memory leaks.
     */
    @Override // android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        this.mRefreshThread.cancel();
        if (this.mThermalThread != null) {
            this.mThermalThread.cancel();
            this.mThermalThread = null;
        }
        this.mRefreshHandler.removeMessages(1);
        this.mRefreshHandler.removeCallbacksAndMessages(null);
        this.mAdapter = null;
        this.mOverView = null;
        this.root = null;
    }

    /**
     * Creates and initializes the fragment's view hierarchy. Inflates the layout,
     * detects available GPU frequency file, starts background refresh and thermal
     * monitoring threads, and sets file permissions if needed.
     *
     * @param inflater the LayoutInflater to inflate views
     * @param container the parent view that this fragment's UI will be attached to
     * @param savedInstanceState saved state from a previous instance, if any
     * @return the root view of the fragment
     */
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
        this.mCPUTemperature = null;
        this.mCPUTemperaturePath = null;
        createList();
        this.mThermalThread = new ThermalThread();
        this.mThermalThread.start();
        this.mThermalThread.setPriority(Thread.MIN_PRIORITY);
        if (!this.mExecuted) {
            setPermissions();
        }
        return this.root;
    }

    /**
     * Called after the activity has been created. Checks if this is the first run
     * and displays a tutorial showcase if the first-run marker file does not exist.
     *
     * @param savedInstanceState saved state from a previous instance, if any
     */
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

    /**
     * Scans the /sys/devices/virtual/thermal directory for CPU thermal zones and
     * returns the temperature file path of the best candidate. Prioritizes zones
     * by type ("cpu", "cpu-therm", "cpu-thermal", "soc") and by zone name if tied.
     * Validates that each candidate returns a valid temperature reading.
     *
     * @return the absolute path to the temperature file of the selected CPU thermal zone,
     *         or null if no valid CPU thermal zone is found
     */
    private String findCPUTemperaturePath() {
        String[] thermalZones = AeroActivity.shell.getDirInfo(THERMAL_ZONE_DIRECTORY, false);
        if (thermalZones == null) {
            thermalZones = AeroActivity.shell.getRootArray("ls -1 " + THERMAL_ZONE_DIRECTORY, "\n");
        }
        if (thermalZones == null) {
            return null;
        }

        List<ThermalZoneCandidate> candidates = new ArrayList<>();
        for (String thermalZone : thermalZones) {
            if (!THERMAL_ZONE_NAME_PATTERN.matcher(thermalZone).matches()) {
                continue;
            }
            String zonePath = THERMAL_ZONE_DIRECTORY + "/" + thermalZone + "/";
            String type = AeroActivity.shell.getInfo(zonePath + THERMAL_ZONE_TYPE_FILE);
            if (!isCPUThermalZone(type)) {
                continue;
            }
            String temperaturePath = zonePath + THERMAL_ZONE_TEMP_FILE;
            String temperature = formatCPUTemperature(AeroActivity.shell.getInfo(temperaturePath));
            if (temperature != null) {
                candidates.add(new ThermalZoneCandidate(
                        thermalZone, getCPUThermalZonePriority(type), temperaturePath));
            }
        }

        ThermalZoneCandidate selected = null;
        for (ThermalZoneCandidate candidate : candidates) {
            if (selected == null
                    || candidate.typePriority < selected.typePriority
                    || (candidate.typePriority == selected.typePriority
                            && candidate.zoneName.compareTo(selected.zoneName) < 0)) {
                selected = candidate;
            }
        }
        return selected == null ? null : selected.temperaturePath;
    }

    /**
     * Determines whether the given thermal zone type string represents a CPU thermal zone.
     *
     * @param type the thermal zone type string to check
     * @return true if the type is recognized as a CPU thermal zone, false otherwise
     */
    private boolean isCPUThermalZone(String type) {
        return getCPUThermalZonePriority(type) != INVALID_THERMAL_ZONE_PRIORITY;
    }

    /**
     * Returns the priority ranking for a given thermal zone type. Lower values indicate
     * higher priority. Recognizes "cpu" (priority 0), "cpu-therm" (1), "cpu-thermal" (2),
     * and "soc" (3). Type comparison is case-insensitive and normalizes underscores to hyphens.
     *
     * @param type the thermal zone type string to evaluate
     * @return the priority value (lower is better), or {@link #INVALID_THERMAL_ZONE_PRIORITY}
     *         if the type is not recognized as a CPU thermal zone
     */
    private int getCPUThermalZonePriority(String type) {
        if (type == null) {
            return INVALID_THERMAL_ZONE_PRIORITY;
        }
        String normalizedType = type.trim().toLowerCase(Locale.US).replace('_', '-');
        if (normalizedType.equals("cpu")) {
            return 0;
        }
        if (normalizedType.equals("cpu-therm")) {
            return 1;
        }
        if (normalizedType.equals("cpu-thermal")) {
            return 2;
        }
        if (normalizedType.equals("soc")) {
            return 3;
        }
        return INVALID_THERMAL_ZONE_PRIORITY;
    }

    private static final class ThermalZoneCandidate {
        private final String zoneName;
        private final int typePriority;
        private final String temperaturePath;

        /**
         * Constructs a thermal zone candidate record.
         *
         * @param zoneName the name of the thermal zone directory (e.g., "thermal_zone0")
         * @param typePriority the priority ranking of the zone's type (lower is better)
         * @param temperaturePath the absolute path to the zone's temperature file
         */
        private ThermalZoneCandidate(String zoneName, int typePriority, String temperaturePath) {
            this.zoneName = zoneName;
            this.typePriority = typePriority;
            this.temperaturePath = temperaturePath;
        }
    }

    /**
     * Requests a UI refresh to update the displayed CPU temperature. Posts a runnable
     * to the main thread handler that updates the frequency data's right_name field
     * with the current temperature and notifies the adapter. Includes safety checks
     * to ensure the thermal thread, views, and adapter are still valid.
     *
     * @param thermalThread the thermal thread requesting the refresh, used to verify
     *                      it is still the active thread
     */
    private void requestTemperatureRefresh(final ThermalThread thermalThread) {
        this.mRefreshHandler.post(new Runnable() {
            @Override
            public void run() {
                if (AeroFragment.this.mThermalThread != thermalThread
                        || AeroFragment.this.root == null
                        || AeroFragment.this.mOverView == null
                        || AeroFragment.this.mAdapter == null
                        || !AeroFragment.this.isAdded()) {
                    return;
                }
                if (AeroFragment.this.mFrequencyData != null) {
                    AeroFragment.this.mFrequencyData.right_name =
                            AeroFragment.this.mCPUTemperature;
                    AeroFragment.this.mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * Formats a raw temperature value read from a thermal zone file into a human-readable
     * string with degrees Celsius. Handles values in millidegrees (divides by 1000 if needed),
     * validates the temperature is within reasonable bounds (-100 to 250 degrees C), and
     * strips trailing zeros from the decimal representation.
     *
     * @param rawTemperature the raw temperature string from the thermal zone file
     * @return a formatted temperature string (e.g., "45.5 °C"), or null if the value
     *         is invalid, out of range, or unavailable
     */
    private String formatCPUTemperature(String rawTemperature) {
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
            if (Math.abs(celsius) >= 1000.0d) {
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

    /**
     * Populates or updates the internal data objects (kernel, governors, I/O scheduler,
     * CPU frequencies, GPU frequency, and memory) with current system information.
     * Reuses existing AeroData objects when available to avoid unnecessary allocations.
     *
     * @param gpu_freq the raw GPU frequency string read from the GPU frequency file
     */
    private void fillData(String gpu_freq) {
        if (this.mKernelData == null) {
            this.mKernelData = new AeroData(getString(R.string.kernel_version), AeroActivity.shell.getKernel(), null);
        } else {
            this.mKernelData.content = AeroActivity.shell.getKernel();
        }
        List<CpuClusterHelper.Cluster> clusters = this.mCpuClusterHelper.getClusters();
        for (int i = 0; i < clusters.size(); i++) {
            CpuClusterHelper.Cluster cluster = clusters.get(i);
            String governor = AeroActivity.shell.getInfo(FilePath.CPU_BASE_PATH + cluster.getRepresentativeCpu() + FilePath.CURRENT_GOV_AVAILABLE);
            if (i < this.mGovernorData.size()) {
                this.mGovernorData.get(i).content = governor;
            } else {
                this.mGovernorData.add(new AeroData(getString(R.string.current_governor_cluster, cluster.getMemberRangeLabel()), governor, null));
            }
        }
        while (this.mGovernorData.size() > clusters.size()) {
            this.mGovernorData.remove(this.mGovernorData.size() - 1);
        }
        if (this.mIOSchedulerData == null) {
            this.mIOSchedulerData = new AeroData(getString(R.string.current_io_governor), AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE), null);
        } else {
            this.mIOSchedulerData.content = AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE);
        }
        int coreCount = Runtime.getRuntime().availableProcessors();
        List<String> coreFrequencies = coreCount <= MAX_GRID_CORES ? getCoreFrequencyList() : null;
        String frequencyContent = coreCount <= MAX_GRID_CORES ? getCpuUtilizationLine() : getFreqPerCore();
        if (this.mFrequencyData == null) {
            this.mFrequencyData = new AeroData(getString(R.string.current_cpu_speed), frequencyContent, this.mCPUTemperature);
        } else {
            this.mFrequencyData.content = frequencyContent;
            this.mFrequencyData.right_name = this.mCPUTemperature;
        }
        this.mFrequencyData.coreFrequencies = coreFrequencies;
        if (this.mGPUData == null) {
            this.mGPUData = new AeroData(getString(R.string.current_gpu_speed), AeroActivity.shell.toMHz(gpu_freq.substring(0, gpu_freq.length() - 3)), null);
        } else {
            this.mGPUData.content = AeroActivity.shell.toMHz(gpu_freq.substring(0, gpu_freq.length() - 3));
        }
        if (this.mRAMData == null) {
            this.mRAMData = new AeroData(getString(R.string.available_memory), AeroActivity.shell.getMemory(FilePath.FILENAME_PROC_MEMINFO), null);
        } else {
            this.mRAMData.content = AeroActivity.shell.getMemory(FilePath.FILENAME_PROC_MEMINFO);
        }
    }

    /**
     * Creates and populates the overview list with system information including
     * kernel version, CPU governors, GPU frequency, and memory status.
     */
    public void createList() {
        if (this.mOverviewData != null) {
            this.mOverviewData.clear();
        }
        if (this.mAdapter != null) {
            this.mAdapter.clear();
            this.mAdapter.notifyDataSetChanged();
        }
        String gpu_freq = AeroActivity.shell.getInfo(this.gpu_file);
        if (gpu_freq.length() <= 3) {
            gpu_freq = NO_DATA_FOUND;
        }
        fillData(gpu_freq);
        this.mOverviewData.add(this.mKernelData);
        this.mOverviewData.addAll(this.mGovernorData);
        this.mOverviewData.add(this.mIOSchedulerData);
        this.mOverviewData.add(this.mFrequencyData);
        this.mOverviewData.add(this.mGPUData);
        this.mOverviewData.add(this.mRAMData);
        if (this.mAdapter == null) {
            this.mAdapter = new AeroAdapter(getActivity(), R.layout.overviewlist_item, this.mOverviewData);
            this.mOverView.setAdapter((ListAdapter) this.mAdapter);
        } else {
            getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.AeroFragment.2
                @Override // java.lang.Runnable
                public void run() {
                    AeroFragment.this.mAdapter.notifyDataSetChanged();
                }
            });
        }
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
