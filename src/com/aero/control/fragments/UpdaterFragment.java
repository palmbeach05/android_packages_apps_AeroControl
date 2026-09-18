package com.aero.control.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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
import com.aero.control.helpers.OperationResult;
import com.aero.control.helpers.StoragePermission;
import com.aero.control.helpers.shellHelper;
import com.aero.control.helpers.updateHelper;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;

/**
 * Fragment for backing up and restoring the boot partition on supported devices.
 * Checks device against a whitelist before allowing backup/restore operations.
 */
public class UpdaterFragment extends PlaceHolderFragment {
    private static final String AERO_PATH = "/sdcard/com.aero.control/backup";
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final int PENDING_STORAGE_ACTION_NONE = 0;
    private static final int PENDING_STORAGE_ACTION_BACKUP = 1;
    private static final int PENDING_STORAGE_ACTION_RESTORE = 2;
    private static final String STATE_PENDING_STORAGE_ACTION =
            "pending_storage_action";
    private static final String STATE_DEFERRED_RESTORE_DIALOG =
            "deferred_restore_dialog";
    private static final Object RESTORE_TASK_LOCK = new Object();
    private static final Queue<CompletedRestore> sCompletedRestores =
            new ArrayDeque<CompletedRestore>();
    private static KernelRestoreTask sRestoreTask;
    private String mBackup = null;
    private CustomPreference mBackupKernel;
    private CustomListPreference mRestoreKernel;
    private LoadKernelInfoTask mLoadTask;
    private KernelRestoreTask mRestoreTask;
    private static final String SDPATH = Environment.getExternalStorageDirectory().getPath();
    private boolean mDeferredRestoreDialog;
    private int mPendingStorageAction = PENDING_STORAGE_ACTION_NONE;
    private static final updateHelper update = new updateHelper();

