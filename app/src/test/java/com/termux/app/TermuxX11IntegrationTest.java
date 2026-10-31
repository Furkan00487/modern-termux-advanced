package com.termux.app;

import com.termux.x11.CmdEntryPoint;
import com.termux.x11.LorieApp;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class TermuxX11IntegrationTest {
    @Test
    public void testLorieAppUsesIsolatedPreferencesName() {
        assertEquals("com.termux.x11_preferences", LorieApp.X11_PREFS_NAME);
    }

    @Test
    public void testResolveNativeLibraryPathPrefersExistingSymlinkOrNativeDir() throws Exception {
        File tempLib = File.createTempFile("libXlorie", ".so");
        tempLib.deleteOnExit();
        String resolved = CmdEntryPoint.resolveNativeLibraryPathFromCandidates(
            tempLib.getAbsolutePath(), "/nonexistent/libXlorie.so", null
        );
        assertEquals(tempLib.getAbsolutePath(), resolved);
    }

    @Test
    public void testTermuxX11ScriptDoesNotRequireLoaderApkAndUsesCmdEntryPoint() {
        String script = TermuxInstaller.getTermuxX11ScriptContent();
        assertFalse(script.contains("loader.apk"));
        assertTrue(script.contains("com.termux.x11.CmdEntryPoint"));
        assertTrue(script.contains("/data/data/com.termux/files/usr/libexec/termux-x11/base.apk"));
        assertTrue(script.contains("pm path com.termux"));
    }

    @Test
    public void testTermuxX11PreferenceScriptUsesDirectReceiverMain() {
        String script = TermuxInstaller.getTermuxX11PreferenceScriptContent();
        assertFalse(script.contains("loader.apk"));
        assertTrue(script.contains("com.termux.x11.LoriePreferences\\$Receiver"));
        assertTrue(script.contains("-p\" \"com.termux\""));
    }

    @Test
    public void testTermuxApplicationExtendsLorieApp() {
        assertTrue(LorieApp.class.isAssignableFrom(TermuxApplication.class));
    }

    @Test
    public void testModernTermuxMotdContentContainsBrandAndCommands() {
        String motd = TermuxInstaller.getModernTermuxMotdContent();
        assertTrue(motd.contains("MODERN TERMUX'A HOŞ GELDİN!"));
        assertTrue(motd.contains("pkg search <sorgu>"));
        assertTrue(motd.contains("pkg install <paket>"));
        assertTrue(motd.contains("https://github.com/Furkan00487/modern-termux"));
        assertFalse(motd.contains("Welcome to Termux!"));
        assertFalse(motd.contains("pkg install x11-repo"));
    }

    @Test
    public void testEnsureModernTermuxMotdFileUpdatesLegacyMotd() throws Exception {
        File tempMotd = File.createTempFile("test_motd", ".txt");
        tempMotd.deleteOnExit();

        String legacyMotd = "Welcome to Termux!\nDocs: https://termux.dev/docs\nSubscribing to additional repositories:\n - X11: pkg install x11-repo\n";
        java.nio.file.Files.write(tempMotd.toPath(), legacyMotd.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        TermuxInstaller.ensureModernTermuxMotdFile(tempMotd);

        String updated = new String(java.nio.file.Files.readAllBytes(tempMotd.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(updated.contains("MODERN TERMUX'A HOŞ GELDİN!"));
        assertFalse(updated.contains("Welcome to Termux!"));
        assertFalse(updated.contains("pkg install x11-repo"));
    }

    @Test
    public void testEnsureModernTermuxMotdFileCreatesIfMissing() throws Exception {
        File tempDir = File.createTempFile("test_etc", "");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();

        File missingMotd = new File(tempDir, "motd");
        TermuxInstaller.ensureModernTermuxMotdFile(missingMotd);

        assertTrue(missingMotd.exists());
        String content = new String(java.nio.file.Files.readAllBytes(missingMotd.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("MODERN TERMUX'A HOŞ GELDİN!"));
    }

    @Test
    public void testNotificationDrawablesExist() {
        assertTrue(com.termux.R.drawable.ic_service_notification != 0);
        assertTrue(com.termux.x11.R.drawable.ic_x11_icon != 0);
    }

    @Test
    public void testDynamicX11NotificationIconState() {
        assertFalse(TermuxService.isX11Active());
        TermuxService.setX11Active(true, null);
        assertTrue(TermuxService.isX11Active());
        TermuxService.setX11Active(false, null);
        assertFalse(TermuxService.isX11Active());
    }

    @Test
    public void testTermuxMotdRefreshScriptContent() {
        String script = TermuxInstaller.getTermuxMotdRefreshScriptContent();
        assertTrue(script.contains("com.termux.motd"));
        assertTrue(script.contains("[HATA]"));
        assertTrue(script.contains("dyn-motd.sh"));
        assertTrue(script.contains("exit 1"));
    }

    @Test
    public void testHardwareAccelerationPreferenceDefinitionInXml() throws Exception {
        File prefXml = new File("../x11/src/main/res/xml/preferences.xml");
        if (!prefXml.exists()) prefXml = new File("x11/src/main/res/xml/preferences.xml");
        assertTrue("preferences.xml exists", prefXml.exists());
        String content = new String(java.nio.file.Files.readAllBytes(prefXml.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("hardwareAcceleration"));
        assertTrue(content.contains("app:defaultValue=\"true\""));
        assertTrue(content.contains("@string/lorie_pref_hardware_acceleration"));
    }

    @Test
    public void testHardwareAccelerationFlagPathConstant() {
        assertEquals("/data/data/com.termux/files/usr/tmp/.lorie_hwaccel", com.termux.x11.LoriePreferences.HW_ACCEL_FLAG_PATH);
    }

    @Test
    public void testTermuxX11EnvScriptContentContainsZinkAndFallback() {
        String script = TermuxInstaller.getTermuxX11EnvScriptContent();
        assertTrue(script.contains("GALLIUM_DRIVER=zink"));
        assertTrue(script.contains("MESA_LOADER_DRIVER_OVERRIDE=zink"));
        assertTrue(script.contains("/system/lib64/libvulkan.so"));
        assertTrue(script.contains(".lorie_hwaccel"));
        assertTrue(script.contains("llvmpipe"));
    }

    @Test
    public void testModernTermuxX11ProfileContentAutoInjectsWhenX11Active() {
        String profile = TermuxInstaller.getModernTermuxX11ProfileContent();
        assertTrue(profile.contains("GALLIUM_DRIVER=zink"));
        assertTrue(profile.contains("MESA_LOADER_DRIVER_OVERRIDE=zink"));
        assertTrue(profile.contains(".lorie_hwaccel"));
    }
}
