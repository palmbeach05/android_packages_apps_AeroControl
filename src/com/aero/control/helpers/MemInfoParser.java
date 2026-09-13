package com.aero.control.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Parses named fields from /proc/meminfo for the Overview memory card. */
public final class MemInfoParser {
    private static final String KILOBYTES = "kB";

    private MemInfoParser() {
    }

    public static MemorySnapshot parse(BufferedReader reader) {
        if (reader == null) {
            return MemorySnapshot.unavailable();
        }

        Map<String, Long> fields = new HashMap<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line, fields);
            }
        } catch (IOException e) {
            return MemorySnapshot.unavailable();
        }

        Long totalKb = fields.get("MemTotal");
        if (totalKb == null || totalKb <= 0L) {
            return MemorySnapshot.unavailable();
        }

        Long availableKb = fields.get("MemAvailable");
        if (availableKb == null || availableKb < 0L) {
            availableKb = sumFallbackAvailable(fields);
        }
        return MemorySnapshot.fromKilobytes(totalKb, availableKb);
    }

    private static void parseLine(String line, Map<String, Long> fields) {
        if (line == null) {
            return;
        }
        int separator = line.indexOf(':');
        if (separator <= 0) {
            return;
        }
        String name = line.substring(0, separator).trim();
        String[] parts = line.substring(separator + 1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].length() == 0) {
            return;
        }
        if (parts.length > 1 && !KILOBYTES.equals(parts[1])) {
            return;
        }
        try {
            long value = Long.parseLong(parts[0]);
            if (value >= 0L) {
                fields.put(name, value);
            }
        } catch (NumberFormatException e) {
            // Ignore malformed fields and let the required-field check decide availability.
        }
    }

    private static long sumFallbackAvailable(Map<String, Long> fields) {
        long available = safeAdd(fields.get("MemFree"), fields.get("Buffers"));
        available = safeAdd(available, fields.get("Cached"));
        available = safeAdd(available, fields.get("SReclaimable"));
        Long shmem = fields.get("Shmem");
        if (shmem != null) {
            available = available < shmem ? Long.MIN_VALUE : available - shmem;
        }
        return available;
    }

    private static long safeAdd(long left, Long right) {
        if (right == null) {
            return left;
        }
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
