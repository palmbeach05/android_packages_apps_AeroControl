package com.aero.control.helpers;

import android.os.SystemClock;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generic (non device-specific) file reads and writes against sysfs/procfs
 * nodes, with a root-shell fallback for paths the app cannot read directly.
 * Depends on a {@link RootShellSession} for the fallback path but knows
 * nothing about kernel-version formatting or any Tegra-I2C/hwmon-specific
 * layout — those live in their own device-specific classes.
 */
final class SysfsReader {
    static final String NO_DATA_FOUND = "Unavailable";
    private static final String LOG_TAG = SysfsReader.class.getName();

    private final RootShellSession session;

    SysfsReader(RootShellSession session) {
        this.session = session;
    }

    /**
     * Reads the first line from a file, falling back to root cat if direct read fails.
     *
     * @param s the file path to read
     * @return the first line of the file, or "Unavailable" if the file cannot be read
     */
    String getInfo(String s) {
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
            synchronized (session) {
                session.openShell();
                session.addCommand("ls -l " + s);
                String tmp = session.getRootResult();
                if (tmp != null && tmp.length() > 10 && !tmp.substring(0, 10).equals("--w-------")) {
                    session.addCommand("cat " + s);
                    String catResult = session.getRootResult();
                    if (catResult != null) {
                        info = catResult;
                    }
                }
                if (info.equals(NO_DATA_FOUND)) {
                    Log.e(LOG_TAG, "IO Exception when trying to get information.", e);
                }
                return info;
            }
        }
    }

    /**
     * Reads a file's first line without fallback to root. Faster than getInfo when
     * the file is known to be readable without root.
     *
     * @param path the file path to read
     * @return the first line of the file
     */
    String getFastInfo(String path) {
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

    /**
     * Reads all lines from a file. Optionally prepends deep sleep time as the first element.
     *
     * @param s the file path to read
     * @param deepsleep if true, prepends the deep sleep time in centiseconds
     * @return array of lines from the file, or null if reading fails
     */
    String[] getInfo(String s, boolean deepsleep) {
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
                return al.toArray(new String[0]);
            } catch (Throwable th) {
                reader.close();
                throw th;
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "IO Exception when trying to get information.", e);
            return null;
        }
    }

    /**
     * Lists directory contents. If flag is true, returns only files; otherwise returns only directories.
     *
     * @param s the directory path
     * @param flag if true, list files; if false, list directories
     * @return array of names, or null if the directory does not exist
     */
    String[] getDirInfo(String s, boolean flag) {
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
        return new File(s).list(new FilenameFilter() {
            @Override // java.io.FilenameFilter
            public boolean accept(File file2, String s2) {
                return new File(file2, s2).isDirectory();
            }
        });
    }

    /**
     * Parses a string into an array by splitting on spaces, optionally removing brackets
     * and converting values to MHz format.
     *
     * @param s the input string to parse
     * @param flag if 1, convert values to MHz; otherwise keep raw strings
     * @param flag_io if 1, remove brackets before splitting; otherwise split as-is
     * @return the parsed array of strings
     */
    private String[] buildArray(String s, int flag, int flag_io) {
        return buildArray(s, flag == 1, flag_io == 1);
    }

    private String[] buildArray(String s, ReadMode mode) {
        return buildArray(s, mode.convertsToMhz(), mode.stripsBrackets());
    }

    private String[] buildArray(String s, boolean convertToMhz, boolean stripBrackets) {
        String[] completeString = new String[0];
        if (s.charAt(s.length() - 1) == '\n') {
            s = s.replace(Character.toString('\n'), "");
        }
        if (stripBrackets) {
            completeString = s.replace("[", "").replace("]", "").split(" ");
        } else {
            completeString = s.split(" ");
        }
        String[] output = new String[completeString.length];
        output[0] = NO_DATA_FOUND;
        for (int i = 0; i < output.length; i++) {
            if (convertToMhz) {
                output[i] = FrequencyFormat.toMHz(completeString[i]);
            } else {
                output[i] = completeString[i];
            }
        }
        return output;
    }

    /**
     * Reads a file and returns its contents as a space-separated array, optionally converting
     * to MHz and optionally removing brackets.
     *
     * @param s the file path to read
     * @param flag if 1, convert values to MHz; otherwise return raw strings
     * @param flag_io if 1, remove brackets; otherwise keep raw format
     * @return array of parsed values, or {"Unavailable"} if reading fails
     */
    String[] getInfoArray(String s, int flag, int flag_io) {
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
            synchronized (session) {
                session.openShell();
                session.addCommand("ls -l " + " " + s);
                String result = session.getRootResult();
                if (result != null && result.length() > 10 && !result.substring(0, 10).equals("--w-------")) {
                    session.addCommand("cat" + " " + s);
                    String tmp = session.getRootResult();
                    output = buildArray(tmp, flag, flag_io);
                }
                if (output[0].equals(NO_DATA_FOUND)) {
                    Log.e(LOG_TAG, "IO Exception when trying to get information.", e);
                }
                return output;
            }
        }
    }

    String[] getInfoArray(String s, ReadMode mode) {
        return getInfoArray(s, mode.convertsToMhz() ? 1 : 0,
                mode.stripsBrackets() ? 1 : 0);
    }

    /**
     * Extracts the substring between the first '[' and last ']' in a string.
     *
     * @param s the input string
     * @return the substring between brackets, or "Unavailable" if brackets not found
     */
    String getInfoString(String s) {
        int open = s.indexOf("[");
        int close = s.lastIndexOf("]");
        if (open < 0 || close < 0) {
            return NO_DATA_FOUND;
        }
        return s.substring(open + 1, close);
    }

    /** Reads a named-field memory snapshot from /proc/meminfo. */
    MemorySnapshot getMemory(String path) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path), 8192);
            try {
                return MemInfoParser.parse(reader);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Yep, i can't read your memory stats :( .", e);
            return MemorySnapshot.unavailable();
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Unable to access memory statistics.", e);
            return MemorySnapshot.unavailable();
        }
    }
}
