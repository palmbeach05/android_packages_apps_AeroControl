package com.aero.control.helpers;

import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Thin, singleton facade over a persistent root shell session. Owns the
 * shared {@link RootShellSession} (the {@code su} process, its command
 * queue, and read/write plumbing) and the small work-queue API built on top
 * of it, and forwards every other concern to a dedicated class:
 * <ul>
 *   <li>{@link SysfsReader} — generic file reads/writes with root fallback</li>
 *   <li>{@link FrequencyFormat} — kHz/Hz -&gt; "N MHz" formatting</li>
 *   <li>{@link KernelVersionParser} — {@code /proc/version} parsing</li>
 *   <li>{@link HwmonInfo} / {@link TegraI2cInfo} — device-specific sysfs
 *       enumeration allowlists</li>
 *   <li>{@link OverclockLegacy} — legacy, device-family-specific overclock
 *       address patching</li>
 * </ul>
 * Existing callers keep using {@code shellHelper.instance()} and its public
 * methods unchanged; only the internals are split by responsibility.
 */
public final class shellHelper {
    private static final String NO_DATA_FOUND = "Unavailable";
    private static shellHelper mShellHelper;

    private final RootShellSession session = new RootShellSession();
    private final SysfsReader sysfsReader = new SysfsReader(session);
    private final SysfsRepository sysfsRepository = new SysfsRepository(session);
    private final HardwareGateway hardwareGateway = new HardwareGateway(session);
    private final HwmonInfo hwmonInfo = new HwmonInfo(session);
    private final TegraI2cInfo tegraI2cInfo = new TegraI2cInfo(session);
    private final OverclockLegacy overclockLegacy = new OverclockLegacy(session);
    private final ShellWorkqueue shWork = new ShellWorkqueue();

    /**
     * Private constructor. Opens the root shell in a background thread.
     */
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

    /**
     * Returns the singleton shellHelper instance, creating it if necessary.
     *
     * @return the shared shellHelper instance
     */
    public static synchronized shellHelper instance() {
        if (mShellHelper == null) {
            mShellHelper = new shellHelper();
        }
        return mShellHelper;
    }

    /**
     * Creates and returns a new shellHelper instance, discarding any existing one.
     * Used when multiple independent shell sessions are needed.
     *
     * @return a new shellHelper instance
     */
    public static synchronized shellHelper forceInstance() {
        mShellHelper = new shellHelper();
        return mShellHelper;
    }

    /**
     * Opens a root shell session if not already open.
     */
    public void openShell() {
        session.openShell();
    }

    /**
     * Closes the root shell session and cleans up resources.
     */
    public void closeShell() {
        session.closeShell();
    }

    /**
     * Internal work queue for batching shell commands.
     */
    private static final class ShellWorkqueue {
        private ArrayList<String> mWorkItems;

        private ShellWorkqueue() {
        }

        /**
         * Adds a command to the work queue, initializing the queue if necessary.
         *
         * @param work the command string to add
         */
        void addToWork(String work) {
            if (this.mWorkItems == null) {
                initWork();
            }
            this.mWorkItems.add(work);
        }

        /**
         * Returns all queued commands as an array.
         *
         * @return array of queued command strings
         */
        String[] execWork() {
            return this.mWorkItems.toArray(new String[0]);
        }

        /**
         * Initializes an empty work items list.
         */
        private void initWork() {
            this.mWorkItems = new ArrayList<>();
        }

        /**
         * Clears all queued work items and releases the list.
         */
        void flushWork() {
            if (this.mWorkItems != null) {
                this.mWorkItems.clear();
                this.mWorkItems = null;
            }
        }
    }

    /**
     * Adds a command to the work queue.
     *
     * @param work the command to queue
     */
    public void queueWork(String work) {
        this.shWork.addToWork(work);
    }

    /**
     * Executes all queued work items and clears the queue.
     */
    public void execWork() {
        this.shWork.addToWork("echo ");
        setRootInfo(this.shWork.execWork());
    }

    /**
     * Clears all queued work items without executing them.
     */
    public void flushWork() {
        this.shWork.flushWork();
    }

