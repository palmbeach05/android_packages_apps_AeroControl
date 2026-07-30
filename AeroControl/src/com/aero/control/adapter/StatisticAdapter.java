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

/* JADX INFO: loaded from: classes.dex */
public class StatisticAdapter extends ArrayAdapter<statisticInit> {
    private Context mContext;
    private List<statisticInit> mData;
    private int mIndex;
    private boolean[] mIsVisible;
    private int mLayoutResourceId;
    private String[] mTags;
    private static final Typeface mFont = Typeface.create("sans-serif-condensed", 0);
    public static final String[] color_code = {"#009688", "#ff5722", "#8bc34a", "#03a9f4", "#e51c23", "#00bcd4", "#607d8b", "#e91e63"};

    public static class Holder {
        TextView frequency;
        TextView percentage;
        TextView timeInState;
    }

    public StatisticAdapter(Context context, int layoutResourceId, statisticInit[] data) {
        super(context, layoutResourceId, data);
        this.mData = new ArrayList();
        this.mIndex = 0;
        this.mTags = new String[0];
        this.mLayoutResourceId = layoutResourceId;
        this.mContext = context;
        this.mIsVisible = new boolean[data.length];
        this.mTags = new String[data.length];
        for (statisticInit i : data) {
            this.mData.add(i);
        }
        int i2 = 0;
        boolean[] arr$ = this.mIsVisible;
        for (boolean z : arr$) {
            this.mIsVisible[i2] = false;
            i2++;
        }
        int i3 = 0;
        int j = 0;
        String[] arr$2 = this.mTags;
        for (String str : arr$2) {
            if (i3 == color_code.length) {
                i3 = 0;
            }
            this.mTags[j] = color_code[i3];
            i3++;
            j++;
        }
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        View row = convertView;
        if (this.mIndex == color_code.length) {
            this.mIndex = 0;
        }
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
            holder.frequency.setTextColor(Color.parseColor(color_code[this.mIndex]));
            holder.timeInState.setTextColor(Color.parseColor(color_code[this.mIndex]));
            holder.percentage.setTextColor(Color.parseColor(color_code[this.mIndex]));
            this.mIndex++;
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
                if (this.mTags[position] != null) {
                    holder.frequency.setTextColor(Color.parseColor(this.mTags[position]));
                    holder.timeInState.setTextColor(Color.parseColor(this.mTags[position]));
                    holder.percentage.setTextColor(Color.parseColor(this.mTags[position]));
                }
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
