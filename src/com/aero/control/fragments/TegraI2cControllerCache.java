package com.aero.control.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Caches Tegra I2C controller discovery for the lifetime of its owner. */
final class TegraI2cControllerCache {
    private static final Pattern CONTROLLER_PATTERN = Pattern.compile("tegra-i2c\\.(\\d+)");
    private static final long INITIAL_RETRY_DELAY_MS = 5000L;
    private static final long MAX_RETRY_DELAY_MS = 60000L;

    static final class Controller {
        final String busNumber;
        final String path;

        /**
         * Creates metadata for a discovered Tegra I2C controller.
         *
         * @param busNumber controller bus number
         * @param path absolute controller directory path
         */
        Controller(String busNumber, String path) {
            this.busNumber = busNumber;
            this.path = path;
        }
    }

    private List<Controller> controllers;
    private long retryAtMs;
    private int failureCount;

    /**
     * Reports whether controller discovery has already completed.
     *
     * @return {@code true} after the first discovery attempt
     */
    boolean isInitialized() {
        return this.controllers != null;
    }

    /**
     * Returns the cached controller list.
     *
     * @return cached controllers, or {@code null} before discovery
     */
    List<Controller> getControllers() {
        return this.controllers;
    }

    boolean canRetry(long nowElapsedRealtimeMs) {
        if (this.controllers != null) {
            return false;
        }
        return nowElapsedRealtimeMs >= this.retryAtMs;
    }

    /**
     * Discovers Tegra I2C controllers once and caches the immutable result.
     *
     * @param platformDirectory absolute platform-device directory
     * @param platformEntries entries found in the platform-device directory
     * @return the cached immutable controller list
     */
    List<Controller> discover(String platformDirectory, String[] platformEntries) {
        return discover(platformDirectory, platformEntries, 0L);
    }

    /**
     * Discovers or retries controllers, applying backoff only to failed attempts.
     *
     * @param nowElapsedRealtimeMs monotonic timestamp used to schedule the next retry
     * @return the cached controller list, or an empty list for a failed attempt
     */
    List<Controller> discover(String platformDirectory, String[] platformEntries,
            long nowElapsedRealtimeMs) {
        if (this.controllers != null) {
            return this.controllers;
        }

        if (platformEntries == null) {
            this.failureCount++;
            long multiplier = 1L << Math.min(this.failureCount - 1, 4);
            long delay = INITIAL_RETRY_DELAY_MS * multiplier;
            if (delay > MAX_RETRY_DELAY_MS) {
                delay = MAX_RETRY_DELAY_MS;
            }
            this.retryAtMs = nowElapsedRealtimeMs + delay;
            return Collections.emptyList();
        }

        List<Controller> discovered = new ArrayList<>();
        for (String entry : platformEntries) {
            Matcher matcher = CONTROLLER_PATTERN.matcher(entry);
            if (matcher.matches()) {
                discovered.add(new Controller(
                        matcher.group(1), platformDirectory + "/" + entry));
            }
        }
        this.controllers = Collections.unmodifiableList(discovered);
        this.failureCount = 0;
        this.retryAtMs = 0L;
        return this.controllers;
    }
}
