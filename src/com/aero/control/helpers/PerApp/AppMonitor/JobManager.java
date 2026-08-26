package com.aero.control.helpers.PerApp.AppMonitor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElement;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElementDetail;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Central manager for the per-app monitoring system. Coordinates data collection from
 * monitoring modules, manages app contexts, handles data import/export to JSON, and
 * provides notification support for the monitoring service.
 */
public final class JobManager {
    private static final String FILENAME_APPMONITOR_NOTIFY = "appmonitor_notify";
    private static JobManager mJobManager = null;
    private static final String mPreferenceValue = "per_app_monitor";
    private AppModuleData mAppModuleData;
    private Context mContext;
    private final String mClassName = getClass().getName();
    private boolean mJobManagerEnable = true;
    private boolean mSleeping = false;
    private boolean mPrevSleeping = false;
    private boolean mNotifcationShowed = false;
    private long mExportThreshold = 0;
    private AppData mAppData = new AppData();
    private List<AppModule> mModules = new ArrayList();

    private JobManager(Context context) {
        this.mContext = context;
        this.mJobManagerEnable = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(mPreferenceValue, true);
        loadModules();
        this.mAppModuleData = new AppModuleData(getModules());
        Runnable run = new Runnable() { // from class: com.aero.control.helpers.PerApp.AppMonitor.JobManager.1
            @Override // java.lang.Runnable
            public void run() {
                try {
                    JobManager.this.importData();
                } catch (OutOfMemoryError e) {
                    AppLogger.print(JobManager.this.mClassName, "We tried to import to much data, deleting import file..." + e, 0);
                    new File(new ContextWrapper(JobManager.this.mContext).getFilesDir() + "/" + Configuration.EMERGENCY_FILE).delete();
                }
            }
        };
        Thread worker = new Thread(run);
        worker.start();
        AppLogger.print(this.mClassName, "JobManager initialized, AppMonitor Version " + getVersion() + " loaded!", -1);
    }

    /**
     * Returns the singleton JobManager instance, creating it if necessary.
     *
     * @param context the context for initializing the manager
     * @return the shared JobManager instance
     */
    public static synchronized JobManager instance(Context context) {
        if (mJobManager == null) {
            mJobManager = new JobManager(context);
        }
        return mJobManager;
    }

    /**
     * Enables the job manager to allow data collection.
     */
    public final void enable() {
        this.mJobManagerEnable = true;
        AppLogger.print(this.mClassName, "JobManager enabled!", 0);
    }

    /**
     * Disables the job manager to pause data collection.
     */
    public final void disable() {
        this.mJobManagerEnable = false;
        AppLogger.print(this.mClassName, "JobManager disabled!", 0);
    }

    /**
     * Returns the AppMonitor framework version string.
     *
     * @return the version string from Configuration
     */
    public final String getVersion() {
        return Configuration.APPMONITOR_VERSION;
    }

    /**
     * Returns whether the job manager is currently enabled for data collection.
     *
     * @return true if enabled, false otherwise
     */
    public final boolean getJobManagerState() {
        return this.mJobManagerEnable;
    }

    /**
     * Updates the context used by the job manager.
     *
     * @param context the new context
     */
    public final void setContext(Context context) {
        this.mContext = context;
    }

    /**
     * Forces cleanup of all module data for a specific app, resetting its monitoring state.
     *
     * @param appname the package name of the app to clean up
     */
    public final void forceCleanUp(String appname) {
        AppContext context = getSimpleAppContext(appname);
        if (context != null) {
            AppModuleMetaData appModuleMetaData = this.mAppModuleData.existsAppModuleMetaData(context);
            if (appModuleMetaData != null) {
                appModuleMetaData.cleanUp();
            }
            context.cleanUp();
        }
    }

