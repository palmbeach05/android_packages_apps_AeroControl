package com.aero.control.helpers.PerApp;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.aero.control.adapter.AeroData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class perAppHelper {
    private Context mContext;
    private String[] mCurrentSelectedPackages;
    private boolean[] mIsChecked;
    private String[] mPackageNames;
    private List<ApplicationInfo> mPackages;
    private List<AeroData> mPerAppData = new ArrayList();
    private boolean mShowSystemApps;

    public perAppHelper(Context context) {
        this.mContext = context;
    }

    public final void setPackages(List<ApplicationInfo> packages) {
        this.mPackages = packages;
        getAllApps(getSystemAppStatus());
    }

    public final List<AeroData> getFullPackages() {
        return this.mPerAppData;
    }

    public final List<ApplicationInfo> getPackages() {
        return this.mPackages;
    }

    public final boolean getSystemAppStatus() {
        return this.mShowSystemApps;
    }

    public final void setSystemAppStatus(boolean showSystemApps) {
        this.mShowSystemApps = showSystemApps;
        this.mIsChecked = null;
    }

    public final boolean[] getCheckedState() {
        return this.mIsChecked;
    }

    public final String[] getCurrentSelectedPackages() {
        if (this.mIsChecked == null) {
            return this.mCurrentSelectedPackages;
        }
        ArrayList<String> selectedPackages = new ArrayList<>();
        int i = 0;
        boolean[] arr$ = this.mIsChecked;
        for (boolean checked : arr$) {
            if (checked) {
                selectedPackages.add(this.mPackageNames[i]);
            }
            i++;
        }
        this.mCurrentSelectedPackages = (String[]) selectedPackages.toArray(new String[0]);
        return this.mCurrentSelectedPackages;
    }

    public final void setChecked(boolean checkedState, int position) {
        if (this.mIsChecked == null) {
            this.mIsChecked = new boolean[this.mPackageNames.length];
        }
        this.mIsChecked[position] = checkedState;
    }

    public final void findMatch(String[] selectedApps) {
        int i = 0;
        String[] arr$ = this.mPackageNames;
        for (String a : arr$) {
            for (String b : selectedApps) {
                if (a.equals(b)) {
                    setChecked(true, i);
                }
            }
            i++;
        }
    }

    public final void getAllApps(boolean showSystemApp) {
        PackageManager pm = this.mContext.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        ArrayList<String> currentPackages = new ArrayList<>();
        if (this.mPackages == null) {
            Collections.sort(packages, new ApplicationInfo.DisplayNameComparator(pm));
            this.mPackages = packages;
        } else {
            packages = this.mPackages;
        }
        if (this.mPerAppData != null) {
            this.mPerAppData.clear();
        }
        this.mShowSystemApps = showSystemApp;
        for (ApplicationInfo packageInfo : packages) {
            if (showSystemApp || (packageInfo.flags & 1) == 0) {
                currentPackages.add(packageInfo.packageName);
                this.mPerAppData.add(new AeroData(packageInfo.loadIcon(pm), packageInfo.loadLabel(this.mContext.getPackageManager()).toString()));
            }
        }
        this.mPackageNames = (String[]) currentPackages.toArray(new String[0]);
    }
}
