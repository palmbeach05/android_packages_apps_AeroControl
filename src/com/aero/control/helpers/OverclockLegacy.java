package com.aero.control.helpers;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Legacy, device-family-specific overclock support: patches kernel function
 * addresses (read from {@code /proc/kallsyms}) into the {@code
 * /proc/overclock/*} procfs nodes exposed by one specific device family's
 * kernel. This has nothing to do with the generic root-shell session and is
 * only still here for backwards compatibility with that hardware.
 */
final class OverclockLegacy {
    private static final int BUFF_LEN = 8192;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = OverclockLegacy.class.getName();

    private final RootShellSession session;

    OverclockLegacy(RootShellSession session) {
        this.session = session;
    }

    /**
     * Legacy method that spawns a new root process for each command. Used for
     * operations that cannot use the persistent shell.
     *
     * @param command the command to execute
     * @param parameter the parameter to pass to the command
     * @return the command output, or "Unavailable" if the command fails
     */
    String getLegacyRootInfo(String command, String parameter) {
        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + " " + parameter + "\n");
            os.flush();
            is = process.getInputStream();
            byte[] localBuffer = new byte[BUFF_LEN];
            String result = "";
            while (true) {
                int read = is.read(localBuffer);
                if (read == -1) {
                    result = NO_DATA_FOUND;
                    break;
                }
                result = result + new String(localBuffer, 0, read);
                if (read < BUFF_LEN) {
                    os.writeBytes("exit\n");
                    os.flush();
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Do you even root, bro? :/", e);
            return NO_DATA_FOUND;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // Nothing useful to do with a failure while tearing down.
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Nothing useful to do with a failure while tearing down.
                }
            }
            if (process != null) {
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Sets kernel overclock addresses by reading kallsyms and writing to procfs.
     * Device-specific feature for legacy overclock support.
     *
     * @return true if addresses were set successfully, false otherwise
     */
    boolean setOverclockAddress() {
        if (!new File("/proc/overclock/omap2_clk_init_cpufreq_table_addr").exists() || !new File("/proc/overclock/cpufreq_stats_update_addr").exists()) {
            return false;
        }
        String omap_result = getLegacyRootInfo("busybox egrep \"omap2_clk_init_cpufreq_table$\"", "/proc/kallsyms");
        String cpufreq_result = getLegacyRootInfo("busybox egrep \"cpufreq_stats_update$\"", "/proc/kallsyms");
        if (omap_result.length() < 8 || cpufreq_result.length() < 8) {
            return false;
        }
        String omap_address = omap_result.substring(0, 8);
        String cpufreq_address = cpufreq_result.substring(0, 8);
        String[] commands = {"echo 0x" + omap_address + " > /proc/overclock/omap2_clk_init_cpufreq_table_addr", "echo 0x" + cpufreq_address + " > /proc/overclock/cpufreq_stats_update_addr"};
        session.addCommands(commands);
        session.runCommands();
        return true;
    }
}
