package com.aero.control.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.aero.control.R;
import java.util.List;

/**
 * Adapter for displaying files and directories in a list. Each row shows an icon
 * (folder or file) and the filename/content text.
 */
public class FileAdapter extends ArrayAdapter<AeroData> {
    private static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private Context context;
    private List<AeroData> data;
    private int layoutResourceId;

    /**
     * View holder for recycling list item views.
     */
    public static class Holder {
        TextView content;
        ImageView header;
    }

    /**
     * Creates a file adapter.
     *
     * @param context the context
     * @param layoutResourceId the row layout resource
     * @param data the list of AeroData containing file/directory info
     */
    public FileAdapter(Context context, int layoutResourceId, List<AeroData> data) {
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
            holder.header = (ImageView) row.findViewById(R.id.rowfolder);
            holder.content = (TextView) row.findViewById(R.id.rowtext);
            holder.content.setTypeface(font);
            row.setTag(holder);
        } else {
            holder = (Holder) row.getTag();
        }
        AeroData overview = this.data.get(position);
        if (this.data != null) {
            holder.header.setImageResource(overview.file);
            holder.content.setText(overview.content);
        } else {
            Log.e("Aero", "No Data found for adapter.");
        }
        return row;
    }
}
