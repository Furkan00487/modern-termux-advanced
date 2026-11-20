package com.termux.app;

import android.app.Application;
import android.app.Notification;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE;
import com.termux.shared.termux.settings.properties.TermuxAppSharedProperties;
import com.termux.terminal.TerminalBuffer;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalOutput;
import com.termux.terminal.TextStyle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class)
public class UpstreamSyncAppFixesTest {

    @Before
    public void resetSharedProperties() throws Exception {
        Field propField = TermuxAppSharedProperties.class.getDeclaredField("properties");
        propField.setAccessible(true);
        propField.set(null, null);
    }

    @Test
    public void testRunCommandServiceRejectsBeforeFileResultConfigWhenAllowExternalAppsIsFalse() {
        File resultDir = new File(Environment.getExternalStorageDirectory(), "run_cmd_test_" + System.nanoTime());
        assertTrue(resultDir.mkdirs());
        try {
            File targetFile = new File(resultDir, "injected.properties");
            assertFalse(targetFile.exists());

            Intent intent = new Intent(RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND);
            intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, "/system/bin/true");
            intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_RESULT_DIRECTORY, resultDir.getAbsolutePath());
            intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_RESULT_SINGLE_FILE, true);
            intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_RESULT_FILE_BASENAME, "injected.properties");
            intent.putExtra(RUN_COMMAND_SERVICE.EXTRA_RESULT_FILE_ERROR_FORMAT, "allow-external-apps=true");

            RunCommandService service = Robolectric.buildService(RunCommandService.class).create().get();
            service.onStartCommand(intent, 0, 1);

            assertFalse("Result file must not be written when allow-external-apps is false", targetFile.exists());
        } finally {
            File[] children = resultDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    //noinspection ResultOfMethodCallIgnored
                    child.delete();
                }
            }
            //noinspection ResultOfMethodCallIgnored
            resultDir.delete();
        }
    }

    @Test
    public void testTermuxOpenReceiverContentProviderGetTypeHandlesNullLastPathSegment() {
        TermuxOpenReceiver.ContentProvider provider = new TermuxOpenReceiver.ContentProvider();
        assertNull(provider.getType(Uri.parse("content://com.termux.files/")));
        assertNull(provider.getType(Uri.parse("content://com.termux.files")));
        assertNull(provider.getType(Uri.parse("content://com.termux.files/noext")));
    }

    @Test
    public void testNotificationUtilsGuardsNullBigText() {
        Notification.Builder builder = NotificationUtils.geNotificationBuilder(
            RuntimeEnvironment.getApplication(),
            "test_channel",
            Notification.PRIORITY_DEFAULT,
            "Test Title",
            "Test Text",
            null,
            null,
            null,
            NotificationUtils.NOTIFICATION_MODE_SILENT
        );
        assertNotNull(builder);
        Notification notification = builder.build();
        assertNull(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
    }

    @Test
    public void testOsc52Supports100KbClipboardAndSixelCreatesBitmap() {
        final List<String> clipboardPuts = new ArrayList<>();
        TerminalOutput output = new TerminalOutput() {
            @Override public void write(byte[] data, int offset, int count) {}
            @Override public void titleChanged(String oldTitle, String newTitle) {}
            @Override public void onCopyTextToClipboard(String text) { clipboardPuts.add(text); }
            @Override public void onPasteTextFromClipboard() {}
            @Override public void onBell() {}
            @Override public void onColorsChanged() {}
        };
        TerminalEmulator emulator = new TerminalEmulator(output, 40, 20, 10, 20, 100, null);

        // OSC 52 > 16KB and < 100KB (~40KB raw -> ~53KB base64)
        byte[] raw = new byte[40 * 1024];
        Arrays.fill(raw, (byte) 'A');
        String b64 = Base64.getEncoder().encodeToString(raw);
        String osc52 = "\033]52;c;" + b64 + "\007";
        byte[] oscBytes = osc52.getBytes(StandardCharsets.UTF_8);
        emulator.append(oscBytes, oscBytes.length);

        assertEquals(1, clipboardPuts.size());
        assertEquals(40 * 1024, clipboardPuts.get(0).length());

        // Sixel sequence should allocate a TerminalBitmap cell on screen
        String sixel = "\033Pq#0;2;0;0;0#1;2;100;100;0#1~~@@vv@@~~@@~~$-\033\\";
        byte[] sixelBytes = sixel.getBytes(StandardCharsets.UTF_8);
        emulator.append(sixelBytes, sixelBytes.length);

        TerminalBuffer screen = emulator.getScreen();
        boolean foundBitmapCell = false;
        for (int r = 0; r < 20; r++) {
            for (int c = 0; c < 40; c++) {
                if (TextStyle.isTerminalBitmap(screen.getStyleAt(r, c))) {
                    foundBitmapCell = true;
                    break;
                }
            }
            if (foundBitmapCell) break;
        }
        assertTrue("Expected Sixel sequence to place a TerminalBitmap cell", foundBitmapCell);
    }
}
