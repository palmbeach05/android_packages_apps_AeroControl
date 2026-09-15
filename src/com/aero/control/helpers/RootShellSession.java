package com.aero.control.helpers;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns a single persistent root ({@code su}) shell process, its command
 * queue, and the read/write plumbing used to send commands and collect
 * their output. This is the low-level session primitive that {@link
 * shellHelper} exposes to the rest of the app; it intentionally knows
 * nothing about sysfs layout, kernel-version parsing, or any
 * device-specific hardware quirks.
 */
public final class RootShellSession {
    private static final int MAX_RESULT_LEN = 65536;
    private static final long DEFAULT_COMMAND_TIMEOUT_MS = 5000L;
    private static final String LOG_TAG = RootShellSession.class.getName();

    private final long commandTimeoutMs;
    private List<String> mCommands = new ArrayList<>();
    private Process mProcess = null;
    private DataOutputStream mShellOutput = null;
    private BufferedReader mOutput = null;
    private boolean mShellLoaded = false;

    public RootShellSession() {
        this(DEFAULT_COMMAND_TIMEOUT_MS);
    }

    public RootShellSession(long commandTimeoutMs) {
        if (commandTimeoutMs <= 0) {
            throw new IllegalArgumentException("commandTimeoutMs must be positive");
        }
        this.commandTimeoutMs = commandTimeoutMs;
    }

    /**
     * Opens the root shell session if not already open.
     */
    synchronized void openShell() {
        if (this.mCommands == null) {
            this.mCommands = new ArrayList<>();
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

    /**
     * Closes the root shell session and cleans up resources.
     */
    synchronized void closeShell() {
        if (this.mShellOutput != null) {
            try {
                this.mShellOutput.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Failed to close root shell input", e);
            }
            this.mShellOutput = null;
        }
        if (this.mOutput != null) {
            try {
                this.mOutput.close();
            } catch (IOException e) {
                Log.w(LOG_TAG, "Failed to close root shell output", e);
            }
            this.mOutput = null;
        }
        if (this.mProcess != null) {
            this.mProcess.destroy();
            this.mProcess = null;
        }
        this.mShellLoaded = false;
    }

    synchronized boolean isLoaded() {
        return this.mShellLoaded;
    }

    /**
     * Adds multiple commands to the command queue.
     *
     * @param commands the array of command strings to add
     */
    synchronized void addCommands(String[] commands) {
        for (String cmd : commands) {
            if (cmd != null) {
                this.mCommands.add(cmd);
            }
        }
    }

    /**
     * Adds a single command to the queue.
     *
     * @param cmd the command to add
     */
    synchronized void addCommand(String cmd) {
        this.mCommands.add(cmd);
    }

    /**
     * Executes all queued commands in the root shell without waiting for output.
     * Clears the command queue after execution.
     *
     * @return true if commands were executed successfully, false if the shell is not loaded
     */
    synchronized boolean runCommands() {
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
                    // Best-effort flush; the writes above already succeeded.
                }
            } catch (IOException e2) {
                Log.e(LOG_TAG, "Something interrupted our operations...", e2);
                this.mCommands.clear();
                return false;
            }
            this.mCommands.clear();
            return true;
        } else {
            this.mCommands.clear();
            return false;
        }
    }

    /**
     * Executes all queued commands and reads the output from the root shell.
     * Blocks until all output has been read or the read is interrupted. Clears
     * the command queue after execution.
     *
     * @return the command output as a string, or null if reading fails or is interrupted
     */
    synchronized String getRootResult() {
        final List<String> commands = new ArrayList<>(this.mCommands);
        this.mCommands.clear();
        if (!this.mShellLoaded) {
            return null;
        }

        final String[] result = new String[1];
        Thread commandThread = new Thread(new Runnable() {
            @Override
            public void run() {
                result[0] = executeAndRead(commands);
            }
        }, "AeroRootShellCommand");
        commandThread.start();
        try {
            commandThread.join(this.commandTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeShell();
            return null;
        }
        if (commandThread.isAlive()) {
            Log.e(LOG_TAG, "Root shell command timed out after " + this.commandTimeoutMs + "ms");
            closeShell();
            return null;
        }
        return result[0];
    }

    private String executeAndRead(List<String> commands) {
        int read;
        char[] buf = new char[8192];
        StringBuilder response = new StringBuilder();
        try {
            for (String cmd : commands) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                this.mShellOutput.write((cmd + "\n").getBytes("UTF-8"));
                do {
                    if (Thread.currentThread().isInterrupted()) {
                        return null;
                    }
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
            this.mShellOutput.flush();
            return response.toString().trim();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Root shell command failed", e);
            return null;
        }
    }

    /**
     * Escapes a string value for safe use as a shell argument by wrapping it in single
     * quotes and escaping any embedded single quotes.
     *
     * @param value the string to escape
     * @return the shell-escaped string
     */
    static String escapeShellArg(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
