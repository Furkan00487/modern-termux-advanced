package com.termux.app.settings.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.termux.R
import com.termux.app.activities.SettingsSection
import com.termux.app.settings.components.ExpressiveActionRow
import com.termux.app.settings.components.ExpressiveNavRow
import com.termux.app.settings.components.SettingsGroupHeader
import com.termux.app.settings.components.SettingsRootTopBar
import com.termux.app.settings.components.SettingsSearchBar
import com.termux.app.settings.components.SettingsSearchResultsList
import com.termux.app.settings.components.SettingsSectionSwitcher
import com.termux.app.settings.components.SettingsSegmentedGroup
import com.termux.app.settings.data.SettingsSearchEntry
import com.termux.app.settings.data.TermuxSettingsUiState
import com.termux.app.settings.data.X11SettingsUiState
import com.termux.app.settings.navigation.SettingsRoute
import com.termux.x11.R as X11R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsRootListPane(
    selectedSection: SettingsSection,
    currentDetailRoute: SettingsRoute?,
    searchQuery: String,
    searchResults: List<SettingsSearchEntry>,
    termuxState: TermuxSettingsUiState,
    x11State: X11SettingsUiState,
    onSelectSection: (SettingsSection) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToDetail: (SettingsRoute) -> Unit,
    onSearchResultClick: (SettingsSearchEntry) -> Unit,
    onSearchResultToggleSwitch: (SettingsSearchEntry, Boolean) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDonate: () -> Unit,
    onCopyX11Version: (String) -> Unit,
    onNavigateUp: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsRootTopBar(
                scrollBehavior = scrollBehavior,
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "section_switcher") {
                SettingsSectionSwitcher(
                    selectedSection = selectedSection,
                    onSelectSection = onSelectSection,
                )
            }

            item(key = "search_bar") {
                SettingsSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                )
            }

            item(key = "main_body") {
                if (searchQuery.isNotBlank()) {
                    SettingsSearchResultsList(
                        query = searchQuery,
                        results = searchResults,
                        onResultClick = onSearchResultClick,
                        onResultToggleSwitch = onSearchResultToggleSwitch,
                    )
                } else {
                    AnimatedContent(
                        targetState = selectedSection,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "section_categories",
                    ) { section ->
                        when (section) {
                            SettingsSection.TERMUX -> TermuxCategoriesContent(
                                currentDetailRoute = currentDetailRoute,
                                termuxState = termuxState,
                                onNavigateToDetail = onNavigateToDetail,
                                onOpenAbout = onOpenAbout,
                                onOpenDonate = onOpenDonate,
                            )

                            SettingsSection.X11 -> X11CategoriesContent(
                                currentDetailRoute = currentDetailRoute,
                                x11State = x11State,
                                onNavigateToDetail = onNavigateToDetail,
                                onCopyX11Version = onCopyX11Version,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TermuxCategoriesContent(
    currentDetailRoute: SettingsRoute?,
    termuxState: TermuxSettingsUiState,
    onNavigateToDetail: (SettingsRoute) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenDonate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader(stringResource(R.string.settings_group_core))
            SettingsSegmentedGroup {
                ExpressiveNavRow(
                    title = stringResource(R.string.termux_debugging_preferences_title),
                    summary = stringResource(R.string.termux_debugging_preferences_summary),
                    icon = Icons.Rounded.BugReport,
                    selected = currentDetailRoute == SettingsRoute.TermuxDebugging,
                    index = 0,
                    count = 3,
                    onClick = { onNavigateToDetail(SettingsRoute.TermuxDebugging) },
                    modifier = Modifier.testTag("cat_termux_debugging"),
                )
                ExpressiveNavRow(
                    title = stringResource(R.string.termux_terminal_io_preferences_title),
                    summary = stringResource(R.string.termux_terminal_io_preferences_summary),
                    icon = Icons.Rounded.Keyboard,
                    selected = currentDetailRoute == SettingsRoute.TermuxTerminalIO,
                    index = 1,
                    count = 3,
                    onClick = { onNavigateToDetail(SettingsRoute.TermuxTerminalIO) },
                    modifier = Modifier.testTag("cat_termux_io"),
                )
                ExpressiveNavRow(
                    title = stringResource(R.string.termux_terminal_view_preferences_title),
                    summary = stringResource(R.string.termux_terminal_view_preferences_summary),
                    icon = Icons.Rounded.ViewAgenda,
                    selected = currentDetailRoute == SettingsRoute.TermuxTerminalView,
                    index = 2,
                    count = 3,
                    onClick = { onNavigateToDetail(SettingsRoute.TermuxTerminalView) },
                    modifier = Modifier.testTag("cat_termux_view"),
                )
            }
        }

        if (termuxState.installedPlugins.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsGroupHeader(stringResource(R.string.settings_group_plugins))
                SettingsSegmentedGroup {
                    val count = termuxState.installedPlugins.size
                    termuxState.installedPlugins.forEachIndexed { idx, plugin ->
                        val route = SettingsRoute.TermuxPlugin(plugin.kind)
                        ExpressiveNavRow(
                            title = stringResource(plugin.titleRes),
                            summary = stringResource(plugin.summaryRes),
                            icon = Icons.Rounded.Extension,
                            selected = currentDetailRoute == route,
                            index = idx,
                            count = count,
                            onClick = { onNavigateToDetail(route) },
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader(stringResource(R.string.settings_group_system))
            SettingsSegmentedGroup {
                val systemCount = if (termuxState.showDonate) 2 else 1
                ExpressiveActionRow(
                    title = stringResource(R.string.about_preference_title),
                    summary = stringResource(R.string.settings_about_summary),
                    icon = Icons.Rounded.Info,
                    index = 0,
                    count = systemCount,
                    onClick = onOpenAbout,
                    modifier = Modifier.testTag("action_termux_about"),
                )
                if (termuxState.showDonate) {
                    ExpressiveActionRow(
                        title = stringResource(R.string.donate_preference_title),
                        summary = stringResource(R.string.settings_donate_summary),
                        icon = Icons.Rounded.Favorite,
                        index = 1,
                        count = systemCount,
                        onClick = onOpenDonate,
                        modifier = Modifier.testTag("action_termux_donate"),
                    )
                }
            }
        }
    }
}

@Composable
private fun X11CategoriesContent(
    currentDetailRoute: SettingsRoute?,
    x11State: X11SettingsUiState,
    onNavigateToDetail: (SettingsRoute) -> Unit,
    onCopyX11Version: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader(stringResource(R.string.settings_group_x11_categories))
            SettingsSegmentedGroup {
                ExpressiveNavRow(
                    title = stringResource(X11R.string.lorie_pref_output),
                    summary = stringResource(R.string.settings_x11_output_summary),
                    icon = Icons.Rounded.DisplaySettings,
                    selected = currentDetailRoute == SettingsRoute.X11Output,
                    index = 0,
                    count = 5,
                    onClick = { onNavigateToDetail(SettingsRoute.X11Output) },
                    modifier = Modifier.testTag("cat_x11_output"),
                )
                ExpressiveNavRow(
                    title = stringResource(X11R.string.lorie_pref_pointer),
                    summary = stringResource(R.string.settings_x11_pointer_summary),
                    icon = Icons.Rounded.Mouse,
                    selected = currentDetailRoute == SettingsRoute.X11Pointer,
                    index = 1,
                    count = 5,
                    onClick = { onNavigateToDetail(SettingsRoute.X11Pointer) },
                    modifier = Modifier.testTag("cat_x11_pointer"),
                )
                ExpressiveNavRow(
                    title = stringResource(X11R.string.lorie_pref_kbd),
                    summary = stringResource(R.string.settings_x11_kbd_summary),
                    icon = Icons.Rounded.Keyboard,
                    selected = currentDetailRoute == SettingsRoute.X11Keyboard,
                    index = 2,
                    count = 5,
                    onClick = { onNavigateToDetail(SettingsRoute.X11Keyboard) },
                    modifier = Modifier.testTag("cat_x11_kbd"),
                )
                ExpressiveNavRow(
                    title = stringResource(X11R.string.lorie_pref_ekbar),
                    summary = "${x11State.labelForChoice("ekbarPosition", x11State.ekbarPosition)} · ${x11State.opacityEKBar}%",
                    icon = Icons.Rounded.Tune,
                    selected = currentDetailRoute == SettingsRoute.X11ExtraKeysBar,
                    index = 3,
                    count = 5,
                    onClick = { onNavigateToDetail(SettingsRoute.X11ExtraKeysBar) },
                    modifier = Modifier.testTag("cat_x11_ekbar"),
                )
                ExpressiveNavRow(
                    title = stringResource(X11R.string.lorie_pref_other),
                    summary = stringResource(R.string.settings_x11_other_summary),
                    icon = Icons.Rounded.SettingsApplications,
                    selected = currentDetailRoute == SettingsRoute.X11Other ||
                        currentDetailRoute == SettingsRoute.X11UserActions,
                    index = 4,
                    count = 5,
                    onClick = { onNavigateToDetail(SettingsRoute.X11Other) },
                    modifier = Modifier.testTag("cat_x11_other"),
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsGroupHeader(stringResource(R.string.settings_group_x11_info))
            SettingsSegmentedGroup {
                ExpressiveActionRow(
                    title = stringResource(X11R.string.lorie_pref_version),
                    valueBadge = x11State.x11VersionName,
                    icon = Icons.Rounded.Verified,
                    index = 0,
                    count = 1,
                    onClick = { onCopyX11Version(x11State.x11VersionName) },
                    modifier = Modifier.testTag("action_x11_version"),
                )
            }
        }
    }
}

@Composable
fun SettingsDetailPlaceholderPane(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_detail_placeholder_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.settings_detail_placeholder_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
