package com.aero.control.helpers.PerApp.AppMonitor;

import android.content.Context;
import com.aero.control.R;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public final class RAMModule extends AppModule {
    private static final String mPROC_MEMINFO = "/proc/meminfo";
    private final String mClassName;

    public RAMModule(Context context) {
        super(context);
        this.mClassName = getClass().getName();
        setName(this.mClassName);
        setIdentifier(30);
        setPrefix(context.getText(R.string.pref_ram_usage));
        setSuffix(" MB");
        setDrawable(context.getResources().getDrawable(R.drawable.appmonitor_ram));
        AppLogger.print(this.mClassName, "RAM Module successfully initialized!", 0);
    }

    @Override // com.aero.control.helpers.PerApp.AppMonitor.AppModule
    protected void operate() {
        super.operate();
        long temp = System.currentTimeMillis();
        String totalFreeMemory = "0";
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"), 1024);
            String totalMemory = reader.readLine();
            totalFreeMemory = reader.readLine();
            if (totalFreeMemory != null) {
                totalMemory.split("\\s+");
                String[] parts = totalFreeMemory.split("\\s+");
                if (parts.length == 3) {
                    totalFreeMemory = Long.parseLong(parts[1]) + "";
                }
            }
        } catch (IOException e) {
        } catch (NumberFormatException e) {
        }
        try {
            Integer freeRAM = Integer.valueOf(Integer.parseInt(totalFreeMemory));
            addValues(Integer.valueOf(freeRAM.intValue() / 1000));
        } catch (NumberFormatException e) {
        }
        AppLogger.print(this.mClassName, "RAMModule.operate() time: " + (System.currentTimeMillis() - temp), 1);
    }
}
