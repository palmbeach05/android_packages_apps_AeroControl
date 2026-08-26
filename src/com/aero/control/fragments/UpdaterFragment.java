package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
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
import com.aero.control.helpers.shellHelper;
import com.aero.control.helpers.updateHelper;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment for backing up and restoring the boot partition on supported devices.
 * Checks device against a whitelist before allowing backup/restore operations.
 */
public class UpdaterFragment extends PlaceHolderFragment {
    private static final String AERO_PATH = "/sdcard/com.aero.control/backup";
    private static final String NO_DATA_FOUND = "Unavailable";
    private String mBackup = null;
    private CustomPreference mBackupKernel;
    private CustomListPreference mRestoreKernel;
    private LoadKernelInfoTask mLoadTask;
    private static final String SDPATH = Environment.getExternalStorageDirectory().getPath();
    private static final String timeStamp = new SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
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
        this.mBackupKernel.setIcon(R.drawable.ic_action_copy);
        this.mRestoreKernel.setIcon(R.drawable.ic_action_time);
        this.mRestoreKernel.setDialogIcon(R.drawable.restore);
        // AeroActivity.shell.getInfo()/getDirInfo() can fall back to spawning
        // a root shell (Runtime.exec("su")) when a file can't be read
        // directly, which used to run synchronously right here in onCreate()
        // on the UI thread and could ANR the app while waiting on the su
        // prompt. Disable both preferences until the background lookup
        // below reports back and updates them on the main thread.
        this.mBackupKernel.setEnabled(false);
        this.mRestoreKernel.setEnabled(false);
        this.mLoadTask = new LoadKernelInfoTask();
        this.mLoadTask.execute();
        this.mRestoreKernel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.UpdaterFragment.1
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                final String s2 = (String) o;
                if (!isValidBackupName(s2)) {
                    Log.e("Aero", "Refusing to restore from suspicious backup name: " + s2);
                    Toast.makeText(UpdaterFragment.this.getActivity(), R.string.unavailable, 1).show();
                    return false;
                }
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
                Log.i("Aero", "Backup preference clicked. Source: " + (UpdaterFragment.this.mBackup != null ? UpdaterFragment.this.mBackup : FilePath.zImage));
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
                        UpdaterFragment.this.startKernelBackup();
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

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mLoadTask != null) {
            this.mLoadTask.cancel(true);
        }
    }

    /**
     * Looks up whether a zImage backup is available, detects a readable
     * boot-partition block device (if any), and lists any existing backup
     * folders. This work can fall back to spawning a root shell (see
     * {@link com.aero.control.helpers.shellHelper#getInfo(String)} and
     * {@link com.aero.control.helpers.shellHelper#isReadableBlockDevice(String)}),
     * so it must not run on the UI thread.
     */
    private class LoadKernelInfoTask extends AsyncTask<Void, Void, LoadKernelInfoTask.Result> {
        private class Result {
            boolean zImageAvailable;
            String bootSource;
            String whitelistedSource;
            String[] backupEntries;

            private Result() {
            }
        }

        @Override // android.os.AsyncTask
        protected Result doInBackground(Void... params) {
            if (isCancelled()) {
                return null;
            }
            Result result = new Result();
            result.zImageAvailable = !AeroActivity.shell.getInfo(FilePath.zImage).equals(UpdaterFragment.NO_DATA_FOUND);
            if (isCancelled()) {
                return null;
            }
            for (String candidate : FilePath.BACKUPPATH) {
                if (isCancelled()) {
                    return null;
                }
                if (AeroActivity.shell.isReadableBlockDevice(candidate)) {
                    result.bootSource = candidate;
                    break;
                }
            }
            if (isCancelled()) {
                return null;
            }
            String whitelisted = update.isWhiteListed(Build.MODEL);
            if (whitelisted != null && AeroActivity.shell.isReadableBlockDevice(whitelisted)) {
                result.whitelistedSource = whitelisted;
            }
            if (isCancelled()) {
                return null;
            }
            result.backupEntries = AeroActivity.shell.getDirInfo(UpdaterFragment.SDPATH + "/com.aero.control/backup/", false);
            return result;
        }

        @Override // android.os.AsyncTask
        protected void onPostExecute(Result result) {
            if (result == null || !UpdaterFragment.this.isAdded()) {
                return;
            }
            if (result.bootSource != null) {
                UpdaterFragment.this.mBackup = result.bootSource;
                Log.i("Aero", "Detected readable boot-partition backup source: " + UpdaterFragment.this.mBackup);
            }
            if (UpdaterFragment.this.mBackup != null) {
                UpdaterFragment.this.mBackupKernel.setEnabled(true);
            } else if (result.zImageAvailable) {
                Log.i("Aero", "No boot-partition source found; using zImage backup source: " + FilePath.zImage);
                UpdaterFragment.this.mBackupKernel.setEnabled(true);
            } else {
                UpdaterFragment.this.mBackupKernel.setEnabled(false);
            }
            if (!UpdaterFragment.this.mBackupKernel.isEnabled() && result.whitelistedSource != null) {
                UpdaterFragment.this.mBackup = result.whitelistedSource;
                Log.i("Aero", "Using whitelisted boot-partition backup source: " + UpdaterFragment.this.mBackup);
                UpdaterFragment.this.mBackupKernel.setEnabled(true);
            }
            if (!result.zImageAvailable) {
                UpdaterFragment.this.mRestoreKernel.setEnabled(false);
            }
            if (result.backupEntries != null && result.backupEntries.length > 0) {
                UpdaterFragment.this.mBackupKernel.setSummary(((Object) UpdaterFragment.this.getText(R.string.last_backup_from)) + " " + result.backupEntries[0]);
                UpdaterFragment.this.mRestoreKernel.setEnabled(true);
            } else {
                UpdaterFragment.this.mBackupKernel.setSummary(((Object) UpdaterFragment.this.getText(R.string.last_backup_from)) + " " + ((Object) UpdaterFragment.this.getText(R.string.unavailable)));
                UpdaterFragment.this.mRestoreKernel.setEnabled(false);
            }
            UpdaterFragment.this.mRestoreKernel.setEntries(result.backupEntries);
            UpdaterFragment.this.mRestoreKernel.setEntryValues(result.backupEntries);
        }
    }

    public void startKernelBackup() {
        new KernelBackupTask().execute();
    }

    /**
     * Runs a kernel backup (boot partition or zImage, depending on device)
     * through the shared root shell on a background thread, blocks until the
     * copy command finishes, and verifies the resulting file before
     * reporting success. This avoids the previous fire-and-forget behaviour
     * where {@link com.aero.control.helpers.shellHelper#setRootInfo(String[])}
     * returned immediately after queuing the {@code dd} command, before the
     * copy had actually completed.
     */
    private class KernelBackupTask extends AsyncTask<Void, Void, File> {
        private static final String SUCCESS_MARKER = "DD_SUCCESS";
        private static final String FAILURE_MARKER = "DD_FAILURE";

        @Override // android.os.AsyncTask
        protected File doInBackground(Void... params) {
            String backupDir = UpdaterFragment.SDPATH + "/com.aero.control/backup/" + UpdaterFragment.timeStamp;
            if (!AeroActivity.genHelper.doesExist(AERO_PATH) && !new File(AERO_PATH).mkdir() && !new File(AERO_PATH).mkdirs()) {
                Log.e("Aero", "Couldn't create file: " + AERO_PATH);
            }
            if (!AeroActivity.genHelper.doesExist(backupDir) && !new File(backupDir).mkdir() && !new File(backupDir).mkdirs()) {
                Log.e("Aero", "Couldn't create file: " + backupDir);
            }
            String source;
            String outputName;
            if (UpdaterFragment.this.mBackup != null) {
                source = UpdaterFragment.this.mBackup;
                outputName = "boot.img";
            } else {
                source = FilePath.zImage;
                outputName = "zImage";
            }
            File outputFile = new File(backupDir, outputName);
            String quotedSource = shellHelper.escapeShellArg(source);
            String quotedOutput = shellHelper.escapeShellArg(outputFile.getPath());
            String command = "dd if=" + quotedSource + " of=" + quotedOutput + " && { chmod 777 " + quotedOutput + "; echo " + SUCCESS_MARKER + "; } || echo " + FAILURE_MARKER;
            String output = AeroActivity.shell.runCommandAndWaitForOutput(command);
            if (output == null) {
                Log.e("Aero", "Kernel backup shell command was interrupted or failed to complete.");
                return null;
            }
            if (!output.contains(SUCCESS_MARKER)) {
                Log.e("Aero", "dd command failed - success marker not found in output. Output: " + output);
                return null;
            }
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            }
            Log.e("Aero", "dd command reported success but output file is missing or empty.");
            return null;
        }

        @Override // android.os.AsyncTask
        protected void onPostExecute(File result) {
            if (!UpdaterFragment.this.isAdded()) {
                return;
            }
            if (result != null) {
                Toast.makeText(UpdaterFragment.this.getActivity(), "Backup was successful!", 1).show();
                UpdaterFragment.this.mBackupKernel.setSummary(((Object) UpdaterFragment.this.getText(R.string.last_backup_from)) + " " + UpdaterFragment.timeStamp);
                String[] entries = AeroActivity.shell.getDirInfo(UpdaterFragment.SDPATH + "/com.aero.control/backup/", false);
                UpdaterFragment.this.mRestoreKernel.setEntries(entries);
                UpdaterFragment.this.mRestoreKernel.setEntryValues(entries);
                UpdaterFragment.this.mRestoreKernel.setEnabled(true);
            } else {
                Log.e("Aero", "Kernel backup failed verification: output file missing or empty.");
                Toast.makeText(UpdaterFragment.this.getActivity(), "Backup failed!", 1).show();
                UpdaterFragment.this.mRestoreKernel.setEnabled(false);
            }
        }
    }

    public void restorezImage(String s) {
        if (!isValidBackupName(s)) {
            Log.e("Aero", "Refusing to restore from suspicious backup name: " + s);
            Toast.makeText(getActivity(), R.string.unavailable, 1).show();
            return;
        }
        String source = new File(AERO_PATH + "/" + s, "zImage").getPath();
        String[] commands = {"rm -f /system/bootstrap/2nd-boot/zImage", "cp " + shellHelper.escapeShellArg(source) + " " + shellHelper.escapeShellArg(FilePath.zImage)};
        AeroActivity.shell.setRootInfo(commands);
        Toast.makeText(getActivity(), R.string.need_reboot, 1).show();
    }

    public void restoreBoot(String s) {
        if (!isValidBackupName(s)) {
            Log.e("Aero", "Refusing to restore from suspicious backup name: " + s);
            Toast.makeText(getActivity(), R.string.unavailable, 1).show();
            return;
        }
        String filepath = new File("/sdcard/com.aero.control/backup/" + s + "/boot.img").getPath();
        String quotedFilepath = shellHelper.escapeShellArg(filepath);
        String[] commands = {"chmod 0777 " + quotedFilepath, "dd if=" + quotedFilepath + " of=" + shellHelper.escapeShellArg(this.mBackup)};
        AeroActivity.shell.setRootInfo(commands);
        Toast.makeText(getActivity(), R.string.need_reboot, 1).show();
    }

    /**
     * Backup folder names are normally generated internally as a ddMMyyyy
     * timestamp, but the restore list is populated from a directory listing
     * on external storage, which is world-writable on legacy Android
     * versions. Reject anything containing path traversal or shell
     * metacharacters before it is concatenated into a privileged shell
     * command.
     */
    private static boolean isValidBackupName(String name) {
        return name != null && name.matches("[a-zA-Z0-9_-]+");
    }
}
