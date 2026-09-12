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
import java.util.ArrayList;
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
    private static class PerformanceHolder {
        TextView cpuHeader;
        TextView cpuContent;
        TableLayout cpuTable;
        TextView[] cpuCells;
        TextView gpuHeader;
        TextView gpuValue;
    }
    private static class TemperatureHolder { LinearLayout rows; }
    private static class BatteryHolder { LinearLayout rows; }
    private static class BatteryDisplayReading {
        final int labelResourceId;
        final String value;

        BatteryDisplayReading(int labelResourceId, String value) {
            this.labelResourceId = labelResourceId;
            this.value = value;
        }
    }
    private static class ConfigurationHolder { LinearLayout rows; }
    private static class SystemHolder { LinearLayout rows; }

    public AeroAdapter(Context context, int ignoredLayoutResourceId, List<AeroData> data) {
        super(context, 0, data);
        this.inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @Override public int getViewTypeCount() { return 8; }
    @Override public int getItemViewType(int position) { return data.get(position).itemType; }
    @Override public boolean isEnabled(int position) { return false; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AeroData item = data.get(position);
        switch (item.itemType) {
            case AeroData.TYPE_SECTION_HEADER: return bindSection(item, convertView, parent);
            case AeroData.TYPE_CPU_FREQUENCY_CARD: return bindFrequency(item, convertView, parent);
            case AeroData.TYPE_TEMPERATURE_CARD: return bindTemperatures(item, convertView, parent);
            case AeroData.TYPE_PERFORMANCE_CARD: return bindPerformance(item, convertView, parent);
            case AeroData.TYPE_CONFIGURATION_CARD: return bindConfiguration(item, convertView, parent);
            case AeroData.TYPE_SYSTEM_CARD: return bindSystem(item, convertView, parent);
            case AeroData.TYPE_BATTERY_CARD: return bindBattery(item, convertView, parent);
            default: return bindStandard(item, convertView, parent);
        }
    }

    private View bindSystem(AeroData item, View row, ViewGroup parent) {
        SystemHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_system_card, parent, false);
            holder = new SystemHolder();
            holder.rows = (LinearLayout) row.findViewById(R.id.system_rows);
            row.setTag(holder);
        } else { holder = (SystemHolder) row.getTag(); }

        holder.rows.removeAllViews();
        if (item.systemReadings != null) {
            for (AeroData.SystemReading reading : item.systemReadings) {
                View readingView = inflater.inflate(
                        R.layout.overview_system_row, holder.rows, false);
                TextView label = (TextView) readingView.findViewById(R.id.system_label);
                TextView value = (TextView) readingView.findViewById(R.id.system_value);
                label.setTypeface(FONT);
                value.setTypeface(FONT);
                label.setText(reading.label == null ? "" : reading.label);
                value.setText(reading.value == null
                        ? getContext().getString(R.string.unavailable) : reading.value);
                holder.rows.addView(readingView);
            }
        }
        return row;
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

    /**
     * Binds temperature readings into a two-column card, adding a spacer for an odd final
     * reading.
     *
     * @param item the temperature card data to display
     * @param row the recycled card view, or null when a new view must be inflated
     * @param parent the parent used to inflate a new card view
     * @return the bound temperature card view
     */
    private View bindTemperatures(AeroData item, View row, ViewGroup parent) {
        TemperatureHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_temperature_card, parent, false);
            holder = new TemperatureHolder();
            holder.rows = (LinearLayout) row.findViewById(R.id.temperature_rows);
            row.setTag(holder);
        } else { holder = (TemperatureHolder) row.getTag(); }
        holder.rows.removeAllViews();
        if (item.temperatures != null) {
            for (int i = 0; i < item.temperatures.size(); i += 2) {
                LinearLayout pairRow = new LinearLayout(getContext());
                pairRow.setOrientation(LinearLayout.HORIZONTAL);
                pairRow.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                addTemperatureReading(pairRow, item.temperatures.get(i), true, true);
                if (i + 1 < item.temperatures.size()) {
                    addTemperatureReading(
                            pairRow, item.temperatures.get(i + 1), true, false);
                } else {
                    addReadingSpacer(pairRow);
                }
                holder.rows.addView(pairRow);
            }
        }
        return row;
    }

    private void addTemperatureReading(LinearLayout parent,
            AeroData.TemperatureReading reading, boolean weighted, boolean leftColumn) {
        View readingView = inflater.inflate(R.layout.overview_temperature_row, parent, false);
        TextView label = (TextView) readingView.findViewById(R.id.temperature_label);
        TextView value = (TextView) readingView.findViewById(R.id.temperature_value);
        label.setTypeface(FONT);
        value.setTypeface(FONT);
        label.setText(reading.label == null ? "" : reading.label);
        value.setText(reading.value == null
                ? getContext().getString(R.string.unavailable) : reading.value);
        if (weighted) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            int spacing = dpToPixels(4);
            if (leftColumn) params.rightMargin = spacing;
            else params.leftMargin = spacing;
            readingView.setLayoutParams(params);
        }
        parent.addView(readingView);
    }

    private void addReadingSpacer(LinearLayout parent) {
        View spacer = new View(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        params.leftMargin = dpToPixels(4);
        spacer.setLayoutParams(params);
        parent.addView(spacer);
    }

    private View bindBattery(AeroData item, View row, ViewGroup parent) {
        BatteryHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_battery_card, parent, false);
            holder = new BatteryHolder();
            holder.rows = (LinearLayout) row.findViewById(R.id.battery_rows);
            row.setTag(holder);
        } else { holder = (BatteryHolder) row.getTag(); }

        holder.rows.removeAllViews();
        AeroData.BatteryReading reading = item.batteryReading;
        List<BatteryDisplayReading> readings = new ArrayList<>();
        if (reading != null) {
            addBatteryDisplayReading(readings, R.string.battery_level, reading.level);
            addBatteryDisplayReading(readings, R.string.battery_status, reading.status);
            addBatteryDisplayReading(
                    readings, R.string.battery_power_source, reading.powerSource);
            addBatteryDisplayReading(readings, R.string.battery_voltage, reading.voltage);
            addBatteryDisplayReading(readings, R.string.battery_current, reading.current);
        }
        for (int i = 0; i < readings.size(); i += 2) {
            LinearLayout pairRow = new LinearLayout(getContext());
            pairRow.setOrientation(LinearLayout.HORIZONTAL);
            pairRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            addBatteryReading(pairRow, readings.get(i), true);
            if (i + 1 < readings.size()) {
                addBatteryReading(pairRow, readings.get(i + 1), false);
            } else {
                addReadingSpacer(pairRow);
            }
            holder.rows.addView(pairRow);
        }
        return row;
    }

    private void addBatteryDisplayReading(
            List<BatteryDisplayReading> readings, int labelResourceId, String value) {
        if (value != null) readings.add(new BatteryDisplayReading(labelResourceId, value));
    }

    private void addBatteryReading(
            LinearLayout parent, BatteryDisplayReading reading, boolean leftColumn) {
        View readingView = inflater.inflate(R.layout.overview_temperature_row, parent, false);
        TextView label = (TextView) readingView.findViewById(R.id.temperature_label);
        TextView value = (TextView) readingView.findViewById(R.id.temperature_value);
        label.setTypeface(FONT);
        value.setTypeface(FONT);
        label.setText(reading.labelResourceId);
        value.setText(reading.value);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        int spacing = dpToPixels(4);
        if (leftColumn) params.rightMargin = spacing;
        else params.leftMargin = spacing;
        readingView.setLayoutParams(params);
        parent.addView(readingView);
    }

    private View bindPerformance(AeroData item, View row, ViewGroup parent) {
        PerformanceHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_performance_card, parent, false);
            holder = new PerformanceHolder();
            holder.cpuHeader = (TextView) row.findViewById(R.id.cpu_frequency_header);
            holder.cpuContent = (TextView) row.findViewById(R.id.cpu_frequency_content);
            holder.cpuTable = (TableLayout) row.findViewById(R.id.compact_frequency_table);
            holder.gpuHeader = (TextView) row.findViewById(R.id.gpu_frequency_header);
            holder.gpuValue = (TextView) row.findViewById(R.id.gpu_frequency_value);
            int[] ids = {R.id.compact_freq_cell_0, R.id.compact_freq_cell_1,
                    R.id.compact_freq_cell_2, R.id.compact_freq_cell_3,
                    R.id.compact_freq_cell_4, R.id.compact_freq_cell_5,
                    R.id.compact_freq_cell_6, R.id.compact_freq_cell_7};
            holder.cpuCells = new TextView[ids.length];
            holder.cpuHeader.setTypeface(FONT);
            holder.cpuContent.setTypeface(FONT);
            holder.gpuHeader.setTypeface(FONT);
            holder.gpuValue.setTypeface(FONT);
            for (int i = 0; i < ids.length; i++) {
                holder.cpuCells[i] = (TextView) row.findViewById(ids[i]);
                holder.cpuCells[i].setTypeface(Typeface.MONOSPACE);
            }
            row.setTag(holder);
        } else { holder = (PerformanceHolder) row.getTag(); }

        holder.cpuHeader.setText(item.cpuFrequencyTitle == null ? "" : item.cpuFrequencyTitle);
        holder.gpuHeader.setText(item.gpuFrequencyTitle == null ? "" : item.gpuFrequencyTitle);
        holder.gpuValue.setText(item.gpuFrequencyValue == null ? "" : item.gpuFrequencyValue);

        List<String> frequencies = item.coreFrequencies == null
                ? Collections.<String>emptyList() : item.coreFrequencies;
        boolean showGrid = frequencies.size() > 0 && frequencies.size() <= MAX_GRID_CORES;
        holder.cpuTable.setVisibility(showGrid ? View.VISIBLE : View.GONE);
        holder.cpuContent.setText(item.cpuFrequencyContent == null
                ? "" : item.cpuFrequencyContent);
        holder.cpuContent.setVisibility(item.cpuFrequencyContent == null
                || item.cpuFrequencyContent.length() == 0 ? View.GONE : View.VISIBLE);
        for (int i = 0; i < holder.cpuCells.length; i++) {
            if (showGrid && i < frequencies.size()) {
                holder.cpuCells[i].setText(frequencies.get(i));
                holder.cpuCells[i].setVisibility(View.VISIBLE);
            } else {
                holder.cpuCells[i].setText("");
                holder.cpuCells[i].setVisibility(View.GONE);
            }
        }
        return row;
    }

    private View bindConfiguration(AeroData item, View row, ViewGroup parent) {
        ConfigurationHolder holder;
        if (row == null) {
            row = inflater.inflate(R.layout.overview_configuration_card, parent, false);
            holder = new ConfigurationHolder();
            holder.rows = (LinearLayout) row.findViewById(R.id.configuration_rows);
            row.setTag(holder);
        } else { holder = (ConfigurationHolder) row.getTag(); }

        holder.rows.removeAllViews();
        List<AeroData.ConfigurationReading> readings = item.configurations == null
                ? Collections.<AeroData.ConfigurationReading>emptyList() : item.configurations;
        List<AeroData.ConfigurationReading> governors = new ArrayList<>();
        List<AeroData.ConfigurationReading> otherReadings = new ArrayList<>();
        for (AeroData.ConfigurationReading reading : readings) {
            if (reading.kind == AeroData.ConfigurationReading.Kind.GOVERNOR) {
                governors.add(reading);
            } else {
                otherReadings.add(reading);
            }
        }

        int firstOtherReading = 0;
        if (governors.size() == 1 && !otherReadings.isEmpty()) {
            addConfigurationPair(holder.rows, governors.get(0), otherReadings.get(0));
            firstOtherReading = 1;
        } else if (governors.size() == 2) {
            addConfigurationPair(holder.rows, governors.get(0), governors.get(1));
        } else {
            for (AeroData.ConfigurationReading reading : governors) {
                addConfigurationCard(holder.rows, reading, false, false);
            }
        }
        for (int i = firstOtherReading; i < otherReadings.size(); i++) {
            addConfigurationCard(holder.rows, otherReadings.get(i), false, false);
        }
        return row;
    }

    private void addConfigurationPair(LinearLayout rows,
            AeroData.ConfigurationReading left, AeroData.ConfigurationReading right) {
        LinearLayout pair = new LinearLayout(getContext());
        pair.setOrientation(LinearLayout.HORIZONTAL);
        pair.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addConfigurationCard(pair, left, true, true);
        addConfigurationCard(pair, right, true, false);
        rows.addView(pair);
    }

    private void addConfigurationCard(LinearLayout parent,
            AeroData.ConfigurationReading reading, boolean weighted, boolean leftCard) {
        View card = inflater.inflate(R.layout.overview_standard_card, parent, false);
        TextView header = (TextView) card.findViewById(R.id.header);
        TextView content = (TextView) card.findViewById(R.id.content);
        header.setTypeface(FONT);
        content.setTypeface(FONT);
        header.setText(reading.label == null ? "" : reading.label);
        content.setText(reading.value == null
                ? getContext().getString(R.string.unavailable) : reading.value);
        if (weighted) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            int spacing = dpToPixels(2);
            if (leftCard) params.rightMargin = spacing;
            else params.leftMargin = spacing;
            card.setLayoutParams(params);
        }
        parent.addView(card);
    }

    private int dpToPixels(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }
}
