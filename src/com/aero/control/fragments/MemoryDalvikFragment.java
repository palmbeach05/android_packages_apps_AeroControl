package com.aero.control.fragments;

import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.ProcfsDirectory;
import com.aero.control.helpers.SysfsResult;

/**
 * Fragment for configuring Dalvik VM and kernel virtual memory parameters
 * including heap sizes, dirty ratios, and other low-level memory tunables.
 */
public class MemoryDalvikFragment extends PlaceHolderFragment {
    private PreferenceCategory PrefCat;
    private PreferenceScreen root;

    /**
     * Initializes the fragment, loads the empty preference layout, and populates it
     * with Dalvik VM and kernel memory parameters from the filesystem.
     *
     * @param savedInstanceState the saved instance state bundle
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.empty_preference);
        this.root = getPreferenceScreen();
        setTitle(getActivity().getText(R.string.pref_dalvik_settings).toString());
        loadDalvik();
    }

    /**
     * Loads and displays Dalvik VM and kernel memory tunable parameters from the filesystem.
     * Dynamically generates preferences for each available parameter found in the Dalvik
     * tweak directory.
     */
    public void loadDalvik() {
        SysfsResult<String[]> listing = AeroActivity.hardware.dynamicTunables()
                .listProcfsParameters(new ProcfsDirectory(FilePath.DALVIK_TWEAK));
        if (!listing.isSuccess()) {
            Log.w("Aero", "Unable to discover Dalvik tunables: " + listing.getError());
            return;
        }
        String[] completeParamterList = listing.getValue();
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        this.PrefCat = new PreferenceCategory(getActivity());
        this.PrefCat.setTitle(R.string.pref_dalvik_settings_heading);
        this.root.addPreference(this.PrefCat);
        PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
        h.genPrefFromDictionary(completeParamterList, FilePath.DALVIK_TWEAK);
    }
}
