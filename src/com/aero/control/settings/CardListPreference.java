package com.aero.control.settings;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class CardListPreference extends ListPreference {

    public CardListPreference(Context context) {
        super(context);
    }

    public CardListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void performCardClick() {
        if (!isEnabled()) {
            return;
        }

        onClick();

        OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null) {
            listener.onPreferenceClick(this);
        }
    }
}
