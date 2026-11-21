package com.aero.control.helpers;

/** Validated access to the device properties used by the Defy parts controls. */
public final class LedController {
    public enum Property {
        CHARGING_MODE("persist.sys.charge_led"),
        MULTI_TOUCH_POINTS("persist.sys.multitouch"),
        BUTTON_BRIGHTNESS("persist.sys.button_brightness");

        private final String key;

        Property(String key) {
            this.key = key;
        }
    }

    private final RootShellSession session;

    LedController(RootShellSession session) {
        this.session = session;
    }

    /** Reads one of the supported Defy properties through the shared root session. */
    public SysfsResult<String> read(Property property) {
        if (property == null) {
            return SysfsResult.failure("Property is required");
        }
        String result = session.execute("getprop "
                + RootShellSession.escapeShellArg(property.key));
        if (result == null || result.trim().length() == 0) {
            return SysfsResult.failure("Unable to read " + property.key);
        }
        return SysfsResult.success(result.trim());
    }

    /** Sets and verifies a supported property, rejecting values outside its capability set. */
    public SysfsResult<String> write(Property property, String value) {
        if (property == null || !accepts(property, value)) {
            return SysfsResult.failure("Property or value is not supported");
        }
        String key = RootShellSession.escapeShellArg(property.key);
        String command = "setprop " + key + " "
                + RootShellSession.escapeShellArg(value.trim()) + " && getprop " + key;
        String readback = session.execute(command);
        if (readback == null || !value.trim().equals(readback.trim())) {
            return SysfsResult.failure("Write verification failed for " + property.key);
        }
        return SysfsResult.success(readback.trim());
    }

    private boolean accepts(Property property, String value) {
        if (value == null || value.trim().length() == 0
                || !value.equals(value.trim()) || !value.matches("[A-Za-z0-9_.:-]{1,64}")) {
            return false;
        }
        switch (property) {
            case CHARGING_MODE:
                return "white".equals(value) || "green".equals(value)
                        || "rgb".equals(value) || "off".equals(value);
            case MULTI_TOUCH_POINTS:
                return "2".equals(value) || "3".equals(value) || "4".equals(value)
                        || "5".equals(value) || "8".equals(value) || "10".equals(value);
            case BUTTON_BRIGHTNESS:
                return value.matches("[0-9]+(?:\\.[0-9]+)?");
            default:
                return false;
        }
    }
}
