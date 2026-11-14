package com.aero.control.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class ProcfsNodeTest {
    @Test
    public void numericBoundsAreInclusive() {
        ProcfsNode node = new ProcfsNode(
                "/proc/sys/kernel/random/read_wakeup_threshold", 0, 4096);

        assertTrue(node.accepts("0"));
        assertTrue(node.accepts("4096"));
        assertFalse(node.accepts("-1"));
        assertFalse(node.accepts("4097"));
    }

    @Test
    public void advertisedValuesAreMatchedExactly() {
        ProcfsNode node = new ProcfsNode(
                "/proc/sys/net/ipv4/tcp_congestion_control",
                Arrays.asList("cubic", "reno"));

        assertTrue(node.accepts("cubic"));
        assertFalse(node.accepts("cubic; reboot"));
        assertFalse(node.accepts("westwood"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSysfsPaths() {
        new ProcfsNode("/sys/devices/test/value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPathsOutsideProcfs() {
        new ProcfsNode("/data/local/value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsParentPathSegments() {
        new ProcfsNode("/proc/sys/../data/value");
    }
}
