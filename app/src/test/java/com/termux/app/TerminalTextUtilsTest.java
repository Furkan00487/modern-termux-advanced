package com.termux.app;

import com.termux.app.terminal.TerminalTextUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TerminalTextUtilsTest {

    @Test
    public void joinLinesAsSingleLineHandlesNullAndBlank() {
        assertNull(TerminalTextUtils.joinLinesAsSingleLine(null));
        assertEquals("", TerminalTextUtils.joinLinesAsSingleLine(""));
        assertEquals("", TerminalTextUtils.joinLinesAsSingleLine("   \n  \r\n\t "));
    }

    @Test
    public void joinLinesAsSingleLineJoinsWrappedCommandCleanly() {
        String wrapped = "  python -c 'ad = input(\"Adın ne? \"); print(f\"Selam {ad},\n"
            + "    harika bir gün olsun! 🎉🔥\")'  \n";
        assertEquals(
            "python -c 'ad = input(\"Adın ne? \"); print(f\"Selam {ad}, harika bir gün olsun! 🎉🔥\")'",
            TerminalTextUtils.joinLinesAsSingleLine(wrapped)
        );
    }

    @Test
    public void joinLinesAsSingleLineSkipsBlankIntermediateLines() {
        assertEquals("echo a b", TerminalTextUtils.joinLinesAsSingleLine("echo a\r\n\r\n  b  "));
    }

    @Test
    public void hasLineBreakInsideQuoteDetectsWrappedSingleAndDoubleQuotes() {
        String wrappedSingle = "python -c 'ad = input(\"Adın?\"); print(f\"Selam {ad},\nharika!\")'";
        assertTrue(TerminalTextUtils.hasLineBreakInsideQuote(wrappedSingle));

        String wrappedDouble = "print(f\"Selam {ad},\nharika!\")";
        assertTrue(TerminalTextUtils.hasLineBreakInsideQuote(wrappedDouble));
    }

    @Test
    public void hasLineBreakInsideQuoteIgnoresBalancedLinesAndTrailingNewline() {
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote(null));
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote(""));
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote("print(\"hello\")\n"));

        String multiLineScript = "ad = input(\"Adın ne? \")\nprint(f\"Selam {ad}\")";
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote(multiLineScript));
    }

    @Test
    public void hasLineBreakInsideQuoteIgnoresTripleQuotesAndEscapedLineContinuation() {
        String tripleQuoted = "doc = \"\"\"\nline 1\nline 2\n\"\"\"";
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote(tripleQuoted));

        String tripleSingle = "doc = '''\nline 1\n'''";
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote(tripleSingle));

        String escapedNewline = "x = \"hello \\\nworld\"";
        assertFalse(TerminalTextUtils.hasLineBreakInsideQuote(escapedNewline));
    }
}
