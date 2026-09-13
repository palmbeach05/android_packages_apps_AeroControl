package com.aero.control.helpers;

/** A validated sysfs node and optional inclusive numeric bounds. */
public final class SysfsNode {
    private final String path;
    private final Integer minimum;
    private final Integer maximum;

    public SysfsNode(String path) {
        this(path, null, null);
    }

    public SysfsNode(String path, Integer minimum, Integer maximum) {
        if (path == null || path.trim().length() == 0 || !path.startsWith("/")) {
            throw new IllegalArgumentException("Sysfs paths must be absolute");
        }
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new IllegalArgumentException("minimum must not exceed maximum");
        }
        this.path = path;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public String getPath() {
        return path;
    }

    boolean accepts(String value) {
        try {
            int numericValue = Integer.parseInt(value.trim());
            return (minimum == null || numericValue >= minimum)
                    && (maximum == null || numericValue <= maximum);
        } catch (NumberFormatException e) {
            return minimum == null && maximum == null;
        }
    }
}
