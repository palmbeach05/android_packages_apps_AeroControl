package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.aero.control.helpers.Util;

/**
 * Context information for a monitored application. Tracks the app name, total
 * time spent in the foreground, and the last time it was checked by the monitoring service.
 */
public final class AppContext {
    private String mAppName;
    private long mTimeUsage = 0;
    private long mLastChecked = 0;

    /**
     * Creates an app context for the specified app.
     *
     * @param appname the package name of the app to monitor
     */
    public AppContext(String appname) {
        if (this.mAppName != null) {
            throw new ExceptionHandler(ExceptionHandler.EX_APP_NAME_OVERRIDE + " (" + this.mAppName + ") ");
        }
        this.mAppName = appname;
    }

    /**
     * Returns the package name of this app.
     *
     * @return the app package name
     */
    public final String getAppName() {
        return this.mAppName;
    }

    /**
     * Sets the last checked timestamp to the current time.
     */
    public final void setLastCheckedNow() {
        this.mLastChecked = System.currentTimeMillis();
    }

    /**
     * Sets the last checked timestamp.
     *
     * @param lastchecked the timestamp in milliseconds
     */
    public final void setLastChecked(long lastchecked) {
        this.mLastChecked = lastchecked;
    }

    /**
     * Returns the last checked timestamp, initializing it to the current time if not yet set.
     *
     * @return the last checked timestamp in milliseconds
     */
    public final long getLastChecked() {
        if (this.mLastChecked == 0) {
            this.mLastChecked = System.currentTimeMillis();
        }
        return this.mLastChecked;
    }

    /**
     * Increases the total time usage for this app by the specified duration.
     *
     * @param time the duration to add in milliseconds
     */
    public final void increaseTimeUsage(long time) {
        this.mTimeUsage += time;
    }

    /**
     * Returns the total time the app has been in the foreground.
     *
     * @return the total time usage in milliseconds
     */
    public final long getTimeUsage() {
        return this.mTimeUsage;
    }

    /**
     * Sets the total time usage for this app.
     *
     * @param timeusage the time usage in milliseconds
     */
    public final void setTimeUsage(long timeusage) {
        this.mTimeUsage = timeusage;
    }

    /**
     * Returns the total time usage formatted as a human-readable string.
     *
     * @return formatted time string (e.g., "2 h 30 min 15 secs")
     */
    public final String getFormatTimeUsage() {
        return Util.getFormatedTimeString(getTimeUsage());
    }

    /**
     * Checks if the app has been used for at least the minimum threshold duration.
     *
     * @return true if time usage exceeds 60 seconds, false otherwise
     */
    public final boolean isAboveThreshold() {
        return getTimeUsage() >= 60000;
    }

    /**
     * Resets all monitoring data for this app, clearing time usage and last checked timestamp.
     */
    public final void cleanUp() {
        this.mLastChecked = 0L;
        this.mTimeUsage = 0L;
    }

    /**
     * Retrieves the human-readable app name from the package manager.
     *
     * @param context the context to access the package manager
     * @return the app's display name, or null if the app is not found
     */
    public final String getRealAppName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(getAppName(), 0);
            return (String) pm.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
