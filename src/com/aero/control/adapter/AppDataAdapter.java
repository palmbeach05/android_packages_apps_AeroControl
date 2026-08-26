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
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElement;
import com.aero.control.helpers.Util;
import java.util.List;

/**
 * Adapter for displaying monitored app data in the AppMonitor fragment. Each row
 * shows an app icon, name, and timestamp of when it was last monitored or logged.
 */
public class AppDataAdapter extends ArrayAdapter<AppElement> {
    private static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private Context context;
    private List<AppElement> data;
    private int layoutResourceId;

    /**
     * View holder for recycling list item views.
     */
    public static class Holder {
        ImageView image;
        TextView text;
        TextView textTime;
    }

    /**
     * Creates an app data adapter.
     *
     * @param context the context
     * @param layoutResourceId the row layout resource
     * @param data the list of AppElement objects to display
     */
    public AppDataAdapter(Context context, int layoutResourceId, List<AppElement> data) {
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
            holder.image = (ImageView) row.findViewById(R.id.rowimage);
            holder.text = (TextView) row.findViewById(R.id.rowtext);
            holder.textTime = (TextView) row.findViewById(R.id.rowtime);
            holder.text.setTypeface(font);
            holder.textTime.setTypeface(font);
            row.setTag(holder);
        } else {
            holder = (Holder) row.getTag();
        }
        AppElement overview = this.data.get(position);
        if (this.data != null) {
            holder.text.setText(overview.getRealName());
            holder.textTime.setText(Util.getFormatedTimeString(overview.getUsage().longValue()));
            holder.image.setImageDrawable(overview.getImage());
            holder.image.setTag(Integer.valueOf(position));
        } else {
            Log.e("Aero", "No Data found for adapter.");
        }
        return row;
    }
}
