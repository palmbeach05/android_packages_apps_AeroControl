package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.StatisticAdapter;
import com.aero.control.adapter.statisticInit;
import com.aero.control.helpers.FilePath;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class StatisticsFragment extends Fragment {
    public static final String FILENAME_STATISTICS = "firstrun_statistics";
    private static final String NO_DATA_FOUND = "Unavailable";
    public ArrayList<Long> cpuResetTime;
    public String[] data;
    public ShowcaseView mShowCase;
    public PieGraph pg;
    public ViewGroup root;
    public ListView statisticView;
    public TextView txtFreq;
    public TextView txtPercentage;
    public TextView txtTime;
    public int mIndex = 0;
    private int mColorIndex = 0;
    private double mCompleteTime = 0.0d;
    public ArrayList<Long> cpuTime = new ArrayList<>();
    public ArrayList<Long> cpuOverallTime = new ArrayList<>();
    public ArrayList<Long> cpuFreq = new ArrayList<>();
    public ArrayList<Long> cpuPercentage = new ArrayList<>();
    public statisticInit[] mResult = new statisticInit[0];

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        this.root = (ViewGroup) inflater.inflate(R.layout.statistics, (ViewGroup) null);
        clearUI();
        loadResetState();
        loadUI(true);
        return this.root;
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.statistic_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override // android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reload /* 2131099752 */:
                showResetDialog();
                break;
            case R.id.action_refresh /* 2131099753 */:
                clearUI();
                loadUI(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME_STATISTICS)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_statistics_fragment, R.string.showcase_statistics_fragment_summary);
        }
    }

    public void DrawFirstStart(int header, int content) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(FILENAME_STATISTICS, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.StatisticsFragment.1
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                int actionBarSize = 96;
                try {
                    int height = StatisticsFragment.this.getActivity().findViewById(R.id.action_refresh).getHeight();
                    if (height > 0) {
                        actionBarSize = height;
                    }
                } catch (NullPointerException e) {
                }
                int x = StatisticsFragment.this.getResources().getDisplayMetrics().widthPixels - (actionBarSize / 2);
                int y = actionBarSize / 2;
                return new Point(x, y);
            }
        };
        this.mShowCase = new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

    private void showResetDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View layout = inflater.inflate(R.layout.about_screen, (ViewGroup) null);
        TextView aboutText = (TextView) layout.findViewById(R.id.aboutScreen);
        builder.setTitle(R.string.proceed_with_reset);
        builder.setIcon(R.drawable.warning);
        aboutText.setText(R.string.delete_statistics);
        builder.setView(layout).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.StatisticsFragment.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int id) {
                StatisticsFragment.this.resetStatistics();
            }
        }).setNegativeButton(R.string.maybe_later, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.StatisticsFragment.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetStatistics() {
        Long[] time = (Long[]) this.cpuOverallTime.toArray(new Long[0]);
        if (this.cpuResetTime != null) {
            this.cpuResetTime = null;
        }
        this.cpuResetTime = new ArrayList<>();
        for (Long t : time) {
            this.cpuResetTime.add(t);
        }
        Collections.reverse(this.cpuResetTime);
        Long[] reversedTime = (Long[]) this.cpuResetTime.toArray(new Long[0]);
        try {
            FileOutputStream fos = getActivity().openFileOutput("offset_stat", 0);
            String a = "";
            for (Long l : reversedTime) {
                long f = l.longValue();
                if (reversedTime.length != 0) {
                    a = f + " " + a;
                }
            }
            fos.write(a.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        clearUI();
        loadUI(false);
    }

    public void loadResetState() {
        File a = new File(FilePath.OFFSET_STAT);
        if (a.exists() && this.cpuResetTime == null) {
            this.cpuResetTime = new ArrayList<>();
            String[] array = AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0);
            for (String b : array) {
                if (array.length > 1) {
                    this.cpuResetTime.add(Long.valueOf(Long.parseLong(b)));
                }
            }
            try {
                if (Long.parseLong(array[array.length - 1]) > SystemClock.elapsedRealtime() / 10) {
                    a.delete();
                    this.cpuResetTime = null;
                }
            } catch (NumberFormatException e) {
                a.delete();
                this.cpuResetTime = null;
            }
        }
    }

    private void loadUI(boolean firstView) {
        String frequency;
        long j;
        final ArrayList<String> cpuGraphValues = new ArrayList<>();
        int cpuData = getCpuData();
        this.mCompleteTime = 0.0d;
        this.pg = (PieGraph) this.root.findViewById(R.id.graph);
        if (cpuData == 0) {
            this.root.findViewById(R.id.noCpuData).setVisibility(0);
        } else {
            this.root.findViewById(R.id.noCpuData).setVisibility(8);
        }
        for (int k = 0; k < cpuData; k++) {
            String b = this.data[k];
            String[] c = b.split(" ");
            if (k == 0) {
                j = Long.parseLong(c[0]);
            } else {
                j = Long.parseLong(c[1]);
            }
            double a = j;
            this.cpuOverallTime.add(Long.valueOf((long) a));
            this.mCompleteTime += a;
        }
        this.cpuOverallTime.add(Long.valueOf((long) this.mCompleteTime));
        if (this.cpuResetTime != null) {
            String resetUptime = NO_DATA_FOUND;
            long mResetTime = 0;
            if (new File(FilePath.OFFSET_STAT).exists()) {
                resetUptime = AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0)[AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0).length - 1];
            }
            if (!resetUptime.equals(NO_DATA_FOUND)) {
                mResetTime = Long.parseLong(resetUptime);
            }
            this.mCompleteTime -= mResetTime;
        }
        for (int i = 0; i < cpuData; i++) {
            String b2 = this.data[i];
            String[] c2 = b2.split(" ");
            Long offsetTime = 0L;
            File offsetFile = new File(FilePath.OFFSET_STAT);
            if (offsetFile.exists()) {
                try {
                    offsetTime = Long.valueOf(Long.parseLong(AeroActivity.shell.getInfoArray(FilePath.OFFSET_STAT, 0, 0)[i]));
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.e("Aero", "The offset file might be smaller as assumed. " + e);
                    offsetFile.delete();
                } catch (NumberFormatException e2) {
                    Log.e("Aero", "The offset file might be unavailable. " + e2);
                    offsetFile.delete();
                }
            }
            if (i == 0) {
                this.cpuFreq.add(0L);
                if (this.cpuResetTime != null) {
                    this.cpuTime.add(Long.valueOf(((long) Integer.parseInt(c2[0])) - offsetTime.longValue()));
                } else {
                    this.cpuTime.add(Long.valueOf(Integer.parseInt(c2[0])));
                }
            } else {
                this.cpuFreq.add(Long.valueOf(Integer.parseInt(c2[0])));
                if (this.cpuResetTime != null) {
                    this.cpuTime.add(Long.valueOf(((long) Integer.parseInt(c2[1])) - offsetTime.longValue()));
                } else {
                    this.cpuTime.add(Long.valueOf(Integer.parseInt(c2[1])));
                }
            }
        }
        Long[] cpuFreqArray = (Long[]) this.cpuFreq.toArray(new Long[0]);
        int i2 = 0;
        int j2 = 0;
        Iterator<Long> it = this.cpuTime.iterator();
        while (it.hasNext()) {
            long g = it.next().longValue();
            if (j2 == 8) {
                j2 = 0;
            }
            if (cpuFreqArray[i2].longValue() == 0) {
                frequency = "DeepSleep";
            } else {
                frequency = AeroActivity.shell.toMHz(cpuFreqArray[i2].toString());
            }
            String time_in_state = convertTime(g);
            int percentage = (int) Math.round((g / this.mCompleteTime) * 100.0d);
            this.cpuPercentage.add(Long.valueOf(percentage));
            if (g != 0 && percentage >= 1) {
                PieSlice slice = new PieSlice();
                cpuGraphValues.add(frequency + " " + time_in_state + " " + percentage + "%");
                slice.setValue(10.0f);
                slice.setGoalValue(percentage);
                slice.setColor(Color.parseColor(FilePath.color_code[j2]));
                this.pg.setThickness(30);
                this.pg.addSlice(slice);
                j2++;
            }
            i2++;
        }
        createList(this.cpuFreq, this.cpuTime, this.cpuPercentage);
        if (firstView) {
            handleOnClick(cpuGraphValues);
        }
        this.pg.setOnTouchListener(new View.OnTouchListener() { // from class: com.aero.control.fragments.StatisticsFragment.4
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() != 0) {
                    return false;
                }
                StatisticsFragment.this.handleOnClick(cpuGraphValues);
                return true;
            }
        });
        this.pg.setDuration(1000);
        this.pg.setInterpolator(new AccelerateDecelerateInterpolator());
        this.pg.animateToGoalValues();
    }

    private void clearUI() {
        if (this.pg != null) {
            this.pg.removeSlices();
        }
        if (this.data != null) {
            this.data = new String[0];
        }
        if (this.cpuTime != null) {
            this.cpuTime.clear();
        }
        if (this.cpuOverallTime != null) {
            this.cpuOverallTime.clear();
        }
        if (this.cpuFreq != null) {
            this.cpuFreq.clear();
        }
        if (this.cpuPercentage != null) {
            this.cpuPercentage.clear();
        }
        if (this.statisticView != null) {
            this.mResult = new statisticInit[0];
        }
        this.mColorIndex = 0;
        this.mIndex = 0;
    }

    public final void handleOnClick(ArrayList<String> list) {
        for (String str : list) {
            int arrayLength = list.size();
            if (this.mIndex == arrayLength) {
                this.mIndex = 0;
                this.mColorIndex = 0;
            }
            if (this.mColorIndex >= 8) {
                this.mColorIndex = 0;
            }
            String currentRow = list.get(this.mIndex);
            String[] tmp = currentRow.split(" ");
            this.txtFreq = (TextView) this.root.findViewById(R.id.statisticFreq);
            this.txtTime = (TextView) this.root.findViewById(R.id.statisticTime);
            this.txtPercentage = (TextView) this.root.findViewById(R.id.statisticPercentage);
            if (tmp[1].contains("MHz")) {
                tmp[0] = tmp[0] + " MHz";
                tmp[1] = tmp[2];
                tmp[2] = tmp[3];
            }
            this.txtFreq.setText(tmp[0]);
            this.txtTime.setText(tmp[1]);
            this.txtPercentage.setText(tmp[2]);
            this.txtFreq.setTypeface(FilePath.kitkatFont);
            this.txtTime.setTypeface(FilePath.kitkatFont);
            this.txtPercentage.setTypeface(FilePath.kitkatFont);
            this.txtFreq.setTextColor(Color.parseColor(FilePath.color_code[this.mColorIndex]));
            this.txtTime.setTextColor(Color.parseColor(FilePath.color_code[this.mColorIndex]));
            this.txtPercentage.setTextColor(Color.parseColor(FilePath.color_code[this.mColorIndex]));
        }
        this.mColorIndex++;
        this.mIndex++;
    }

    public final String convertTime(long msTime) {
        long msTime2 = msTime * 10;
        return String.format("%02dh:%02dm:%02ds", Long.valueOf(TimeUnit.MILLISECONDS.toHours(msTime2)), Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(msTime2) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(msTime2))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(msTime2) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(msTime2))));
    }

    public final void createList(ArrayList<Long> cpuFreq, ArrayList<Long> cpuTime, ArrayList<Long> cpuPercentage) {
        cpuFreq.add(1L);
        cpuTime.add(Long.valueOf((long) this.mCompleteTime));
        cpuPercentage.add(100L);
        Long[] freq = (Long[]) cpuFreq.toArray(new Long[0]);
        Long[] time = (Long[]) cpuTime.toArray(new Long[0]);
        Long[] percentage = (Long[]) cpuPercentage.toArray(new Long[0]);
        ArrayDataLoader adl = new ArrayDataLoader();
        adl.loadSingleEntry(freq, time, percentage);
        this.statisticView = (ListView) this.root.findViewById(R.id.statisticListView);
        StatisticAdapter adapter = new StatisticAdapter(getActivity(), R.layout.statistic_layout, this.mResult);
        this.statisticView.setAdapter((ListAdapter) adapter);
    }

    public final int getCpuData() {
        if (!AeroActivity.genHelper.doesExist(FilePath.TIME_IN_STATE_PATH)) {
            return 0;
        }
        this.data = AeroActivity.shell.getInfo(FilePath.TIME_IN_STATE_PATH, true);
        if (this.data != null) {
            return this.data.length;
        }
        return 0;
    }

    private final class ArrayDataLoader {
        private ArrayDataLoader() {
        }

        public final void loadSingleEntry(Long[] freq, Long[] time, Long[] percentage) {
            int length = freq.length;
            for (int j = 0; j < length; j++) {
                if (percentage[j].longValue() != 0 && percentage[j].longValue() >= 1) {
                    String convertedFreq = AeroActivity.shell.toMHz(freq[j] + "");
                    if (convertedFreq.length() < 8) {
                        convertedFreq = convertedFreq + "\t";
                    } else if (convertedFreq.length() < 7) {
                        convertedFreq = convertedFreq + "\t\t";
                    }
                    if (j == 0) {
                        loadArray(StatisticsFragment.this.mResult, new statisticInit("Deepsleep", StatisticsFragment.this.convertTime(time[j].longValue()) + "", percentage[j] + "%"));
                    } else if (j == length - 1) {
                        loadArray(StatisticsFragment.this.mResult, new statisticInit("Uptime   ", StatisticsFragment.this.convertTime(time[j].longValue()) + "", percentage[j] + "%"));
                    } else {
                        loadArray(StatisticsFragment.this.mResult, new statisticInit(convertedFreq, StatisticsFragment.this.convertTime(time[j].longValue()) + "", percentage[j] + "%"));
                    }
                }
            }
        }

        public final void loadArray(statisticInit[] resultSet, statisticInit data) {
            StatisticsFragment.this.mResult = fillArray(resultSet, data);
        }

        public final statisticInit[] fillArray(statisticInit[] resultSet, statisticInit data) {
            statisticInit[] result = (statisticInit[]) Arrays.copyOf(resultSet, resultSet.length + 1);
            result[resultSet.length] = data;
            return result;
        }
    }
}
