package com.aero.control.helpers.PerApp.AppMonitor.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AppElementDetail implements Parcelable {
    public static final Parcelable.Creator<AppElementDetail> CREATOR = new Parcelable.Creator<AppElementDetail>() {
        @Override // android.os.Parcelable.Creator
        public AppElementDetail createFromParcel(Parcel source) {
            return new AppElementDetail(source);
        }

        @Override // android.os.Parcelable.Creator
        public AppElementDetail[] newArray(int size) {
            return new AppElementDetail[size];
        }
    };
    private String mContent;
    private String mTitle;

    public AppElementDetail(String title, String content) {
        this.mTitle = title;
        this.mContent = content;
    }

    public AppElementDetail(Parcel parcel) {
        this.mTitle = parcel.readString();
        this.mContent = parcel.readString();
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String getContent() {
        return this.mContent;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mTitle);
        parcel.writeString(this.mContent);
    }
}
