package com.aero.control.helpers;

import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* JADX INFO: loaded from: classes.dex */
public final class shellHelper {
    private static final int BUFF_LEN = 8192;
    private static final int MAX_RESULT_LEN = 65536;
    private static final String NO_DATA_FOUND = "Unavailable";
    private static shellHelper mShellHelper;
    private List<String> mCommands;
    private static final String LOG_TAG = shellHelper.class.getName();
    private ShellWorkqueue shWork = new ShellWorkqueue();
    private Process mProcess = null;
    private DataOutputStream mShellOutput = null;
    private BufferedReader mOutput = null;
    private boolean mShellLoaded = false;

    private shellHelper() {
        Runnable run = new Runnable() { // from class: com.aero.control.helpers.shellHelper.1
            @Override // java.lang.Runnable
            public void run() {
                shellHelper.this.openShell();
            }
        };
        Thread worker = new Thread(run);
        worker.start();
    }

    public static synchronized shellHelper instance() {
        if (mShellHelper == null) {
            mShellHelper = new shellHelper();
        }
        return mShellHelper;
    }

    public static synchronized shellHelper forceInstance() {
        mShellHelper = new shellHelper();
        return mShellHelper;
    }

    private synchronized void addCommands(String[] commands) {
        for (String cmd : commands) {
            if (cmd != null) {
                this.mCommands.add(cmd);
            }
        }
    }

    public synchronized void addCommand(String cmd) {
        this.mCommands.add(cmd);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void openShell() {
        if (this.mCommands == null) {
            this.mCommands = new ArrayList();
        }
        try {
            if (this.mProcess == null) {
                this.mProcess = Runtime.getRuntime().exec("su");
            }
            if (this.mShellOutput == null) {
                this.mShellOutput = new DataOutputStream(this.mProcess.getOutputStream());
            }
            if (this.mOutput == null) {
                this.mOutput = new BufferedReader(new InputStreamReader(this.mProcess.getInputStream()));
            }
            this.mShellLoaded = true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "We were not able to create a shell!", e);
            this.mShellLoaded = false;
        }
    }

