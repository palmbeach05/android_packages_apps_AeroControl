package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.AeroActivity;
import com.aero.control.R;

public final class CPUFreqModule extends AppModule {
    private static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu";
    private static final String SCALE_PATH_NAME = "/cpufreq/scaling_cur_freq";
    private final String mClassName;

    public CPUFreqModule(Context context) {
        super(context);
        this.mClassName = getClass().getName();
        setName(this.mClassName);
        setIdentifier(10);
        setPrefix(context.getText(R.string.pref_cpu_frequency));
        setSuffix(" Mhz");
        setDrawable(context.getResources().getDrawable(R.drawable.appmonitor_cpu));
        AppLogger.print(this.mClassName, "CPU Freq Module successfully initialized!", 0);
    }

    @Override // com.aero.control.helpers.PerApp.AppMonitor.AppModule
    protected final void operate() {
        super.operate();
        long temp = System.currentTimeMillis();
        int averageFreq = 0;
        int onlineCPUs = 0;
        int i = Runtime.getRuntime().availableProcessors();
        boolean sampleValid = true;
        if (i == 1) {
            try {
                averageFreq = 0 + Integer.parseInt(AeroActivity.shell.getFastInfo("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"));
                onlineCPUs = 0 + 1;
            } catch (NumberFormatException e) {
                sampleValid = false;
            }
        } else {
            for (int k = 0; k < i; k++) {
                if (AeroActivity.shell.getFastInfo("/sys/devices/system/cpu/cpu" + k + "/online").equals("1")) {
                    String complete_path = "/sys/devices/system/cpu/cpu" + k + SCALE_PATH_NAME;
                    try {
                        averageFreq += Integer.parseInt(AeroActivity.shell.getFastInfo(complete_path));
                    } catch (NumberFormatException e) {
                    }
                    onlineCPUs++;
                }
            }
        }
        if (sampleValid) {
            if (onlineCPUs == 0) {
                onlineCPUs = 1;
            }
            addValues(Integer.valueOf((averageFreq / onlineCPUs) / 1000));
        }
        AppLogger.print(this.mClassName, "CPUFreqModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }
}
