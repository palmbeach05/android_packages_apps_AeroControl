package com.aero.control.fragments;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.Android.CustomTextPreference;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.LedController;
import com.aero.control.helpers.SysfsResult;
import java.util.regex.Pattern;

/**
 * Fragment for Motorola Defy-specific settings including display color control,
 * RGB calibration, sweep2wake, and doubletap2wake touch gesture features.
 */
public class DefyPartsFragment extends PlaceHolderFragment {
    // Only allow the characters property values for these settings can legitimately
    // contain. This prevents shell metacharacters (;, |, &, $, `, etc.) supplied via
    // a preference value from being injected into the "setprop" root command.
    private static final Pattern SAFE_PROP_VALUE = Pattern.compile("^[a-zA-Z0-9_.:-]{1,64}$");
    private CustomTextPreference button_brightness;
    private CustomListPreference led_charging;
    private CustomListPreference multi_touch;

    /**
     * Initializes the fragment and creates preferences for Defy-specific hardware settings.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.defy_parts);
        PreferenceScreen root = getPreferenceScreen();
        PreferenceCategory defyParts = (PreferenceCategory) root.findPreference("defy_parts");
        this.led_charging = new CustomListPreference(getActivity());
        this.led_charging.setName("led_charging");
        this.led_charging.setTitle(R.string.pref_charging_led);
        this.led_charging.setDialogTitle(R.string.pref_charging_led);
        this.led_charging.setOrder(1);
        this.led_charging.setEnabled(false);
        defyParts.addPreference(this.led_charging);
        this.button_brightness = new CustomTextPreference(getActivity());
        this.button_brightness.setName("button_brightness");
        this.button_brightness.setHelpLookupKey("button_brightness");
        this.button_brightness.setPrefText(getText(R.string.pref_button_brightness).toString());
        this.button_brightness.setDialogTitle(getText(R.string.pref_button_brightness).toString());
        this.button_brightness.setTitle(getText(R.string.pref_button_brightness).toString());
        this.button_brightness.getEditText().setInputType(2);
        this.button_brightness.setOrder(5);
        this.button_brightness.setEnabled(false);
        defyParts.addPreference(this.button_brightness);
        this.multi_touch = new CustomListPreference(getActivity());
        this.multi_touch.setName("multi_touch");
        this.multi_touch.setTitle(R.string.pref_multitouch);
        this.multi_touch.setDialogTitle(R.string.pref_multitouch);
        this.multi_touch.setOrder(10);
        this.multi_touch.setEnabled(false);
        defyParts.addPreference(this.multi_touch);
        if (Build.MODEL.equals("DROIDX")) {
            this.led_charging.setEntryValues(R.array.charge_led_mode_values_droidx);
            this.led_charging.setEntries(R.array.charge_led_mode_entries_droidx);
        } else {
            this.led_charging.setEntryValues(R.array.charge_led_mode_values);
            this.led_charging.setEntries(R.array.charge_led_mode_entries);
        }
        this.multi_touch.setEntryValues(R.array.touch_point_values);
        this.multi_touch.setEntries(R.array.touch_point_values);
        this.led_charging.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.DefyPartsFragment.1
            /**
             * Handles LED charging mode preference changes.
             *
             * @param preference the preference that changed
             * @param o the new value
             * @return true if the change should be persisted
             */
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String value = o.toString();
                if (!DefyPartsFragment.this.changePreference(preference, value,
                        LedController.Property.CHARGING_MODE)) {
                    return false;
                }
                DefyPartsFragment.this.led_charging.setValue(value);
                DefyPartsFragment.this.led_charging.setSummary(value);
                return true;
            }
        });
        this.multi_touch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.DefyPartsFragment.2
            /**
             * Handles multi-touch preference changes.
             *
             * @param preference the preference that changed
             * @param o the new value
             * @return true if the change should be persisted
             */
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!DefyPartsFragment.this.changePreference(preference, o,
                        LedController.Property.MULTI_TOUCH_POINTS)) {
                    return false;
                }
                DefyPartsFragment.this.multi_touch.setValue(o.toString());
                DefyPartsFragment.this.multi_touch.setSummary(o.toString());
                return true;
            }
        });
        this.button_brightness.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.DefyPartsFragment.3
            /**
             * Handles button brightness preference changes.
             *
             * @param preference the preference that changed
             * @param o the new value
             * @return true if the change should be persisted
             */
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!DefyPartsFragment.this.changePreference(preference, o,
                        LedController.Property.BUTTON_BRIGHTNESS)) {
                    return false;
                }
                DefyPartsFragment.this.button_brightness.setText(o.toString());
                DefyPartsFragment.this.button_brightness.setPrefSummary(o.toString());
                return true;
            }
        });
        // Root-backed operations can block on a permission prompt. Do this
        // off the main thread, then apply results only if the fragment is
        // still attached.
        final AeroActivity activity = (AeroActivity) getActivity();
        final String unavailableMarker = activity.getText(R.string.unavailable).toString();
        new Thread(new Runnable() {
            /**
             * Loads current property values from the system in a background thread.
             */
            @Override // java.lang.Runnable
            public void run() {
                final SysfsResult<String> charger = AeroActivity.hardware.led().read(
                        LedController.Property.CHARGING_MODE);
                final SysfsResult<String> multitouch = AeroActivity.hardware.led().read(
                        LedController.Property.MULTI_TOUCH_POINTS);
                final SysfsResult<String> brightness = AeroActivity.hardware.led().read(
                        LedController.Property.BUTTON_BRIGHTNESS);
                activity.runOnUiThread(new Runnable() {
                    /**
                     * Updates UI with loaded property values on the main thread.
                     */
                    @Override // java.lang.Runnable
                    public void run() {
                        if (!DefyPartsFragment.this.isAdded()) {
                            return;
                        }
                        if (charger.isSuccess()) {
                            DefyPartsFragment.this.led_charging.setEnabled(true);
                            DefyPartsFragment.this.led_charging.setValue(charger.getValue());
                            DefyPartsFragment.this.led_charging.setSummary(charger.getValue());
                        } else {
                            DefyPartsFragment.this.led_charging.setEnabled(false);
                            DefyPartsFragment.this.led_charging.setSummary(unavailableMarker);
                        }
                        if (multitouch.isSuccess()) {
                            DefyPartsFragment.this.multi_touch.setEnabled(true);
                            DefyPartsFragment.this.multi_touch.setValue(multitouch.getValue());
                            DefyPartsFragment.this.multi_touch.setSummary(multitouch.getValue());
                        } else {
                            DefyPartsFragment.this.multi_touch.setEnabled(false);
                            DefyPartsFragment.this.multi_touch.setSummary(unavailableMarker);
                        }
                        if (brightness.isSuccess()) {
                            DefyPartsFragment.this.button_brightness.setEnabled(true);
                            DefyPartsFragment.this.button_brightness.setText(brightness.getValue());
                            DefyPartsFragment.this.button_brightness.setPrefSummary(brightness.getValue());
                            DefyPartsFragment.this.button_brightness.setSummary(brightness.getValue());
                        } else {
                            DefyPartsFragment.this.button_brightness.setEnabled(false);
                            DefyPartsFragment.this.button_brightness.setSummary(unavailableMarker);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Applies a value through the typed LED capability API.
     *
     * @return true if the value was valid and the command was executed, false if the value
     *         was rejected (in which case the calling listener should also return false so
     *         the invalid value is not persisted).
     */
    public boolean changePreference(final Preference preference, final Object o,
            final LedController.Property property) {
        final String value = o == null ? null : o.toString();
        if (value == null || !SAFE_PROP_VALUE.matcher(value).matches() || property == null) {
            Toast.makeText(getActivity(), R.string.error_detected, 0).show();
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SysfsResult<String> result = AeroActivity.hardware.led().write(property, value);
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isSuccess()) {
                            Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(getActivity(), R.string.need_reboot, 0).show();
                    }
                });
            }
        }).start();
        return true;
    }

}