    private synchronized void runCommands() {
        openShell();
        if (this.mShellLoaded) {
            List<String> commands = Collections.synchronizedList(this.mCommands);
            try {
                for (String cmd : commands) {
                    this.mShellOutput.write((cmd + "\n").getBytes("UTF-8"));
                    this.mShellOutput.flush();
                }
                try {
                    this.mShellOutput.flush();
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                Log.e(LOG_TAG, "Something interrupted our operations...", e2);
            }
            this.mCommands.clear();
        } else {
            this.mCommands.clear();
        }
    }

    private String getRootResult() {
        int read;
        List<String> commands = Collections.synchronizedList(this.mCommands);
        char[] buf = new char[8192];
        StringBuilder response = new StringBuilder();
        try {
            if (this.mShellLoaded) {
                for (String cmd : commands) {
                    this.mShellOutput.write((cmd + "\n").getBytes("UTF-8"));
                    do {
                        read = this.mOutput.read(buf);
                        if (read == -1) {
                            return null;
                        }
                        int remaining = MAX_RESULT_LEN - response.length();
                        if (remaining > 0) {
                            response.append(buf, 0, Math.min(read, remaining));
                        }
                    } while (read >= 8192);
                    this.mShellOutput.flush();
                }
                try {
                    this.mShellOutput.flush();
                } catch (IOException e) {
                }
            }
            return response.toString();
        } catch (IOException e2) {
            Log.e(LOG_TAG, "Something interrupted our operations...", e2);
            return null;
        } finally {
            this.mCommands.clear();
        }
    }

    private class ShellWorkqueue {
        private ArrayList<String> mWorkItems;

        private ShellWorkqueue() {
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void addToWork(String work) {
            if (this.mWorkItems == null) {
                initWork();
            }
            this.mWorkItems.add(work);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public String[] execWork() {
            return (String[]) this.mWorkItems.toArray(new String[0]);
        }

        private void initWork() {
            this.mWorkItems = new ArrayList<>();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void flushWork() {
            if (this.mWorkItems != null) {
                this.mWorkItems.clear();
                this.mWorkItems = null;
            }
        }
    }

    public void queueWork(String work) {
        this.shWork.addToWork(work);
    }

    public void execWork() {
        this.shWork.addToWork("echo ");
        setRootInfo(this.shWork.execWork());
    }

    public void flushWork() {
        this.shWork.flushWork();
    }

    public final String getKernel() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 8192);
            try {
                String procVersionStr = reader.readLine();
                reader.close();
                Pattern p = Pattern.compile("\\w+\\s+\\w+\\s+([^\\s]+)\\s+\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+([^\\s]+)\\s+(?:PREEMPT\\s+)?(.+)");
                Matcher m = p.matcher(procVersionStr);
                if (!m.matches()) {
                    Log.e(LOG_TAG, "Regex did not match on /proc/version: " + procVersionStr);
                    return NO_DATA_FOUND;
                }
                if (m.groupCount() < 4) {
                    Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
                    return NO_DATA_FOUND;
                }
                return m.group(1) + "\n" + m.group(2) + " " + m.group(3) + "\n" + m.group(4);
            } catch (Throwable th) {
                reader.close();
                throw th;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO Exception when getting kernel version for Device Info screen", e);
            return NO_DATA_FOUND;
        }
    }

    public final String getInfo(String s) {
        String info = NO_DATA_FOUND;
        if (s == null || !new File(s).exists()) {
            return NO_DATA_FOUND;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(s), 8192);
            try {
                info = reader.readLine();
                if (info == null) {
                    info = NO_DATA_FOUND;
                }
                return info;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            openShell();
            addCommand("ls -l " + s);
            String tmp = getRootResult();
            if (tmp != null && tmp.length() > 10 && !tmp.substring(0, 10).equals("--w-------")) {
                addCommand("cat " + s);
                info = getRootResult();
            }
            if (info.equals(NO_DATA_FOUND)) {
                Log.e(LOG_TAG, "IO Exception when trying to get information.", e);
            }
            return info;
        }
    }

    public final String getFastInfo(String path) {
        try {
            FileInputStream fis = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String tmp = br.readLine();
            return tmp;
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO Exception when trying to get information. Fallback to getInfo()", e);
            String tmp2 = getInfo(path);
            return tmp2;
        }
    }

    public final String[] getInfo(String s, boolean deepsleep) {
        ArrayList<String> al = new ArrayList<>();
        if (deepsleep) {
            long sleepTime = (SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()) / 10;
            al.add(Long.toString(sleepTime));
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(s), 8192);
            try {
                for (String info = reader.readLine(); info != null; info = reader.readLine()) {
                    al.add(info);
                }
                reader.close();
                return (String[]) al.toArray(new String[0]);
            } catch (Throwable th) {
                reader.close();
                throw th;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO Exception when trying to get information.", e);
            return null;
        }
    }

    public final String[] getDirInfo(String s, boolean flag) {
        if (!new File(s).exists()) {
            return null;
        }
        if (flag) {
            List<String> results = new ArrayList<>();
            File[] files = new File(s).listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    results.add(file.getName());
                }
            }
            String[] result = new String[results.size()];
            for (int i = 0; i < results.size(); i++) {
                result[i] = results.get(i);
            }
            Arrays.sort(result);
            return result;
        }
        return new File(s).list(new FilenameFilter() { // from class: com.aero.control.helpers.shellHelper.2
            @Override // java.io.FilenameFilter
            public boolean accept(File file2, String s2) {
                return new File(file2, s2).isDirectory();
            }
        });
    }

    private String[] buildArray(String s, int flag, int flag_io) {
        String[] completeString = new String[0];
        if (s.charAt(s.length() - 1) == '\n') {
            s = s.replace(Character.toString('\n'), "");
        }
        if (flag_io == 1) {
            completeString = s.replace("[", "").replace("]", "").split(" ");
        } else if (flag_io == 0) {
            completeString = s.split(" ");
        }
        String[] output = new String[completeString.length];
        output[0] = NO_DATA_FOUND;
        for (int i = 0; i < output.length; i++) {
            if (flag == 1) {
                output[i] = toMHz(completeString[i]);
            } else {
                output[i] = completeString[i];
            }
        }
        return output;
    }

