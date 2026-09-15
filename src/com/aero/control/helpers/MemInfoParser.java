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
        if (availableKb != null && availableKb >= 0L) {
            return MemorySnapshot.fromKilobytes(totalKb, availableKb);
        }
        Long fallbackAvailableKb = sumFallbackAvailable(fields);
        if (fallbackAvailableKb == null) {
            return MemorySnapshot.unavailable();
        }
        return MemorySnapshot.fromKilobytes(totalKb, fallbackAvailableKb);
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

    private static Long sumFallbackAvailable(Map<String, Long> fields) {
        Long memFree = fields.get("MemFree");
        Long buffers = fields.get("Buffers");
        Long cached = fields.get("Cached");
        Long sReclaimable = fields.get("SReclaimable");
        if (memFree == null || buffers == null || cached == null || sReclaimable == null) {
            return null;
        }

        long available = memFree + buffers + cached + sReclaimable;
        Long shmem = fields.get("Shmem");
        if (shmem != null) {
            if (shmem < 0L || available < shmem) {
                return null;
            }
            available -= shmem;
        }
        return available < 0L ? 0L : available;
    }
}
