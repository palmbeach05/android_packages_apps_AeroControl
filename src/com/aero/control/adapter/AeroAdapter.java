package com.aero.control.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import com.aero.control.R;
import java.util.List;

/**
 * Adapter for displaying system information in the Overview and other fragments.
 * Supports both standard text rows (header, content, optional right-aligned text)
 * and multi-core CPU frequency grid rows when AeroData contains a coreFrequencies list.
 */
public class AeroAdapter extends ArrayAdapter<AeroData> {
    private static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private static final int MAX_GRID_CORES = 8;
    private Context context;
    private List<AeroData> data;
    private int layoutResourceId;

    /**
     * View holder for recycling list item views.
     */
    public static class Holder {
        TextView content;
        TextView header;
        TextView right_header;
        TableLayout freqTable;
        TextView[] freqCells;
    }

    /**
     * Creates an AeroAdapter.
     *
     * @param context the context
     * @param layoutResourceId the row layout resource
     * @param data the list of AeroData items to display
     */
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
            holder.freqTable = (TableLayout) row.findViewById(R.id.freq_table);
            holder.header.setTypeface(font);
            holder.right_header.setTypeface(font);
            holder.content.setTypeface(font);
            int[] cellIds = {R.id.freq_cell_0, R.id.freq_cell_1, R.id.freq_cell_2, R.id.freq_cell_3, R.id.freq_cell_4, R.id.freq_cell_5, R.id.freq_cell_6, R.id.freq_cell_7};
            holder.freqCells = new TextView[cellIds.length];
            for (int i = 0; i < cellIds.length; i++) {
                holder.freqCells[i] = (TextView) row.findViewById(cellIds[i]);
                holder.freqCells[i].setTypeface(font);
                holder.freqCells[i].setTypeface(Typeface.MONOSPACE);
            }
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
            List<String> coreFrequencies = overview.coreFrequencies;
            if (coreFrequencies != null && coreFrequencies.size() >= 1 && coreFrequencies.size() <= MAX_GRID_CORES) {
                holder.freqTable.setVisibility(View.VISIBLE);
                for (int i = 0; i < holder.freqCells.length; i++) {
                    if (i < coreFrequencies.size()) {
                        holder.freqCells[i].setText(coreFrequencies.get(i));
                        holder.freqCells[i].setVisibility(View.VISIBLE);
                    } else {
                        holder.freqCells[i].setVisibility(View.GONE);
                    }
                }
                if (overview.content == null || overview.content.length() == 0) {
                    holder.content.setVisibility(View.GONE);
                } else {
                    holder.content.setVisibility(View.VISIBLE);
                    holder.content.setText(overview.content);
                }
            } else {
                holder.freqTable.setVisibility(View.GONE);
                holder.content.setVisibility(View.VISIBLE);
                if (overview.content != null && !overview.content.equals("A")) {
                    holder.content.setText(overview.content);
                }
            }
        } else {
            Log.e("Aero", "No Data found for adapter.");
        }
        return row;
    }
}
