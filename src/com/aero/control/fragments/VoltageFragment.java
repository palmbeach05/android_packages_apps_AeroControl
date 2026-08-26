package com.aero.control.fragments;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomTextPreference;
import com.aero.control.helpers.FilePath;
import java.util.ArrayList;

public class VoltageFragment extends PlaceHolderFragment {
    private PreferenceCategory PrefCat;
    private SharedPreferences mPrefs;
    private PreferenceScreen root;
    private final ArrayList<String> voltList = new ArrayList<>();

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.layout.empty_preference);
        this.root = getPreferenceScreen();
        setTitle(getActivity().getText(R.string.perf_voltage_control).toString());
        loadVoltage();
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.voltage_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        String[] voltArray = (String[]) this.voltList.toArray(new String[0]);
        String exec = "";
        switch (item.getItemId()) {
            case R.id.action_mv_plus /* 2131099754 */:
                this.voltList.clear();
                for (String a : voltArray) {
                    try {
                        int tmp = Integer.parseInt(a) + 25;
                        this.voltList.add("" + tmp);
                        exec = exec + " " + tmp;
                    } catch (NumberFormatException e) {
                        this.voltList.add(a);
                        exec = exec + " " + a;
                    }
                }
                executeVolt(exec);
                break;
            case R.id.action_mv_minus /* 2131099755 */:
                this.voltList.clear();
                for (String a2 : voltArray) {
                    try {
                        int tmp2 = Integer.parseInt(a2) - 25;
                        this.voltList.add("" + tmp2);
                        exec = exec + " " + tmp2;
                    } catch (NumberFormatException e) {
                        this.voltList.add(a2);
                        exec = exec + " " + a2;
                    }
                }
                executeVolt(exec);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadVoltage() {
        String[] completeParamterList = AeroActivity.shell.getInfo(FilePath.VOLTAGE_PATH, false);
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.PrefCat = new PreferenceCategory(getActivity());
        this.PrefCat.setTitle(R.string.perf_voltage_control);
        this.root.addPreference(this.PrefCat);
        for (String s : completeParamterList) {
            String freqTmp = s.split(":")[0];
            String volTmp = s.split(":")[1].replace(" ", "");
            this.voltList.add(volTmp.replace("mV", ""));
            final CustomTextPreference voltPref = new CustomTextPreference(getActivity());
            voltPref.getEditText().setInputType(2);
            voltPref.setPrefSummary(volTmp);
            voltPref.setTitle(freqTmp);
            voltPref.setPrefText(freqTmp);
            voltPref.setText(volTmp.replace("mV", ""));
            voltPref.setDialogTitle(freqTmp);
            voltPref.setHideOnBoot(true);
            voltPref.setHelpEnable(false);
            this.PrefCat.addPreference(voltPref);
            voltPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.VoltageFragment.1
                @Override // android.preference.Preference.OnPreferenceChangeListener
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String value = o.toString().trim();
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    String[] voltArray = (String[]) VoltageFragment.this.voltList.toArray(new String[0]);
                    String tmp = "";
                    voltArray[preference.getOrder()] = value;
                    preference.setSummary(value + "mV");
                    voltPref.setPrefSummary(value + "mV");
                    VoltageFragment.this.voltList.clear();
                    for (String a : voltArray) {
                        tmp = tmp + " " + a;
                        VoltageFragment.this.voltList.add(a);
                    }
                    VoltageFragment.this.executeVolt(tmp);
                    return true;
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 19) {
            Preference blankedPref = new Preference(getActivity());
            blankedPref.setSelectable(false);
            this.PrefCat.addPreference(blankedPref);
        }
    }

    public void executeVolt(String exeVolt) {
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        AeroActivity.shell.setRootInfo(exeVolt, FilePath.VOLTAGE_PATH);
        updateUI();
    }

    public void updateUI() {
        String[] voltArray = (String[]) this.voltList.toArray(new String[0]);
        for (int i = 0; i < this.PrefCat.getPreferenceCount() - 1; i++) {
            CustomTextPreference voltPref = (CustomTextPreference) this.PrefCat.getPreference(i);
            voltPref.setSummary(voltArray[i] + "mV");
            voltPref.setPrefSummary(voltArray[i] + "mV");
            voltPref.setText(voltArray[i]);
        }
    }
}