    /**
     * Retrieves all monitoring data organized as parent-child elements for display in
     * the expandable list UI. Only includes apps that have exceeded the usage threshold.
     *
     * @param context the context for accessing package information
     * @return a list of AppElement objects sorted by usage time in descending order
     */
    public final synchronized List<AppElement> getParentChildData(Context context) {
        List<AppElement> data;
        Drawable appicon;
        data = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        List<AppModuleMetaData> appModuleMetaData = getModuleData().getAppModuleData();
        for (AppModuleMetaData ammd : appModuleMetaData) {
            AppLogger.print(this.mClassName, "App Module Data found! (" + ammd.getAppContext().getAppName() + ")", 2);
            AppLogger.print(this.mClassName, ammd.getAppContext().getAppName() + " Time used: (" + ammd.getAppContext().getTimeUsage() + "ms) :", 2);
            if (ammd.getAppContext().isAboveThreshold()) {
                try {
                    appicon = pm.getApplicationIcon(ammd.getAppContext().getAppName());
                } catch (PackageManager.NameNotFoundException e) {
                    appicon = null;
                }
                AppElement parentData = new AppElement(ammd.getAppContext().getAppName(), appicon);
                parentData.setUsage(Long.valueOf(ammd.getAppContext().getTimeUsage()));
                parentData.setRealName(ammd.getAppContext().getRealAppName(context));
                parentData.getChildData().add(new AppElementDetail(ammd.getAppContext().getFormatTimeUsage(), ""));
                for (AppModule module : getModules()) {
                    parentData.getChildData().add(new AppElementDetail(module.getPrefix(), ammd.getAverage(module.getIdentifier()) + module.getSuffix()));
                    AppLogger.print(this.mClassName, "------ Average: " + ammd.getAverage(module.getIdentifier()), 2);
                }
                if (parentData.getRealName() != null) {
                    data.add(parentData);
                }
            }
        }
        Collections.sort(data, new Comparator<AppElement>() { // from class: com.aero.control.helpers.PerApp.AppMonitor.JobManager.2
            @Override // java.util.Comparator
            public int compare(AppElement lhs, AppElement rhs) {
                return rhs.getUsage().compareTo(lhs.getUsage());
            }
        });
        return data;
    }

    /**
     * Retrieves the raw monitoring data for a specific app and module.
     *
     * @param appname the package name of the app
     * @param identifier the module identifier to retrieve data for
     * @return a list of raw integer values collected by the module, or null if app not found
     */
    public final List<Integer> getRawData(String appname, int identifier) {
        AppContext context = getSimpleAppContext(appname);
        if (context == null) {
            return null;
        }
        for (AppModuleMetaData ammd : getModuleData().getAppModuleData()) {
            if (ammd.getAppContext() == context) {
                return ammd.getRawData(identifier);
            }
        }
        return null;
    }

