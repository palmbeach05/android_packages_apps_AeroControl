package com.aero.control.settings;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.aero.control.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders each {@link PreferenceCategory} of a {@link PreferenceScreen} as a single
 * list item containing the category header and its child preference rows inside one
 * shared Material card. Existing {@link Preference} instances are bound with
 * {@link Preference#getView}, preserving persistence, change listeners, dialogs,
 * icons and enabled state. {@link TwoStatePreference} and {@link ListPreference}
 * instances are instead bound to dedicated application-owned rows so their content
 * shares the same alignment inside the card.
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
            View preferenceView;
            if (preference instanceof TwoStatePreference) {
                preferenceView = getSwitchPreferenceView((TwoStatePreference) preference, items);
            } else if (preference instanceof ListPreference) {
                preferenceView = getListPreferenceView((ListPreference) preference, items);
            } else {
                preferenceView = preference.getView(null, items);
                preferenceView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        preference.performClick(null);
                    }
                });
            }
            items.addView(preferenceView);
        }

        return card;
    }

    private View getListPreferenceView(final ListPreference preference, ViewGroup parent) {
        View row = LayoutInflater.from(mContext).inflate(
                R.layout.settings_list_preference, parent, false);

        ImageView icon = (ImageView) row.findViewById(R.id.settings_list_icon);
        icon.setImageDrawable(preference.getIcon());

        TextView title = (TextView) row.findViewById(R.id.settings_list_title);
        title.setText(preference.getTitle());

        TextView summary = (TextView) row.findViewById(R.id.settings_list_summary);
        summary.setText(preference.getSummary());
        summary.setVisibility(TextUtils.isEmpty(preference.getSummary()) ? View.GONE : View.VISIBLE);

        boolean enabled = preference.isEnabled();
        row.setEnabled(enabled);
        icon.setEnabled(enabled);
        title.setEnabled(enabled);
        summary.setEnabled(enabled);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preference.performClick(null);
            }
        });
        return row;
    }

    private View getSwitchPreferenceView(final TwoStatePreference preference, ViewGroup parent) {
        final View row = LayoutInflater.from(mContext).inflate(
                R.layout.settings_switch_preference, parent, false);

        ((ImageView) row.findViewById(R.id.settings_switch_icon)).setImageDrawable(preference.getIcon());
        ((TextView) row.findViewById(R.id.settings_switch_title)).setText(preference.getTitle());

        final Switch switchWidget = (Switch) row.findViewById(R.id.settings_switch);
        switchWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                preference.performClick(null);
                bindSwitchMetadata(preference, row);
            }
        });
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchWidget.performClick();
            }
        });

        bindSwitchState(preference, row);
        return row;
    }

    private void bindSwitchState(TwoStatePreference preference, View row) {
        ((Switch) row.findViewById(R.id.settings_switch)).setChecked(preference.isChecked());
        bindSwitchMetadata(preference, row);
    }

    private void bindSwitchMetadata(TwoStatePreference preference, View row) {
        boolean enabled = preference.isEnabled();
        row.setEnabled(enabled);
        row.findViewById(R.id.settings_switch_icon).setEnabled(enabled);
        row.findViewById(R.id.settings_switch_title).setEnabled(enabled);

        Switch switchWidget = (Switch) row.findViewById(R.id.settings_switch);
        switchWidget.setEnabled(enabled);

        TextView summary = (TextView) row.findViewById(R.id.settings_switch_summary);
        summary.setEnabled(enabled);
        summary.setText(preference.getSummary());
        summary.setVisibility(TextUtils.isEmpty(preference.getSummary()) ? View.GONE : View.VISIBLE);
    }

    /**
     * Refreshes all visible preference rows to reflect updated summaries, enabled
     * states, and checked states after preference mutations.
     */
    public void refresh() {
        notifyDataSetChanged();
    }
}
