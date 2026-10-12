package com.aero.control.helpers;

import android.util.Log;

/** Typed, component-scoped facade for Android logging. */
public final class AeroLog {
	private static final int MAX_TAG_LENGTH = 23;
	private static final String TAG_PREFIX = "Aero";

	private final String tag;

	private AeroLog(String tag) {
		this.tag = tag;
	}

	public static AeroLog forClass(Class<?> componentClass) {
		if (componentClass == null) {
			throw new IllegalArgumentException("componentClass must not be null");
		}
		return forTag(componentClass.getSimpleName());
	}

	public static AeroLog forTag(String component) {
		if (component == null || component.length() == 0) {
			throw new IllegalArgumentException("component must not be empty");
		}
		int separator = component.lastIndexOf('.');
		String simpleName = separator < 0 ? component : component.substring(separator + 1);
		String logTag = TAG_PREFIX + simpleName;
		if (logTag.length() > MAX_TAG_LENGTH) {
			logTag = logTag.substring(0, MAX_TAG_LENGTH);
		}
		return new AeroLog(logTag);
	}

	public void verbose(String message) {
		Log.v(this.tag, message);
	}

	public void debug(String message) {
		Log.d(this.tag, message);
	}

	public void info(String message) {
		Log.i(this.tag, message);
	}

	public void warn(String message) {
		Log.w(this.tag, message);
	}

	public void warn(String message, Throwable throwable) {
		Log.w(this.tag, message, throwable);
	}

	public void error(String message) {
		Log.e(this.tag, message);
	}

	public void error(String message, Throwable throwable) {
		Log.e(this.tag, message, throwable);
	}
}
