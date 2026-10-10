package com.aero.control.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.aero.control.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying CPU frequency statistics with color-coded rows and fade-in
 * animations. Each row shows a frequency, time in state, and percentage, with colors
 * assigned based on position or dynamically generated for positions beyond the predefined
 * color array.
 */
public class StatisticAdapter extends ArrayAdapter<statisticInit> {
    private Context mContext;
    private List<statisticInit> mData;
    private boolean[] mIsVisible;
    private int mLayoutResourceId;
    private int[] mTags;
    private static final Typeface mFont = Typeface.create("sans-serif-condensed", 0);
    public static final String[] color_code = {"#009688", "#ff5722", "#8bc34a", "#03a9f4", "#e51c23", "#00bcd4", "#607d8b", "#e91e63"};
    private static final float EXTRA_COLOR_HUE_STEP = 137.508f;
    private static final float EXTRA_COLOR_SATURATION = 0.55f;
    private static final float EXTRA_COLOR_VALUE = 0.85f;

    /**
     * View holder for recycling list item views.
     */
    public static class Holder {
        TextView frequency;
        TextView percentage;
        TextView timeInState;
    }

    /**
     * Creates a statistic adapter.
     *
     * @param context the context
     * @param layoutResourceId the row layout resource
     * @param data the array of statisticInit objects to display
     */
    public StatisticAdapter(Context context, int layoutResourceId, statisticInit[] data) {
        super(context, layoutResourceId, data);
        this.mData = new ArrayList();
        this.mLayoutResourceId = layoutResourceId;
        this.mContext = context;
        this.mIsVisible = new boolean[data.length];
        this.mTags = new int[data.length];
        for (statisticInit i : data) {
            this.mData.add(i);
        }
        int i2 = 0;
        boolean[] arr$ = this.mIsVisible;
        for (boolean z : arr$) {
            this.mIsVisible[i2] = false;
            i2++;
        }
        for (int j = 0; j < this.mTags.length; j++) {
            int colorIndex = (data[j] != null && data[j].acceptedIndex >= 0) ? data[j].acceptedIndex : j;
            this.mTags[j] = getColorForIndex(colorIndex);
        }
    }

    /**
     * Returns a deterministic color for {@code index}, shared by the pie graph, its
     * center text and this adapter's rows so all three stay in the same color order.
     * Indexes within the base {@link #color_code} palette return the corresponding fixed
     * color; indexes beyond it generate additional colors with a varied hue and a stable
     * saturation/value, so additional visible frequency states never repeat the base
     * palette or each other.
     */
    public static int getColorForIndex(int index) {
        if (index >= 0 && index < color_code.length) {
            return Color.parseColor(color_code[index]);
        }
        int extraIndex = index - color_code.length;
        float hue = (extraIndex * EXTRA_COLOR_HUE_STEP) % 360.0f;
        return Color.HSVToColor(new float[]{hue, EXTRA_COLOR_SATURATION, EXTRA_COLOR_VALUE});
    }

    /**
     * Returns the view for displaying a statistic row at the specified position with
     * color-coded text and fade-in animation.
     *
     * @param position the position of the item
     * @param convertView the recycled view to reuse if available
     * @param parent the parent view group
     * @return the configured view for the row
     */
    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        View row = convertView;
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
            row = inflater.inflate(this.mLayoutResourceId, parent, false);
            holder = new Holder();
            holder.frequency = (TextView) row.findViewById(R.id.frequency);
            holder.timeInState = (TextView) row.findViewById(R.id.timeInState);
            holder.percentage = (TextView) row.findViewById(R.id.percentage);
            holder.frequency.setTypeface(mFont);
            holder.timeInState.setTypeface(mFont);
            holder.percentage.setTypeface(mFont);
            row.setTag(holder);
        } else {
            holder = (Holder) row.getTag();
        }
        statisticInit overview = this.mData.get(position);
        if (overview != null) {
            if (this.mData != null) {
                if (overview.mFrequency != null) {
                    holder.frequency.setText(overview.mFrequency);
                }
                if (overview.mTimeInState != null) {
                    holder.timeInState.setText(overview.mTimeInState);
                }
                if (overview.mPercentage != null) {
                    holder.percentage.setText(overview.mPercentage);
                }
                holder.frequency.setTextColor(this.mTags[position]);
                holder.timeInState.setTextColor(this.mTags[position]);
                holder.percentage.setTextColor(this.mTags[position]);
            } else {
                Log.e("Aero", "No Data found for adapter.");
            }
            if (!this.mIsVisible[position]) {
                int delay = position * 50;
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.bottom_up);
                animation.setStartOffset(delay);
                row.setAnimation(animation);
                this.mIsVisible[position] = true;
            }
        }
        return row;
    }
}