    public final String[] getInfoArray(String s, int flag, int flag_io) {
        String[] output = {NO_DATA_FOUND};
        try {
            BufferedReader reader = new BufferedReader(new FileReader(s), 8192);
            try {
                output = buildArray(reader.readLine(), flag, flag_io);
                return output;
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            openShell();
            String result = getRootInfo("ls -l", s);
            if (result != null && result.length() > 10 && !result.substring(0, 10).equals("--w-------")) {
                String tmp = getRootInfo("cat", s);
                output = buildArray(tmp, flag, flag_io);
            }
            if (output[0].equals(NO_DATA_FOUND)) {
                Log.e(LOG_TAG, "IO Exception when trying to get information.", e);
            }
            return output;
        }
    }

    public final String getInfoString(String s) {
        int open = s.indexOf("[");
        int close = s.lastIndexOf("]");
        if (open < 0 || close < 0) {
            return NO_DATA_FOUND;
        }
        String finalString = s.substring(open + 1, close);
        return finalString;
    }

    public final String toMHz(String mhzString) {
        String str;
        if (mhzString.equals(NO_DATA_FOUND) || mhzString.equals("Unavaila")) {
            return NO_DATA_FOUND;
        }
        try {
            if (mhzString.length() < 8) {
                str = (Integer.valueOf(mhzString).intValue() / 1000) + " MHz";
            } else {
                str = (Integer.valueOf(mhzString).intValue() / 1000000) + " MHz";
            }
            return str;
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Tried to add something to a non existing string.", e);
            return NO_DATA_FOUND;
        }
    }

    public final String getMemory(String s) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(s), 8192);
            String totalMemory = reader.readLine();
            String totalFreeMemory = reader.readLine();
            if (totalMemory != null && totalFreeMemory != null) {
                String[] parts = totalMemory.split("\\s+");
                if (parts.length == 3) {
                    totalMemory = (Long.parseLong(parts[1]) / 1024) + " MB";
                }
                String[] parts2 = totalFreeMemory.split("\\s+");
                if (parts2.length == 3) {
                    totalFreeMemory = (Long.parseLong(parts2[1]) / 1024) + " MB";
                }
            }
            return totalFreeMemory + " / " + totalMemory;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Yep, i can't read your memory stats :( .", e);
            return NO_DATA_FOUND;
        }
    }

    public final synchronized void setRootInfo(String command, String content) {
        String tmp;
        String tmp2 = command.substring(command.length() - 1);
        if (tmp2.matches("^\\s*$")) {
            tmp = command.substring(0, command.length() - 1);
        } else {
            tmp = command;
        }
        String[] commands = {"chmod 0666 " + content, "echo \"" + tmp + "\" > " + content};
        addCommands(commands);
        runCommands();
    }

    public final void setRootInfo(String[] array) {
        addCommands(array);
        runCommands();
    }

    public final void remountSystem() {
        addCommand("mount -o remount,rw -t ext3 /dev/block/mmcblk1p21 /system");
        runCommands();
    }

    public final String getRootInfo(String command, String parameter) {
        addCommand(command + " " + parameter);
        String ret = getRootResult();
        if (ret == null) {
            return NO_DATA_FOUND;
        }
        return ret;
    }

    public final String[] getRootArray(String command, String split) {
        ArrayList<String> temp = new ArrayList<>();
        addCommand(command);
        String ret = getRootResult();
        String[] arr$ = ret.split(split);
        for (String a : arr$) {
            temp.add(a);
        }
        return (String[]) temp.toArray(new String[0]);
    }

    public String getLegacyRootInfo(String command, String parameter) {
        Process process = null;
        DataOutputStream os = null;
        InputStream is = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + " " + parameter + "\n");
            os.flush();
            is = process.getInputStream();
            byte[] localBuffer = new byte[BUFF_LEN];
            String result = "";
            while (true) {
                int read = is.read(localBuffer);
                if (read == -1) {
                    result = NO_DATA_FOUND;
                    break;
                }
                result = result + new String(localBuffer, 0, read);
                if (read < BUFF_LEN) {
                    os.writeBytes("exit\n");
                    os.flush();
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Do you even root, bro? :/", e);
            return NO_DATA_FOUND;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
            if (process != null) {
                process.destroy();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public final boolean setOverclockAddress() {
        if (!new File("/proc/overclock/omap2_clk_init_cpufreq_table_addr").exists() || !new File("/proc/overclock/cpufreq_stats_update_addr").exists()) {
            return false;
        }
        String omap_result = getLegacyRootInfo("busybox egrep \"omap2_clk_init_cpufreq_table$\"", "/proc/kallsyms");
        String cpufreq_result = getLegacyRootInfo("busybox egrep \"cpufreq_stats_update$\"", "/proc/kallsyms");
        if (omap_result.length() < 8 || cpufreq_result.length() < 8) {
            return false;
        }
        String omap_address = omap_result.substring(0, 8);
        String cpufreq_address = cpufreq_result.substring(0, 8);
        String[] commands = {"echo 0x" + omap_address + " > /proc/overclock/omap2_clk_init_cpufreq_table_addr", "echo 0x" + cpufreq_address + " > /proc/overclock/cpufreq_stats_update_addr"};
        setRootInfo(commands);
        return true;
    }
}
