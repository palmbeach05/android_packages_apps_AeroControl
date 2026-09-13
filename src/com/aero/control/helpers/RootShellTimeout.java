package com.aero.control.helpers;

import android.util.Log;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Shared helper for bounding blocking reads from an {@code su} process/shell.
 *
 * <p>{@code Runtime.getRuntime().exec("su")} followed by a synchronous read on the
 * process' stdout can block forever if the device's superuser manager shows an
 * interactive grant prompt that the user never answers. This helper runs the
 * blocking read on a dedicated daemon thread and bounds the wait with
 * {@link Future#get(long, TimeUnit)}, so a caller on the main thread can never be
 * stuck longer than {@link #DEFAULT_TIMEOUT_MS}. On timeout the supplied
 * {@code Process} (if any) is destroyed so the abandoned read thread's underlying
 * stream is unblocked/closed rather than leaked.
 */
final class RootShellTimeout {
    static final long DEFAULT_TIMEOUT_MS = 15000L;
    private static final String LOG_TAG = RootShellTimeout.class.getName();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override // java.util.concurrent.ThreadFactory
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "root-shell-io");
            t.setDaemon(true);
            return t;
        }
    });

    private RootShellTimeout() {
    }

    /**
     * Runs {@code task} on a background thread and waits at most {@code timeoutMs}
     * for it to complete. If the deadline is reached, {@code processToDestroy} (if
     * non-null) is destroyed to unblock/abort the underlying read, and
     * {@code onTimeout} is returned instead.
     *
     * @param task the blocking operation to run with a bounded wait
     * @param processToDestroy the process to destroy if the wait times out, or null
     * @param onTimeout the value to return if the wait times out or is interrupted
     * @param timeoutMs the maximum time to wait, in milliseconds
     * @param <T> the result type
     * @return the result of {@code task}, or {@code onTimeout} if it didn't finish in time
     */
    static <T> T runBounded(Callable<T> task, Process processToDestroy, T onTimeout, long timeoutMs) {
        Future<T> future = EXECUTOR.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Log.e(LOG_TAG, "su root shell operation timed out after " + timeoutMs + "ms; aborting.", e);
            future.cancel(true);
            if (processToDestroy != null) {
                processToDestroy.destroy();
            }
            return onTimeout;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            if (processToDestroy != null) {
                processToDestroy.destroy();
            }
            return onTimeout;
        } catch (ExecutionException e) {
            Log.e(LOG_TAG, "su root shell operation failed.", e);
            return onTimeout;
        }
    }
}
