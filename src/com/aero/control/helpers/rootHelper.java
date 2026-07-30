package com.aero.control.helpers;

/* JADX INFO: loaded from: classes.dex */
public class rootHelper {
    private static final int BUFF_LEN = 1024;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = rootHelper.class.getName();
    private static final byte[] buffer = new byte[1024];

    public boolean isDeviceRooted() {
        return checkRootMethod();
    }

    private boolean checkRootMethod() {
        String output = suCheckRootMethod();
        if (output.equals(NO_DATA_FOUND)) {
            return false;
        }
        return output.contains("uid=0");
    }

    /* JADX WARN: Code restructure failed: missing block: B:13:0x0050, code lost:
    
        r4.writeBytes("exit\n");
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0055, code lost:
    
        if (r3 == null) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0057, code lost:
    
        r3.waitFor();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.lang.String suCheckRootMethod() {
        /*
            r10 = this;
            r3 = 0
            java.lang.Runtime r6 = java.lang.Runtime.getRuntime()     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.String r7 = "su"
            java.lang.Process r3 = r6.exec(r7)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.io.DataOutputStream r4 = new java.io.DataOutputStream     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.io.OutputStream r6 = r3.getOutputStream()     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            r4.<init>(r6)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.String r6 = "id\n"
            r4.writeBytes(r6)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.io.InputStream r5 = r3.getInputStream()     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.String r1 = ""
        L1f:
            byte[] r6 = com.aero.control.helpers.rootHelper.buffer     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            int r2 = r5.read(r6)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            r6 = -1
            if (r2 != r6) goto L33
            java.lang.String r1 = "Unavailable"
            if (r3 == 0) goto L32
            r3.waitFor()     // Catch: java.lang.InterruptedException -> L7b
        L2f:
            r3.destroy()
        L32:
            return r1
        L33:
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            r6.<init>()     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.StringBuilder r6 = r6.append(r1)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.String r7 = new java.lang.String     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            byte[] r8 = com.aero.control.helpers.rootHelper.buffer     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            r9 = 0
            r7.<init>(r8, r9, r2)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.StringBuilder r6 = r6.append(r7)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            java.lang.String r1 = r6.toString()     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            r6 = 1024(0x400, float:1.435E-42)
            if (r2 >= r6) goto L1f
            java.lang.String r6 = "exit\n"
            r4.writeBytes(r6)     // Catch: java.io.IOException -> L5e java.lang.Throwable -> L71
            if (r3 == 0) goto L32
            r3.waitFor()     // Catch: java.lang.InterruptedException -> L7d
        L5a:
            r3.destroy()
            goto L32
        L5e:
            r0 = move-exception
            java.lang.String r6 = com.aero.control.helpers.rootHelper.LOG_TAG     // Catch: java.lang.Throwable -> L71
            java.lang.String r7 = "Do you even root, bro? :/"
            android.util.Log.e(r6, r7, r0)     // Catch: java.lang.Throwable -> L71
            if (r3 == 0) goto L6e
            r3.waitFor()     // Catch: java.lang.InterruptedException -> L7f
        L6b:
            r3.destroy()
        L6e:
            java.lang.String r1 = "Unavailable"
            goto L32
        L71:
            r6 = move-exception
            if (r3 == 0) goto L7a
            r3.waitFor()     // Catch: java.lang.InterruptedException -> L81
        L77:
            r3.destroy()
        L7a:
            throw r6
        L7b:
            r6 = move-exception
            goto L2f
        L7d:
            r6 = move-exception
            goto L5a
        L7f:
            r6 = move-exception
            goto L6b
        L81:
            r7 = move-exception
            goto L77
        */
        throw new UnsupportedOperationException("Method not decompiled: com.aero.control.helpers.rootHelper.suCheckRootMethod():java.lang.String");
    }
}
