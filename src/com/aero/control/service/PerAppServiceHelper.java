package com.aero.control.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.Calendar;

/* JADX INFO: loaded from: classes.dex */
public class PerAppServiceHelper {
    private Intent mBackgroundIntent;
    private Context mContext;
    private PendingIntent mPendingIntent = null;
    private SharedPreferences mPrefs;
    private Boolean mState;

    public PerAppServiceHelper(Context context) {
        this.mBackgroundIntent = null;
        this.mContext = context;
        this.mBackgroundIntent = new Intent(this.mContext, (Class<?>) PerAppService.class);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
    }

    public final void setState(boolean state) {
        this.mState = Boolean.valueOf(state);
    }

    public final boolean getState() {
        if (this.mState == null) {
            shouldBeStarted();
        }
        return this.mState.booleanValue();
    }

    public final boolean shouldBeStarted() {
        boolean tmp = this.mPrefs.getBoolean("per_app_service", false);
        if (!tmp) {
            setState(false);
        } else if (tmp) {
            setState(true);
        }
        return getState();
    }

    public final void startService() {
        Calendar.getInstance();
        Log.e("Aero", "Service should be started now!");
        this.mBackgroundIntent = new Intent(this.mContext, (Class<?>) PerAppService.class);
        this.mContext.startService(this.mBackgroundIntent);
        this.mPendingIntent = PendingIntent.getService(this.mContext, 0, this.mBackgroundIntent, 0);
        setState(true);
    }

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
