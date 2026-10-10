package com.aero.control.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class BackConfirmationTest {
    @Before
    public void resetConfirmation() {
        BackConfirmation.clear();
    }

    @Test
    public void activityHandoffKeepsOnlyRemainingInterval() {
        BackConfirmation.start(1000);
        long savedDeadline = BackConfirmation.getDeadline();
        BackConfirmation.restore(savedDeadline);

        assertEquals(4500, BackConfirmation.getDeadline());
        assertTrue(BackConfirmation.isPending(4499));
        assertFalse(BackConfirmation.isPending(4500));
    }

    @Test
    public void expiredConfirmationStartsANewInterval() {
        BackConfirmation.start(1000);
        assertFalse(BackConfirmation.isPending(5000));

        BackConfirmation.start(5000);
        assertTrue(BackConfirmation.isPending(8499));
        assertFalse(BackConfirmation.isPending(8500));
    }

    @Test
    public void staleActivityStateCannotExtendCurrentDeadline() {
        BackConfirmation.start(1000);
        BackConfirmation.restore(8000);

        assertEquals(4500, BackConfirmation.getDeadline());
        assertFalse(BackConfirmation.isPending(4500));
    }

    @Test
    public void clearedConfirmationCannotBeRestoredByUnderlyingActivity() {
        BackConfirmation.start(1000);
        long savedDeadline = BackConfirmation.getDeadline();
        BackConfirmation.clear();
        BackConfirmation.restore(savedDeadline);

        assertFalse(BackConfirmation.isPending(2000));
    }

    @Test
    public void deadlineFromBeforeClockResetIsNotPending() {
        BackConfirmation.start(10000);

        assertFalse(BackConfirmation.isPending(100));
    }
}
