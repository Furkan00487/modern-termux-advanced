package com.termux.app.settings

import android.content.ComponentName
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.termux.R
import com.termux.app.activities.SettingsActivity
import com.termux.app.activities.SettingsSection
import com.termux.app.settings.navigation.SettingsNavHost
import com.termux.app.settings.theme.ModernTermuxTheme
import com.termux.x11.R as X11R
import com.termux.x11.X11PreferenceBridge
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsNavHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchBetweenTermuxAndX11SectionsAndNavigateDetail() {
        val context = RuntimeEnvironment.getApplication()
        composeRule.setContent {
            ModernTermuxTheme {
                SettingsNavHost(
                    launchTarget = SettingsActivity.SettingsLaunchTarget(
                        section = SettingsSection.TERMUX,
                        subscreen = null,
                        displayId = 0,
                    ),
                    onNavigateUp = {},
                )
            }
        }

        // Terminal root shows Debugging, Terminal I/O, Terminal View
        composeRule.onNodeWithText(context.getString(R.string.termux_debugging_preferences_title))
            .assertIsDisplayed()

        // Switch cleanly to X11 Desktop section
        composeRule.onNodeWithTag("section_tab_x11").performClick()
        composeRule.onNodeWithTag("cat_x11_output").assertIsDisplayed()

        // Navigate into X11 Output detail screen
        composeRule.onNodeWithTag("cat_x11_output").performClick()
        composeRule.onNodeWithText(context.getString(X11R.string.lorie_pref_displayResolutionMode))
            .assertIsDisplayed()
    }

    @Test
    fun searchFiltersAcrossSectionsAndDisplaysDirectMatches() {
        val context = RuntimeEnvironment.getApplication()
        composeRule.setContent {
            ModernTermuxTheme {
                SettingsNavHost(
                    launchTarget = SettingsActivity.SettingsLaunchTarget(
                        section = SettingsSection.TERMUX,
                        subscreen = null,
                        displayId = 0,
                    ),
                    onNavigateUp = {},
                )
            }
        }

        composeRule.onNodeWithTag("settings_search_input").performTextInput("clipboard")
        composeRule.onNodeWithTag("search_result_clipboardEnable").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(X11R.string.lorie_pref_clipboardEnable))
            .assertIsDisplayed()
    }

    @Test
    fun resolveLaunchTargetHonorsX11AliasAndExtras() {
        val context = RuntimeEnvironment.getApplication()
        val aliasIntent = Intent(X11PreferenceBridge.ACTION_OPEN_SETTINGS).apply {
            component = ComponentName(context.packageName, X11PreferenceBridge.X11_SETTINGS_ALIAS_CLASS)
            putExtra(X11PreferenceBridge.EXTRA_INITIAL_SUBSCREEN, "ekbar")
            putExtra(X11PreferenceBridge.EXTRA_DISPLAY_ID, 2)
        }
        val x11Target = SettingsActivity.resolveLaunchTarget(aliasIntent)
        assertEquals(SettingsSection.X11, x11Target.section)
        assertEquals("ekbar", x11Target.subscreen)
        assertEquals(2, x11Target.displayId)

        val defaultTarget = SettingsActivity.resolveLaunchTarget(Intent())
        assertEquals(SettingsSection.TERMUX, defaultTarget.section)
    }
}
