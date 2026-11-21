package com.aero.control.helpers;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** API-23-compatible gate for user actions that write shared external storage. */
public final class StoragePermission {
    private static final int MARSHMALLOW_API = 23;
    public static final int REQUEST_CODE = 4101;

    private StoragePermission() {
    }

    public static boolean isGranted(Context context) {
        if (Build.VERSION.SDK_INT < MARSHMALLOW_API) {
            return true;
        }
        try {
            Method checkSelfPermission = Context.class.getMethod(
                    "checkSelfPermission", String.class);
            Integer result = (Integer) checkSelfPermission.invoke(
                    context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result.intValue() == PackageManager.PERMISSION_GRANTED;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
    }

    public static void request(Fragment fragment) {
        if (Build.VERSION.SDK_INT >= MARSHMALLOW_API) {
            try {
                Method requestPermissions = Fragment.class.getMethod(
                        "requestPermissions", String[].class, int.class);
                requestPermissions.invoke(fragment,
                        new Object[] {new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                Integer.valueOf(REQUEST_CODE)});
            } catch (NoSuchMethodException e) {
                // The method is available on API 23+, but absent from older compile APIs.
            } catch (IllegalAccessException e) {
                // Permission request cannot be issued from this fragment instance.
            } catch (InvocationTargetException e) {
                // Permission request failed; the caller remains blocked from the operation.
            }
        }
    }
}
