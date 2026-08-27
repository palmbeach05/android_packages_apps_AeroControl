package com.aero.control.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

/**
 * A two-state toggle preference displayed inside a Material Design card container.
 * Provides a public click dispatch method for use by {@link SettingsCardAdapter}
 * to toggle the switch state from custom card-based layouts.
 */
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

    /**
     * Triggers this preference's click handling to toggle its checked state.
     * Invokes {@link #onClick()} and notifies any registered
     * {@link OnPreferenceClickListener}. Does nothing if the preference is disabled.
     */
    public void performCardClick() {
        if (!isEnabled()) {
            return;
        }

        OnPreferenceClickListener listener = getOnPreferenceClickListener();
        if (listener != null && listener.onPreferenceClick(this)) {
            return;
        }

        onClick();
            if (getIntent() != null) {
                getContext().startActivity(getIntent());
        }
    }
}
