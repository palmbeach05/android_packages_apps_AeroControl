package com.aero.control.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Calendar;

/**
 * Helper class for managing the lifecycle of the per-app monitoring background service.
 * Provides methods to start, stop, and check the state of the monitoring service based
 * on user preferences.
 */
public class PerAppServiceHelper {
    private Intent mBackgroundIntent;
    private Context mContext;
    private PendingIntent mPendingIntent = null;
    private SharedPreferences mPrefs;
    private Boolean mState;

    /**
     * Creates a new helper for managing the per-app monitoring service.
     *
     * @param context the context used to start and stop the service
     */
    public PerAppServiceHelper(Context context) {
        this.mBackgroundIntent = null;
        this.mContext = context;
        this.mBackgroundIntent = new Intent(this.mContext, (Class<?>) PerAppService.class);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    /**
     * Sets the current state of the service.
     *
     * @param state true if the service is running, false otherwise
     */
    public final void setState(boolean state) {
        this.mState = Boolean.valueOf(state);
    }

    /**
     * Returns the cached state of the service based on the mState field and preference
     * setting. This reflects the preference-backed state rather than whether PerAppService
     * is currently running.
     *
     * @return true if the service state is cached as active, false otherwise
     */
    public final boolean getState() {
        if (this.mState == null) {
            shouldBeStarted();
        }
        return this.mState.booleanValue();
    }

    /**
     * Checks the user preference to determine if the service should be running.
     *
     * @return true if the service should be started, false otherwise
     */
    public final boolean shouldBeStarted() {
        boolean tmp = this.mPrefs.getBoolean("per_app_service", false);
        if (!tmp) {
            setState(false);
        } else if (tmp) {
            setState(true);
        }
        return getState();
    }

    /**
     * Starts the per-app monitoring background service.
     */
    public final void startService() {
        Calendar.getInstance();
        Log.e("Aero", "Service should be started now!");
        this.mBackgroundIntent = new Intent(this.mContext, (Class<?>) PerAppService.class);
        this.mContext.startService(this.mBackgroundIntent);
        this.mPendingIntent = PendingIntent.getService(this.mContext, 0, this.mBackgroundIntent, 0);
        setState(true);
    }

    /**
     * Stops the per-app monitoring background service and cancels any pending intents.
     */
    public final void stopService() {
        this.mContext.stopService(new Intent(this.mContext, (Class<?>) PerAppService.class));
        if (this.mBackgroundIntent != null) {
            this.mContext.stopService(this.mBackgroundIntent);
        }
        if (this.mPendingIntent != null) {
            this.mPendingIntent.cancel();
        }
        this.mBackgroundIntent = null;
        this.mPendingIntent = null;
        setState(false);
        Log.e("Aero", "Service should be stopped now!");
    }
}
