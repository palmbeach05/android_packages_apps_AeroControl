package com.aero.control.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomEditText;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.Android.CustomPreference;
import com.aero.control.helpers.CpuClusterHelper;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.FrequencyFormat;
import com.aero.control.helpers.HardwareMetricsPoller;
import com.aero.control.helpers.legacy.LegacyOverclockController;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.SysfsDirectory;
import com.aero.control.helpers.SysfsResult;
import com.aero.control.helpers.ReadMode;
import com.aero.control.helpers.SysfsResult;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fragment for configuring CPU settings including min/max frequency, governor,
 * and governor-specific parameters. Supports multi-cluster CPUs with separate
 * controls for big and little cores. Provides save/apply functionality for
 * boot and profile persistence.
 */
public class CPUFragment extends PlaceHolderFragment {
    private static final String FILENAME = "firstrun_cpu";
    private static final String NO_DATA_FOUND = "Unavailable";
    private PreferenceCategory PrefCat;
    private CPUBoostFragment mCPUBoostFragment;
    private CustomListPreference mCPUGovernor;
    private CPUHotplugFragment mHotplugFragment;
    private String mHotplugPath;
    private ShowcaseView mShowCase;
    private VoltageFragment mVoltageFragment;
    private PreferenceScreen root;
    private final CpuClusterHelper mClusterHelper = new CpuClusterHelper();
    private final List<ClusterControls> mClusterControls = new ArrayList<>();
    private CheckBoxPreference mApplyToAllClusters;
    /** Synchronizes access to ListPreference state (getValue, getEntryValues) between the serialized mirror executor and the main thread. */
    private final Object mPreferenceLock = new Object();
    /** Serializes mirrored max-frequency, min-frequency and governor operations so they observe each other's committed state and run in submission order. */
    private final ExecutorService mMirrorExecutor = Executors.newSingleThreadExecutor();
    /** Monotonically increasing per-request sequence number for max-frequency requests, incremented at enqueue time to ensure each queued request gets a distinct ID. */
    private final AtomicInteger mMaxFreqRequestSequence = new AtomicInteger(0);
    /** Monotonically increasing per-request sequence number for min-frequency requests, incremented at enqueue time to ensure each queued request gets a distinct ID. */
    private final AtomicInteger mMinFreqRequestSequence = new AtomicInteger(0);
    /** Tracks the last committed max-frequency request sequence; used to discard stale UI callbacks from superseded max-frequency requests. */
    private final AtomicInteger mMaxFreqMirrorGeneration = new AtomicInteger(0);
    /** Tracks the last committed min-frequency request sequence; used to discard stale UI callbacks from superseded min-frequency requests. */
    private final AtomicInteger mMinFreqMirrorGeneration = new AtomicInteger(0);
    /** Incremented every time an apply-all governor mirror request is initiated; used to discard stale UI callbacks from superseded governor requests. */
    private final AtomicInteger mGovernorMirrorGeneration = new AtomicInteger(0);
    /** Incremented on fragment destruction to invalidate all pending mirror callbacks regardless of their setting type. */
    private final AtomicInteger mLifecycleGeneration = new AtomicInteger(0);
    private HardwareMetricsPoller.Subscription mMetricsSubscription;
    private boolean mWantsMetricsPolling;
    private volatile boolean mTableUpdate;

    /** Holds the max-frequency, min-frequency and governor controls generated for a single cpufreq cluster. */
    private static final class ClusterControls {
        private final int index;
        private final CpuClusterHelper.Cluster cluster;
        private final CustomListPreference maxFrequency;
        private final CustomListPreference minFrequency;
        private final CustomListPreference governor;
        /** Last max/min frequency committed by a serialized mirror operation, ahead of the value reflected by the preference itself. Null until a mirror operation commits a value. */
        private String pendingMaxFrequency;
        private String pendingMinFrequency;

