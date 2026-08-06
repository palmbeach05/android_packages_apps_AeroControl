package com.aero.control.fragments;

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
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
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
    private final List<ClusterControls> mClusterControls = new ArrayList<>();
    private CheckBoxPreference mApplyToAllClusters;
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
            String representativeGovPath = FilePath.CPU_BASE_PATH + cluster.getRepresentativeCpu() + FilePath.CPU_AVAILABLE_GOV_SUFFIX;
            governor.setEntries(AeroActivity.shell.getInfoArray(representativeGovPath, 0, 0));
            governor.setEntryValues(AeroActivity.shell.getInfoArray(representativeGovPath, 0, 0));
            String currentGovernor = AeroActivity.shell.getInfo(FilePath.CPU_BASE_PATH + cluster.getRepresentativeCpu() + FilePath.CURRENT_GOV_AVAILABLE);
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
                    AeroActivity.mHandler.postDelayed(new Runnable() { // from class: com.aero.control.fragments.CPUFragment.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            CPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, CPUFragment.this.mHotplugFragment).addToBackStack("Hotplug").commit();
                        }
                    }, AeroActivity.genHelper.getDefaultDelay());
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
                    AeroActivity.mHandler.postDelayed(new Runnable() { // from class: com.aero.control.fragments.CPUFragment.2.1
                        @Override // java.lang.Runnable
                        public void run() {
                            CPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, CPUFragment.this.mVoltageFragment).addToBackStack("Voltage").commit();
                        }
                    }, AeroActivity.genHelper.getDefaultDelay());
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
                    AeroActivity.mHandler.postDelayed(new Runnable() { // from class: com.aero.control.fragments.CPUFragment.3.1
                        @Override // java.lang.Runnable
                        public void run() {
                            CPUFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, CPUFragment.this.mCPUBoostFragment).addToBackStack("CPUBoost").commit();
                        }
                    }, AeroActivity.genHelper.getDefaultDelay());
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
                CharSequence[] cpufreq = AeroActivity.shell.getInfoArray(FilePath.CPU_BASE_PATH + 0 + FilePath.CPU_AVAILABLE_FREQ_SUFFIX, 0, 0);
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

    private void attachFrequencyListeners(final ClusterControls controls) {
        controls.maxFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.6
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String a = (String) o;
                try {
                    if (Integer.parseInt(a) < Integer.parseInt(controls.minFrequency.getValue())) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
                if (controls.index == 0 && CPUFragment.this.isApplyToAllClustersEnabled()) {
                    return CPUFragment.this.applyMaxFrequencyToAllClusters(controls, a);
                } else {
                    ArrayList<String> array = new ArrayList<>();
                    for (Integer cpu : controls.cluster.getMembers()) {
                        array.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                        array.add("echo " + a + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MAX_FREQ);
                    }
                    controls.maxFrequency.setSummary(AeroActivity.shell.toMHz(a));
                    String[] commands = (String[]) array.toArray(new String[0]);
                    AeroActivity.shell.setRootInfo(commands);
                }
                return true;
            }
        });
        controls.minFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.7
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String a = (String) o;
                try {
                    if (Integer.parseInt(a) > Integer.parseInt(controls.maxFrequency.getValue())) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
                if (controls.index == 0 && CPUFragment.this.isApplyToAllClustersEnabled()) {
                    return CPUFragment.this.applyMinFrequencyToAllClusters(controls, a);
                } else {
                    ArrayList<String> array = new ArrayList<>();
                    for (Integer cpu : controls.cluster.getMembers()) {
                        array.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                        array.add("echo " + a + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CPU_MIN_FREQ);
                    }
                    controls.minFrequency.setSummary(AeroActivity.shell.toMHz(a));
                    String[] commands = (String[]) array.toArray(new String[0]);
                    AeroActivity.shell.setRootInfo(commands);
                }
                return true;
            }
        });
    }

    private void attachGovernorListener(final ClusterControls controls) {
        controls.governor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.5
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String a = (String) o;
                if (CPUFragment.this.PrefCat != null) {
                    CPUFragment.this.root.removePreference(CPUFragment.this.PrefCat);
                }
                if (controls.index == 0 && CPUFragment.this.isApplyToAllClustersEnabled()) {
                    return CPUFragment.this.applyGovernorToAllClusters(controls, a);
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            CPUFragment.this.setGovernor(a, controls.cluster.getMembers());
                            // Poll for governor change completion (max 10 attempts * 50ms = 500ms)
                            final String governorPath = FilePath.CPU_BASE_PATH + controls.cluster.getRepresentativeCpu() + FilePath.CURRENT_GOV_AVAILABLE;
                            String currentGovernor = null;
                            for (int i = 0; i < 10; i++) {
                                try {
                                    Thread.sleep(50L);
                                } catch (InterruptedException e) {
                                    Log.e("Aero", "Governor polling interrupted", e);
                                    break;
                                }
                                currentGovernor = AeroActivity.shell.getInfo(governorPath);
                                if (a.equals(currentGovernor)) {
                                    break;
                                }
                            }
                            final String finalGovernor = (currentGovernor != null && !NO_DATA_FOUND.equals(currentGovernor)) ? currentGovernor : a;
                            AeroActivity.mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    controls.governor.setSummary(finalGovernor);
                                    controls.governor.setValue(finalGovernor);
                                }
                            });
                        }
                    }).start();
                }
                return true;
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
        applyFrequencyToAllClusters(source, value, true);
        return false;
    }

    private boolean applyMinFrequencyToAllClusters(ClusterControls source, String value) {
        applyFrequencyToAllClusters(source, value, false);
        return false;
    }

    /**
     * Mirrors a max- or min-frequency change from the first cluster's controls to every detected
     * cluster. Validates the value against each cluster's supported frequencies and its opposite
     * frequency limit, then runs the root commands on a worker thread and posts the resulting
     * preference updates back to the UI thread for the clusters that were actually written.
     */
    private void applyFrequencyToAllClusters(final ClusterControls source, final String value, final boolean isMax) {
        // Build intersection of supported frequencies across all clusters
        HashSet<String> supportedFreqs = null;
        for (ClusterControls controls : this.mClusterControls) {
            CharSequence[] entries = (isMax ? controls.maxFrequency : controls.minFrequency).getEntryValues();
            if (entries != null) {
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
            Log.e("Aero", (isMax ? "Max" : "Min") + " frequency " + value + " not supported by all clusters");
            return;
        }

        final ArrayList<String> array = new ArrayList<>();
        final ArrayList<ClusterControls> eligibleClusters = new ArrayList<>();
        for (ClusterControls target : this.mClusterControls) {
            // Validate against the opposite frequency limit
            try {
                String oppositeStr = (isMax ? target.minFrequency : target.maxFrequency).getValue();
                if (oppositeStr != null && !oppositeStr.equals(NO_DATA_FOUND)) {
                    int opposite = Integer.parseInt(oppositeStr);
                    int requested = Integer.parseInt(value);
                    if (isMax ? requested < opposite : requested > opposite) {
                        if (isMax) {
                            Log.e("Aero", "Max frequency " + value + " is lower than min frequency " + oppositeStr + " for cluster " + target.index);
                        } else {
                            Log.e("Aero", "Min frequency " + value + " is higher than max frequency " + oppositeStr + " for cluster " + target.index);
                        }
                        continue;
                    }
                }
            } catch (NumberFormatException e) {
                Log.e("Aero", "Invalid frequency format", e);
                continue;
            }

            eligibleClusters.add(target);
            String sysfsSuffix = isMax ? FilePath.CPU_MAX_FREQ : FilePath.CPU_MIN_FREQ;
            for (Integer cpu : target.cluster.getMembers()) {
                array.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
                array.add("echo " + value + " > " + FilePath.CPU_BASE_PATH + cpu + sysfsSuffix);
            }
        }

        if (array.isEmpty()) {
            Log.e("Aero", "No valid clusters to apply " + (isMax ? "max" : "min") + " frequency");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] commands = array.toArray(new String[0]);
                if (AeroActivity.shell.setRootInfo(commands)) {
                    AeroActivity.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (ClusterControls target : eligibleClusters) {
                                CustomListPreference pref = isMax ? target.maxFrequency : target.minFrequency;
                                pref.setSummary(AeroActivity.shell.toMHz(value));
                                pref.setValue(value);
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private boolean applyGovernorToAllClusters(final ClusterControls source, final String value) {
        // Build intersection of supported governors across all clusters
        HashSet<String> supportedGovs = null;
        for (ClusterControls controls : this.mClusterControls) {
            CharSequence[] entries = controls.governor.getEntryValues();
            if (entries != null) {
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (ClusterControls target : CPUFragment.this.mClusterControls) {
                    setGovernor(value, target.cluster.getMembers());
                }

                // Poll for governor change completion on all clusters
                final String[] capturedGovernors = new String[CPUFragment.this.mClusterControls.size()];
                boolean allComplete = false;
                for (int i = 0; i < 10 && !allComplete; i++) {
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException e) {
                        Log.e("Aero", "Governor polling interrupted", e);
                        break;
                    }
                    allComplete = true;
                    for (int j = 0; j < CPUFragment.this.mClusterControls.size(); j++) {
                        ClusterControls target = CPUFragment.this.mClusterControls.get(j);
                        String governorPath = FilePath.CPU_BASE_PATH + target.cluster.getRepresentativeCpu() + FilePath.CURRENT_GOV_AVAILABLE;
                        String currentGovernor = AeroActivity.shell.getInfo(governorPath);
                        capturedGovernors[j] = currentGovernor;
                        if (!value.equals(currentGovernor)) {
                            allComplete = false;
                        }
                    }
                }

                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int j = 0; j < CPUFragment.this.mClusterControls.size(); j++) {
                            ClusterControls target = CPUFragment.this.mClusterControls.get(j);
                            String currentGovernor = capturedGovernors[j];
                            if (currentGovernor != null && !NO_DATA_FOUND.equals(currentGovernor)) {
                                target.governor.setSummary(currentGovernor);
                                target.governor.setValue(currentGovernor);
                            }
                        }
                    }
                });
            }
        }).start();
        return false;
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

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

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mVisible = false;
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
    }

    @Override // com.aero.control.fragments.PlaceHolderFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mVisible = true;
    }

    /** Writes the governor only to the CPUs belonging to a single cluster. */
    public void setGovernor(String s, List<Integer> members) {
        ArrayList<String> array = new ArrayList<>();
        for (Integer cpu : members) {
            array.add("echo 1 > " + FilePath.CPU_BASE_PATH + cpu + "/online");
            array.add("echo " + s + " > " + FilePath.CPU_BASE_PATH + cpu + FilePath.CURRENT_GOV_AVAILABLE);
        }
        String[] commands = (String[]) array.toArray(new String[0]);
        AeroActivity.shell.setRootInfo(commands);
    }

    public void updateMinFreq() {
        for (ClusterControls controls : this.mClusterControls) {
            int representativeCpu = controls.cluster.getRepresentativeCpu();
            String availablePath = FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CPU_AVAILABLE_FREQ_SUFFIX;
            controls.minFrequency.setEntries(AeroActivity.shell.getInfoArray(availablePath, 1, 0));
            controls.minFrequency.setEntryValues(AeroActivity.shell.getInfoArray(availablePath, 0, 0));
            String currentPath = FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CPU_MIN_FREQ;
            try {
                controls.minFrequency.setValue(AeroActivity.shell.getInfoArray(currentPath, 0, 0)[0]);
                controls.minFrequency.setSummary(AeroActivity.shell.getInfoArray(currentPath, 1, 0)[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                controls.minFrequency.setValue(NO_DATA_FOUND);
                controls.minFrequency.setSummary(NO_DATA_FOUND);
            }
        }
    }

    public void updateMaxFreq() {
        for (ClusterControls controls : this.mClusterControls) {
            int representativeCpu = controls.cluster.getRepresentativeCpu();
            String availablePath = FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CPU_AVAILABLE_FREQ_SUFFIX;
            controls.maxFrequency.setEntries(AeroActivity.shell.getInfoArray(availablePath, 1, 0));
            controls.maxFrequency.setEntryValues(AeroActivity.shell.getInfoArray(availablePath, 0, 0));
            String currentPath = FilePath.CPU_BASE_PATH + representativeCpu + FilePath.CPU_MAX_FREQ;
            try {
                controls.maxFrequency.setValue(AeroActivity.shell.getInfoArray(currentPath, 0, 0)[0]);
                controls.maxFrequency.setSummary(AeroActivity.shell.getInfoArray(currentPath, 1, 0)[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                controls.maxFrequency.setValue(NO_DATA_FOUND);
                controls.maxFrequency.setSummary(NO_DATA_FOUND);
            }
        }
    }

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
                int actionBarSize = 96;
                try {
                    int height = CPUFragment.this.getActivity().findViewById(R.id.action_governor_settings).getHeight();
                    if (height > 0) {
                        actionBarSize = height;
                    }
                } catch (NullPointerException e) {
                }
                int x = CPUFragment.this.getResources().getDisplayMetrics().widthPixels - (actionBarSize / 2);
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
