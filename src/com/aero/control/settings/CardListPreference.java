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

    /**
     * Creates a new CardListPreference.
     *
     * @param context the context in which the preference is created
     */
    public CardListPreference(Context context) {
        super(context);
    }

    /**
     * Creates a new CardListPreference with attributes from XML.
     *
     * @param context the context in which the preference is created
     * @param attrs the attributes from the XML tag
     */
    public CardListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates a new CardListPreference with attributes and default style.
     *
     * @param context the context in which the preference is created
     * @param attrs the attributes from the XML tag
     * @param defStyle the default style resource
     */
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
