package com.aero.control.helpers;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

/**
 * Utility class for detecting whether the device has root access by attempting
 * to execute the 'su' command and checking for uid=0 in the output.
 */
public class rootHelper {
    private static final int BUFF_LEN = 1024;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = rootHelper.class.getName();

    /**
     * Checks whether the device has root access available.
     *
     * @return true if root access is available, false otherwise
     */
    public boolean isDeviceRooted() {
        return checkRootMethod();
    }

    /**
     * Checks whether the device has root access by executing the 'id' command via 'su'
     * and checking for uid=0 in the output.
     *
     * @return true if root access is available, false otherwise
     */
    private boolean checkRootMethod() {
        String output = suCheckRootMethod();
        if (output.equals(NO_DATA_FOUND)) {
            return false;
        }
        return output.contains("uid=0");
    }

    /**
     * Executes the 'id' command via 'su' and returns the output for root detection.
     *
     * @return the output of the 'id' command or "Unavailable" if failed
     */
    private String suCheckRootMethod() {
        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        try {
            process = Runtime.getRuntime().exec("su");
            final Process suProcess = process;
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("id\n");
            os.flush();
            is = process.getInputStream();
            final InputStream suInput = is;
            final DataOutputStream suOutput = os;
            // Bound the read: if the su prompt is never answered, this would
            // otherwise block the calling thread (possibly the UI thread)
            // forever. Run it on a background thread and give up after a
            // fixed timeout, destroying the process to unblock the read.
            return RootShellTimeout.runBounded(new Callable<String>() {
                @Override // java.util.concurrent.Callable
                public String call() throws IOException {
                    byte[] localBuffer = new byte[BUFF_LEN];
                    String result = "";
                    while (true) {
                        int read = suInput.read(localBuffer);
                        if (read == -1) {
                            result = NO_DATA_FOUND;
                            break;
                        }
                        result = result + new String(localBuffer, 0, read);
                        if (read < BUFF_LEN) {
                            suOutput.writeBytes("exit\n");
                            suOutput.flush();
                            break;
                        }
                    }
                    return result;
                }
            }, suProcess, NO_DATA_FOUND, RootShellTimeout.DEFAULT_TIMEOUT_MS);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Do you even root, bro? :/", e);
            return NO_DATA_FOUND;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
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
}
