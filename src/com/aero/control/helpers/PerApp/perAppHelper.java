package com.aero.control.helpers.PerApp;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import com.aero.control.adapter.AeroData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class for managing the list of applications available for per-app monitoring.
 * Handles filtering system/user apps, maintaining selection state, and retrieving
 * package information for display in the per-app configuration UI.
 */
public class perAppHelper {
    private Context mContext;
    private String[] mCurrentSelectedPackages;
    private boolean[] mIsChecked;
    private String[] mPackageNames;
    private List<ApplicationInfo> mPackages;
    private List<AeroData> mPerAppData = new ArrayList();
    private boolean mShowSystemApps;

    /**
     * Creates a new per-app helper.
     *
     * @param context the context used to access the package manager
     */
    public perAppHelper(Context context) {
        this.mContext = context;
    }

    /**
     * Sets the list of installed applications and rebuilds the display list.
     *
     * @param packages the list of application info objects from the package manager
     */
    public final void setPackages(List<ApplicationInfo> packages) {
        this.mPackages = packages;
        getAllApps(getSystemAppStatus());
    }

    /**
     * Returns the list of application data ready for display in the UI.
     *
     * @return list of AeroData objects containing app names and package names
     */
    public final List<AeroData> getFullPackages() {
        return this.mPerAppData;
    }

    /**
     * Returns the raw list of ApplicationInfo objects.
     *
     * @return the list of installed applications
     */
    public final List<ApplicationInfo> getPackages() {
        return this.mPackages;
    }

    /**
     * Returns whether system applications are currently being shown.
     *
     * @return true if system apps are included in the list, false otherwise
     */
    public final boolean getSystemAppStatus() {
        return this.mShowSystemApps;
    }

    /**
     * Sets whether to include system applications in the list and resets the selection state.
     *
     * @param showSystemApps true to include system apps, false to show only user apps
     */
    public final void setSystemAppStatus(boolean showSystemApps) {
        this.mShowSystemApps = showSystemApps;
        this.mIsChecked = null;
    }

    /**
     * Returns the checked state array for all applications in the list.
     *
     * @return boolean array indicating which apps are selected for monitoring
     */
    public final boolean[] getCheckedState() {
        return this.mIsChecked;
    }

    /**
     * Returns an array of package names for all applications currently selected for monitoring.
     *
     * @return array of package name strings for checked apps
     */
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

    /**
     * Updates the checked state for an application at the specified position.
     *
     * @param checkedState the new checked state
     * @param position the position in the application list
     */
    public final void setChecked(boolean checkedState, int position) {
        if (this.mIsChecked == null) {
            this.mIsChecked = new boolean[this.mPackageNames.length];
        }
        this.mIsChecked[position] = checkedState;
    }

    /**
     * Matches a list of previously selected package names against the current list
     * and marks them as checked.
     *
     * @param selectedApps array of package names to mark as selected
     */
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

    /**
     * Rebuilds the application list based on the system app filter setting. Retrieves
     * installed applications from the package manager and populates the display data.
     *
     * @param showSystemApp true to include system apps, false to show only user apps
     */
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
