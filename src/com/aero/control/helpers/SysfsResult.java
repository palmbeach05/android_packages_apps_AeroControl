package com.aero.control.helpers;

/** Explicit success/failure value returned by hardware operations. */
public final class SysfsResult<T> {
    private final T value;
    private final String error;

    private SysfsResult(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <T> SysfsResult<T> success(T value) {
        return new SysfsResult<>(value, null);
    }

    public static <T> SysfsResult<T> failure(String error) {
        return new SysfsResult<>(null, error == null ? "Unknown sysfs error" : error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public T getValue() {
        if (!isSuccess()) {
            throw new IllegalStateException(error);
        }
        return value;
    }

    public String getError() {
        return error;
    }
}
