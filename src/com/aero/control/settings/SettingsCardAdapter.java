package com.aero.control.settings;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.aero.control.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders each {@link PreferenceCategory} of a {@link PreferenceScreen} as a single
 * list item containing the category header and its child preference rows inside one
 * shared Material card. Existing {@link Preference} instances are bound with
 * {@link Preference#getView}, preserving persistence, change listeners, dialogs,
 * icons and enabled state.
 */
class SettingsCardAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<PreferenceCategory> mCategories = new ArrayList<>();

    SettingsCardAdapter(Context context, PreferenceScreen preferenceScreen) {
        mContext = context;
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (preference instanceof PreferenceCategory) {
                mCategories.add((PreferenceCategory) preference);
            }
        }
    }

    @Override
    public int getCount() {
        return mCategories.size();
    }

    @Override
    public PreferenceCategory getItem(int position) {
        return mCategories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View card = LayoutInflater.from(mContext).inflate(R.layout.settings_category_card, parent, false);

        PreferenceCategory category = mCategories.get(position);
        ((TextView) card.findViewById(R.id.settings_card_header)).setText(category.getTitle());

        LinearLayout items = (LinearLayout) card.findViewById(R.id.settings_card_items);
        items.removeAllViews();
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            final Preference preference = category.getPreference(i);
            View preferenceView = preference.getView(null, items);
            preferenceView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    preference.performClick(null);
                }
            });
            items.addView(preferenceView);
        }

        return card;
    }
}