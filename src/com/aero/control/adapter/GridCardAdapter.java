package com.aero.control.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.aero.control.helpers.Android.Material.CardBox;
import java.util.List;

public class GridCardAdapter extends ArrayAdapter<CardBox> {
    private List<CardBox> data;

    public GridCardAdapter(Context context, int layoutResourceId, List<CardBox> data) {
        super(context, layoutResourceId, data);
        this.data = data;
    }

    @Override // android.widget.ArrayAdapter
    public void clear() {
        super.clear();
        this.data.clear();
        notifyDataSetChanged();
    }

    @Override // android.widget.ArrayAdapter, android.widget.BaseAdapter
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        return this.data.get(position);
    }
}
