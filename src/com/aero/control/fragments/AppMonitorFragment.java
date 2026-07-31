package com.aero.control.fragments;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.aero.control.AeroActivity;
import com.aero.control.R;
import com.aero.control.adapter.AppDataAdapter;
import com.aero.control.helpers.PerApp.AppMonitor.AppLogger;
import com.aero.control.helpers.PerApp.AppMonitor.JobManager;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElement;
import com.aero.control.helpers.PerApp.AppMonitor.model.AppElementDetail;
import com.aero.control.helpers.Util;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class AppMonitorFragment extends Fragment {
    private static final String FILENAME = "firstrun_appmonitor";
    private AppMonitorDetailFragment mAppMonitorDetailFragment;
    private final String mClassName = getClass().getName();
    private Context mContext;
    private ListView mListView;
    private ProgressDialog mProgressDialog;
    private ViewGroup mRoot;

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mRoot = (ViewGroup) inflater.inflate(R.layout.appmonitor_fragment, (ViewGroup) null);
        this.mContext = getActivity();
        loadUI();
        return this.mRoot;
    }

    @Override // android.app.Fragment
    public void onResume() {
        super.onResume();
        clearUI();
        loadUI();
    }

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int output = 0;
        if (AeroActivity.genHelper.doesExist(getActivity().getFilesDir().getAbsolutePath() + "/" + FILENAME)) {
            output = 1;
        }
        if (output == 0) {
            DrawFirstStart(R.string.showcase_app_monitor_fragment, R.string.showcase_app_monitor_fragment_summary);
        }
    }

    private void DrawFirstStart(int header, int content) {
        try {
            FileOutputStream fos = getActivity().openFileOutput(FILENAME, 0);
            fos.write("1".getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e("Aero", "Could not save file. ", e);
        }
        Target homeTarget = new Target() { // from class: com.aero.control.fragments.AppMonitorFragment.1
            @Override // com.github.amlcurran.showcaseview.targets.Target
            public Point getPoint() {
                return new Point(150, 125);
            }
        };
        new ShowcaseView.Builder(getActivity()).setContentTitle(header).setContentText(content).setTarget(homeTarget).build();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearUI() {
        this.mListView = null;
        this.mRoot.invalidate();
    }

    private void loadUI() {
        if (AeroActivity.mJobManager == null) {
            AeroActivity.mJobManager = JobManager.instance(this.mContext);
        }
        if (this.mProgressDialog == null) {
            this.mProgressDialog = new ProgressDialog(this.mContext);
            this.mProgressDialog.setMessage(Util.getRandomLoadingText(getActivity()));
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.spinner_animation));
        }
        this.mProgressDialog.show();
        AeroActivity.resetBackCounter();
        Runnable runnable = new AnonymousClass2();
        new Thread(runnable).start();
    }

    /* JADX INFO: renamed from: com.aero.control.fragments.AppMonitorFragment$2, reason: invalid class name */
    class AnonymousClass2 implements Runnable {
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            List<AppElement> appData = AeroActivity.mJobManager.getParentChildData(AppMonitorFragment.this.mContext);
            if (AppMonitorFragment.this.getActivity() != null) {
                AppMonitorFragment.this.getActivity().runOnUiThread(new AnonymousClass1(appData));
            }
        }

        /* JADX INFO: renamed from: com.aero.control.fragments.AppMonitorFragment$2$1, reason: invalid class name */
        class AnonymousClass1 implements Runnable {
            final /* synthetic */ List<AppElement> val$appData;

            AnonymousClass1(List<AppElement> list) {
                this.val$appData = list;
            }

            @Override // java.lang.Runnable
            public void run() {
                TextView tmp = (TextView) AppMonitorFragment.this.mRoot.findViewById(R.id.noData);
                ImageView iv = (ImageView) AppMonitorFragment.this.mRoot.findViewById(R.id.empty_image);
                AppMonitorFragment.this.mProgressDialog.dismiss();
                AppMonitorFragment.this.mProgressDialog.cancel();
                if (this.val$appData.size() > 0) {
                    tmp.setVisibility(8);
                    iv.setVisibility(8);
                } else {
                    tmp.setText(AppMonitorFragment.this.getText(R.string.pref_no_app_monitor_data));
                    tmp.setVisibility(0);
                    iv.setVisibility(0);
                }
                if (!AeroActivity.mJobManager.getJobManagerState()) {
                    tmp.setText(AppMonitorFragment.this.getText(R.string.pref_app_monitor_disabled));
                    tmp.setVisibility(0);
                    iv.setVisibility(0);
                    AppMonitorFragment.this.clearUI();
                    return;
                }
                if (AppLogger.getLogLevel() >= 1) {
                    for (AppElement a : this.val$appData) {
                        AppLogger.print(AppMonitorFragment.this.mClassName, a.getName(), 1);
                        for (AppElementDetail acd : a.getChildData()) {
                            AppLogger.print(AppMonitorFragment.this.mClassName, " -------> " + acd.getTitle() + " \n" + acd.getContent(), 1);
                        }
                    }
                }
                AppMonitorFragment.this.mListView = (ListView) AppMonitorFragment.this.mRoot.findViewById(R.id.apppstatistics);
                AppDataAdapter adapter = new AppDataAdapter(AppMonitorFragment.this.getActivity(), R.layout.perapp_stat_row, this.val$appData);
                AppMonitorFragment.this.mListView.setAdapter((ListAdapter) adapter);
                AppMonitorFragment.this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.aero.control.fragments.AppMonitorFragment.2.1.1
                    @Override // android.widget.AdapterView.OnItemClickListener
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Intent intent = AppMonitorFragment.this.getActivity().getIntent();
                        intent.putExtra("aero_data", (Parcelable) AnonymousClass1.this.val$appData.get(position));
                        if (AppMonitorFragment.this.mAppMonitorDetailFragment == null) {
                            AppMonitorFragment.this.mAppMonitorDetailFragment = new AppMonitorDetailFragment();
                        }
                        AeroActivity.mHandler.postDelayed(new Runnable() { // from class: com.aero.control.fragments.AppMonitorFragment.2.1.1.1
                            @Override // java.lang.Runnable
                            public void run() {
                                AppMonitorFragment.this.getFragmentManager().beginTransaction().setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).replace(R.id.content_frame, AppMonitorFragment.this.mAppMonitorDetailFragment).addToBackStack("AppDetail").commit();
                            }
                        }, AeroActivity.genHelper.getDefaultDelay());
                    }
                });
            }
        }
    }
}
