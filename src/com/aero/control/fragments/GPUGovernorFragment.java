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
 * Fragment for configuring GPU governor-specific parameters. Allows tuning
 * of settings specific to the currently selected GPU governor.
 */
public class GPUGovernorFragment extends PlaceHolderFragment {
    private PreferenceCategory PrefCat;
    private PreferenceScreen root;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.empty_preference);
        this.root = getPreferenceScreen();
        setTitle(getActivity().getText(R.string.perf_gpu_gov).toString());
        loadGPUGov();
    }

    public void loadGPUGov() {
        String[] completeParamterList = AeroActivity.shell.getDirInfo(FilePath.GPU_GOV_PATH, true);
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.PrefCat = new PreferenceCategory(getActivity());
        this.PrefCat.setTitle(R.string.perf_gpu_gov_settings);
        this.root.addPreference(this.PrefCat);
        try {
            PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
            h.genPrefFromDictionary(completeParamterList, FilePath.GPU_GOV_PATH);
        } catch (NullPointerException e) {
            Log.e("Aero", "I couldn't get any files!", e);
        }
    }
}
