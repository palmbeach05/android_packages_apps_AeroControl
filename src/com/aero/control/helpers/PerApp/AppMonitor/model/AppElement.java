package com.aero.control.helpers.PerApp.AppMonitor.model;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class AppElement implements Parcelable {
    public static final Parcelable.Creator<AppElement> CREATOR = new Parcelable.Creator<AppElement>() { // from class: com.aero.control.helpers.PerApp.AppMonitor.model.AppElement.2
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public AppElement createFromParcel(Parcel source) {
            return new AppElement(source);
        }

        /* JADX WARN: Can't rename method to resolve collision */
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

    public AppElement(String name, Drawable appDrawable) {
        this.mAppName = name;
        this.mAppDrawable = appDrawable;
        this.mAverageData = new ArrayList<>();
    }

    public AppElement(Parcel parcel) {
        this.mAppName = parcel.readString();
        this.mAverageData = parcel.createTypedArrayList(AppElementDetail.CREATOR);
        this.mRealAppName = parcel.readString();
    }

    public List<AppElementDetail> getChildData() {
        return this.mAverageData;
    }

    public String getName() {
        return this.mAppName;
    }

    public String getRealName() {
        return this.mRealAppName;
    }

    public void setRealName(String realName) {
        this.mRealAppName = realName;
    }

    public Drawable getImage() {
        return this.mAppDrawable;
    }

    public Long getUsage() {
        return this.mUsage;
    }

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
