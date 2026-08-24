package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.ThemeHelper;
import com.aero.control.helpers.PerApp.PerAppManager;
import com.aero.control.helpers.PerApp.perAppHelper;
import com.aero.control.helpers.Util;
import com.aero.control.helpers.settingsHelper;
import com.aero.control.service.PerAppServiceHelper;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class ProfileFragment extends PreferenceFragment {
    public static final String FILENAME_PERAPP = "firstrun_perapp";
    public static final String FILENAME_PROFILES = "firstrun_profiles";
    private static final String perAppProfileHandler = "perAppProfileHandler";
    private String[] mCompleteProfiles;
    private ViewGroup mContainerView;
    private Context mContext;
    private List<ApplicationInfo> mPackages;
    private boolean mPerAppDialogVisible;
    private SharedPreferences mPerAppPrefs;
    private SharedPreferences mPrefs;
    private ProgressDialog mProgressDialog;
    private ViewGroup mRootView;
    public ShowcaseView mShowCase;
    private boolean mWarning;
    private static final String LOG_TAG = PreferenceFragment.class.getName();
    public static final settingsHelper settings = new settingsHelper();

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = getActivity();
        this.mWarning = false;
        this.mPerAppDialogVisible = false;
        this.mPerAppPrefs = this.mContext.getSharedPreferences(perAppProfileHandler, 0);
        View v = inflater.inflate(R.layout.profile_fragment, container, false);
        TextView empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setTypeface(FilePath.kitkatFont);
        this.mRootView = (ViewGroup) v.findViewById(R.id.root_container);
        this.mContainerView = (ViewGroup) this.mRootView.findViewById(R.id.container);
        loadFloatingMenu();
        loadProfiles();
        return this.mRootView;
    }

    private String[] getDirectoryEntries(String path) {
        String[] entries = AeroActivity.shell.getDirInfo(path, true);
        return entries != null ? entries : new String[0];
    }

    private void loadFloatingMenu() {
        final FloatingActionsMenu floatMenu = (FloatingActionsMenu) this.mRootView.findViewById(R.id.float_menu);
        FloatingActionButton addProfiles = (FloatingActionButton) this.mRootView.findViewById(R.id.add_button);
        FloatingActionButton toggleSystem = (FloatingActionButton) this.mRootView.findViewById(R.id.toggle_system);
        FloatingActionButton resetButton = (FloatingActionButton) this.mRootView.findViewById(R.id.reset_button);
        addProfiles.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                ProfileFragment.this.showDialog(new EditText(ProfileFragment.this.mContext));
                floatMenu.toggle();
            }
        });
        toggleSystem.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.2
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                String systemStatus;
                String systemStatus2 = ProfileFragment.this.mPerAppPrefs.getString("systemStatus", "false");
                if (systemStatus2.equals("false")) {
                    systemStatus = "true";
                    Toast.makeText(ProfileFragment.this.mContext, ((Object) ProfileFragment.this.getText(R.string.pref_profile_show_system)) + ": " + ((Object) ProfileFragment.this.getText(R.string.enabled)), 0).show();
                } else {
                    systemStatus = "false";
                    Toast.makeText(ProfileFragment.this.mContext, ((Object) ProfileFragment.this.getText(R.string.pref_profile_show_system)) + ": " + ((Object) ProfileFragment.this.getText(R.string.disabled)), 0).show();
                }
                ProfileFragment.this.mPerAppPrefs.edit().putString("systemStatus", systemStatus).commit();
                ProfileFragment.this.mPerAppDialogVisible = false;
                floatMenu.toggle();
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.3
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                ProfileFragment.this.showResetDialog();
                floatMenu.toggle();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadProfiles() {
        this.mCompleteProfiles = getDirectoryEntries(FilePath.sharedPrefsPath);
        String[] arr$ = this.mCompleteProfiles;
        for (String s : arr$) {
            if (!s.equals("com.aero.control_preferences.xml") && !s.equals("showcase_internal.xml") && !s.equals("app_rate_prefs.xml") && !s.equals("perAppProfileHandler.xml") && !s.equals("miscSettingsStorage.xml")) {
                addProfile(s.replace(".xml", ""), false);
                this.mContainerView.findViewById(android.R.id.empty).setVisibility(8);
                this.mContainerView.findViewById(R.id.empty_image).setVisibility(8);
            }
        }
        if (AeroActivity.perAppService == null) {
            AeroActivity.perAppService = new PerAppServiceHelper(this.mContext);
        }
        if (!AeroActivity.perAppService.getState() && checkAllStates() && !this.mWarning) {
            Toast.makeText(this.mContext, R.string.pref_profile_service_not_running, Toast.LENGTH_LONG).show();
            this.mWarning = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext);
        builder.setIcon(R.drawable.warning);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
        TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
        builder.setTitle(R.string.pref_profile_reset);
        aboutText.setText(R.string.pref_profile_reset_summary);
        builder.setView(layout).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.5
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int id) {
                ProfileFragment.this.mPrefs = PreferenceManager.getDefaultSharedPreferences(ProfileFragment.this.mContext);
                SharedPreferences.Editor editor = ProfileFragment.this.mPrefs.edit();
                editor.clear();
                editor.commit();
                Toast.makeText(ProfileFragment.this.mContext, R.string.successful, 1).show();
            }
        }).setNegativeButton(R.string.maybe_later, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDialog(final EditText editText) {
        this.mCompleteProfiles = getDirectoryEntries(FilePath.sharedPrefsPath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            showMaterialProfileDialog(editText);
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(R.string.add_a_name).setIcon(R.drawable.profile_new).setMessage(R.string.define_a_name).setView(editText).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.7
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int which) {
                ProfileFragment.this.saveProfile(editText);
            }
        }).setNeutralButton(R.string.pref_profile_import, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.6
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                ProfileFragment.this.showImportDialog();
            }
        }).create();
        dialog.show();
    }

    private void showMaterialProfileDialog(final EditText editText) {
        View content = LayoutInflater.from(this.mContext).inflate(R.layout.profile_name_dialog, null);
        editText.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((FrameLayout) content.findViewById(R.id.profile_name_input)).addView(editText);
        final AlertDialog dialog = new AlertDialog.Builder(this.mContext)
                .setTitle(R.string.add_a_name)
                .setIcon(R.drawable.profile_new)
                .setMessage(R.string.define_a_name)
                .setView(content)
                .create();
        ((Button) content.findViewById(R.id.profile_import)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                ProfileFragment.this.showImportDialog();
            }
        });
        ((Button) content.findViewById(R.id.profile_save)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProfileFragment.this.saveProfile(editText);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void saveProfile(EditText editText) {
        String profileTitle = sanitizeProfileName(editText.getText().toString());
        if (profileTitle.equals("")) {
            Toast.makeText(this.mContext, R.string.pref_profile_enter_name, 1).show();
            return;
        }
        String targetFilename = profileTitle + ".xml";
        for (String profile : this.mCompleteProfiles) {
            if (profile.equals(targetFilename)) {
                Toast.makeText(this.mContext, R.string.pref_profile_name_exists, 1).show();
                return;
            }
        }
        addProfile(profileTitle, true);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME_PERAPP)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_perapp_profiles, R.string.showcase_perapp_profiles_summary, FILENAME_PERAPP, null);
        }
        this.mContainerView.findViewById(android.R.id.empty).setVisibility(8);
        this.mContainerView.findViewById(R.id.empty_image).setVisibility(8);
    }

    private void showImportDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this.mContext);
        final String dir = FilePath.EXTERNAL_PATH + "/com.aero.control/profiles";
        if (!AeroActivity.genHelper.doesExist(dir)) {
            Toast.makeText(this.mContext, R.string.pref_profile_no_import, 1).show();
            return;
        }
        final String[] strings = getDirectoryEntries(dir);
        final ArrayList<Boolean> importProfiles = new ArrayList<>();
        for (String str : strings) {
            importProfiles.add(false);
        }
        dialog.setTitle(R.string.pref_profile_import_select);
        dialog.setIcon(R.drawable.restore);
        dialog.setMultiChoiceItems(strings, (boolean[]) null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index, boolean checked) {
                if (checked) {
                    importProfiles.set(index, true);
                } else {
                    importProfiles.set(index, false);
                }
            }
        }).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int id) {
                for (int i = 0; i < strings.length; i++) {
                    String profile = strings[i];
                    if (importProfiles.get(i).booleanValue()) {
                        try {
                            AeroActivity.genHelper.copyFile(AeroActivity.genHelper.getNewFile(dir + "/" + profile), AeroActivity.genHelper.getNewFile(FilePath.sharedPrefsPath + profile));
                        } catch (IOException e) {
                            Log.e(ProfileFragment.LOG_TAG, "Couldn't copy file: " + dir + "/" + profile, e);
                        }
                        if (AeroActivity.genHelper.doesExist(FilePath.sharedPrefsPath + profile)) {
                            Toast.makeText(ProfileFragment.this.mContext, R.string.successful, 0).show();
                        }
                    }
                }
                for (int i = 0; i < ProfileFragment.this.mContainerView.getChildCount(); i++) {
                    ProfileFragment.this.mContainerView.getChildAt(i).setVisibility(8);
                }
                ProfileFragment.this.loadProfiles();
            }
        });
        dialog.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addProfile(final String s, boolean flag) {
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        SharedPreferences AeroProfile = this.mContext.getSharedPreferences(s, 0);
        final perAppHelper perApp = new perAppHelper(this.mContext);
        if (flag) {
            saveNewProfile(AeroProfile);
        }
        final ViewGroup childView = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.profiles_list, this.mContainerView, false);
        final TextView txtView = (TextView) childView.findViewById(R.id.profile_text);
        final TextView txtViewSummary = (TextView) childView.findViewById(R.id.profile_text_summary);
        txtView.setText(s);
        if (checkState(s)) {
            updateStatus(txtViewSummary, true);
        } else {
            updateStatus(txtViewSummary, false);
        }
        txtView.setTypeface(FilePath.kitkatFont);
        txtViewSummary.setTypeface(FilePath.kitkatFont);
        createListener(txtView, txtViewSummary);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            TypedValue iconColor = new TypedValue();
            this.mContext.getTheme().resolveAttribute(R.attr.aeroPrimaryTextColor, iconColor, true);
            ImageButton deleteButton = (ImageButton) childView.findViewById(R.id.delete_button);
            deleteButton.getDrawable().mutate().setColorFilter(iconColor.data, PorterDuff.Mode.SRC_IN);
        }
        childView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.8
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                new AlertDialog.Builder(ProfileFragment.this.mContext).setTitle(R.string.profile_remove).setMessage(R.string.profile_remove_confirmation).setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.8.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int which) {
                        boolean success = ProfileFragment.this.deleteProfile(txtView.getText().toString());
                        if (success) {
                            ProfileFragment.this.mContainerView.removeView(childView);
                            if (ProfileFragment.this.mContainerView.getChildCount() == 2) {
                                ProfileFragment.this.mContainerView.findViewById(android.R.id.empty).setVisibility(0);
                                ProfileFragment.this.mContainerView.findViewById(R.id.empty_image).setVisibility(0);
                            }
                            ProfileFragment.this.showProfileDeletedToast();
                        } else {
                            Toast.makeText(ProfileFragment.this.mContext, R.string.pref_profile_not_deleted, Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null).show();
            }
        });
        childView.findViewById(R.id.assign_to_app).setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.9
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                ProfileFragment.this.getPersistentData(perApp, s, txtViewSummary);
            }
        });
        this.mContainerView.addView(childView, 0);
    }

    private void showProfileDeletedToast() {
        String theme = ThemeHelper.getTheme(this.mContext);
        int themeResource = ThemeHelper.THEME_DARK.equals(theme)
                ? R.style.AeroTheme_Dark
                : ThemeHelper.THEME_SYSTEM.equals(theme) ? R.style.AeroTheme_System : R.style.AeroTheme;
        Context themedContext = new ContextThemeWrapper(this.mContext, themeResource);
        View toastView = LayoutInflater.from(themedContext).inflate(R.layout.profile_deleted_toast, null);
        Toast toast = new Toast(this.mContext);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);
        toast.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkAllStates() {
        String[] arr$ = this.mCompleteProfiles;
        for (String s : arr$) {
            if (!s.equals("com.aero.control_preferences.xml") && !s.equals("showcase_internal.xml") && !s.equals("app_rate_prefs.xml") && !s.equals("perAppProfileHandler.xml") && !s.equals("miscSettingsStorage.xml") && checkState(s.replace(".xml", ""))) {
                return true;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean checkState(String name) {
        String profile = this.mPerAppPrefs.getString(name, null);
        if (profile == null) {
            return false;
        }
        String[] tmp = profile.replace("+", " ").split(" ");
        for (String a : tmp) {
            if (a.equals("")) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void getPersistentData(final perAppHelper perApp, final String profileName, final TextView txtViewSummary) {
        if (!this.mPerAppDialogVisible) {
            this.mPerAppDialogVisible = true;
            final String savedSelectedProfiles = this.mPerAppPrefs.getString(profileName, null);
            String systemApps = this.mPerAppPrefs.getString("systemStatus", null);
            if (systemApps == null) {
                systemApps = "false";
            }
            perApp.setSystemAppStatus(Boolean.valueOf(systemApps).booleanValue());
            if (this.mPackages != null) {
                perApp.setPackages(this.mPackages);
                if (savedSelectedProfiles != null) {
                    String[] tmp = savedSelectedProfiles.replace("+", " ").split(" ");
                    perApp.findMatch(tmp);
                }
                showPerAppDialog(perApp, profileName, txtViewSummary);
                return;
            }
            if (this.mProgressDialog == null) {
                TypedValue dialogTheme = new TypedValue();
                this.mContext.getTheme().resolveAttribute(android.R.attr.dialogTheme, dialogTheme, true);
                this.mProgressDialog = new ProgressDialog(this.mContext, dialogTheme.resourceId);
                this.mProgressDialog.setMessage(Util.getRandomLoadingText(this.mContext));
                this.mProgressDialog.setIndeterminate(true);
                this.mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.spinner_animation));
            }
            this.mProgressDialog.show();
            Runnable runnable = new Runnable() { // from class: com.aero.control.fragments.ProfileFragment.10
                @Override // java.lang.Runnable
                public void run() {
                    perApp.getAllApps(perApp.getSystemAppStatus());
                    ProfileFragment.this.mPackages = perApp.getPackages();
                    if (savedSelectedProfiles != null) {
                        String[] tmp2 = savedSelectedProfiles.replace("+", " ").split(" ");
                        perApp.findMatch(tmp2);
                    }
                    ProfileFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.ProfileFragment.10.1
                        @Override // java.lang.Runnable
                        public void run() {
                            ProfileFragment.this.mProgressDialog.dismiss();
                            ProfileFragment.this.showPerAppDialog(perApp, profileName, txtViewSummary);
                        }
                    });
                }
            };
            new Thread(runnable).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateStatus(TextView txtView, boolean toggle) {
        if (toggle) {
            txtView.setText(R.string.per_app_active);
            txtView.setTextColor(Color.parseColor("#1abc9c"));
        } else {
            txtView.setText(R.string.per_app_not_active);
            txtView.setTextColor(Color.parseColor("#e74c3c"));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showPerAppDialog(final perAppHelper perApp, final String profileName, final TextView txtViewSummary) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this.mContext);
        PerAppManager pam = new PerAppManager(this.mContext, null, perApp);
        dialog.setView(pam);
        dialog.setTitle(R.string.pref_profile_per_app);
        dialog.setIcon(R.drawable.rocket);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.12
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int id) {
                String[] packageNames = perApp.getCurrentSelectedPackages();
                String tmp = "";
                if (packageNames != null) {
                    for (String a : packageNames) {
                        tmp = tmp + a + "+";
                    }
                }
                ProfileFragment.this.mPerAppPrefs.edit().remove(profileName);
                ProfileFragment.this.mPerAppPrefs.edit().putString("systemStatus", perApp.getSystemAppStatus() + "").commit();
                ProfileFragment.this.mPerAppPrefs.edit().putString(profileName, tmp).commit();
                if (ProfileFragment.this.checkState(profileName)) {
                    ProfileFragment.this.updateStatus(txtViewSummary, true);
                } else {
                    ProfileFragment.this.updateStatus(txtViewSummary, false);
                }
                if (AeroActivity.perAppService == null) {
                    AeroActivity.perAppService = new PerAppServiceHelper(ProfileFragment.this.mContext);
                }
                if (!AeroActivity.perAppService.getState() && ProfileFragment.this.checkAllStates() && !ProfileFragment.this.mWarning) {
                    Toast.makeText(ProfileFragment.this.mContext, R.string.pref_profile_service_not_running, Toast.LENGTH_LONG).show();
                    ProfileFragment.this.mWarning = true;
                }
                ProfileFragment.this.mPerAppDialogVisible = false;
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.11
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog2, int id) {
                ProfileFragment.this.mPerAppDialogVisible = false;
            }
        });
        dialog.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean deleteProfile(String ProfileName) {
        File prefFile = new File(FilePath.sharedPrefsPath + ProfileName + ".xml");
        this.mPerAppPrefs.edit().remove(ProfileName).commit();
        boolean deleted = prefFile.delete();
        if (!deleted && prefFile.exists()) {
            Log.e(LOG_TAG, "Whoop, it still exists, something went wrong");
            String[] cmd = {"rm " + escapeShellArg("/data/data/com.aero.control/shared_prefs/" + ProfileName + ".xml")};
            AeroActivity.shell.setRootInfo(cmd);
            try {
                Thread.sleep(350L);
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Something interrupted the main Thread, try again.", e);
            }
        }
        return !prefFile.exists();
    }

    /**
     * Restricts profile names to a safe character set (letters, digits, spaces,
     * underscore and hyphen) so they cannot be used to inject shell metacharacters
     * (e.g. ", ;, |, $(), `, &&) or path traversal sequences (.., /) into the
     * privileged shell commands built from them.
     */
    private static String sanitizeProfileName(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceAll("[^a-zA-Z0-9 _-]", "").trim();
    }

    /**
     * Wraps a value in single quotes for safe use as a single argument in a
     * shell command, escaping any embedded single quotes.
     */
    private static String escapeShellArg(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void saveNewProfile(SharedPreferences AeroProfile) {
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        SharedPreferences.Editor editor = AeroProfile.edit();
        Map<String, ?> allKeys = this.mPrefs.getAll();
        saveProfile(allKeys, editor);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyProfile(SharedPreferences AeroProfile) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        Map<String, ?> allKeys = AeroProfile.getAll();
        saveProfile(allKeys, editor);
    }

    private void saveProfile(Map<String, ?> allKeys, SharedPreferences.Editor editor) {
        Boolean tmp;
        for (Map.Entry<String, ?> entry : allKeys.entrySet()) {
            String value = entry.getValue().toString();
            String key = entry.getKey().toString();
            if (value.equals("true") || value.equals("false")) {
                if (!value.equals("false") && value.equals("true")) {
                    tmp = true;
                } else {
                    tmp = false;
                }
                editor.putBoolean(key, tmp.booleanValue());
            } else {
                editor.putString(key, value);
            }
        }
        editor.commit();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void renameProfile(CharSequence oldName, String newName, TextView txtView, TextView txtViewSummary) {
        File prefFile = new File(FilePath.sharedPrefsPath + oldName.toString() + ".xml");
        String newName2 = sanitizeProfileName(newName);
        boolean renameSuccess = prefFile.renameTo(AeroActivity.genHelper.getNewFile(FilePath.sharedPrefsPath + newName2 + ".xml"));
        if (!renameSuccess) {
            String[] cmd = {"mv " + escapeShellArg("/data/data/com.aero.control/shared_prefs/" + oldName.toString() + ".xml") + " " + escapeShellArg(FilePath.sharedPrefsPath + newName2 + ".xml")};
            AeroActivity.shell.setRootInfo(cmd);
        } else {
            prefFile.delete();
        }
        String valueOld = this.mPerAppPrefs.getString(oldName.toString(), null);
        this.mPerAppPrefs.edit().remove(oldName.toString()).commit();
        this.mPerAppPrefs.edit().putString(newName2, valueOld).commit();
        txtView.setText(newName2);
    }

    private void createListener(final TextView txtView, final TextView txtViewSummary) {
        View v = (View) txtView.getParent();
        v.setOnClickListener(new View.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.13
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                SharedPreferences AeroProfile = ProfileFragment.this.mContext.getSharedPreferences(txtView.getText().toString(), 0);
                TextView profileText = new TextView(ProfileFragment.this.mContext);
                String content = "";
                Map<String, ?> allKeys = AeroProfile.getAll();
                for (Map.Entry<String, ?> entry : allKeys.entrySet()) {
                    String tmp = entry.getKey();
                    if (tmp.contains(FilePath.CPU_GOV_BASE)) {
                        tmp = tmp.replace(FilePath.CPU_GOV_BASE, "");
                    } else if (tmp.contains("/proc/sys/vm/")) {
                        tmp = tmp.replace("/proc/sys/vm/", "");
                    } else if (tmp.contains("/sys/module/msm_kgsl_core/parameters/")) {
                        tmp = tmp.replace("/sys/module/msm_kgsl_core/parameters/", "gpu -> ");
                    } else if (tmp.contains("/sys/kernel/hotplug_control/")) {
                        tmp = tmp.replace("/sys/kernel/hotplug_control/", "hotplug_control -> ");
                    } else if (tmp.contains("/sys/devices/virtual/timed_output/vibrator/")) {
                        tmp = tmp.replace("/sys/devices/virtual/timed_output/vibrator/", "vibrator -> ");
                    } else if (tmp.contains("/sys/module/msm_thermal/parameters/")) {
                        tmp = tmp.replace("/sys/module/msm_thermal/parameters/", "thermal_control -> ");
                    } else if (tmp.contains(FilePath.GPU_GOV_BASE_FB00000)) {
                        tmp = tmp.replace(FilePath.GPU_GOV_BASE_FB00000, "");
                    } else if (tmp.contains("/sys/class/misc/soundcontrol/")) {
                        tmp = tmp.replace("/sys/class/misc/soundcontrol/", "");
                    } else if (tmp.contains("/sys/class/misc/mako_hotplug_control/")) {
                        tmp = tmp.replace("/sys/class/misc/mako_hotplug_control/", "hotplug_control -> ");
                    } else if (tmp.contains(FilePath.CPU_BOOST)) {
                        tmp = tmp.replace(FilePath.CPU_BOOST, "cpu_boostl -> ");
                    }
                    content = tmp + " = " + entry.getValue().toString() + "\n" + content;
                    profileText.setText(content);
                }
                profileText.setVerticalScrollBarEnabled(true);
                profileText.setMovementMethod(new ScrollingMovementMethod());
                profileText.setPadding(20, 20, 20, 20);
                profileText.setTypeface(FilePath.kitkatFont);
                AlertDialog dialog = new AlertDialog.Builder(ProfileFragment.this.mContext).setTitle(((Object) ProfileFragment.this.getText(R.string.slider_overview)) + ": " + txtView.getText().toString()).setView(profileText).setIcon(R.drawable.profile).setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.13.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog2, int which) {
                        ProfileFragment.this.mPrefs = ProfileFragment.this.mContext.getSharedPreferences("com.aero.control_preferences", 0);
                        ProfileFragment.this.deleteProfile("com.aero.control_preferences");
                        SharedPreferences AeroProfile2 = ProfileFragment.this.mContext.getSharedPreferences(txtView.getText().toString(), 0);
                        ProfileFragment.this.applyProfile(AeroProfile2);
                        ProfileFragment.settings.setSettings(ProfileFragment.this.mContext, null, false);
                    }
                }).setNeutralButton(R.string.pref_profile_export, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.13.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String dir = FilePath.EXTERNAL_PATH + "/com.aero.control/profiles";
                        String title = txtView.getText().toString() + ".xml";
                        if (!AeroActivity.genHelper.doesExist(dir) && !new File(dir).mkdirs()) {
                            Log.e(ProfileFragment.LOG_TAG, "Couldn't create path: " + dir);
                        }
                        try {
                            AeroActivity.genHelper.copyFile(AeroActivity.genHelper.getNewFile(FilePath.sharedPrefsPath + title), AeroActivity.genHelper.getNewFile(dir + "/" + title));
                        } catch (IOException e) {
                            Log.e(ProfileFragment.LOG_TAG, "Couldn't copy file: /data/data/com.aero.control/shared_prefs/" + title, e);
                        }
                        if (AeroActivity.genHelper.doesExist(dir + "/" + title)) {
                            Toast.makeText(ProfileFragment.this.mContext, R.string.successful, 0).show();
                        }
                    }
                }).create();
                dialog.show();
            }
        });
        v.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.aero.control.fragments.ProfileFragment.14
            @Override // android.view.View.OnLongClickListener
            public boolean onLongClick(View view) {
                ProfileFragment.this.mCompleteProfiles = ProfileFragment.this.getDirectoryEntries(FilePath.sharedPrefsPath);
                final EditText editText = new EditText(ProfileFragment.this.mContext);
                final CharSequence oldName = txtView.getText();
                editText.setText(oldName);
                AlertDialog dialog = new AlertDialog.Builder(ProfileFragment.this.mContext).setTitle(R.string.pref_profile_change_name).setMessage(R.string.pref_profile_change_name_summary).setView(editText).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.ProfileFragment.14.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog2, int which) {
                        String newName = ProfileFragment.sanitizeProfileName(editText.getText().toString());
                        if (newName.equals("")) {
                            Toast.makeText(ProfileFragment.this.mContext, R.string.pref_profile_enter_name, 1).show();
                            return;
                        }
                        String allProfiles = Arrays.asList(ProfileFragment.this.mCompleteProfiles).toString();
                        if (allProfiles.contains(newName + ".xml")) {
                            Toast.makeText(ProfileFragment.this.mContext, R.string.pref_profile_name_exists, 1).show();
                        } else {
                            txtView.setText(newName);
                            ProfileFragment.this.renameProfile(oldName, newName, txtView, txtViewSummary);
                        }
                    }
                }).create();
                dialog.show();
                return true;
            }
        });
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME_PROFILES)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_profile_fragment, R.string.showcase_profile_fragment_summary, FILENAME_PROFILES, Integer.valueOf(R.id.action_add_item));
        }
    }

    public void DrawFirstStart(int header, int content, String filename, final Integer id) {
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.ProfileFragment.15
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                return new Point(150, 125);
            }
        };
        Target actionIcon = new Target() { // from class: com.aero.control.fragments.ProfileFragment.16
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                int actionBarSize = 96;
                try {
                    actionBarSize = ProfileFragment.this.getActivity().findViewById(id.intValue()).getHeight();
                } catch (NullPointerException e) {
                }
                int x = ProfileFragment.this.getResources().getDisplayMetrics().widthPixels - (actionBarSize / 2);
                int y = actionBarSize / 2;
                return new Point(x, y);
            }
        };
        if (id == null) {
            this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
        } else {
            this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(actionIcon).build();
        }
        try {
            FileOutputStream fos = this.mContext.openFileOutput(filename, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Could not save file. ", e);
        }
    }
}
