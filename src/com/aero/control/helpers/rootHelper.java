package com.aero.control.helpers;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class rootHelper {
    private static final int BUFF_LEN = 1024;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = rootHelper.class.getName();
    private static final byte[] buffer = new byte[1024];

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
        try {
            process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("id\n");
            InputStream is = process.getInputStream();
            String result = "";
            while (true) {
                int read = is.read(buffer);
                if (read == -1) {
                    result = NO_DATA_FOUND;
                    break;
                }
                result = result + new String(buffer, 0, read);
                if (read < BUFF_LEN) {
                    os.writeBytes("exit\n");
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Do you even root, bro? :/", e);
            return NO_DATA_FOUND;
        } finally {
            if (process != null) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                process.destroy();
            }
        }
    }
}
