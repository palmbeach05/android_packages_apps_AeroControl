package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.Android.CustomPreference;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.updateHelper;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/* JADX INFO: loaded from: classes.dex */
public class UpdaterFragment extends PlaceHolderFragment {
    private static final String AERO_PATH = "/sdcard/com.aero.control/backup";
    private static final String NO_DATA_FOUND = "Unavailable";
    private String mBackup = null;
    private CustomPreference mBackupKernel;
    private CustomListPreference mRestoreKernel;
    private static final String SDPATH = Environment.getExternalStorageDirectory().getPath();
    private static final String timeStamp = new SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
    private static final File BACKUP_PATH = new File(SDPATH + "/com.aero.control/backup/" + timeStamp + "/zImage");
    private static final File IMAGE = new File(FilePath.zImage);
    private static final updateHelper update = new updateHelper();

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.updater_fragment);
        this.mBackupKernel = (CustomPreference) findPreference("backup_kernel");
        this.mBackupKernel.setHideOnBoot(true);
        this.mBackupKernel.setHelpEnable(false);
        this.mRestoreKernel = new CustomListPreference(getActivity());
        this.mRestoreKernel.setName("restore_kernel");
        this.mRestoreKernel.setTitle(R.string.pref_restore_kernel);
        this.mRestoreKernel.setDialogTitle(R.string.pref_restore_kernel);
        this.mRestoreKernel.setHideOnBoot(true);
        this.mRestoreKernel.setHelpEnable(false);
        getPreferenceScreen().addPreference(this.mRestoreKernel);
        String[] arr$ = FilePath.BACKUPPATH;
        for (String s : arr$) {
            if (AeroActivity.genHelper.doesExist(s)) {
                this.mBackup = s;
            }
        }
        if (AeroActivity.shell.getInfo(FilePath.zImage).equals(NO_DATA_FOUND)) {
            this.mBackupKernel.setEnabled(false);
        }
        if (this.mBackup != null) {
            this.mBackupKernel.setEnabled(true);
        }
        if (!this.mBackupKernel.isEnabled() && update.isWhiteListed(Build.MODEL) != null) {
            this.mBackup = update.isWhiteListed(Build.MODEL);
            this.mBackupKernel.setEnabled(true);
        }
        if (AeroActivity.shell.getInfo(FilePath.zImage).equals(NO_DATA_FOUND)) {
            this.mRestoreKernel.setEnabled(false);
        }
        try {
            this.mBackupKernel.setSummary(((Object) getText(R.string.last_backup_from)) + " " + AeroActivity.shell.getDirInfo(SDPATH + "/com.aero.control/backup/", false)[0]);
            this.mRestoreKernel.setEnabled(true);
        } catch (NullPointerException e) {
            this.mBackupKernel.setSummary(((Object) getText(R.string.last_backup_from)) + " " + ((Object) getText(R.string.unavailable)));
            this.mRestoreKernel.setEnabled(false);
        }
        this.mBackupKernel.setIcon(R.drawable.ic_action_copy);
        this.mRestoreKernel.setIcon(R.drawable.ic_action_time);
        this.mRestoreKernel.setEntries(AeroActivity.shell.getDirInfo(SDPATH + File.separator + "/com.aero.control/backup/", false));
        this.mRestoreKernel.setEntryValues(AeroActivity.shell.getDirInfo(SDPATH + "/com.aero.control/backup/", false));
        this.mRestoreKernel.setDialogIcon(R.drawable.restore);
        this.mRestoreKernel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.UpdaterFragment.1
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String s2 = (String) o;
                AlertDialog.Builder builder = new AlertDialog.Builder(UpdaterFragment.this.getActivity());
                LayoutInflater inflater = UpdaterFragment.this.getActivity().getLayoutInflater();
                View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
                TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
                builder.setTitle(((Object) UpdaterFragment.this.getText(R.string.backup_from)) + " " + s2);
                aboutText.setText(((Object) UpdaterFragment.this.getText(R.string.restore_from_backup)) + " " + s2 + " ?");
                AeroActivity.shell.remountSystem();
                preference.getEditor().remove(preference.getKey()).commit();
                builder.setView(layout).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.UpdaterFragment.1.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int id) {
                        if (UpdaterFragment.this.mBackup != null) {
                            UpdaterFragment.this.restoreBoot(s2);
                        } else {
                            UpdaterFragment.this.restorezImage(s2);
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.UpdaterFragment.1.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builder.show();
                return true;
            }
        });
        this.mBackupKernel.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.UpdaterFragment.2
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(UpdaterFragment.this.getActivity());
                LayoutInflater inflater = UpdaterFragment.this.getActivity().getLayoutInflater();
                View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
                TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
                builder.setTitle("Backup");
                builder.setIcon(R.drawable.backup);
                aboutText.setText(R.string.proceed_backup);
                builder.setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.UpdaterFragment.2.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int id) {
                        if (UpdaterFragment.this.mBackup != null) {
                            UpdaterFragment.this.backupBoot();
                        } else {
                            UpdaterFragment.this.backupzImage();
                        }
                        UpdaterFragment.this.mBackupKernel.setSummary(((Object) UpdaterFragment.this.getText(R.string.last_backup_from)) + " " + UpdaterFragment.timeStamp);
                        UpdaterFragment.this.mRestoreKernel.setEntries(AeroActivity.shell.getDirInfo(UpdaterFragment.SDPATH + "/com.aero.control/backup/", false));
                        UpdaterFragment.this.mRestoreKernel.setEntryValues(AeroActivity.shell.getDirInfo(UpdaterFragment.SDPATH + "/com.aero.control/backup/", false));
                        UpdaterFragment.this.mRestoreKernel.setEnabled(true);
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.UpdaterFragment.2.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
                builder.show();
                return true;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void backupzImage() {
        try {
            update.copyFile(IMAGE, BACKUP_PATH, false);
            Toast.makeText(getActivity(), "Backup was successful!", 1).show();
        } catch (IOException e) {
            Log.e("Aero", "A problem occured while saving a backup.", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void backupBoot() {
        String backuppath = "/sdcard/com.aero.control/backup/" + timeStamp;
        if (!AeroActivity.genHelper.doesExist(AERO_PATH) && !new File(AERO_PATH).mkdir() && !new File(AERO_PATH).mkdirs()) {
            Log.e("Aero", "Couldn't create file: /sdcard/com.aero.control/backup");
        }
        if (!AeroActivity.genHelper.doesExist(backuppath) && !new File(backuppath).mkdir() && !new File(backuppath).mkdirs()) {
            Log.e("Aero", "Couldn't create file: " + backuppath);
        }
        String[] commands = {"dd if=" + this.mBackup + " of=" + backuppath + "/boot.img", "chmod 777 " + backuppath + "/boot.img"};
        AeroActivity.shell.setRootInfo(commands);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restorezImage(String s) {
        String[] commands = {"rm -f /system/bootstrap/2nd-boot/zImage", "cp /sdcard/com.aero.control/backup/" + s + "/zImage " + FilePath.zImage};
        AeroActivity.shell.setRootInfo(commands);
        Toast.makeText(getActivity(), R.string.need_reboot, 1).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void restoreBoot(String s) {
        String filepath = new File("/sdcard/com.aero.control/backup/" + s + "/boot.img").getPath();
        String[] commands = {"chmod 0777 " + filepath, "dd if=" + filepath + " of=" + this.mBackup};
        AeroActivity.shell.setRootInfo(commands);
        Toast.makeText(getActivity(), R.string.need_reboot, 1).show();
    }
}
