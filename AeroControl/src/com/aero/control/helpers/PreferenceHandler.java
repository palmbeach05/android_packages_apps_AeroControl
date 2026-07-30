package com.aero.control.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ViewConfiguration;
import com.aero.control.AeroActivity;
import com.aero.control.helpers.Android.CustomTextPreference;

/* JADX INFO: loaded from: classes.dex */
public class PreferenceHandler {
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String PREF_BLANKED = "BLANKED";
    public Context mContext;
    private boolean mInvisibleAdded = false;
    public PreferenceCategory mPrefCat;
    public PreferenceManager mPrefMan;
    private SharedPreferences mPreferences;

    public PreferenceHandler(Context context, PreferenceCategory PrefCat, PreferenceManager PrefMan) {
        this.mContext = context;
        this.mPrefCat = PrefCat;
        this.mPrefMan = PrefMan;
    }

    public final void removeInvisiblePreference() {
        if (this.mInvisibleAdded) {
            for (int i = 0; i < this.mPrefCat.getPreferenceCount(); i++) {
                if (this.mPrefCat.getPreference(i).getKey().equals(PREF_BLANKED)) {
                    this.mPrefCat.removePreference(this.mPrefCat.getPreference(i));
                    this.mInvisibleAdded = false;
                }
            }
        }
    }

    public final void addInvisiblePreference() {
        if (this.mPrefCat != null && this.mContext != null && !this.mInvisibleAdded && Build.VERSION.SDK_INT >= 19 && !ViewConfiguration.get(this.mContext).hasPermanentMenuKey()) {
            Preference blankedPref = new Preference(this.mContext);
            blankedPref.setSelectable(false);
            blankedPref.setKey(PREF_BLANKED);
            this.mPrefCat.addPreference(blankedPref);
            this.mInvisibleAdded = true;
        }
    }

    public final void genPrefFromDictionary(String[] array, String path) {
        int counter = array.length;
        int i = 0;
        for (String b : array) {
            generateSettings(b, path, false);
            i++;
            if (i == counter) {
                addInvisiblePreference();
            }
        }
    }

    public final void genPrefFromFiles(String[] nameArray, String[] paraArray, Boolean showEmpty) {
        int counter = nameArray.length;
        int i = 0;
        for (int j = 0; j < nameArray.length; j++) {
            if (nameArray[j].equals("vtg_level") || nameArray[j].equals("amp")) {
                generateSettings(nameArray[j], paraArray[j], true);
            } else {
                generateSettings(nameArray[j], paraArray[j], false);
            }
            i++;
            if (i == counter && showEmpty.booleanValue()) {
                addInvisiblePreference();
            }
        }
    }

    public final void genPrefFromSingleFile(String path) {
        removeInvisiblePreference();
        String[] array = path.split("/");
        String paraName = "";
        int i = 0;
        for (String a : array) {
            if (array.length - 1 == i) {
                paraName = a;
            }
            i++;
        }
        generateSettings(paraName, path.replace("/" + paraName, ""), false);
    }

    private void generateSettings(String parameter, String path, final boolean flag) {
        final CustomTextPreference prefload = new CustomTextPreference(this.mContext);
        final String parameterPath = path + "/" + parameter;
        String summary = AeroActivity.shell.getInfo(parameterPath);
        if (!summary.equals(NO_DATA_FOUND) && !parameter.equals("uevent") && !parameter.equals("dev") && AeroActivity.genHelper.doesExist(parameterPath)) {
            Integer tmp = null;
            try {
                tmp = Integer.valueOf(Integer.parseInt(summary));
            } catch (NumberFormatException e) {
            }
            if (tmp != null) {
                prefload.getEditText().setInputType(2);
            }
            this.mPreferences = this.mPrefMan.getSharedPreferences();
            if (this.mPreferences.getString(parameterPath, null) != null) {
                prefload.setChecked(true);
            }
            prefload.setPrefSummary(summary);
            prefload.setTitle(parameter);
            prefload.setText(summary);
            prefload.setPrefText(parameter);
            prefload.setDialogTitle(parameter);
            prefload.setName(parameterPath);
            if (prefload.getPrefSummary().equals(NO_DATA_FOUND)) {
                prefload.setEnabled(false);
                prefload.setPrefSummary("This value can't be changed.");
            }
            this.mPrefCat.addPreference(prefload);
            prefload.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.aero.control.helpers.PreferenceHandler.1
                @Override // android.preference.Preference.OnPreferenceChangeListener
                public boolean onPreferenceChange(Preference preference, Object o) {
                    String a = (String) o;
                    if (a.equals("")) {
                        return false;
                    }
                    AeroActivity.shell.setRootInfo(a, parameterPath);
                    prefload.setPrefSummary(a);
                    if (prefload.isChecked().booleanValue()) {
                        PreferenceHandler.this.mPreferences.edit().putString(parameterPath, o.toString()).commit();
                    }
                    if (!flag) {
                        return true;
                    }
                    PreferenceHandler.this.forceVibration();
                    return true;
                }
            });
        }
    }

    public void forceVibration() {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            Log.e("Aero", "Something interrupted the main Thread, try again.", e);
        }
        Context context = this.mContext;
        Context context2 = this.mContext;
        Vibrator vibrate = (Vibrator) context.getSystemService("vibrator");
        vibrate.vibrate(500L);
    }
}
