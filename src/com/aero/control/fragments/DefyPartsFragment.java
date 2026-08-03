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
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public class DefyPartsFragment extends PlaceHolderFragment {
    // Only allow the characters property values for these settings can legitimately
    // contain. This prevents shell metacharacters (;, |, &, $, `, etc.) supplied via
    // a preference value from being injected into the "setprop" root command.
    private static final Pattern SAFE_PROP_VALUE = Pattern.compile("^[a-zA-Z0-9_.:-]{1,64}$");
    private CustomTextPreference button_brightness;
    private CustomListPreference led_charging;
    private CustomListPreference multi_touch;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.defy_parts);
        PreferenceScreen root = getPreferenceScreen();
        PreferenceCategory defyParts = (PreferenceCategory) root.findPreference("defy_parts");
        String charger = AeroActivity.shell.getRootInfo("getprop ", FilePath.PROP_CHARGE_LED_MODE);
        String multitouch = AeroActivity.shell.getRootInfo("getprop ", FilePath.PROP_TOUCH_POINTS);
        String brightness = AeroActivity.shell.getRootInfo("getprop", FilePath.PROP_BUTTON_BRIGHTNESS);
        this.led_charging = new CustomListPreference(getActivity());
        this.led_charging.setName("led_charging");
        this.led_charging.setSummary(R.string.pref_charging_led_summary);
        this.led_charging.setTitle(R.string.pref_charging_led);
        this.led_charging.setDialogTitle(R.string.pref_charging_led);
        this.led_charging.setOrder(1);
        defyParts.addPreference(this.led_charging);
        this.button_brightness = new CustomTextPreference(getActivity());
        this.button_brightness.setName("button_brightness");
        this.button_brightness.setPrefSummary(brightness);
        this.button_brightness.setPrefText(getText(R.string.pref_button_brightness).toString());
        this.button_brightness.setDialogTitle(getText(R.string.pref_button_brightness).toString());
        this.button_brightness.setSummary(brightness);
        this.button_brightness.setTitle(getText(R.string.pref_button_brightness).toString());
        this.button_brightness.getEditText().setInputType(2);
        this.button_brightness.setOrder(5);
        defyParts.addPreference(this.button_brightness);
        this.multi_touch = new CustomListPreference(getActivity());
        this.multi_touch.setName("multi_touch");
        this.multi_touch.setSummary(R.string.pref_multitouch_summary);
        this.multi_touch.setTitle(R.string.pref_multitouch);
        this.multi_touch.setDialogTitle(R.string.pref_multitouch);
        this.multi_touch.setOrder(10);
        defyParts.addPreference(this.multi_touch);
        if (Build.MODEL.equals("DROIDX")) {
            this.led_charging.setEntryValues(R.array.charge_led_mode_values_droidx);
            this.led_charging.setEntries(R.array.charge_led_mode_entries_droidx);
        } else {
            this.led_charging.setEntryValues(R.array.charge_led_mode_values);
            this.led_charging.setEntries(R.array.charge_led_mode_entries);
        }
        if (charger.length() > 1) {
            this.led_charging.setValue(charger);
            this.led_charging.setSummary(charger);
        } else {
            this.led_charging.setEnabled(false);
        }
        this.multi_touch.setEntryValues(R.array.touch_point_values);
        this.multi_touch.setEntries(R.array.touch_point_values);
        if (charger.length() > 1) {
            this.multi_touch.setValue(multitouch);
            this.multi_touch.setSummary(multitouch);
        } else {
            this.multi_touch.setEnabled(false);
        }
        if (charger.length() > 1) {
            this.button_brightness.setText(brightness);
        } else {
            this.button_brightness.setEnabled(false);
        }
        this.led_charging.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.DefyPartsFragment.1
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String value = o.toString();
                if (!DefyPartsFragment.this.changePreference(preference, value, FilePath.PROP_CHARGE_LED_MODE)) {
                    return false;
                }
                DefyPartsFragment.this.led_charging.setValue(value);
                DefyPartsFragment.this.led_charging.setSummary(value);
                return true;
            }
        });
        this.multi_touch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.DefyPartsFragment.2
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!DefyPartsFragment.this.changePreference(preference, o, FilePath.PROP_TOUCH_POINTS)) {
                    return false;
                }
                DefyPartsFragment.this.multi_touch.setValue(o.toString());
                DefyPartsFragment.this.multi_touch.setSummary(o.toString());
                return true;
            }
        });
        this.button_brightness.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.DefyPartsFragment.3
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!DefyPartsFragment.this.changePreference(preference, o, FilePath.PROP_BUTTON_BRIGHTNESS)) {
                    return false;
                }
                DefyPartsFragment.this.button_brightness.setText(o.toString());
                DefyPartsFragment.this.button_brightness.setPrefSummary(o.toString());
                return true;
            }
        });
    }

    /**
     * Applies a preference-driven value via a root "setprop" call.
     *
     * The value is validated against a strict allow-list of characters before being
     * concatenated into the shell command, since it previously flowed unsanitized from
     * user-editable preferences straight into a privileged root command (command injection).
     *
     * @return true if the value was valid and the command was executed, false if the value
     *         was rejected (in which case the calling listener should also return false so
     *         the invalid value is not persisted).
     */
    /* JADX INFO: Access modifiers changed from: private */
    public boolean changePreference(Preference preference, Object o, String file) {
        String value = o == null ? null : o.toString();
        if (value == null || !SAFE_PROP_VALUE.matcher(value).matches()) {
            Toast.makeText(getActivity(), R.string.error_detected, 0).show();
            return false;
        }
        String[] command = {"setprop " + file + " " + value};
        AeroActivity.shell.setRootInfo(command);
        Toast.makeText(getActivity(), R.string.need_reboot, 0).show();
        return true;
    }
}
