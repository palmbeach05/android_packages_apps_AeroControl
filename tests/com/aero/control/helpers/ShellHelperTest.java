package com.aero.control.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.Test;

public class ShellHelperTest {
    private static final String COMPLETION_MARKER = "__AERO_ROOT_COMPLETE_test__";

    @Test
    public void completionMarkersAreUniquePerRequest() {
        assertNotEquals(shellHelper.newRootCompletionMarker(),
                shellHelper.newRootCompletionMarker());
    }

    @Test
    public void emptyDirectoryCompletesWithEmptyResult() throws Exception {
        BufferedReader output = persistentShellOutput("");

        assertEquals("", shellHelper.readUntilCompletionMarker(output, COMPLETION_MARKER));
        assertEquals("next-command-output", output.readLine());
    }

    @Test
    public void absentTemperatureNodeReturnsUnavailable() throws Exception {
        BufferedReader output = persistentShellOutput("");

        String result = shellHelper.readUntilCompletionMarker(output, COMPLETION_MARKER);

        assertEquals("Unavailable", shellHelper.unavailableIfEmpty(result));
        assertEquals("next-command-output", output.readLine());
    }

    private BufferedReader persistentShellOutput(String commandOutput) {
        return new BufferedReader(new StringReader(commandOutput + "\n"
                + COMPLETION_MARKER + "\nnext-command-output\n"));
    }
}
