package com.termux.app;

import com.termux.x11.ScreenIdlePolicy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ScreenIdlePolicyTest {

    @Test
    public void disconnectedClearsKeepScreenOnEvenWhenInhibited() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(false, true, "never", 30_000L, 0L);
        assertFalse(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }

    @Test
    public void x11InhibitOverridesSystemTimeout() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, true, "system", 60_000L, 500_000L);
        assertTrue(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }

    @Test
    public void x11InhibitOverridesExpiredNumericTimeout() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, true, "5", 30_000L, 600_000L);
        assertTrue(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }

    @Test
    public void systemModeDropsKeepScreenOnWhenNotInhibited() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, false, "system", 60_000L, 0L);
        assertFalse(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }

    @Test
    public void neverModeKeepsScreenOnWhenNotInhibited() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, false, "never", 60_000L, 900_000L);
        assertTrue(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }

    @Test
    public void numericModeSchedulesRemainingDelay() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, false, "5", 60_000L, 30_000L);
        assertTrue(d.keepScreenOn);
        assertEquals(210_000L, d.delayMs);
        assertEquals("5", d.armedMode);
    }

    @Test
    public void numericModeTurnsOffOnceElapsedReachesRemainingTimeout() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, false, "5", 60_000L, 240_000L);
        assertFalse(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }

    @Test
    public void invalidNumericModeFailsClosedWithoutKeepScreenOn() {
        ScreenIdlePolicy.Decision d = ScreenIdlePolicy.evaluate(true, false, "bogus", 60_000L, 0L);
        assertFalse(d.keepScreenOn);
        assertEquals(ScreenIdlePolicy.NO_DELAY, d.delayMs);
        assertNull(d.armedMode);
    }
}
