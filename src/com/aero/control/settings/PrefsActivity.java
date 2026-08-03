package com.aero.control.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.AboutDialog;
import com.aero.control.helpers.ThemeHelper;
import com.aero.control.helpers.Util;
import com.aero.control.service.PerAppServiceHelper;
import java.io.File;

/* JADX INFO: loaded from: classes.dex */
public class PrefsActivity extends PreferenceActivity {
    static Context context;
    public static final Typeface font = Typeface.create("sans-serif-condensed", 0);
    private ActionBar mActionBar;
    public TextView mActionBarTitle;
    public int mActionBarTitleID;
    private ListPreference mAppTheme;
    private ListPreference mBootDelay;
    private CheckBoxPreference mPerAppMonitor;
    private CheckBoxPreference mPerAppToasts;
    private CheckBoxPreference mPer_app_check;
    private CheckBoxPreference mRebootChecker;
    private int mIconTintColor;

    @Override // android.preference.PreferenceActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        TypedValue tintTypedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.aeroIconTint, tintTypedValue, true);
        this.mIconTintColor = tintTypedValue.data;
        if (getResources().getBoolean(R.bool.portrait_only)) {
            setRequestedOrientation(1);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            this.mActionBar = getActionBar();
            this.mActionBar.setIcon(android.R.color.transparent);
        } else {
            getActionBar().setIcon(R.drawable.app_icon_actionbar);
            this.mActionBarTitleID = getResources().getIdentifier("action_bar_title", "id", "android");
            this.mActionBarTitle = (TextView) findViewById(this.mActionBarTitleID);
            this.mActionBarTitle.setTypeface(font);
        }
        addPreferencesFromResource(R.layout.preference);
        setTitle(R.string.aero_settings);
        context = this;
        getActionBar().setDisplayHomeAsUpEnabled(true);
        PreferenceScreen root = getPreferenceScreen();
        if (this.mRebootChecker == null) {
            this.mRebootChecker = (CheckBoxPreference) root.findPreference("reboot_checker");
        }
        if (this.mPer_app_check == null) {
            this.mPer_app_check = (CheckBoxPreference) root.findPreference("per_app_service");
        }
        if (this.mBootDelay == null) {
            this.mBootDelay = (ListPreference) root.findPreference("boot_delay");
        }
        if (this.mAppTheme == null) {
            this.mAppTheme = (ListPreference) root.findPreference("app_theme");
        }
        if (this.mPerAppMonitor == null) {
            this.mPerAppMonitor = (CheckBoxPreference) root.findPreference("per_app_monitor");
        }
        if (this.mPerAppToasts == null) {
            this.mPerAppToasts = (CheckBoxPreference) root.findPreference("per_app_toast");
        }
        Preference resetTutorials = root.findPreference("reset_tutorials");
        Preference about = root.findPreference("about");
        Preference version = root.findPreference("version");
        Preference legal = root.findPreference("legal");
        Preference xda = root.findPreference("xda_thread");
        Preference github = root.findPreference("github_link");
        setTintedIcon(this.mRebootChecker, R.drawable.ic_action_phone);
        setCheckedState(this.mRebootChecker);
        setTintedIcon(this.mPer_app_check, R.drawable.ic_action_person);
        setTintedIcon(this.mPerAppMonitor, R.drawable.ic_action_appmonitor);
        setTintedIcon(this.mBootDelay, R.drawable.timer);
        setTintedDialogIcon(this.mBootDelay, R.drawable.timer);
        setTintedIcon(this.mPerAppToasts, R.drawable.ic_action_toast);
        setCheckedState(this.mPerAppToasts);
        setMinutes(this.mBootDelay, this.mBootDelay.getValue());
        setTintedIcon(this.mAppTheme, R.drawable.theme);
        setTintedDialogIcon(this.mAppTheme, R.drawable.theme);
        setThemeSummary(this.mAppTheme, this.mAppTheme.getValue());
        setTintedIcon(resetTutorials, R.drawable.ic_action_warning);
        setCheckedState(this.mPer_app_check);
        setCheckedState(this.mPerAppMonitor);
        setTintedIcon(version, R.drawable.rocket);
        setTintedIcon(xda, R.drawable.xda);
        setTintedIcon(github, R.drawable.github);
        if (AeroActivity.mJobManager != null) {
            this.mPerAppMonitor.setChecked(AeroActivity.mJobManager.getJobManagerState());
        }
        if (!this.mPer_app_check.isChecked()) {
            this.mPerAppMonitor.setEnabled(false);
        }
        try {
            version.setTitle("Version: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            version.setSummary("Build: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
        } catch (PackageManager.NameNotFoundException e) {
        }
        setTintedIcon(about, R.drawable.ic_action_about);
        setTintedIcon(legal, R.drawable.ic_action_legal);
        xda.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.3
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("http://forum.xda-developers.com/showthread.php?t=2483827");
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                PrefsActivity.this.startActivity(intent);
                return true;
            }
        });
        github.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.4
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                Uri uri = Uri.parse("https://github.com/Blechd0se/android_packages_apps_AeroControl");
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                PrefsActivity.this.startActivity(intent);
                return true;
            }
        });
        this.mRebootChecker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.5
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                PrefsActivity.this.setCheckedState((CheckBoxPreference) preference);
                return false;
            }
        });
        this.mPerAppToasts.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.6
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                PrefsActivity.this.setCheckedState((CheckBoxPreference) preference);
                return false;
            }
        });
        this.mBootDelay.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.settings.PrefsActivity.7
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PrefsActivity.this.setMinutes(PrefsActivity.this.mBootDelay, newValue.toString());
                return false;
            }
        });
        this.mAppTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.settings.PrefsActivity.13
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PrefsActivity.this.setThemeSummary(PrefsActivity.this.mAppTheme, newValue.toString());
                PrefsActivity.this.recreate();
                return true;
            }
        });
        this.mPer_app_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.8
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                PrefsActivity.this.setCheckedState((CheckBoxPreference) preference);
                if (!PrefsActivity.this.mPer_app_check.isChecked()) {
                    PrefsActivity.this.mPerAppMonitor.setEnabled(false);
                    PrefsActivity.this.mPerAppMonitor.setChecked(false);
                    AeroActivity.mJobManager.disable();
                    PrefsActivity.this.setCheckedState(PrefsActivity.this.mPerAppMonitor);
                    if (AeroActivity.perAppService == null) {
                        return false;
                    }
                    if (AeroActivity.perAppService.getState()) {
                        AeroActivity.perAppService.stopService();
                    }
                    return false;
                }
                Util.showUsageStatDialog(PrefsActivity.this);
                PrefsActivity.this.mPerAppMonitor.setEnabled(true);
                PrefsActivity.this.mPerAppMonitor.setChecked(true);
                AeroActivity.mJobManager.enable();
                PrefsActivity.this.setCheckedState(PrefsActivity.this.mPerAppMonitor);
                if (AeroActivity.perAppService == null) {
                    AeroActivity.perAppService = new PerAppServiceHelper(PrefsActivity.this.getBaseContext());
                }
                if (!AeroActivity.perAppService.getState()) {
                    AeroActivity.perAppService.startService();
                }
                preference.getEditor().commit();
                return true;
            }
        });
        this.mPerAppMonitor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.9
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                PrefsActivity.this.setCheckedState((CheckBoxPreference) preference);
                if (((CheckBoxPreference) preference).isChecked()) {
                    Util.showUsageStatDialog(PrefsActivity.this);
                    if (AeroActivity.mJobManager != null) {
                        AeroActivity.mJobManager.enable();
                        return false;
                    }
                    return false;
                }
                if (AeroActivity.mJobManager != null) {
                    AeroActivity.mJobManager.disable();
                    return false;
                }
                return false;
            }
        });
        resetTutorials.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.10
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(PrefsActivity.context);
                dialog.setTitle(R.string.pref_reset_tutorials_title);
                dialog.setMessage(R.string.pref_reset_tutorials_dialog);
                Drawable warningIcon = PrefsActivity.this.getResources().getDrawable(R.drawable.warning).mutate();
                warningIcon.setColorFilter(PrefsActivity.this.mIconTintColor, PorterDuff.Mode.SRC_IN);
                dialog.setIcon(warningIcon);
                dialog.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.settings.PrefsActivity.10.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String[] fileArray = AeroActivity.shell.getDirInfo(PrefsActivity.this.getApplicationInfo().dataDir + "/files/", true);
                        for (String s : fileArray) {
                            if (!new File(PrefsActivity.this.getApplicationInfo().dataDir + "/files/" + s).delete()) {
                                Log.e("Aero", "Couldn't delete: " + s);
                            }
                        }
                    }
                });
                dialog.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
                dialog.create().show();
                return false;
            }
        });
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.11
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                AboutDialog alertDialog = new AboutDialog();
                LayoutInflater inflater = PrefsActivity.this.getLayoutInflater();
                View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
                TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
                alertDialog.setContext(PrefsActivity.context);
                alertDialog.setTitle(R.string.about);
                Drawable beerIcon = PrefsActivity.this.getResources().getDrawable(R.drawable.beer).mutate();
                alertDialog.setIcon(beerIcon);
                alertDialog.setView(layout);
                alertDialog.setPayPalIcons(true);
                alertDialog.setPositiveButton(R.string.donation_blechdose);
                aboutText.setText(PrefsActivity.this.getText(R.string.about_dialog));
                alertDialog.show(PrefsActivity.this.getFragmentManager(), "");
                return true;
            }
        });
        legal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.settings.PrefsActivity.12
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PrefsActivity.context);
                LayoutInflater inflater = PrefsActivity.this.getLayoutInflater();
                View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
                TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
                builder.setTitle(R.string.legal);
                Drawable emailIcon = PrefsActivity.this.getResources().getDrawable(R.drawable.email).mutate();
                emailIcon.setColorFilter(PrefsActivity.this.mIconTintColor, PorterDuff.Mode.SRC_IN);
                builder.setIcon(emailIcon);
                aboutText.setText(PrefsActivity.this.getText(R.string.legal_dialog));
                aboutText.setTextSize(13.0f);
                builder.setView(layout).setPositiveButton(R.string.send_email, new DialogInterface.OnClickListener() { // from class: com.aero.control.settings.PrefsActivity.12.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int id) {
                        Intent emailIntent = new Intent("android.intent.action.SENDTO", Uri.fromParts("mailto", "alex.christ@hotmail.de", null));
                        try {
                            emailIntent.putExtra("android.intent.extra.SUBJECT", "[AeroControl] Got something for you (" + PrefsActivity.this.getPackageManager().getPackageInfo(PrefsActivity.this.getPackageName(), 0).versionName + ")");
                        } catch (PackageManager.NameNotFoundException e2) {
                            emailIntent.putExtra("android.intent.extra.SUBJECT", "[AeroControl] Got something for you");
                        }
                        PrefsActivity.this.startActivity(Intent.createChooser(emailIntent, PrefsActivity.this.getText(R.string.send_email)));
                    }
                });
                builder.show();
                return true;
            }
        });
    }

    private void setTintedIcon(Preference preference, int resId) {
        Drawable icon = getResources().getDrawable(resId).mutate();
        icon.setColorFilter(this.mIconTintColor, PorterDuff.Mode.SRC_IN);
        preference.setIcon(icon);
    }

    private void setTintedDialogIcon(ListPreference preference, int resId) {
        Drawable icon = getResources().getDrawable(resId).mutate();
        icon.setColorFilter(this.mIconTintColor, PorterDuff.Mode.SRC_IN);
        preference.setDialogIcon(icon);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setCheckedState(CheckBoxPreference preference) {
        if (preference.isChecked()) {
            preference.setSummary(R.string.enabled);
        } else {
            preference.setSummary(R.string.disabled);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setMinutes(ListPreference preference, String value) {
        if (value != null) {
            if (value.equals(getText(R.string.disabled)) || value.equals("0")) {
                preference.setSummary(R.string.disabled);
                preference.setValue("0");
                return;
            }
            int i = Integer.parseInt(value);
            if (i == 1) {
                preference.setSummary(value + " " + ((Object) getText(R.string.minute)));
            } else {
                preference.setSummary(value + " " + ((Object) getText(R.string.minutes)));
            }
            preference.setValue(value);
            return;
        }
        preference.setSummary(R.string.disabled);
        preference.setValue("0");
        preference.setValueIndex(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setThemeSummary(ListPreference preference, String value) {
        int index = preference.findIndexOfValue(value);
        CharSequence[] entries = preference.getEntries();
        if (index >= 0 && index < entries.length) {
            preference.setSummary(entries[index]);
        }
    }

    @Override // android.preference.PreferenceActivity, android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                finish();
                Intent i = new Intent(context, (Class<?>) AeroActivity.class);
                context.startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
