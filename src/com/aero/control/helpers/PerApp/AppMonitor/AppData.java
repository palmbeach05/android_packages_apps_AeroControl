package com.aero.control.helpers.PerApp.AppMonitor;

import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public final class AppData {
    private final String mClassName = getClass().getName();
    private AppMetaData mMetaData = new AppMetaData();

    public AppData() {
        AppLogger.print(this.mClassName, "App Data has been initialized!", 0);
    }

    public final void addContext(AppContext context) {
        AppLogger.print(this.mClassName, "Trying to add the following app: " + context.getAppName(), 1);
        this.mMetaData.addToList(context);
    }

    public final AppContext getAppContext(String appname) {
        return this.mMetaData.getAppContext(appname);
    }

    public final AppContext getSimpleAppContext(String appname) {
        return this.mMetaData.getSimpleAppContext(appname);
    }

    public final ArrayList<AppContext> getAppList() {
        return this.mMetaData.mAppList;
    }

    public final void clearData() {
        this.mMetaData = new AppMetaData();
    }

    private final class AppMetaData {
        private final String mClassName = getClass().getName();
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
            AppLogger.print(this.mClassName, "Couldn't add the following app, bailing out: " + context.getAppName(), 1);
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
                AppLogger.print(this.mClassName, "We found no match for: " + appname + " we don't know this app yet", 1);
                return null;
            }
        }

        public final AppContext getAppContext(String appname) {
            AppContext localContext;
            try {
                localContext = this.mAppList.get(getAppPosition(appname).intValue());
            } catch (NullPointerException e) {
                AppLogger.print(this.mClassName, "We found no match for: " + appname + " we will add it", 1);
                localContext = new AppContext(appname);
                this.mAppList.add(localContext);
            }
            localContext.increaseTimeUsage(System.currentTimeMillis() - localContext.getLastChecked());
            for (AppContext ac : this.mAppList) {
                ac.setLastCheckedNow();
            }
            AppLogger.print(this.mClassName, "App (" + appname + ") is running for: " + localContext.getTimeUsage() + " ms", 1);
            return localContext;
        }

        public final void addToList(AppContext context) {
            if (existsContext(context)) {
                AppLogger.print(this.mClassName, "App: " + context.getAppName() + " already added.", 1);
                return;
            }
            if (context != null) {
                this.mAppList.add(context);
            }
            AppLogger.print(this.mClassName, "App: " + context.getAppName() + " successfully added!", 1);
        }
    }
}
