package com.aero.control.helpers;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final long INITIAL_RECOVERY_DELAY_MS = 1000L;
    private static final long MAX_RECOVERY_DELAY_MS = 30000L;
    private static final long SLOW_OPERATION_LOG_THRESHOLD_MS = 250L;
    private static final String LOG_TAG = RootShellSession.class.getName();
    private static final AtomicLong COMMAND_SEQUENCE = new AtomicLong();

    private final long commandTimeoutMs;
    private List<String> mCommands = new ArrayList<>();
    private Process mProcess = null;
    private DataOutputStream mShellOutput = null;
    private BufferedReader mOutput = null;
    private boolean mShellLoaded = false;
    private int mConsecutiveFailures;
    private long mRetryAtNanos;

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
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        if (this.mRetryAtNanos != 0L && System.nanoTime() < this.mRetryAtNanos) {
            return;
        }
        try {
            if (this.mProcess == null) {
                this.mProcess = Runtime.getRuntime().exec("su");
                Log.i(LOG_TAG, "Root shell started");
            }
            if (this.mShellOutput == null) {
                this.mShellOutput = new DataOutputStream(this.mProcess.getOutputStream());
            }
            if (this.mOutput == null) {
                this.mOutput = new BufferedReader(new InputStreamReader(this.mProcess.getInputStream()));
            }
            this.mShellLoaded = true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Root shell startup failed", e);
            this.mShellLoaded = false;
            closeShell("startup_failure");
        }
    }

    /**
     * Closes the root shell session and cleans up resources.
     */
    private synchronized void closeShell(String reason) {
        boolean wasActive = this.mProcess != null || this.mShellOutput != null
                || this.mOutput != null || this.mShellLoaded;
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
        if (wasActive) {
            Log.i(LOG_TAG, "Root shell closed; reason=" + reason);
        }
        if (isRecoverableFailure(reason)) {
            this.mConsecutiveFailures++;
            long multiplier = 1L << Math.min(this.mConsecutiveFailures - 1, 5);
            long delayMs = Math.min(
                    INITIAL_RECOVERY_DELAY_MS * multiplier, MAX_RECOVERY_DELAY_MS);
            this.mRetryAtNanos = System.nanoTime() + delayMs * 1000000L;
            Log.w(LOG_TAG, "Root shell recovery deferred for " + delayMs
                    + "ms; reason=" + reason);
        }
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
        if (Thread.currentThread().isInterrupted()) {
            return null;
        }
        final long operationId = COMMAND_SEQUENCE.incrementAndGet();
        final long startedAtNanos = System.nanoTime();
        openShell();
        if (Thread.currentThread().isInterrupted() || !this.mShellLoaded) {
            return null;
        }
        final String marker = "__AERO_COMMAND_" + operationId + "_" + startedAtNanos + "__";
        final String terminator = "\n" + marker + "\n";
        final DataOutputStream output = this.mShellOutput;
        final BufferedReader input = this.mOutput;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final CommandExecution[] result = new CommandExecution[1];
        Thread commandThread = new Thread(new Runnable() {
            @Override
            public void run() {
                result[0] = executeAndRead(
                        commands, output, input, marker, terminator, cancelled);
            }
        }, "AeroRootShellCommand");
        commandThread.setDaemon(true);
        commandThread.start();
        try {
            commandThread.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cancelled.set(true);
            closeShell("caller_interrupted");
            commandThread.interrupt();
            Log.i(LOG_TAG, "Root shell operation " + operationId
                    + " interrupted by caller; elapsed="
                    + elapsedMillis(startedAtNanos) + "ms");
            return null;
        }
        if (commandThread.isAlive()) {
            Log.e(LOG_TAG, "Root shell operation " + operationId + " timed out after "
                    + timeoutMs + "ms; elapsed="
                    + elapsedMillis(startedAtNanos) + "ms");
            cancelled.set(true);
            closeShell("command_timeout");
            commandThread.interrupt();
            return null;
        }
        CommandExecution execution = result[0];
        if (execution == null) {
            closeShell("command_worker_failed");
            Log.e(LOG_TAG, "Root shell operation " + operationId
                    + " ended without a result; elapsed="
                    + elapsedMillis(startedAtNanos) + "ms");
            return null;
        }
        if (execution.failureReason != null) {
            closeShell(execution.failureReason);
            if (execution.error != null && !cancelled.get()) {
                Log.e(LOG_TAG, "Root shell operation " + operationId
                        + " failed; reason=" + execution.failureReason
                        + "; elapsed=" + elapsedMillis(startedAtNanos) + "ms",
                        execution.error);
            } else {
                Log.w(LOG_TAG, "Root shell operation " + operationId
                        + " failed; reason=" + execution.failureReason
                        + "; elapsed=" + elapsedMillis(startedAtNanos) + "ms");
            }
            return null;
        }
        this.mConsecutiveFailures = 0;
        this.mRetryAtNanos = 0L;
        long elapsedMs = elapsedMillis(startedAtNanos);
        if (elapsedMs >= SLOW_OPERATION_LOG_THRESHOLD_MS) {
            Log.i(LOG_TAG, "Root shell operation " + operationId
                    + " completed slowly; elapsed=" + elapsedMs + "ms");
        } else {
            Log.d(LOG_TAG, "Root shell operation " + operationId
                    + " completed; elapsed=" + elapsedMs + "ms");
        }
        return execution.output;
    }

    private static boolean isRecoverableFailure(String reason) {
        return "startup_failure".equals(reason)
                || "command_timeout".equals(reason)
                || "command_worker_failed".equals(reason)
                || "stream_closed".equals(reason)
                || "io_failure".equals(reason);
    }

    private CommandExecution executeAndRead(List<String> commands, DataOutputStream output,
            BufferedReader input, String marker, String terminator,
            AtomicBoolean cancelled) {
        char[] buf = new char[8192];
        StringBuilder response = new StringBuilder();
        StringBuilder tail = new StringBuilder();
        try {
            StringBuilder script = new StringBuilder();
            for (String cmd : commands) {
                if (Thread.currentThread().isInterrupted() || cancelled.get()) {
                    return CommandExecution.failure("caller_interrupted", null);
                }
                script.append(cmd).append('\n');
            }
            script.append("printf '\\n%s\\n' ")
                    .append(escapeShellArg(marker)).append('\n');
            output.write(script.toString().getBytes("UTF-8"));
            output.flush();

            int read;
            while ((read = input.read(buf)) != -1) {
                if (Thread.currentThread().isInterrupted() || cancelled.get()) {
                    return CommandExecution.failure("caller_interrupted", null);
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
                        return CommandExecution.success(response.toString().trim());
                    }
                }
            }
            return CommandExecution.failure("stream_closed", null);
        } catch (IOException e) {
            return CommandExecution.failure(
                    cancelled.get() ? "caller_interrupted" : "io_failure",
                    cancelled.get() ? null : e);
        }
    }

    private static long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1000000L;
    }

    private static final class CommandExecution {
        private final String output;
        private final String failureReason;
        private final IOException error;

        private CommandExecution(String output, String failureReason, IOException error) {
            this.output = output;
            this.failureReason = failureReason;
            this.error = error;
        }

        private static CommandExecution success(String output) {
            return new CommandExecution(output, null, null);
        }

        private static CommandExecution failure(String reason, IOException error) {
            return new CommandExecution(null, reason, error);
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
