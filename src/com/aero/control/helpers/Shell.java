package com.aero.control.helpers;

import android.os.Looper;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides an interactive shell session for executing multiple root commands efficiently.
 * Maintains a persistent shell process to avoid the overhead of launching a new shell
 * for each command.
 */
public class Shell {
    private static final String LOG_TAG = Shell.class.getName();
    private Process mProcess = null;
    private DataOutputStream mShellOutput = null;
    private List<String> mCommands = new ArrayList();

    /**
     * Creates and initializes an interactive shell session.
     *
     * @param commands the shell binary to execute (typically "su" for root)
     * @param runOnOwnThread whether to initialize the shell on a background thread
     */
    public Shell(final String commands, boolean runOnOwnThread) {
        if (!runOnOwnThread) {
            initInteractive(commands);
            return;
        }
        Runnable run = new Runnable() { // from class: com.aero.control.helpers.Shell.1
            /**
             * Initializes the shell in a background thread.
             */
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Shell.this.initInteractive(commands);
                } catch (ShellException e) {
                    Log.e(Shell.LOG_TAG, "No shell was created.", e);
                }
            }
        };
        Thread worker = new Thread(run);
        worker.start();
    }

    /**
     * Verifies that the current thread is not the main UI thread and throws an
     * exception if it is.
     */
    private void checkUIThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new ShellException(ShellException.MAIN_UI_EXCEPTION);
        }
    }

    /**
     * Initializes the interactive shell by executing the specified shell command.
     *
     * @param su the shell binary to execute (typically "su" for root)
     */
    public synchronized void initInteractive(String su) {
        checkUIThread();
        try {
            this.mProcess = Runtime.getRuntime().exec(su);
            this.mShellOutput = new DataOutputStream(this.mProcess.getOutputStream());
        } catch (IOException e) {
            throw new ShellException(ShellException.NO_INTERACTIVE_SHELL);
        }
    }

    /**
     * Queues a single command to be executed in the shell.
     *
     * @param cmd the command string to add
     */
    public synchronized void addCommand(String cmd) {
        this.mCommands.add(cmd);
    }

    /**
     * Queues multiple commands to be executed in the shell.
     *
     * @param cmds the list of command strings to add
     */
    public synchronized void addCommand(List<String> cmds) {
        this.mCommands.addAll(cmds);
    }

    /**
     * Queues multiple commands from an array to be executed in the shell.
     *
     * @param cmds the array of command strings to add
     */
    public synchronized void addCommand(String[] cmds) {
        for (String cmd : cmds) {
            if (cmd != null) {
                this.mCommands.add(cmd);
            }
        }
    }

    /**
     * Executes all queued commands in the interactive shell session and clears the queue.
     */
    public void runInteractive() {
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
    }

    /**
     * Closes the interactive shell session and releases associated resources.
     */
    public void closeInteractive() {
        try {
            this.mShellOutput.close();
        } catch (IOException e) {
        }
        this.mProcess.destroy();
    }

    /**
     * Exception thrown when shell operations fail or are attempted on the wrong thread.
     */
    public static class ShellException extends RuntimeException {
        public static final String MAIN_UI_EXCEPTION = "You have tried to execute your commands in the main UI Thread. Consider using async-tasks or a thread instead.";
        public static final String NO_INTERACTIVE_SHELL = "The interactive shell couldn't be created";

        /**
         * Creates a new shell exception with the specified message.
         *
         * @param message the detail message
         */
        public ShellException(String message) {
            super(message);
        }
    }
}
