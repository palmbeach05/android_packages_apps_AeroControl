package com.aero.control.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

public class CardSwitchPreference extends SwitchPreference {

    public CardSwitchPreference(Context context) {
        super(context);
    }

    public CardSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
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
