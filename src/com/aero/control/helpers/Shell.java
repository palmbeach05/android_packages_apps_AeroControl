package com.aero.control.helpers;

import android.os.Looper;
import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Shell {
    private static final String LOG_TAG = Shell.class.getName();
    private Process mProcess = null;
    private DataOutputStream mShellOutput = null;
    private List<String> mCommands = new ArrayList();

    public Shell(final String commands, boolean runOnOwnThread) {
        if (!runOnOwnThread) {
            initInteractive(commands);
            return;
        }
        Runnable run = new Runnable() { // from class: com.aero.control.helpers.Shell.1
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

    private void checkUIThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new ShellException(ShellException.MAIN_UI_EXCEPTION);
        }
    }

    public synchronized void initInteractive(String su) {
        checkUIThread();
        try {
            this.mProcess = Runtime.getRuntime().exec(su);
            this.mShellOutput = new DataOutputStream(this.mProcess.getOutputStream());
        } catch (IOException e) {
            throw new ShellException(ShellException.NO_INTERACTIVE_SHELL);
        }
    }

    public synchronized void addCommand(String cmd) {
        this.mCommands.add(cmd);
    }

    public synchronized void addCommand(List<String> cmds) {
        this.mCommands.addAll(cmds);
    }

    public synchronized void addCommand(String[] cmds) {
        for (String cmd : cmds) {
            if (cmd != null) {
                this.mCommands.add(cmd);
            }
        }
    }

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

    public void closeInteractive() {
        try {
            this.mShellOutput.close();
        } catch (IOException e) {
        }
        this.mProcess.destroy();
    }

    public static class ShellException extends RuntimeException {
        public static final String MAIN_UI_EXCEPTION = "You have tried to execute your commands in the main UI Thread. Consider using async-tasks or a thread instead.";
        public static final String NO_INTERACTIVE_SHELL = "The interactive shell couldn't be created";

        public ShellException(String message) {
            super(message);
        }
    }
}
