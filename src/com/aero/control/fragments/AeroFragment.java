package com.aero.control.fragments;

import android.app.Fragment;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.AeroAdapter;
import com.aero.control.adapter.AeroData;
import com.aero.control.helpers.FilePath;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import fr.nicolaspomepuy.discreetapprate.AppRate;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AeroFragment extends Fragment {
    private static final String FILENAME = "firstrun";
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String SCALE_CPU_UTIL = "/cpufreq/cpu_utilization";
    private static final String SCALE_CUR_FILE = "/sys/devices/system/cpu/cpu";
    private static final String SCALE_PATH_NAME = "/cpufreq/scaling_cur_freq";
    private String gpu_file;
    private AeroAdapter mAdapter;
    private AeroData mFrequencyData;
    private AeroData mGPUData;
    private AeroData mGovernorData;
    private AeroData mIOSchedulerData;
    private AeroData mKernelData;
    private ListView mOverView;
    private AeroData mRAMData;
    private ShowcaseView mShowCase;
    private ViewGroup root;
    private List<AeroData> mOverviewData = new ArrayList();
    private int mActionBarHeight = 0;
    private boolean mVisible = true;
    private boolean mExecuted = false;
    private RefreshThread mRefreshThread = new RefreshThread();
    private Handler mRefreshHandler = new Handler() { // from class: com.aero.control.fragments.AeroFragment.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what >= 1 && AeroFragment.this.isVisible() && AeroFragment.this.mVisible) {
                AeroFragment.this.createList();
                AeroFragment.this.mVisible = true;
            }
        }
    };

    private class RefreshThread extends Thread {
        private boolean mInterrupt;

        private RefreshThread() {
            this.mInterrupt = false;
        }

        @Override // java.lang.Thread
        public void interrupt() {
            this.mInterrupt = true;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (!this.mInterrupt) {
                try {
                    sleep(1000L);
                    AeroFragment.this.mRefreshHandler.sendEmptyMessage(1);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    @Override // android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mVisible = false;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mVisible = true;
        this.mAdapter = null;
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.root = (ViewGroup) inflater.inflate(R.layout.overviewlist_item, (ViewGroup) null);
        this.mOverView = (ListView) this.root.findViewById(R.id.listView1);
        String[] arr$ = FilePath.GPU_FILES_RATE;
        int len$ = arr$.length;
        int i$ = 0;
        while (true) {
            if (i$ >= len$) {
                break;
            }
            String a = arr$[i$];
            if (!AeroActivity.genHelper.doesExist(a)) {
                i$++;
            } else {
                this.gpu_file = a;
                break;
            }
        }
        if (!this.mRefreshThread.isAlive()) {
            this.mRefreshThread.start();
            this.mRefreshThread.setPriority(1);
        }
        createList();
        if (!this.mExecuted) {
            setPermissions();
        }
        AppRate.with(getActivity()).text(R.string.rateIt).fromTop(false).delay(2000).autoHide(10000).allowPlayLink(true).checkAndShow();
        return this.root;
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_aero_fragment, R.string.showcase_aero_fragment_sum);
        }
    }

    public final String getFreqPerCore() {
        String freq_string = "";
        String cpu_util = "";
        int i = Runtime.getRuntime().availableProcessors();
        for (int k = 0; k < i; k++) {
            String complete_path = "/sys/devices/system/cpu/cpu" + k + SCALE_PATH_NAME;
            freq_string = freq_string + " " + AeroActivity.shell.toMHz(AeroActivity.shell.getInfo(complete_path));
        }
        String freq_string2 = freq_string.replace(NO_DATA_FOUND, " Offline ");
        if (!AeroActivity.genHelper.doesExist("/sys/devices/system/cpu/cpu0/cpufreq/cpu_utilization")) {
            return freq_string2;
        }
        for (int j = 0; j < i; j++) {
            String complete_path2 = "/sys/devices/system/cpu/cpu" + j + SCALE_CPU_UTIL;
            String tmp = AeroActivity.shell.getInfo(complete_path2);
            if (!tmp.equals(NO_DATA_FOUND)) {
                try {
                    if (Integer.parseInt(tmp.trim()) < 10) {
                        tmp = " " + tmp;
                    }
                } catch (NumberFormatException e) {
                    tmp = "0";
                }
            }
            cpu_util = cpu_util + "\t\t\t" + tmp + "%";
        }
        return freq_string2 + "\n" + cpu_util.replace("Unavailable%", "--");
    }

    private String getCPUTemp() {
        if (!AeroActivity.genHelper.doesExist(FilePath.CPU_TEMP_FILE)) {
            return null;
        }
        String tmp = AeroActivity.shell.getInfo(FilePath.CPU_TEMP_FILE);
        if (tmp.length() > 2) {
            tmp = tmp.substring(0, 2);
        }
        return tmp + " °C";
    }

    private void fillData(String gpu_freq) {
        if (this.mKernelData == null) {
            this.mKernelData = new AeroData(getString(R.string.kernel_version), AeroActivity.shell.getKernel(), null);
        } else {
            this.mKernelData.content = AeroActivity.shell.getKernel();
        }
        if (this.mGovernorData == null) {
            this.mGovernorData = new AeroData(getString(R.string.current_governor), AeroActivity.shell.getInfo(FilePath.GOV_FILE), null);
        } else {
            this.mGovernorData.content = AeroActivity.shell.getInfo(FilePath.GOV_FILE);
        }
        if (this.mIOSchedulerData == null) {
            this.mIOSchedulerData = new AeroData(getString(R.string.current_io_governor), AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE), null);
        } else {
            this.mIOSchedulerData.content = AeroActivity.shell.getInfo(FilePath.GOV_IO_FILE);
        }
        if (this.mFrequencyData == null) {
            this.mFrequencyData = new AeroData(getString(R.string.current_cpu_speed), getFreqPerCore(), getCPUTemp());
        } else {
            this.mFrequencyData.content = getFreqPerCore();
            this.mFrequencyData.right_name = getCPUTemp();
        }
        if (this.mGPUData == null) {
            this.mGPUData = new AeroData(getString(R.string.current_gpu_speed), AeroActivity.shell.toMHz(gpu_freq.substring(0, gpu_freq.length() - 3)), null);
        } else {
            this.mGPUData.content = AeroActivity.shell.toMHz(gpu_freq.substring(0, gpu_freq.length() - 3));
        }
        if (this.mRAMData == null) {
            this.mRAMData = new AeroData(getString(R.string.available_memory), AeroActivity.shell.getMemory(FilePath.FILENAME_PROC_MEMINFO), null);
        } else {
            this.mRAMData.content = AeroActivity.shell.getMemory(FilePath.FILENAME_PROC_MEMINFO);
        }
    }

    public void createList() {
        if (this.mOverviewData != null) {
            this.mOverviewData.clear();
        }
        if (this.mAdapter != null) {
            this.mAdapter.clear();
            this.mAdapter.notifyDataSetChanged();
        }
        String gpu_freq = AeroActivity.shell.getInfo(this.gpu_file);
        if (gpu_freq.length() <= 3) {
            gpu_freq = NO_DATA_FOUND;
        }
        fillData(gpu_freq);
        this.mOverviewData.add(this.mKernelData);
        this.mOverviewData.add(this.mGovernorData);
        this.mOverviewData.add(this.mIOSchedulerData);
        this.mOverviewData.add(this.mFrequencyData);
        this.mOverviewData.add(this.mGPUData);
        this.mOverviewData.add(this.mRAMData);
        if (this.mAdapter == null) {
            this.mAdapter = new AeroAdapter(getActivity(), R.layout.overviewlist_item, this.mOverviewData);
            this.mOverView.setAdapter((ListAdapter) this.mAdapter);
        } else {
            getActivity().runOnUiThread(new Runnable() { // from class: com.aero.control.fragments.AeroFragment.2
                @Override // java.lang.Runnable
                public void run() {
                    AeroFragment.this.mAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void DrawFirstStart(int header, int content) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(FILENAME, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            this.mActionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.AeroFragment.3
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                return new Point(100, AeroFragment.this.mActionBarHeight);
            }
        };
        this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

    public void setPermissions() {
        String[] commands = {"chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq", "chmod 0664 /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq"};
        AeroActivity.shell.setRootInfo(commands);
        this.mExecuted = true;
    }
}
