package com.aero.control.helpers;

import android.util.Log;

/**
 * Formats raw kHz/Hz frequency strings read from sysfs (e.g. {@code
 * scaling_cur_freq}) into the "N MHz" form shown throughout the CPU/GPU
 * settings screens. Pure string/number formatting, no I/O.
 */
final class FrequencyFormat {
    static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = FrequencyFormat.class.getName();

    private FrequencyFormat() {
    }

    /**
     * Converts a frequency string from kHz or Hz to MHz with proper formatting.
     *
     * @param mhzString the frequency string to convert
     * @return the frequency in MHz with " MHz" suffix, or "Unavailable" if conversion fails
     */
    static String toMHz(String mhzString) {
        String str;
        if (mhzString.equals(NO_DATA_FOUND) || mhzString.equals("Unavaila")) {
            return NO_DATA_FOUND;
        }
        try {
            if (mhzString.length() < 8) {
                str = (Integer.valueOf(mhzString).intValue() / 1000) + " MHz";
            } else {
                str = (Integer.valueOf(mhzString).intValue() / 1000000) + " MHz";
            }
            return str;
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Tried to add something to a non existing string.", e);
            return NO_DATA_FOUND;
        }
    }
}
