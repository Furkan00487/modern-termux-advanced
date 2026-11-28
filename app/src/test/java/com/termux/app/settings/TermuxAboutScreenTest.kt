package com.termux.app.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.termux.R
import com.termux.app.activities.SettingsActivity
import com.termux.app.activities.SettingsSection
import com.termux.app.settings.data.SearchControlType
import com.termux.app.settings.data.SettingsSearchIndex
import com.termux.app.settings.data.TermuxSettingsUiState
import com.termux.app.settings.data.X11SettingsUiState
import com.termux.app.settings.navigation.SettingsNavHost
import com.termux.app.settings.navigation.SettingsRoute
import com.termux.app.settings.screens.TermuxWebViewerScreen
import com.termux.app.settings.theme.ModernTermuxTheme
import com.termux.shared.termux.TermuxConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TermuxAboutScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchIndexRoutesAboutToComposeScreen() {
        val context = RuntimeEnvironment.getApplication()
        val entry = SettingsSearchIndex.search(
            query = "about",
            context = context,
            termux = TermuxSettingsUiState(showDonate = false),
            x11 = X11SettingsUiState(),
        ).firstOrNull { it.key == "about" }

        assertNotNull(entry)
        assertEquals(SettingsRoute.TermuxAbout, entry?.targetRoute)
        assertEquals(SearchControlType.NAVIGATION, entry?.controlType)
    }

    @Test
    fun aboutScreenShowsHeroSystemReportDialogAndClearUpstreamLabels() {
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

        composeRule.onNodeWithTag("cat_termux_about")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithTag("about_hero_card").assertIsDisplayed()
        composeRule.onNodeWithTag("about_screen_list")
            .performScrollToNode(hasTestTag("about_view_system_report"))
        composeRule.onNodeWithTag("about_view_system_report").assertIsDisplayed()

        composeRule.onNodeWithTag("about_screen_list")
            .performScrollToNode(hasTestTag("about_report_issue"))
        composeRule.onNodeWithTag("about_report_issue").assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_about_report_issue, TermuxConstants.MODERN_TERMUX_VARIANT_NAME),
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("about_screen_list")
            .performScrollToNode(hasTestTag("about_upstream_issues_link"))
        composeRule.onNodeWithTag("about_upstream_issues_link").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_about_upstream_issues_title))
            .assertIsDisplayed()

        composeRule.onNodeWithTag("about_screen_list")
            .performScrollToNode(hasTestTag("about_view_system_report"))
        composeRule.onNodeWithTag("about_view_system_report").performClick()
        composeRule.onNodeWithTag("about_system_report_dialog").assertIsDisplayed()
    }

    @Test
    fun webViewerDisplaysTopRightOpenInBrowserAction() {
        val targetUrl = TermuxConstants.TERMUX_GITHUB_ISSUES_REPO_URL
        var openedExternally: String? = null

        composeRule.setContent {
            ModernTermuxTheme {
                TermuxWebViewerScreen(
                    initialTitle = "Termux Issues (Upstream)",
                    initialUrl = targetUrl,
                    onBack = {},
                    onOpenInBrowser = { openedExternally = it },
                )
            }
        }

        composeRule.onNodeWithTag("web_viewer_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("web_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("web_open_in_browser")
            .assertIsDisplayed()
            .performClick()

        assertEquals(targetUrl, openedExternally)
    }
}