    /**
     * Parses and returns kernel version information from /proc/version.
     *
     * @return formatted kernel version string, or "Unavailable" if parsing fails
     */
    public String getKernel() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 8192);
            try {
                String procVersionStr = reader.readLine();
                reader.close();
                return KernelVersionParser.format(procVersionStr);
            } catch (Throwable th) {
                reader.close();
                throw th;
            }
        } catch (IOException e) {
            Log.e(shellHelper.class.getName(),
                    "IO Exception when getting kernel version for Device Info screen", e);
            return NO_DATA_FOUND;
        }
    }

    /**
     * Reads the first line from a file, falling back to root cat if direct read fails.
     *
     * @param s the file path to read
     * @return the first line of the file, or "Unavailable" if the file cannot be read
     */
    public String getInfo(String s) {
        return sysfsReader.getInfo(s);
    }

    /**
     * Reads a file's first line without fallback to root. Faster than getInfo when
     * the file is known to be readable without root.
     *
     * @param path the file path to read
     * @return the first line of the file
     */
    public String getFastInfo(String path) {
        return sysfsReader.getFastInfo(path);
    }

    /**
     * Reads all lines from a file. Optionally prepends deep sleep time as the first element.
     *
     * @param s the file path to read
     * @param deepsleep if true, prepends the deep sleep time in centiseconds
     * @return array of lines from the file, or null if reading fails
     */
    public String[] getInfo(String s, boolean deepsleep) {
        return sysfsReader.getInfo(s, deepsleep);
    }

    /**
     * Lists directory contents. If flag is true, returns only files; otherwise returns only directories.
     *
     * @param s the directory path
     * @param flag if true, list files; if false, list directories
     * @return array of names, or null if the directory does not exist
     */
    public String[] getDirInfo(String s, boolean flag) {
        return sysfsReader.getDirInfo(s, flag);
    }

    /**
     * Lists entries in the fixed hwmon sysfs hierarchy, falling back to the shared root
     * shell when the app cannot enumerate the directory directly.
     *
     * @param path /sys/class/hwmon or one of its hwmonN directories
     * @param files if true, list files; if false, list directories
     * @return sorted entry names, or an empty array when the directory cannot be listed
     */
    public String[] getRootAwareHwmonDirInfo(String path, boolean files) {
        return hwmonInfo.getRootAwareHwmonDirInfo(path, files);
    }

    /**
     * Lists entries in the allowlisted Tegra I2C temperature hierarchy, using the
     * shared root shell only when direct directory enumeration is unavailable.
     *
     * @param path an allowlisted Tegra I2C hierarchy directory
     * @param files if true, list files; if false, list directories
     * @return sorted child names, or an empty array when access is rejected or fails
     */
    public String[] getRootAwareTegraI2cDirInfo(String path, boolean files) {
        return tegraI2cInfo.getRootAwareTegraI2cDirInfo(path, files);
    }

    /**
     * Reads an allowlisted Tegra I2C tempN_input node, using the shared root shell only
     * when a direct read fails.
     *
     * @param path an allowlisted Tegra I2C tempN_input path
     * @return the node contents, or "Unavailable" when access is rejected or fails
     */
    public String getRootAwareTegraI2cInfo(String path) {
        return tegraI2cInfo.getRootAwareTegraI2cInfo(path);
    }

    /**
     * Reads optional metadata from an allowlisted Tegra I2C device without using
     * the root shell when direct access is unavailable.
     *
     * @param path an allowlisted Tegra I2C name or tempN_label path
     * @return the trimmed node contents, or "Unavailable" when access is rejected or fails
     */
    public String getDirectTegraI2cInfo(String path) {
        return tegraI2cInfo.getDirectTegraI2cInfo(path);
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
    public String[] getInfoArray(String s, int flag, int flag_io) {
        return sysfsReader.getInfoArray(s, flag, flag_io);
    }

    /** Reads a value list using an explicit decoding mode instead of magic flags. */
    public String[] getInfoArray(String path, ReadMode mode) {
        if (mode == null) {
            return new String[] {NO_DATA_FOUND};
        }
        return sysfsReader.getInfoArray(path, mode);
    }

    /** Returns the shared typed sysfs boundary for new hardware controllers. */
    public SysfsRepository getSysfsRepository() {
        return sysfsRepository;
    }

    /** Returns the domain API for new callers that should not issue shell commands directly. */
    public HardwareGateway getHardwareGateway() {
        return hardwareGateway;
    }

    /**
     * Extracts the substring between the first '[' and last ']' in a string.
     *
     * @param s the input string
     * @return the substring between brackets, or "Unavailable" if brackets not found
     */
    public String getInfoString(String s) {
        return sysfsReader.getInfoString(s);
    }

    /**
     * Converts a frequency string from kHz or Hz to MHz with proper formatting.
     *
     * @param mhzString the frequency string to convert
     * @return the frequency in MHz with " MHz" suffix, or "Unavailable" if conversion fails
     */
    public String toMHz(String mhzString) {
        return FrequencyFormat.toMHz(mhzString);
    }

    /** Reads /proc/meminfo and returns a validated Overview memory snapshot. */
    public MemorySnapshot getMemory(String s) {
        return sysfsReader.getMemory(s);
    }

    /**
     * Writes content to a sysfs file using root permissions. Changes file permissions to
     * writable and uses printf to write the value safely.
     *
     * @param content the content to write to the file
     * @param path the sysfs path to write to
     * @return true when commands are successfully submitted to the root shell, false when
     *         parameters are invalid, the shell is unavailable, or command submission fails
     */
    public boolean setRootInfo(String content, String path) {
        if (content == null || content.isEmpty() || content.trim().isEmpty() || path == null || path.isEmpty() || path.trim().isEmpty()) {
            Log.e(shellHelper.class.getName(), "setRootInfo called with invalid content or path, ignoring.");
            return false;
        }
        String quotedPath = escapeShellArg(path);
        String[] commands = {"chmod 0666 " + quotedPath, "printf %s " + escapeShellArg(content) + " > " + quotedPath};
        session.addCommands(commands);
        return session.runCommands();
    }

    /**
     * Escapes a string value for safe use as a shell argument by wrapping it in single
     * quotes and escaping any embedded single quotes.
     *
     * @param value the string to escape
     * @return the shell-escaped string
     */
    public static String escapeShellArg(String value) {
        return RootShellSession.escapeShellArg(value);
    }

    /**
     * Executes an array of shell commands as root.
     *
     * @param array the array of commands to execute
     * @return true if commands were executed successfully, false otherwise
     */
    public boolean setRootInfo(String[] array) {
        session.addCommands(array);
        return session.runCommands();
    }

    /**
     * Remounts /system as read-write.
     */
    public void remountSystem() {
        session.addCommand("mount -o remount,rw -t ext3 /dev/block/mmcblk1p21 /system");
        session.runCommands();
    }

    /**
     * Executes a root command with a parameter and returns the output.
     *
     * @param command the command to execute
     * @param parameter the parameter to pass to the command
     * @return the command output, or "Unavailable" if the command fails
     */
    public String getRootInfo(String command, String parameter) {
        session.addCommand(command + " " + parameter);
        String ret = session.getRootResult();
        if (ret == null) {
            return NO_DATA_FOUND;
        }
        return ret;
    }

    /**
     * Sends a single command to the root shell and blocks until the shell
     * produces a result for it (or the read fails/is interrupted), unlike
     * {@link #setRootInfo(String[])} which only writes the command and
     * returns immediately. Operations such as {@code dd} produce no output
     * of their own, so the caller should chain an {@code echo} marker onto
     * the command to give the blocking read something to return once the
     * preceding operation has actually finished.
     */
    public boolean runCommandAndWait(String command) {
        session.openShell();
        if (!session.isLoaded()) {
            return false;
        }
        session.addCommand(command);
        return session.getRootResult() != null;
    }

    /**
     * Sends a single command to the root shell and blocks until the shell
     * produces a result, returning the actual output. Returns null if the
     * shell is not loaded or the command fails.
     */
    public String runCommandAndWaitForOutput(String command) {
        session.openShell();
        if (!session.isLoaded()) {
            return null;
        }
        session.addCommand(command);
        return session.getRootResult();
    }

    /**
     * Root-aware check for whether a path is a readable block device.
     * Unlike {@link com.aero.control.helpers.GenericHelper#doesExist(String)},
     * which uses app-level {@code File.exists()} and can reject a boot
     * partition that the app can't see directly but the root shell can,
     * this runs the check through the shared root shell. Only intended for
     * use with a fixed, application-controlled set of candidate paths.
     */
    public boolean isReadableBlockDevice(String path) {
        if (path == null) {
            return false;
        }
        String command = "[ -b \"" + path + "\" ] && [ -r \"" + path + "\" ] && echo BLOCK_OK || echo BLOCK_FAIL";
        String output = runCommandAndWaitForOutput(command);
        return output != null && output.contains("BLOCK_OK");
    }

    /**
     * Executes a root command and splits the output into an array using the specified delimiter.
     *
     * @param command the command to execute
     * @param split the delimiter regex to use for splitting the output
     * @return an array of strings split from the command output
     */
    public String[] getRootArray(String command, String split) {
        ArrayList<String> temp = new ArrayList<>();
        session.addCommand(command);
        String ret = session.getRootResult();
        if (ret == null) {
            ret = NO_DATA_FOUND;
        }
        String[] arr$ = ret.split(split);
        for (String a : arr$) {
            temp.add(a);
        }
        return temp.toArray(new String[0]);
    }

    /**
     * Legacy method that spawns a new root process for each command. Used for
     * operations that cannot use the persistent shell.
     *
     * @param command the command to execute
     * @param parameter the parameter to pass to the command
     * @return the command output, or "Unavailable" if the command fails
     */
    public String getLegacyRootInfo(String command, String parameter) {
        return overclockLegacy.getLegacyRootInfo(command, parameter);
    }

    /**
     * Sets kernel overclock addresses by reading kallsyms and writing to procfs.
     * Device-specific feature for legacy overclock support.
     *
     * @return true if addresses were set successfully, false otherwise
     */
    public boolean setOverclockAddress() {
        return overclockLegacy.setOverclockAddress();
    }
}
