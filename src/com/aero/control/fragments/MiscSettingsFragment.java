package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.Android.CustomTextPreference;
import com.aero.control.helpers.FileManager.FileManager;
import com.aero.control.helpers.FileManager.FileManagerListener;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.LedController;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.SysfsResult;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Fragment for miscellaneous system settings including TCP congestion control,
 * vibrator strength, thermal throttling, headset volume boost, and Motorola Defy
 * hardware controls. Provides save/apply functionality for boot and profile persistence.
 */
public class MiscSettingsFragment extends PlaceHolderFragment implements FileManagerListener {
    public static final String FILENAME_MISC = "firstrun_misc";
    private static final String MISC_SETTINGS_STORAGE = "miscSettingsStorage";
    // Only allow characters that legitimate property/sysfs values contain to prevent
    // shell metacharacter injection in root commands.
    private static final Pattern SAFE_PROP_VALUE = Pattern.compile("^[a-zA-Z0-9_.:-]{1,64}$");

    private PreferenceCategory PrefCat;
    private PreferenceCategory mDefyPartsCat;
    private CustomTextPreference button_brightness;
    private CustomListPreference led_charging;
    private CustomListPreference multi_touch;

    private Context mContext;
    private Dialog mFileDialog;
    private PreferenceHandler mHandler;
    private FileManager mLocalFolders;
    private PreferenceCategory mMiscCat;
    private SharedPreferences mMiscSettings;
    private ArrayList<String> mNameList;
    private ArrayList<String> mParaList;
    private SharedPreferences mPrefs;
    private ShowcaseView mShowCase;
    private PreferenceScreen root;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.empty_preference);
        this.root = getPreferenceScreen();
        this.mContext = getActivity();
        this.mMiscSettings = this.mContext.getSharedPreferences(MISC_SETTINGS_STORAGE, 0);
        loadParalist();
        loadSettings();
		if ("MB525".equals(Build.MODEL)
				|| "MB526".equals(Build.MODEL)
				|| "DROIDX".equals(Build.MODEL)) {
	        initDefyParts();
		}
    }

    /**
     * Builds and populates the Defy/Droid X hardware preferences category and loads current
     * sysfs values off the UI thread.
     */
    private void initDefyParts() {
        if (this.mContext == null || this.root == null) {
            return;
        }
        if (this.mDefyPartsCat != null) {
            this.root.removePreference(this.mDefyPartsCat);
        }

        this.mDefyPartsCat = new PreferenceCategory(this.mContext);
        this.mDefyPartsCat.setKey("defy_parts");
        this.mDefyPartsCat.setTitle("Defy/Droid X Parts");
        this.root.addPreference(this.mDefyPartsCat);

        this.led_charging = new CustomListPreference(this.mContext);
        this.led_charging.setName("led_charging");
        this.led_charging.setTitle(R.string.pref_charging_led);
        this.led_charging.setDialogTitle(R.string.pref_charging_led);
        this.led_charging.setOrder(1);
        this.led_charging.setEnabled(false);
        this.mDefyPartsCat.addPreference(this.led_charging);

        this.button_brightness = new CustomTextPreference(this.mContext);
        this.button_brightness.setName("button_brightness");
        this.button_brightness.setHelpLookupKey("button_brightness");
        this.button_brightness.setPrefText(this.mContext.getText(R.string.pref_button_brightness).toString());
        this.button_brightness.setDialogTitle(this.mContext.getText(R.string.pref_button_brightness).toString());
        this.button_brightness.setTitle(this.mContext.getText(R.string.pref_button_brightness).toString());
        this.button_brightness.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        this.button_brightness.setOrder(5);
        this.button_brightness.setEnabled(false);
        this.mDefyPartsCat.addPreference(this.button_brightness);

        this.multi_touch = new CustomListPreference(this.mContext);
        this.multi_touch.setName("multi_touch");
        this.multi_touch.setTitle(R.string.pref_multitouch);
        this.multi_touch.setDialogTitle(R.string.pref_multitouch);
        this.multi_touch.setOrder(10);
        this.multi_touch.setEnabled(false);
        this.mDefyPartsCat.addPreference(this.multi_touch);

        if ("DROIDX".equals(Build.MODEL)) {
            this.led_charging.setEntryValues(R.array.charge_led_mode_values_droidx);
            this.led_charging.setEntries(R.array.charge_led_mode_entries_droidx);
        } else {
            this.led_charging.setEntryValues(R.array.charge_led_mode_values);
            this.led_charging.setEntries(R.array.charge_led_mode_entries);
        }
        this.multi_touch.setEntryValues(R.array.touch_point_values);
        this.multi_touch.setEntries(R.array.touch_point_values);

        this.led_charging.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o == null) {
                    return false;
                }
                String value = o.toString();
                if (!changeDefyPreference(preference, value, LedController.Property.CHARGING_MODE)) {
                    return false;
                }
                led_charging.setValue(value);
                led_charging.setSummary(value);
                return true;
            }
        });

        this.multi_touch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o == null) {
                    return false;
                }
                String value = o.toString();
                if (!changeDefyPreference(preference, value, LedController.Property.MULTI_TOUCH_POINTS)) {
                    return false;
                }
                multi_touch.setValue(value);
                multi_touch.setSummary(value);
                return true;
            }
        });

        this.button_brightness.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o == null) {
                    return false;
                }
                String value = o.toString();
                if (!changeDefyPreference(preference, value, LedController.Property.BUTTON_BRIGHTNESS)) {
                    return false;
                }
                button_brightness.setText(value);
                button_brightness.setPrefSummary(value);
                return true;
            }
        });

        final String unavailableMarker = this.mContext.getText(R.string.unavailable).toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (AeroActivity.hardware == null || AeroActivity.hardware.led() == null) {
                    return;
                }
                final SysfsResult<String> charger =
                        AeroActivity.hardware.led().read(LedController.Property.CHARGING_MODE);
                final SysfsResult<String> multitouch =
                        AeroActivity.hardware.led().read(LedController.Property.MULTI_TOUCH_POINTS);
                final SysfsResult<String> brightness =
                        AeroActivity.hardware.led().read(LedController.Property.BUTTON_BRIGHTNESS);

                if (!isAdded() || getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        if (charger.isSuccess()) {
                            led_charging.setEnabled(true);
                            led_charging.setValue(charger.getValue());
                            led_charging.setSummary(charger.getValue());
                        } else {
                            led_charging.setEnabled(false);
                            led_charging.setSummary(unavailableMarker);
                        }

                        if (multitouch.isSuccess()) {
                            multi_touch.setEnabled(true);
                            multi_touch.setValue(multitouch.getValue());
                            multi_touch.setSummary(multitouch.getValue());
                        } else {
                            multi_touch.setEnabled(false);
                            multi_touch.setSummary(unavailableMarker);
                        }

                        if (brightness.isSuccess()) {
                            button_brightness.setEnabled(true);
                            button_brightness.setText(brightness.getValue());
                            button_brightness.setPrefSummary(brightness.getValue());
                            button_brightness.setSummary(brightness.getValue());
                        } else {
                            button_brightness.setEnabled(false);
                            button_brightness.setSummary(unavailableMarker);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Validates and writes a Defy hardware property through the LED controller.
     */
    private boolean changeDefyPreference(final Preference preference, final Object o,
            final LedController.Property property) {
        final String value = o == null ? null : o.toString();
        if (value == null || !SAFE_PROP_VALUE.matcher(value).matches() || property == null) {
            if (isAdded() && getActivity() != null) {
                Toast.makeText(getActivity(), R.string.error_detected, Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (AeroActivity.hardware == null || AeroActivity.hardware.led() == null) {
                    return;
                }
                final SysfsResult<String> result = AeroActivity.hardware.led().write(property, value);
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded() || getActivity() == null) {
                            return;
                        }
                        if (!result.isSuccess()) {
                            Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(getActivity(), R.string.need_reboot, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
        return true;
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext.getApplicationContext());
        inflater.inflate(R.menu.misc_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_item /* 2131099750 */:
                if (this.mMiscCat != null) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this.mContext);
                    ArrayList<String> allMiscSettings = new ArrayList<>();
                    final ArrayList<Boolean> miscSettingsDelete = new ArrayList<>();
                    for (int i = 0; i < this.mMiscCat.getPreferenceCount(); i++) {
                        allMiscSettings.add(this.mMiscCat.getPreference(i).getTitle().toString());
                    }
                    if (allMiscSettings.size() == 0) {
                        Toast.makeText(this.mContext, R.string.pref_misc_no_settings, Toast.LENGTH_LONG).show();
                    } else {
                        for (int idx = 0; idx < allMiscSettings.size(); idx++) {
                            miscSettingsDelete.add(false);
                        }
                        final String[] preferenceData = allMiscSettings.toArray(new String[0]);
                        dialog.setTitle(R.string.pref_misc_delete_misc);
                        dialog.setIcon(R.drawable.warning);
                        dialog.setMultiChoiceItems(preferenceData, (boolean[]) null, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i2, boolean b) {
                                miscSettingsDelete.set(i2, b);
                            }
                        }).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog2, int id) {
                                SharedPreferences.Editor editor = MiscSettingsFragment.this.mMiscSettings.edit();
                                SharedPreferences.Editor aero_editor = MiscSettingsFragment.this.mPrefs.edit();
                                Map<String, ?> keys = MiscSettingsFragment.this.mMiscSettings.getAll();
                                for (int i2 = 0; i2 < preferenceData.length; i2++) {
                                    if (miscSettingsDelete.get(i2).booleanValue()) {
                                        for (Map.Entry<String, ?> entry : keys.entrySet()) {
                                            String key = entry.getKey();
                                            String value = entry.getValue().toString();
                                            if (preferenceData[i2].equals(value)) {
                                                editor.remove(key).commit();
                                                aero_editor.remove(key).commit();
                                            }
                                        }
                                    }
                                }
                                MiscSettingsFragment.this.root.removePreference(MiscSettingsFragment.this.mMiscCat);
                                MiscSettingsFragment.this.mMiscCat = null;
                                MiscSettingsFragment.this.initMisc();
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog2, int id) {
                            }
                        });
                        dialog.create().show();
                    }
                }
                break;
            case R.id.action_add_item /* 2131099751 */:
                this.mLocalFolders = new FileManager(this.mContext, null);
                this.mLocalFolders.setIFolderItemListener(this);
                this.mLocalFolders.setDir("/");
                if (this.mFileDialog == null) {
                    this.mFileDialog = new Dialog(this.mContext);
                    ViewGroup.LayoutParams abc = new ViewGroup.LayoutParams(-2, -1);
                    this.mFileDialog.addContentView(this.mLocalFolders, abc);
                    this.mFileDialog.setTitle("/");
                    this.mLocalFolders.setDialog(this.mFileDialog);
                }
                this.mFileDialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME_MISC)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_your_settings, R.string.showcase_your_settings_summary);
        }
    }

    /**
     * Displays the first-time tutorial showcase overlay for the misc settings fragment.
     *
     * @param header the resource ID for the showcase title
     * @param content the resource ID for the showcase content text
     */
    public void DrawFirstStart(int header, int content) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(FILENAME_MISC, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        Target homeTarget = new Target() {
            @Override
            public Point getPoint() {
                int actionBarSize = 96;
                try {
                    int height = MiscSettingsFragment.this.getActivity().findViewById(R.id.action_add_item).getHeight();
                    if (height > 0) {
                        actionBarSize = height;
                    }
                } catch (NullPointerException e) {
                }
                int x = MiscSettingsFragment.this.getResources().getDisplayMetrics().widthPixels - (actionBarSize / 2);
                int y = actionBarSize / 2;
                return new Point(x, y);
            }
        };
        this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

    @Override // com.aero.control.helpers.FileManager.FileManagerListener
    public void OnCannotFileRead(File file) {
    }

    @Override // com.aero.control.helpers.FileManager.FileManagerListener
    public void OnFileClicked(File file) {
        for (int i = 0; i < this.mMiscCat.getPreferenceCount(); i++) {
            if (file.toString().contains(this.mMiscCat.getPreference(i).getTitle().toString())) {
                Toast.makeText(this.mContext, "This tunable was already added!", Toast.LENGTH_LONG).show();
                this.mFileDialog.dismiss();
                return;
            }
        }
        this.mHandler.genPrefFromSingleFile(file.toString());
        String[] array = file.toString().split("/");
        String paraName = "";
        int i2 = 0;
        for (String a : array) {
            if (array.length - 1 == i2) {
                paraName = a;
            }
            i2++;
        }
        this.mMiscSettings.edit().putString(file.toString(), paraName).commit();
        this.root.addPreference(this.mMiscCat);
        this.mFileDialog.dismiss();
    }

    /**
     * Initializes the misc settings section by loading previously saved
     * custom tunable parameters from shared preferences.
     */
    public void initMisc() {
        Map<String, ?> keys = this.mMiscSettings.getAll();
        if (this.mMiscCat == null) {
            this.mMiscCat = new PreferenceCategory(this.mContext);
            this.mMiscCat.setTitle(R.string.pref_misc_your_settings);
            this.root.addPreference(this.mMiscCat);
            this.mHandler = new PreferenceHandler(this.mContext, this.mMiscCat, getPreferenceManager());
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                String key = entry.getKey();
                this.mHandler.genPrefFromSingleFile(key);
            }
        } else {
            this.root.addPreference(this.mMiscCat);
        }
        if (this.mHandler == null) {
            this.mHandler = new PreferenceHandler(this.mContext, this.mMiscCat, getPreferenceManager());
        }
        if (this.mMiscCat.getPreferenceCount() == 0) {
            this.root.removePreference(this.mMiscCat);
        }
    }

    private void loadParalist() {
        this.mParaList = new ArrayList<>();
        this.mNameList = new ArrayList<>();
        this.mNameList.add("vtg_level");
        this.mParaList.add(FilePath.MISC_VIBRATOR_CONTROL);
        this.mNameList.add("amp");
        this.mParaList.add(FilePath.MISC_VIBRATOR_CONTROL);
        this.mNameList.add("temp_threshold");
        this.mParaList.add(FilePath.MISC_THERMAL_CONTROL);
        this.mNameList.add("volume_boost");
        this.mParaList.add(FilePath.MISC_HEADSET_VOLUME_BOOST);
        setHasOptionsMenu(true);
    }

    /**
     * Loads and displays the misc settings preferences including custom tunables,
     * TCP congestion control, and other system parameters.
     */
    public void loadSettings() {
        if (this.PrefCat != null) {
            this.root.removePreference(this.PrefCat);
        }
        final CustomListPreference tcpPreference = new CustomListPreference(this.mContext);
        this.PrefCat = new PreferenceCategory(this.mContext);
        this.PrefCat.setTitle(R.string.pref_misc_settings);
        this.root.addPreference(this.PrefCat);
        initMisc();
        try {
            PreferenceHandler h = new PreferenceHandler(this.mContext, this.PrefCat, getPreferenceManager());
            h.genPrefFromFiles((String[]) this.mNameList.toArray(new String[0]), (String[]) this.mParaList.toArray(new String[0]), false);
        } catch (NullPointerException e) {
            Log.e("Aero", "I couldn't get any files!", e);
        }
        tcpPreference.setName("tcp_congestion");
        tcpPreference.setTitle(R.string.pref_misc_tcp_congestion);
        tcpPreference.setDialogTitle(R.string.pref_misc_tcp_congestion);
        SysfsResult<String> currentControl =
                AeroActivity.hardware.network().readCongestionControl();
        SysfsResult<String[]> availableControls =
                AeroActivity.hardware.network().readAvailableCongestionControls();
        if (currentControl.isSuccess() && availableControls.isSuccess()) {
            tcpPreference.setSummary(currentControl.getValue());
            tcpPreference.setValue(currentControl.getValue());
            tcpPreference.setEntries(availableControls.getValue());
            tcpPreference.setEntryValues(availableControls.getValue());
            this.PrefCat.addPreference(tcpPreference);
        } else {
            Log.w("Aero", "TCP congestion control is unavailable: "
                    + (!currentControl.isSuccess()
                    ? currentControl.getError() : availableControls.getError()));
        }
        tcpPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String a = (String) o;
                final String previousValue = tcpPreference.getValue();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final SysfsResult<String> writeResult =
                                AeroActivity.hardware.network().writeCongestionControl(a);
                        final SysfsResult<String> readbackResult =
                                AeroActivity.hardware.network().readCongestionControl();
                        if (!MiscSettingsFragment.this.isAdded()) {
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!MiscSettingsFragment.this.isAdded()) {
                                    return;
                                }
                                if (readbackResult.isSuccess()) {
                                    tcpPreference.setValue(readbackResult.getValue());
                                    tcpPreference.setSummary(readbackResult.getValue());
                                } else if (previousValue != null) {
                                    tcpPreference.setValue(previousValue);
                                    tcpPreference.setSummary(previousValue);
                                }
                                if (!readbackResult.isSuccess()
                                        || !a.equals(readbackResult.getValue())) {
                                    Log.e("Aero", "Failed to change TCP congestion control: "
                                            + (!readbackResult.isSuccess()
                                            ? readbackResult.getError()
                                            : "readback was " + readbackResult.getValue()));
                                    Toast.makeText(MiscSettingsFragment.this.mContext, R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                if (!writeResult.isSuccess()) {
                                    Log.w("Aero", "TCP congestion control readback confirms "
                                            + "the requested value despite a write verification "
                                            + "error: " + writeResult.getError());
                                }
                                if (tcpPreference.isChecked().booleanValue()) {
                                    boolean saved = PreferenceManager
                                            .getDefaultSharedPreferences(mContext).edit()
                                            .putString(tcpPreference.getName(),
                                                    readbackResult.getValue())
                                            .commit();
                                    if (!saved) {
                                        Log.e("Aero", "Failed to save TCP congestion control");
                                        Toast.makeText(MiscSettingsFragment.this.mContext,
                                                R.string.storage_operation_failed,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                    }
                }).start();
                return true;
            }
        });
    }
}
