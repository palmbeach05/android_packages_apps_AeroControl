package com.aero.control.fragments;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.SysfsDirectory;
import com.aero.control.helpers.SysfsResult;

/**
 * Fragment for configuring CPU boost settings. Allows tuning of CPU boost
 * parameters to control performance bursts on supported kernels.
 */
public class CPUBoostFragment extends PlaceHolderFragment {
    private PreferenceCategory PrefCat;
    private PreferenceScreen root;

    /**
     * Initializes the fragment and loads CPU boost settings.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.empty_preference);
        this.root = getPreferenceScreen();
        setTitle(getActivity().getText(R.string.perf_cpu_boost_control).toString());
        loadCPUBoost();
    }

    /**
     * Loads CPU boost parameters from the kernel and generates preference UI elements.
     */
    public void loadCPUBoost() {
        SysfsResult<String[]> listing = AeroActivity.hardware.dynamicTunables()
                .listSysfsParameters(new SysfsDirectory(FilePath.CPU_BOOST));
        if (!listing.isSuccess()) {
            Log.w("Aero", "Unable to discover CPU boost settings: " + listing.getError());
            return;
        }
        String[] completeParamterList = listing.getValue();
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.PrefCat = new PreferenceCategory(getActivity());
        this.PrefCat.setTitle(R.string.perf_cpu_boost_control);
        this.root.addPreference(this.PrefCat);
        PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
        h.genPrefFromDictionary(completeParamterList, FilePath.CPU_BOOST);
    }
}
