package com.aero.control.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.aero.control.helpers.CpuController;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.ReadMode;
import com.aero.control.helpers.SysfsResult;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    private static final ArrayList<String> mVselList = new ArrayList<>();
    private final CpuClusterHelper mClusterHelper = new CpuClusterHelper();
    private final CpuController mCpuController = AeroActivity.hardware.cpu();
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
    private boolean mVisible = true;
    private RefreshThread mRefreshThread = new RefreshThread();
    private Handler mRefreshHandler = new Handler() { // from class: com.aero.control.fragments.CPUFragment.11
        boolean tableUpdate = false;

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what >= 1 && CPUFragment.this.isVisible() && CPUFragment.this.mVisible) {
                CPUFragment.this.updateMaxFreq();
                CPUFragment.this.updateMinFreq();
                if (!this.tableUpdate) {
                    this.tableUpdate = AeroActivity.shell.setOverclockAddress();
                }
                CPUFragment.this.mVisible = true;
            }
        }
    };

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

    /** A resolved online sysfs target and the value captured before an update starts. */
    private static final class CpuWriteTarget {
        private final ClusterControls controls;
        private final int cpu;
        private final String originalValue;

        private CpuWriteTarget(ClusterControls controls, int cpu, String originalValue) {
            this.controls = controls;
            this.cpu = cpu;
            this.originalValue = originalValue;
        }
    }

    /** Initializes the CPU controls and populates them from the detected cpufreq clusters. */
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
            SysfsResult<List<Integer>> governorTargets = this.mCpuController.getGovernorWriteTargets(cluster);
            int governorCpu = governorTargets.isSuccess() ? governorTargets.getValue().get(0) : -1;
            SysfsResult<String[]> governorValues = governorCpu >= 0
                    ? this.mCpuController.readAvailableGovernors(governorCpu)
                    : SysfsResult.<String[]>failure(governorTargets.getError());
            SysfsResult<String> currentGovernor = governorCpu >= 0
                    ? this.mCpuController.readGovernor(governorCpu)
                    : SysfsResult.<String>failure(governorTargets.getError());
            if (governorValues.isSuccess() && currentGovernor.isSuccess()) {
                governor.setEntries(governorValues.getValue());
                governor.setEntryValues(governorValues.getValue());
                governor.setValue(currentGovernor.getValue());
                governor.setSummary(currentGovernor.getValue());
            } else {
                governor.setSummary(NO_DATA_FOUND);
                governor.setEnabled(false);
                Log.e("Aero", "Unable to load CPU governor controls for cluster " + i);
            }
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
        updateMaxFreq();
        updateMinFreq();
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
        if (AeroActivity.shell.getInfo(FilePath.CPU_VSEL).equals(NO_DATA_FOUND)) {
            cpuCategory.removePreference(cpu_oc_uc);
        } else {
            cpu_oc_uc.setOrder(20);
        }
        cpu_oc_uc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.CPUFragment.4
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                String overclockOutput = AeroActivity.shell.getRootInfo("cat", FilePath.CPU_VSEL);
                CharSequence[] cpufreq = AeroActivity.shell.getInfoArray(FilePath.CPU_BASE_PATH + 0 + FilePath.CPU_AVAILABLE_FREQ_SUFFIX, ReadMode.RAW);
                AlertDialog.Builder builder = new AlertDialog.Builder(CPUFragment.this.getActivity());
                LayoutInflater inflater = CPUFragment.this.getActivity().getLayoutInflater();
                View layout = inflater.inflate(R.layout.cpu_oc_uc, (ViewGroup) null);
                final ViewGroup viewGroup = (ViewGroup) layout.findViewById(R.id.cpu_container);
                int i = 0;
                CPUFragment.mVselList.clear();
                int k = -1;
                while (true) {
                    k = overclockOutput.indexOf(" vsel=", k + 1);
                    if (k == -1) {
                        break;
                    }
                    CPUFragment.mVselList.add(overclockOutput.substring(k + 6, k + 8));
                }
                for (CharSequence a : cpufreq) {
                    for (int j = 0; j < 2; j++) {
                        CustomEditText cpuValues = new CustomEditText(CPUFragment.this.getActivity());
                        if (j != 0) {
                            cpuValues.setText(((String[]) CPUFragment.mVselList.toArray(new String[0]))[i]);
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
                        CPUFragment.mVselList.clear();
                        int listLength = newFrequencies.length;
                        int i3 = 0;
                        CPUFragment.mVselList.add("echo " + vselValues.get(0) + " > " + FilePath.CPU_VSEL_MAX);
                        for (Integer freq : newFrequencies) {
                            CPUFragment.mVselList.add("echo " + listLength + " " + freq + "000 " + vselValues.get(i3) + " > " + FilePath.CPU_VSEL);
                            CPUFragment.mVselList.add("echo " + i3 + " " + freq + " > " + FilePath.CPU_FREQ_TABLE);
                            Log.e("Aero", "echo " + listLength + " " + freq + "000 " + vselValues.get(i3) + " > " + FilePath.CPU_VSEL);
                            listLength--;
                            i3++;
                        }
                        CPUFragment.mVselList.add("echo " + newFrequencies[0] + " > " + FilePath.CPU_MAX_RATE);
                        CPUFragment.mVselList.add("echo " + newFrequencies[newFrequencies.length - 1] + " > " + FilePath.CPU_BASE_PATH + 0 + FilePath.CPU_MIN_FREQ);
                        String[] commands = (String[]) CPUFragment.mVselList.toArray(new String[0]);
                        AeroActivity.shell.setRootInfo(commands);
                        SharedPreferences preference2 = PreferenceManager.getDefaultSharedPreferences(CPUFragment.this.getActivity().getBaseContext());
                        preference2.edit().putStringSet("cpu_commands", new HashSet(Arrays.asList(commands))).commit();
                        try {
                            if (!CPUFragment.this.mRefreshThread.isAlive()) {
                                CPUFragment.this.mRefreshThread.start();
                                CPUFragment.this.mRefreshThread.setPriority(1);
                            }
                        } catch (NullPointerException e2) {
                            Log.e("Aero", "Couldn't start Refresher Thread.", e2);
                        }
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

    /** Connects the maximum and minimum frequency controls for a cluster to their sysfs writes. */
    private void attachFrequencyListeners(final ClusterControls controls) {
        controls.maxFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.6
            /** {@inheritDoc} */
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
                        /** {@inheritDoc} */
                        @Override
                        public void run() {
                            ArrayList<ClusterControls> targets = new ArrayList<>();
                            targets.add(controls);
                            CPUFragment.this.applyFrequencyUpdate(targets, a, true);
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                                        return;
                                    }
                                    CPUFragment.this.updateMaxFreq();
                                }
                            });
                        }
                    });
                    return false;
                }
            }
        });
        controls.minFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.7
            /** {@inheritDoc} */
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
                        /** {@inheritDoc} */
                        @Override
                        public void run() {
                            ArrayList<ClusterControls> targets = new ArrayList<>();
                            targets.add(controls);
                            CPUFragment.this.applyFrequencyUpdate(targets, a, false);
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                                        return;
                                    }
                                    CPUFragment.this.updateMinFreq();
                                }
                            });
                        }
                    });
                    return false;
                }
            }
        });
    }

    /** Connects a cluster's governor control to its serialized sysfs update. */
    private void attachGovernorListener(final ClusterControls controls) {
        controls.governor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.5
            /** {@inheritDoc} */
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
                        /** {@inheritDoc} */
                        @Override
                        public void run() {
                            ArrayList<ClusterControls> targets = new ArrayList<>();
                            targets.add(controls);
                            CPUFragment.this.applyGovernorUpdate(targets, a);
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                                        return;
                                    }
                                    CPUFragment.this.updateGovernorControls();
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
            controls.maxFrequency.setEnabled(!applyToAll && hasConfiguredValues(controls.maxFrequency));
            controls.minFrequency.setEnabled(!applyToAll && hasConfiguredValues(controls.minFrequency));
            controls.governor.setEnabled(!applyToAll && hasConfiguredValues(controls.governor));
        }
    }

    /** Returns whether a list preference has at least one selectable value. */
    private boolean hasConfiguredValues(CustomListPreference preference) {
        CharSequence[] values = preference.getEntryValues();
        return values != null && values.length > 0;
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
            Log.e("Aero", (isMax ? "Max" : "Min") + " frequency " + value
                    + " not supported by all clusters; rejecting request");
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
                            Log.e("Aero", "Max frequency " + value + " is lower than min frequency "
                                    + oppositeStr + " for cluster " + target.index + "; rejecting request");
                        } else {
                            Log.e("Aero", "Min frequency " + value + " is higher than max frequency "
                                    + oppositeStr + " for cluster " + target.index + "; rejecting request");
                        }
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                Log.e("Aero", "Invalid frequency format; rejecting request", e);
                return;
            }

            eligibleClusters.add(target);
        }

        if (eligibleClusters.isEmpty()) {
            Log.e("Aero", "No valid clusters to apply " + (isMax ? "max" : "min")
                    + " frequency; rejecting request");
            return;
        }

        boolean writeSucceeded = applyFrequencyUpdate(eligibleClusters, value, isMax);
        if (writeSucceeded) {
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
                    if (isMax) {
                        updateMaxFreq();
                    } else {
                        updateMinFreq();
                    }
                }
            });
        } else {
            Log.e("Aero", "Failed to apply " + (isMax ? "max" : "min")
                    + " frequency to all clusters; final state will be refreshed from sysfs");
            AeroActivity.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (lifecycleGeneration != mLifecycleGeneration.get()) {
                        return;
                    }
                    if (isMax) {
                        updateMaxFreq();
                    } else {
                        updateMinFreq();
                    }
                }
            });
        }
    }

    /** Validates and asynchronously applies a governor supported by every detected cluster. */
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
            /** {@inheritDoc} */
            @Override
            public void run() {
                CPUFragment.this.applyGovernorUpdate(
                        new ArrayList<>(CPUFragment.this.mClusterControls), value);

                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (lifecycleGeneration != mLifecycleGeneration.get()) {
                            return;
                        }
                        if (requestGeneration != mGovernorMirrorGeneration.get()) {
                            return;
                        }
                        CPUFragment.this.updateGovernorControls();
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
        try {
            String[] completeParamterList = AeroActivity.shell.getDirInfo(complete_path, true);
            if (this.PrefCat != null) {
                this.root.removePreference(this.PrefCat);
            }
            if (completeParamterList.length == 0) {
                Toast.makeText(getActivity(), R.string.pref_gov_set_no_parameter, 1).show();
                return;
            }
            this.PrefCat = new PreferenceCategory(getActivity());
            this.PrefCat.setTitle(R.string.pref_gov_set);
            this.root.addPreference(this.PrefCat);
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Log.e("Aero", "Something interrupted the main Thread, try again.", e);
            }
            PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
            h.genPrefFromDictionary(completeParamterList, complete_path);
        } catch (NullPointerException e2) {
            Toast.makeText(getActivity(), R.string.pref_gov_set_no_parameter, 1).show();
            Log.e("Aero", "There isn't any folder i can check. Does this governor has parameters?", e2);
        }
    }

    /**
     * Pauses UI updates when the fragment becomes invisible and removes any displayed
     * governor parameter preference category.
     */
    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mVisible = false;
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
        this.mVisible = true;
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
        this.mMirrorExecutor.shutdownNow();
        super.onDestroy();
    }

    /** Applies a frequency transaction after resolving and snapshotting every online target. */
    private boolean applyFrequencyUpdate(
            List<ClusterControls> clusters, String value, boolean isMax) {
        ArrayList<CpuWriteTarget> snapshots = new ArrayList<>();
        for (ClusterControls controls : clusters) {
            SysfsResult<List<Integer>> targetResult = this.mCpuController
                    .getFrequencyWriteTargets(controls.cluster, isMax);
            if (!targetResult.isSuccess()) {
                Log.e("Aero", targetResult.getError());
                return false;
            }
            for (Integer cpu : targetResult.getValue()) {
                SysfsResult<String> currentResult = isMax
                        ? this.mCpuController.readMaxFrequency(cpu)
                        : this.mCpuController.readMinFrequency(cpu);
                if (!currentResult.isSuccess()) {
                    Log.e("Aero", "Unable to snapshot " + (isMax ? "max" : "min")
                            + " frequency for CPU " + cpu + ": " + currentResult.getError());
                    return false;
                }
                snapshots.add(new CpuWriteTarget(controls, cpu, currentResult.getValue()));
            }
        }

        ArrayList<CpuWriteTarget> changed = new ArrayList<>();
        for (CpuWriteTarget target : snapshots) {
            SysfsResult<String> writeResult = isMax
                    ? this.mCpuController.writeMaxFrequency(target.cpu, value)
                    : this.mCpuController.writeMinFrequency(target.cpu, value);
            if (!writeResult.isSuccess() || !value.equals(writeResult.getValue())) {
                String error = writeResult.isSuccess()
                        ? "Unexpected frequency readback for CPU " + target.cpu
                        : writeResult.getError();
                Log.e("Aero", "CPU frequency update failed: " + error);
                changed.add(target);
                rollbackFrequencyTargets(changed, isMax);
                captureFinalFrequencyState(snapshots, isMax);
                return false;
            }
            changed.add(target);
        }
        captureFinalFrequencyState(snapshots, isMax);
        return true;
    }

    private void rollbackFrequencyTargets(List<CpuWriteTarget> changed, boolean isMax) {
        for (int i = changed.size() - 1; i >= 0; i--) {
            CpuWriteTarget target = changed.get(i);
            SysfsResult<String> rollbackResult = isMax
                    ? this.mCpuController.writeMaxFrequency(target.cpu, target.originalValue)
                    : this.mCpuController.writeMinFrequency(target.cpu, target.originalValue);
            if (!rollbackResult.isSuccess()) {
                Log.e("Aero", "Failed to restore " + (isMax ? "max" : "min")
                        + " frequency for CPU " + target.cpu + ": " + rollbackResult.getError());
            }
        }
    }

    /** Updates executor-visible state from final sysfs values before another queued request runs. */
    private void captureFinalFrequencyState(List<CpuWriteTarget> targets, boolean isMax) {
        for (CpuWriteTarget target : targets) {
            SysfsResult<String> finalResult = isMax
                    ? this.mCpuController.readMaxFrequency(target.cpu)
                    : this.mCpuController.readMinFrequency(target.cpu);
            if (!finalResult.isSuccess()) {
                Log.e("Aero", "Unable to read final " + (isMax ? "max" : "min")
                        + " frequency for CPU " + target.cpu + ": " + finalResult.getError());
                continue;
            }
            synchronized (mPreferenceLock) {
                if (isMax) {
                    target.controls.pendingMaxFrequency = finalResult.getValue();
                } else {
                    target.controls.pendingMinFrequency = finalResult.getValue();
                }
            }
        }
    }

    /** Applies a governor transaction after resolving and snapshotting every online target. */
    private boolean applyGovernorUpdate(List<ClusterControls> clusters, String value) {
        ArrayList<CpuWriteTarget> snapshots = new ArrayList<>();
        for (ClusterControls controls : clusters) {
            SysfsResult<List<Integer>> targetResult = this.mCpuController
                    .getGovernorWriteTargets(controls.cluster);
            if (!targetResult.isSuccess()) {
                Log.e("Aero", targetResult.getError());
                return false;
            }
            for (Integer cpu : targetResult.getValue()) {
                SysfsResult<String> currentResult = this.mCpuController.readGovernor(cpu);
                if (!currentResult.isSuccess()) {
                    Log.e("Aero", "Unable to snapshot governor for CPU " + cpu + ": "
                            + currentResult.getError());
                    return false;
                }
                snapshots.add(new CpuWriteTarget(controls, cpu, currentResult.getValue()));
            }
        }

        ArrayList<CpuWriteTarget> changed = new ArrayList<>();
        for (CpuWriteTarget target : snapshots) {
            SysfsResult<String> writeResult = this.mCpuController.writeGovernor(target.cpu, value);
            if (!writeResult.isSuccess() || !value.equals(writeResult.getValue())) {
                String error = writeResult.isSuccess()
                        ? "Unexpected governor readback for CPU " + target.cpu
                        : writeResult.getError();
                Log.e("Aero", "CPU governor update failed: " + error);
                changed.add(target);
                rollbackGovernorTargets(changed);
                return false;
            }
            changed.add(target);
        }
        return true;
    }

    private void rollbackGovernorTargets(List<CpuWriteTarget> changed) {
        for (int i = changed.size() - 1; i >= 0; i--) {
            CpuWriteTarget target = changed.get(i);
            SysfsResult<String> rollbackResult = this.mCpuController.writeGovernor(
                    target.cpu, target.originalValue);
            if (!rollbackResult.isSuccess()) {
                Log.e("Aero", "Failed to restore governor for CPU " + target.cpu + ": "
                        + rollbackResult.getError());
            }
        }
    }

    /** Returns whether a candidate exactly matches one of a list preference's configured values. */
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

    /** Returns the display summary paired with a raw frequency value, or an unavailable marker. */
    private String findFrequencySummary(String currentValue, String[] values, String[] summaries) {
        for (int i = 0; i < values.length && i < summaries.length; i++) {
            if (currentValue.equals(values[i])) {
                return summaries[i];
            }
        }
        return NO_DATA_FOUND;
    }

    /**
     * Updates the minimum frequency preference UI for all CPU clusters by reading
     * current values from sysfs and refreshing the preference controls.
     */
    public void updateMinFreq() {
        // Read sysfs values outside lock to minimize critical section
        for (ClusterControls controls : this.mClusterControls) {
            SysfsResult<List<Integer>> targets = this.mCpuController
                    .getFrequencyWriteTargets(controls.cluster, false);
            int targetCpu = targets.isSuccess() ? targets.getValue().get(0) : -1;
            SysfsResult<String[]> entriesResult = targetCpu >= 0
                    ? this.mCpuController.readAvailableFrequencies(targetCpu, ReadMode.FREQUENCY_MHZ)
                    : SysfsResult.<String[]>failure(targets.getError());
            SysfsResult<String[]> entryValuesResult = targetCpu >= 0
                    ? this.mCpuController.readAvailableFrequencies(targetCpu, ReadMode.RAW)
                    : SysfsResult.<String[]>failure(targets.getError());
            SysfsResult<String> currentResult = targetCpu >= 0
                    ? this.mCpuController.readMinFrequency(targetCpu)
                    : SysfsResult.<String>failure(targets.getError());

            // Apply updates under lock
            synchronized (mPreferenceLock) {
                if (entriesResult.isSuccess() && entryValuesResult.isSuccess() && currentResult.isSuccess()) {
                    String currentValue = currentResult.getValue();
                    controls.minFrequency.setEntries(entriesResult.getValue());
                    controls.minFrequency.setEntryValues(entryValuesResult.getValue());
                    controls.minFrequency.setValue(currentValue);
                    controls.minFrequency.setSummary(findFrequencySummary(currentValue,
                            entryValuesResult.getValue(), entriesResult.getValue()));
                    controls.minFrequency.setEnabled(controls.index == 0 || !isApplyToAllClustersEnabled());
                    controls.pendingMinFrequency = currentValue;
                } else {
                    controls.minFrequency.setEntries(new String[0]);
                    controls.minFrequency.setEntryValues(new String[0]);
                    controls.minFrequency.setValue(NO_DATA_FOUND);
                    controls.minFrequency.setSummary(NO_DATA_FOUND);
                    controls.minFrequency.setEnabled(false);
                    controls.pendingMinFrequency = null;
                    Log.e("Aero", "Unable to load minimum CPU frequency for cluster "
                            + controls.cluster.getMemberRangeLabel());
                }
            }
        }
    }

    /**
     * Updates the maximum frequency preference UI for all CPU clusters by reading
     * current values from sysfs and refreshing the preference controls.
     */
    public void updateMaxFreq() {
        // Read sysfs values outside lock to minimize critical section
        for (ClusterControls controls : this.mClusterControls) {
            SysfsResult<List<Integer>> targets = this.mCpuController
                    .getFrequencyWriteTargets(controls.cluster, true);
            int targetCpu = targets.isSuccess() ? targets.getValue().get(0) : -1;
            SysfsResult<String[]> entriesResult = targetCpu >= 0
                    ? this.mCpuController.readAvailableFrequencies(targetCpu, ReadMode.FREQUENCY_MHZ)
                    : SysfsResult.<String[]>failure(targets.getError());
            SysfsResult<String[]> entryValuesResult = targetCpu >= 0
                    ? this.mCpuController.readAvailableFrequencies(targetCpu, ReadMode.RAW)
                    : SysfsResult.<String[]>failure(targets.getError());
            SysfsResult<String> currentResult = targetCpu >= 0
                    ? this.mCpuController.readMaxFrequency(targetCpu)
                    : SysfsResult.<String>failure(targets.getError());

            // Apply updates under lock
            synchronized (mPreferenceLock) {
                if (entriesResult.isSuccess() && entryValuesResult.isSuccess() && currentResult.isSuccess()) {
                    String currentValue = currentResult.getValue();
                    controls.maxFrequency.setEntries(entriesResult.getValue());
                    controls.maxFrequency.setEntryValues(entryValuesResult.getValue());
                    controls.maxFrequency.setValue(currentValue);
                    controls.maxFrequency.setSummary(findFrequencySummary(currentValue,
                            entryValuesResult.getValue(), entriesResult.getValue()));
                    controls.maxFrequency.setEnabled(controls.index == 0 || !isApplyToAllClustersEnabled());
                    controls.pendingMaxFrequency = currentValue;
                } else {
                    controls.maxFrequency.setEntries(new String[0]);
                    controls.maxFrequency.setEntryValues(new String[0]);
                    controls.maxFrequency.setValue(NO_DATA_FOUND);
                    controls.maxFrequency.setSummary(NO_DATA_FOUND);
                    controls.maxFrequency.setEnabled(false);
                    controls.pendingMaxFrequency = null;
                    Log.e("Aero", "Unable to load maximum CPU frequency for cluster "
                            + controls.cluster.getMemberRangeLabel());
                }
            }
        }
    }

    /** Refreshes every governor control from the final observed sysfs state. */
    private void updateGovernorControls() {
        for (ClusterControls controls : this.mClusterControls) {
            SysfsResult<List<Integer>> targets = this.mCpuController
                    .getGovernorWriteTargets(controls.cluster);
            int targetCpu = targets.isSuccess() ? targets.getValue().get(0) : -1;
            SysfsResult<String[]> valuesResult = targetCpu >= 0
                    ? this.mCpuController.readAvailableGovernors(targetCpu)
                    : SysfsResult.<String[]>failure(targets.getError());
            SysfsResult<String> currentResult = targetCpu >= 0
                    ? this.mCpuController.readGovernor(targetCpu)
                    : SysfsResult.<String>failure(targets.getError());
            synchronized (mPreferenceLock) {
                if (valuesResult.isSuccess() && currentResult.isSuccess()) {
                    controls.governor.setEntries(valuesResult.getValue());
                    controls.governor.setEntryValues(valuesResult.getValue());
                    controls.governor.setValue(currentResult.getValue());
                    controls.governor.setSummary(currentResult.getValue());
                    controls.governor.setEnabled(
                            controls.index == 0 || !isApplyToAllClustersEnabled());
                } else {
                    controls.governor.setEntries(new String[0]);
                    controls.governor.setEntryValues(new String[0]);
                    controls.governor.setSummary(NO_DATA_FOUND);
                    controls.governor.setEnabled(false);
                    Log.e("Aero", "Unable to load governor for cluster "
                            + controls.cluster.getMemberRangeLabel());
                }
            }
        }
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

    private class RefreshThread extends Thread {
        private boolean mInterrupt;

        private RefreshThread() {
            this.mInterrupt = false;
        }

        @Override // java.lang.Thread
        public void interrupt() {
            this.mInterrupt = true;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (!this.mInterrupt) {
                try {
                    sleep(1000L);
                    CPUFragment.this.mRefreshHandler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
