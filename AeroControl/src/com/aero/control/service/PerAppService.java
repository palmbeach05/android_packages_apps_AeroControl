package com.aero.control.service;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
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
import java.util.SortedMap;
import java.util.TreeMap;

/* JADX INFO: loaded from: classes.dex */
public final class PerAppService extends Service {
    private static JobManager mJobManager = null;
    private static final String perAppProfileHandler = "perAppProfileHandler";
    private boolean mActive;
    private ActivityManager mAm;
    private Context mContext;
    private SharedPreferences mPerAppPrefs;
    private String mProfile;
    private Runnable mRunnable;
    private boolean mDestroyed;
    private static String mPreviousApp = null;
    private static String mCurrentApp = null;
    private static final settingsHelper settingsHelper = new settingsHelper();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private final String mClassName = getClass().getName();
    private boolean mShowToasts = true;

    @Override // android.app.Service
    public void onCreate() {
        mDestroyed = false;

        final boolean enabled = AeroActivity.perAppService != null ? AeroActivity.perAppService.getState() : true;
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

                            if (enabled) {
                                PerAppService.this.runTask();
                            }

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

    /* JADX INFO: Access modifiers changed from: private */
    public void runTask() {
        if (this.mPerAppPrefs == null) {
            this.mPerAppPrefs = this.mContext.getSharedPreferences(perAppProfileHandler, 0);
        }
        this.mShowToasts = PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean("per_app_toast", true);
        setAppData();
        if (mJobManager != null) {
            mJobManager.setContext(this.mContext);
            AppContext localContext = mJobManager.getAppContext(mCurrentApp);
            mJobManager.setSleep(isScreenOn());
            if (localContext == null && !mJobManager.getSleepState() && isScreenOn()) {
                AppLogger.print(this.mClassName, "Shutting down JobManager...", 0);
                mJobManager = null;
            } else {
                mJobManager.schedule(localContext);
            }
            AeroActivity.mJobManager = mJobManager;
        }
        if (!isScreenOn() && mPreviousApp != null && mCurrentApp != null && !mPreviousApp.equals(mCurrentApp)) {
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

    private void setAppData() {
        String PackageName;
        String PackageName2 = mCurrentApp;
        mPreviousApp = PackageName2;
        if (this.mAm == null) {
            this.mAm = (ActivityManager) getSystemService("activity");
        }
        if (Build.VERSION.SDK_INT > 19) {
            PackageName = getTopApp();
        } else {
            ActivityManager.RunningTaskInfo AppInfo = this.mAm.getRunningTasks(1).get(0);
            PackageName = AppInfo.topActivity.getPackageName();
        }
        if (PackageName != null) {
            PackageName = PackageName.trim();
        }
        mCurrentApp = PackageName;
    }

    @TargetApi(21)
    private String getTopApp() {
        long time = System.currentTimeMillis();
        UsageStatsManager usm = (UsageStatsManager) getSystemService("usagestats");
        List<UsageStats> appList = usm.queryUsageStats(0, time - 1000000, time);
        if (appList == null || appList.size() <= 0) {
            return null;
        }
        SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
        for (UsageStats usageStats : appList) {
            sortedMap.put(Long.valueOf(usageStats.getLastTimeUsed()), usageStats);
        }
        if (sortedMap.isEmpty()) {
            return null;
        }
        String visibleApp = sortedMap.get(sortedMap.lastKey()).getPackageName();
        return visibleApp;
    }

    private boolean isScreenOn() {
        if (Build.VERSION.SDK_INT > 19) {
            Context context = this.mContext;
            Context context2 = this.mContext;
            DisplayManager dm = (DisplayManager) context.getSystemService("display");
            Display[] arr$ = dm.getDisplays();
            for (Display display : arr$) {
                if (display.getState() != 1) {
                    return false;
                }
            }
            return true;
        }
        PowerManager pm = (PowerManager) getSystemService("power");
        return !pm.isScreenOn();
    }
}