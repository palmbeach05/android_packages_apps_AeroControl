package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.R;
import com.aero.control.helpers.CpuTemperatureReader;

/**
 * Monitoring module that tracks CPU temperature from the thermal zone sensor.
 * Reads the temperature from sysfs and reports it in degrees Celsius.
 */
public final class TEMPModule extends AppModule {
    private final String mClassName;

    /**
     * Creates a temperature monitoring module.
     *
     * @param context the application context
     */
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
        Integer temperature = CpuTemperatureReader.readCelsius();
        if (temperature != null) {
            addValues(temperature);
        }
        AppLogger.print(this.mClassName, "TEMPModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }
}
