package com.aero.control.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.aero.control.R;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AeroAdapter extends ArrayAdapter<AeroData> {
    private static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private Context context;
    private List<AeroData> data;
    private int layoutResourceId;

    public static class Holder {
        TextView content;
        TextView header;
        TextView right_header;
    }

    public AeroAdapter(Context context, int layoutResourceId, List<AeroData> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
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
        Holder holder;
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService("layout_inflater");
            row = inflater.inflate(this.layoutResourceId, (ViewGroup) null);
            holder = new Holder();
            holder.header = (TextView) row.findViewById(R.id.header);
            holder.right_header = (TextView) row.findViewById(R.id.right_header);
            holder.content = (TextView) row.findViewById(R.id.content);
            holder.header.setTypeface(font);
            holder.right_header.setTypeface(font);
            holder.content.setTypeface(font);
            row.setTag(holder);
        } else {
            holder = (Holder) row.getTag();
        }
        AeroData overview = this.data.get(position);
        if (this.data != null) {
            if (!overview.name.equals("A")) {
                holder.header.setText(overview.name);
            }
            if (overview.right_name != null) {
                holder.right_header.setText(overview.right_name);
            }
            if (!overview.content.equals("A")) {
                holder.content.setText(overview.content);
            }
        } else {
            Log.e("Aero", "No Data found for adapter.");
        }
        return row;
    }
}
