package com.aero.control.helpers;

import com.aero.control.helpers.legacy.LegacyOverclockController;

/** Narrow typed hardware API exposed to UI code instead of raw shell access. */
public interface HardwareAccessService {
    SysfsResult<String> read(SysfsNode node);

    SysfsResult<String[]> readValues(SysfsNode node, ReadMode mode);

    SysfsResult<String[]> readLines(SysfsNode node);

    String[] readMetrics(String[] paths);

    SysfsResult<String> write(SysfsNode node, String value);

    ProcfsRepository procfs();

    SysfsResult<String[]> listSysfsDirectory(
            SysfsDirectory directory, DirectoryEntryType entryType);

    HwmonInfo hwmon();

    TegraI2cInfo tegraI2c();

    CpuController cpu();

    GpuController gpu();

    LedController led();

    DisplayColorController displayColor();

    IoSchedulerController ioScheduler();

    NetworkController network();

    DynamicTunableController dynamicTunables();

    LegacyOverclockController legacyOverclock();
}
