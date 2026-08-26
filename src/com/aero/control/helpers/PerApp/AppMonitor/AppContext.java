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

    public final long getLastChecked() {
        if (this.mLastChecked == 0) {
            this.mLastChecked = System.currentTimeMillis();
        }
        return this.mLastChecked;
    }

    public final void increaseTimeUsage(long time) {
        this.mTimeUsage += time;
    }

    public final long getTimeUsage() {
        return this.mTimeUsage;
    }

    public final void setTimeUsage(long timeusage) {
        this.mTimeUsage = timeusage;
    }

    public final String getFormatTimeUsage() {
        return Util.getFormatedTimeString(getTimeUsage());
    }

    public final boolean isAboveThreshold() {
        return getTimeUsage() >= 60000;
    }

    public final void cleanUp() {
        this.mLastChecked = 0L;
        this.mTimeUsage = 0L;
    }

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
