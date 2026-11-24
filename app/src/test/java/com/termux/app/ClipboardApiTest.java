package com.termux.app;

import com.termux.api.TermuxApiReceiver;
import com.termux.api.apis.ClipboardAPI;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClipboardApiTest {

    @Test
    public void clipboardApiAndReceiverClassesArePresent() {
        assertNotNull(TermuxApiReceiver.class);
        assertNotNull(ClipboardAPI.class);
    }

    @Test
    public void clipboardGetScriptUsesBuiltinHelperAndSupportsHelp() {
        String script = TermuxInstaller.getTermuxClipboardGetScriptContent();
        assertTrue(script.contains("libexec/termux-clipboard-api"));
        assertTrue(script.contains("Clipboard"));
        assertTrue(script.contains("SCRIPTNAME=termux-clipboard-get"));
        assertTrue(script.contains("Modern Termux"));
    }

    @Test
    public void clipboardSetScriptUsesApiVersion2AndSupportsStdinAndArgs() {
        String script = TermuxInstaller.getTermuxClipboardSetScriptContent();
        assertTrue(script.contains("libexec/termux-clipboard-api"));
        assertTrue(script.contains("Clipboard -e api_version 2 --ez set true"));
        assertTrue(script.contains("SCRIPTNAME=termux-clipboard-set"));
        assertTrue(script.contains("printf '%s' \"$*\""));
    }

    @Test
    public void managedClipboardScriptGuardAvoidsOverwritingExternalTermuxApiPackage() throws Exception {
        File tempDir = File.createTempFile("clip_bin", "");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        File missingScript = new File(tempDir, "termux-clipboard-get");
        assertTrue(TermuxInstaller.shouldWriteManagedClipboardScript(missingScript));

        File externalScript = new File(tempDir, "termux-clipboard-set");
        Files.write(externalScript.toPath(), "#!/bin/sh\n/data/data/com.termux/files/usr/libexec/termux-api Clipboard\n".getBytes(StandardCharsets.UTF_8));
        assertFalse(TermuxInstaller.shouldWriteManagedClipboardScript(externalScript));

        File managedScript = new File(tempDir, "termux-clipboard-managed");
        Files.write(managedScript.toPath(), TermuxInstaller.getTermuxClipboardGetScriptContent().getBytes(StandardCharsets.UTF_8));
        assertTrue(TermuxInstaller.shouldWriteManagedClipboardScript(managedScript));
    }
}
