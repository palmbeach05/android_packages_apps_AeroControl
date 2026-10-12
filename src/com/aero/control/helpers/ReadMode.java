package com.aero.control.helpers;

/** Describes how a space-separated sysfs value list should be decoded. */
public enum ReadMode {
    RAW(false, false),
    BRACKETED_RAW(false, true),
    FREQUENCY_MHZ(true, false),
    BRACKETED_FREQUENCY_MHZ(true, true);

    private final boolean convertToMhz;
    private final boolean stripBrackets;

    ReadMode(boolean convertToMhz, boolean stripBrackets) {
        this.convertToMhz = convertToMhz;
        this.stripBrackets = stripBrackets;
    }

    boolean convertsToMhz() {
        return convertToMhz;
    }

    boolean stripsBrackets() {
        return stripBrackets;
    }
}
