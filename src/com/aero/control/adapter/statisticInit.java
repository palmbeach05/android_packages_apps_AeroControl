package com.aero.control.adapter;

/* JADX INFO: loaded from: classes.dex */
public class statisticInit {
    public String mFrequency;
    public String mPercentage;
    public String mTimeInState;
    public int acceptedIndex;

    public statisticInit(String frequency, String timeInState, String percentage) {
        this.mFrequency = frequency;
        this.mTimeInState = timeInState;
        this.mPercentage = percentage;
        this.acceptedIndex = -1;
    }

    public statisticInit(String frequency, String timeInState, String percentage, int acceptedIndex) {
        this.mFrequency = frequency;
        this.mTimeInState = timeInState;
        this.mPercentage = percentage;
        this.acceptedIndex = acceptedIndex;
    }
}
