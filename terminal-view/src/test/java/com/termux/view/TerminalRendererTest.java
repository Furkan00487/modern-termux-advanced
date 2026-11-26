package com.termux.view;

import com.termux.terminal.TerminalRow;
import com.termux.terminal.TextStyle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TerminalRendererTest {

    private static TerminalRow rowWithText(int columns, String text, long style) {
        TerminalRow row = new TerminalRow(columns, style);
        int col = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            row.setChar(col, cp, style);
            col += Math.max(1, com.termux.terminal.WcWidth.width(cp));
            i += Character.charCount(cp);
        }
        return row;
    }

    @Test
    public void isRegionalIndicatorMatchesExactUnicodeRange() {
        assertFalse(TerminalRenderer.isRegionalIndicator('A'));
        assertFalse(TerminalRenderer.isRegionalIndicator(0x1F1E5));
        assertTrue(TerminalRenderer.isRegionalIndicator(0x1F1E6));
        assertTrue(TerminalRenderer.isRegionalIndicator(0x1F1F9));
        assertTrue(TerminalRenderer.isRegionalIndicator(0x1F1FF));
        assertFalse(TerminalRenderer.isRegionalIndicator(0x1F200));
    }

    @Test
    public void pairsAdjacentRegionalIndicatorsForFlagEmoji() {
        long style = TextStyle.NORMAL;
        TerminalRow row = rowWithText(10, "\uD83C\uDDF9\uD83C\uDDF7a", style); // 🇹🇷a

        int pairChars = TerminalRenderer.getRegionalIndicatorPairChars(
            row.mText, 0, 2, row.getSpaceUsed(), 0, 10, row, style, -1, -1);
        assertEquals(4, pairChars);
    }

    @Test
    public void pairsConsecutiveFlagsWithoutMisaligningBoundary() {
        long style = TextStyle.NORMAL;
        // 🇹🇷🇺🇸
        TerminalRow row = rowWithText(10, "\uD83C\uDDF9\uD83C\uDDF7\uD83C\uDDFA\uD83C\uDDF8", style);

        assertEquals(4, TerminalRenderer.getRegionalIndicatorPairChars(
            row.mText, 0, 2, row.getSpaceUsed(), 0, 10, row, style, -1, -1));
        assertEquals(4, TerminalRenderer.getRegionalIndicatorPairChars(
            row.mText, 4, 2, row.getSpaceUsed(), 2, 10, row, style, -1, -1));
    }

    @Test
    public void doesNotPairUnpairedRegionalIndicatorOrRowEdge() {
        long style = TextStyle.NORMAL;
        TerminalRow row = rowWithText(4, "\uD83C\uDDF9a", style); // 🇹a
        assertEquals(0, TerminalRenderer.getRegionalIndicatorPairChars(
            row.mText, 0, 2, row.getSpaceUsed(), 0, 4, row, style, -1, -1));

        TerminalRow edgeRow = rowWithText(4, "abc\uD83C\uDDF9", style);
        assertEquals(0, TerminalRenderer.getRegionalIndicatorPairChars(
            edgeRow.mText, 3, 2, edgeRow.getSpaceUsed(), 3, 4, edgeRow, style, -1, -1));
    }

    @Test
    public void doesNotPairAcrossStyleOrPartialSelectionBoundary() {
        long styleA = TextStyle.encode(1, 0, 0);
        long styleB = TextStyle.encode(2, 0, 0);
        TerminalRow styledRow = new TerminalRow(6, styleA);
        styledRow.setChar(0, 0x1F1F9, styleA);
        styledRow.setChar(1, 0x1F1F7, styleB);

        assertEquals(0, TerminalRenderer.getRegionalIndicatorPairChars(
            styledRow.mText, 0, 2, styledRow.getSpaceUsed(), 0, 6, styledRow, styleA, -1, -1));

        TerminalRow sameStyleRow = rowWithText(6, "\uD83C\uDDF9\uD83C\uDDF7", styleA);
        // Only column 0 selected -> do not pair across selection boundary
        assertEquals(0, TerminalRenderer.getRegionalIndicatorPairChars(
            sameStyleRow.mText, 0, 2, sameStyleRow.getSpaceUsed(), 0, 6, sameStyleRow, styleA, 0, 0));
        // Both columns 0 and 1 selected -> pair cleanly
        assertEquals(4, TerminalRenderer.getRegionalIndicatorPairChars(
            sameStyleRow.mText, 0, 2, sameStyleRow.getSpaceUsed(), 0, 6, sameStyleRow, styleA, 0, 1));
    }
}
