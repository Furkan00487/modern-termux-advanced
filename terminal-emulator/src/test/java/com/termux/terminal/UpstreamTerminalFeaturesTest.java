package com.termux.terminal;

import java.lang.reflect.Field;

public class UpstreamTerminalFeaturesTest extends TerminalTestCase {

    public void testKeyHandlerPageUpAndPageDownTermcaps() {
        assertEquals("\033[5~", KeyHandler.getCodeFromTermcap("kP", false, false));
        assertEquals("\033[6~", KeyHandler.getCodeFromTermcap("kN", false, false));
    }

    public void testTerminalSessionUses64KbBuffers() throws Exception {
        TerminalSession session = new TerminalSession("/bin/sh", "/", new String[]{}, new String[]{}, null, null);
        Field bufField = ByteQueue.class.getDeclaredField("mBuffer");
        bufField.setAccessible(true);
        byte[] processToTermBuf = (byte[]) bufField.get(session.mProcessToTerminalIOQueue);
        assertEquals(64 * 1024, processToTermBuf.length);

        Field recvBufField = session.mMainThreadHandler.getClass().getDeclaredField("mReceiveBuffer");
        recvBufField.setAccessible(true);
        byte[] receiveBuffer = (byte[]) recvBufField.get(session.mMainThreadHandler);
        assertEquals(64 * 1024, receiveBuffer.length);
    }

    public void testTerminalEmulatorTopRowCaching() {
        withTerminalSized(10, 5);
        assertEquals(0, mTerminal.getTopRow());
        mTerminal.setTopRow(-3);
        assertEquals(-3, mTerminal.getTopRow());
    }

    public void testTerminalBufferBlockSetClearsLineWrap() {
        TerminalBuffer buffer = new TerminalBuffer(4, 4, 4);
        buffer.setLineWrap(0);
        assertTrue(buffer.getLineWrap(0));
        buffer.blockSet(0, 0, 4, 1, ' ', TextStyle.NORMAL);
        assertFalse(buffer.getLineWrap(0));
    }

    public void testSixelAndITermImageClassesAndSequences() {
        withTerminalSized(20, 10);
        // Primary Device Attributes response should advertise Sixel ('4')
        assertEnteringStringGivesResponse("\033[c", "\033[?64;1;2;4;6;9;15;18;21;22c");

        // Verify TerminalSixel, ITermImage, TerminalBitmap, AndroidUtils classes & constants exist
        assertEquals(6, TerminalSixel.SIXEL__LINE_LEN);
        assertEquals(8192, TerminalSixel.SIXEL__MAX_REPEAT);
        assertTrue(TerminalBitmap.MAX_BITMAP_SIZE > 0);
        assertNotNull(ITermImage.ImageState.INIT);
        assertNotNull(AndroidUtils.getSystemProperties("UpstreamTest"));

        // Verify TextStyle bitmap encoding/decoding
        long bitmapStyle = TextStyle.encodeTerminalBitmap(3, 1, 2);
        assertTrue(TextStyle.isTerminalBitmap(bitmapStyle));
        assertEquals(3, TextStyle.getTerminalBitmapNum(bitmapStyle));
        assertEquals(1, TextStyle.getTerminalBitmapX(bitmapStyle));
        assertEquals(2, TextStyle.getTerminalBitmapY(bitmapStyle));

        // Verify Sixel DCS sequence and iTerm OSC 1337 sequence are consumed cleanly without printing raw payload
        enterString("\033Pq#0;2;0;0;0#1;2;100;100;0#1~~@@vv@@~~@@~~$-\033\\");
        enterString("\033]1337;File=inline=0:AAAA\007");
        enterString("OK");
        assertTrue(mTerminal.getScreen().getTranscriptText().contains("OK"));
    }
}
