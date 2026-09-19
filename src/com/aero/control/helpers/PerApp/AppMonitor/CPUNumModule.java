package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.AeroLog;

/**
 * Monitoring module that tracks the number of online CPU cores.
 * Counts cores by checking their online status in sysfs.
 */
public final class CPUNumModule extends AppModule {
    private static final AeroLog LOG = AeroLog.forClass(CPUNumModule.class);
    private static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu";
    private final String mClassName;

    /**
     * Creates a CPU core count monitoring module.
     *
     * @param context the application context
     */
    public CPUNumModule(Context context) {
        super(context);
        this.mClassName = getClass().getName();
        setName(this.mClassName);
        setIdentifier(20);
        setPrefix(context.getText(R.string.pref_cpu_number));
        setSuffix(" Cores");
        setDrawable(context.getResources().getDrawable(R.drawable.appmonitor_number));
        LOG.info("CPU Num Module successfully initialized!");
    }

    @Override // com.aero.control.helpers.PerApp.AppMonitor.AppModule
    protected void operate() {
        super.operate();
        long temp = System.currentTimeMillis();
        int onlineCPUs = 0;
        int i = Runtime.getRuntime().availableProcessors();
        if (i == 1) {
            onlineCPUs = 0 + 1;
        } else {
            for (int j = 0; j < i; j++) {
                if (AeroActivity.shell.getFastInfo("/sys/devices/system/cpu/cpu" + j + "/online").equals("1")) {
                    onlineCPUs++;
                }
            }
        }
        addValues(Integer.valueOf(onlineCPUs));
        LOG.debug("CPUNumModule.operate() time: " + (System.currentTimeMillis() - temp));
    }
}
