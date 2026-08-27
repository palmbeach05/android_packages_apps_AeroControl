package com.aero.control.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class CardPreference extends Preference {

    public CardPreference(Context context) {
        super(context);
    }

    public CardPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void performCardClick() {
        if (!isEnabled()) {
            return;
        }

        OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null && listener.onPreferenceClick(this)) {
            return;
        }

        onClick();
    }
}