    /** Initializes the backup and restore preferences and loads their current state. */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mPendingStorageAction = savedInstanceState.getInt(
                    STATE_PENDING_STORAGE_ACTION, PENDING_STORAGE_ACTION_NONE);
            this.mDeferredRestoreDialog = savedInstanceState.getBoolean(
                    STATE_DEFERRED_RESTORE_DIALOG, false);
        }
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
        observeActiveRestoreTask();
        loadKernelInfo();
        this.mRestoreKernel.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.UpdaterFragment.1
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (UpdaterFragment.this.isRestoreInProgress()) {
                    return false;
                }
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
        this.mRestoreKernel.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    /** Requests storage access before showing available restore backups. */
                    @Override // android.preference.Preference.OnPreferenceClickListener
                    public boolean onPreferenceClick(Preference preference) {
                        if (UpdaterFragment.this.isRestoreInProgress()) {
                            return true;
                        }
                        if (StoragePermission.isGranted(
                                UpdaterFragment.this.getActivity())) {
                            return false;
                        }
                        UpdaterFragment.this.mPendingStorageAction =
                                PENDING_STORAGE_ACTION_RESTORE;
                        StoragePermission.request(UpdaterFragment.this);
                        return true;
                    }
                });
        this.mBackupKernel.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.fragments.UpdaterFragment.2
            /** Shows the backup confirmation dialog when the backup preference is selected. */
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
                    /** Starts the backup immediately or requests the required storage permission. */
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialog, int id) {
                            if (StoragePermission.isGranted(UpdaterFragment.this.getActivity())) {
                                UpdaterFragment.this.startKernelBackup();
                            } else {
                                UpdaterFragment.this.mPendingStorageAction = PENDING_STORAGE_ACTION_BACKUP;
                                StoragePermission.request(UpdaterFragment.this);
                            }
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

    /** Preserves an action while Android recreates the Fragment for a permission request. */
    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_PENDING_STORAGE_ACTION, this.mPendingStorageAction);
        outState.putBoolean(STATE_DEFERRED_RESTORE_DIALOG,
                this.mDeferredRestoreDialog);
        super.onSaveInstanceState(outState);
    }

    /** Recovers a pending storage action when Android omits the permission callback. */
    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        observeActiveRestoreTask();
        consumeCompletedRestores();
        if (this.mPendingStorageAction != PENDING_STORAGE_ACTION_NONE
                && StoragePermission.isGranted(getActivity())) {
            consumePendingStorageAction();
        }
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mLoadTask != null) {
            this.mLoadTask.cancel(true);
        }
        if (this.mRestoreTask != null) {
            this.mRestoreTask.stopObserving(this);
            this.mRestoreTask = null;
        }
    }

    /** Observes the process-local restore task that may outlive this Fragment instance. */
    private void observeActiveRestoreTask() {
        synchronized (RESTORE_TASK_LOCK) {
            this.mRestoreTask = sRestoreTask;
            if (this.mRestoreTask != null) {
                this.mRestoreTask.observe(this);
                if (this.mRestoreKernel != null) {
                    this.mRestoreKernel.setEnabled(false);
                }
            }
        }
    }

    /** Displays retained restore completions in the order they finished. */
    private void consumeCompletedRestores() {
        while (true) {
            CompletedRestore completedRestore;
            synchronized (RESTORE_TASK_LOCK) {
                completedRestore = sCompletedRestores.poll();
            }
            if (completedRestore == null) {
                return;
            }
            showKernelRestoreResult(completedRestore.mRestoredBoot,
                    completedRestore.mResult);
        }
    }

    /** Returns whether any UpdaterFragment currently owns an active restore. */
    private boolean isRestoreInProgress() {
        synchronized (RESTORE_TASK_LOCK) {
            return sRestoreTask != null;
        }
    }

    /** Cancels any previous lookup and reloads kernel and backup information. */
    private void loadKernelInfo() {
        if (this.mLoadTask != null) {
            this.mLoadTask.cancel(true);
        }
        this.mLoadTask = new LoadKernelInfoTask(
                StoragePermission.isGranted(getActivity()));
        this.mLoadTask.execute();
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
        private final boolean mCanReadBackupStorage;
    
        private LoadKernelInfoTask(boolean canReadBackupStorage) {
            this.mCanReadBackupStorage = canReadBackupStorage;
        }
        private class Result {
            boolean canReadBackupStorage;
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
            result.canReadBackupStorage = this.mCanReadBackupStorage;
            if (this.mCanReadBackupStorage) {
                result.backupEntries = getSortedBackupEntries();
            }
            return result;
        }

        /** Applies discovered kernel and backup information to the preferences. */
        @Override // android.os.AsyncTask
        protected void onPostExecute(Result result) {
            if (result == null || !UpdaterFragment.this.isAdded()) {
                return;
            }
            UpdaterFragment.this.mRestoreKernel.setEntries(result.backupEntries);
            UpdaterFragment.this.mRestoreKernel.setEntryValues(result.backupEntries);
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
            if (!result.canReadBackupStorage) {
                UpdaterFragment.this.mBackupKernel.setSummary(
                        ((Object) UpdaterFragment.this.getText(R.string.last_backup_from))
                                + " "
                                + ((Object) UpdaterFragment.this.getText(
                                        R.string.storage_permission_required)));
                UpdaterFragment.this.mRestoreKernel.setEnabled(
                        UpdaterFragment.this.mRestoreTask == null);
            } else if (result.backupEntries != null && result.backupEntries.length > 0) {
                UpdaterFragment.this.mBackupKernel.setSummary(
                        ((Object) UpdaterFragment.this.getText(R.string.last_backup_from))
                                + " " + result.backupEntries[0]);
                UpdaterFragment.this.mRestoreKernel.setEnabled(
                        UpdaterFragment.this.mRestoreTask == null);
            } else {
                UpdaterFragment.this.mBackupKernel.setSummary(
                        ((Object) UpdaterFragment.this.getText(R.string.last_backup_from))
                                + " "
                                + ((Object) UpdaterFragment.this.getText(R.string.unavailable)));
                UpdaterFragment.this.mRestoreKernel.setEnabled(false);
            }
            boolean showDeferredRestoreDialog =
                    UpdaterFragment.this.mDeferredRestoreDialog;
            UpdaterFragment.this.mDeferredRestoreDialog = false;
            if (showDeferredRestoreDialog
                    && UpdaterFragment.this.mRestoreKernel.isEnabled()) {
                UpdaterFragment.this.mRestoreKernel.performCustomClick();
            }
        }
    }

    /**
     * Initiates a kernel backup operation in the background.
     * Backs up either the boot partition or zImage depending on device configuration.
     */
    public void startKernelBackup() {
        new KernelBackupTask().execute();
    }

    /**
     * Resumes a pending backup after a successful storage permission request.
     *
     * @param requestCode the permission request identifier
     * @param permissions the requested permissions
     * @param grantResults grant results corresponding to {@code permissions}
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        boolean permissionGranted = isStoragePermissionGranted(grantResults);
        if (requestCode != StoragePermission.REQUEST_CODE) {
            return;
        }

        if (permissionGranted) {
            consumePendingStorageAction();
            return;
        }

        boolean hadPendingStorageAction =
                this.mPendingStorageAction != PENDING_STORAGE_ACTION_NONE;
        this.mPendingStorageAction = PENDING_STORAGE_ACTION_NONE;
        this.mDeferredRestoreDialog = false;
        if (hadPendingStorageAction) {
            Toast.makeText(getActivity(), R.string.storage_permission_required,
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Clears and executes the pending action so lifecycle paths cannot run it twice. */
    private void consumePendingStorageAction() {
        int pendingAction = this.mPendingStorageAction;
        this.mPendingStorageAction = PENDING_STORAGE_ACTION_NONE;
        if (pendingAction == PENDING_STORAGE_ACTION_BACKUP) {
            startKernelBackup();
        } else if (pendingAction == PENDING_STORAGE_ACTION_RESTORE) {
            if (isRestoreInProgress()) {
                return;
            }
            this.mDeferredRestoreDialog = true;
            loadKernelInfo();
        }
    }

    /**
     * Checks whether a storage permission request was granted.
     *
     * @param grantResults results returned by the permission request
     * @return {@code true} when the first requested permission was granted
     */
    static boolean isStoragePermissionGranted(int[] grantResults) {
        return grantResults != null
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
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
        private String mTimeStamp;

        /**
         * Copies the active kernel image into a timestamped backup directory.
         *
         * @param params unused task parameters
         * @return the verified backup file, or {@code null} when the backup fails
         */
        @Override // android.os.AsyncTask
        protected File doInBackground(Void... params) {
            this.mTimeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault())
                    .format(new Date());
            File backupRoot = new File(AERO_PATH);
            if ((!backupRoot.exists() && !backupRoot.mkdirs()) || !backupRoot.isDirectory()) {
                Log.e("Aero", "Couldn't create backup directory: " + AERO_PATH);
                return null;
            }
            File backupDirectory;
            int suffix = 0;
            while (true) {
                String directoryName = this.mTimeStamp
                        + (suffix == 0 ? "" : "_" + suffix);
                backupDirectory = new File(backupRoot, directoryName);
                if (backupDirectory.mkdir()) {
                    break;
                }
                if (!backupDirectory.exists()) {
                    Log.e("Aero", "Couldn't create backup directory: "
                            + backupDirectory.getPath());
                    return null;
                }
                suffix++;
            }
            String backupDir = backupDirectory.getPath();
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
            String command = "dd if=" + quotedSource + " of=" + quotedOutput 
                + " && chmod 777 " + quotedOutput;
            OperationResult commandResult = AeroActivity.shell.runLongRunningRootCommand(command);
            if (!commandResult.isSuccess()) {
                Log.e("Aero", "Kernel backup failed: " + commandResult.getStatus()
                        + " " + commandResult.getMessage());
                return null;
            }
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            }
            Log.e("Aero", "dd command reported success but output file is missing or empty.");
            return null;
        }

        /** Reports the backup result and refreshes the available backup information. */
        @Override // android.os.AsyncTask
        protected void onPostExecute(File result) {
            if (!UpdaterFragment.this.isAdded()) {
                return;
            }
            if (result != null) {
                Toast.makeText(UpdaterFragment.this.getActivity(), "Backup was successful!", 1).show();
                UpdaterFragment.this.loadKernelInfo();
            } else {
                Log.e("Aero", "Kernel backup failed verification: output file missing or empty.");
                Toast.makeText(UpdaterFragment.this.getActivity(), "Backup failed!", 1).show();
                UpdaterFragment.this.mRestoreKernel.setEnabled(false);
            }
        }
    }

    private String[] getSortedBackupEntries() {
        String backupRoot = UpdaterFragment.SDPATH + "/com.aero.control/backup/";
        String[] entries = AeroActivity.shell.getDirInfo(backupRoot, false);
        if (entries == null || entries.length < 2) {
            return entries;
        }
        final File root = new File(backupRoot);
        final SimpleDateFormat backupFormat = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US);
        Arrays.sort(entries, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                long leftTime = getBackupTimestamp(root, left, backupFormat);
                long rightTime = getBackupTimestamp(root, right, backupFormat);
                if (leftTime < rightTime) {
                    return 1;
                }
                if (leftTime > rightTime) {
                    return -1;
                }
                return right.compareTo(left);
            }
        });
        return entries;
    }

    private long getBackupTimestamp(File root, String entry, SimpleDateFormat format) {
        File backupDir = new File(root, entry);
        if (entry != null && entry.matches("[a-zA-Z0-9_-]+")) {
            try {
                Date parsed = format.parse(entry);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException e) {
                Log.w("Aero", "Could not parse backup timestamp from entry: " + entry, e);
            }
        }
        return backupDir.lastModified();
    }

    /**
     * Restores a zImage kernel from a backup directory.
     *
     * @param s the backup directory name
     */
    public void restorezImage(String s) {
        if (!isValidBackupName(s)) {
            Log.e("Aero", "Refusing to restore from suspicious backup name: " + s);
            Toast.makeText(getActivity(), R.string.unavailable, 1).show();
            return;
        }
        startKernelRestore(false, s, null);
    }

    /**
     * Restores a boot.img from a backup directory to the boot partition.
     *
     * @param s the backup directory name
     */
    public void restoreBoot(String s) {
        if (!isValidBackupName(s)) {
            Log.e("Aero", "Refusing to restore from suspicious backup name: " + s);
            Toast.makeText(getActivity(), R.string.unavailable, 1).show();
            return;
        }
        startKernelRestore(true, s, this.mBackup);
    }

    /** Starts one restore process-wide and attaches this Fragment to its result. */
    private void startKernelRestore(boolean restoreBoot, String backupName,
            String bootPartition) {
        KernelRestoreTask task;
        synchronized (RESTORE_TASK_LOCK) {
            if (sRestoreTask != null) {
                this.mRestoreTask = sRestoreTask;
                this.mRestoreTask.observe(this);
                this.mRestoreKernel.setEnabled(false);
                return;
            }
            task = new KernelRestoreTask(restoreBoot, backupName, bootPartition);
            sRestoreTask = task;
            this.mRestoreTask = task;
            task.observe(this);
        }
        this.mRestoreKernel.setEnabled(false);
        task.execute();
    }

    /** Clears and reports a completed restore, retaining it until an observer is active. */
    private static void finishKernelRestore(KernelRestoreTask task,
            OperationResult result) {
        UpdaterFragment observer;
        synchronized (RESTORE_TASK_LOCK) {
            if (sRestoreTask != task) {
                return;
            }
            sRestoreTask = null;
            observer = task.takeObserver();
            if (observer == null || !observer.isAdded()) {
                sCompletedRestores.offer(new CompletedRestore(
                        task.restoresBoot(), result));
                observer = null;
            }
        }
        if (observer != null) {
            observer.onKernelRestoreFinished(task, result);
        }
    }

    /** Updates the current Fragment after the shared restore task completes. */
    private void onKernelRestoreFinished(KernelRestoreTask task,
            OperationResult result) {
        if (this.mRestoreTask != task) {
            return;
        }
        this.mRestoreTask = null;
        if (!isAdded()) {
            return;
        }
        showKernelRestoreResult(task.restoresBoot(), result);
    }

    /** Displays one restore result and refreshes the available restore entries. */
    private void showKernelRestoreResult(boolean restoredBoot,
            OperationResult result) {
        if (result.isSuccess()) {
            Toast.makeText(getActivity(), R.string.need_reboot, 1).show();
        } else {
            Log.e("Aero", (restoredBoot ? "Boot" : "zImage")
                    + " restore failed: " + result.getStatus() + " "
                    + result.getMessage());
            Toast.makeText(getActivity(), R.string.storage_operation_failed, 1).show();
        }
        loadKernelInfo();
    }

    /** Restore result retained together with the metadata needed to report it. */
    private static class CompletedRestore {
        private final boolean mRestoredBoot;
        private final OperationResult mResult;

        CompletedRestore(boolean restoredBoot, OperationResult result) {
            this.mRestoredBoot = restoredBoot;
            this.mResult = result;
        }
    }

    /** Performs boot and zImage restores without blocking the main thread. */
    private static class KernelRestoreTask extends AsyncTask<Void, Void, OperationResult> {
        private final boolean mRestoreBoot;
        private final String mBackupName;
        private final String mBootPartition;
        private UpdaterFragment mObserver;

        KernelRestoreTask(boolean restoreBoot, String backupName, String bootPartition) {
            this.mRestoreBoot = restoreBoot;
            this.mBackupName = backupName;
            this.mBootPartition = bootPartition;
        }

        /** Attaches the current Fragment instance to receive this task's result. */
        private void observe(UpdaterFragment observer) {
            this.mObserver = observer;
        }

        /** Detaches the supplied Fragment when it is still observing this task. */
        private void stopObserving(UpdaterFragment observer) {
            if (this.mObserver == observer) {
                this.mObserver = null;
            }
        }

        /** Removes and returns the Fragment currently observing this task. */
        private UpdaterFragment takeObserver() {
            UpdaterFragment observer = this.mObserver;
            this.mObserver = null;
            return observer;
        }

        private boolean restoresBoot() {
            return this.mRestoreBoot;
        }

        @Override
        protected OperationResult doInBackground(Void... params) {
            if (this.mRestoreBoot) {
                String filepath = new File(AERO_PATH + "/" + this.mBackupName,
                        "boot.img").getPath();
                String quotedFilepath = shellHelper.escapeShellArg(filepath);
                String command = "chmod 0777 " + quotedFilepath + " && dd if="
                        + quotedFilepath + " of=" + shellHelper.escapeShellArg(this.mBootPartition);
                return AeroActivity.shell.runLongRunningRootCommand(command);
            }

            OperationResult remountResult = AeroActivity.shell.remountSystem();
            if (!remountResult.isSuccess()) {
                return remountResult;
            }
            String source = new File(AERO_PATH + "/" + this.mBackupName, "zImage").getPath();
            String command = "rm -f /system/bootstrap/2nd-boot/zImage && cp "
                    + shellHelper.escapeShellArg(source) + " "
                    + shellHelper.escapeShellArg(FilePath.zImage);
            return AeroActivity.shell.runLongRunningRootCommand(command);
        }

        @Override
        protected void onPostExecute(OperationResult result) {
            finishKernelRestore(this, result);
        }
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
