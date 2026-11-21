package com.aero.control.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class SysfsNodeTest {
    @Test
    public void numericBoundsAreInclusive() {
        SysfsNode node = new SysfsNode("/sys/devices/test/value", 10, 20);

        assertTrue(node.accepts("10"));
        assertTrue(node.accepts("20"));
        assertFalse(node.accepts("9"));
        assertFalse(node.accepts("21"));
        assertFalse(node.accepts("invalid"));
    }

    @Test
    public void advertisedValuesAreMatchedExactly() {
        SysfsNode node = new SysfsNode("/sys/devices/test/governor",
                Arrays.asList("powersave", "performance"));

        assertTrue(node.accepts("powersave"));
        assertFalse(node.accepts("power save"));
        assertFalse(node.accepts("userspace"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathsOutsideSysfs() {
        new SysfsNode("/data/local/value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsProcfsPaths() {
        new SysfsNode("/proc/sys/kernel/random/read_wakeup_threshold");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentPathSegments() {
        new SysfsNode("/sys/devices/../proc/value");
    }
}
