package com.aero.control.helpers.PerApp.AppMonitor.model;

/* JADX INFO: loaded from: classes.dex */
public class AppElementDetail {
    private String mContent;
    private String mTitle;

    public AppElementDetail(String title, String content) {
        this.mTitle = title;
        this.mContent = content;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String getContent() {
        return this.mContent;
    }
}
