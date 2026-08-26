package com.aero.control.helpers.PerApp.AppMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages monitoring data collection across multiple apps and modules.
 * Stores per-app metadata and coordinates automatic cleanup when thresholds are reached.
 */
public class AppModuleData {
    private List<AppModule> mModules;
    private final String mClassName = getClass().getName();
    private boolean mCleanUpEnabled = true;
    private List<AppModuleMetaData> mAppModuleData = new ArrayList();

    /**
     * Creates an app module data manager with the specified monitoring modules.
     *
     * @param modules the list of monitoring modules to track
     */
    public AppModuleData(List<AppModule> modules) {
        this.mModules = modules;
        AppLogger.print(this.mClassName, "AppModuleData successfully initialized!", 0);
    }

    /**
     * Checks if metadata exists for the specified app context.
     *
     * @param context the app context to look up
     * @return the existing metadata, or null if not found
     */
    public final AppModuleMetaData existsAppModuleMetaData(AppContext context) {
        for (AppModuleMetaData ammd : this.mAppModuleData) {
            if (ammd.getAppContext() == context) {
                return ammd;
            }
        }
        return null;
    }

    /**
     * Returns the list of all app module metadata.
     *
     * @return the list of metadata for all monitored apps
     */
    public final List<AppModuleMetaData> getAppModuleData() {
        return this.mAppModuleData;
    }

    /**
     * Enables or disables automatic cleanup of monitoring data.
     *
     * @param enable true to enable cleanup, false to disable
     */
    public final void setCleanupEnable(boolean enable) {
        this.mCleanUpEnabled = enable;
    }

    /**
     * Adds multiple data values for a specific module identifier.
     *
     * @param context the app context
     * @param values the list of values to add
     * @param identifier the module identifier
     * @throws ExceptionHandler if the module identifier is not found
     */
    public void addData(AppContext context, ArrayList<Integer> values, Integer identifier) {
        AppModule targetModule = null;
        for (AppModule module : this.mModules) {
            if (module.getIdentifier() == identifier.intValue()) {
                targetModule = module;
            }
        }
        if (targetModule != null) {
            for (Integer i : values) {
                addData(context, i, targetModule);
            }
            return;
        }
        throw new ExceptionHandler(ExceptionHandler.EX_MODULE_NOT_FOUND + " (" + identifier + ")");
    }

    /**
     * Adds a single data value for a specific app and module, creating metadata if needed.
     *
     * @param context the app context
     * @param value the value to add
     * @param module the monitoring module
     */
    public void addData(AppContext context, Integer value, AppModule module) {
        if (value != null) {
            AppModuleMetaData appmetadata = existsAppModuleMetaData(context);
            if (appmetadata == null) {
                appmetadata = new AppModuleMetaData(context, this.mModules);
                AppLogger.print(this.mClassName, "Adding a new meta data module for: " + context.getAppName(), 0);
                this.mAppModuleData.add(appmetadata);
            }
            checkForCleanup(appmetadata, context);
            appmetadata.addMetaData(value, module);
        }
    }

    private void checkForCleanup(AppModuleMetaData appmetadata, AppContext context) {
        if (this.mCleanUpEnabled && appmetadata.readForCleanUp()) {
            AppLogger.print(this.mClassName, "Cleaning up the found data for: " + context.getAppName(), 0);
            appmetadata.cleanUp();
            context.cleanUp();
        }
    }
}
