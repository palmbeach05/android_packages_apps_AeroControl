package com.aero.control.fragments;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;

/**
 * Fragment for configuring CPU hotplug settings. Allows tuning of CPU hotplug
 * driver parameters that control when cores are brought online or offline.
 */
public class CPUHotplugFragment extends PlaceHolderFragment {
    private PreferenceCategory PrefCat;
    private String mHotplugPath;
    private PreferenceScreen root;

    /**
     * Initializes the fragment and loads CPU hotplug settings.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.empty_preference);
        this.root = getPreferenceScreen();
        String[] arr$ = FilePath.HOTPLUG_PATH;
        for (String s : arr$) {
            if (AeroActivity.genHelper.doesExist(s)) {
                this.mHotplugPath = s;
            }
        }
        setTitle(getActivity().getText(R.string.perf_cpu_hotplug_driver).toString());
        loadHotplug();
    }

    /**
     * Loads CPU hotplug parameters from the kernel and generates preference UI elements.
     */
    public void loadHotplug() {
        String[] completeParamterList = AeroActivity.shell.getDirInfo(this.mHotplugPath, true);
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.PrefCat = new PreferenceCategory(getActivity());
        this.PrefCat.setTitle(R.string.perf_cpu_hotplug);
        this.root.addPreference(this.PrefCat);
        try {
            PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
            h.genPrefFromDictionary(completeParamterList, this.mHotplugPath);
        } catch (NullPointerException e) {
            Log.e("Aero", "I couldn't get any files!", e);
        }
    }
}
