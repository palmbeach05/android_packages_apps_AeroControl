package com.aero.control.helpers;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    private boolean checkRootMethod() {
        String output = suCheckRootMethod();
        if (output.equals(NO_DATA_FOUND)) {
            return false;
        }
        return output.contains("uid=0");
    }

    private String suCheckRootMethod() {
        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("id\n");
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
