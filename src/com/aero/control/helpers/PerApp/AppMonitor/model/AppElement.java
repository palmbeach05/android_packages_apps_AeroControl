package com.aero.control.helpers.PerApp.AppMonitor.model;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a monitored application in the AppMonitor UI. Contains the app
 * name, icon drawable, usage time, and a list of detailed metrics (AppElementDetail)
 * for display in expandable list views. Implements Parcelable for passing between activities.
 */
public class AppElement implements Parcelable {
    public static final Parcelable.Creator<AppElement> CREATOR = new Parcelable.Creator<AppElement>() { // from class: com.aero.control.helpers.PerApp.AppMonitor.model.AppElement.2
        @Override // android.os.Parcelable.Creator
        public AppElement createFromParcel(Parcel source) {
            return new AppElement(source);
        }

        @Override // android.os.Parcelable.Creator
        public AppElement[] newArray(int size) {
            return new AppElement[size];
        }
    };
    private Drawable mAppDrawable;
    private String mAppName;
    private ArrayList<AppElementDetail> mAverageData;
    private String mRealAppName;
    private Long mUsage;

    /**
     * Creates an app element with the specified name and icon.
     *
     * @param name the display name of the app
     * @param appDrawable the app icon drawable
     */
    public AppElement(String name, Drawable appDrawable) {
        this.mAppName = name;
        this.mAppDrawable = appDrawable;
        this.mAverageData = new ArrayList<>();
    }

    /**
     * Reconstructs an app element from a parcel.
     *
     * @param parcel the parcel containing the serialized data
     */
    public AppElement(Parcel parcel) {
        this.mAppName = parcel.readString();
        this.mAverageData = parcel.createTypedArrayList(AppElementDetail.CREATOR);
        this.mRealAppName = parcel.readString();
    }

    /**
     * Returns the list of detail items for this app.
     *
     * @return the list of AppElementDetail objects
     */
    public List<AppElementDetail> getChildData() {
        return this.mAverageData;
    }

    /**
     * Returns the display name of the app.
     *
     * @return the app display name
     */
    public String getName() {
        return this.mAppName;
    }

    /**
     * Returns the real package name of the app.
     *
     * @return the package name
     */
    public String getRealName() {
        return this.mRealAppName;
    }

    /**
     * Sets the real package name of the app.
     *
     * @param realName the package name
     */
    public void setRealName(String realName) {
        this.mRealAppName = realName;
    }

    /**
     * Returns the app icon drawable.
     *
     * @return the app icon
     */
    public Drawable getImage() {
        return this.mAppDrawable;
    }

    /**
     * Returns the total usage time for this app.
     *
     * @return the usage time in milliseconds
     */
    public Long getUsage() {
        return this.mUsage;
    }

    /**
     * Sets the total usage time for this app.
     *
     * @param usage the usage time in milliseconds
     */
    public void setUsage(Long usage) {
        this.mUsage = usage;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mAppName);
        parcel.writeTypedList(this.mAverageData);
        parcel.writeString(this.mRealAppName);
    }
}
