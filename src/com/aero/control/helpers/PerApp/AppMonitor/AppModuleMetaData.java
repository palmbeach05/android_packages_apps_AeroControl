package com.aero.control.helpers.PerApp.AppMonitor;

import java.util.ArrayList;
import java.util.List;

public class AppModuleMetaData {
    private AppContext mAppContext;
    private List<AppModule> mAppModules;
    private final String mClassName = getClass().getName();
    private int mUsage = 0;
    private int mModuleUsage = 0;
    private List<List<Integer>> mModules = new ArrayList();

    public AppModuleMetaData(AppContext context, List<AppModule> modules) {
        this.mAppContext = context;
        this.mAppModules = modules;
        for (AppModule appModule : modules) {
            this.mModules.add(new ArrayList());
        }
    }

    public final AppContext getAppContext() {
        return this.mAppContext;
    }

    public final boolean readForCleanUp() {
        return this.mUsage >= 5760;
    }

    public final int getUsage() {
        return this.mUsage;
    }

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
