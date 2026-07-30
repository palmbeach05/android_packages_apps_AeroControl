package com.aero.control.helpers.PerApp.AppMonitor;

import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AppModuleData {
    private List<AppModule> mModules;
    private final String mClassName = getClass().getName();
    private boolean mCleanUpEnabled = true;
    private List<AppModuleMetaData> mAppModuleData = new ArrayList();

    public AppModuleData(List<AppModule> modules) {
        this.mModules = modules;
        AppLogger.print(this.mClassName, "AppModuleData successfully initialized!", 0);
    }

    public final AppModuleMetaData existsAppModuleMetaData(AppContext context) {
        for (AppModuleMetaData ammd : this.mAppModuleData) {
            if (ammd.getAppContext() == context) {
                return ammd;
            }
        }
        return null;
    }

    public final List<AppModuleMetaData> getAppModuleData() {
        return this.mAppModuleData;
    }

    public final void setCleanupEnable(boolean enable) {
        this.mCleanUpEnabled = enable;
    }

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
