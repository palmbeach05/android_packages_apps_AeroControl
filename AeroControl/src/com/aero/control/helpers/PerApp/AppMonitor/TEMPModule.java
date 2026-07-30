package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.AeroActivity;
import com.aero.control.R;

/* JADX INFO: loaded from: classes.dex */
public final class TEMPModule extends AppModule {
    private static final String CPU_TEMP_FILE = "/sys/devices/virtual/thermal/thermal_zone1/temp";
    private final String mClassName;

    public TEMPModule(Context context) {
        super(context);
        this.mClassName = getClass().getName();
        setName(this.mClassName);
        setIdentifier(40);
        setPrefix(context.getText(R.string.pref_temp_usage));
        setSuffix(" °C");
        setDrawable(context.getResources().getDrawable(R.drawable.appmonitor_temp));
        AppLogger.print(this.mClassName, "Temperature Module successfully initialized!", 0);
    }

    @Override // com.aero.control.helpers.PerApp.AppMonitor.AppModule
    protected void operate() {
        super.operate();
        long temp = System.currentTimeMillis();
        Integer temperature = null;
        try {
            temperature = Integer.valueOf(Integer.parseInt(AeroActivity.shell.getFastInfo("/sys/devices/virtual/thermal/thermal_zone1/temp")));
        } catch (NumberFormatException e) {
        }
        if (temperature != null) {
            addValues(temperature);
        }
        AppLogger.print(this.mClassName, "TEMPModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }
}
