package com.aero.control.helpers.PerApp.AppMonitor;

import com.aero.control.helpers.AeroLog;

/** Compatibility adapter for existing App Monitor logging calls. */
public final class AppLogger {
    private AppLogger() {
    }

    public static void print(String tag, String message, int level) {
        if (!Configuration.APPLOGGER_ENABLED || level > Configuration.LOG_LEVEL) {
            return;
        }
        AeroLog logger = AeroLog.forTag(tag);
        if (level <= -1) {
            logger.error(message);
        } else if (level == 0) {
            logger.warn(message);
        } else if (level == 1) {
            logger.info(message);
        } else {
            logger.debug(message);
        }
    }

    public static int getLogLevel() {
        return Configuration.LOG_LEVEL;
    }
}
