package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.AeroLog;
import com.aero.control.helpers.SysfsNode;
import com.aero.control.helpers.SysfsResult;

/**
 * Monitoring module that tracks average CPU frequency across all online cores.
 * Samples the current frequency from each online CPU and computes the mean.
 */
public final class CPUFreqModule extends AppModule {
    private static final AeroLog LOG = AeroLog.forClass(CPUFreqModule.class);
    private static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu";
    private static final String SCALE_PATH_NAME = "/cpufreq/scaling_cur_freq";
    private final String mClassName;

    /**
     * Creates a CPU frequency monitoring module.
     *
     * @param context the application context
     */
    public CPUFreqModule(Context context) {
        super(context);
        this.mClassName = getClass().getName();
        setName(this.mClassName);
        setIdentifier(10);
        setPrefix(context.getText(R.string.pref_cpu_frequency));
        setSuffix(" Mhz");
        setDrawable(context.getResources().getDrawable(R.drawable.appmonitor_cpu));
        LOG.info("CPU Freq Module successfully initialized!");
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
            SysfsResult<String> frequency = AeroActivity.hardware.read(new SysfsNode(
                    "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"));
            try {
                averageFreq = Integer.parseInt(requireValue(frequency));
                onlineCPUs = 0 + 1;
            } catch (NumberFormatException e) {
                sampleValid = false;
            }
        } else {
            for (int k = 0; k < i; k++) {
                SysfsResult<Boolean> online = AeroActivity.hardware.cpu().readOnlineState(k);
                if (!online.isSuccess()) {
                    LOG.warn("Unable to read CPU " + k + " online state: " + online.getError());
                    continue;
                }
                if (online.getValue()) {
                    String complete_path = "/sys/devices/system/cpu/cpu" + k + SCALE_PATH_NAME;
                    try {
                        SysfsResult<String> frequency =
                                AeroActivity.hardware.read(new SysfsNode(complete_path));
                        averageFreq += Integer.parseInt(requireValue(frequency));
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
        LOG.debug("CPUFreqModule.operate() time: " + (System.currentTimeMillis() - temp));
    }

    private static String requireValue(SysfsResult<String> result) {
        if (!result.isSuccess()) {
            throw new NumberFormatException(result.getError());
        }
        return result.getValue();
    }
}
