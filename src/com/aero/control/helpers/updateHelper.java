package com.aero.control.helpers;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Helper class for backing up and restoring boot partition images. Provides device
 * whitelist checking and file copy operations for boot image backup/restore functionality.
 */
public class updateHelper {
    public static final String timeStamp = new SimpleDateFormat("ddMMyyyy").format(Calendar.getInstance().getTime());
    private static final String LOG_TAG = updateHelper.class.getName();
    private static final String[][] WHITE_LIST_DEVICES = {new String[]{"Nexus 4", FilePath.BACKUPPATH[0]}, new String[]{"Nexus 5", FilePath.BACKUPPATH[0]}, new String[]{"ASUS_T00N", FilePath.BACKUPPATH[0]}, new String[]{"XT1032", FilePath.BACKUPPATH[0]}, new String[]{"XT1033", FilePath.BACKUPPATH[0]}, new String[]{"Nexus 7", FilePath.BACKUPPATH[1]}, new String[]{"MB860", "/dev/block/platform/sdhci-tegra.3/by-num/p11"}};

    /**
     * Copies a file from source to destination. If rest is true, performs a simple copy;
     * otherwise, copies to a timestamped backup directory on external storage.
     *
     * @param original the source file
     * @param copy the destination file
     * @param rest whether to perform a simple restore copy (true) or create a timestamped backup (false)
     * @throws IOException if an I/O error occurs during the copy operation
     */
    public final void copyFile(File original, File copy, boolean rest) throws IOException {
        FileChannel input = null;
        FileChannel output = null;
        if (rest) {
            FileChannel input2 = new FileInputStream(original).getChannel();
            FileChannel output2 = new FileOutputStream(copy).getChannel();
            output2.transferFrom(input2, 0L, input2.size());
            input2.close();
            output2.close();
            return;
        }
        try {
            if (Environment.getExternalStorageState().equals("mounted")) {
                File file = new File(Environment.getExternalStorageDirectory() + File.separator + "com.aero.control" + File.separator + "backup" + File.separator + timeStamp);
                file.mkdirs();
                input = new FileInputStream(original).getChannel();
                output = new FileOutputStream(copy).getChannel();
                output.transferFrom(input, 0L, input.size());
                if (input == null || output == null) {
                    Log.e(LOG_TAG, "Could not copy files or something went wrong.");
                } else {
                    input.close();
                    output.close();
                }
            } else {
                Log.e(LOG_TAG, "No Sdcard found!");
            }
        } finally {
            if (input == null || output == null) {
                Log.e(LOG_TAG, "Could not copy files or something went wrong.");
            } else {
                input.close();
                output.close();
            }
        }
    }

    /**
     * Checks if a device model is on the whitelist for boot partition backup/restore
     * operations and returns the corresponding boot partition path.
     *
     * @param model the device model name to check
     * @return the boot partition block device path if whitelisted, null otherwise
     */
    public String isWhiteListed(String model) {
        String[][] arr$ = WHITE_LIST_DEVICES;
        for (String[] s : arr$) {
            if (s[0].equals(model)) {
                return s[1];
            }
        }
        return null;
    }
}
