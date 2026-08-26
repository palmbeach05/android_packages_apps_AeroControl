package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;

public class AppModule {
    public static final int MODULE_CPU_FREQ_IDENTIFIER = 10;
    public static final int MODULE_CPU_NUM_IDENTIFIER = 20;
    public static final int MODULE_GPU_IDENTIFIER = 50;
    public static final int MODULE_RAM_IDENTIFIER = 30;
    public static final int MODULE_TEMP_IDENTIFIER = 40;
    private Context mContext;
    private Drawable mDrawable;
    private Integer mModuleIdentifier;
    private final String mClassName = getClass().getName();
    private String mSuffix = "";
    private String mPrefix = "";
    private String mName = this.mClassName;
    private List<Integer> mValues = new ArrayList();

    public AppModule(Context context) {
        this.mContext = context;
        AppLogger.print(this.mClassName, "App Module initialized", 0);
    }

    protected final void setIdentifier(int identifier) {
        if (this.mModuleIdentifier != null) {
            throw new ExceptionHandler(ExceptionHandler.EX_IDENTIFIER_ALREADY_DEFINED);
        }
        this.mModuleIdentifier = Integer.valueOf(identifier);
    }

    public final int getIdentifier() {
        return this.mModuleIdentifier.intValue();
    }

    protected final void setSuffix(String suffix) {
        this.mSuffix = suffix;
    }

    public final String getPrefix() {
        return this.mPrefix;
    }

    public final void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }

    public final Drawable getDrawable() {
        return this.mDrawable;
    }

    protected final void setPrefix(String prefix) {
        this.mPrefix = prefix;
    }

    protected final void setPrefix(CharSequence charSequence) {
        setPrefix(charSequence.toString());
    }

    public final String getSuffix() {
        return this.mSuffix;
    }

    protected final String getName() {
        return this.mName;
    }

    protected final void setName(String name) {
        this.mName = name;
    }

    protected final void addValues(Integer value) {
        this.mValues.add(value);
        AppLogger.print(this.mClassName, "Value added to module: " + value, 1);
    }

    protected final List<Integer> getValues() {
        return this.mValues;
    }

    protected final Integer getLastValue() {
        if (this.mValues.size() > 0) {
            return this.mValues.get(this.mValues.size() - 1);
        }
        return null;
    }

    protected final void cleanUp() {
        this.mValues.clear();
        this.mValues = new ArrayList();
    }

    protected void operate() {
        if (this.mName == null) {
            throw new ExceptionHandler("This module has no name, please enter a name!");
        }
    }
}
