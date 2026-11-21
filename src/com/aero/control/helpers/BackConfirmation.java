package com.aero.control.helpers;

/** Shared Back confirmation for the main and Settings activities. */
public final class BackConfirmation {
    public static final String STATE_DEADLINE = "BackConfirmationDeadline";
    private static final int CLOSE_CONFIRMATION_TIMEOUT_MS = 3500;
    private static long sDeadline;
    private static boolean sInitialized;

    private BackConfirmation() {
    }

    /** Restore once after process recreation; older activity state must not overwrite a handoff. */
    public static void restore(long deadline) {
        if (!sInitialized) {
            sDeadline = deadline;
            sInitialized = true;
        }
    }

    /** All times use SystemClock.elapsedRealtime(), including saved deadlines. */
    public static void start(long now) {
        sInitialized = true;
        sDeadline = now + CLOSE_CONFIRMATION_TIMEOUT_MS;
    }

    public static boolean isPending(long now) {
        return now < sDeadline && sDeadline - now <= CLOSE_CONFIRMATION_TIMEOUT_MS;
    }

    public static long getDeadline() {
        return sDeadline;
    }

    public static void clear() {
        sInitialized = true;
        sDeadline = 0;
    }
}
