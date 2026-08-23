package com.aero.control.testsuite;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.TextView;
import com.aero.control.R;
import com.aero.control.helpers.ThemeHelper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
public class TestSuiteFragment extends PreferenceFragment {
    private static final int WARMUP_RUNS = 5;
    private static final long BENCHMARK_DURATION_MS = 5000L;
    private static final String PREF_USE_ALL_CPUS = "linpack_use_all_cpus";
    private static final String LOG_TAG = TestSuiteFragment.class.getName();

    private ActionBar mActionBar;
    private PreferenceScreen root;
    private RunBenchmark mRunBenchmark;
    private final AtomicBoolean mBenchmarkRunning = new AtomicBoolean(false);

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.layout.testsuite_fragment);
        this.root = getPreferenceScreen();
        if (Build.VERSION.SDK_INT >= 21) {
            this.mActionBar = getActivity().getActionBar();
            this.mActionBar.setTitle(getText(R.string.slider_test_suite_settings));
        } else {
            TextView mActionBarTitle = (TextView) getActivity().findViewById(getResources().getIdentifier("action_bar_title", "id", "android"));
            mActionBarTitle.setText(R.string.slider_test_suite_settings);
        }
        loadSettings();
    }

    public void loadSettings() {
        Preference lpPreference = this.root.findPreference("linpack_test");
        lpPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.testsuite.TestSuiteFragment.1
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                startBenchmark();
                return false;
            }
        });
    }

    private void startBenchmark() {
        if (!this.mBenchmarkRunning.compareAndSet(false, true)) {
            Log.e(LOG_TAG, "Benchmark already running, ignoring request");
            return;
        }
        boolean useAllCpus = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREF_USE_ALL_CPUS, false);
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int workerCount = useAllCpus ? Math.max(1, availableProcessors) : Math.max(1, availableProcessors - 1);
        this.mRunBenchmark = new RunBenchmark(workerCount, useAllCpus, availableProcessors);
        this.mRunBenchmark.execute(new Void[0]);
    }

    private int getLinpackDialogTheme(boolean alertDialog) {
        String theme = ThemeHelper.getTheme(getActivity());
        if (ThemeHelper.THEME_LIGHT.equals(theme)) {
            return alertDialog
                    ? R.style.AeroDialog_Light_Alert : R.style.AeroDialog_Light;
        }
        if (ThemeHelper.THEME_DARK.equals(theme)) {
            return alertDialog
                    ? R.style.AeroDialog_Dark_Alert : R.style.AeroDialog_Dark;
        }
        return 0;
    }

    @Override // android.app.Fragment
    public void onDestroyView() {
        if (this.mRunBenchmark != null) {
            this.mRunBenchmark.cancel(true);
            this.mRunBenchmark = null;
        }
        super.onDestroyView();
    }

    private static double runWorker(Linpack lp, long targetElapsedRealtimeMs, AsyncTask<?, ?, ?> task) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            lp.run_benchmark();
        }
        lp.resetBenchmark();
        while (SystemClock.elapsedRealtime() < targetElapsedRealtimeMs && !task.isCancelled()) {
            lp.run_benchmark();
        }
        return lp.getMFlops();
    }

    private class RunBenchmark extends AsyncTask<Void, Void, Double> {
        AlertDialog progressDialog;
        private final int workerCount;
        private final boolean useAllCpus;
        private final int availableProcessors;

        private RunBenchmark(int workerCount, boolean useAllCpus, int availableProcessors) {
            this.workerCount = workerCount;
            this.useAllCpus = useAllCpus;
            this.availableProcessors = availableProcessors;
        }

        private String getModeText() {
            String mode = this.useAllCpus ? "All CPUs" : "Responsive";
            if (this.availableProcessors == 1) {
                mode += " (single CPU)";
            }
            return mode;
        }

        @Override // android.os.AsyncTask
        protected void onPreExecute() {
            super.onPreExecute();
            int dialogTheme = TestSuiteFragment.this.getLinpackDialogTheme(true);
            AlertDialog.Builder builder = dialogTheme == 0
                    ? new AlertDialog.Builder(TestSuiteFragment.this.getActivity())
                    : new AlertDialog.Builder(TestSuiteFragment.this.getActivity(), dialogTheme);
            this.progressDialog = builder
                    .setTitle(R.string.testsuite_linpack_running)
                    .setView(R.layout.linpack_progress_dialog)
                    .setCancelable(false)
                    .create();
            this.progressDialog.show();
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Double doInBackground(Void... params) {
            final double[] results = new double[this.workerCount];
            final CountDownLatch latch = new CountDownLatch(this.workerCount);
            final long targetElapsedRealtimeMs = SystemClock.elapsedRealtime() + BENCHMARK_DURATION_MS;
            final Thread[] workers = new Thread[this.workerCount];
            for (int i = 0; i < this.workerCount; i++) {
                final int slot = i;
                Thread mWorker = new Thread(new Runnable() { // from class: com.aero.control.testsuite.TestSuiteFragment.2
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            results[slot] = runWorker(new Linpack(), targetElapsedRealtimeMs, RunBenchmark.this);
                        } finally {
                            latch.countDown();
                        }
                    }
                });
                workers[i] = mWorker;
                mWorker.start();
            }
            Log.e(LOG_TAG, "Running now!");
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                for (Thread worker : workers) {
                    worker.interrupt();
                }
                for (Thread worker : workers) {
                    try {
                        worker.join();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                return null;
            }
            double mflops = 0.0d;
            for (double result : results) {
                mflops += result;
            }
            String modeText = getModeText();
            Log.i(LOG_TAG, "Stopped the test. Mode: " + modeText + ", CPUs: " + this.workerCount + ", Total MFlop sum: " + mflops);
            return Double.valueOf(mflops);
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Double result) {
            super.onPostExecute(result);
            TestSuiteFragment.this.mBenchmarkRunning.set(false);
            if (this.progressDialog != null && this.progressDialog.isShowing()) {
                this.progressDialog.dismiss();
            }
            if (!TestSuiteFragment.this.isAdded() || TestSuiteFragment.this.getActivity() == null) {
                return;
            }
            int dialogTheme = TestSuiteFragment.this.getLinpackDialogTheme(true);
            AlertDialog.Builder builder = dialogTheme == 0
                    ? new AlertDialog.Builder(TestSuiteFragment.this.getActivity())
                    : new AlertDialog.Builder(TestSuiteFragment.this.getActivity(), dialogTheme);
            builder.setTitle("Result");
            builder.setMessage("Great! \nYou have achieved: \n" + result + " MFlops\n\nMode: " + getModeText() + "\nCPUs: " + this.workerCount);
            builder.show();
        }

        @Override // android.os.AsyncTask
        protected void onCancelled() {
            super.onCancelled();
            TestSuiteFragment.this.mBenchmarkRunning.set(false);
            if (this.progressDialog != null && this.progressDialog.isShowing()) {
                this.progressDialog.dismiss();
            }
        }
    }
}
