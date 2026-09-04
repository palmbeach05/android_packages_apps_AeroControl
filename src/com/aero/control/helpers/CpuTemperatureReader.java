package com.aero.control.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * Locates and reads a CPU thermal-zone temperature without requiring root.
 */
public final class CpuTemperatureReader {
    private static final File THERMAL_DIRECTORY = new File("/sys/class/thermal");
    private static final int MIN_TEMPERATURE_C = -40;
    private static final int MAX_TEMPERATURE_C = 150;

    private CpuTemperatureReader() {
    }

    /**
     * Returns the first valid CPU temperature in degrees Celsius, or {@code null}
     * when no readable CPU thermal zone contains a plausible value.
     */
    public static Integer readCelsius() {
        File[] zones = THERMAL_DIRECTORY.listFiles();
        if (zones == null) {
            return null;
        }
        Arrays.sort(zones, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                return left.getName().compareTo(right.getName());
            }
        });
        for (File zone : zones) {
            if (!zone.getName().startsWith("thermal_zone") || !zone.isDirectory()) {
                continue;
            }
            String type = readLine(new File(zone, "type"));
            if (!isCpuType(type)) {
                continue;
            }
            Integer temperature = normalize(readLine(new File(zone, "temp")));
            if (temperature != null) {
                return temperature;
            }
        }
        return null;
    }

    private static boolean isCpuType(String type) {
        if (type == null) {
            return false;
        }
        String normalized = type.trim().toLowerCase(Locale.US);
        return normalized.contains("cpu") || normalized.contains("cluster")
                || normalized.contains("little") || normalized.contains("big");
    }

    private static Integer normalize(String value) {
        if (value == null) {
            return null;
        }
        final long raw;
        try {
            raw = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        long celsius = raw;
        if (raw >= 1000L || raw <= -1000L) {
            celsius = Math.round(raw / 1000.0d);
        }
        if (celsius < MIN_TEMPERATURE_C || celsius > MAX_TEMPERATURE_C) {
            return null;
        }
        return Integer.valueOf((int) celsius);
    }

    private static String readLine(File file) {
        if (!file.isFile() || !file.canRead()) {
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return reader.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
