package com.aero.control.helpers;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shares one worker and one batched root-shell read across hardware metric observers. */
public final class HardwareMetricsPoller {
    private static final long POLL_INTERVAL_MS = 1000L;
    private static HardwareMetricsPoller sInstance;

    public interface Listener {
        void onMetrics(Map<String, String> values);
    }

    public final class Subscription {
        private final String[] paths;
        private final Listener listener;
        private boolean active = true;

        private Subscription(String[] paths, Listener listener) {
            this.paths = paths;
            this.listener = listener;
        }

        public void cancel() {
            synchronized (HardwareMetricsPoller.this) {
                if (!this.active) {
                    return;
                }
                this.active = false;
                mSubscriptions.remove(this);
                if (mSubscriptions.isEmpty()) {
                    mPollGeneration++;
                    if (mPollScheduled) {
                        mWorker.removeCallbacks(mPollRunnable);
                        mPollScheduled = false;
                    }
                }
            }
        }
    }

    private final shellHelper mShell;
    private final HandlerThread mWorkerThread;
    private final Handler mWorker;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private final List<Subscription> mSubscriptions = new ArrayList<>();
    private boolean mPollScheduled;
    private long mPollGeneration;

    private final Runnable mPollRunnable = new Runnable() {
        @Override
        public void run() {
            pollOnce();
        }
    };

    private HardwareMetricsPoller() {
        this.mShell = shellHelper.instance();
        this.mWorkerThread = new HandlerThread("HardwareMetricsPoller");
        this.mWorkerThread.start();
        this.mWorker = new Handler(this.mWorkerThread.getLooper());
    }

    public static synchronized HardwareMetricsPoller instance() {
        if (sInstance == null) {
            sInstance = new HardwareMetricsPoller();
        }
        return sInstance;
    }

    /** Registers paths for shared periodic reads until the returned subscription is cancelled. */
    public synchronized Subscription observe(String[] paths, Listener listener) {
        if (paths == null || paths.length == 0 || listener == null) {
            throw new IllegalArgumentException("Paths and listener are required");
        }
        Subscription subscription = new Subscription(paths.clone(), listener);
        boolean wasEmpty = this.mSubscriptions.isEmpty();
        this.mSubscriptions.add(subscription);
        if (wasEmpty) {
            this.mPollGeneration++;
        }
        if (wasEmpty && !this.mPollScheduled) {
            this.mPollScheduled = true;
            this.mWorker.post(this.mPollRunnable);
        }
        return subscription;
    }

    /** Reads a one-time snapshot on the shared worker and returns it on the main looper. */
    public void readOnce(final String[] paths, final Listener listener) {
        if (paths == null || listener == null) {
            throw new IllegalArgumentException("Paths and listener are required");
        }
        final String[] requestedPaths = paths.clone();
        this.mWorker.post(new Runnable() {
            @Override
            public void run() {
                final Map<String, String> values = readPaths(requestedPaths);
                mMain.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onMetrics(values);
                    }
                });
            }
        });
    }

    /** Runs a one-off hardware command on the shared worker. */
    public void executeOnWorker(final Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Task is required");
        }
        this.mWorker.post(task);
    }

    private void pollOnce() {
        final List<Subscription> subscriptions;
        final String[] paths;
        final long pollGeneration;
        synchronized (this) {
            subscriptions = new ArrayList<>(this.mSubscriptions);
            if (subscriptions.isEmpty()) {
                this.mPollScheduled = false;
                return;
            }
            pollGeneration = this.mPollGeneration;
            Set<String> uniquePaths = new LinkedHashSet<>();
            for (Subscription subscription : subscriptions) {
                if (subscription.active) {
                    Collections.addAll(uniquePaths, subscription.paths);
                }
            }
            paths = uniquePaths.toArray(new String[uniquePaths.size()]);
        }

        final Map<String, String> values = readPaths(paths);
        this.mMain.post(new Runnable() {
            @Override
            public void run() {
                try {
                    for (Subscription subscription : subscriptions) {
                        synchronized (HardwareMetricsPoller.this) {
                            if (!subscription.active) {
                                continue;
                            }
                        }
                        subscription.listener.onMetrics(new HashMap<>(values));
                    }
                } finally {
                    synchronized (HardwareMetricsPoller.this) {
                        if (pollGeneration != mPollGeneration) {
                            return;
                        }
                        if (mSubscriptions.isEmpty()) {
                            mPollScheduled = false;
                        } else {
                            mWorker.postDelayed(mPollRunnable, POLL_INTERVAL_MS);
                        }
                    }
                }
            }
        });
    }

    private Map<String, String> readPaths(String[] paths) {
        String[] contents = this.mShell.readFiles(paths);
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < paths.length; i++) {
            values.put(paths[i], contents == null || i >= contents.length
                    ? "Unavailable" : contents[i]);
        }
        return Collections.unmodifiableMap(values);
    }
}