        private ClusterControls(int index, CpuClusterHelper.Cluster cluster, CustomListPreference maxFrequency, CustomListPreference minFrequency, CustomListPreference governor) {
            this.index = index;
            this.cluster = cluster;
            this.maxFrequency = maxFrequency;
            this.minFrequency = minFrequency;
            this.governor = governor;
        }
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.layout.cpu_fragment);
        this.root = getPreferenceScreen();
        PreferenceCategory cpuCategory = (PreferenceCategory) findPreference("cpu_settings");
        PreferenceCategory cpuGovernor = (PreferenceCategory) findPreference("cpu_governor");
        List<CpuClusterHelper.Cluster> clusters = this.mClusterHelper.getClusters();
        boolean singleCluster = clusters.size() == 1;
        this.mClusterControls.clear();
        for (int i = 0; i < clusters.size(); i++) {
            CpuClusterHelper.Cluster cluster = clusters.get(i);
            String cpuRange = cluster.getMemberRangeLabel();

            CustomListPreference maxFrequency = new CustomListPreference(getActivity());
            maxFrequency.setName("cpu_cluster_" + i + "_max_frequency");
            CharSequence maxFrequencyTitle = singleCluster ? getString(R.string.pref_max_freq_cpu) : getString(R.string.pref_max_freq_cpu_cluster, cpuRange);
            maxFrequency.setTitle(maxFrequencyTitle);
            maxFrequency.setDialogTitle(maxFrequencyTitle);
            maxFrequency.setSummary(maxFrequencyTitle);
            maxFrequency.setDialogIcon(R.drawable.lightning);
            maxFrequency.setOrder(i * 2);
            cpuCategory.addPreference(maxFrequency);

            CustomListPreference minFrequency = new CustomListPreference(getActivity());
            minFrequency.setName("cpu_cluster_" + i + "_min_frequency");
            CharSequence minFrequencyTitle = singleCluster ? getString(R.string.pref_min_freq_cpu) : getString(R.string.pref_min_freq_cpu_cluster, cpuRange);
            minFrequency.setTitle(minFrequencyTitle);
            minFrequency.setDialogTitle(minFrequencyTitle);
            minFrequency.setSummary(minFrequencyTitle);
            minFrequency.setDialogIcon(R.drawable.lightning);
            minFrequency.setOrder((i * 2) + 1);
            cpuCategory.addPreference(minFrequency);

            CustomListPreference governor = new CustomListPreference(getActivity());
            governor.setName("cpu_cluster_" + i + "_governor");
            CharSequence governorTitle = singleCluster ? getString(R.string.pref_cpu_governor) : getString(R.string.pref_cpu_governor_cluster, cpuRange);
            governor.setTitle(governorTitle);
            governor.setDialogTitle(governorTitle);
            SysfsResult<String[]> governorCapabilities =
                    AeroActivity.hardware.cpu().readAvailableGovernors(cluster);
            String[] availableGovernors = governorCapabilities.isSuccess()
                    ? governorCapabilities.getValue() : new String[] {NO_DATA_FOUND};
            governor.setEntries(availableGovernors);
            governor.setEntryValues(availableGovernors);
            SysfsResult<String> governorResult =
                    AeroActivity.hardware.cpu().readGovernor(cluster);
            String currentGovernor = governorResult.isSuccess()
                    ? governorResult.getValue() : NO_DATA_FOUND;
            governor.setValue(currentGovernor);
            governor.setSummary(currentGovernor);
            governor.setDialogIcon(R.drawable.cpu);
            governor.setOrder(i);
            cpuGovernor.addPreference(governor);

            ClusterControls controls = new ClusterControls(i, cluster, maxFrequency, minFrequency, governor);
            this.mClusterControls.add(controls);
            attachFrequencyListeners(controls);
            attachGovernorListener(controls);
            if (i == 0) {
                this.mCPUGovernor = governor;
            }
        }
        this.mApplyToAllClusters = (CheckBoxPreference) cpuCategory.findPreference("apply_to_all_cpu_clusters");
        if (this.mApplyToAllClusters != null) {
            if (this.mClusterControls.size() <= 1) {
                cpuCategory.removePreference(this.mApplyToAllClusters);
                this.mApplyToAllClusters = null;
            } else {
                this.mApplyToAllClusters.setOrder(-1);
                updateClusterControlsEnabled(this.mApplyToAllClusters.isChecked());
                this.mApplyToAllClusters.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override // android.preference.Preference.OnPreferenceChangeListener
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        CPUFragment.this.updateClusterControlsEnabled((Boolean) newValue);
                        return true;
                    }
                });
            }
        }
        refreshCpuFrequencies();
        String[] arr$ = FilePath.HOTPLUG_PATH;
        for (String s : arr$) {
            if (AeroActivity.genHelper.doesExist(s)) {
                this.mHotplugPath = s;
            }
        }
        CustomPreference cpu_hotplug = (CustomPreference) this.root.findPreference("hotplug_control");
        if (AeroActivity.genHelper.doesExist(this.mHotplugPath)) {
            cpu_hotplug.setHideOnBoot(true);
            cpu_hotplug.setOrder(10);
            cpu_hotplug.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.CPUFragment.1
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    if (CPUFragment.this.mHotplugFragment == null) {
                        CPUFragment.this.mHotplugFragment = new CPUHotplugFragment();
                    }
                    AeroActivity.mHandler.post(new Runnable() { // from class: com.aero.control.fragments.CPUFragment.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!CPUFragment.this.isAdded() || CPUFragment.this.getFragmentManager() == null) {
                                return;
                            }
                            try {
                                CPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, CPUFragment.this.mHotplugFragment).addToBackStack("Hotplug").commit();
                            } catch (IllegalStateException e) {
                                Log.e("Aero", "Could not commit fragment transaction, state already saved.", e);
                            }
                        }
                    });
                    return true;
                }
            });
        } else {
            cpuCategory.removePreference(cpu_hotplug);
        }
        CustomPreference voltage_control = (CustomPreference) this.root.findPreference("voltage_values");
        if (AeroActivity.genHelper.doesExist(FilePath.VOLTAGE_PATH)) {
            voltage_control.setOrder(15);
            voltage_control.setLookUpDefault(FilePath.VOLTAGE_PATH);
            voltage_control.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.CPUFragment.2
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    if (CPUFragment.this.mVoltageFragment == null) {
                        CPUFragment.this.mVoltageFragment = new VoltageFragment();
                    }
                    AeroActivity.mHandler.post(new Runnable() { // from class: com.aero.control.fragments.CPUFragment.2.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!CPUFragment.this.isAdded() || CPUFragment.this.getFragmentManager() == null) {
                                return;
                            }
                            try {
                                CPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, CPUFragment.this.mVoltageFragment).addToBackStack("Voltage").commit();
                            } catch (IllegalStateException e) {
                                Log.e("Aero", "Could not commit fragment transaction, state already saved.", e);
                            }
                        }
                    });
                    return true;
                }
            });
        } else {
            cpuCategory.removePreference(voltage_control);
        }
        CustomPreference cpu_boost_control = (CustomPreference) this.root.findPreference("cpu_boost_control");
        if (AeroActivity.genHelper.doesExist(FilePath.CPU_BOOST)) {
            cpu_boost_control.setOrder(18);
            cpu_boost_control.setHideOnBoot(true);
            cpu_boost_control.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.CPUFragment.3
                @Override // android.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    if (CPUFragment.this.mCPUBoostFragment == null) {
                        CPUFragment.this.mCPUBoostFragment = new CPUBoostFragment();
                    }
                    AeroActivity.mHandler.post(new Runnable() { // from class: com.aero.control.fragments.CPUFragment.3.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!CPUFragment.this.isAdded() || CPUFragment.this.getFragmentManager() == null) {
                                return;
                            }
                            try {
                                CPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, CPUFragment.this.mCPUBoostFragment).addToBackStack("CPUBoost").commit();
                            } catch (IllegalStateException e) {
                                Log.e("Aero", "Could not commit fragment transaction, state already saved.", e);
                            }
                        }
                    });
                    return true;
                }
            });
        } else {
            cpuCategory.removePreference(cpu_boost_control);
        }
        Preference cpu_oc_uc = this.root.findPreference("cpu_commands");
        SysfsResult<int[]> overclockVoltages =
                AeroActivity.hardware.legacyOverclock().readVoltageSelections();
        if (!overclockVoltages.isSuccess()) {
            cpuCategory.removePreference(cpu_oc_uc);
        } else {
            cpu_oc_uc.setOrder(20);
        }
        cpu_oc_uc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.CPUFragment.4
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                SysfsResult<int[]> voltageSelections =
                        AeroActivity.hardware.legacyOverclock().readVoltageSelections();
                SysfsResult<String[]> availableFrequencies =
                        AeroActivity.hardware.cpu().readAvailableFrequencies(0, ReadMode.RAW);
                if (!availableFrequencies.isSuccess()) {
                    Log.e("Aero", "Unable to read CPU frequencies for legacy overclock: "
                            + availableFrequencies.getError());
                    return false;
                }
                CharSequence[] cpufreq = availableFrequencies.getValue();
                if (!voltageSelections.isSuccess()
                        || voltageSelections.getValue().length != cpufreq.length) {
                    Log.e("Aero", "Unable to load legacy overclock table: "
                            + (voltageSelections.isSuccess()
                                    ? "frequency and voltage table sizes do not match"
                                    : voltageSelections.getError()));
                    return false;
                }
                final int[] currentVoltages = voltageSelections.getValue();
                AlertDialog.Builder builder = new AlertDialog.Builder(CPUFragment.this.getActivity());
                LayoutInflater inflater = CPUFragment.this.getActivity().getLayoutInflater();
                View layout = inflater.inflate(R.layout.cpu_oc_uc, (ViewGroup) null);
                final ViewGroup viewGroup = (ViewGroup) layout.findViewById(R.id.cpu_container);
                int i = 0;
                for (CharSequence a : cpufreq) {
                    for (int j = 0; j < 2; j++) {
                        CustomEditText cpuValues = new CustomEditText(CPUFragment.this.getActivity());
                        if (j != 0) {
                            cpuValues.setText(Integer.toString(currentVoltages[i]));
                        } else {
                            cpuValues.setText(a);
                        }
                        viewGroup.addView(cpuValues);
                        ViewGroup.MarginLayoutParams cpuMargins = new ViewGroup.MarginLayoutParams(cpuValues.getLayoutParams());
                        cpuMargins.setMargins(0, i * 75, j * 30, 0);
                        RelativeLayout.LayoutParams cpuLayout = new RelativeLayout.LayoutParams(cpuMargins);
                        if (j > 0) {
                            cpuLayout.addRule(11);
                            cpuLayout.width = 100;
                        } else {
                            cpuLayout.width = 200;
                        }
                        cpuValues.setLayoutParams(cpuLayout);
                    }
                    i++;
                }
                builder.setIcon(R.drawable.calculator);
                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.CPUFragment.4.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        ArrayList<Integer> cpuFreqs = new ArrayList<>();
                        ArrayList<Integer> vselValues = new ArrayList<>();
                        int t = viewGroup.getChildCount();
                        for (int l = 0; l < t; l++) {
                            CustomEditText editText = (CustomEditText) viewGroup.getChildAt(l);
                            try {
                                int tmp = Integer.parseInt(editText.getText().toString());
                                if (l % 2 > 0) {
                                    if (l > 1 && vselValues.get(vselValues.size() - 1).intValue() < tmp && tmp > 15 && tmp < 80) {
                                        Log.e("Aero", "Invalid input: " + tmp + " Last input: " + vselValues.get(vselValues.size() - 1));
                                        return;
                                    }
                                    vselValues.add(Integer.valueOf(tmp));
                                } else {
                                    if (l > 1 && cpuFreqs.get(cpuFreqs.size() - 1).intValue() < tmp && tmp > 1500000 && tmp > 300000) {
                                        Log.e("Aero", "Invalid input: " + tmp + " Last input: " + cpuFreqs.get(cpuFreqs.size() - 1));
                                        return;
                                    }
                                    cpuFreqs.add(Integer.valueOf(tmp));
                                }
                            } catch (NumberFormatException e) {
                                Log.e("Aero", "An Error occured! ", e);
                                return;
                            }
                        }
                        Integer[] newFrequencies = (Integer[]) cpuFreqs.toArray(new Integer[0]);
                        int[] frequencyValues = new int[newFrequencies.length];
                        int[] voltageValues = new int[vselValues.size()];
                        for (int j = 0; j < newFrequencies.length; j++) {
                            frequencyValues[j] = newFrequencies[j];
                            voltageValues[j] = vselValues.get(j);
                        }
                        SysfsResult<Boolean> writeResult = AeroActivity.hardware.legacyOverclock()
                                .writeFrequencyTable(frequencyValues, voltageValues);
                        if (!writeResult.isSuccess()) {
                            Log.e("Aero", "Failed to apply legacy overclock table: "
                                    + writeResult.getError());
                            Toast.makeText(CPUFragment.this.getActivity(),
                                    R.string.hardware_operation_failed,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        SharedPreferences preference2 = PreferenceManager.getDefaultSharedPreferences(CPUFragment.this.getActivity().getBaseContext());
                        boolean saved = preference2.edit()
                                .putString(LegacyOverclockController.PREF_FREQUENCIES,
                                        serializeIntegers(frequencyValues))
                                .putString(LegacyOverclockController.PREF_VOLTAGES,
                                        serializeIntegers(voltageValues))
                                .remove("cpu_commands")
                                .commit();
                        if (!saved) {
                            Log.e("Aero", "Failed to save the legacy overclock table");
                        }
                        CPUFragment.this.mWantsMetricsPolling = true;
                        CPUFragment.this.startMetricsPolling();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.CPUFragment.4.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                    }
                });
                builder.setView(layout).setTitle(R.string.perf_live_oc_uc).show();
                return false;
            }
        });
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
    }

    private void attachFrequencyListeners(final ClusterControls controls) {
        controls.maxFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.6
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String a = (String) o;
                if (controls.index == 0 && CPUFragment.this.isApplyToAllClustersEnabled()) {
                    return CPUFragment.this.applyMaxFrequencyToAllClusters(controls, a);
                } else {
                    // Validate under lock, but perform shell I/O outside lock
                    final String minFreqValue;
                    synchronized (mPreferenceLock) {
                        if (!CPUFragment.this.isConfiguredListValue(controls.maxFrequency, a)) {
                            return false;
                        }
                        minFreqValue = controls.minFrequency.getValue();
                    }
                    if (minFreqValue != null && !minFreqValue.equals(NO_DATA_FOUND)) {
                        try {
                            if (Integer.parseInt(a) < Integer.parseInt(minFreqValue)) {
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                    final int lifecycleGeneration = mLifecycleGeneration.get();
                    mMirrorExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            final SysfsResult<String> result =
                                    AeroActivity.hardware.cpu().writeMaxFrequency(
                                            controls.cluster, a);
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                                        return;
                                    }
                                    if (result.isSuccess()) {
                                        synchronized (mPreferenceLock) {
                                            controls.maxFrequency.setSummary(FrequencyFormat.toMHz(a));
                                            controls.maxFrequency.setValue(a);
                                            controls.pendingMaxFrequency = a;
                                        }
                                    } else {
                                        Log.e("Aero", "Failed to write CPU maximum frequency: "
                                                + result.getError());
                                    }
                                }
                            });
                        }
                    });
                    return false;
                }
            }
        });
        controls.minFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.7
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String a = (String) o;
                if (controls.index == 0 && CPUFragment.this.isApplyToAllClustersEnabled()) {
                    return CPUFragment.this.applyMinFrequencyToAllClusters(controls, a);
                } else {
                    // Validate under lock, but perform shell I/O outside lock
                    final String maxFreqValue;
                    synchronized (mPreferenceLock) {
                        if (!CPUFragment.this.isConfiguredListValue(controls.minFrequency, a)) {
                            return false;
                        }
                        maxFreqValue = controls.maxFrequency.getValue();
                    }
                    if (maxFreqValue != null && !maxFreqValue.equals(NO_DATA_FOUND)) {
                        try {
                            if (Integer.parseInt(a) > Integer.parseInt(maxFreqValue)) {
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }

                    final int lifecycleGeneration = mLifecycleGeneration.get();
                    mMirrorExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            final SysfsResult<String> result =
                                    AeroActivity.hardware.cpu().writeMinFrequency(
                                            controls.cluster, a);
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                                        return;
                                    }
                                    if (result.isSuccess()) {
                                        synchronized (mPreferenceLock) {
                                            controls.minFrequency.setSummary(FrequencyFormat.toMHz(a));
                                            controls.minFrequency.setValue(a);
                                            controls.pendingMinFrequency = a;
                                        }
                                    } else {
                                        Log.e("Aero", "Failed to write CPU minimum frequency: "
                                                + result.getError());
                                    }
                                }
                            });
                        }
                    });
                    return false;
                }
            }
        });
    }

    private void attachGovernorListener(final ClusterControls controls) {
        controls.governor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.5
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String a = (String) o;
                synchronized (mPreferenceLock) {
                    if (!CPUFragment.this.isConfiguredListValue(controls.governor, a)) {
                        return false;
                    }
                }
                if (CPUFragment.this.PrefCat != null) {
                    CPUFragment.this.root.removePreference(CPUFragment.this.PrefCat);
                }
                if (controls.index == 0 && CPUFragment.this.isApplyToAllClustersEnabled()) {
                    return CPUFragment.this.applyGovernorToAllClusters(controls, a);
                } else {
                    final int lifecycleGeneration = mLifecycleGeneration.get();
                    mMirrorExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            boolean writeSucceeded = CPUFragment.this.setGovernor(a, controls.cluster);
                            if (!writeSucceeded) {
                                Log.e("Aero", "Failed to write governor " + a + " to cluster " + controls.index);
                            }
                            // Poll for governor change completion (max 10 attempts * 50ms = 500ms)
                            String currentGovernor = null;
                            for (int i = 0; i < 10; i++) {
                                try {
                                    Thread.sleep(50L);
                                } catch (InterruptedException e) {
                                    Log.e("Aero", "Governor polling interrupted", e);
                                    break;
                                }
                                SysfsResult<String> governorResult =
                                        AeroActivity.hardware.cpu().readGovernor(controls.cluster);
                                currentGovernor = governorResult.isSuccess()
                                        ? governorResult.getValue() : NO_DATA_FOUND;
                                if (a.equals(currentGovernor)) {
                                    break;
                                }
                            }
                            final String capturedGovernor = currentGovernor;
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                                        return;
                                    }
                                    // Only apply valid values (skip null or NO_DATA_FOUND)
                                    if (capturedGovernor != null && !NO_DATA_FOUND.equals(capturedGovernor)) {
                                        synchronized (mPreferenceLock) {
                                            controls.governor.setSummary(capturedGovernor);
                                            controls.governor.setValue(capturedGovernor);
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
                return false;
            }
        });
    }

    /** Whether changes made through the first cluster's controls should be mirrored to every detected cluster. */
    private boolean isApplyToAllClustersEnabled() {
        return this.mApplyToAllClusters != null && this.mApplyToAllClusters.isChecked();
    }

    /** Enables or disables the max-frequency, min-frequency and governor controls for every cluster after the first. */
    private void updateClusterControlsEnabled(boolean applyToAll) {
        for (int i = 1; i < this.mClusterControls.size(); i++) {
            ClusterControls controls = this.mClusterControls.get(i);
            controls.maxFrequency.setEnabled(!applyToAll);
            controls.minFrequency.setEnabled(!applyToAll);
            controls.governor.setEnabled(!applyToAll);
        }
    }

    private boolean applyMaxFrequencyToAllClusters(ClusterControls source, String value) {
        queueFrequencyMirrorOperation(source, value, true);
        return false;
    }

    private boolean applyMinFrequencyToAllClusters(ClusterControls source, String value) {
        queueFrequencyMirrorOperation(source, value, false);
        return false;
    }

    /** Captures a unique per-request sequence number and enqueues the mirror operation on the shared serialized executor. */
    private void queueFrequencyMirrorOperation(final ClusterControls source, final String value, final boolean isMax) {
        final int requestSequence = (isMax ? mMaxFreqRequestSequence : mMinFreqRequestSequence).incrementAndGet();
        final int lifecycleGeneration = mLifecycleGeneration.get();
        mMirrorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                applyFrequencyToAllClusters(source, value, isMax, requestSequence, lifecycleGeneration);
            }
        });
    }

    /** Returns the most recently committed max frequency for a cluster, falling back to the preference's current value if no mirror operation has committed one yet. */
    private String getPendingMaxFrequency(ClusterControls target) {
        synchronized (mPreferenceLock) {
            return target.pendingMaxFrequency != null ? target.pendingMaxFrequency : target.maxFrequency.getValue();
        }
    }

    /** Returns the most recently committed min frequency for a cluster, falling back to the preference's current value if no mirror operation has committed one yet. */
    private String getPendingMinFrequency(ClusterControls target) {
        synchronized (mPreferenceLock) {
            return target.pendingMinFrequency != null ? target.pendingMinFrequency : target.minFrequency.getValue();
        }
    }

    /**
     * Rolls back frequency preferences to the last committed state when a mirror operation
     * fails or encounters unsupported frequencies. This ensures the UI reflects the actual
     * committed state and prevents a failed later request from suppressing an earlier
     * successful update.
     */
    private void rollbackFrequencyPreferences(final boolean isMax, final int lifecycleGeneration) {
        AeroActivity.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (lifecycleGeneration != mLifecycleGeneration.get()) {
                    return;
                }
                synchronized (mPreferenceLock) {
                    for (ClusterControls target : mClusterControls) {
                        CustomListPreference pref = isMax ? target.maxFrequency : target.minFrequency;
                        String committed = isMax ? target.pendingMaxFrequency : target.pendingMinFrequency;
                        if (committed != null) {
                            pref.setSummary(FrequencyFormat.toMHz(committed));
                            pref.setValue(committed);
                        }
                    }
                }
            }
        });
    }

    /**
     * Mirrors a max- or min-frequency change from the first cluster's controls to every detected
     * cluster. Runs on the shared serialized mirror executor, so it always observes the
     * committed state of any preceding queued mirror operation rather than the preference's
     * stale on-screen value. Validates the value against each cluster's supported frequencies and
     * its opposite frequency limit, then performs the root write and, once it succeeds, commits
     * the new value for subsequent queued operations before posting the preference updates back
     * to the UI thread for the clusters that were actually written.
     */
    private void applyFrequencyToAllClusters(final ClusterControls source, final String value, final boolean isMax, final int requestSequence, final int lifecycleGeneration) {
        final AtomicInteger committedGeneration = isMax ? mMaxFreqMirrorGeneration : mMinFreqMirrorGeneration;

        // Check if this request is stale (a newer request was already committed)
        if (requestSequence <= committedGeneration.get()) {
            Log.e("Aero", "Discarding stale " + (isMax ? "max" : "min") + " frequency request (sequence " + requestSequence + " <= committed " + committedGeneration.get() + ")");
            return;
        }

        // Build intersection of supported frequencies across all clusters
        HashSet<String> supportedFreqs = null;
        synchronized (mPreferenceLock) {
            for (ClusterControls controls : this.mClusterControls) {
                CharSequence[] entries = (isMax ? controls.maxFrequency : controls.minFrequency).getEntryValues();
                if (entries == null) {
                    Log.e("Aero", "Cluster " + controls.index + " has no " + (isMax ? "max" : "min") + " frequency capability entries; rejecting apply-all request");
                    return;
                }
                HashSet<String> clusterFreqs = new HashSet<>();
                for (CharSequence entry : entries) {
                    clusterFreqs.add(entry.toString());
                }
                if (supportedFreqs == null) {
                    supportedFreqs = clusterFreqs;
                } else {
                    supportedFreqs.retainAll(clusterFreqs);
                }
            }
        }

        // Validate the requested frequency is in the intersection
        if (supportedFreqs == null || !supportedFreqs.contains(value)) {
            Log.e("Aero", (isMax ? "Max" : "Min") + " frequency " + value + " not supported by all clusters; rolling back to last committed state");
            rollbackFrequencyPreferences(isMax, lifecycleGeneration);
            return;
        }

        final ArrayList<ClusterControls> eligibleClusters = new ArrayList<>();
        for (ClusterControls target : this.mClusterControls) {
            // Validate against the opposite frequency limit, using the value a preceding queued
            // mirror operation has already committed rather than the (possibly stale) preference.
            try {
                String oppositeStr = isMax ? getPendingMinFrequency(target) : getPendingMaxFrequency(target);
                if (oppositeStr != null && !oppositeStr.equals(NO_DATA_FOUND)) {
                    int opposite = Integer.parseInt(oppositeStr);
                    int requested = Integer.parseInt(value);
                    if (isMax ? requested < opposite : requested > opposite) {
                        if (isMax) {
                            Log.e("Aero", "Max frequency " + value + " is lower than min frequency " + oppositeStr + " for cluster " + target.index + "; rolling back to last committed state");
                        } else {
                            Log.e("Aero", "Min frequency " + value + " is higher than max frequency " + oppositeStr + " for cluster " + target.index + "; rolling back to last committed state");
                        }
                        rollbackFrequencyPreferences(isMax, lifecycleGeneration);
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                Log.e("Aero", "Invalid frequency format; rolling back to last committed state", e);
                rollbackFrequencyPreferences(isMax, lifecycleGeneration);
                return;
            }

            eligibleClusters.add(target);
        }

        if (eligibleClusters.isEmpty()) {
            Log.e("Aero", "No valid clusters to apply " + (isMax ? "max" : "min") + " frequency; rolling back to last committed state");
            rollbackFrequencyPreferences(isMax, lifecycleGeneration);
            return;
        }

        boolean allWritesSucceeded = true;
        for (ClusterControls target : eligibleClusters) {
            SysfsResult<String> result = isMax
                    ? AeroActivity.hardware.cpu().writeMaxFrequency(target.cluster, value)
                    : AeroActivity.hardware.cpu().writeMinFrequency(target.cluster, value);
            if (!result.isSuccess()) {
                Log.e("Aero", "Failed to apply " + (isMax ? "maximum" : "minimum")
                        + " frequency to cluster " + target.index + ": " + result.getError());
                allWritesSucceeded = false;
            }
        }

        if (allWritesSucceeded) {
            // Mark this request sequence as committed
            committedGeneration.set(requestSequence);

            synchronized (mPreferenceLock) {
                for (ClusterControls target : eligibleClusters) {
                    if (isMax) {
                        target.pendingMaxFrequency = value;
                    } else {
                        target.pendingMinFrequency = value;
                    }
                }
            }
            AeroActivity.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                        return;
                    }
                    if (requestSequence != committedGeneration.get()) {
                        return;
                    }
                    synchronized (mPreferenceLock) {
                        for (ClusterControls target : eligibleClusters) {
                            CustomListPreference pref = isMax ? target.maxFrequency : target.minFrequency;
                            pref.setSummary(FrequencyFormat.toMHz(value));
                            pref.setValue(value);
                        }
                    }
                }
            });
        } else {
            Log.e("Aero", "Failed to apply " + (isMax ? "max" : "min") + " frequency to all clusters; rolling back to last committed state");
            rollbackFrequencyPreferences(isMax, lifecycleGeneration);
        }
    }

    private boolean applyGovernorToAllClusters(final ClusterControls source, final String value) {
        // Build intersection of supported governors across all clusters
        HashSet<String> supportedGovs = null;
        synchronized (mPreferenceLock) {
            for (ClusterControls controls : this.mClusterControls) {
                CharSequence[] entries = controls.governor.getEntryValues();
                if (entries == null) {
                    Log.e("Aero", "Cluster " + controls.index + " has no governor capability entries; rejecting apply-all request");
                    return false;
                }
                HashSet<String> clusterGovs = new HashSet<>();
                for (CharSequence entry : entries) {
                    clusterGovs.add(entry.toString());
                }
                if (supportedGovs == null) {
                    supportedGovs = clusterGovs;
                } else {
                    supportedGovs.retainAll(clusterGovs);
                }
            }
        }

        // Validate the requested governor is in the intersection
        if (supportedGovs == null || !supportedGovs.contains(value)) {
            Log.e("Aero", "Governor " + value + " not supported by all clusters");
            return false;
        }

        final int requestGeneration = mGovernorMirrorGeneration.incrementAndGet();
        final int lifecycleGeneration = mLifecycleGeneration.get();
        mMirrorExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (ClusterControls target : CPUFragment.this.mClusterControls) {
                    boolean writeSucceeded = setGovernor(value, target.cluster);
                    if (!writeSucceeded) {
                        Log.e("Aero", "Failed to write governor " + value + " to cluster " + target.index);
                    }
                }

                // Poll for governor change completion on all clusters
                final String[] capturedGovernors = new String[CPUFragment.this.mClusterControls.size()];
                boolean allComplete = false;
                boolean interrupted = false;
                for (int i = 0; i < 10 && !allComplete; i++) {
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        Log.e("Aero", "Governor polling interrupted", e);
                        interrupted = true;
                        break;
                    }
                    allComplete = true;
                    for (int j = 0; j < CPUFragment.this.mClusterControls.size(); j++) {
                        ClusterControls target = CPUFragment.this.mClusterControls.get(j);
                        SysfsResult<String> governorResult =
                                AeroActivity.hardware.cpu().readGovernor(target.cluster);
                        String currentGovernor = governorResult.isSuccess()
                                ? governorResult.getValue() : NO_DATA_FOUND;
                        capturedGovernors[j] = currentGovernor;
                        if (!value.equals(currentGovernor)) {
                            allComplete = false;
                        }
                    }
                }

                if (!allComplete) {
                    Log.e("Aero", "Governor mirror to all clusters did not complete (" + (interrupted ? "interrupted" : "timed out") + "); applying captured values to UI");
                }

                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (lifecycleGeneration != mLifecycleGeneration.get()) {
                            return;
                        }
                        if (requestGeneration != mGovernorMirrorGeneration.get()) {
                            return;
                        }
                        synchronized (mPreferenceLock) {
                            for (int j = 0; j < CPUFragment.this.mClusterControls.size(); j++) {
                                ClusterControls target = CPUFragment.this.mClusterControls.get(j);
                                String currentGovernor = capturedGovernors[j];
                                // Only apply valid values (skip null or NO_DATA_FOUND)
                                if (currentGovernor != null && !NO_DATA_FOUND.equals(currentGovernor)) {
                                    target.governor.setSummary(currentGovernor);
                                    target.governor.setValue(currentGovernor);
                                }
                            }
                        }
                    }
                });
            }
        });
        return false;
    }

    /**
     * Inflates the CPU fragment's options menu with governor settings menu items.
     *
     * @param menu the menu to inflate into
     * @param inflater the menu inflater to use
     */
    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Handles options menu item selections. Shows governor settings for the appropriate
     * cluster when the governor settings menu item is selected. For multi-cluster systems,
     * displays a cluster selection dialog first.
     *
     * @param item the menu item that was selected
     * @return true if the item was handled, false otherwise
     */
    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_governor_settings /* 2131099747 */:
                if (this.mClusterControls.size() > 1) {
                    showClusterSelectionForGovernorSettings();
                } else if (!this.mClusterControls.isEmpty()) {
                    showGovernorSettings(this.mClusterControls.get(0));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showClusterSelectionForGovernorSettings() {
        final CharSequence[] clusterLabels = new CharSequence[this.mClusterControls.size()];
        for (int i = 0; i < this.mClusterControls.size(); i++) {
            ClusterControls controls = this.mClusterControls.get(i);
            String cpuRange = controls.cluster.getMemberRangeLabel();
            String governor = controls.governor.getValue();
            clusterLabels[i] = "CPU " + cpuRange + " (" + governor + ")";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Cluster");
        builder.setItems(clusterLabels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showGovernorSettings(CPUFragment.this.mClusterControls.get(which));
            }
        });
        builder.show();
    }

    private void showGovernorSettings(ClusterControls controls) {
        String complete_path = FilePath.CPU_GOV_BASE + controls.governor.getValue();
        SysfsResult<String[]> listing;
        try {
            listing = AeroActivity.hardware.dynamicTunables().listSysfsParameters(
                    new SysfsDirectory(complete_path));
        } catch (IllegalArgumentException e) {
            Log.w("Aero", "Rejected CPU governor parameter directory " + complete_path, e);
            Toast.makeText(getActivity(), R.string.pref_gov_set_no_parameter, 1).show();
            return;
        }
        if (!listing.isSuccess() || listing.getValue().length == 0) {
            String error = listing.isSuccess() ? "No parameters are available"
                    : listing.getError();
            Log.w("Aero", "Unable to discover CPU governor settings: " + error);
            Toast.makeText(getActivity(), R.string.pref_gov_set_no_parameter, 1).show();
            return;
        }
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.PrefCat = new PreferenceCategory(getActivity());
        this.PrefCat.setTitle(R.string.pref_gov_set);
        this.root.addPreference(this.PrefCat);
        PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
        h.genPrefFromDictionary(listing.getValue(), complete_path);
    }

    /**
     * Pauses UI updates when the fragment becomes invisible and removes any displayed
     * governor parameter preference category.
     */
    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        stopMetricsPolling();
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
    }

    /**
     * Resumes UI updates when the fragment becomes visible again.
     */
    @Override // com.aero.control.fragments.PlaceHolderFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mWantsMetricsPolling) {
            startMetricsPolling();
        }
    }

    /**
     * Cleans up resources when the fragment is destroyed. Invalidates all pending mirror
     * request callbacks, shuts down the mirror executor, and interrupts any in-flight work.
     */
    @Override // android.app.Fragment
    public void onDestroy() {
        // Invalidate any mirror-request UI callback already posted to the main thread before the
        // fragment is torn down, then stop accepting and interrupt any in-flight mirror work.
        this.mLifecycleGeneration.incrementAndGet();
        this.mWantsMetricsPolling = false;
        stopMetricsPolling();
        this.mMirrorExecutor.shutdownNow();
        super.onDestroy();
    }

    /** Writes a supported governor to the active policy node for a single cluster. */
    public boolean setGovernor(String governor, CpuClusterHelper.Cluster cluster) {
        SysfsResult<String> result = AeroActivity.hardware.cpu().writeGovernor(cluster, governor);
        if (!result.isSuccess()) {
            Log.e("Aero", "Failed to write CPU governor: " + result.getError());
        }
        return result.isSuccess();
    }

    private boolean isConfiguredListValue(CustomListPreference preference, String candidate) {
        CharSequence[] entryValues = preference.getEntryValues();
        if (candidate == null || entryValues == null) {
            return false;
        }
        for (CharSequence entryValue : entryValues) {
            if (candidate.contentEquals(entryValue)) {
                return true;
            }
        }
        return false;
    }

    private void refreshCpuFrequencies() {
        final List<ClusterControls> controls = new ArrayList<>(this.mClusterControls);
        if (controls.isEmpty()) {
            return;
        }
        final String[] paths = getCpuFrequencyPaths(controls);
        final int lifecycleGeneration = this.mLifecycleGeneration.get();
        HardwareMetricsPoller.instance().readOnce(paths, new HardwareMetricsPoller.Listener() {
            @Override
            public void onMetrics(Map<String, String> values) {
                if (lifecycleGeneration == mLifecycleGeneration.get() && isAdded()) {
                    applyCpuFrequencies(controls, values);
                }
            }
        });
    }

    private String[] getCpuFrequencyPaths(List<ClusterControls> controls) {
        String[] paths = new String[controls.size() * 3];
        for (int i = 0; i < controls.size(); i++) {
            String[] clusterPaths = AeroActivity.hardware.cpu()
                    .getFrequencyMetricPaths(controls.get(i).cluster);
            System.arraycopy(clusterPaths, 0, paths, i * 3, clusterPaths.length);
        }
        return paths;
    }

    private String serializeIntegers(int[] values) {
        StringBuilder serialized = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) serialized.append(',');
            serialized.append(values[i]);
        }
        return serialized.toString();
    }

    private void startMetricsPolling() {
        if (!this.mWantsMetricsPolling || this.mMetricsSubscription != null || !isAdded()) {
            return;
        }
        final List<ClusterControls> controls = new ArrayList<>(this.mClusterControls);
        if (controls.isEmpty()) {
            return;
        }
        final String[] paths = getCpuFrequencyPaths(controls);
        final int lifecycleGeneration = this.mLifecycleGeneration.get();
        final HardwareMetricsPoller poller = HardwareMetricsPoller.instance();
        if (!this.mTableUpdate) {
            poller.executeOnWorker(new Runnable() {
                @Override
                public void run() {
                    SysfsResult<Boolean> result =
                            AeroActivity.hardware.legacyOverclock().initialize();
                    mTableUpdate = result.isSuccess();
                    if (!result.isSuccess()) {
                        Log.e("Aero", "Failed to initialize legacy overclock: "
                                + result.getError());
                    }
                }
            });
        }
        this.mMetricsSubscription = poller.observe(paths, new HardwareMetricsPoller.Listener() {
            @Override
            public void onMetrics(Map<String, String> values) {
                if (lifecycleGeneration == mLifecycleGeneration.get()
                        && mWantsMetricsPolling && isAdded()) {
                    applyCpuFrequencies(controls, values);
                }
            }
        });
    }

    private void stopMetricsPolling() {
        if (this.mMetricsSubscription != null) {
            this.mMetricsSubscription.cancel();
            this.mMetricsSubscription = null;
        }
    }

    private void applyCpuFrequencies(List<ClusterControls> controls,
            Map<String, String> values) {
        for (int i = 0; i < controls.size(); i++) {
            ClusterControls cluster = controls.get(i);
            String availablePath = getCpuFrequencyPathsForCluster(cluster)[0];
            String minPath = getCpuFrequencyPathsForCluster(cluster)[1];
            String maxPath = getCpuFrequencyPathsForCluster(cluster)[2];
            String[] entryValues = splitFrequencyValues(values.get(availablePath), false);
            String[] entries = splitFrequencyValues(values.get(availablePath), true);
            String minValue = firstFrequencyValue(values.get(minPath));
            String maxValue = firstFrequencyValue(values.get(maxPath));

            synchronized (this.mPreferenceLock) {
                cluster.minFrequency.setEntries(entries);
                cluster.minFrequency.setEntryValues(entryValues);
                cluster.maxFrequency.setEntries(entries);
                cluster.maxFrequency.setEntryValues(entryValues);
                if (minValue != null) {
                    cluster.minFrequency.setValue(minValue);
                    cluster.minFrequency.setSummary(FrequencyFormat.toMHz(minValue));
                    cluster.pendingMinFrequency = minValue;
                } else {
                    cluster.minFrequency.setValue(NO_DATA_FOUND);
                    cluster.minFrequency.setSummary(NO_DATA_FOUND);
                    cluster.pendingMinFrequency = null;
                }
                if (maxValue != null) {
                    cluster.maxFrequency.setValue(maxValue);
                    cluster.maxFrequency.setSummary(FrequencyFormat.toMHz(maxValue));
                    cluster.pendingMaxFrequency = maxValue;
                } else {
                    cluster.maxFrequency.setValue(NO_DATA_FOUND);
                    cluster.maxFrequency.setSummary(NO_DATA_FOUND);
                    cluster.pendingMaxFrequency = null;
                }
            }
        }
    }

    private String[] getCpuFrequencyPathsForCluster(ClusterControls controls) {
        return AeroActivity.hardware.cpu().getFrequencyMetricPaths(controls.cluster);
    }

    private String[] splitFrequencyValues(String value, boolean convertToMhz) {
        if (value == null || value.equals(NO_DATA_FOUND)) {
            return new String[]{NO_DATA_FOUND};
        }
        String[] rawValues = value.trim().split("\\s+");
        String[] parsedValues = new String[rawValues.length];
        for (int i = 0; i < rawValues.length; i++) {
            parsedValues[i] = convertToMhz
                    ? FrequencyFormat.toMHz(rawValues[i]) : rawValues[i];
        }
        return parsedValues;
    }

    private String firstFrequencyValue(String value) {
        if (value == null || value.equals(NO_DATA_FOUND)) {
            return null;
        }
        String[] values = value.trim().split("\\s+");
        if (values.length == 0 || values[0].length() == 0
                || values[0].equals(NO_DATA_FOUND)) {
            return null;
        }
        return values[0];
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view hierarchy
     * instantiated. Checks if this is the first time the fragment is shown and displays a
     * showcase tutorial highlighting the CPU governor settings if needed.
     *
     * @param savedInstanceState the saved instance state bundle
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_cpu_fragment_governor, R.string.showcase_cpu_fragment_governor_summary);
        }
    }

    /**
     * Displays the first-time tutorial showcase overlay for the CPU fragment.
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
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.CPUFragment.10
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                if (!CPUFragment.this.isAdded()) {
                    return null;
                }
                Activity activity = CPUFragment.this.getActivity();
                if (activity == null) {
                    return null;
                }
                int actionBarSize = 96;
                try {
                    int height = activity.findViewById(R.id.action_governor_settings).getHeight();
                    if (height > 0) {
                        actionBarSize = height;
                    }
                } catch (NullPointerException e) {
                }
                int x = activity.getResources().getDisplayMetrics().widthPixels - (actionBarSize / 2);
                int y = actionBarSize / 2;
                return new Point(x, y);
            }
        };
        this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

}
