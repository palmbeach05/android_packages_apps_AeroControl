package com.aero.control.helpers.PerApp.AppMonitor.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model representing a single metric detail line for a monitored app. Contains a
 * title (e.g., "Average CPU") and content (e.g., "1500 MHz"). Used as child items
 * in the expandable app monitoring list.
 */
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

    /**
     * Creates a detail item with a title and content.
     *
     * @param title the metric title
     * @param content the metric value or content
     */
    public AppElementDetail(String title, String content) {
        this.mTitle = title;
        this.mContent = content;
    }

    /**
     * Reconstructs a detail item from a parcel.
     *
     * @param parcel the parcel containing the serialized data
     */
    public AppElementDetail(Parcel parcel) {
        this.mTitle = parcel.readString();
        this.mContent = parcel.readString();
    }

    /**
     * Returns the metric title.
     *
     * @return the title string
     */
    public String getTitle() {
        return this.mTitle;
    }

    /**
     * Returns the metric content or value.
     *
     * @return the content string
     */
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
