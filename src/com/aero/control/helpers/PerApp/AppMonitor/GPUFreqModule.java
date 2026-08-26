package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.FilePath;

public final class GPUFreqModule extends AppModule {
    private final String mClassName;
    private String mGPUFile;

    public GPUFreqModule(Context context) {
        super(context);
        this.mClassName = getClass().getName();
        setName(this.mClassName);
        setIdentifier(50);
        setPrefix(context.getText(R.string.pref_gpu_frequency));
        setSuffix(" Mhz");
        setDrawable(context.getResources().getDrawable(R.drawable.appmonitor_gpu));
        String[] arr$ = FilePath.GPU_FILES_RATE;
        for (String s : arr$) {
            if (AeroActivity.genHelper.doesExist(s)) {
                this.mGPUFile = s;
            }
        }
        AppLogger.print(this.mClassName, "GPU Frequency Module successfully initialized!", 0);
    }

    private Integer getFormatInt(String s) {
        return s.length() < 8 ? Integer.valueOf(Integer.valueOf(s).intValue() / 1000) : Integer.valueOf(Integer.valueOf(s).intValue() / 1000000);
    }

    @Override // com.aero.control.helpers.PerApp.AppMonitor.AppModule
    protected void operate() {
        super.operate();
        long temp = System.currentTimeMillis();
        Integer gpufreq = null;
        try {
            gpufreq = getFormatInt(AeroActivity.shell.getFastInfo(this.mGPUFile));
        } catch (NumberFormatException e) {
        }
        if (gpufreq != null) {
            addValues(gpufreq);
        }
        AppLogger.print(this.mClassName, "GOUFreqModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }
}
