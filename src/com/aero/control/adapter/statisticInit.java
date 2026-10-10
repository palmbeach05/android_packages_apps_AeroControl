package com.aero.control.adapter;

/**
 * Data model for CPU frequency statistics. Holds a frequency value, the time spent
 * in that frequency state, and the percentage of total time, along with an optional
 * accepted index for filtering or selection purposes.
 */
public class statisticInit {
    public String mFrequency;
    public String mPercentage;
    public String mTimeInState;
    public int acceptedIndex;

    /**
     * Creates a statistic entry with no accepted index (-1).
     *
     * @param frequency the CPU frequency value
     * @param timeInState the time spent in this frequency state
     * @param percentage the percentage of total time
     */
    public statisticInit(String frequency, String timeInState, String percentage) {
        this.mFrequency = frequency;
        this.mTimeInState = timeInState;
        this.mPercentage = percentage;
        this.acceptedIndex = -1;
    }

    /**
     * Creates a statistic entry with an accepted index for filtering.
     *
     * @param frequency the CPU frequency value
     * @param timeInState the time spent in this frequency state
     * @param percentage the percentage of total time
     * @param acceptedIndex the index used for filtering or selection
     */
    public statisticInit(String frequency, String timeInState, String percentage, int acceptedIndex) {
        this.mFrequency = frequency;
        this.mTimeInState = timeInState;
        this.mPercentage = percentage;
        this.acceptedIndex = acceptedIndex;
    }
}
