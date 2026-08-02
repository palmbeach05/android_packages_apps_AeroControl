package com.aero.control.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.AboutDialog;
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
    private ListPreference mBootDelay;
    private CheckBoxPreference mPerAppMonitor;
    private CheckBoxPreference mPerAppToasts;
    private CheckBoxPreference mPer_app_check;
    private CheckBoxPreference mRebootChecker;

    @Override // android.preference.PreferenceActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        this.mRebootChecker.setIcon(R.drawable.ic_action_phone);
        setCheckedState(this.mRebootChecker);
        this.mPer_app_check.setIcon(R.drawable.ic_action_person);
        this.mPerAppMonitor.setIcon(R.drawable.ic_action_appmonitor);
        this.mBootDelay.setIcon(R.drawable.timer);
        this.mBootDelay.setDialogIcon(R.drawable.timer);
        this.mPerAppToasts.setIcon(R.drawable.ic_action_toast);
        setCheckedState(this.mPerAppToasts);
        setMinutes(this.mBootDelay, this.mBootDelay.getValue());
        resetTutorials.setIcon(R.drawable.ic_action_warning);
        setCheckedState(this.mPer_app_check);
        setCheckedState(this.mPerAppMonitor);
        version.setIcon(R.drawable.rocket);
        xda.setIcon(R.drawable.xda);
        github.setIcon(R.drawable.github);
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
        about.setIcon(R.drawable.ic_action_about);
        legal.setIcon(R.drawable.ic_action_legal);
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
                dialog.setIcon(R.drawable.warning);
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
                alertDialog.setIcon(R.drawable.beer);
                alertDialog.setView(layout);
                alertDialog.setPayPalIcons(true);
                alertDialog.setNeutralButton(R.string.donation_quarx);
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
                builder.setIcon(R.drawable.email);
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
