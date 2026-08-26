package com.aero.control.helpers.PerApp.AppMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata container for a single app's monitoring data across all modules.
 * Stores timestamped values for each module and tracks cleanup thresholds.
 */
public class AppModuleMetaData {
    private AppContext mAppContext;
    private List<AppModule> mAppModules;
    private final String mClassName = getClass().getName();
    private int mUsage = 0;
    private int mModuleUsage = 0;
    private List<List<Integer>> mModules = new ArrayList();

    /**
     * Creates metadata for an app context with the specified monitoring modules.
     *
     * @param context the app context
     * @param modules the list of monitoring modules
     */
    public AppModuleMetaData(AppContext context, List<AppModule> modules) {
        this.mAppContext = context;
        this.mAppModules = modules;
        for (AppModule appModule : modules) {
            this.mModules.add(new ArrayList());
        }
    }

    /**
     * Returns the app context associated with this metadata.
     *
     * @return the app context
     */
    public final AppContext getAppContext() {
        return this.mAppContext;
    }

    /**
     * Checks if the metadata has reached the cleanup threshold.
     *
     * @return true if cleanup should be triggered, false otherwise
     */
    public final boolean readForCleanUp() {
        return this.mUsage >= 5760;
    }

    /**
     * Returns the current usage count (number of complete data collection cycles).
     *
     * @return the usage count
     */
    public final int getUsage() {
        return this.mUsage;
    }

    /**
     * Clears all collected data and resets usage counters.
     */
    public final void cleanUp() {
        this.mUsage = 0;
        int i = 0;
        for (List<Integer> moduleData : this.mModules) {
            moduleData.clear();
            this.mModules.get(i).clear();
            i++;
        }
        for (AppModule module : this.mAppModules) {
            module.cleanUp();
        }
    }

    /**
     * Retrieves the raw data values for a specific module identifier.
     *
     * @param identifier the module identifier
     * @return the list of raw data values, or null if not found
     */
    public final List<Integer> getRawData(int identifier) {
        int n = 0;
        List<Integer> rawData = null;
        for (AppModule modules : this.mAppModules) {
            if (modules.getIdentifier() == identifier) {
                List<Integer> rawData2 = this.mModules.get(n);
                rawData = rawData2;
            }
            n++;
        }
        return rawData;
    }

    /**
     * Adds a metadata value for a specific module and updates usage counters.
     *
     * @param value the value to add
     * @param module the module to add the value to
     */
    public final void addMetaData(Integer value, AppModule module) {
        int i = 0;
        if (value != null) {
            if (this.mModuleUsage >= this.mAppModules.size()) {
                this.mModuleUsage = 0;
                this.mUsage++;
            } else {
                this.mModuleUsage++;
            }
            for (AppModule modules : this.mAppModules) {
                if (module == modules) {
                    this.mModules.get(i).add(value);
                }
                i++;
            }
        }
    }

    /**
     * Calculates the average value for a specific module identifier.
     *
     * @param identifier the module identifier
     * @return the average value, or 0 if no data exists
     */
    public final int getAverage(int identifier) {
        int k = 0;
        List<Integer> tmp = null;
        for (AppModule modules : this.mAppModules) {
            if (modules.getIdentifier() == identifier) {
                List<Integer> tmp2 = this.mModules.get(k);
                tmp = tmp2;
            }
            k++;
        }
        int average = 0;
        if (tmp == null) {
            throw new ExceptionHandler(ExceptionHandler.EX_NO_IDENTIFIER_FOUND);
        }
        for (Integer n : tmp) {
            average += n.intValue();
        }
        return average / Math.max(tmp.size(), 1);
    }
}
