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
import android.os.HandlerThread;
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

/**
 * Background service that monitors the foreground application and applies per-app
 * CPU/GPU/memory profiles when configured apps come to the foreground. Polls every
 * 10 seconds to detect app switches and screen on/off transitions, restoring default
 * settings when appropriate.
 */
public final class PerAppService extends Service {
    private static JobManager mJobManager = null;
    private static final long POLL_INTERVAL_MS = 10000L;
    private static final long INITIAL_USAGE_EVENTS_WINDOW_MS = 1000000L;
    private static final long USAGE_EVENTS_WINDOW_MS = 60000L;
    private static final String perAppProfileHandler = "perAppProfileHandler";
    private boolean mActive;
    private ActivityManager mAm;
    private Context mContext;
    private SharedPreferences mPerAppPrefs;
    private String mProfile;
    private Runnable mRunnable;
    private HandlerThread mPollThread;
    private Handler mPollHandler;
    private volatile boolean mDestroyed;
    private Boolean mLastScreenOnState = null;
    private boolean mHasQueriedUsageEvents;
    private String mLastObservedForegroundApp;
    private static String mPreviousApp = null;
    private static String mCurrentApp = null;
    private static final settingsHelper settingsHelper = new settingsHelper();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private final String mClassName = getClass().getName();
    private boolean mShowToasts = true;

    /**
     * Initializes the service state and starts the foreground-app polling runnable.
        * Usage queries run on a dedicated worker; profile transitions and Toasts remain
        * on the main thread.
     */
    @Override // android.app.Service
    public void onCreate() {
        mDestroyed = false;

        mJobManager = JobManager.instance(this);
        if (this.mContext == null) {
            this.mContext = this;
        }
        this.mPollThread = new HandlerThread("PerAppServicePoller");
        this.mPollThread.start();
        this.mPollHandler = new Handler(this.mPollThread.getLooper());
        if (this.mRunnable == null) {
            this.mRunnable = new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    if (mDestroyed) {
                        return;
                    }
                    boolean enabled = PreferenceManager.getDefaultSharedPreferences(
                            PerAppService.this.mContext).getBoolean("per_app_service", false);
                    if (!enabled) {
                        PerAppService.mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (AeroActivity.perAppService != null) {
                                    AeroActivity.perAppService.setState(false);
                                }
                                PerAppService.this.stopSelf();
                            }
                        });
                        return;
                    }

                    final String foregroundPackage = getForegroundApp();
                    PerAppService.mHandler.post(new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            if (mDestroyed) {
                                return;
                            }
                            if (foregroundPackage != null) {
                                PerAppService.this.runTask(foregroundPackage);
                            }
                            if (!mDestroyed && PerAppService.this.mPollHandler != null) {
                                PerAppService.this.mPollHandler.postDelayed(
                                    PerAppService.this.mRunnable, POLL_INTERVAL_MS);
                            }
                        }
                    });
                }
            };
        }
        this.mPollHandler.post(this.mRunnable);
    }

    /**
     * Stops future polling by marking the service as destroyed and removing pending
     * handler callbacks. The destroyed state also prevents an already-posted callback
     * from scheduling another poll.
     */
    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mRunnable != null) {
            if (mPollHandler != null) {
                mPollHandler.removeCallbacks(mRunnable);
            }
        }
        if (mPollThread != null) {
            mPollThread.quit();
            mPollThread = null;
            mPollHandler = null;
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

    /**
    * Reads the foreground app and, after {@link #setAppData(String)} succeeds, updates the
     * {@link JobManager}. When the screen is on, both app names are non-null, and the
     * foreground app changes, restores defaults if {@code mActive} and applies the
     * matching per-app profile.
     */
    public void runTask() {
        String foregroundPackage = getForegroundApp();
        if (foregroundPackage != null) {
            runTask(foregroundPackage);
        }
    }

    private void runTask(String foregroundPackage) {
        if (this.mPerAppPrefs == null) {
            this.mPerAppPrefs = this.mContext.getSharedPreferences(perAppProfileHandler, 0);
        }
        this.mShowToasts = PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean("per_app_toast", true);
        if (!setAppData(foregroundPackage)) {
            return;
        }
        if (mJobManager != null) {
            mJobManager.setContext(this.mContext);
            AppContext localContext = mJobManager.getAppContext(mCurrentApp);
            mJobManager.setSleep(!isScreenOn());
            mJobManager.schedule(localContext);
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

    private String getForegroundApp() {
        if (this.mAm == null) {
            this.mAm = (ActivityManager) getSystemService("activity");
        }
        String packageName;
        if (Build.VERSION.SDK_INT >= 21) {
            packageName = getTopApp();
        } else {
            List<ActivityManager.RunningTaskInfo> tasks = this.mAm.getRunningTasks(1);
            if (tasks == null || tasks.isEmpty() || tasks.get(0).topActivity == null) {
                packageName = null;
            } else {
                packageName = tasks.get(0).topActivity.getPackageName();
            }
        }
        if (packageName != null) {
            packageName = packageName.trim();
        }
        return packageName == null || packageName.isEmpty() ? null : packageName;
    }

    private boolean setAppData(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        mPreviousApp = mCurrentApp;
        mCurrentApp = packageName.trim();
        return true;
    }

    @TargetApi(21)
    private String getTopApp() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService("usagestats");
        if (usm == null) {
            return null;
        }
        long endTime = System.currentTimeMillis();
        long windowMs = this.mHasQueriedUsageEvents
            ? USAGE_EVENTS_WINDOW_MS : INITIAL_USAGE_EVENTS_WINDOW_MS;
        long startTime = endTime - windowMs;
        UsageEvents events = usm.queryEvents(startTime, endTime);
        this.mHasQueriedUsageEvents = true;
        if (events == null) {
            return this.mLastObservedForegroundApp;
        }
        String foregroundApp = null;
        UsageEvents.Event event = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.getPackageName();
            }
        }
        if (foregroundApp != null) {
            this.mLastObservedForegroundApp = foregroundApp;
        }
        return this.mLastObservedForegroundApp;
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
