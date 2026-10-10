package com.aero.control.helpers.PerApp.AppMonitor;

import com.aero.control.helpers.AeroLog;

import java.util.ArrayList;

/**
 * Container for all monitored application data. Manages the list of AppContext objects
 * representing each monitored app and provides methods to add, retrieve, and clear data.
 */
public final class AppData {
    private static final AeroLog LOG = AeroLog.forClass(AppData.class);
    private AppMetaData mMetaData = new AppMetaData();

    /**
     * Creates an AppData container.
     */
    public AppData() {
        LOG.info("App Data has been initialized!");
    }

    /**
     * Adds an app context to the monitored app list.
     *
     * @param context the app context to add
     */
    public final void addContext(AppContext context) {
        LOG.debug("Trying to add the following app: " + context.getAppName());
        this.mMetaData.addToList(context);
    }

    /**
     * Retrieves the app context for a given app name.
     *
     * @param appname the app package name
     * @return the AppContext for that app, or null if not found
     */
    public final AppContext getAppContext(String appname) {
        return this.mMetaData.getAppContext(appname);
    }

    /**
     * Retrieves a simple app context for a given app name without full metadata.
     *
     * @param appname the app package name
     * @return the AppContext for that app, or null if not found
     */
    public final AppContext getSimpleAppContext(String appname) {
        return this.mMetaData.getSimpleAppContext(appname);
    }

    /**
     * Returns the list of all monitored app contexts.
     *
     * @return the list of AppContext objects
     */
    public final ArrayList<AppContext> getAppList() {
        return this.mMetaData.mAppList;
    }

    /**
     * Clears all monitored app data by replacing the metadata container with a fresh instance.
     */
    public final void clearData() {
        this.mMetaData = new AppMetaData();
    }

    private final class AppMetaData {
        private ArrayList<AppContext> mAppList = new ArrayList<>();

        public AppMetaData() {
        }

        private boolean existsContext(AppContext context) {
            int i = 0;
            if (context == null) {
                return false;
            }
            for (AppContext ac : this.mAppList) {
                if (ac.getAppName() != null && ac.getAppName().equals(context.getAppName())) {
                    i++;
                }
            }
            if (i > 0) {
                return true;
            }
            return false;
        }

        private Integer getAppPosition(String appname) {
            int i = 0;
            for (AppContext ac : this.mAppList) {
                if (ac.getAppName().equals(appname)) {
                    return Integer.valueOf(i);
                }
                i++;
            }
            return null;
        }

        public final AppContext getSimpleAppContext(String appname) {
            try {
                AppContext localContext = this.mAppList.get(getAppPosition(appname).intValue());
                return localContext;
            } catch (NullPointerException e) {
                LOG.debug("We found no match for: " + appname + " we don't know this app yet");
                return null;
            }
        }

        public final AppContext getAppContext(String appname) {
            AppContext localContext;
            try {
                localContext = this.mAppList.get(getAppPosition(appname).intValue());
            } catch (NullPointerException e) {
                LOG.debug("We found no match for: " + appname + " we will add it");
                localContext = new AppContext(appname);
                this.mAppList.add(localContext);
            }
            localContext.increaseTimeUsage(System.currentTimeMillis() - localContext.getLastChecked());
            for (AppContext ac : this.mAppList) {
                ac.setLastCheckedNow();
            }
            LOG.debug("App (" + appname + ") is running for: " + localContext.getTimeUsage() + " ms");
            return localContext;
        }

        public final void addToList(AppContext context) {
            if (existsContext(context)) {
                LOG.debug("App: " + context.getAppName() + " already added.");
                return;
            }
            if (context != null) {
                this.mAppList.add(context);
            }
            LOG.debug("App: " + context.getAppName() + " successfully added!");
        }
    }
}
