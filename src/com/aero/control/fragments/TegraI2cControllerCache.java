package com.aero.control.fragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Caches Tegra I2C controller discovery for the lifetime of its owner. */
final class TegraI2cControllerCache {
    private static final Pattern CONTROLLER_PATTERN = Pattern.compile("tegra-i2c\\.(\\d+)");

    static final class Controller {
        final String busNumber;
        final String path;

        Controller(String busNumber, String path) {
            this.busNumber = busNumber;
            this.path = path;
        }
    }

    private List<Controller> controllers;

    boolean isInitialized() {
        return this.controllers != null;
    }

    List<Controller> getControllers() {
        return this.controllers;
    }

    List<Controller> discover(String platformDirectory, String[] platformEntries) {
        if (this.controllers != null) {
            return this.controllers;
        }

        List<Controller> discovered = new ArrayList<>();
        if (platformEntries != null) {
            for (String entry : platformEntries) {
                Matcher matcher = CONTROLLER_PATTERN.matcher(entry);
                if (matcher.matches()) {
                    discovered.add(new Controller(
                            matcher.group(1), platformDirectory + "/" + entry));
                }
            }
        }
        this.controllers = Collections.unmodifiableList(discovered);
        return this.controllers;
    }
}
