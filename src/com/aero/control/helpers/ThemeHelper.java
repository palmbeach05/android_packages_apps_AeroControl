package com.aero.control.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.aero.control.R;

/* Handles persisting and applying the user-selected light/dark/system app theme. */
public class ThemeHelper {
    public static final String PREF_KEY = "app_theme";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    public static String getTheme(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = prefs.getString(PREF_KEY, THEME_LIGHT);

        // Validate theme against known constants; migrate legacy values to THEME_LIGHT
        if (!THEME_LIGHT.equals(theme) && !THEME_DARK.equals(theme) && !THEME_SYSTEM.equals(theme)) {
            prefs.edit().putString(PREF_KEY, THEME_LIGHT).apply();
            return THEME_LIGHT;
        }

        return theme;
    }

    public static void applyTheme(Activity activity) {
        String theme = getTheme(activity);
        // The System App Theme option is only exposed to users on API 21+
        // (see res/values-v21/arrays.xml); guard here as well so a value
        // synced from a newer device onto an older one can't try to apply a
        // style that doesn't exist below API 21.
        if (THEME_SYSTEM.equals(theme) && Build.VERSION.SDK_INT >= 21) {
            activity.setTheme(R.style.AeroTheme_System);
        } else if (THEME_DARK.equals(theme)) {
            activity.setTheme(R.style.AeroTheme_Dark);
        } else {
            activity.setTheme(R.style.AeroTheme);
        }
    }

    public static void applySettingsTheme(Activity activity) {
        if (Build.VERSION.SDK_INT < 21) {
            applyTheme(activity);
            return;
        }

        String theme = getTheme(activity);
        if (THEME_SYSTEM.equals(theme)) {
            activity.setTheme(R.style.AeroSettingsTheme_System);
        } else if (THEME_DARK.equals(theme)) {
            activity.setTheme(R.style.AeroSettingsTheme_Dark);
        } else {
            activity.setTheme(R.style.AeroSettingsTheme);
        }
    }
}