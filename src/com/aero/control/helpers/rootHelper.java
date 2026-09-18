package com.aero.control.helpers;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for detecting whether the device has root access by attempting
 * to execute the 'su' command and checking for uid=0 in the output.
 */
public class rootHelper {
    private static final int BUFF_LEN = 1024;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = rootHelper.class.getName();
    private static final long ROOT_CHECK_TIMEOUT_SECONDS = 10;

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
        final Process[] processHolder = new Process[1];
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> rootCheck = executor.submit(new Callable<String>() {
            @Override
            public String call() {
                return runRootCheck(processHolder);
            }
        });

        try {
            return rootCheck.get(ROOT_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Log.w(LOG_TAG, "Root check timed out");
            return NO_DATA_FOUND;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NO_DATA_FOUND;
        } catch (ExecutionException e) {
            Log.e(LOG_TAG, "Root check failed", e.getCause());
            return NO_DATA_FOUND;
        } finally {
            rootCheck.cancel(true);
            synchronized (processHolder) {
                if (processHolder[0] != null) {
                    processHolder[0].destroy();
                }
            }
            executor.shutdownNow();
        }
    }

    private String runRootCheck(Process[] processHolder) {
        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        try {
            process = new ProcessBuilder("su").redirectErrorStream(true).start();
            synchronized (processHolder) {
                processHolder[0] = process;
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("id\nexit\n");
            os.flush();
            os.close();
            os = null;

            is = process.getInputStream();
            byte[] buffer = new byte[BUFF_LEN];
            StringBuilder result = new StringBuilder();
            int read;
            while ((read = is.read(buffer)) != -1) {
                result.append(new String(buffer, 0, read));
            }
            process.waitFor();
            return process.exitValue() == 0 ? result.toString() : NO_DATA_FOUND;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to run root check", e);
            return NO_DATA_FOUND;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NO_DATA_FOUND;
        } finally {
            close(os);
            close(is);
            if (process != null) {
                process.destroy();
            }
            synchronized (processHolder) {
                processHolder[0] = null;
            }
        }
    }

    private void close(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Unable to close root-check stream", e);
            }
        }
    }
}
