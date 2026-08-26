package com.aero.control.helpers.PerApp.AppMonitor;

import android.util.Log;

public class AppLogger {
    public static void print(String tag, String message, int level) {
        if (level <= -1) {
            Log.e(tag, message);
        }
    }

    public static int getLogLevel() {
        return -1;
    }
}
