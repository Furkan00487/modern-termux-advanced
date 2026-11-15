package com.termux.x11;

/**
 * Computes whether Termux:X11 should keep the screen awake and when the idle timeout check must re-run.
 */
public final class ScreenIdlePolicy {
    public static final long NO_DELAY = -1L;

    public static final class Decision {
        public final boolean keepScreenOn;
        public final long delayMs;
        public final String armedMode;

        public Decision(boolean keepScreenOn, long delayMs, String armedMode) {
            this.keepScreenOn = keepScreenOn;
            this.delayMs = delayMs;
            this.armedMode = armedMode;
        }
    }

    private ScreenIdlePolicy() {}

    /**
     * Evaluates keep-screen-on state and follow-up delay from connection, X11 inhibit, and timeout mode.
     */
    public static Decision evaluate(
            boolean connected,
            boolean appSleepInhibited,
            String mode,
            long systemTimeoutMs,
            long elapsedSinceActivityMs) {
        if (!connected)
            return new Decision(false, NO_DELAY, null);

        if (appSleepInhibited || "never".equals(mode))
            return new Decision(true, NO_DELAY, null);

        if ("system".equals(mode))
            return new Decision(false, NO_DELAY, null);

        long minutes;
        try {
            minutes = Long.parseLong(mode);
        } catch (NumberFormatException ignored) {
            return new Decision(false, NO_DELAY, null);
        }

        long timeoutMs = Math.max(minutes * 60_000L - systemTimeoutMs, 0L);
        if (elapsedSinceActivityMs >= timeoutMs)
            return new Decision(false, NO_DELAY, null);

        return new Decision(true, timeoutMs - elapsedSinceActivityMs, mode);
    }
}
