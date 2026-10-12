package com.aero.control.helpers;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns a single persistent root ({@code su}) shell process, its command
 * queue, and the read/write plumbing used to send commands and collect
 * their output. This low-level session primitive is owned by
 * {@link HardwareServices}; it intentionally knows nothing about sysfs layout,
 * kernel-version parsing, or device-specific hardware quirks.
 */
public final class RootShellSession {
    private static final int MAX_RESULT_LEN = 65536;
    private static final long DEFAULT_COMMAND_TIMEOUT_MS = 5000L;
    private static final String LOG_TAG = RootShellSession.class.getName();
    private static final AtomicLong COMMAND_SEQUENCE = new AtomicLong();

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
        if (this.mProcess != null) {
            this.mProcess.destroy();
            this.mProcess = null;
        }
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
        return getRootResult() != null;
    }

    /**
     * Executes all queued commands and reads the output from the root shell.
     * Blocks until all output has been read or the read is interrupted. Clears
     * the command queue after execution.
     *
     * @return the command output as a string, or null if reading fails or is interrupted
     */
    synchronized String getRootResult() {
        return getRootResult(this.commandTimeoutMs);
    }

    /**
     * Executes queued commands, allowing callers to select a timeout for an
     * operation that is expected to take longer than an ordinary sysfs command.
     */
    synchronized String getRootResult(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be positive");
        }
        final List<String> commands = new ArrayList<>(this.mCommands);
        this.mCommands.clear();
        return executeCommands(commands, timeoutMs);
    }

    /**
     * Executes a complete command as one serialized request and returns its output.
     * The unique completion marker prevents partial reads and output from concurrent
     * requests from being mistaken for a completed command.
     */
    public synchronized String execute(String command) {
        return execute(command, this.commandTimeoutMs);
    }

    /** Executes one serialized command with an explicit positive timeout. */
    public synchronized String execute(String command, long timeoutMs) {
        if (command == null || command.trim().length() == 0) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be positive");
        }
        List<String> commands = new ArrayList<>();
        commands.add(command);
        return executeCommands(commands, timeoutMs);
    }

    /** Executes a root operation atomically and reports command failure or timeout. */
    public OperationResult executeOperation(String command, long timeoutMs) {
        if (command == null || command.trim().length() == 0) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be positive");
        }
        long operationId = COMMAND_SEQUENCE.incrementAndGet();
        String successMarker = "__AERO_OPERATION_" + operationId + "_OK__";
        String failureMarker = "__AERO_OPERATION_" + operationId + "_FAILED__";
        String checkedCommand = command + " && printf '%s' " + successMarker
                + " || printf '%s' " + failureMarker;
        String output = execute(checkedCommand, timeoutMs);
        if (output == null) {
            return OperationResult.timedOut("Root shell did not complete the operation");
        }
        if (output.contains(successMarker)) {
            return OperationResult.ok();
        }
        return OperationResult.failed(output);
    }

    private String executeCommands(final List<String> commands, long timeoutMs) {
        openShell();
        if (!this.mShellLoaded) {
            return null;
        }
        final String marker = "__AERO_COMMAND_" + COMMAND_SEQUENCE.incrementAndGet()
                + "_" + System.nanoTime() + "__";
        final String terminator = "\n" + marker + "\n";
        final DataOutputStream output = this.mShellOutput;
        final BufferedReader input = this.mOutput;
        final String[] result = new String[1];
        Thread commandThread = new Thread(new Runnable() {
            @Override
            public void run() {
                result[0] = executeAndRead(commands, output, input, marker, terminator);
            }
        }, "AeroRootShellCommand");
        commandThread.setDaemon(true);
        commandThread.start();
        try {
            commandThread.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeShell();
            return null;
        }
        if (commandThread.isAlive()) {
            Log.e(LOG_TAG, "Root shell command timed out after " + timeoutMs + "ms");
            closeShell();
            commandThread.interrupt();
            return null;
        }
        return result[0];
    }

    private String executeAndRead(List<String> commands, DataOutputStream output,
            BufferedReader input, String marker, String terminator) {
        char[] buf = new char[8192];
        StringBuilder response = new StringBuilder();
        StringBuilder tail = new StringBuilder();
        try {
            StringBuilder script = new StringBuilder();
            for (String cmd : commands) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                script.append(cmd).append('\n');
            }
            script.append("printf '\\n%s\\n' ")
                    .append(escapeShellArg(marker)).append('\n');
            output.write(script.toString().getBytes("UTF-8"));
            output.flush();

            int read;
            while ((read = input.read(buf)) != -1) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                for (int i = 0; i < read; i++) {
                    char value = buf[i];
                    if (response.length() < MAX_RESULT_LEN) {
                        response.append(value);
                    }
                    tail.append(value);
                    if (tail.length() > terminator.length()) {
                        tail.deleteCharAt(0);
                    }
                    if (terminator.contentEquals(tail)) {
                        int markerStart = response.lastIndexOf(marker);
                        if (markerStart >= 0) {
                            int outputEnd = markerStart;
                            if (outputEnd > 0 && response.charAt(outputEnd - 1) == '\n') {
                                outputEnd--;
                            }
                            response.setLength(outputEnd);
                        }
                        return response.toString().trim();
                    }
                }
            }
            return null;
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
    public static String escapeShellArg(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
