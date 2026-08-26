package com.aero.control.fragments;

import android.R;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.aero.control.AeroActivity;

public class PlaceHolderFragment extends PreferenceFragment {
    private String mTitle;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if (v != null) {
            ListView lv = (ListView) v.findViewById(R.id.list);
            lv.setPadding(10, 0, 10, 0);
            lv.setDivider(null);
            lv.setDividerHeight(10);
            lv.setDrawSelectorOnTop(true);
        }
        return v;
    }

    public final void setTitle(String title) {
        ((AeroActivity) getActivity()).setActionBarTitle(title);
        this.mTitle = title;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mTitle != null) {
            setTitle(this.mTitle);
        }
    }
}
