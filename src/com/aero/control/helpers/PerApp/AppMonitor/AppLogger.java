package com.aero.control.helpers.PerApp.AppMonitor;

import android.util.Log;

/**
 * Simple logging utility for the App Monitor module that conditionally logs messages
 * based on a configured log level.
 */
public class AppLogger {
    /**
     * Prints a log message at the specified level if the level meets the threshold.
     *
     * @param tag the log tag to identify the source
     * @param message the message to log
     * @param level the log level (messages at level -1 or below are logged as errors)
     */
    public static void print(String tag, String message, int level) {
        if (level <= -1) {
            Log.e(tag, message);
        }
    }

    /**
     * Returns the current log level threshold for the App Monitor.
     *
     * @return the log level (-1 for error level)
     */
    public static int getLogLevel() {
        return -1;
    }
}
