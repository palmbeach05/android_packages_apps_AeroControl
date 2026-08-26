package com.aero.control.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;

/**
 * Applies the user-selected Screen Rotation preference to an activity.
 */
public class OrientationHelper {
    public static final String PREF_KEY = "screen_rotation";

    /**
     * Checks whether screen rotation is enabled in the user preferences.
     *
     * @param context the context to access shared preferences
     * @return true if rotation is enabled, false if locked to portrait
     */
    public static boolean isRotationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_KEY, false);
    }

    /**
     * Applies the user's rotation preference to the specified activity. If rotation
     * is enabled, sets the orientation to sensor-based; otherwise locks to portrait.
     *
     * @param activity the activity to configure
     */
    public static void applyOrientation(Activity activity) {
        activity.setRequestedOrientation(isRotationEnabled(activity)
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}