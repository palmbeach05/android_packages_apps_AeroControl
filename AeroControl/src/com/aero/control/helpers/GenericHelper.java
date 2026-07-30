package com.aero.control.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public class GenericHelper {
    private static final int BYTE = 1024;
    private static final int DEFAULT_DELAY = 200;

    public final int getDefaultDelay() {
        return DEFAULT_DELAY;
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
