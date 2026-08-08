package com.aero.control.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;

/* Applies the user-selected Screen Rotation preference to an activity. */
public class OrientationHelper {
    public static final String PREF_KEY = "screen_rotation";

    public static boolean isRotationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PREF_KEY, false);
    }

    public static void applyOrientation(Activity activity) {
        activity.setRequestedOrientation(isRotationEnabled(activity)
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}