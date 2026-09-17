package com.aero.control.fragments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class TegraI2cControllerCacheTest {
    private static final String PLATFORM_DIRECTORY = "/sys/devices/platform";

    /** Verifies that an empty Tegra discovery result is cached. */
    @Test
    public void unreadablePlatformDirectoryIsNotCached() {
        TegraI2cControllerCache cache = new TegraI2cControllerCache();

        assertTrue(cache.discover(PLATFORM_DIRECTORY, null).isEmpty());
        assertFalse(cache.isInitialized());

        List<TegraI2cControllerCache.Controller> controllers = cache.discover(
                PLATFORM_DIRECTORY, new String[] {"tegra-i2c.4"});

        assertTrue(cache.isInitialized());
        assertEquals("4", controllers.get(0).busNumber);
    }

    @Test
    public void nonTegraPlatformIsCachedAsUnavailable() {
        TegraI2cControllerCache cache = new TegraI2cControllerCache();

        assertTrue(cache.discover(PLATFORM_DIRECTORY,
                new String[] {"serial8250", "alarmtimer"}).isEmpty());
        assertTrue(cache.isInitialized());
        assertTrue(cache.discover(PLATFORM_DIRECTORY,
                new String[] {"tegra-i2c.4"}).isEmpty());
    }

    /** Verifies that valid controller entries retain their bus numbers and paths. */
    @Test
    public void validControllersAreDiscoveredWithBusAndPath() {
        TegraI2cControllerCache cache = new TegraI2cControllerCache();

        List<TegraI2cControllerCache.Controller> controllers = cache.discover(
                PLATFORM_DIRECTORY,
                new String[] {"serial8250", "tegra-i2c.1", "tegra-i2c.10", "tegra-i2c.x"});

        assertEquals(2, controllers.size());
        assertEquals("1", controllers.get(0).busNumber);
        assertEquals("/sys/devices/platform/tegra-i2c.1", controllers.get(0).path);
        assertEquals("10", controllers.get(1).busNumber);
        assertEquals("/sys/devices/platform/tegra-i2c.10", controllers.get(1).path);
    }

    /** Verifies that repeated discovery returns the original cached controllers. */
    @Test
    public void repeatedDiscoveryReusesSupportedControllerResult() {
        TegraI2cControllerCache cache = new TegraI2cControllerCache();
        List<TegraI2cControllerCache.Controller> first = cache.discover(
                PLATFORM_DIRECTORY, new String[] {"tegra-i2c.4"});

        List<TegraI2cControllerCache.Controller> second = cache.discover(
                PLATFORM_DIRECTORY, new String[] {"tegra-i2c.9"});

        assertTrue(first == second);
        assertEquals("4", second.get(0).busNumber);
    }
}
