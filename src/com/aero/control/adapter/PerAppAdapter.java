package com.aero.control.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.PerApp.PerAppListener;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class PerAppAdapter extends ArrayAdapter<AeroData> {
    private static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private Context context;
    private List<AeroData> data;
    private int layoutResourceId;
    private boolean[] mCheckedState;
    private PerAppListener mPerAppListener;

    public static class Holder {
        CheckBox check;
        TextView content;
        ImageView header;
    }

    public PerAppAdapter(Context context, int layoutResourceId, List<AeroData> data, boolean[] checkedState) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.mCheckedState = checkedState;
    }

    public void setPerAppListener(PerAppListener perAppListener) {
        this.mPerAppListener = perAppListener;
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
        final Holder holder;
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService("layout_inflater");
            row = inflater.inflate(this.layoutResourceId, (ViewGroup) null);
            holder = new Holder();
            holder.header = (ImageView) row.findViewById(R.id.rowfolder);
            holder.content = (TextView) row.findViewById(R.id.rowtext);
            holder.check = (CheckBox) row.findViewById(R.id.rowcheck);
            holder.content.setTypeface(font);
            holder.check.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.adapter.PerAppAdapter.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    CheckBox cb = (CheckBox) view;
                    int row_id = ((Integer) cb.getTag()).intValue();
                    ((AeroData) PerAppAdapter.this.data.get(row_id)).isChecked = cb.isChecked();
                    PerAppAdapter.this.mPerAppListener.OnAppItemClicked(row_id, holder.check.isChecked());
                }
            });
            row.setTag(holder);
            row.setTag(R.id.rowcheck, holder.check);
        } else {
            holder = (Holder) row.getTag();
        }
        AeroData overview = this.data.get(position);
        if (this.data != null) {
            holder.header.setImageDrawable(overview.image);
            holder.content.setText(overview.name);
            holder.check.setTag(Integer.valueOf(position));
            int row_id = ((Integer) holder.check.getTag()).intValue();
            if (this.mCheckedState != null) {
                this.data.get(row_id).isChecked = this.mCheckedState[row_id];
            }
            holder.check.setTag(Integer.valueOf(row_id));
            holder.check.setChecked(overview.isChecked);
        } else {
            Log.e("Aero", "No Data found for adapter.");
        }
        return row;
    }
}
