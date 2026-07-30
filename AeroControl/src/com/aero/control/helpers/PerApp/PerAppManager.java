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

/* JADX INFO: loaded from: classes.dex */
public class PerAppManager extends LinearLayout implements PerAppListener {
    private PerAppAdapter mAdapter;
    Context mContext;
    private ListView mListView;
    private perAppHelper mPerApp;

    public PerAppManager(Context context, AttributeSet attrs, perAppHelper perApp) {
        super(context, attrs);
        this.mContext = context;
        this.mPerApp = perApp;
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        View view = layoutInflater.inflate(R.layout.file_folder, this);
        this.mListView = (ListView) view.findViewById(R.id.list);
        setItemList();
    }

    public void setItemList() {
        List<AeroData> mData = this.mPerApp.getFullPackages();
        this.mAdapter = new PerAppAdapter(this.mContext, R.layout.perapp_row, mData, this.mPerApp.getCheckedState());
        this.mAdapter.setPerAppListener(this);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
    }

    @Override // com.aero.control.helpers.PerApp.PerAppListener
    public void OnAppItemClicked(int position, boolean isChecked) {
        this.mPerApp.setChecked(isChecked, position);
    }
}
