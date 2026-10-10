package com.aero.control.helpers.PerApp.AppMonitor;

/**
 * Configuration constants for the App Monitor module, defining thresholds and
 * settings for data collection, cleanup, export, and logging behavior.
 */
public class Configuration {
    /** Whether to enable App Monitor logging output. */
    public static final boolean APPLOGGER_ENABLED = true;

    /** Version string for the App Monitor module. */
    public static final String APPMONITOR_VERSION = "1.0.0";

    /** Data points threshold before triggering automatic cleanup. */
    public static final int CLEANUP_THRESHOLD = 5760;

    /** Filename for emergency data backup if normal export fails. */
    public static final String EMERGENCY_FILE = "APPMonitorData.json";

    /** Milliseconds between automatic data exports. */
    public static final int EXPORT_THRESHOLD = 60000;

    /** Log level for filtering messages (-1 for error level). */
    public static final int LOG_LEVEL = -1;

    /** Whether to use threaded imports for performance. */
    public static final boolean THREADED_IMPORT = true;

    /** Milliseconds of app usage time to trigger a monitoring event. */
    public static final int TIME_THRESHOLD = 60000;
}
