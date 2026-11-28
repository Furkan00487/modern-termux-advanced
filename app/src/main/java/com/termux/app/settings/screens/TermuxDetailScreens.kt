package com.termux.app.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.termux.R
import com.termux.app.settings.components.ChoicePickerDialog
import com.termux.app.settings.components.ExpressiveChoiceRow
import com.termux.app.settings.components.ExpressiveSwitchRow
import com.termux.app.settings.components.SettingsDetailTopBar
import com.termux.app.settings.components.SettingsGroupHeader
import com.termux.app.settings.components.SettingsSegmentedGroup
import com.termux.app.settings.data.InstalledPluginUi
import com.termux.app.settings.data.PluginKind
import com.termux.app.settings.data.TermuxSettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxDebuggingScreen(
    state: TermuxSettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetLogLevel: (Int) -> Unit,
    onSetKeyLogging: (Boolean) -> Unit,
    onSetPluginErrorNotifications: (Boolean) -> Unit,
    onSetCrashReportNotifications: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showLogLevelDialog by remember { mutableStateOf(false) }

    val currentLogLabel = state.logLevelOptions
        .firstOrNull { it.value == state.logLevel.toString() }
        ?.label
        ?: state.logLevel.toString()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(R.string.termux_debugging_preferences_title),
                sectionBadge = stringResource(R.string.settings_section_termux),
                showBackButton = showBackButton,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "logging_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.termux_logging_header))
                    SettingsSegmentedGroup {
                        ExpressiveChoiceRow(
                            title = stringResource(R.string.termux_log_level_title),
                            currentLabel = currentLogLabel,
                            icon = Icons.Rounded.Description,
                            index = 0,
                            count = 4,
                            onClick = { showLogLevelDialog = true },
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(R.string.termux_terminal_view_key_logging_enabled_title),
                            summary = stringResource(
                                if (state.terminalViewKeyLogging) {
                                    R.string.termux_terminal_view_key_logging_enabled_on
                                } else {
                                    R.string.termux_terminal_view_key_logging_enabled_off
                                },
                            ),
                            checked = state.terminalViewKeyLogging,
                            onCheckedChange = onSetKeyLogging,
                            icon = Icons.Rounded.BugReport,
                            index = 1,
                            count = 4,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(R.string.termux_plugin_error_notifications_enabled_title),
                            summary = stringResource(
                                if (state.pluginErrorNotifications) {
                                    R.string.termux_plugin_error_notifications_enabled_on
                                } else {
                                    R.string.termux_plugin_error_notifications_enabled_off
                                },
                            ),
                            checked = state.pluginErrorNotifications,
                            onCheckedChange = onSetPluginErrorNotifications,
                            icon = Icons.Rounded.Notifications,
                            index = 2,
                            count = 4,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(R.string.termux_crash_report_notifications_enabled_title),
                            summary = stringResource(
                                if (state.crashReportNotifications) {
                                    R.string.termux_crash_report_notifications_enabled_on
                                } else {
                                    R.string.termux_crash_report_notifications_enabled_off
                                },
                            ),
                            checked = state.crashReportNotifications,
                            onCheckedChange = onSetCrashReportNotifications,
                            icon = Icons.Rounded.ErrorOutline,
                            index = 3,
                            count = 4,
                        )
                    }
                }
            }
        }
    }

    if (showLogLevelDialog) {
        ChoicePickerDialog(
            title = stringResource(R.string.termux_log_level_title),
            options = state.logLevelOptions,
            selectedValue = state.logLevel.toString(),
            onSelect = { selected ->
                selected.toIntOrNull()?.let(onSetLogLevel)
            },
            onDismiss = { showLogLevelDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxTerminalIOScreen(
    state: TermuxSettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetSoftKeyboardEnabled: (Boolean) -> Unit,
    onSetSoftKeyboardOnlyIfNoHardware: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(R.string.termux_terminal_io_preferences_title),
                sectionBadge = stringResource(R.string.settings_section_termux),
                showBackButton = showBackButton,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "keyboard_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.termux_keyboard_header))
                    SettingsSegmentedGroup {
                        ExpressiveSwitchRow(
                            title = stringResource(R.string.termux_soft_keyboard_enabled_title),
                            summary = stringResource(
                                if (state.softKeyboardEnabled) {
                                    R.string.termux_soft_keyboard_enabled_on
                                } else {
                                    R.string.termux_soft_keyboard_enabled_off
                                },
                            ),
                            checked = state.softKeyboardEnabled,
                            onCheckedChange = onSetSoftKeyboardEnabled,
                            icon = Icons.Rounded.Keyboard,
                            index = 0,
                            count = 2,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(R.string.termux_soft_keyboard_enabled_only_if_no_hardware_title),
                            summary = stringResource(
                                if (state.softKeyboardOnlyIfNoHardware) {
                                    R.string.termux_soft_keyboard_enabled_only_if_no_hardware_on
                                } else {
                                    R.string.termux_soft_keyboard_enabled_only_if_no_hardware_off
                                },
                            ),
                            checked = state.softKeyboardOnlyIfNoHardware,
                            onCheckedChange = onSetSoftKeyboardOnlyIfNoHardware,
                            icon = Icons.Rounded.Keyboard,
                            index = 1,
                            count = 2,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxTerminalViewScreen(
    state: TermuxSettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetTerminalMarginAdjustment: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(R.string.termux_terminal_view_preferences_title),
                sectionBadge = stringResource(R.string.settings_section_termux),
                showBackButton = showBackButton,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "view_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.termux_terminal_view_view_header))
                    SettingsSegmentedGroup {
                        ExpressiveSwitchRow(
                            title = stringResource(R.string.termux_terminal_view_terminal_margin_adjustment_title),
                            summary = stringResource(
                                if (state.terminalMarginAdjustment) {
                                    R.string.termux_terminal_view_terminal_margin_adjustment_on
                                } else {
                                    R.string.termux_terminal_view_terminal_margin_adjustment_off
                                },
                            ),
                            checked = state.terminalMarginAdjustment,
                            onCheckedChange = onSetTerminalMarginAdjustment,
                            icon = Icons.Rounded.AspectRatio,
                            index = 0,
                            count = 1,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxPluginScreen(
    pluginKind: PluginKind,
    state: TermuxSettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetPluginLogLevel: (PluginKind, Int) -> Unit,
    onSetPluginKeyLogging: (PluginKind, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showLogDialog by remember { mutableStateOf(false) }

    val plugin: InstalledPluginUi? = state.installedPlugins.firstOrNull { it.kind == pluginKind }
    val titleRes = plugin?.titleRes ?: when (pluginKind) {
        PluginKind.API -> R.string.termux_api_preferences_title
        PluginKind.FLOAT -> R.string.termux_float_preferences_title
        PluginKind.TASKER -> R.string.termux_tasker_preferences_title
        PluginKind.WIDGET -> R.string.termux_widget_preferences_title
    }
    val currentLevel = plugin?.logLevel ?: 1
    val currentLabel = state.logLevelOptions
        .firstOrNull { it.value == currentLevel.toString() }
        ?.label
        ?: currentLevel.toString()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(titleRes),
                sectionBadge = stringResource(R.string.settings_group_plugins),
                showBackButton = showBackButton,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "plugin_logging") {
                val itemCount = if (plugin?.supportsKeyLogging == true) 2 else 1
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.termux_logging_header))
                    SettingsSegmentedGroup {
                        ExpressiveChoiceRow(
                            title = stringResource(R.string.termux_log_level_title),
                            currentLabel = currentLabel,
                            icon = Icons.Rounded.Extension,
                            index = 0,
                            count = itemCount,
                            onClick = { showLogDialog = true },
                        )
                        if (plugin?.supportsKeyLogging == true) {
                            ExpressiveSwitchRow(
                                title = stringResource(R.string.termux_terminal_view_key_logging_enabled_title),
                                summary = stringResource(
                                    if (plugin.keyLoggingEnabled) {
                                        R.string.termux_terminal_view_key_logging_enabled_on
                                    } else {
                                        R.string.termux_terminal_view_key_logging_enabled_off
                                    },
                                ),
                                checked = plugin.keyLoggingEnabled,
                                onCheckedChange = { onSetPluginKeyLogging(pluginKind, it) },
                                icon = Icons.Rounded.BugReport,
                                index = 1,
                                count = itemCount,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        ChoicePickerDialog(
            title = stringResource(R.string.termux_log_level_title),
            options = state.logLevelOptions,
            selectedValue = currentLevel.toString(),
            onSelect = { selected ->
                selected.toIntOrNull()?.let { onSetPluginLogLevel(pluginKind, it) }
            },
            onDismiss = { showLogDialog = false },
        )
    }
}
