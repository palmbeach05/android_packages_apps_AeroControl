package com.aero.control.helpers;

import java.math.BigInteger;

/** Immutable Overview memory values, expressed in whole megabytes. */
public final class MemorySnapshot {
    private final long totalMb;
    private final long availableMb;
    private final long usedMb;
    private final long usedPercent;
    private final boolean available;

    private MemorySnapshot(long totalMb, long availableMb, long usedMb, long usedPercent,
            boolean available) {
        this.totalMb = totalMb;
        this.availableMb = availableMb;
        this.usedMb = usedMb;
        this.usedPercent = usedPercent;
        this.available = available;
    }

    public static MemorySnapshot unavailable() {
        return new MemorySnapshot(0L, 0L, 0L, 0L, false);
    }

    static MemorySnapshot fromKilobytes(long totalKb, long availableKb) {
        if (totalKb <= 0L) {
            return unavailable();
        }
        long boundedAvailableKb = Math.max(0L, Math.min(availableKb, totalKb));
        long usedKb = totalKb - boundedAvailableKb;
        long usedPercent = BigInteger.valueOf(usedKb)
            .multiply(BigInteger.valueOf(100L))
            .divide(BigInteger.valueOf(totalKb))
            .longValue();
        return new MemorySnapshot(totalKb / 1024L, boundedAvailableKb / 1024L,
                usedKb / 1024L, usedPercent, true);
    }

    public boolean isAvailable() {
        return available;
    }

    public long getTotalMb() {
        return totalMb;
    }

    public long getAvailableMb() {
        return availableMb;
    }

    public long getUsedMb() {
        return usedMb;
    }

    public long getUsedPercent() {
        return usedPercent;
    }
}
