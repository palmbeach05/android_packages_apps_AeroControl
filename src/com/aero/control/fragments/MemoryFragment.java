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
import com.aero.control.helpers.DirectoryEntryType;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.HardwareServices;
import com.aero.control.helpers.OperationResult;
import com.aero.control.helpers.PreferenceHandler;
import com.aero.control.helpers.ProcfsNode;
import com.aero.control.helpers.SysfsDirectory;
import com.aero.control.helpers.SysfsNode;
import com.aero.control.helpers.SysfsResult;
import com.aero.control.helpers.Util;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Fragment for configuring memory and I/O settings including read-ahead buffer,
 * I/O scheduler, fsync, dynamic fsync, KSM, and writeback options. Provides
 * save/apply functionality for boot and profile persistence.
 */
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
    private CustomPreference mLowMemoryStatus;
    private MemoryDalvikFragment mMemoryDalvikFragment;
    private CustomPreference mRandomSettings;
    private CustomListPreference mReadAHead;
    private CustomPreference mWriteBackControl;
    private PreferenceScreen root;
    private boolean showDialog = true;

    /**
     * Initializes the fragment, loads preferences from layout, and configures all memory
     * and I/O related settings including dynamic fsync, fsync, KSM, writeback, read-ahead,
     * entropy, fstrim, Dalvik settings, and I/O scheduler preferences.
     *
     * @param savedInstanceState the saved instance state bundle
     */
    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.memory_fragment);
        setHasOptionsMenu(true);
        ArrayList<CharSequence> readaheadValues = new ArrayList<>();
        this.root = getPreferenceScreen();
        PreferenceCategory memorySettingsCategory = (PreferenceCategory) findPreference(MEMORY_SETTINGS_CATEGORY);
        PreferenceCategory ioSettingsCategory = (PreferenceCategory) findPreference(IO_SETTINGS_CATEGORY);
        addLowMemoryStatusPreference(memorySettingsCategory);
        this.mDynFSync = new CustomPreference(getActivity());
        this.mDynFSync.setName("dynFsync");
        this.mDynFSync.setTitle(R.string.pref_dynamic_fsync);
        this.mDynFSync.setSummary(R.string.pref_dynamic_fsync_summary);
        this.mDynFSync.setLookUpDefault(FilePath.DYANMIC_FSYNC);
        this.mDynFSync.setOrder(15);
        memorySettingsCategory.addPreference(this.mDynFSync);
        if ("1".equals(readSysfsValue(FilePath.DYANMIC_FSYNC))) {
            this.mDynFSync.setClicked(true);
            this.mDynFSync.setSummary(R.string.enabled);
        } else if ("0".equals(readSysfsValue(FilePath.DYANMIC_FSYNC))) {
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
        String temp = readSysfsValue(FilePath.FSYNC);
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
        String temp2 = readSysfsValue(FilePath.KSM_SETTINGS);
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
        if ("1".equals(readSysfsValue(FilePath.WRITEBACK))) {
            this.mWriteBackControl.setClicked(true);
            this.mWriteBackControl.setSummary(R.string.enabled);
        } else if ("0".equals(readSysfsValue(FilePath.WRITEBACK))) {
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
        String readAheadValue = readSysfsValue(FilePath.READAHEAD_PARAMETER);
        this.mReadAHead.setValue(readAheadValue);
        this.mReadAHead.setSummary(readAheadValue);
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
        Preference.OnPreferenceClickListener preferenceClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return handlePreferenceClick(preference);
            }
        };
        this.mDynFSync.setOnPreferenceClickListener(preferenceClickListener);
        this.mFsync.setOnPreferenceClickListener(preferenceClickListener);
        this.mKSMSettings.setOnPreferenceClickListener(preferenceClickListener);
        this.mWriteBackControl.setOnPreferenceClickListener(preferenceClickListener);
        this.mFSTrimToggle.setOnPreferenceClickListener(preferenceClickListener);
        this.mDalvikSettings.setOnPreferenceClickListener(preferenceClickListener);
        this.mRandomSettings.setOnPreferenceClickListener(preferenceClickListener);
        this.mIOScheduler = new CustomListPreference(getActivity());
        this.mIOScheduler.setName("io_scheduler_list");
        this.mIOScheduler.setTitle(R.string.io_scheduler);
        this.mIOScheduler.setDialogTitle(R.string.io_scheduler);
        SysfsResult<String[]> availableSchedulers =
                AeroActivity.hardware.ioScheduler().readAvailableSchedulers();
        SysfsResult<String> currentScheduler =
                AeroActivity.hardware.ioScheduler().readCurrentScheduler();
        if (availableSchedulers.isSuccess() && currentScheduler.isSuccess()) {
            this.mIOScheduler.setEntries(availableSchedulers.getValue());
            this.mIOScheduler.setEntryValues(availableSchedulers.getValue());
            this.mIOScheduler.setValue(currentScheduler.getValue());
            this.mIOScheduler.setSummary(currentScheduler.getValue());
            ioSettingsCategory.addPreference(this.mIOScheduler);
        } else {
            Log.w("Aero", "I/O scheduler is unavailable: "
                    + (!availableSchedulers.isSuccess()
                    ? availableSchedulers.getError() : currentScheduler.getError()));
        }
        this.mIOScheduler.setDialogIcon(R.drawable.device_drive);
        this.mIOScheduler.setOnPreferenceChangeListener(this);
        this.mIOSchedulerHandler = new PreferenceHandler(getActivity(), ioSettingsCategory, getPreferenceManager());
        this.mIOSchedulerHandler.addInvisiblePreference();
        if (this.showDialog) {
            Runnable runnable = new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.1
                @Override // java.lang.Runnable
                public void run() {
                    boolean fileSystemCheck = false;
                    String[] fileMount = readProcfsLines("/proc/mounts");
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
                        SysfsResult<String> journalResult =
                                HardwareServices.memoryMaintenance().readJournalInfo();
                        if (!journalResult.isSuccess()) {
                            Log.w("MemoryFragment", journalResult.getError());
                        } else if (journalResult.getValue().contains("has_journal")) {
                            fileSystemCheck = true;
                        }
                        
						final boolean hasJournal = fileSystemCheck;
                        MemoryFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.1.1
                            @Override // java.lang.Runnable
                            public void run() {
                                if (!hasJournal) {
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

    /**
     * Adds a read-only low memory capability status preference to the memory settings
     * category. Asynchronously checks if the low memory property is supported and reads
     * its value, then adds the preference to the UI on the main thread if supported.
     *
     * @param memorySettingsCategory the preference category to add the low memory status preference to
     */
    private void addLowMemoryStatusPreference(final PreferenceCategory memorySettingsCategory) {
        if (memorySettingsCategory == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                SysfsResult<String> statusResult =
                        HardwareServices.memoryMaintenance().readLowMemoryStatus();
                if (!statusResult.isSuccess()) {
                    Log.w("MemoryFragment", statusResult.getError());
                    return;
                }
                final String lowMemoryStatus = statusResult.getValue();
                AeroActivity.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!MemoryFragment.this.isAdded()) {
                            return;
                        }
                        MemoryFragment.this.mLowMemoryStatus = new CustomPreference(MemoryFragment.this.getActivity());
                        MemoryFragment.this.mLowMemoryStatus.setName("low_memory_status");
                        MemoryFragment.this.mLowMemoryStatus.setTitle(R.string.pref_low_memory);
                        MemoryFragment.this.mLowMemoryStatus.setSummary(MemoryFragment.this.getString(R.string.pref_low_memory_summary, lowMemoryStatus));
                        MemoryFragment.this.mLowMemoryStatus.setOrder(18);
                        MemoryFragment.this.mLowMemoryStatus.setHideOnBoot(true);
                        MemoryFragment.this.mLowMemoryStatus.setHelpEnable(false);
                        MemoryFragment.this.mLowMemoryStatus.setSelectable(false);
                        memorySettingsCategory.addPreference(MemoryFragment.this.mLowMemoryStatus);
                    }
                });
            }
        }).start();
    }

    /**
     * Called when the fragment's activity has been created and this fragment's view hierarchy
     * instantiated. Checks if this is the first time the fragment is shown and displays a
     * showcase tutorial highlighting the fstrim feature if needed.
     *
     * @param savedInstanceState the saved instance state bundle
     */
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

    /**
     * Inflates the memory fragment's options menu with I/O settings menu items.
     *
     * @param menu the menu to inflate into
     * @param inflater the menu inflater to use
     */
    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.memory_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * Handles options menu item selections. Loads I/O scheduler parameters when the
     * I/O settings menu item is selected.
     *
     * @param item the menu item that was selected
     * @return true if the item was handled, false otherwise
     */
    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_io_settings /* 2131099749 */:
                loadIOParameter();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles clicks on preferences in the preference tree. Delegates to handlePreferenceClick
     * for processing the specific preference action.
     *
     * @param preferenceScreen the preference screen containing the clicked preference
     * @param preference the preference that was clicked
     * @return true if the click was handled
     */
    @Override // android.preference.PreferenceFragment
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        handlePreferenceClick(preference);
        return true;
    }

    /**
     * Handles clicks on the various memory and I/O preferences. Toggles the state of
     * CustomPreferences (fsync, dynamic fsync, KSM, writeback), triggers fstrim, opens
     * the Dalvik settings fragment, or shows the entropy/random settings dialog depending
     * on which preference is clicked.
     *
     * @param preference the preference that was clicked
     * @return true if the click was handled, false otherwise
     */
    private boolean handlePreferenceClick(Preference preference) {
        if (preference == this.mDynFSync) {
            if (!applyToggle(this.mDynFSync, FilePath.DYANMIC_FSYNC)) return false;
        } else if (preference == this.mFsync) {
            if (!applyToggle(this.mFsync, FilePath.FSYNC)) return false;
        } else if (preference == this.mKSMSettings) {
            if (!applyToggle(this.mKSMSettings, FilePath.KSM_SETTINGS)) return false;
        } else if (preference == this.mWriteBackControl) {
            if (!applyToggle(this.mWriteBackControl, FilePath.WRITEBACK)) return false;
        } else if (preference == this.mFSTrimToggle) {
            fsTrimToggleClick();
        } else if (preference == this.mDalvikSettings) {
            if (this.mMemoryDalvikFragment == null) {
                this.mMemoryDalvikFragment = new MemoryDalvikFragment();
            }
            AeroActivity.mHandler.post(new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.2
                @Override // java.lang.Runnable
                public void run() {
                    if (!MemoryFragment.this.isAdded() || MemoryFragment.this.getFragmentManager() == null) {
                        return;
                    }
                    try {
                        MemoryFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, MemoryFragment.this.mMemoryDalvikFragment).addToBackStack("Memory").commit();
                    } catch (IllegalStateException e) {
                        Log.e("Aero", "Could not commit fragment transaction, state already saved.", e);
                    }
                }
            });
        } else if (preference == this.mRandomSettings) {
            onRandomClick();
        } else {
            return false;
        }
        return true;
    }

    private boolean applyToggle(final CustomPreference preference, final String path) {
        final boolean previous = preference.isClicked().booleanValue();
        final boolean requested = !previous;
        final boolean shouldPersist = preference.isChecked().booleanValue();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean fsyncToggle = FilePath.FSYNC.equals(path);
                String requestedValue = fsyncToggle
                        ? (requested ? "Y" : "N")
                        : (requested ? "1" : "0");
                SysfsNode node = fsyncToggle
                        ? new SysfsNode(path, Arrays.asList("Y", "N"))
                        : new SysfsNode(path, 0, 1);
                final SysfsResult<String> writeResult = AeroActivity.hardware.write(
                        node, requestedValue);
                final Boolean actual = readToggleValue(path);
                if (!isAdded()) {
                    return;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isAdded()) {
                            return;
                        }
                        if (actual != null) {
                            preference.setClicked(actual);
                        } else {
                            preference.setClicked(Boolean.valueOf(previous));
                        }
                        if (actual == null || actual.booleanValue() != requested) {
                            Log.e("Aero", "Failed to update memory toggle " + path + ": "
                                    + (!writeResult.isSuccess()
                                    ? writeResult.getError()
                                    : "readback did not match the requested state"));
                            Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!writeResult.isSuccess()) {
                            Log.w("Aero", "Memory toggle readback confirms the requested state "
                                    + "despite a write verification error for " + path + ": "
                                    + writeResult.getError());
                        }
                        if (shouldPersist) {
                            SharedPreferences sharedPrefs = PreferenceManager
                                    .getDefaultSharedPreferences(getActivity());
                            boolean saved = sharedPrefs.edit()
                                    .putString(preference.getName(),
                                            actual.booleanValue() ? "1" : "0")
                                    .commit();
                            if (!saved) {
                                Log.e("Aero", "Failed to save memory toggle preference: "
                                        + preference.getName());
                                Toast.makeText(getActivity(),
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

    private Boolean readToggleValue(String path) {
        SysfsResult<String> result = AeroActivity.hardware.read(new SysfsNode(path));
        if (!result.isSuccess()) {
            Log.e("Aero", "Unable to verify memory toggle " + path + ": "
                    + result.getError());
            return null;
        }
        String value = result.getValue().trim();
        if ("1".equals(value) || "Y".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("0".equals(value) || "2".equals(value) || "N".equalsIgnoreCase(value)
                || "no".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        Log.e("Aero", "Unrecognized memory toggle value for " + path + ": " + value);
        return null;
    }

    /**
     * Handles preference value changes for the I/O scheduler and read-ahead preferences.
     * Updates preference summaries and applies changes through the hardware service.
     *
     * @param preference the preference that changed
     * @param newValue the new value for the preference
     * @return true if the change was handled successfully, false otherwise
     */
    @Override // android.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String value = (String) newValue;
        if (preference == this.mIOScheduler) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final SysfsResult<String> writeResult =
                            AeroActivity.hardware.ioScheduler().writeScheduler(value);
                    final SysfsResult<String> readbackResult =
                            AeroActivity.hardware.ioScheduler().readCurrentScheduler();
                    if (!isAdded()) {
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isAdded()) {
                                return;
                            }
                            if (readbackResult.isSuccess()) {
                                String actualValue = readbackResult.getValue();
                                mIOScheduler.setValue(actualValue);
                                mIOScheduler.setSummary(actualValue);
                            }
                            if (!readbackResult.isSuccess()
                                    || !value.equals(readbackResult.getValue())) {
                                Log.e("Aero", "Failed to change I/O scheduler: "
                                        + (!writeResult.isSuccess()
                                        ? writeResult.getError()
                                        : "readback did not match the requested value"));
                                if (!readbackResult.isSuccess()) {
                                    Log.e("Aero", "Unable to read active I/O scheduler: "
                                            + readbackResult.getError());
                                } else {
                                    Log.w("Aero", "Active I/O scheduler is "
                                            + readbackResult.getValue() + " after requesting "
                                            + value);
                                }
                                Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (!writeResult.isSuccess()) {
                                Log.w("Aero", "I/O scheduler readback confirms the requested "
                                        + "value despite a write verification error: "
                                        + writeResult.getError());
                            }
                            if (PrefCat != null) {
                                root.removePreference(PrefCat);
                            }
                        }
                    });
                }
            }).start();
            return true;
        } else if (preference == this.mReadAHead) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final SysfsResult<String> writeResult = AeroActivity.hardware.write(
                            new SysfsNode(FilePath.READAHEAD_PARAMETER, 0,
                                    Integer.MAX_VALUE), value);
                    final SysfsResult<String> readbackResult = AeroActivity.hardware.read(
                            new SysfsNode(FilePath.READAHEAD_PARAMETER, 0,
                                    Integer.MAX_VALUE));
                    if (!isAdded()) {
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isAdded()) {
                                return;
                            }
                            if (readbackResult.isSuccess()) {
                                String actualValue = readbackResult.getValue();
                                mReadAHead.setValue(actualValue);
                                mReadAHead.setSummary(actualValue);
                            }
                            if (!readbackResult.isSuccess()
                                    || !value.equals(readbackResult.getValue())) {
                                Log.e("Aero", "Failed to update read-ahead: "
                                        + (!writeResult.isSuccess()
                                        ? writeResult.getError()
                                        : "readback did not match the requested value"));
                                if (!readbackResult.isSuccess()) {
                                    Log.e("Aero", "Unable to read active read-ahead value: "
                                            + readbackResult.getError());
                                } else {
                                    Log.w("Aero", "Active read-ahead value is "
                                            + readbackResult.getValue() + " after requesting "
                                            + value);
                                }
                                Toast.makeText(getActivity(), R.string.storage_operation_failed, Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (!writeResult.isSuccess()) {
                                Log.w("Aero", "Read-ahead readback confirms the requested "
                                        + "value despite a write verification error: "
                                        + writeResult.getError());
                            }
                        }
                    });
                }
            }).start();
            return true;
        } else {
            return false;
        }
    }

    private String readProcfsValue(String path) {
        SysfsResult<String> result =
                AeroActivity.hardware.procfs().readValue(new ProcfsNode(path));
        if (!result.isSuccess()) {
            Log.w("Aero", "Unable to read hardware value " + path + ": "
                    + result.getError());
            return "Unavailable";
        }
        return result.getValue();
    }

    private String readSysfsValue(String path) {
        SysfsResult<String> result = AeroActivity.hardware.read(new SysfsNode(path));
        if (!result.isSuccess()) {
            Log.w("Aero", "Unable to read hardware value " + path + ": "
                    + result.getError());
            return "Unavailable";
        }
        return result.getValue();
    }

    private String[] readProcfsLines(String path) {
        SysfsResult<String[]> result =
                AeroActivity.hardware.procfs().readLines(new ProcfsNode(path));
        if (!result.isSuccess()) {
            Log.w("Aero", "Unable to read procfs file " + path + ": "
                    + result.getError());
            return new String[0];
        }
        return result.getValue();
    }

    /**
     * Displays a dialog for configuring random/entropy pool settings. Shows current
     * read and write wakeup thresholds and allows editing them. Validates input and
     * applies changes through the typed hardware service if valid.
     */
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
        editRandomRead.setText(readProcfsValue(FilePath.RANDOM_READ_WAKEUP));
        editRandomWrite.setText(readProcfsValue(FilePath.RANDOM_WRITE_WAKEUP));
        builder.setIcon(R.drawable.puzzle);
        builder.setTitle(((Object) getText(R.string.pref_entropy_settings)) + " ("
                + readProcfsValue(FilePath.RANDOM_POOL_AVAIL) + "/"
                + readProcfsValue(FilePath.RANDOM_POOL_SIZE) + ")");
        builder.setView(layout);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MemoryFragment.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                final String readValue = editRandomRead.getText().toString().trim();
                final String writeValue = editRandomWrite.getText().toString().trim();
                if (!readValue.matches("^\\d+$") || !writeValue.matches("^\\d+$")) {
                    Toast.makeText(MemoryFragment.this.getActivity(), "Please enter a valid non-negative number.", 1).show();
                    return;
                }
                final ProcfsNode readNode = new ProcfsNode(FilePath.RANDOM_READ_WAKEUP, 0,
                        Integer.MAX_VALUE);
                final ProcfsNode writeNode = new ProcfsNode(FilePath.RANDOM_WRITE_WAKEUP, 0,
                        Integer.MAX_VALUE);
                final boolean shouldPersist = MemoryFragment.this.mRandomSettings
                        .isChecked().booleanValue();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final OperationResult result = applyEntropyValues(
                                readNode, writeNode, readValue, writeValue);
                        if (!isAdded()) {
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isAdded()) {
                                    return;
                                }
                                if (!result.isSuccess()) {
                                    Log.e("Aero", "Failed to apply entropy settings: "
                                            + result.getMessage());
                                    Toast.makeText(getActivity(),
                                            R.string.storage_operation_failed,
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                if (shouldPersist) {
                                    SharedPreferences preference = PreferenceManager
                                            .getDefaultSharedPreferences(getActivity());
                                    boolean saved = preference.edit()
                                            .putString("entropy_read_wakeup", readValue)
                                            .putString("entropy_write_wakeup", writeValue)
                                            .putString(mRandomSettings.getKey(), "enabled")
                                            .commit();
                                    if (!saved) {
                                        Log.e("Aero", "Failed to save entropy settings");
                                        Toast.makeText(getActivity(),
                                                R.string.storage_operation_failed,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                    }
                }).start();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MemoryFragment.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.show();
    }

    private OperationResult applyEntropyValues(ProcfsNode readNode, ProcfsNode writeNode,
            String readValue, String writeValue) {
        SysfsResult<String> oldReadResult = AeroActivity.hardware.procfs().readValue(readNode);
        SysfsResult<String> oldWriteResult = AeroActivity.hardware.procfs().readValue(writeNode);
        if (!oldReadResult.isSuccess() || !oldWriteResult.isSuccess()) {
            return OperationResult.failed("Unable to capture existing entropy thresholds: "
                    + (!oldReadResult.isSuccess() ? oldReadResult.getError()
                    : oldWriteResult.getError()));
        }

        String oldReadValue = oldReadResult.getValue().trim();
        String oldWriteValue = oldWriteResult.getValue().trim();
        SysfsResult<String> readWriteResult = AeroActivity.hardware.procfs()
                .writeValue(readNode, readValue);
        SysfsResult<String> readBack = AeroActivity.hardware.procfs().readValue(readNode);
        if (!readBack.isSuccess() || !readValue.equals(readBack.getValue().trim())) {
            restoreEntropyValues(readNode, writeNode, oldReadValue, oldWriteValue);
            return OperationResult.failed("Entropy read threshold did not match the requested value");
        }
        if (!readWriteResult.isSuccess()) {
            Log.w("Aero", "Entropy read threshold readback confirms the requested value despite "
                    + "a write verification error: " + readWriteResult.getError());
        }

        SysfsResult<String> writeWriteResult = AeroActivity.hardware.procfs()
                .writeValue(writeNode, writeValue);
        SysfsResult<String> finalReadBack = AeroActivity.hardware.procfs().readValue(readNode);
        SysfsResult<String> finalWriteBack = AeroActivity.hardware.procfs().readValue(writeNode);
        if (!finalReadBack.isSuccess() || !finalWriteBack.isSuccess()
                || !readValue.equals(finalReadBack.getValue().trim())
                || !writeValue.equals(finalWriteBack.getValue().trim())) {
            restoreEntropyValues(readNode, writeNode, oldReadValue, oldWriteValue);
            return OperationResult.failed("Entropy thresholds did not match the requested values");
        }
        if (!writeWriteResult.isSuccess()) {
            Log.w("Aero", "Entropy write threshold readback confirms the requested value despite "
                    + "a write verification error: " + writeWriteResult.getError());
        }
        return OperationResult.ok();
    }

    private void restoreEntropyValues(ProcfsNode readNode, ProcfsNode writeNode,
            String readValue, String writeValue) {
        SysfsResult<String> readRestore = AeroActivity.hardware.procfs()
                .writeValue(readNode, readValue);
        SysfsResult<String> writeRestore = AeroActivity.hardware.procfs()
                .writeValue(writeNode, writeValue);
        SysfsResult<String> readBack = AeroActivity.hardware.procfs().readValue(readNode);
        SysfsResult<String> writeBack = AeroActivity.hardware.procfs().readValue(writeNode);
        if (!readRestore.isSuccess() || !writeRestore.isSuccess()
                || !readBack.isSuccess() || !writeBack.isSuccess()
                || !readValue.equals(readBack.getValue().trim())
                || !writeValue.equals(writeBack.getValue().trim())) {
            Log.e("Aero", "Unable to fully restore entropy thresholds after a failed update");
        }
    }

    /**
     * Handles the fstrim toggle preference click. Checks for fstrim availability,
     * detects ext3/ext4 filesystems (system, data, cache), and presents a dialog
     * allowing the user to select which partition to trim. Displays a progress
     * dialog while trimming and remounts system partition as necessary.
     */
    private void fsTrimToggleClick() {
        if (!AeroActivity.genHelper.doesExist("/system/xbin/fstrim")) {
            Toast.makeText(getActivity(), R.string.pref_fstrim_no_busybox, 0).show();
            return;
        }
        if (this.mFileSystem == null) {
            SysfsResult<String> mountResult =
                    HardwareServices.memoryMaintenance().readMountTable();
            if (!mountResult.isSuccess()) {
                Log.e("MemoryFragment", mountResult.getError());
                Toast.makeText(getActivity(), R.string.storage_operation_failed,
                        Toast.LENGTH_LONG).show();
                return;
            } else {
                this.mFileSystem = mountResult.getValue();
            }
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

    /**
     * Click listener for filesystem trim dialog items. Handles the selection of a
     * filesystem partition to trim and executes the fstrim operation in a background
     * thread with progress indication.
     */
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
            Runnable runnable = new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.5.1
                @Override // java.lang.Runnable
                public void run() {
                    String mountPoint = b.trim();
                    OperationResult result = HardwareServices.memoryMaintenance()
                            .remountSystemReadWrite();
                    if (result.isSuccess()) {
                        result = HardwareServices.memoryMaintenance()
                                .trimFilesystem(mountPoint);
                    }
                    final OperationResult trimResult = result;
                    if (!trimResult.isSuccess()) {
                        Log.e("MemoryFragment", "Unable to trim " + mountPoint + ": "
                                + trimResult.getMessage());
                    }
                    if (!MemoryFragment.this.isAdded()) {
                        return;
                    }
                    MemoryFragment.this.getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.MemoryFragment.5.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!MemoryFragment.this.isAdded()) {
                                return;
                            }
                            if (AnonymousClass5.this.val$update.isShowing()) {
                                AnonymousClass5.this.val$update.dismiss();
                            }
                            if (!trimResult.isSuccess()) {
                                Toast.makeText(MemoryFragment.this.getActivity(),
                                        R.string.storage_operation_failed,
                                        Toast.LENGTH_LONG).show();
                            }
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

    /**
     * Displays the first-time tutorial showcase overlay for this fragment.
     *
     * @param header the resource ID for the showcase title
     * @param content the resource ID for the showcase content text
     * @param filename the name of the file to mark tutorial as completed
     */
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

    /**
     * Loads and displays I/O scheduler parameters for the current scheduler. Removes
     * the invisible preference placeholder, reads available parameters from the kernel,
     * and dynamically generates preferences for each parameter. Shows a toast if no
     * parameters are available.
     */
    private void loadIOParameter() {
        this.mIOSchedulerHandler.removeInvisiblePreference();
        try {
            SysfsResult<String[]> parameterFiles = AeroActivity.hardware.listSysfsDirectory(
                    new SysfsDirectory(FilePath.GOV_IO_PARAMETER), DirectoryEntryType.FILES);
            if (!parameterFiles.isSuccess()) {
                Log.w("Aero", "Unable to list I/O scheduler parameters: "
                        + parameterFiles.getError());
                Toast.makeText(getActivity(), R.string.pref_io_scheduler_no_parameter, 1).show();
                return;
            }
            String[] completeParamterList = parameterFiles.getValue();
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
