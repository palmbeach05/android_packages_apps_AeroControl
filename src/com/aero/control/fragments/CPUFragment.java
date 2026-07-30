package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/* JADX INFO: loaded from: classes.dex */
public class CPUFragment extends PlaceHolderFragment {
    private static final String FILENAME = "firstrun_cpu";
    private static final String NO_DATA_FOUND = "Unavailable";
    private PreferenceCategory PrefCat;
    private CustomListPreference mBIGMaxFrequency;
    private CustomListPreference mBIGMinFrequency;
    private CPUBoostFragment mCPUBoostFragment;
    private CustomListPreference mCPUGovernor;
    private CPUHotplugFragment mHotplugFragment;
    private String mHotplugPath;
    private CustomListPreference mMaxFrequency;
    private CustomListPreference mMinFrequency;
    private ShowcaseView mShowCase;
    private VoltageFragment mVoltageFragment;
    private PreferenceScreen root;
    private static final ArrayList<String> mVselList = new ArrayList<>();
    private static final int mNumCpus = Runtime.getRuntime().availableProcessors();
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

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.layout.cpu_fragment);
        this.root = getPreferenceScreen();
        PreferenceCategory cpuCategory = (PreferenceCategory) findPreference("cpu_settings");
        PreferenceCategory cpuGovernor = (PreferenceCategory) findPreference("cpu_governor");
        this.mMaxFrequency = new CustomListPreference(getActivity());
        this.mMaxFrequency.setName("max_frequency");
        this.mMaxFrequency.setTitle(R.string.pref_cpu_freqmax);
        this.mMaxFrequency.setDialogTitle(R.string.pref_cpu_freqmax);
        this.mMaxFrequency.setSummary(R.string.pref_cpu_freqmax);
        this.mMaxFrequency.setDialogIcon(R.drawable.lightning);
        this.mMaxFrequency.setOrder(0);
        cpuCategory.addPreference(this.mMaxFrequency);
        this.mMinFrequency = new CustomListPreference(getActivity());
        this.mMinFrequency.setName("min_frequency");
        this.mMinFrequency.setTitle(R.string.pref_cpu_freqmin);
        this.mMinFrequency.setDialogTitle(R.string.pref_cpu_freqmin);
        this.mMinFrequency.setSummary(R.string.pref_cpu_freqmin);
        this.mMinFrequency.setDialogIcon(R.drawable.lightning);
        this.mMinFrequency.setOrder(5);
        cpuCategory.addPreference(this.mMinFrequency);
        if (mNumCpus > 4) {
            this.mBIGMaxFrequency = new CustomListPreference(getActivity());
            this.mBIGMaxFrequency.setName("big_max_frequency");
            this.mBIGMaxFrequency.setTitle(R.string.pref_big_cpu_freqmax);
            this.mBIGMaxFrequency.setDialogTitle(R.string.pref_big_cpu_freqmax);
            this.mBIGMaxFrequency.setSummary(R.string.pref_big_cpu_freqmax);
            this.mBIGMaxFrequency.setDialogIcon(R.drawable.lightning);
            this.mBIGMaxFrequency.setOrder(1);
            cpuCategory.addPreference(this.mBIGMaxFrequency);
            this.mBIGMinFrequency = new CustomListPreference(getActivity());
            this.mBIGMinFrequency.setName("big_min_frequency");
            this.mBIGMinFrequency.setTitle(R.string.pref_big_cpu_freqmin);
            this.mBIGMinFrequency.setDialogTitle(R.string.pref_big_cpu_freqmin);
            this.mBIGMinFrequency.setSummary(R.string.pref_big_cpu_freqmin);
            this.mBIGMinFrequency.setDialogIcon(R.drawable.lightning);
            this.mBIGMinFrequency.setOrder(6);
            cpuCategory.addPreference(this.mBIGMinFrequency);
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
                CharSequence[] cpufreq = AeroActivity.shell.getInfoArray(FilePath.CPU_AVAILABLE_FREQ, 0, 0);
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
        this.mCPUGovernor = new CustomListPreference(getActivity());
        this.mCPUGovernor.setName("set_governor");
        this.mCPUGovernor.setTitle(R.string.pref_cpu_governor);
        this.mCPUGovernor.setDialogTitle(R.string.pref_cpu_governor);
        this.mCPUGovernor.setEntries(AeroActivity.shell.getInfoArray(FilePath.ALL_GOV_AVAILABLE, 0, 0));
        this.mCPUGovernor.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.ALL_GOV_AVAILABLE, 0, 0));
        this.mCPUGovernor.setValue(AeroActivity.shell.getInfo(FilePath.GOV_FILE));
        this.mCPUGovernor.setSummary(AeroActivity.shell.getInfo(FilePath.GOV_FILE));
        this.mCPUGovernor.setDialogIcon(R.drawable.cpu);
        cpuGovernor.addPreference(this.mCPUGovernor);
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.mCPUGovernor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.5
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String a = (String) o;
                if (CPUFragment.this.PrefCat != null) {
                    CPUFragment.this.root.removePreference(CPUFragment.this.PrefCat);
                }
                CPUFragment.this.setGovernor(a);
                try {
                    Thread.sleep(450L);
                } catch (InterruptedException e) {
                    Log.e("Aero", "Something interrupted the main Thread, try again.", e);
                }
                CPUFragment.this.mCPUGovernor.setSummary(AeroActivity.shell.getInfo(FilePath.GOV_FILE));
                return true;
            }
        });
        this.mMaxFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.6
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String a = (String) o;
                ArrayList<String> array = new ArrayList<>();
                try {
                    if (Integer.parseInt(a) < Integer.parseInt(CPUFragment.this.mMinFrequency.getValue())) {
                        return false;
                    }
                    for (int k = 0; k < CPUFragment.mNumCpus; k++) {
                        array.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                        array.add("echo " + a + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MAX_FREQ);
                    }
                    CPUFragment.this.mMaxFrequency.setSummary(AeroActivity.shell.toMHz(a));
                    String[] commands = (String[]) array.toArray(new String[0]);
                    AeroActivity.shell.setRootInfo(commands);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        this.mMinFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.7
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String a = (String) o;
                ArrayList<String> array = new ArrayList<>();
                try {
                    if (Integer.parseInt(a) > Integer.parseInt(CPUFragment.this.mMaxFrequency.getValue())) {
                        return false;
                    }
                    for (int k = 0; k < CPUFragment.mNumCpus; k++) {
                        array.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                        array.add("echo " + a + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MIN_FREQ);
                    }
                    CPUFragment.this.mMinFrequency.setSummary(AeroActivity.shell.toMHz(a));
                    String[] commands = (String[]) array.toArray(new String[0]);
                    AeroActivity.shell.setRootInfo(commands);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        if (mNumCpus > 4) {
            this.mBIGMinFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.8
                @Override // android.preference.Preference.OnPreferenceChangeListener
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String a = (String) o;
                    ArrayList<String> array = new ArrayList<>();
                    try {
                        if (Integer.parseInt(a) < Integer.parseInt(CPUFragment.this.mBIGMinFrequency.getValue())) {
                            return false;
                        }
                        for (int k = 4; k < CPUFragment.mNumCpus; k++) {
                            array.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                            array.add("echo " + a + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MAX_FREQ);
                        }
                        CPUFragment.this.mBIGMinFrequency.setSummary(AeroActivity.shell.toMHz(a));
                        String[] commands = (String[]) array.toArray(new String[0]);
                        AeroActivity.shell.setRootInfo(commands);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
            this.mBIGMaxFrequency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.CPUFragment.9
                @Override // android.preference.Preference.OnPreferenceChangeListener
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String a = (String) o;
                    ArrayList<String> array = new ArrayList<>();
                    try {
                        if (Integer.parseInt(a) > Integer.parseInt(CPUFragment.this.mBIGMaxFrequency.getValue())) {
                            return false;
                        }
                        for (int k = 4; k < CPUFragment.mNumCpus; k++) {
                            array.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
                            array.add("echo " + a + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CPU_MIN_FREQ);
                        }
                        CPUFragment.this.mBIGMaxFrequency.setSummary(AeroActivity.shell.toMHz(a));
                        String[] commands = (String[]) array.toArray(new String[0]);
                        AeroActivity.shell.setRootInfo(commands);
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            });
        }
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
                String complete_path = FilePath.CPU_GOV_BASE + this.mCPUGovernor.getValue();
                try {
                    String[] completeParamterList = AeroActivity.shell.getDirInfo(complete_path, true);
                    if (this.PrefCat != null) {
                        this.root.removePreference(this.PrefCat);
                    }
                    if (completeParamterList.length == 0) {
                        Toast.makeText(getActivity(), R.string.pref_gov_set_no_parameter, 1).show();
                        return true;
                    }
                    this.PrefCat = new PreferenceCategory(getActivity());
                    this.PrefCat.setTitle(R.string.pref_gov_set);
                    this.root.addPreference(this.PrefCat);
                    try {
                        Thread.sleep(200L);
                        break;
                    } catch (InterruptedException e) {
                        Log.e("Aero", "Something interrupted the main Thread, try again.", e);
                    }
                    PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
                    h.genPrefFromDictionary(completeParamterList, complete_path);
                    return true;
                } catch (NullPointerException e2) {
                    Toast.makeText(getActivity(), R.string.pref_gov_set_no_parameter, 1).show();
                    Log.e("Aero", "There isn't any folder i can check. Does this governor has parameters?", e2);
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
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

    public void setGovernor(String s) {
        ArrayList<String> array = new ArrayList<>();
        for (int k = 0; k < mNumCpus; k++) {
            array.add("echo 1 > /sys/devices/system/cpu/cpu" + k + "/online");
            array.add("echo " + s + " > " + FilePath.CPU_BASE_PATH + k + FilePath.CURRENT_GOV_AVAILABLE);
        }
        String[] commands = (String[]) array.toArray(new String[0]);
        AeroActivity.shell.setRootInfo(commands);
    }

    public void updateMinFreq() {
        this.mMinFrequency.setEntries(AeroActivity.shell.getInfoArray(FilePath.CPU_AVAILABLE_FREQ, 1, 0));
        this.mMinFrequency.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.CPU_AVAILABLE_FREQ, 0, 0));
        try {
            this.mMinFrequency.setValue(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq", 0, 0)[0]);
            this.mMinFrequency.setSummary(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq", 1, 0)[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            this.mMinFrequency.setValue(NO_DATA_FOUND);
            this.mMinFrequency.setSummary(NO_DATA_FOUND);
        }
        if (mNumCpus > 4) {
            this.mBIGMinFrequency.setEntries(AeroActivity.shell.getInfoArray(FilePath.CPU_BIG_AVAILABLE_FREQ, 1, 0));
            this.mBIGMinFrequency.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.CPU_BIG_AVAILABLE_FREQ, 0, 0));
            try {
                this.mBIGMinFrequency.setValue(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq", 0, 0)[0]);
                this.mBIGMinFrequency.setSummary(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq", 1, 0)[0]);
            } catch (ArrayIndexOutOfBoundsException e2) {
                this.mBIGMinFrequency.setValue(NO_DATA_FOUND);
                this.mBIGMinFrequency.setSummary(NO_DATA_FOUND);
            }
        }
    }

    public void updateMaxFreq() {
        this.mMaxFrequency.setEntries(AeroActivity.shell.getInfoArray(FilePath.CPU_AVAILABLE_FREQ, 1, 0));
        this.mMaxFrequency.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.CPU_AVAILABLE_FREQ, 0, 0));
        try {
            this.mMaxFrequency.setValue(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq", 0, 0)[0]);
            this.mMaxFrequency.setSummary(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq", 1, 0)[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            this.mMaxFrequency.setValue(NO_DATA_FOUND);
            this.mMaxFrequency.setSummary(NO_DATA_FOUND);
        }
        if (mNumCpus > 4) {
            this.mBIGMaxFrequency.setEntries(AeroActivity.shell.getInfoArray(FilePath.CPU_BIG_AVAILABLE_FREQ, 1, 0));
            this.mBIGMaxFrequency.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.CPU_BIG_AVAILABLE_FREQ, 0, 0));
            try {
                this.mBIGMaxFrequency.setValue(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq", 0, 0)[0]);
                this.mBIGMaxFrequency.setSummary(AeroActivity.shell.getInfoArray("/sys/devices/system/cpu/cpu4/cpufreq/scaling_max_freq", 1, 0)[0]);
            } catch (ArrayIndexOutOfBoundsException e2) {
                this.mBIGMaxFrequency.setValue(NO_DATA_FOUND);
                this.mBIGMaxFrequency.setSummary(NO_DATA_FOUND);
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
                int actionBarSize = CPUFragment.this.getActivity().findViewById(R.id.action_governor_settings).getHeight();
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
