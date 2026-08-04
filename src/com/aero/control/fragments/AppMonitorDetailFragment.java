package com.aero.control.fragments;

import android.animation.TimeInterpolator;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.helpers.Android.Material.CardBox;
import com.aero.control.helpers.FilePath;
import com.aero.control.helpers.PerApp.AppMonitor.AppLogger;
import com.aero.control.helpers.PerApp.AppMonitor.AppModule;
import com.aero.control.helpers.PerApp.AppMonitor.JobManager;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElement;
import com.db.chart.Tools;
import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.animation.Animation;
import com.db.chart.view.animation.easing.cubic.CubicEaseOut;
import com.db.chart.view.animation.style.DashAnimation;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AppMonitorDetailFragment extends Fragment {
    private static final int MIN_VISIBLE_TABS = 3;
    private final String mClassName = getClass().getName();
    private LineChartView mLineChart;
    private TextView mAverage;
    private List<CardBox> mCards;
    private TextView mHeader;
    private Paint mLineGridPaint;
    private TextView mLineTooltip;
    private ImageButton mResetButton;
    private ViewGroup mRoot;
    private final TimeInterpolator enterInterpolator = new DecelerateInterpolator(1.5f);
    private final TimeInterpolator exitInterpolator = new AccelerateInterpolator();
    private String mAppName = null;
    private int mMaxValue = 0;
    private int mModule = 10;
    private int mPositionModule = 0;
    private final OnEntryClickListener lineEntryListener = new OnEntryClickListener() { // from class: com.aero.control.fragments.AppMonitorDetailFragment.1
        @Override // com.db.chart.listener.OnEntryClickListener
        public void onClick(int setIndex, int entryIndex, Rect rect) {
            if (AppMonitorDetailFragment.this.mLineTooltip == null) {
                AppMonitorDetailFragment.this.showLineTooltip(entryIndex, rect);
            } else {
                AppMonitorDetailFragment.this.dismissLineTooltip(entryIndex, rect);
            }
        }
    };
    private final View.OnClickListener lineClickListener = new View.OnClickListener() { // from class: com.aero.control.fragments.AppMonitorDetailFragment.2
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            if (AppMonitorDetailFragment.this.mLineTooltip != null) {
                AppMonitorDetailFragment.this.dismissLineTooltip(-1, null);
            }
        }
    };
    private final View.OnClickListener mCardListener = new View.OnClickListener() { // from class: com.aero.control.fragments.AppMonitorDetailFragment.3
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            int i = 0;
            CardBox cb = (CardBox) v;
            for (AppModule module : AeroActivity.mJobManager.getModules()) {
                if (module.getPrefix().equals(cb.getTitle())) {
                    AppMonitorDetailFragment.this.mModule = module.getIdentifier();
                    AppMonitorDetailFragment.this.mPositionModule = i;
                }
                i++;
            }
            AppMonitorDetailFragment.this.clearUI();
            AppMonitorDetailFragment.this.loadUI();
        }
    };
    private final View.OnClickListener mResetListener = new View.OnClickListener() { // from class: com.aero.control.fragments.AppMonitorDetailFragment.4
        @Override // android.view.View.OnClickListener
        public void onClick(View v) {
            AlertDialog dialog = new AlertDialog.Builder(AppMonitorDetailFragment.this.getActivity()).setTitle(R.string.warning).setIcon(R.drawable.warning).setMessage(R.string.pref_reset_stats_for_app).setPositiveButton(R.string.aero_continue, new DialogInterface.OnClickListener() { // from class: com.aero.control.fragments.AppMonitorDetailFragment.4.1
                @Override // android.content.DialogInterface.OnClickListener
                public void onClick(DialogInterface dialog2, int which) {
                    AeroActivity.mJobManager.forceCleanUp(AppMonitorDetailFragment.this.mAppName);
                    ((AeroActivity) AppMonitorDetailFragment.this.getActivity()).closeAppDetail();
                }
            }).create();
            dialog.show();
        }
    };

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLogger.print(this.mClassName, "Creating AppMonitorDetailFragment result view", -1);
        this.mRoot = (ViewGroup) inflater.inflate(R.layout.appmonitor_detail, (ViewGroup) null);
        this.mAppName = null;
        this.mHeader = (TextView) this.mRoot.findViewById(R.id.usageTimer);
        this.mAverage = (TextView) this.mRoot.findViewById(R.id.topValue);
        this.mCards = new ArrayList();
        this.mResetButton = (ImageButton) this.mRoot.findViewById(R.id.reset_stats);
        final HorizontalScrollView horizontalScroll = (HorizontalScrollView) this.mRoot.findViewById(R.id.horizontalscreen);
        LinearLayout layoutHolder = (LinearLayout) this.mRoot.findViewById(R.id.layouthorizontal);
        for (AppModule appModule : AeroActivity.mJobManager.getModules()) {
            CardBox cardbox = new CardBox(getActivity());
            cardbox.setOnClickListener(this.mCardListener);
            this.mCards.add(cardbox);
        }
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        for (CardBox c : this.mCards) {
            layoutHolder.addView(c, layoutParams);
        }
        horizontalScroll.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                int viewportWidth = horizontalScroll.getWidth();
                if (viewportWidth <= 0 || AppMonitorDetailFragment.this.mCards == null || AppMonitorDetailFragment.this.mCards.isEmpty()) {
                    return;
                }
                int visibleTabs = Math.min(MIN_VISIBLE_TABS, AppMonitorDetailFragment.this.mCards.size());
                int edgeSpacing = getResources().getDimensionPixelSize(R.dimen.appmonitor_tab_edge_spacing);
                int tabSpacing = getResources().getDimensionPixelSize(R.dimen.appmonitor_tab_spacing);
                int availableWidth = viewportWidth - (edgeSpacing * 2) - (tabSpacing * (visibleTabs - 1));
                int maxTabWidth = getResources().getDimensionPixelSize(R.dimen.appmonitor_tab_max_width);
                int tabWidth = Math.min(availableWidth / visibleTabs, maxTabWidth);
                for (CardBox c : AppMonitorDetailFragment.this.mCards) {
                    ViewGroup.LayoutParams lp = c.getLayoutParams();
                    lp.width = tabWidth;
                    c.setLayoutParams(lp);
                }
                horizontalScroll.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
        clearUI();
        loadUI();
        return this.mRoot;
    }

    @Override // android.app.Fragment
    public void onDestroyView() {
        AppLogger.print(this.mClassName, "Destroying AppMonitorDetailFragment result view", -1);
        if (this.mLineTooltip != null) {
            this.mLineTooltip.animate().cancel();
            if (this.mLineChart != null) {
                this.mLineChart.dismissTooltip(this.mLineTooltip);
            }
            this.mLineTooltip = null;
        }
        if (this.mLineChart != null) {
            this.mLineChart.setOnEntryClickListener(null);
            this.mLineChart.setOnClickListener(null);
            this.mLineChart.reset();
            this.mLineChart = null;
        }
        if (this.mCards != null) {
            for (CardBox c : this.mCards) {
                c.setOnClickListener(null);
            }
            this.mCards.clear();
            this.mCards = null;
        }
        if (this.mResetButton != null) {
            this.mResetButton.setOnClickListener(null);
            this.mResetButton = null;
        }
        this.mHeader = null;
        this.mAverage = null;
        this.mRoot = null;
        super.onDestroyView();
    }

    public final void setTitle(String title) {
        ((AeroActivity) getActivity()).setActionBarTitle(title);
    }

    private int resolveThemeDrawable(int attrResId, int fallbackResId) {
        TypedValue typedValue = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.resourceId;
        }
        return fallbackResId;
    }

    private int resolveThemeColor(int attrResId, int fallbackColorResId) {
        TypedValue typedValue = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.resourceId != 0 ? getResources().getColor(typedValue.resourceId) : typedValue.data;
        }
        return getResources().getColor(fallbackColorResId);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearUI() {
        this.mMaxValue = 0;
        this.mRoot.invalidate();
        int cardBackground = resolveThemeDrawable(R.attr.aeroCardBackground, R.drawable.card);
        for (CardBox b : this.mCards) {
            b.setBackground(cardBackground);
        }
        int cardBackgroundClicked = resolveThemeDrawable(R.attr.aeroCardBackgroundClicked, R.drawable.card_clicked);
        this.mCards.get(this.mPositionModule).setBackground(cardBackgroundClicked);
        if (this.mLineTooltip != null) {
            mLineChart.dismissTooltip(this.mLineTooltip);
            this.mLineTooltip = null;
        }
        if (mLineChart != null) {
            mLineChart.reset();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadUI() {
        int i = 0;
        if (AeroActivity.mJobManager == null) {
            AeroActivity.mJobManager = JobManager.instance(getActivity());
        }
        AeroActivity.mJobManager.wakeUp();
        AppElement data = null;
        String suffix = "";
        if (this.mAppName == null) {
            Bundle args = getArguments();
            data = args != null ? (AppElement) args.getParcelable("aero_data") : null;
            if (data == null) {
                if (isAdded()) {
                    ((AeroActivity) getActivity()).closeAppDetail();
                }
                return;
            }
        } else {
            List<AppElement> AppData = AeroActivity.mJobManager.getParentChildData(getActivity());
            for (AppElement a : AppData) {
                if (a.getName().equals(this.mAppName)) {
                    data = a;
                }
            }
        }
        if (data == null) {
            if (isAdded()) {
                ((AeroActivity) getActivity()).closeAppDetail();
            }
            return;
        }
        setTitle(data.getRealName());
        this.mAppName = data.getName();
        this.mResetButton.setOnClickListener(this.mResetListener);
        for (AppModule module : AeroActivity.mJobManager.getModules()) {
            if (this.mModule == module.getIdentifier()) {
                suffix = module.getSuffix();
                this.mAverage.setText(getString(R.string.pref_app_monitor_average_summary, getString(R.string.average), data.getChildData().get(i + 1).getTitle(), data.getChildData().get(i + 1).getContent()));
            }
            this.mCards.get(i).setContent(module.getDrawable());
            this.mCards.get(i).setTitle(data.getChildData().get(i + 1).getTitle());
            i++;
        }
        loadGraph(data, suffix);
        this.mHeader.setText(((Object) getText(R.string.usage_time)) + " - " + data.getChildData().get(0).getTitle());
        this.mHeader.setTypeface(FilePath.kitkatFont);
    }

    private void loadGraph(AppElement data, String suffix) {
        mLineChart = (LineChartView) this.mRoot.findViewById(R.id.graph);
        mLineChart.setOnEntryClickListener(this.lineEntryListener);
        mLineChart.setOnClickListener(this.lineClickListener);
        mLineChart.setLabelColor(resolveThemeColor(R.attr.aeroGraphLabelColor, R.color.text_color));
        this.mLineGridPaint = new Paint();
        this.mLineGridPaint.setColor(resolveThemeColor(R.attr.aeroDividerColor, R.color.grey));
        this.mLineGridPaint.setPathEffect(new DashPathEffect(new float[]{5.0f, 5.0f}, 0.0f));
        this.mLineGridPaint.setStyle(Paint.Style.STROKE);
        this.mLineGridPaint.setAntiAlias(true);
        this.mLineGridPaint.setStrokeWidth(Tools.fromDpToPx(0.75f));
        mLineChart.reset();
        loadLine(data);
        mLineChart.setBorderSpacing(Tools.fromDpToPx(10.0f)).setGrid(ChartView.GridType.HORIZONTAL, this.mLineGridPaint).setXLabels(AxisController.LabelPosition.OUTSIDE).setYLabels(AxisController.LabelPosition.OUTSIDE).setXAxis(false).setYAxis(false).setAxisBorderValues(0, this.mMaxValue, calculateSteps(0, this.mMaxValue)).setLabelsFormat(new DecimalFormat("##" + suffix)).show(new Animation().setAlpha(-1).setEasing(new CubicEaseOut()).setOverlap(0.5f).setDuration(750).setStartPoint(0.5f, 0.5f));
        mLineChart.animateSet(1, new DashAnimation());
    }

    private void loadLine(AppElement data) {
        int realPart;
        List<Integer> rawData = AeroActivity.mJobManager.getRawData(data.getName(), this.mModule);
        if (rawData != null) {
            int lineColor = resolveThemeColor(R.attr.aeroGraphLineColor, R.color.material_blue);
            int size = rawData.size();
            int position = 0;
            int chunks = size / 10;
            int rest = size % 10;
            int average = 0;
            LineSet dataSet = new LineSet();
            this.mMaxValue = 0;
            for (Integer j : rawData) {
                if (j.intValue() > this.mMaxValue) {
                    this.mMaxValue = j.intValue();
                }
            }
            int i = 0;
            while (i < size) {
                int counter = 0;
                if (rest > 0) {
                    rest--;
                    realPart = chunks + 1;
                } else {
                    realPart = chunks;
                }
                for (int n = i; n < i + realPart; n++) {
                    average += rawData.get(n).intValue();
                    counter++;
                }
                dataSet.addPoint(position + "", average / Math.max(counter, 1));
                dataSet.setDots(true).setDotsColor(lineColor).setDotsRadius(Tools.fromDpToPx(5.0f)).setDotsStrokeThickness(Tools.fromDpToPx(2.0f)).setDotsStrokeColor(getResources().getColor(R.color.white)).setLineColor(lineColor).setLineThickness(Tools.fromDpToPx(3.0f)).setSmooth(true);
                position++;
                average = 0;
                i += realPart;
            }
            mLineChart.addData(dataSet);
        }
    }

    private int calculateSteps(int minValue, int maxValue) {
        int range = maxValue - minValue;
        return (int) Math.max(Math.ceil(range / 5), 1.0d);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showLineTooltip(int entryIndex, Rect rect) {
        this.mLineTooltip = (TextView) getActivity().getLayoutInflater().inflate(R.layout.circular_tooltip, (ViewGroup) null);
        this.mLineTooltip.setText(((int) mLineChart.getData().get(0).getEntry(entryIndex).getValue()) + "");
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams((int) Tools.fromDpToPx(64.0f), (int) Tools.fromDpToPx(64.0f));
        layoutParams.leftMargin = rect.centerX() - (layoutParams.width / 2);
        layoutParams.topMargin = rect.centerY() - (layoutParams.height / 2);
        this.mLineTooltip.setLayoutParams(layoutParams);
        if (Build.VERSION.SDK_INT >= 12) {
            this.mLineTooltip.setPivotX(layoutParams.width / 2);
            this.mLineTooltip.setPivotY(layoutParams.height / 2);
            this.mLineTooltip.setAlpha(0.0f);
            this.mLineTooltip.setScaleX(0.0f);
            this.mLineTooltip.setScaleY(0.0f);
            this.mLineTooltip.animate().setDuration(150L).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).rotation(360.0f).setInterpolator(this.enterInterpolator);
        }
        mLineChart.showTooltip(this.mLineTooltip);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void dismissLineTooltip(final int entryIndex, final Rect rect) {
        if (Build.VERSION.SDK_INT >= 16) {
            this.mLineTooltip.animate().setDuration(100L).scaleX(0.0f).scaleY(0.0f).alpha(0.0f).setInterpolator(this.exitInterpolator).withEndAction(new Runnable() { // from class: com.aero.control.fragments.AppMonitorDetailFragment.5
                @Override // java.lang.Runnable
                public void run() {
                    if (!AppMonitorDetailFragment.this.isAdded() || AppMonitorDetailFragment.this.mLineChart == null || AppMonitorDetailFragment.this.mLineTooltip == null) {
                        return;
                    }
                    AppMonitorDetailFragment.this.mLineChart.removeView(AppMonitorDetailFragment.this.mLineTooltip);
                    AppMonitorDetailFragment.this.mLineTooltip = null;
                    if (entryIndex != -1) {
                        AppMonitorDetailFragment.this.showLineTooltip(entryIndex, rect);
                    }
                }
            });
            return;
        }
        mLineChart.dismissTooltip(this.mLineTooltip);
        this.mLineTooltip = null;
        if (entryIndex != -1) {
            showLineTooltip(entryIndex, rect);
        }
    }
}
