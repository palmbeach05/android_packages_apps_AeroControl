package com.aero.control.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import com.aero.control.R;

/**
 * Handles persisting and applying the user-selected light/dark/system app theme.
 * Provides methods to retrieve the current theme preference and apply it to activities.
 */
public class ThemeHelper {
    public static final String PREF_KEY = "app_theme";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    /**
     * Retrieves the user's theme preference from shared preferences. Validates
     * the stored value and falls back to light theme if invalid.
     *
     * @param context the context to access shared preferences
     * @return the theme identifier (THEME_LIGHT, THEME_DARK, or THEME_SYSTEM)
     */
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

    /**
     * Applies the user's selected theme to the specified activity. Should be
     * called before setContentView in onCreate.
     *
     * @param activity the activity to apply the theme to
     */
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

    /**
     * Applies the user's selected theme to a settings activity, using the settings-specific
     * theme variants that include Material Design card styling on API 21+.
     *
     * @param activity the settings activity to apply the theme to
     */
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