package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.Android.CustomPreference;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.Util;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/* JADX INFO: loaded from: classes.dex */
public class MemoryFragment extends PlaceHolderFragment implements Preference.OnPreferenceChangeListener {
    private static final String IO_SETTINGS_CATEGORY = "io_scheduler_parameter";
    private static final String MEMORY_SETTINGS_CATEGORY = "memory_settings";
    private PreferenceCategory PrefCat;
    private CustomPreference mDalvikSettings;
    private CustomPreference mDynFSync;
    private CustomPreference mFSTrimToggle;
    private String mFileSystem;
    private CustomPreference mFsync;
    private CustomListPreference mIOScheduler;
    private PreferenceHandler mIOSchedulerHandler;
    private CustomPreference mKSMSettings;
    private MemoryDalvikFragment mMemoryDalvikFragment;
    private CustomPreference mRandomSettings;
    private CustomListPreference mReadAHead;
    private CustomPreference mWriteBackControl;
    private PreferenceScreen root;
    private boolean showDialog = true;

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.memory_fragment);
        setHasOptionsMenu(true);
        ArrayList<CharSequence> readaheadValues = new ArrayList<>();
        this.root = getPreferenceScreen();
        PreferenceCategory memorySettingsCategory = (PreferenceCategory) findPreference(MEMORY_SETTINGS_CATEGORY);
        PreferenceCategory ioSettingsCategory = (PreferenceCategory) findPreference(IO_SETTINGS_CATEGORY);
        this.mDynFSync = new CustomPreference(getActivity());
        this.mDynFSync.setName("dynFsync");
        this.mDynFSync.setTitle(R.string.pref_dynamic_fsync);
        this.mDynFSync.setSummary(R.string.pref_dynamic_fsync_summary);
        this.mDynFSync.setLookUpDefault(FilePath.DYANMIC_FSYNC);
        this.mDynFSync.setOrder(15);
        memorySettingsCategory.addPreference(this.mDynFSync);
        if ("1".equals(AeroActivity.shell.getInfo(FilePath.DYANMIC_FSYNC))) {
            this.mDynFSync.setClicked(true);
            this.mDynFSync.setSummary(R.string.enabled);
        } else if ("0".equals(AeroActivity.shell.getInfo(FilePath.DYANMIC_FSYNC))) {
            this.mDynFSync.setClicked(false);
            this.mDynFSync.setSummary(R.string.disabled);
        } else if (memorySettingsCategory != null) {
            memorySettingsCategory.removePreference(this.mDynFSync);
        }
        this.mFsync = new CustomPreference(getActivity());
        this.mFsync.setName("fsync");
        this.mFsync.setTitle(R.string.pref_fsync);
        this.mFsync.setSummary(R.string.pref_fsync_summary);
        this.mFsync.setLookUpDefault(FilePath.FSYNC);
        this.mFsync.setOrder(14);
        memorySettingsCategory.addPreference(this.mFsync);
        String temp = AeroActivity.shell.getInfo(FilePath.FSYNC);
        if ("Y".equals(temp) || "1".equals(temp)) {
            this.mFsync.setClicked(true);
            this.mFsync.setSummary(R.string.enabled);
        } else if ("N".equals(temp) || "0".equals(temp)) {
            this.mFsync.setClicked(false);
            this.mFsync.setSummary(R.string.disabled);
        } else if (memorySettingsCategory != null) {
            memorySettingsCategory.removePreference(this.mFsync);
        }
        this.mKSMSettings = new CustomPreference(getActivity());
        this.mKSMSettings.setName("ksm");
        this.mKSMSettings.setTitle(R.string.pref_ksm);
        this.mKSMSettings.setSummary(R.string.pref_ksm_summary);
        this.mKSMSettings.setLookUpDefault(FilePath.KSM_SETTINGS);
        this.mKSMSettings.setOrder(16);
        memorySettingsCategory.addPreference(this.mKSMSettings);
        String temp2 = AeroActivity.shell.getInfo(FilePath.KSM_SETTINGS);
        if ("1".equals(temp2)) {
            this.mKSMSettings.setClicked(true);
            this.mKSMSettings.setSummary(R.string.enabled);
        } else if ("2".equals(temp2) || "0".equals(temp2)) {
            this.mKSMSettings.setClicked(false);
            this.mKSMSettings.setSummary(R.string.disabled);
        } else if (memorySettingsCategory != null) {
            memorySettingsCategory.removePreference(this.mKSMSettings);
        }
        this.mWriteBackControl = new CustomPreference(getActivity());
        this.mWriteBackControl.setName("writeback");
        this.mWriteBackControl.setTitle(R.string.pref_dynamic_writeback);
        this.mWriteBackControl.setSummary(R.string.pref_dynamic_writeback_summary);
        this.mWriteBackControl.setLookUpDefault(FilePath.WRITEBACK);
        this.mWriteBackControl.setOrder(20);
        memorySettingsCategory.addPreference(this.mWriteBackControl);
        if ("1".equals(AeroActivity.shell.getInfo(FilePath.WRITEBACK))) {
            this.mWriteBackControl.setClicked(true);
            this.mWriteBackControl.setSummary(R.string.enabled);
        } else if ("0".equals(AeroActivity.shell.getInfo(FilePath.WRITEBACK))) {
            this.mWriteBackControl.setClicked(false);
            this.mWriteBackControl.setSummary(R.string.disabled);
        } else if (memorySettingsCategory != null) {
            memorySettingsCategory.removePreference(this.mWriteBackControl);
        }
        for (int i = 1; i <= 32; i++) {
            readaheadValues.add("" + (i * 128));
        }
        this.mReadAHead = new CustomListPreference(getActivity());
        this.mReadAHead.setName("read_ahead");
        this.mReadAHead.setOrder(12);
        this.mReadAHead.setTitle(R.string.pref_readahead);
        this.mReadAHead.setDialogTitle(R.string.pref_readahead_dialog);
        this.mReadAHead.setEntries((CharSequence[]) readaheadValues.toArray(new CharSequence[0]));
        this.mReadAHead.setEntryValues((CharSequence[]) readaheadValues.toArray(new CharSequence[0]));
        this.mReadAHead.setValue(AeroActivity.shell.getInfo(FilePath.READAHEAD_PARAMETER));
        this.mReadAHead.setSummary(AeroActivity.shell.getInfo(FilePath.READAHEAD_PARAMETER));
        this.mReadAHead.setOnPreferenceChangeListener(this);
        memorySettingsCategory.addPreference(this.mReadAHead);
        this.mRandomSettings = (CustomPreference) findPreference("entropy_settings");
        this.mRandomSettings.setOrder(22);
        this.mFSTrimToggle = (CustomPreference) findPreference("fstrim_toggle");
        this.mFSTrimToggle.setOrder(25);
        this.mFSTrimToggle.setHideOnBoot(true);
        this.mDalvikSettings = (CustomPreference) findPreference("dalvik_settings");
        this.mDalvikSettings.setOrder(30);
        this.mDalvikSettings.setHideOnBoot(true);
        this.mIOScheduler = new CustomListPreference(getActivity());
        this.mIOScheduler.setName("io_scheduler_list");
        this.mIOScheduler.setTitle(R.string.io_scheduler);
        this.mIOScheduler.setDialogTitle(R.string.io_scheduler);
        this.mIOScheduler.setEntries(AeroActivity.shell.getInfoArray(FilePath.GOV_IO_FILE, 0, 1));
        this.mIOScheduler.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.GOV_IO_FILE, 0, 1));
        this.mIOScheduler.setValue(AeroActivity.shell.getInfoString(AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE)));
        this.mIOScheduler.setSummary(AeroActivity.shell.getInfoString(AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE)));
        this.mIOScheduler.setDialogIcon(R.drawable.device_drive);
        this.mIOScheduler.setOnPreferenceChangeListener(this);
        ioSettingsCategory.addPreference(this.mIOScheduler);
        this.mIOSchedulerHandler = new PreferenceHandler(getActivity(), ioSettingsCategory, getPreferenceManager());
        this.mIOSchedulerHandler.addInvisiblePreference();
        if (this.showDialog) {
            Runnable runnable = new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.1
                @Override // java.lang.Runnable
                public void run() {
                    boolean fileSystemCheck = false;
                    String[] fileMount = AeroActivity.shell.getInfo("/proc/mounts", false);
                    boolean fileMountCheck = false;
                    int len$ = fileMount.length;
                    int i$ = 0;
                    while (true) {
                        if (i$ >= len$) {
                            break;
                        }
                        String tmp = fileMount[i$];
                        if (!tmp.contains("/dev/block/mmcblk1p25")) {
                            i$++;
                        } else {
                            fileMountCheck = true;
                            break;
                        }
                    }
                    MemoryFragment.this.showDialog = false;
                    if (fileMountCheck) {
                        String fileJournal = AeroActivity.shell.getRootInfo("tune2fs -l", "/dev/block/mmcblk1p25");
                        if (fileJournal.length() != 0 && fileJournal.contains("has_journal")) {
                            fileSystemCheck = true;
                        }
                        MemoryFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.1.1
                            @Override // java.lang.Runnable
                            public void run() {
                                if (!fileSystemCheck) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MemoryFragment.this.getActivity());
                                    LayoutInflater inflater = MemoryFragment.this.getActivity().getLayoutInflater();
                                    View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
                                    TextView aboutText = (TextView) (layout != null ? layout.findViewById(R.id.aboutScreen) : null);
                                    builder.setTitle(R.string.has_journal_dialog_header);
                                    if (aboutText != null) {
                                        aboutText.setText(MemoryFragment.this.getText(R.string.has_journal_dialog));
                                        aboutText.setTextSize(13.0f);
                                    }
                                    builder.setView(layout).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MemoryFragment.1.1.1
                                        @Override // android.content.DialogInterface.OnClickListener
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                    });
                                    builder.show();
                                }
                            }
                        });
                    }
                }
            };
            Thread checkThread = new Thread(runnable);
            checkThread.start();
        }
    }

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FilePath.FILENAME)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_memory_fragment_trim, R.string.showcase_memory_fragment_trim_summary, FilePath.FILENAME);
        }
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.memory_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_io_settings /* 2131099749 */:
                loadIOParameter();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override // android.preference.PreferenceFragment
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        CustomPreference cusPref = null;
        if (preference == this.mDynFSync) {
            this.mDynFSync.setClicked(Boolean.valueOf(this.mDynFSync.isClicked().booleanValue() ? false : true));
            if (this.mDynFSync.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.DYANMIC_FSYNC);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.DYANMIC_FSYNC);
            }
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mFsync) {
            this.mFsync.setClicked(Boolean.valueOf(this.mFsync.isClicked().booleanValue() ? false : true));
            if (this.mFsync.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.FSYNC);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.FSYNC);
            }
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mKSMSettings) {
            this.mKSMSettings.setClicked(Boolean.valueOf(this.mKSMSettings.isClicked().booleanValue() ? false : true));
            if (this.mKSMSettings.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.KSM_SETTINGS);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.KSM_SETTINGS);
            }
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mWriteBackControl) {
            this.mWriteBackControl.setClicked(Boolean.valueOf(this.mWriteBackControl.isClicked().booleanValue() ? false : true));
            if (this.mWriteBackControl.isClicked().booleanValue()) {
                AeroActivity.shell.setRootInfo("1", FilePath.WRITEBACK);
            } else {
                AeroActivity.shell.setRootInfo("0", FilePath.WRITEBACK);
            }
            cusPref = (CustomPreference) preference;
        } else if (preference == this.mFSTrimToggle) {
            fsTrimToggleClick();
        } else if (preference == this.mDalvikSettings) {
            if (this.mMemoryDalvikFragment == null) {
                this.mMemoryDalvikFragment = new MemoryDalvikFragment();
            }
            AeroActivity.mHandler.postDelayed(new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.2
                @Override // java.lang.Runnable
                public void run() {
                    MemoryFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, MemoryFragment.this.mMemoryDalvikFragment).addToBackStack("Memory").commit();
                }
            }, AeroActivity.genHelper.getDefaultDelay());
        } else if (preference == this.mRandomSettings) {
            onRandomClick();
        }
        if (cusPref != null && cusPref.isChecked().booleanValue()) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = sharedPrefs.edit();
            String state = cusPref.isClicked().booleanValue() ? "1" : "0";
            editor.putString(cusPref.getName(), state).commit();
        }
        return true;
    }

    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        if (preference == this.mIOScheduler) {
            this.mIOScheduler.setSummary(value);
            AeroActivity.shell.setRootInfo(value, FilePath.GOV_IO_FILE);
            if (this.PrefCat != null) {
                this.root.removePreference(this.PrefCat);
            }
        } else if (preference == this.mReadAHead) {
            AeroActivity.shell.setRootInfo(value, FilePath.READAHEAD_PARAMETER);
            this.mReadAHead.setSummary(value);
        } else {
            return false;
        }
        return true;
    }

    private void onRandomClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.memory_random, (ViewGroup) null);
        TextView txtRandomRead = (TextView) layout.findViewById(R.id.random_read);
        TextView txtRandomWrite = (TextView) layout.findViewById(R.id.random_write);
        final EditText editRandomRead = (EditText) layout.findViewById(R.id.random_read_value);
        final EditText editRandomWrite = (EditText) layout.findViewById(R.id.random_write_value);
        txtRandomRead.setText(Util.getLastSysValue(FilePath.RANDOM_READ_WAKEUP));
        txtRandomWrite.setText(Util.getLastSysValue(FilePath.RANDOM_WRITE_WAKEUP));
        editRandomRead.setText(AeroActivity.shell.getFastInfo(FilePath.RANDOM_READ_WAKEUP));
        editRandomWrite.setText(AeroActivity.shell.getFastInfo(FilePath.RANDOM_WRITE_WAKEUP));
        builder.setIcon(R.drawable.puzzle);
        builder.setTitle(((Object) getText(R.string.pref_entropy_settings)) + " (" + AeroActivity.shell.getFastInfo(FilePath.RANDOM_POOL_AVAIL) + "/" + AeroActivity.shell.getFastInfo(FilePath.RANDOM_POOL_SIZE) + ")");
        builder.setView(layout);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MemoryFragment.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                String[] cmds = {"echo " + ((Object) editRandomRead.getText()) + " > " + FilePath.RANDOM_READ_WAKEUP, "echo " + ((Object) editRandomWrite.getText()) + " > " + FilePath.RANDOM_WRITE_WAKEUP};
                AeroActivity.shell.setRootInfo(cmds);
                if (MemoryFragment.this.mRandomSettings.isChecked().booleanValue()) {
                    SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MemoryFragment.this.getActivity().getBaseContext());
                    preference.edit().putStringSet(MemoryFragment.this.mRandomSettings.getKey(), new HashSet(Arrays.asList(cmds))).commit();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MemoryFragment.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    private void fsTrimToggleClick() {
        if (!AeroActivity.genHelper.doesExist("/system/xbin/fstrim")) {
            Toast.makeText(getActivity(), R.string.pref_fstrim_no_busybox, 0).show();
            return;
        }
        if (this.mFileSystem == null) {
            this.mFileSystem = AeroActivity.shell.getRootInfo("mount", "");
        }
        CharSequence[] system = {" /system ", " /data ", " /cache "};
        ArrayList<String> fs = new ArrayList<>();
        int count = 0;
        for (CharSequence a : system) {
            if (this.mFileSystem.contains(a)) {
                int tmp = this.mFileSystem.indexOf(a.toString());
                String temp = this.mFileSystem.substring(tmp, a.length() + tmp + 4).replace(a, "");
                if (temp.equals("ext3") || temp.equals("ext4")) {
                    fs.add(a.toString());
                    count++;
                }
            }
        }
        CharSequence[] fsystem = (CharSequence[]) fs.toArray(new CharSequence[0]);
        if (count == 0) {
            Toast.makeText(getActivity(), R.string.unavailable, 0).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ProgressDialog update = new ProgressDialog(getActivity());
        builder.setTitle(R.string.pref_fstrim);
        builder.setIcon(R.drawable.file_exe);
        builder.setItems(fsystem, new AnonymousClass5(fsystem, update)).show();
    }

    /* JADX INFO: renamed from: com.aero.control.fragments.MemoryFragment$5, reason: invalid class name */
    class AnonymousClass5 implements DialogInterface.OnClickListener {
        final /* synthetic */ CharSequence[] val$fsystem;
        final /* synthetic */ ProgressDialog val$update;

        AnonymousClass5(CharSequence[] charSequenceArr, ProgressDialog progressDialog) {
            this.val$fsystem = charSequenceArr;
            this.val$update = progressDialog;
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int item) {
            final String b = (String) this.val$fsystem[item];
            this.val$update.setProgressStyle(0);
            this.val$update.setCancelable(false);
            this.val$update.setIndeterminate(true);
            this.val$update.setIndeterminateDrawable(MemoryFragment.this.getResources().getDrawable(R.drawable.spinner_animation));
            this.val$update.setMessage(Util.getRandomLoadingText(MemoryFragment.this.getActivity()));
            this.val$update.show();
            AeroActivity.shell.remountSystem();
            Runnable runnable = new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.5.1
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        AeroActivity.shell.getRootInfo("fstrim -v", b);
                        Thread.sleep(2000L);
                    } catch (Exception e) {
                        Log.e("Aero", "An error occurred while trimming.", e);
                    }
                    MemoryFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.5.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            AnonymousClass5.this.val$update.dismiss();
                        }
                    });
                }
            };
            Thread trimThread = new Thread(runnable);
            if (!trimThread.isAlive()) {
                trimThread.start();
            }
        }
    }

    public void DrawFirstStart(int header, int content, String filename) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(filename, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.MemoryFragment.6
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                return new Point(200, 200);
            }
        };
        new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

    private void loadIOParameter() {
        this.mIOSchedulerHandler.removeInvisiblePreference();
        try {
            String[] completeParamterList = AeroActivity.shell.getDirInfo(FilePath.GOV_IO_PARAMETER, true);
            if (this.PrefCat != null) {
                this.root.removePreference(this.PrefCat);
            }
            if (completeParamterList.length == 0) {
                Toast.makeText(getActivity(), R.string.pref_io_scheduler_no_parameter, 1).show();
                return;
            }
            this.PrefCat = new PreferenceCategory(getActivity());
            this.PrefCat.setTitle(R.string.pref_io_scheduler);
            this.root.addPreference(this.PrefCat);
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Log.e("Aero", "Something interrupted the main Thread, try again.", e);
            }
            PreferenceHandler h = new PreferenceHandler(getActivity(), this.PrefCat, getPreferenceManager());
            h.genPrefFromDictionary(completeParamterList, FilePath.GOV_IO_PARAMETER);
        } catch (NullPointerException e2) {
            Toast.makeText(getActivity(), R.string.pref_io_scheduler_no_parameter, 1).show();
            this.root.removePreference(this.PrefCat);
            Log.e("Aero", "There isn't any folder i can check. Does this governor has parameters?", e2);
        }
    }
}
