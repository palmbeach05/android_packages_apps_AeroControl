package com.aero.control.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
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
import com.aero.control.helpers.CpuClusterHelper;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class StatisticsFragment extends Fragment {
    public static final String FILENAME_STATISTICS = "firstrun_statistics";
    private static final String STATE_SELECTED_CLUSTER_MEMBERS = "selected_cluster_members";
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
    private double mCompleteTime = 0.0d;
    private final CpuClusterHelper mClusterHelper = new CpuClusterHelper();
    private List<CpuClusterHelper.Cluster> mClusters = Collections.emptyList();
    private CpuClusterHelper.Cluster mSelectedCluster;
    public ArrayList<Long> cpuTime = new ArrayList<>();
    public ArrayList<Long> cpuOverallTime = new ArrayList<>();
    public ArrayList<Long> cpuFreq = new ArrayList<>();
    public ArrayList<Long> cpuPercentage = new ArrayList<>();
    public statisticInit[] mResult = new statisticInit[0];

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        this.root = (ViewGroup) inflater.inflate(R.layout.statistics, (ViewGroup) null);
        this.mClusters = this.mClusterHelper.getClusters();
        this.mSelectedCluster = restoreSelectedCluster(savedInstanceState);
        clearUI();
        loadResetState();
        loadUI(true);
        return this.root;
    }

    @Override // android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.statistic_menu, menu);
        MenuItem selectCluster = menu.findItem(R.id.action_select_cluster);
        if (selectCluster != null) {
            selectCluster.setVisible(this.mClusters.size() > 1);
        }
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
            case R.id.action_select_cluster:
                showClusterSelectionDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showClusterSelectionDialog() {
        if (this.mClusters.size() <= 1) {
            return;
        }
        final CharSequence[] clusterLabels = new CharSequence[this.mClusters.size()];
        for (int i = 0; i < this.mClusters.size(); i++) {
            clusterLabels[i] = getString(R.string.select_cluster_cpu_label, this.mClusters.get(i).getMemberRangeLabel());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.select_cluster_title);
        builder.setItems(clusterLabels, new DialogInterface.OnClickListener() {
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                StatisticsFragment.this.selectCluster(StatisticsFragment.this.mClusters.get(which));
            }
        });
        builder.show();
    }

    private void selectCluster(CpuClusterHelper.Cluster cluster) {
        if (cluster == this.mSelectedCluster) {
            return;
        }
        this.mSelectedCluster = cluster;
        clearUI();
        loadResetState();
        loadUI(true);
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

    @Override // android.app.Fragment
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mSelectedCluster != null) {
            int[] memberIds = new int[this.mSelectedCluster.getMembers().size()];
            for (int i = 0; i < memberIds.length; i++) {
                memberIds[i] = this.mSelectedCluster.getMembers().get(i);
            }
            outState.putIntArray(STATE_SELECTED_CLUSTER_MEMBERS, memberIds);
        }
    }

    private CpuClusterHelper.Cluster restoreSelectedCluster(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int[] savedMemberIds = savedInstanceState.getIntArray(STATE_SELECTED_CLUSTER_MEMBERS);
            if (savedMemberIds != null && savedMemberIds.length > 0) {
                List<Integer> savedMembers = new ArrayList<>();
                for (int id : savedMemberIds) {
                    savedMembers.add(id);
                }
                for (CpuClusterHelper.Cluster cluster : this.mClusters) {
                    if (cluster.getMembers().equals(savedMembers)) {
                        return cluster;
                    }
                }
            }
        }
        return this.mClusters.isEmpty() ? null : this.mClusters.get(0);
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
        if (this.mSelectedCluster == null) {
            return;
        }
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
            FileOutputStream fos = getActivity().openFileOutput(getResetFileName(this.mSelectedCluster), 0);
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

    /**
     * Builds the stable per-cluster reset offset filename from the cluster's sorted member
     * IDs, e.g. {@code offset_stat_cpu_0_1} for members {@code [0, 1]}. This ties the reset
     * state to a specific set of CPUs rather than a shared, cluster-agnostic file.
     */
    private String getResetFileName(CpuClusterHelper.Cluster cluster) {
        StringBuilder name = new StringBuilder("offset_stat_cpu");
        for (Integer member : cluster.getMembers()) {
            name.append('_').append(member);
        }
        return name.toString();
    }

    private File getResetFile(CpuClusterHelper.Cluster cluster) {
        return new File(getActivity().getFilesDir(), getResetFileName(cluster));
    }

    public void loadResetState() {
        File legacy = new File(FilePath.OFFSET_STAT);
        if (legacy.exists()) {
            Log.w("Aero", "Discarding legacy offset_stat file; it has no cluster identity.");
            legacy.delete();
        }
        this.cpuResetTime = (this.mSelectedCluster != null && getResetFile(this.mSelectedCluster).exists()) ? new ArrayList<Long>() : null;
    }

    private void loadUI(boolean firstView) {
        final ArrayList<GraphEntry> cpuGraphValues = new ArrayList<>();
        getCpuData();
        ArrayList<ParsedEntry> acceptedEntries = orderAcceptedEntries(parseAcceptedEntries(this.data));
        this.mCompleteTime = 0.0d;
        this.pg = (PieGraph) this.root.findViewById(R.id.graph);
        if (acceptedEntries.isEmpty()) {
            this.root.findViewById(R.id.noCpuData).setVisibility(0);
        } else {
            this.root.findViewById(R.id.noCpuData).setVisibility(8);
        }
        for (ParsedEntry entry : acceptedEntries) {
            this.cpuOverallTime.add(Long.valueOf(entry.residency));
            this.mCompleteTime += entry.residency;
        }
        this.cpuOverallTime.add(Long.valueOf((long) this.mCompleteTime));

        long[] resetOffsets = this.cpuResetTime != null ? readResetOffsets(acceptedEntries.size()) : null;
        long[] adjustedResidencies = null;
        if (resetOffsets != null) {
            adjustedResidencies = new long[acceptedEntries.size()];
            boolean resetValid = true;
            for (int i = 0; i < acceptedEntries.size(); i++) {
                long adjusted = acceptedEntries.get(i).residency - resetOffsets[i];
                if (adjusted < 0) {
                    resetValid = false;
                    break;
                }
                adjustedResidencies[i] = adjusted;
            }
            if (resetValid) {
                this.mCompleteTime -= resetOffsets[acceptedEntries.size()];
            } else {
                Log.w("Aero", "A reset offset would produce a negative residency; discarding reset state.");
                getResetFile(this.mSelectedCluster).delete();
                this.cpuResetTime = null;
                adjustedResidencies = null;
            }
        }

        for (int i = 0; i < acceptedEntries.size(); i++) {
            ParsedEntry entry = acceptedEntries.get(i);
            long residency = adjustedResidencies != null ? adjustedResidencies[i] : entry.residency;
            this.cpuFreq.add(Long.valueOf(entry.frequency));
            this.cpuTime.add(Long.valueOf(residency));
        }

        Long[] cpuFreqArray = (Long[]) this.cpuFreq.toArray(new Long[0]);
        if (this.mCompleteTime > 0.0d) {
            int i2 = 0;
            Iterator<Long> it = this.cpuTime.iterator();
            while (it.hasNext()) {
                long g = it.next().longValue();
                String frequency = acceptedEntries.get(i2).isDeepsleep ? "Deep Sleep" : AeroActivity.shell.toMHz(cpuFreqArray[i2].toString());
                String time_in_state = convertTime(g);
                int percentage = (int) Math.round((g / this.mCompleteTime) * 100.0d);
                this.cpuPercentage.add(Long.valueOf(percentage));
                PieSlice slice = new PieSlice();
                cpuGraphValues.add(new GraphEntry(frequency + " " + time_in_state + " " + percentage + "%", i2));
                slice.setValue(10.0f);
                slice.setGoalValue(percentage);
                slice.setColor(StatisticAdapter.getColorForIndex(i2));
                this.pg.setThickness(30);
                this.pg.addSlice(slice);
                i2++;
            }
        } else {
            Log.w("Aero", "Total accepted residency is zero or negative; skipping percentage calculations.");
            int i2 = 0;
            Iterator<Long> it = this.cpuTime.iterator();
            while (it.hasNext()) {
                long g = it.next().longValue();
                String frequency = acceptedEntries.get(i2).isDeepsleep ? "Deep Sleep" : AeroActivity.shell.toMHz(cpuFreqArray[i2].toString());
                String time_in_state = convertTime(g);
                this.cpuPercentage.add(0L);
                PieSlice slice = new PieSlice();
                cpuGraphValues.add(new GraphEntry(frequency + " " + time_in_state + " 0%", i2));
                slice.setValue(10.0f);
                slice.setGoalValue(0);
                slice.setColor(StatisticAdapter.getColorForIndex(i2));
                this.pg.setThickness(30);
                this.pg.addSlice(slice);
                i2++;
            }
        }
        if (acceptedEntries.isEmpty()) {
            this.statisticView = (ListView) this.root.findViewById(R.id.statisticListView);
            StatisticAdapter adapter = new StatisticAdapter(getActivity(), R.layout.statistic_layout, this.mResult);
            this.statisticView.setAdapter((ListAdapter) adapter);
        } else {
            createList(this.cpuFreq, this.cpuTime, this.cpuPercentage, acceptedEntries);
        }
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
        this.mIndex = 0;
    }

    private final void handleOnClick(ArrayList<GraphEntry> list) {
        for (GraphEntry ignored : list) {
            int arrayLength = list.size();
            if (this.mIndex == arrayLength) {
                this.mIndex = 0;
            }
            GraphEntry currentEntry = list.get(this.mIndex);
            String[] tmp = currentEntry.displayText.split(" ");
            this.txtFreq = (TextView) this.root.findViewById(R.id.statisticFreq);
            this.txtTime = (TextView) this.root.findViewById(R.id.statisticTime);
            this.txtPercentage = (TextView) this.root.findViewById(R.id.statisticPercentage);
            if (tmp[1].contains("MHz")) {
                tmp[0] = tmp[0] + " MHz";
                tmp[1] = tmp[2];
                tmp[2] = tmp[3];
            } else if ("Deep".equals(tmp[0]) && "Sleep".equals(tmp[1])) {
                tmp[0] = tmp[0] + " " + tmp[1];
                tmp[1] = tmp[2];
                tmp[2] = tmp[3];
            }
            this.txtFreq.setText(tmp[0]);
            this.txtTime.setText(tmp[1]);
            this.txtPercentage.setText(tmp[2]);
            this.txtFreq.setTypeface(FilePath.kitkatFont);
            this.txtTime.setTypeface(FilePath.kitkatFont);
            this.txtPercentage.setTypeface(FilePath.kitkatFont);
            int color = StatisticAdapter.getColorForIndex(currentEntry.acceptedIndex);
            this.txtFreq.setTextColor(color);
            this.txtTime.setTextColor(color);
            this.txtPercentage.setTextColor(color);
        }
        this.mIndex++;
    }

    public final String convertTime(long msTime) {
        long msTime2 = msTime * 10;
        return String.format("%02dh:%02dm:%02ds", Long.valueOf(TimeUnit.MILLISECONDS.toHours(msTime2)), Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(msTime2) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(msTime2))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(msTime2) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(msTime2))));
    }

    public final void createList(ArrayList<Long> cpuFreq, ArrayList<Long> cpuTime, ArrayList<Long> cpuPercentage, ArrayList<ParsedEntry> acceptedEntries) {
        cpuFreq.add(1L);
        cpuTime.add(Long.valueOf((long) this.mCompleteTime));
        cpuPercentage.add(100L);
        boolean[] isDeepsleep = new boolean[cpuFreq.size()];
        for (int i = 0; i < acceptedEntries.size(); i++) {
            isDeepsleep[i] = acceptedEntries.get(i).isDeepsleep;
        }
        isDeepsleep[isDeepsleep.length - 1] = false;
        Long[] freq = (Long[]) cpuFreq.toArray(new Long[0]);
        Long[] time = (Long[]) cpuTime.toArray(new Long[0]);
        Long[] percentage = (Long[]) cpuPercentage.toArray(new Long[0]);
        ArrayDataLoader adl = new ArrayDataLoader();
        adl.loadSingleEntry(freq, time, percentage, isDeepsleep);
        this.statisticView = (ListView) this.root.findViewById(R.id.statisticListView);
        StatisticAdapter adapter = new StatisticAdapter(getActivity(), R.layout.statistic_layout, this.mResult);
        this.statisticView.setAdapter((ListAdapter) adapter);
    }

    public final int getCpuData() {
        if (this.mSelectedCluster == null) {
            return 0;
        }
        String path = FilePath.CPU_BASE_PATH + this.mSelectedCluster.getRepresentativeCpu() + FilePath.CPU_TIME_IN_STATE_SUFFIX;
        if (!AeroActivity.genHelper.doesExist(path)) {
            return 0;
        }
        this.data = AeroActivity.shell.getInfo(path, true);
        if (this.data != null) {
            return this.data.length;
        }
        return 0;
    }

    /**
     * A visible pie slice's center-display text paired with the accepted-entry index it was
     * derived from, so the pie center and the pie slice always resolve to the same color via
     * {@link StatisticAdapter#getColorForIndex(int)} even when earlier accepted entries were
     * excluded from the pie graph.
     */
    private static final class GraphEntry {
        private final String displayText;
        private final int acceptedIndex;

        private GraphEntry(String displayText, int acceptedIndex) {
            this.displayText = displayText;
            this.acceptedIndex = acceptedIndex;
        }
    }

    /**
     * A single accepted row from a per-cluster {@code cpufreq} {@code time_in_state} file
     * (see {@link FilePath#CPU_TIME_IN_STATE_SUFFIX}): either a deep sleep duration or a
     * frequency/residency pair.
     */
    private static final class ParsedEntry {
        private final long frequency;
        private final long residency;
        private final boolean isDeepsleep;

        private ParsedEntry(long frequency, long residency, boolean isDeepsleep) {
            this.frequency = frequency;
            this.residency = residency;
            this.isDeepsleep = isDeepsleep;
        }
    }

    /**
     * Parses the raw rows of the legacy cpufreq {@code time_in_state} format:
     * <ul>
     *   <li>a single non-negative {@code long} token: a deep sleep residency duration
     *       (frequency is reported as {@code 0})</li>
     *   <li>two non-negative {@code long} tokens "frequency residency": a frequency
     *       residency entry</li>
     * </ul>
     * Blank rows, rows with any other token count, negative values, and numeric-overflow
     * values are ignored and logged (without a stack trace) rather than aborting the
     * refresh. Accepted rows are returned in their original source order; a row's type is
     * always determined by its own token count, never by its position.
     */
    private static ArrayList<ParsedEntry> parseAcceptedEntries(String[] rawRows) {
        ArrayList<ParsedEntry> accepted = new ArrayList<>();
        if (rawRows == null) {
            return accepted;
        }
        for (String rawRow : rawRows) {
            ParsedEntry entry = parseTimeInStateRow(rawRow);
            if (entry != null) {
                accepted.add(entry);
            }
        }
        return accepted;
    }

    /**
     * Orders accepted entries into the standardized display order used for reset offsets,
     * percentages, list rows, pie slices and colors: a valid {@code DeepSleep} entry first,
     * followed by frequency entries sorted by ascending numeric frequency. {@code Uptime} is
     * never part of this collection; it is always appended separately as the final synthetic
     * list row.
     */
    private static ArrayList<ParsedEntry> orderAcceptedEntries(ArrayList<ParsedEntry> entries) {
        ArrayList<ParsedEntry> ordered = new ArrayList<>(entries);
        Collections.sort(ordered, new Comparator<ParsedEntry>() {
            @Override // java.util.Comparator
            public int compare(ParsedEntry a, ParsedEntry b) {
                if (a.isDeepsleep != b.isDeepsleep) {
                    return a.isDeepsleep ? -1 : 1;
                }
                return Long.valueOf(a.frequency).compareTo(Long.valueOf(b.frequency));
            }
        });
        return ordered;
    }

    private static ParsedEntry parseTimeInStateRow(String rawRow) {
        if (rawRow == null) {
            return null;
        }
        String trimmed = rawRow.trim();
        if (trimmed.length() == 0) {
            return null;
        }
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length == 1) {
            long duration = parseNonNegativeLong(tokens[0]);
            if (duration < 0) {
                Log.w("Aero", "Ignoring malformed time_in_state deep sleep row: " + rawRow);
                return null;
            }
            return new ParsedEntry(0L, duration, true);
        }
        if (tokens.length == 2) {
            long frequency = parseNonNegativeLong(tokens[0]);
            long residency = parseNonNegativeLong(tokens[1]);
            if (frequency < 0 || residency < 0) {
                Log.w("Aero", "Ignoring malformed time_in_state frequency row: " + rawRow);
                return null;
            }
            return new ParsedEntry(frequency, residency, false);
        }
        Log.w("Aero", "Ignoring time_in_state row with an unexpected token count: " + rawRow);
        return null;
    }

    private static long parseNonNegativeLong(String token) {
        try {
            long value = Long.parseLong(token);
            return value < 0 ? -1L : value;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * Reads and validates the on-disk reset offsets for the selected cluster so they can be
     * applied to {@code acceptedCount} accepted parsed entries, in the standardized display
     * order. The offset file stores one non-negative long per accepted entry followed by a
     * trailing total-uptime offset (the format written by {@link #resetStatistics()}); the
     * trailing total must equal the sum of the entry offsets and must not exceed the current
     * elapsed uptime. Discards only the selected cluster's on-disk reset state and returns
     * {@code null} when the file does not contain exactly {@code acceptedCount + 1} values,
     * any value is malformed or negative, the entry-offset sum overflows, or the trailing
     * total does not match that sum or exceeds uptime.
     */
    private long[] readResetOffsets(int acceptedCount) {
        if (this.mSelectedCluster == null) {
            return null;
        }
        File offsetFile = getResetFile(this.mSelectedCluster);
        if (!offsetFile.exists()) {
            return null;
        }
        String[] rawOffsets = AeroActivity.shell.getInfoArray(offsetFile.getAbsolutePath(), 0, 0);
        if (rawOffsets == null || rawOffsets.length != acceptedCount + 1) {
            Log.w("Aero", "Offset file does not contain exactly the expected number of offsets; discarding reset state.");
            offsetFile.delete();
            this.cpuResetTime = null;
            return null;
        }
        long[] offsets = new long[acceptedCount + 1];
        long entryOffsetSum = 0L;
        for (int i = 0; i < offsets.length; i++) {
            long value;
            try {
                value = Long.parseLong(rawOffsets[i]);
            } catch (NumberFormatException e) {
                Log.w("Aero", "Offset file contains a malformed value; discarding reset state.");
                offsetFile.delete();
                this.cpuResetTime = null;
                return null;
            }
            if (value < 0) {
                Log.w("Aero", "Offset file contains a negative offset; discarding reset state.");
                offsetFile.delete();
                this.cpuResetTime = null;
                return null;
            }
            offsets[i] = value;
            if (i < acceptedCount) {
                long newSum = entryOffsetSum + value;
                if (newSum < entryOffsetSum) {
                    Log.w("Aero", "Offset file entry-offset sum overflowed; discarding reset state.");
                    offsetFile.delete();
                    this.cpuResetTime = null;
                    return null;
                }
                entryOffsetSum = newSum;
            }
        }
        long trailingTotal = offsets[acceptedCount];
        if (trailingTotal != entryOffsetSum) {
            Log.w("Aero", "Offset file trailing total does not equal the entry-offset sum; discarding reset state.");
            offsetFile.delete();
            this.cpuResetTime = null;
            return null;
        }
        if (trailingTotal > SystemClock.elapsedRealtime() / 10) {
            Log.w("Aero", "Offset file trailing total exceeds elapsed uptime; discarding reset state.");
            offsetFile.delete();
            this.cpuResetTime = null;
            return null;
        }
        return offsets;
    }

    private final class ArrayDataLoader {
        private ArrayDataLoader() {
        }

        public final void loadSingleEntry(Long[] freq, Long[] time, Long[] percentage, boolean[] isDeepsleep) {
            int length = freq.length;
            for (int j = 0; j < length; j++) {
                String convertedFreq = AeroActivity.shell.toMHz(freq[j] + "");
                if (convertedFreq.length() < 8) {
                    convertedFreq = convertedFreq + "\t";
                } else if (convertedFreq.length() < 7) {
                    convertedFreq = convertedFreq + "\t\t";
                }
                if (isDeepsleep[j]) {
                    loadArray(StatisticsFragment.this.mResult, new statisticInit("Deep Sleep", StatisticsFragment.this.convertTime(time[j].longValue()) + "", percentage[j] + "%", j));
                } else if (j == length - 1) {
                    loadArray(StatisticsFragment.this.mResult, new statisticInit("Uptime   ", StatisticsFragment.this.convertTime(time[j].longValue()) + "", percentage[j] + "%", j));
                } else {
                    loadArray(StatisticsFragment.this.mResult, new statisticInit(convertedFreq, StatisticsFragment.this.convertTime(time[j].longValue()) + "", percentage[j] + "%", j));
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
