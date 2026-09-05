package com.aero.control.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import com.aero.control.R;
import java.util.Collections;
import java.util.List;

/** Adapter for the typed cards and section headers in the Overview list. */
public class AeroAdapter extends ArrayAdapter<AeroData> {
    private static final int MAX_GRID_CORES = 8;
    private static final Typeface FONT = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    private final LayoutInflater inflater;
    private final List<AeroData> data;

    private static class SectionHolder { TextView header; }
    private static class StandardHolder { TextView header; TextView content; }
    private static class FrequencyHolder { TextView header; TextView content; TableLayout table; TextView[] cells; }
    private static class TemperatureHolder { TextView header; LinearLayout rows; }

    public AeroAdapter(Context context, int ignoredLayoutResourceId, List<AeroData> data) {
        super(context, 0, data);
        this.inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @Override public int getViewTypeCount() { return 4; }
    @Override public int getItemViewType(int position) { return data.get(position).itemType; }
    @Override public boolean isEnabled(int position) { return false; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AeroData item = data.get(position);
        switch (item.itemType) {
            case AeroData.TYPE_SECTION_HEADER: return bindSection(item, convertView, parent);
            case AeroData.TYPE_CPU_FREQUENCY_CARD: return bindFrequency(item, convertView, parent);
            case AeroData.TYPE_TEMPERATURE_CARD: return bindTemperatures(item, convertView, parent);
            default: return bindStandard(item, convertView, parent);
        }
    }

    private View bindSection(AeroData item, View row, ViewGroup parent) {
        SectionHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_section_header, parent, false);
            holder = new SectionHolder();
            holder.header = (TextView) row.findViewById(R.id.section_header);
            holder.header.setTypeface(FONT);
            row.setTag(holder);
        } else { holder = (SectionHolder) row.getTag(); }
        holder.header.setText(item.name == null ? "" : item.name);
        holder.header.setVisibility(View.VISIBLE);
        return row;
    }

    private View bindStandard(AeroData item, View row, ViewGroup parent) {
        StandardHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_standard_card, parent, false);
            holder = new StandardHolder();
            holder.header = (TextView) row.findViewById(R.id.header);
            holder.content = (TextView) row.findViewById(R.id.content);
            holder.header.setTypeface(FONT);
            holder.content.setTypeface(FONT);
            row.setTag(holder);
        } else { holder = (StandardHolder) row.getTag(); }
        holder.header.setText(item.name == null ? "" : item.name);
        holder.content.setText(item.content == null ? "" : item.content);
        holder.header.setVisibility(View.VISIBLE);
        holder.content.setVisibility(View.VISIBLE);
        return row;
    }

    private View bindFrequency(AeroData item, View row, ViewGroup parent) {
        FrequencyHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_cpu_frequency_card, parent, false);
            holder = new FrequencyHolder();
            holder.header = (TextView) row.findViewById(R.id.header);
            holder.content = (TextView) row.findViewById(R.id.content);
            holder.table = (TableLayout) row.findViewById(R.id.freq_table);
            int[] ids = {R.id.freq_cell_0, R.id.freq_cell_1, R.id.freq_cell_2, R.id.freq_cell_3,
                    R.id.freq_cell_4, R.id.freq_cell_5, R.id.freq_cell_6, R.id.freq_cell_7};
            holder.cells = new TextView[ids.length];
            holder.header.setTypeface(FONT);
            holder.content.setTypeface(FONT);
            for (int i = 0; i < ids.length; i++) {
                holder.cells[i] = (TextView) row.findViewById(ids[i]);
                holder.cells[i].setTypeface(Typeface.MONOSPACE);
            }
            row.setTag(holder);
        } else { holder = (FrequencyHolder) row.getTag(); }
        holder.header.setText(item.name == null ? "" : item.name);
        holder.header.setVisibility(View.VISIBLE);
        holder.content.setText(item.content == null ? "" : item.content);
        holder.content.setVisibility(item.content == null || item.content.length() == 0 ? View.GONE : View.VISIBLE);
        List<String> frequencies = item.coreFrequencies == null ? Collections.<String>emptyList() : item.coreFrequencies;
        boolean showGrid = frequencies.size() > 0 && frequencies.size() <= MAX_GRID_CORES;
        holder.table.setVisibility(showGrid ? View.VISIBLE : View.GONE);
        for (int i = 0; i < holder.cells.length; i++) {
            if (showGrid && i < frequencies.size()) {
                holder.cells[i].setText(frequencies.get(i));
                holder.cells[i].setVisibility(View.VISIBLE);
            } else {
                holder.cells[i].setText("");
                holder.cells[i].setVisibility(View.GONE);
            }
        }
        return row;
    }

    private View bindTemperatures(AeroData item, View row, ViewGroup parent) {
        TemperatureHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_temperature_card, parent, false);
            holder = new TemperatureHolder();
            holder.header = (TextView) row.findViewById(R.id.header);
            holder.rows = (LinearLayout) row.findViewById(R.id.temperature_rows);
            holder.header.setTypeface(FONT);
            row.setTag(holder);
        } else { holder = (TemperatureHolder) row.getTag(); }
        holder.header.setText(item.name == null ? "" : item.name);
        holder.header.setVisibility(View.VISIBLE);
        holder.rows.removeAllViews();
        if (item.temperatures != null) {
            for (AeroData.TemperatureReading reading : item.temperatures) {
                View readingView = inflater.inflate(R.layout.overview_temperature_row, holder.rows, false);
                TextView label = (TextView) readingView.findViewById(R.id.temperature_label);
                TextView value = (TextView) readingView.findViewById(R.id.temperature_value);
                label.setTypeface(FONT);
                value.setTypeface(FONT);
                label.setText(reading.label == null ? "" : reading.label);
                value.setText(reading.value == null ? "" : reading.value);
                label.setVisibility(View.VISIBLE);
                value.setVisibility(View.VISIBLE);
                holder.rows.addView(readingView);
            }
        }
        return row;
    }
}
