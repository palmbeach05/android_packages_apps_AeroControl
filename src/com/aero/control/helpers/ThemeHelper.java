package com.aero.control.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.aero.control.R;

/* Handles persisting and applying the user-selected light/dark app theme. */
public class ThemeHelper {
    public static final String PREF_KEY = "app_theme";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static String getTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_KEY, THEME_LIGHT);
    }

    public static void applyTheme(Activity activity) {
        if (THEME_DARK.equals(getTheme(activity))) {
            activity.setTheme(R.style.AeroTheme_Dark);
        } else {
            activity.setTheme(R.style.AeroTheme);
        }
    }
}