package com.termux.app.settings.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ZoomIn
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
import com.termux.app.settings.components.CustomResolutionDialog
import com.termux.app.settings.components.ExpressiveActionRow
import com.termux.app.settings.components.ExpressiveChoiceRow
import com.termux.app.settings.components.ExpressiveNavRow
import com.termux.app.settings.components.ExpressiveSliderRow
import com.termux.app.settings.components.ExpressiveSwitchRow
import com.termux.app.settings.components.ExtraKeysConfigDialog
import com.termux.app.settings.components.SettingsDetailTopBar
import com.termux.app.settings.components.SettingsGroupHeader
import com.termux.app.settings.components.SettingsInfoBanner
import com.termux.app.settings.components.SettingsSegmentedGroup
import com.termux.app.settings.components.WriteSecureSettingsDialog
import com.termux.app.settings.data.AccessibilityToggleResult
import com.termux.app.settings.data.ResolutionUpdateResult
import com.termux.app.settings.data.X11SettingsRules
import com.termux.app.settings.data.X11SettingsUiState
import com.termux.x11.R as X11R

private data class ChoiceDialogState(
    val key: String,
    val title: String,
    val selectedValue: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X11OutputScreen(
    state: X11SettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetBoolean: (String, Boolean) -> Unit,
    onSetList: (String, String) -> Unit,
    onSetDisplayScale: (Int) -> Int,
    onSetCustomResolution: (String) -> ResolutionUpdateResult,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var activeChoice by remember { mutableStateOf<ChoiceDialogState?>(null) }
    var showCustomResDialog by remember { mutableStateOf(false) }

    val displayItemsCount = buildList {
        if (state.hasHardwareAccelerationFeature) add("hardwareAcceleration")
        add("displayResolutionMode")
        if (state.isDisplayScaleVisible) add("displayScale")
        if (state.isDisplayExactVisible) add("displayResolutionExact")
        if (state.isDisplayCustomVisible) add("displayResolutionCustom")
        add("displayFilteringMode")
        add("adjustResolution")
        add("displayStretch")
    }.size
    val windowItemsCount = if (state.hideCutoutAvailable) 6 else 5

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(X11R.string.lorie_pref_output),
                sectionBadge = stringResource(R.string.settings_section_x11),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "display_scaling_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_display))
                    SettingsSegmentedGroup {
                        var idx = 0
                        if (state.hasHardwareAccelerationFeature) {
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_hardware_acceleration),
                                summary = stringResource(X11R.string.lorie_pref_hardware_acceleration_summary),
                                checked = state.hardwareAcceleration,
                                onCheckedChange = { onSetBoolean("hardwareAcceleration", it) },
                                icon = Icons.Rounded.Speed,
                                index = idx++,
                                count = displayItemsCount,
                            )
                        }
                        val modeTitle = stringResource(X11R.string.lorie_pref_displayResolutionMode)
                        ExpressiveChoiceRow(
                            title = modeTitle,
                            currentLabel = state.labelForChoice("displayResolutionMode", state.displayResolutionMode),
                            icon = Icons.Rounded.DisplaySettings,
                            index = idx++,
                            count = displayItemsCount,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "displayResolutionMode",
                                    title = modeTitle,
                                    selectedValue = state.displayResolutionMode,
                                )
                            },
                        )
                        if (state.isDisplayScaleVisible) {
                            ExpressiveSliderRow(
                                title = stringResource(X11R.string.lorie_pref_displayScale),
                                value = state.displayScale,
                                valueRange = X11SettingsRules.DISPLAY_SCALE_MIN..X11SettingsRules.DISPLAY_SCALE_MAX,
                                step = 10,
                                onValueCommitted = { onSetDisplayScale(it) },
                                snapTransform = X11SettingsRules::snapDisplayScale,
                                index = idx++,
                                count = displayItemsCount,
                            )
                        }
                        if (state.isDisplayExactVisible) {
                            val exactTitle = stringResource(X11R.string.lorie_pref_displayResolutionExact)
                            ExpressiveChoiceRow(
                                title = exactTitle,
                                currentLabel = state.labelForChoice("displayResolutionExact", state.displayResolutionExact),
                                icon = Icons.Rounded.AspectRatio,
                                index = idx++,
                                count = displayItemsCount,
                                onClick = {
                                    activeChoice = ChoiceDialogState(
                                        key = "displayResolutionExact",
                                        title = exactTitle,
                                        selectedValue = state.displayResolutionExact,
                                    )
                                },
                            )
                        }
                        if (state.isDisplayCustomVisible) {
                            ExpressiveActionRow(
                                title = stringResource(X11R.string.lorie_pref_displayResolutionCustom),
                                valueBadge = state.displayResolutionCustom,
                                icon = Icons.Rounded.Edit,
                                index = idx++,
                                count = displayItemsCount,
                                onClick = { showCustomResDialog = true },
                            )
                        }
                        val filteringTitle = stringResource(X11R.string.lorie_pref_displayFilteringMode)
                        ExpressiveChoiceRow(
                            title = filteringTitle,
                            currentLabel = state.labelForChoice("displayFilteringMode", state.displayFilteringMode),
                            icon = Icons.Rounded.Tune,
                            index = idx++,
                            count = displayItemsCount,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "displayFilteringMode",
                                    title = filteringTitle,
                                    selectedValue = state.displayFilteringMode,
                                )
                            },
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_adjustResolution),
                            summary = if (!state.isDisplayStretchEnabled) {
                                stringResource(X11R.string.lorie_pref_summary_requiresExactOrCustom)
                            } else {
                                null
                            },
                            checked = state.adjustResolution,
                            enabled = state.isDisplayStretchEnabled,
                            onCheckedChange = { onSetBoolean("adjustResolution", it) },
                            icon = Icons.Rounded.ScreenRotation,
                            index = idx++,
                            count = displayItemsCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_displayStretch),
                            summary = if (!state.isDisplayStretchEnabled) {
                                stringResource(X11R.string.lorie_pref_summary_requiresExactOrCustom)
                            } else {
                                null
                            },
                            checked = state.displayStretch,
                            enabled = state.isDisplayStretchEnabled,
                            onCheckedChange = { onSetBoolean("displayStretch", it) },
                            icon = Icons.Rounded.AspectRatio,
                            index = idx,
                            count = displayItemsCount,
                        )
                    }
                }
            }

            item(key = "window_power_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_window))
                    SettingsSegmentedGroup {
                        var idx = 0
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_Reseed),
                            summary = stringResource(X11R.string.lorie_pref_Reseed_summary),
                            checked = state.reseed,
                            onCheckedChange = { onSetBoolean("Reseed", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = idx++,
                            count = windowItemsCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_PIP),
                            summary = stringResource(X11R.string.lorie_pref_PIP_summary),
                            checked = state.pip,
                            onCheckedChange = { onSetBoolean("PIP", it) },
                            icon = Icons.Rounded.PictureInPictureAlt,
                            index = idx++,
                            count = windowItemsCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_fullscreen),
                            summary = stringResource(X11R.string.lorie_pref_fullscreen_summary),
                            checked = state.fullscreen,
                            onCheckedChange = { onSetBoolean("fullscreen", it) },
                            icon = Icons.Rounded.Fullscreen,
                            index = idx++,
                            count = windowItemsCount,
                        )
                        val orientationTitle = stringResource(X11R.string.lorie_pref_forceOrientation)
                        ExpressiveChoiceRow(
                            title = orientationTitle,
                            currentLabel = state.labelForChoice("forceOrientation", state.forceOrientation),
                            icon = Icons.Rounded.ScreenRotation,
                            index = idx++,
                            count = windowItemsCount,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "forceOrientation",
                                    title = orientationTitle,
                                    selectedValue = state.forceOrientation,
                                )
                            },
                        )
                        if (state.hideCutoutAvailable) {
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_hideCutout),
                                checked = state.hideCutout,
                                onCheckedChange = { onSetBoolean("hideCutout", it) },
                                icon = Icons.Rounded.Fullscreen,
                                index = idx++,
                                count = windowItemsCount,
                            )
                        }
                        val timeoutTitle = stringResource(X11R.string.lorie_pref_screenIdleTimeout)
                        ExpressiveChoiceRow(
                            title = timeoutTitle,
                            currentLabel = state.labelForChoice("screenIdleTimeout", state.screenIdleTimeout),
                            icon = Icons.Rounded.Timer,
                            index = idx,
                            count = windowItemsCount,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "screenIdleTimeout",
                                    title = timeoutTitle,
                                    selectedValue = state.screenIdleTimeout,
                                )
                            },
                        )
                    }
                }
            }

            if (state.hasScreenIdleConflict) {
                item(key = "screen_idle_conflict_warning") {
                    SettingsInfoBanner(
                        title = stringResource(X11R.string.lorie_pref_screenIdleTimeout),
                        message = stringResource(
                            X11R.string.lorie_pref_summary_screenIdleTimeoutConflict,
                            state.systemTimeoutFormatted,
                        ),
                        icon = Icons.Rounded.Warning,
                        isWarning = true,
                    )
                }
            }
        }
    }

    activeChoice?.let { choice ->
        ChoicePickerDialog(
            title = choice.title,
            options = state.choiceOptionsByKey[choice.key].orEmpty(),
            selectedValue = choice.selectedValue,
            onSelect = { selected -> onSetList(choice.key, selected) },
            onDismiss = { activeChoice = null },
        )
    }

    if (showCustomResDialog) {
        CustomResolutionDialog(
            initialValue = state.displayResolutionCustom,
            onSave = { onSetCustomResolution(it) == ResolutionUpdateResult.Saved },
            onDismiss = { showCustomResDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X11PointerScreen(
    state: X11SettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetBoolean: (String, Boolean) -> Unit,
    onSetList: (String, String) -> Unit,
    onSetInt: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var activeChoice by remember { mutableStateOf<ChoiceDialogState?>(null) }

    val touchCount = 3 + if (state.stylusAvailable) 3 else 0
    val mouseCount = 5 + if (state.pointerCaptureAvailable) 1 else 0

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(X11R.string.lorie_pref_pointer),
                sectionBadge = stringResource(R.string.settings_section_x11),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "touch_stylus_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_touch))
                    SettingsSegmentedGroup {
                        var idx = 0
                        val touchTitle = stringResource(X11R.string.lorie_pref_touchMode)
                        ExpressiveChoiceRow(
                            title = touchTitle,
                            currentLabel = state.labelForChoice("touchMode", state.touchMode),
                            icon = Icons.Rounded.TouchApp,
                            index = idx++,
                            count = touchCount,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "touchMode",
                                    title = touchTitle,
                                    selectedValue = state.touchMode,
                                )
                            },
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_scaleTouchpad),
                            summary = if (!state.isScaleTouchpadEnabled) {
                                stringResource(X11R.string.lorie_pref_summary_requiresTrackpadAndNative)
                            } else {
                                null
                            },
                            checked = state.scaleTouchpad,
                            enabled = state.isScaleTouchpadEnabled,
                            onCheckedChange = { onSetBoolean("scaleTouchpad", it) },
                            icon = Icons.Rounded.AspectRatio,
                            index = idx++,
                            count = touchCount,
                        )
                        if (state.stylusAvailable) {
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_showStylusClickOverride),
                                summary = stringResource(X11R.string.lorie_pref_showStylusClickOverride_summary),
                                checked = state.showStylusClickOverride,
                                onCheckedChange = { onSetBoolean("showStylusClickOverride", it) },
                                icon = Icons.Rounded.Draw,
                                index = idx++,
                                count = touchCount,
                            )
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_stylusIsMouse),
                                summary = stringResource(X11R.string.lorie_pref_stylusIsMouse_summary),
                                checked = state.stylusIsMouse,
                                onCheckedChange = { onSetBoolean("stylusIsMouse", it) },
                                icon = Icons.Rounded.Draw,
                                index = idx++,
                                count = touchCount,
                            )
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_stylusButtonContactModifierMode),
                                summary = stringResource(X11R.string.lorie_pref_stylusButtonContactModifierMode_summary),
                                checked = state.stylusButtonContactModifierMode,
                                onCheckedChange = { onSetBoolean("stylusButtonContactModifierMode", it) },
                                icon = Icons.Rounded.Draw,
                                index = idx++,
                                count = touchCount,
                            )
                        }
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_showMouseHelper),
                            summary = stringResource(X11R.string.lorie_pref_showMouseHelper_summary),
                            checked = state.showMouseHelper,
                            enabled = state.isMouseHelperEnabled,
                            onCheckedChange = { onSetBoolean("showMouseHelper", it) },
                            icon = Icons.Rounded.Mouse,
                            index = idx,
                            count = touchCount,
                        )
                    }
                }
            }

            item(key = "mouse_input_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_mouse))
                    SettingsSegmentedGroup {
                        var idx = 0
                        if (state.pointerCaptureAvailable) {
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_pointerCapture),
                                summary = stringResource(X11R.string.lorie_pref_pointerCapture_summary),
                                checked = state.pointerCapture,
                                onCheckedChange = { onSetBoolean("pointerCapture", it) },
                                icon = Icons.Rounded.Mouse,
                                index = idx++,
                                count = mouseCount,
                            )
                        }
                        val transformTitle = stringResource(X11R.string.lorie_pref_transformCapturedPointer)
                        ExpressiveChoiceRow(
                            title = transformTitle,
                            currentLabel = state.labelForChoice("transformCapturedPointer", state.transformCapturedPointer),
                            icon = Icons.Rounded.Tune,
                            index = idx++,
                            count = mouseCount,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "transformCapturedPointer",
                                    title = transformTitle,
                                    selectedValue = state.transformCapturedPointer,
                                )
                            },
                        )
                        ExpressiveSliderRow(
                            title = stringResource(X11R.string.lorie_pref_capturedPointerSpeedFactor),
                            value = state.capturedPointerSpeedFactor,
                            valueRange = X11SettingsRules.POINTER_SPEED_MIN..X11SettingsRules.POINTER_SPEED_MAX,
                            step = 1,
                            onValueCommitted = { onSetInt("capturedPointerSpeedFactor", it) },
                            index = idx++,
                            count = mouseCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_tapToMove),
                            checked = state.tapToMove,
                            onCheckedChange = { onSetBoolean("tapToMove", it) },
                            icon = Icons.Rounded.TouchApp,
                            index = idx++,
                            count = mouseCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_ignoreGamepadEvents),
                            summary = stringResource(X11R.string.lorie_pref_ignoreGamepadEvents_summary),
                            checked = state.ignoreGamepadEvents,
                            onCheckedChange = { onSetBoolean("ignoreGamepadEvents", it) },
                            icon = Icons.Rounded.SportsEsports,
                            index = idx++,
                            count = mouseCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_zoomFollowsCursor),
                            checked = state.zoomFollowsCursor,
                            onCheckedChange = { onSetBoolean("zoomFollowsCursor", it) },
                            icon = Icons.Rounded.ZoomIn,
                            index = idx,
                            count = mouseCount,
                        )
                    }
                }
            }
        }
    }

    activeChoice?.let { choice ->
        ChoicePickerDialog(
            title = choice.title,
            options = state.choiceOptionsByKey[choice.key].orEmpty(),
            selectedValue = choice.selectedValue,
            onSelect = { selected -> onSetList(choice.key, selected) },
            onDismiss = { activeChoice = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X11KeyboardScreen(
    state: X11SettingsUiState,
    packageName: String,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onOpenEkBar: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSetBoolean: (String, Boolean) -> Unit,
    onSetAccessibilityAuto: (Boolean) -> AccessibilityToggleResult,
    onCopyCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showWriteSecureDialog by remember { mutableStateOf(false) }

    val interceptCount = 4 + if (state.dexAvailable) 1 else 0

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(X11R.string.lorie_pref_kbd),
                sectionBadge = stringResource(R.string.settings_section_x11),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "kbd_bars_hardware") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_kbd_bars))
                    SettingsSegmentedGroup {
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_showAdditionalKbd),
                            summary = stringResource(X11R.string.lorie_pref_showAdditionalKbd_summary),
                            checked = state.showAdditionalKbd,
                            onCheckedChange = { onSetBoolean("showAdditionalKbd", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = 0,
                            count = 6,
                        )
                        ExpressiveNavRow(
                            title = stringResource(X11R.string.lorie_pref_ekbar),
                            trailingBadge = "${state.opacityEKBar}%",
                            icon = Icons.Rounded.Tune,
                            index = 1,
                            count = 6,
                            onClick = onOpenEkBar,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_showIMEWhileExternalConnected),
                            summary = stringResource(X11R.string.lorie_pref_showIMEWhileExternalConnected_summary),
                            checked = state.showIMEWhileExternalConnected,
                            onCheckedChange = { onSetBoolean("showIMEWhileExternalConnected", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = 2,
                            count = 6,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_preferScancodes),
                            summary = stringResource(X11R.string.lorie_pref_preferScancodes_summary),
                            checked = state.preferScancodes,
                            onCheckedChange = { onSetBoolean("preferScancodes", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = 3,
                            count = 6,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_hardwareKbdScancodesWorkaround),
                            summary = stringResource(X11R.string.lorie_pref_hardwareKbdScancodesWorkaround_summary),
                            checked = state.hardwareKbdScancodesWorkaround,
                            onCheckedChange = { onSetBoolean("hardwareKbdScancodesWorkaround", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = 4,
                            count = 6,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_enforceCharBasedInput),
                            summary = stringResource(X11R.string.lorie_pref_enforceCharBasedInput_summary),
                            checked = state.enforceCharBasedInput,
                            onCheckedChange = { onSetBoolean("enforceCharBasedInput", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = 5,
                            count = 6,
                        )
                    }
                }
            }

            item(key = "kbd_intercept_accessibility") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_kbd_intercept))
                    SettingsSegmentedGroup {
                        var idx = 0
                        if (state.dexAvailable) {
                            ExpressiveSwitchRow(
                                title = stringResource(X11R.string.lorie_pref_dexMetaKeyCapture),
                                summary = stringResource(X11R.string.lorie_pref_dexMetaKeyCapture_summary),
                                checked = state.dexMetaKeyCapture,
                                enabled = state.isDexCaptureEnabled,
                                onCheckedChange = { onSetBoolean("dexMetaKeyCapture", it) },
                                icon = Icons.Rounded.Tv,
                                index = idx++,
                                count = interceptCount,
                            )
                        }
                        ExpressiveActionRow(
                            title = stringResource(X11R.string.lorie_pref_enableAccessibilityService),
                            summary = stringResource(X11R.string.lorie_pref_enableAccessibilityService_summary),
                            icon = Icons.Rounded.AccessibilityNew,
                            index = idx++,
                            count = interceptCount,
                            onClick = onOpenAccessibilitySettings,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_enableAccessibilityServiceAutomatically),
                            summary = stringResource(X11R.string.lorie_pref_enableAccessibilityServiceAutomatically_summary),
                            checked = state.enableAccessibilityServiceAutomatically,
                            enabled = state.isAutoAccessibilityEnabled,
                            onCheckedChange = { next ->
                                if (onSetAccessibilityAuto(next) == AccessibilityToggleResult.RequiresWriteSecureSettings) {
                                    showWriteSecureDialog = true
                                }
                            },
                            icon = Icons.Rounded.AccessibilityNew,
                            index = idx++,
                            count = interceptCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_pauseKeyInterceptingWithEsc),
                            summary = if (!state.isPauseEscEnabled) {
                                stringResource(X11R.string.lorie_pref_summary_requiresIntercepting)
                            } else {
                                null
                            },
                            checked = state.pauseKeyInterceptingWithEsc,
                            enabled = state.isPauseEscEnabled,
                            onCheckedChange = { onSetBoolean("pauseKeyInterceptingWithEsc", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = idx++,
                            count = interceptCount,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_filterOutWinkey),
                            summary = stringResource(X11R.string.lorie_pref_filterOutWinkey_summary),
                            checked = state.filterOutWinkey,
                            enabled = state.isFilterWinKeyEnabled,
                            onCheckedChange = { onSetBoolean("filterOutWinkey", it) },
                            icon = Icons.Rounded.Keyboard,
                            index = idx,
                            count = interceptCount,
                        )
                    }
                }
            }
        }
    }

    if (showWriteSecureDialog) {
        WriteSecureSettingsDialog(
            packageName = packageName,
            onCopyCommand = onCopyCommand,
            onDismiss = { showWriteSecureDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X11ExtraKeysBarScreen(
    state: X11SettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetBoolean: (String, Boolean) -> Unit,
    onSetList: (String, String) -> Unit,
    onSetInt: (String, Int) -> Unit,
    onSaveExtraKeysConfig: (String) -> Unit,
    onResetExtraKeysConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var activeChoice by remember { mutableStateOf<ChoiceDialogState?>(null) }
    var showExtraKeysDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(X11R.string.lorie_pref_ekbar),
                sectionBadge = stringResource(R.string.settings_section_x11),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "ekbar_layout_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_ekbar_layout))
                    SettingsSegmentedGroup {
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_adjustHeightForEK),
                            checked = state.adjustHeightForEK,
                            onCheckedChange = { onSetBoolean("adjustHeightForEK", it) },
                            icon = Icons.Rounded.AspectRatio,
                            index = 0,
                            count = 5,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_useTermuxEKBarBehaviour),
                            summary = stringResource(X11R.string.lorie_pref_useTermuxEKBarBehaviour_summary),
                            checked = state.useTermuxEKBarBehaviour,
                            onCheckedChange = { onSetBoolean("useTermuxEKBarBehaviour", it) },
                            icon = Icons.Rounded.TouchApp,
                            index = 1,
                            count = 5,
                        )
                        val posTitle = stringResource(X11R.string.lorie_pref_ekbarPosition)
                        ExpressiveChoiceRow(
                            title = posTitle,
                            currentLabel = state.labelForChoice("ekbarPosition", state.ekbarPosition),
                            icon = Icons.Rounded.Tune,
                            index = 2,
                            count = 5,
                            onClick = {
                                activeChoice = ChoiceDialogState(
                                    key = "ekbarPosition",
                                    title = posTitle,
                                    selectedValue = state.ekbarPosition,
                                )
                            },
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_ekbarPositionIgnoreOrientation),
                            checked = state.ekbarPositionIgnoreOrientation,
                            onCheckedChange = { onSetBoolean("ekbarPositionIgnoreOrientation", it) },
                            icon = Icons.Rounded.ScreenRotation,
                            index = 3,
                            count = 5,
                        )
                        ExpressiveSliderRow(
                            title = stringResource(X11R.string.lorie_pref_opacityEKBar),
                            value = state.opacityEKBar,
                            valueRange = X11SettingsRules.EKBAR_OPACITY_MIN..X11SettingsRules.EKBAR_OPACITY_MAX,
                            step = 1,
                            onValueCommitted = { onSetInt("opacityEKBar", it) },
                            index = 4,
                            count = 5,
                        )
                    }
                }
            }

            item(key = "ekbar_keys_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_ekbar_keys))
                    SettingsSegmentedGroup {
                        ExpressiveActionRow(
                            title = stringResource(X11R.string.lorie_pref_extra_keys_config),
                            summary = state.extraKeysConfig.lineSequence().firstOrNull().orEmpty(),
                            icon = Icons.Rounded.Edit,
                            index = 0,
                            count = 1,
                            onClick = { showExtraKeysDialog = true },
                        )
                    }
                }
            }
        }
    }

    activeChoice?.let { choice ->
        ChoicePickerDialog(
            title = choice.title,
            options = state.choiceOptionsByKey[choice.key].orEmpty(),
            selectedValue = choice.selectedValue,
            onSelect = { selected -> onSetList(choice.key, selected) },
            onDismiss = { activeChoice = null },
        )
    }

    if (showExtraKeysDialog) {
        ExtraKeysConfigDialog(
            initialConfig = state.extraKeysConfig,
            onSave = onSaveExtraKeysConfig,
            onReset = onResetExtraKeysConfig,
            onDismiss = { showExtraKeysDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X11OtherScreen(
    state: X11SettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetBoolean: (String, Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenUserActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val totalCount = 3 + if (state.notificationPermissionVisible) 1 else 0

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(X11R.string.lorie_pref_other),
                sectionBadge = stringResource(R.string.settings_section_x11),
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "other_actions_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(R.string.settings_group_x11_other_actions))
                    SettingsSegmentedGroup {
                        var idx = 0
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_clipboardEnable),
                            summary = stringResource(R.string.settings_clipboard_sync_summary),
                            checked = state.clipboardEnable,
                            onCheckedChange = { onSetBoolean("clipboardEnable", it) },
                            icon = Icons.Rounded.ContentPaste,
                            index = idx++,
                            count = totalCount,
                        )
                        if (state.notificationPermissionVisible) {
                            ExpressiveActionRow(
                                title = stringResource(X11R.string.lorie_pref_requestNotificationPermission),
                                summary = stringResource(R.string.settings_notification_permission_summary),
                                valueBadge = stringResource(R.string.settings_grant),
                                icon = Icons.Rounded.Notifications,
                                index = idx++,
                                count = totalCount,
                                onClick = onRequestNotificationPermission,
                            )
                        }
                        ExpressiveNavRow(
                            title = stringResource(X11R.string.lorie_pref_configureResponseToUserActions),
                            icon = Icons.Rounded.Gesture,
                            index = idx++,
                            count = totalCount,
                            onClick = onOpenUserActions,
                        )
                        ExpressiveSwitchRow(
                            title = stringResource(X11R.string.lorie_pref_storeSecondaryDisplayPreferencesSeparately),
                            summary = stringResource(X11R.string.lorie_pref_storeSecondaryDisplayPreferencesSeparately_summary),
                            checked = state.storeSecondaryDisplayPreferencesSeparately,
                            onCheckedChange = { onSetBoolean("storeSecondaryDisplayPreferencesSeparately", it) },
                            icon = Icons.Rounded.Tv,
                            index = idx,
                            count = totalCount,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X11UserActionsScreen(
    state: X11SettingsUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSetList: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var activeChoice by remember { mutableStateOf<ChoiceDialogState?>(null) }

    val actionEntries = listOf(
        Triple("swipeUpAction", stringResource(X11R.string.lorie_pref_swipeUpAction), state.swipeUpAction),
        Triple("swipeDownAction", stringResource(X11R.string.lorie_pref_swipeDownAction), state.swipeDownAction),
        Triple("volumeUpAction", stringResource(X11R.string.lorie_pref_volumeUpAction), state.volumeUpAction),
        Triple("volumeDownAction", stringResource(X11R.string.lorie_pref_volumeDownAction), state.volumeDownAction),
        Triple("backButtonAction", stringResource(X11R.string.lorie_pref_backButtonAction), state.backButtonAction),
        Triple("notificationTapAction", stringResource(X11R.string.lorie_pref_notificationTapAction), state.notificationTapAction),
        Triple("notificationButton0Action", stringResource(X11R.string.lorie_pref_notificationButton0Action), state.notificationButton0Action),
        Triple("notificationButton1Action", stringResource(X11R.string.lorie_pref_notificationButton1Action), state.notificationButton1Action),
        Triple("mediaKeysAction", stringResource(X11R.string.lorie_pref_mediaKeysAction), state.mediaKeysAction),
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SettingsDetailTopBar(
                title = stringResource(X11R.string.lorie_pref_userActions),
                sectionBadge = stringResource(R.string.settings_section_x11),
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
            item(key = "user_actions_group") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupHeader(stringResource(X11R.string.lorie_pref_configureResponseToUserActions))
                    SettingsSegmentedGroup {
                        actionEntries.forEachIndexed { idx, (key, title, currentValue) ->
                            val icon = when (key) {
                                "volumeUpAction" -> Icons.AutoMirrored.Rounded.VolumeUp
                                "volumeDownAction" -> Icons.AutoMirrored.Rounded.VolumeDown
                                "backButtonAction" -> Icons.AutoMirrored.Rounded.ArrowBack
                                "notificationTapAction",
                                "notificationButton0Action",
                                "notificationButton1Action" -> Icons.Rounded.Notifications
                                "mediaKeysAction" -> Icons.Rounded.Speed
                                else -> Icons.Rounded.Gesture
                            }
                            ExpressiveChoiceRow(
                                title = title,
                                currentLabel = state.labelForChoice(key, currentValue),
                                icon = icon,
                                index = idx,
                                count = actionEntries.size,
                                onClick = {
                                    activeChoice = ChoiceDialogState(
                                        key = key,
                                        title = title,
                                        selectedValue = currentValue,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    activeChoice?.let { choice ->
        ChoicePickerDialog(
            title = choice.title,
            options = state.choiceOptionsByKey[choice.key].orEmpty(),
            selectedValue = choice.selectedValue,
            onSelect = { selected -> onSetList(choice.key, selected) },
            onDismiss = { activeChoice = null },
        )
    }
}
