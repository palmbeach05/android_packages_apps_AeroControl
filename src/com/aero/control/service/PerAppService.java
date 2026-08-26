package com.aero.control.service;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.Display;
import android.widget.Toast;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.PerApp.AppMonitor.AppContext;
import com.aero.control.helpers.PerApp.AppMonitor.AppLogger;
import com.aero.control.helpers.PerApp.AppMonitor.JobManager;
import com.aero.control.helpers.settingsHelper;
import java.util.List;
import java.util.Map;

public final class PerAppService extends Service {
    private static JobManager mJobManager = null;
    private static final String perAppProfileHandler = "perAppProfileHandler";
    private boolean mActive;
    private ActivityManager mAm;
    private Context mContext;
    private SharedPreferences mPerAppPrefs;
    private String mProfile;
    private Runnable mRunnable;
    private volatile boolean mDestroyed;
    private Boolean mLastScreenOnState = null;
    private static String mPreviousApp = null;
    private static String mCurrentApp = null;
    private static final settingsHelper settingsHelper = new settingsHelper();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private final String mClassName = getClass().getName();
    private boolean mShowToasts = true;

    @Override // android.app.Service
    public void onCreate() {
        mDestroyed = false;

        mJobManager = JobManager.instance(this);
        if (this.mContext == null) {
            this.mContext = this;
        }
        if (this.mRunnable == null) {
            this.mRunnable = new Runnable() { // from class: com.aero.control.service.PerAppService.1
                @Override // java.lang.Runnable
                public void run() {
                    PerAppService.mHandler.post(new Runnable() { // from class: com.aero.control.service.PerAppService.1.1
                        @Override // java.lang.Runnable
                        public void run() {
                            if (mDestroyed) {
                                return;
                            }

                            boolean enabled = PreferenceManager.getDefaultSharedPreferences(PerAppService.this.mContext).getBoolean("per_app_service", false);
                            if (!enabled) {
                                PerAppService.mHandler.removeCallbacks(PerAppService.this.mRunnable);
                                if (AeroActivity.perAppService != null) {
                                    AeroActivity.perAppService.setState(false);
                                }
                                PerAppService.this.stopSelf();
                                return;
                            }

                            PerAppService.this.runTask();

                            if (!mDestroyed) {
                                PerAppService.mHandler.postDelayed(PerAppService.this.mRunnable, 5000L);
                            }
                        }
                    });
                }
            };
        }
        new Thread(this.mRunnable).start();
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void runTask() {
        if (this.mPerAppPrefs == null) {
            this.mPerAppPrefs = this.mContext.getSharedPreferences(perAppProfileHandler, 0);
        }
        this.mShowToasts = PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean("per_app_toast", true);
        if (!setAppData()) {
            return;
        }
        if (mJobManager != null) {
            mJobManager.setContext(this.mContext);
            AppContext localContext = mJobManager.getAppContext(mCurrentApp);
            mJobManager.setSleep(!isScreenOn());
            if (localContext == null && !mJobManager.getSleepState() && !isScreenOn()) {
                AppLogger.print(this.mClassName, "Shutting down JobManager...", 0);
                mJobManager = null;
            } else {
                mJobManager.schedule(localContext);
            }
            AeroActivity.mJobManager = mJobManager;
        }
        if (isScreenOn() && mPreviousApp != null && mCurrentApp != null && !mPreviousApp.equals(mCurrentApp)) {
            if (this.mActive) {
                if (this.mShowToasts) {
                    Toast.makeText(this.mContext, this.mContext.getText(R.string.return_to_normal), 1).show();
                }
                this.mActive = false;
                this.mProfile = null;
                settingsHelper.executeDefault();
            }
            Map<String, ?> keys = this.mPerAppPrefs.getAll();
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                String savedSelectedProfiles = this.mPerAppPrefs.getString(entry.getKey(), null);
                if (savedSelectedProfiles != null) {
                    String[] tmp = savedSelectedProfiles.replace("+", " ").split(" ");
                    for (String a : tmp) {
                        if (mCurrentApp.equals(a)) {
                            this.mProfile = entry.getKey();
                            if (this.mShowToasts) {
                                Toast.makeText(this.mContext, ((Object) this.mContext.getText(R.string.apply_profile)) + " " + this.mProfile, 1).show();
                            }
                            this.mActive = true;
                            settingsHelper.setSettings(this.mContext, this.mProfile, false);
                        }
                    }
                } else {
                    return;
                }
            }
        }
    }

    private boolean setAppData() {
        String PackageName;
        if (this.mAm == null) {
            this.mAm = (ActivityManager) getSystemService("activity");
        }
        if (Build.VERSION.SDK_INT >= 21) {
            PackageName = getTopApp();
        } else {
            List<ActivityManager.RunningTaskInfo> tasks = this.mAm.getRunningTasks(1);
            if (tasks == null || tasks.isEmpty() || tasks.get(0).topActivity == null) {
                PackageName = null;
            } else {
                PackageName = tasks.get(0).topActivity.getPackageName();
            }
        }
        if (PackageName != null) {
            PackageName = PackageName.trim();
        }
        if (PackageName == null || PackageName.isEmpty()) {
            return false;
        }
        mPreviousApp = mCurrentApp;
        mCurrentApp = PackageName;
        return true;
    }

    @TargetApi(21)
    private String getTopApp() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService("usagestats");
        if (usm == null) {
            return null;
        }
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 1000000;
        UsageEvents events = usm.queryEvents(startTime, endTime);
        if (events == null) {
            return null;
        }
        String foregroundApp = null;
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.getPackageName();
            }
        }
        return foregroundApp;
    }

    private boolean isScreenOn() {
        boolean screenOn;
        if (Build.VERSION.SDK_INT > 19) {
            DisplayManager dm = (DisplayManager) this.mContext.getSystemService("display");
            screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                    break;
                }
            }
        } else {
            PowerManager pm = (PowerManager) getSystemService("power");
            screenOn = pm.isScreenOn();
        }
        if (this.mLastScreenOnState == null || this.mLastScreenOnState.booleanValue() != screenOn) {
            AppLogger.print(this.mClassName, "Screen state changed, isScreenOn=" + screenOn, -1);
            this.mLastScreenOnState = Boolean.valueOf(screenOn);
        }
        return screenOn;
    }
}
