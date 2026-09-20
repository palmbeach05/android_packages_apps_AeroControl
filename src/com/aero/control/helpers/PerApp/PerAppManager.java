package com.aero.control.helpers.PerApp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.aero.control.R;
import com.aero.control.adapter.AeroData;
import com.aero.control.adapter.PerAppAdapter;
import java.util.List;

/**
 * Custom view that displays a list of installed applications for per-app monitoring
 * configuration. Allows users to select which apps should be monitored by the
 * per-app service.
 */
public class PerAppManager extends LinearLayout implements PerAppListener {
    private PerAppAdapter mAdapter;
    Context mContext;
    private ListView mListView;
    private perAppHelper mPerApp;

    /**
     * Constructs a PerAppManager view.
     *
     * @param context the context
     * @param attrs the attribute set from XML
     * @param perApp the helper that manages per-app monitoring data
     */
    public PerAppManager(Context context, AttributeSet attrs, perAppHelper perApp) {
        super(context, attrs);
        this.mContext = context;
        this.mPerApp = perApp;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        View view = layoutInflater.inflate(R.layout.file_folder, this);
        this.mListView = (ListView) view.findViewById(R.id.list);
        setItemList();
    }

    /**
     * Populates the list view with installed applications and their monitoring states.
     */
    public void setItemList() {
        List<AeroData> mData = this.mPerApp.getFullPackages();
        this.mAdapter = new PerAppAdapter(this.mContext, R.layout.perapp_row, mData, this.mPerApp.getCheckedState());
        this.mAdapter.setPerAppListener(this);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
    }

    /**
     * Called when a user toggles monitoring for an application in the list.
     *
     * @param position the position of the clicked item in the list
     * @param isChecked whether monitoring is now enabled for this app
     */
    @Override // com.aero.control.helpers.PerApp.PerAppListener
    public void OnAppItemClicked(int position, boolean isChecked) {
        this.mPerApp.setChecked(isChecked, position);
    }
}
