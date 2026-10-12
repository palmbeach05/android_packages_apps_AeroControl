package com.aero.control.helpers;

/** Outcome of a privileged operation, including a diagnostic for user-facing callers. */
public final class OperationResult {
    public enum Status {
        OK,
        FAILED,
        TIMED_OUT,
        VERIFICATION_MISMATCH,
        PARTIAL
    }

    private final Status status;
    private final String message;

    private OperationResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static OperationResult ok() {
        return new OperationResult(Status.OK, null);
    }

    public static OperationResult failed(String message) {
        return new OperationResult(Status.FAILED, message);
    }

    public static OperationResult timedOut(String message) {
        return new OperationResult(Status.TIMED_OUT, message);
    }

    public static OperationResult verificationMismatch(String message) {
        return new OperationResult(Status.VERIFICATION_MISMATCH, message);
    }

    public static OperationResult partial(String message) {
        return new OperationResult(Status.PARTIAL, message);
    }

    public boolean isSuccess() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
