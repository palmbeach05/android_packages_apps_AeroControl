package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.CustomListPreference;
import com.aero.control.helpers.FileManager.FileManager;
import com.aero.control.helpers.FileManager.FileManagerListener;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PreferenceHandler;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class MiscSettingsFragment extends PlaceHolderFragment implements FileManagerListener {
    public static final String FILENAME_MISC = "firstrun_misc";
    private static final String MISC_SETTINGS_STORAGE = "miscSettingsStorage";
    private PreferenceCategory PrefCat;
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
        Context context = this.mContext;
        Context context2 = this.mContext;
        this.mMiscSettings = context.getSharedPreferences(MISC_SETTINGS_STORAGE, 0);
        loadParalist();
        loadSettings();
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
                        Toast.makeText(this.mContext, R.string.pref_misc_no_settings, 1).show();
                    } else {
                        for (String str : allMiscSettings) {
                            miscSettingsDelete.add(false);
                        }
                        final String[] preferenceData = (String[]) allMiscSettings.toArray(new String[0]);
                        dialog.setTitle(R.string.pref_misc_delete_misc);
                        dialog.setIcon(R.drawable.warning);
                        dialog.setMultiChoiceItems(preferenceData, (boolean[]) null, new DialogInterface.OnMultiChoiceClickListener() { // from class: com.aero.control.fragments.MiscSettingsFragment.3
                            @Override // android.content.DialogInterface.OnMultiChoiceClickListener
                            public void onClick(DialogInterface dialogInterface, int i2, boolean b) {
                                if (b) {
                                    miscSettingsDelete.add(i2, true);
                                } else {
                                    miscSettingsDelete.add(i2, false);
                                }
                            }
                        }).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MiscSettingsFragment.2
                            @Override // android.content.DialogInterface.OnClickListener
                            public void onClick(DialogInterface dialog2, int id) {
                                SharedPreferences.Editor editor = MiscSettingsFragment.this.mMiscSettings.edit();
                                SharedPreferences.Editor aero_editor = MiscSettingsFragment.this.mPrefs.edit();
                                Map<String, ?> keys = MiscSettingsFragment.this.mMiscSettings.getAll();
                                int i2 = 0;
                                String[] arr$ = preferenceData;
                                for (String str2 : arr$) {
                                    if (((Boolean) miscSettingsDelete.get(i2)).booleanValue()) {
                                        for (Map.Entry<String, ?> entry : keys.entrySet()) {
                                            String key = entry.getKey();
                                            String value = entry.getValue().toString();
                                            if (preferenceData[i2].equals(value)) {
                                                editor.remove(key).commit();
                                                aero_editor.remove(key).commit();
                                            }
                                        }
                                    }
                                    i2++;
                                }
                                MiscSettingsFragment.this.root.removePreference(MiscSettingsFragment.this.mMiscCat);
                                MiscSettingsFragment.this.mMiscCat = null;
                                MiscSettingsFragment.this.initMisc();
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.MiscSettingsFragment.1
                            @Override // android.content.DialogInterface.OnClickListener
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

    public void DrawFirstStart(int header, int content) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(FILENAME_MISC, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.MiscSettingsFragment.4
            @Override // com.github.amlcurran.showcaseview.targets.Target
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
                Toast.makeText(this.mContext, "This tunable was already added!", 1).show();
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

    /* JADX INFO: Access modifiers changed from: private */
    public void initMisc() {
        Map<String, ?> keys = this.mMiscSettings.getAll();
        int i = 0;
        if (this.mMiscCat == null) {
            this.mMiscCat = new PreferenceCategory(this.mContext);
            this.mMiscCat.setTitle(R.string.pref_misc_your_settings);
            this.root.addPreference(this.mMiscCat);
            this.mHandler = new PreferenceHandler(this.mContext, this.mMiscCat, getPreferenceManager());
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                String key = entry.getKey();
                this.mHandler.genPrefFromSingleFile(key);
                i++;
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
        tcpPreference.setSummary(AeroActivity.shell.getInfo(FilePath.MISC_TCP_CONGESTION_CURRENT));
        tcpPreference.setValue(AeroActivity.shell.getInfo(FilePath.MISC_TCP_CONGESTION_CURRENT));
        tcpPreference.setEntries(AeroActivity.shell.getInfoArray(FilePath.MISC_TCP_CONGESTION_AVAILABLE, 0, 0));
        tcpPreference.setEntryValues(AeroActivity.shell.getInfoArray(FilePath.MISC_TCP_CONGESTION_AVAILABLE, 0, 0));
        if (AeroActivity.genHelper.doesExist(FilePath.MISC_TCP_CONGESTION_AVAILABLE)) {
            this.PrefCat.addPreference(tcpPreference);
        }
        tcpPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.fragments.MiscSettingsFragment.5
            @Override // android.preference.Preference.OnPreferenceChangeListener
            public boolean onPreferenceChange(Preference preference, Object o) {
                String a = (String) o;
                AeroActivity.shell.setRootInfo(a, FilePath.MISC_TCP_CONGESTION_CURRENT);
                tcpPreference.setSummary(a);
                return true;
            }
        });
    }
}
