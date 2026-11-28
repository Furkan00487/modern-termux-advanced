package com.termux.app.settings

import android.content.Context
import com.termux.app.settings.data.AccessibilityToggleResult
import com.termux.app.settings.data.ResolutionUpdateResult
import com.termux.app.settings.data.X11SettingsRepository
import com.termux.app.settings.data.X11SettingsRules
import com.termux.x11.LorieApp
import com.termux.x11.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class X11SettingsRulesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(LorieApp.X11_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun displayScaleSnapsToMultiplesOfTen() {
        assertEquals(130, X11SettingsRules.snapDisplayScale(134))
        assertEquals(140, X11SettingsRules.snapDisplayScale(136))
        assertEquals(30, X11SettingsRules.snapDisplayScale(5))
        assertEquals(300, X11SettingsRules.snapDisplayScale(400))

        val repo = X11SettingsRepository(context)
        val saved = repo.setDisplayScale(143)
        assertEquals(140, saved)
        assertEquals(140, repo.state.value.displayScale)
    }

    @Test
    fun customResolutionRejectsInvalidFormatsAndPreservesExisting() {
        val repo = X11SettingsRepository(context)
        assertEquals(ResolutionUpdateResult.Saved, repo.setCustomResolution("1920x1080"))
        assertEquals("1920x1080", repo.state.value.displayResolutionCustom)

        assertEquals(ResolutionUpdateResult.InvalidFormat, repo.setCustomResolution(""))
        assertEquals(ResolutionUpdateResult.InvalidFormat, repo.setCustomResolution("1280"))
        assertEquals(ResolutionUpdateResult.InvalidFormat, repo.setCustomResolution("0x720"))
        assertEquals(ResolutionUpdateResult.InvalidFormat, repo.setCustomResolution("-1920x1080"))
        assertEquals(ResolutionUpdateResult.InvalidFormat, repo.setCustomResolution("abcxdef"))
        assertEquals("1920x1080", repo.state.value.displayResolutionCustom)
    }

    @Test
    fun mutualExclusionAndAccessibilityPermissionGuard() {
        val repo = X11SettingsRepository(context)
        assertTrue(repo.state.value.isDexCaptureEnabled)
        assertTrue(repo.state.value.isAutoAccessibilityEnabled)

        // Enabling without WRITE_SECURE_SETTINGS must be blocked safely.
        assertEquals(
            AccessibilityToggleResult.RequiresWriteSecureSettings,
            repo.setAccessibilityAuto(true),
        )
        assertFalse(repo.state.value.enableAccessibilityServiceAutomatically)

        repo.setBoolean("dexMetaKeyCapture", true)
        assertFalse(repo.state.value.isAutoAccessibilityEnabled)
        assertTrue(repo.state.value.isPauseEscEnabled)
    }

    @Test
    fun enablingShowAdditionalKbdForcesAdditionalKbdVisible() {
        val repo = X11SettingsRepository(context)
        repo.setBoolean("additionalKbdVisible", false)
        assertFalse(repo.state.value.additionalKbdVisible)

        repo.setBoolean("showAdditionalKbd", true)
        assertTrue(repo.state.value.showAdditionalKbd)
        assertTrue(repo.state.value.additionalKbdVisible)
    }

    @Test
    fun screenIdleTimeoutConflictDetection() {
        assertFalse(X11SettingsRules.hasScreenIdleTimeoutConflict("never", 600_000L))
        assertFalse(X11SettingsRules.hasScreenIdleTimeoutConflict("system", 600_000L))
        assertFalse(X11SettingsRules.hasScreenIdleTimeoutConflict("10", 60_000L))
        assertTrue(X11SettingsRules.hasScreenIdleTimeoutConflict("5", 600_000L))
    }

    @Test
    fun dependencyVisibilityAndEnablementRules() {
        assertFalse(X11SettingsRules.isDisplayScaleVisible("native"))
        assertTrue(X11SettingsRules.isDisplayScaleVisible("scaled"))
        assertTrue(X11SettingsRules.isDisplayStretchEnabled("exact"))
        assertTrue(X11SettingsRules.isDisplayStretchEnabled("custom"))
        assertFalse(X11SettingsRules.isDisplayStretchEnabled("scaled"))
        assertFalse(X11SettingsRules.isScaleTouchpadEnabled("1", "native"))
        assertTrue(X11SettingsRules.isScaleTouchpadEnabled("1", "scaled"))
        assertFalse(X11SettingsRules.isScaleTouchpadEnabled("2", "scaled"))
    }
}
