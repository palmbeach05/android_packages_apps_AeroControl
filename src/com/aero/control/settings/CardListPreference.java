package com.aero.control.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * A list selection preference displayed inside a Material Design card container.
 * Provides a public click dispatch method for use by {@link SettingsCardAdapter}
 * to open the selection dialog from custom card-based layouts.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
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

    /**
     * Triggers this preference's click handling to display its selection dialog.
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
