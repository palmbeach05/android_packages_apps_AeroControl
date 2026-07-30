package com.aero.control.testsuite;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.TextView;
import com.aero.control.R;

/* JADX INFO: loaded from: classes.dex */
public class TestSuiteFragment extends PreferenceFragment {
    private ActionBar mActionBar;
    private double mMFlops = 0.0d;
    private int mProgress;
    private double mStartTime;
    private double mTargetTime;
    private PreferenceScreen root;
    private static final int mNumProcessors = Runtime.getRuntime().availableProcessors();
    private static final String LOG_TAG = PreferenceFragment.class.getName();

    @Override // android.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        addPreferencesFromResource(R.layout.testsuite_fragment);
        this.root = getPreferenceScreen();
        if (Build.VERSION.SDK_INT >= 21) {
            this.mActionBar = getActivity().getActionBar();
            this.mActionBar.setTitle(getText(R.string.slider_testsuite_settings));
        } else {
            TextView mActionBarTitle = (TextView) getActivity().findViewById(getResources().getIdentifier("action_bar_title", "id", "android"));
            mActionBarTitle.setText(R.string.slider_testsuite_settings);
        }
        loadSettings();
    }

    public void loadSettings() {
        PreferenceCategory TestSuiteCat = (PreferenceCategory) findPreference("testsuite_settings");
        Preference lpPreference = this.root.findPreference("linpack_test");
        lpPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.aero.control.testsuite.TestSuiteFragment.1
            @Override // android.preference.Preference.OnPreferenceClickListener
            public boolean onPreferenceClick(Preference preference) {
                new RunBenchmark().execute(new Void[0]);
                return false;
            }
        });
        TestSuiteCat.addPreference(lpPreference);
    }

    public final void setUpBenchmark(int numThreads) {
        this.mMFlops = 0.0d;
        Runnable[] runWorker = new Runnable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final Linpack lp = new Linpack();
            lp.resetBenchmark();
            warmUp(lp);
            lp.resetBenchmark();
            runWorker[i] = new Runnable() { // from class: com.aero.control.testsuite.TestSuiteFragment.2
                @Override // java.lang.Runnable
                public void run() {
                    synchronized (this) {
                        TestSuiteFragment.this.runTest(lp);
                    }
                }
            };
        }
        this.mStartTime = System.currentTimeMillis();
        this.mTargetTime = this.mStartTime + 5000.0d;
        for (int j = 0; j < numThreads; j++) {
            Thread mWorker = new Thread(runWorker[j]);
            mWorker.start();
            Log.e(LOG_TAG, "Running now!");
        }
    }

    public final void warmUp(Linpack lp) {
        for (int i = 0; i < 50; i++) {
            lp.run_benchmark();
        }
    }

    public final void runTest(Linpack lp) {
        if (System.currentTimeMillis() < this.mTargetTime) {
            lp.run_benchmark();
            runTest(lp);
        } else {
            Log.e(LOG_TAG, "Stopped the test");
            gatherResults(lp);
        }
    }

    public final void gatherResults(Linpack lp) {
        this.mMFlops += lp.getMFlops();
        this.mProgress++;
        Log.e(LOG_TAG, "Average MFLop-Counter: " + this.mMFlops + " Time Passed:" + lp.getTimePassed());
    }

    private class RunBenchmark extends AsyncTask<Void, Integer, Void> {
        ProgressDialog progressDialog;

        private RunBenchmark() {
        }

        @Override // android.os.AsyncTask
        protected void onPreExecute() {
            super.onPreExecute();
            this.progressDialog = ProgressDialog.show(TestSuiteFragment.this.getActivity(), "Running Linpack", "Burning your CPUs...", false);
            this.progressDialog.setIndeterminateDrawable(TestSuiteFragment.this.getResources().getDrawable(R.drawable.spinner_animation));
            TestSuiteFragment.this.mProgress = 0;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... params) {
            while (TestSuiteFragment.this.mProgress < TestSuiteFragment.mNumProcessors) {
                publishProgress(Integer.valueOf(TestSuiteFragment.this.mProgress));
                TestSuiteFragment.this.setUpBenchmark(TestSuiteFragment.mNumProcessors);
                while (TestSuiteFragment.this.mProgress < TestSuiteFragment.mNumProcessors) {
                }
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Void result) {
            super.onPostExecute(result);
            AlertDialog.Builder builder = new AlertDialog.Builder(TestSuiteFragment.this.getActivity());
            builder.setTitle("Result");
            builder.setMessage("Great! \nYou have achieved; \n" + TestSuiteFragment.this.mMFlops + " MFlops");
            builder.show();
            TestSuiteFragment.this.mMFlops = 0.0d;
            this.progressDialog.dismiss();
        }
    }
}
