package com.termux.app.terminal;

/**
 * Utilities for normalizing wrapped terminal selections and detecting quote-splitting line breaks.
 */
public final class TerminalTextUtils {

    private TerminalTextUtils() {
    }

    /**
     * Join multi-line text into a single space-separated line, trimming horizontal padding
     * introduced by terminal wrapping or TUI margins.
     */
    public static String joinLinesAsSingleLine(String text) {
        if (text == null) return null;
        if (text.isEmpty()) return "";

        String[] lines = text.split("\r?\n");
        StringBuilder result = new StringBuilder(text.length());
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(trimmed);
        }
        return result.toString();
    }

    /**
     * Detect whether a line break occurs inside an unclosed single or double quote
     * (excluding triple-quoted multi-line strings and backslash-escaped line continuations).
     */
    public static boolean hasLineBreakInsideQuote(String text) {
        if (text == null || text.isEmpty()) return false;

        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) {
            end--;
        }
        if (end == 0) return false;

        final int NONE = 0;
        final int SINGLE = 1;
        final int DOUBLE = 2;
        final int TRIPLE_SINGLE = 3;
        final int TRIPLE_DOUBLE = 4;

        int state = NONE;
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);

            if (c == '\r' || c == '\n') {
                if (state == SINGLE || state == DOUBLE) {
                    return true;
                }
                if (c == '\r' && i + 1 < end && text.charAt(i + 1) == '\n') {
                    i++;
                }
                continue;
            }

            if (state == NONE) {
                if (c == '\'' && i + 2 < end && text.charAt(i + 1) == '\'' && text.charAt(i + 2) == '\'') {
                    state = TRIPLE_SINGLE;
                    i += 2;
                } else if (c == '"' && i + 2 < end && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"') {
                    state = TRIPLE_DOUBLE;
                    i += 2;
                } else if (c == '\'') {
                    state = SINGLE;
                } else if (c == '"') {
                    state = DOUBLE;
                }
            } else if (state == SINGLE) {
                if (c == '\'') {
                    state = NONE;
                }
            } else if (state == DOUBLE) {
                if (c == '\\') {
                    if (i + 1 < end && text.charAt(i + 1) == '\r' && i + 2 < end && text.charAt(i + 2) == '\n') {
                        i += 2;
                    } else {
                        i++;
                    }
                } else if (c == '"') {
                    state = NONE;
                }
            } else if (state == TRIPLE_SINGLE) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'' && i + 2 < end && text.charAt(i + 1) == '\'' && text.charAt(i + 2) == '\'') {
                    state = NONE;
                    i += 2;
                }
            } else if (state == TRIPLE_DOUBLE) {
                if (c == '\\') {
                    i++;
                } else if (c == '"' && i + 2 < end && text.charAt(i + 1) == '"' && text.charAt(i + 2) == '"') {
                    state = NONE;
                    i += 2;
                }
            }
        }
        return false;
    }
}
