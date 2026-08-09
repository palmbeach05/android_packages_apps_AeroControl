package com.aero.control.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public class GenericHelper {
    private static final int BYTE = 1024;
    private static final int DEFAULT_DELAY = 200;
    private static final Pattern SAFE_SHELL_VALUE = Pattern.compile("[\\w.,\\- ]+");

    public final int getDefaultDelay() {
        return DEFAULT_DELAY;
    }

    /**
     * Whether a value read from preferences is safe to interpolate directly into a
     * privileged shell command (e.g. "echo <value> > <sysfs path>"). Only allows
     * letters, digits, '.', ',', '-', '_' and spaces so that shell metacharacters
     * (';', '|', '&', '$', backticks, quotes, redirections, newlines, etc.) present
     * in a corrupted or tampered preference value cannot be executed, and malformed
     * values cannot be written into kernel sysfs nodes.
     */
    public final boolean isSafeShellValue(String value) {
        return value != null && SAFE_SHELL_VALUE.matcher(value).matches();
    }

    public final boolean doesExist(String s) {
        if (s == null) {
            return false;
        }
        return new File(s).exists();
    }

    public final File getNewFile(String s) {
        return new File(s);
    }

    public void copyFile(File source, File destination) throws IOException {
        InputStream input = new FileInputStream(source);
        OutputStream output = new FileOutputStream(destination);
        byte[] buf = new byte[1024];
        while (true) {
            int len = input.read(buf);
            if (len > 0) {
                output.write(buf, 0, len);
            } else {
                input.close();
                output.close();
                return;
            }
        }
    }
}