    /**
     * Exports all collected monitoring data to a JSON file for persistence across
     * service restarts or app crashes. Writes to the emergency file location.
     */
    public void exportData() {
        JSONObject jSONObject = new JSONObject();
        long time = System.currentTimeMillis();
        new File(new ContextWrapper(this.mContext).getFilesDir() + "/" + Configuration.EMERGENCY_FILE).delete();
        AppLogger.print(this.mClassName, "Starting emergency write of data...", 0);
        try {
            for (AppContext context : this.mAppData.getAppList()) {
                JSONObject jSONObject2 = new JSONObject();
                JSONObject jSONObject3 = new JSONObject();
                jSONObject3.put("TimeUsed", context.getTimeUsage());
                jSONObject3.put("LastChecked", context.getLastChecked());
                jSONObject3.put("AppMonitorVersion", getVersion());
                AppLogger.print(this.mClassName, "Starting export for: " + context.getAppName(), 1);
                List<AppModuleMetaData> moduleMetaData = Collections.synchronizedList(getModuleData().getAppModuleData());
                synchronized (moduleMetaData) {
                    for (AppModuleMetaData ammd : moduleMetaData) {
                        if (ammd.getAppContext() == context) {
                            AppLogger.print(this.mClassName, "Current Context: " + context.getAppName(), 1);
                            for (AppModule module : this.mModules) {
                                JSONObject appModule = new JSONObject();
                                JSONArray values = new JSONArray();
                                AppLogger.print(this.mClassName, "Adding Data for module: " + module.getName(), 1);
                                List<Integer> currentValues = Collections.synchronizedList(ammd.getRawData(module.getIdentifier()));
                                synchronized (currentValues) {
                                    for (Integer i : currentValues) {
                                        values.put(i);
                                    }
                                }
                                appModule.put("Values", values);
                                jSONObject3.put(module.getIdentifier() + "", appModule);
                            }
                        }
                    }
                }
                jSONObject2.put(context.getRealAppName(this.mContext), jSONObject3);
                jSONObject.put(context.getAppName(), jSONObject2);
            }
        } catch (OutOfMemoryError e) {
            AppLogger.print(this.mClassName, "We got OOM, forcing cleanup! Exception: " + e, 0);
            for (AppModuleMetaData ammd2 : getModuleData().getAppModuleData()) {
                forceCleanUp(ammd2.getAppContext().getAppName());
            }
        } catch (JSONException e2) {
        }
        AppLogger.print(this.mClassName, "Data gathered, writing to disk..", 1);
        try {
            FileOutputStream fos = this.mContext.openFileOutput(Configuration.EMERGENCY_FILE, 0);
            BufferedOutputStream bos = new BufferedOutputStream(fos, 8192);
            try {
                bos.write(jSONObject.toString().getBytes());
            } catch (OutOfMemoryError e3) {
                AppLogger.print(this.mClassName, "We tried to save a too large file, forcing cleanup! Exception: " + e3, 0);
                for (AppModuleMetaData ammd3 : getModuleData().getAppModuleData()) {
                    forceCleanUp(ammd3.getAppContext().getAppName());
                }
            }
            bos.flush();
            bos.close();
            AppLogger.print(this.mClassName, "Data successfully written to disk in (" + (System.currentTimeMillis() - time) + " ms).", 0);
        } catch (IOException e4) {
            AppLogger.print(this.mClassName, "Error during data-write..." + e4, 0);
        }
    }

