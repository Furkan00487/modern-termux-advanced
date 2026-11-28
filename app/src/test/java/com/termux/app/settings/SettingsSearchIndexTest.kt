package com.termux.app.settings

import android.content.Context
import com.termux.app.settings.data.PluginKind
import com.termux.app.settings.data.SettingsSearchIndex
import com.termux.app.settings.data.TermuxSettingsUiState
import com.termux.app.settings.data.X11SettingsUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsSearchIndexTest {

    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun hidesConditionallyInvisibleEntries() {
        val termux = TermuxSettingsUiState(showDonate = false, installedPlugins = emptyList())
        val x11 = X11SettingsUiState(
            hasHardwareAccelerationFeature = false,
            displayResolutionMode = "native",
            notificationPermissionVisible = false,
        )

        val entries = SettingsSearchIndex.buildVisibleEntries(context, termux, x11)
        val keys = entries.map { it.key }.toSet()

        assertFalse("displayScale should stay hidden in native mode", "displayScale" in keys)
        assertFalse("donate should stay hidden when showDonate is false", "donate" in keys)
        assertFalse("notification permission should stay hidden when granted", "requestNotificationPermission" in keys)
        assertFalse("hardwareAcceleration must not appear in standard build", "hardwareAcceleration" in keys)
    }

    @Test
    fun exposesScaleAndSearchFindsCrossSectionMatches() {
        val termux = TermuxSettingsUiState(showDonate = true)
        val x11 = X11SettingsUiState(
            hasHardwareAccelerationFeature = false,
            displayResolutionMode = "scaled",
            notificationPermissionVisible = true,
        )

        val scaleResults = SettingsSearchIndex.search("scale", context, termux, x11)
        assertTrue(scaleResults.any { it.key == "displayScale" })

        val logResults = SettingsSearchIndex.search("log", context, termux, x11)
        assertTrue(logResults.any { it.key == "log_level" })

        val clipResults = SettingsSearchIndex.search("clipboard", context, termux, x11)
        assertTrue(clipResults.any { it.key == "clipboardEnable" })
    }

    @Test
    fun includesInstalledPluginsOnlyWhenPresent() {
        val noPlugins = SettingsSearchIndex.search("api", context, TermuxSettingsUiState(), X11SettingsUiState())
        assertFalse(noPlugins.any { it.key == "plugin_api" })

        val withApi = TermuxSettingsUiState(
            installedPlugins = listOf(
                com.termux.app.settings.data.InstalledPluginUi(
                    kind = PluginKind.API,
                    titleRes = com.termux.R.string.termux_api_preferences_title,
                    summaryRes = com.termux.R.string.termux_api_preferences_summary,
                    logLevel = 1,
                ),
            ),
        )
        val found = SettingsSearchIndex.search("api", context, withApi, X11SettingsUiState())
        assertTrue(found.any { it.key == "plugin_api" })
    }

    @Test
    fun exposesHardwareAccelerationSearchInAdvanced() {
        val advanced = X11SettingsUiState(hasHardwareAccelerationFeature = true, hardwareAcceleration = true)
        val entries = SettingsSearchIndex.buildVisibleEntries(context, TermuxSettingsUiState(), advanced)
        assertTrue(entries.any { it.key == "hardwareAcceleration" })

        val gpuResults = SettingsSearchIndex.search("vulkan", context, TermuxSettingsUiState(), advanced)
        assertTrue(gpuResults.any { it.key == "hardwareAcceleration" })
    }

    @Test
    fun floatPluginExposesKeyLoggingAndHidesInternalAdditionalKbdVisible() {
        val withFloat = TermuxSettingsUiState(
            installedPlugins = listOf(
                com.termux.app.settings.data.InstalledPluginUi(
                    kind = PluginKind.FLOAT,
                    titleRes = com.termux.R.string.termux_float_preferences_title,
                    summaryRes = com.termux.R.string.termux_float_preferences_summary,
                    logLevel = 1,
                    supportsKeyLogging = true,
                    keyLoggingEnabled = true,
                ),
            ),
        )
        val entries = SettingsSearchIndex.buildVisibleEntries(
            context,
            withFloat,
            X11SettingsUiState(hasHardwareAccelerationFeature = true, displayResolutionMode = "native"),
        )
        assertTrue(entries.any { it.key == "plugin_float_key_logging" && it.currentBoolean == true })
        assertFalse("internal additionalKbdVisible must not appear in search", entries.any { it.key == "additionalKbdVisible" })

        val adjustEntry = entries.first { it.key == "adjustResolution" }
        val stretchEntry = entries.first { it.key == "displayStretch" }
        assertFalse("adjustResolution must be disabled in native mode", adjustEntry.enabled)
        assertFalse("displayStretch must be disabled in native mode", stretchEntry.enabled)
    }
}
