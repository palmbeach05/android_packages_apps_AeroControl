package com.aero.control.helpers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** A validated sysfs node and optional inclusive numeric bounds. */
public final class SysfsNode {
    private final String path;
    private final Integer minimum;
    private final Integer maximum;
    private final Set<String> allowedValues;

    public SysfsNode(String path) {
        this(path, null, null, null);
    }

    public SysfsNode(String path, Integer minimum, Integer maximum) {
        this(path, minimum, maximum, null);
    }

    /** Creates a node whose writes must match one of the advertised values. */
    public SysfsNode(String path, Collection<String> allowedValues) {
        this(path, null, null, allowedValues);
    }

    private SysfsNode(String path, Integer minimum, Integer maximum,
            Collection<String> allowedValues) {
        if (path == null || !path.startsWith("/sys/") || containsParentSegment(path)) {
            throw new IllegalArgumentException("Sysfs nodes must be under /sys");
        }
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
        if (allowedValues != null && allowedValues.isEmpty()) {
            throw new IllegalArgumentException("allowedValues must not be empty");
        }
        this.path = path;
        this.minimum = minimum;
        this.maximum = maximum;
        this.allowedValues = allowedValues == null ? null
                : Collections.unmodifiableSet(new HashSet<>(allowedValues));
    }

    public String getPath() {
        return path;
    }

    boolean accepts(String value) {
        if (this.allowedValues != null) {
            return this.allowedValues.contains(value.trim());
        }
        try {
            int numericValue = Integer.parseInt(value.trim());
            return (minimum == null || numericValue >= minimum)
                    && (maximum == null || numericValue <= maximum);
        } catch (NumberFormatException e) {
            return minimum == null && maximum == null;
        }
    }

    private static boolean containsParentSegment(String path) {
        for (String segment : path.split("/")) {
            if ("..".equals(segment)) return true;
        }
        return false;
    }
}