    /**
     * Imports previously exported monitoring data from the emergency JSON file,
     * restoring app contexts and module data.
     */
    public void importData() {
        ContextWrapper cw = new ContextWrapper(this.mContext);
        long time = System.currentTimeMillis();
        this.mSleeping = true;
        if (AeroActivity.genHelper.doesExist(cw.getFilesDir() + "/" + Configuration.EMERGENCY_FILE)) {
            AppLogger.print(this.mClassName, "Emergency file detected, starting import... ", 0);
            try {
                InputStream is = this.mContext.openFileInput(Configuration.EMERGENCY_FILE);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String tmp = new String(buffer, "UTF-8");
                this.mAppData.clearData();
                this.mModules.clear();
                this.mModules = new ArrayList();
                loadModules();
                this.mAppModuleData = new AppModuleData(getModules());
                this.mAppModuleData.setCleanupEnable(false);
                try {
                    JSONObject json = new JSONObject(tmp);
                    Iterator<?> keys = json.keys();
                    while (keys.hasNext()) {
                        String tempAppName = keys.next().toString();
                        AppLogger.print(this.mClassName, tempAppName + " : ", 1);
                        AppContext localContext = new AppContext(tempAppName);
                        this.mAppData.addContext(localContext);
                        JSONObject appParent = json.getJSONObject(tempAppName);
                        Iterator<?> appKeys = appParent.keys();
                        while (appKeys.hasNext()) {
                            String tempApp = appKeys.next().toString();
                            AppLogger.print(this.mClassName, tempApp + ": ", 1);
                            JSONObject appData = appParent.getJSONObject(tempApp);
                            Iterator<?> dataKeys = appData.keys();
                            while (dataKeys.hasNext()) {
                                String tempData = dataKeys.next().toString();
                                try {
                                    Integer.parseInt(tempData);
                                    JSONObject moduleData = appData.getJSONObject(tempData);
                                    Iterator<?> moduleKeys = moduleData.keys();
                                    while (moduleKeys.hasNext()) {
                                        String tempModule = moduleKeys.next().toString();
                                        ArrayList<Integer> values = new ArrayList<>();
                                        int length = moduleData.getJSONArray(tempModule).length();
                                        for (int j = 0; j < length; j++) {
                                            values.add(Integer.valueOf(Integer.parseInt(moduleData.getJSONArray(tempModule).get(j).toString())));
                                        }
                                        for (AppModule appModule : this.mModules) {
                                            try {
                                                this.mAppModuleData.addData(localContext, values, Integer.valueOf(Integer.parseInt(tempData)));
                                            } catch (RuntimeException e) {
                                                AppLogger.print(this.mClassName, "The data for this module was not added, maybe you tried to add data for a non-existing module?", 0);
                                            }
                                        }
                                        AppLogger.print(this.mClassName, tempModule + ": " + moduleData.getJSONArray(tempModule), 1);
                                    }
                                } catch (NumberFormatException e2) {
                                    AppLogger.print(this.mClassName, tempData + ": " + appData.get(tempData), 1);
                                    if (tempData.equals("TimeUsed")) {
                                        localContext.setTimeUsage(appData.getLong(tempData));
                                    } else if (tempData.equals("LastChecked")) {
                                        localContext.setLastChecked(appData.getLong(tempData));
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e3) {
                    AppLogger.print(this.mClassName, "Error during json-parsing: " + e3, 0);
                    this.mSleeping = false;
                }
                this.mSleeping = false;
                this.mAppModuleData.setCleanupEnable(true);
                AppLogger.print(this.mClassName, "Import of data successful in (" + (System.currentTimeMillis() - time) + " ms).", 0);
                return;
            } catch (IOException e4) {
                AppLogger.print(this.mClassName, "Error during import... " + e4, 0);
                this.mSleeping = false;
                return;
            }
        }
        this.mSleeping = false;
    }

    /**
     * Schedules monitoring for the given app context. Runs all registered modules,
     * collects their data, and handles periodic data export.
     *
     * @param context the app context to monitor
     */
    public final void schedule(AppContext context) {
        if (!PreferenceManager.getDefaultSharedPreferences(this.mContext).getBoolean(mPreferenceValue, true)) {
            if (this.mJobManagerEnable) {
                disable();
            }
            return;
        }
        if (context != null) {
            if (this.mPrevSleeping && !this.mSleeping) {
                context.setLastCheckedNow();
            }
            if (!this.mSleeping) {
                if (this.mExportThreshold == 0) {
                    setExportTimeNow();
                }
                if (System.currentTimeMillis() > this.mExportThreshold) {
                    exportData();
                    setExportTimeNow();
                }
                AppLogger.print(this.mClassName, "Calling context switch for: " + context.getAppName(), 1);
                this.mAppData.addContext(context);
                for (AppModule module : this.mModules) {
                    try {
                        module.operate();
                        this.mAppModuleData.addData(context, module.getLastValue(), module);
                    } catch (RuntimeException e) {
                        AppLogger.print(this.mClassName, "Module " + module.getName() + " failed during scheduling, skipping this cycle: " + e, 0);
                    }
                }
                if (!this.mNotifcationShowed) {
                    List<AppModuleMetaData> moduleMetaData = Collections.synchronizedList(getModuleData().getAppModuleData());
                    synchronized (moduleMetaData) {
                        for (AppModuleMetaData ammd : moduleMetaData) {
                            if (ammd.getAppContext().isAboveThreshold() && !AeroActivity.genHelper.doesExist(this.mContext.getFilesDir().getAbsolutePath() + "/" + FILENAME_APPMONITOR_NOTIFY)) {
                                showNotification();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieves an app context without triggering time usage tracking or other side effects.
     *
     * @param appname the package name of the app
     * @return the app context, or null if the manager is disabled or sleeping
     */
    public final AppContext getSimpleAppContext(String appname) {
        if (this.mJobManagerEnable && !this.mSleeping) {
            return this.mAppData.getSimpleAppContext(appname);
        }
        if (this.mSleeping && !this.mPrevSleeping) {
            AppLogger.print(this.mClassName, "JobManager is disabled", 0);
        }
        return null;
    }

    /**
     * Retrieves an app context and updates its usage tracking. Creates a new context
     * if the app hasn't been seen before.
     *
     * @param appname the package name of the app
     * @return the app context, or null if the manager is disabled or sleeping
     */
    public final AppContext getAppContext(String appname) {
        if (!this.mJobManagerEnable || this.mSleeping) {
            if (this.mSleeping && !this.mPrevSleeping) {
                AppLogger.print(this.mClassName, "JobManager is disabled", 0);
            }
            return null;
        }
        if (this.mPrevSleeping && !this.mSleeping && this.mAppData.getSimpleAppContext(appname) != null) {
            this.mAppData.getSimpleAppContext(appname).setLastCheckedNow();
        }
        return this.mAppData.getAppContext(appname);
    }

    /**
     * Sets the sleep state of the job manager. When sleeping, monitoring is paused
     * (typically when the screen is off).
     *
     * @param sleepValue true to put the manager to sleep, false to wake it
     */
    public final void setSleep(boolean sleepValue) {
        if (sleepValue && !this.mSleeping) {
            AppLogger.print(this.mClassName, "JobManager is sleeping because the display is off!", 0);
        }
        this.mPrevSleeping = this.mSleeping;
        this.mSleeping = sleepValue;
    }

    /**
     * Returns whether the job manager is currently in sleep mode.
     *
     * @return true if sleeping, false otherwise
     */
    public final boolean getSleepState() {
        return this.mSleeping;
    }

    /**
     * Forces the job manager to wake up from sleep mode and resume monitoring.
     */
    public final synchronized void wakeUp() {
        if (getSleepState()) {
            AppLogger.print(this.mClassName, "Forcing a wakeup of the JobManager...", 0);
            setSleep(false);
        }
    }

    private void loadModules() {
        int counter = 0;
        this.mModules.add(new CPUFreqModule(this.mContext));
        if (Runtime.getRuntime().availableProcessors() > 1) {
            this.mModules.add(new CPUNumModule(this.mContext));
        }
        this.mModules.add(new RAMModule(this.mContext));
        if (AeroActivity.genHelper.doesExist(FilePath.CPU_TEMP_FILE)) {
            this.mModules.add(new TEMPModule(this.mContext));
        }
        String[] arr$ = FilePath.GPU_FILES_RATE;
        for (String s : arr$) {
            if (AeroActivity.genHelper.doesExist(s)) {
                counter++;
            }
        }
        if (counter > 0) {
            this.mModules.add(new GPUFreqModule(this.mContext));
        }
        AppLogger.print(this.mClassName, "Modules successfully initialized!", 0);
    }

    private AppModuleData getModuleData() {
        return this.mAppModuleData;
    }

    private void setExportTimeNow() {
        this.mExportThreshold = System.currentTimeMillis() + 60000;
    }

    /**
     * Returns the list of registered monitoring modules.
     *
     * @return the list of AppModule instances currently loaded
     */
    public final List<AppModule> getModules() {
        return this.mModules;
    }

    protected final void showNotification() {
        Intent resultIntent = new Intent(this.mContext, (Class<?>) AeroActivity.class);
        resultIntent.putExtra("NOTIFY_STRING", "APPMONITOR");
        resultIntent.setFlags(603979776);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(this.mContext, 0, resultIntent, 134217728);
        Notification.Builder builder = new Notification.Builder(this.mContext).setContentTitle(this.mContext.getText(R.string.app_name)).setContentText(this.mContext.getText(R.string.notify_app_monitor_data)).setSmallIcon(R.drawable.rocket).setContentIntent(viewPendingIntent).setAutoCancel(true);
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (Build.VERSION.SDK_INT >= 16) {
            notificationManager.notify(0, builder.build());
        } else if (Build.VERSION.SDK_INT >= 11) {
            notificationManager.notify(0, builder.getNotification());
        }
        try {
            FileOutputStream fos = this.mContext.openFileOutput(FILENAME_APPMONITOR_NOTIFY, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
        }
        this.mNotifcationShowed = true;
    }
}
