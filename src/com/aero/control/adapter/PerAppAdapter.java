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

/**
 * Adapter for displaying a list of applications with checkboxes in the per-app
 * configuration UI. Each row shows the app icon, app name, and a checkbox for
 * selecting which apps should have per-app profiles applied.
 */
public class PerAppAdapter extends ArrayAdapter<AeroData> {
    private static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private Context context;
    private List<AeroData> data;
    private int layoutResourceId;
    private boolean[] mCheckedState;
    private PerAppListener mPerAppListener;

    /**
     * View holder for recycling list item views.
     */
    public static class Holder {
        CheckBox check;
        TextView content;
        ImageView header;
    }

    /**
     * Creates a per-app adapter.
     *
     * @param context the context
     * @param layoutResourceId the row layout resource
     * @param data the list of AeroData containing app info
     * @param checkedState the initial checked state for each app
     */
    public PerAppAdapter(Context context, int layoutResourceId, List<AeroData> data, boolean[] checkedState) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        this.mCheckedState = checkedState;
    }

    /**
     * Sets the listener for checkbox click events.
     *
     * @param perAppListener the listener to notify when checkboxes are clicked
     */
    public void setPerAppListener(PerAppListener perAppListener) {
        this.mPerAppListener = perAppListener;
    }

    /**
     * Clears all items from the adapter and the underlying data list.
     */
    @Override // android.widget.ArrayAdapter
    public void clear() {
        super.clear();
        this.data.clear();
        notifyDataSetChanged();
    }

    /**
     * Notifies observers that the underlying data has changed.
     */
    @Override // android.widget.ArrayAdapter, android.widget.BaseAdapter
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    /**
     * Returns the view for displaying an app row with a checkbox at the specified position.
     *
     * @param position the position of the item
     * @param convertView the recycled view to reuse if available
     * @param parent the parent view group
     * @return the configured view for the row
     */
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
                /**
                 * Handles checkbox clicks by updating the checked state and notifying the listener.
                 *
                 * @param view the view that was clicked
                 */
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
