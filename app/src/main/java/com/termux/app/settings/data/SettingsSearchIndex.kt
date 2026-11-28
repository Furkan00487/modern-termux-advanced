package com.termux.app.settings.data

import android.content.Context
import com.termux.R
import com.termux.app.activities.SettingsSection
import com.termux.app.settings.navigation.SettingsRoute
import com.termux.x11.R as X11R

enum class SearchControlType {
    SWITCH,
    CHOICE,
    SLIDER,
    TEXT_INPUT,
    NAVIGATION,
    ACTION,
    PERMISSION,
}

data class SettingsSearchEntry(
    val key: String,
    val title: String,
    val summary: String,
    val section: SettingsSection,
    val categoryTitle: String,
    val targetRoute: SettingsRoute,
    val controlType: SearchControlType,
    val currentBoolean: Boolean? = null,
    val enabled: Boolean = true,
    val keywords: String = "",
)

/**
 * Builds and filters the unified search catalog across both Termux and X11 settings
 * while honoring every conditional visibility rule.
 */
object SettingsSearchIndex {

    fun buildVisibleEntries(
        context: Context,
        termux: TermuxSettingsUiState,
        x11: X11SettingsUiState,
    ): List<SettingsSearchEntry> = buildList {
        val termuxDebugCat = context.getString(R.string.termux_debugging_preferences_title)
        val termuxIoCat = context.getString(R.string.termux_terminal_io_preferences_title)
        val termuxViewCat = context.getString(R.string.termux_terminal_view_preferences_title)
        val termuxSystemCat = context.getString(R.string.settings_group_system)

        // Termux entries
        add(
            SettingsSearchEntry(
                key = "log_level",
                title = context.getString(R.string.termux_log_level_title),
                summary = termux.logLevelOptions.firstOrNull { it.value == termux.logLevel.toString() }?.label ?: "",
                section = SettingsSection.TERMUX,
                categoryTitle = termuxDebugCat,
                targetRoute = SettingsRoute.TermuxDebugging,
                controlType = SearchControlType.CHOICE,
                keywords = "log debug verbose logcat günlük kaydı",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "terminal_view_key_logging_enabled",
                title = context.getString(R.string.termux_terminal_view_key_logging_enabled_title),
                summary = context.getString(
                    if (termux.terminalViewKeyLogging) R.string.termux_terminal_view_key_logging_enabled_on
                    else R.string.termux_terminal_view_key_logging_enabled_off,
                ),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxDebugCat,
                targetRoute = SettingsRoute.TermuxDebugging,
                controlType = SearchControlType.SWITCH,
                currentBoolean = termux.terminalViewKeyLogging,
                keywords = "key logcat input tuş",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "plugin_error_notifications_enabled",
                title = context.getString(R.string.termux_plugin_error_notifications_enabled_title),
                summary = context.getString(
                    if (termux.pluginErrorNotifications) R.string.termux_plugin_error_notifications_enabled_on
                    else R.string.termux_plugin_error_notifications_enabled_off,
                ),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxDebugCat,
                targetRoute = SettingsRoute.TermuxDebugging,
                controlType = SearchControlType.SWITCH,
                currentBoolean = termux.pluginErrorNotifications,
                keywords = "plugin notification eklenti hata",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "crash_report_notifications_enabled",
                title = context.getString(R.string.termux_crash_report_notifications_enabled_title),
                summary = context.getString(
                    if (termux.crashReportNotifications) R.string.termux_crash_report_notifications_enabled_on
                    else R.string.termux_crash_report_notifications_enabled_off,
                ),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxDebugCat,
                targetRoute = SettingsRoute.TermuxDebugging,
                controlType = SearchControlType.SWITCH,
                currentBoolean = termux.crashReportNotifications,
                keywords = "crash çökme rapor",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "soft_keyboard_enabled",
                title = context.getString(R.string.termux_soft_keyboard_enabled_title),
                summary = context.getString(
                    if (termux.softKeyboardEnabled) R.string.termux_soft_keyboard_enabled_on
                    else R.string.termux_soft_keyboard_enabled_off,
                ),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxIoCat,
                targetRoute = SettingsRoute.TermuxTerminalIO,
                controlType = SearchControlType.SWITCH,
                currentBoolean = termux.softKeyboardEnabled,
                keywords = "ime keyboard klavye sanal",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "soft_keyboard_enabled_only_if_no_hardware",
                title = context.getString(R.string.termux_soft_keyboard_enabled_only_if_no_hardware_title),
                summary = context.getString(
                    if (termux.softKeyboardOnlyIfNoHardware) R.string.termux_soft_keyboard_enabled_only_if_no_hardware_on
                    else R.string.termux_soft_keyboard_enabled_only_if_no_hardware_off,
                ),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxIoCat,
                targetRoute = SettingsRoute.TermuxTerminalIO,
                controlType = SearchControlType.SWITCH,
                currentBoolean = termux.softKeyboardOnlyIfNoHardware,
                keywords = "hardware keyboard fiziksel donanım klavye",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "terminal_margin_adjustment",
                title = context.getString(R.string.termux_terminal_view_terminal_margin_adjustment_title),
                summary = context.getString(
                    if (termux.terminalMarginAdjustment) R.string.termux_terminal_view_terminal_margin_adjustment_on
                    else R.string.termux_terminal_view_terminal_margin_adjustment_off,
                ),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxViewCat,
                targetRoute = SettingsRoute.TermuxTerminalView,
                controlType = SearchControlType.SWITCH,
                currentBoolean = termux.terminalMarginAdjustment,
                keywords = "margin kenar boşluğu view",
            ),
        )
        termux.installedPlugins.forEach { plugin ->
            add(
                SettingsSearchEntry(
                    key = "plugin_${plugin.kind.name.lowercase()}",
                    title = context.getString(plugin.titleRes),
                    summary = context.getString(plugin.summaryRes),
                    section = SettingsSection.TERMUX,
                    categoryTitle = context.getString(R.string.settings_group_plugins),
                    targetRoute = SettingsRoute.TermuxPlugin(plugin.kind),
                    controlType = SearchControlType.NAVIGATION,
                    keywords = "plugin log eklenti ${plugin.kind.name.lowercase()}",
                ),
            )
            if (plugin.supportsKeyLogging) {
                add(
                    SettingsSearchEntry(
                        key = "plugin_${plugin.kind.name.lowercase()}_key_logging",
                        title = "${context.getString(plugin.titleRes)} · ${context.getString(R.string.termux_terminal_view_key_logging_enabled_title)}",
                        summary = context.getString(
                            if (plugin.keyLoggingEnabled) {
                                R.string.termux_terminal_view_key_logging_enabled_on
                            } else {
                                R.string.termux_terminal_view_key_logging_enabled_off
                            },
                        ),
                        section = SettingsSection.TERMUX,
                        categoryTitle = context.getString(R.string.settings_group_plugins),
                        targetRoute = SettingsRoute.TermuxPlugin(plugin.kind),
                        controlType = SearchControlType.SWITCH,
                        currentBoolean = plugin.keyLoggingEnabled,
                        keywords = "plugin float key logging tuş günlüğü",
                    ),
                )
            }
        }
        add(
            SettingsSearchEntry(
                key = "about",
                title = context.getString(R.string.about_preference_title),
                summary = context.getString(R.string.settings_about_summary),
                section = SettingsSection.TERMUX,
                categoryTitle = termuxSystemCat,
                targetRoute = SettingsRoute.RootList,
                controlType = SearchControlType.ACTION,
                keywords = "about version info links hakkında sürüm",
            ),
        )
        if (termux.showDonate) {
            add(
                SettingsSearchEntry(
                    key = "donate",
                    title = context.getString(R.string.donate_preference_title),
                    summary = context.getString(R.string.settings_donate_summary),
                    section = SettingsSection.TERMUX,
                    categoryTitle = termuxSystemCat,
                    targetRoute = SettingsRoute.RootList,
                    controlType = SearchControlType.ACTION,
                    keywords = "donate sponsor support upstream termux bağış",
                ),
            )
        }

        // X11 Output
        val outCat = context.getString(X11R.string.lorie_pref_output)
        if (x11.hasHardwareAccelerationFeature) {
            add(
                SettingsSearchEntry(
                    key = "hardwareAcceleration",
                    title = context.getString(X11R.string.lorie_pref_hardware_acceleration),
                    summary = context.getString(X11R.string.lorie_pref_hardware_acceleration_summary),
                    section = SettingsSection.X11,
                    categoryTitle = outCat,
                    targetRoute = SettingsRoute.X11Output,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.hardwareAcceleration,
                    keywords = "gpu vulkan zink mali acceleration donanım hızlandırma",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "displayResolutionMode",
                title = context.getString(X11R.string.lorie_pref_displayResolutionMode),
                summary = x11.labelForChoice("displayResolutionMode", x11.displayResolutionMode),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.CHOICE,
                keywords = "resolution çözünürlük native scaled exact custom",
            ),
        )
        if (x11.isDisplayScaleVisible) {
            add(
                SettingsSearchEntry(
                    key = "displayScale",
                    title = context.getString(X11R.string.lorie_pref_displayScale),
                    summary = "${x11.displayScale}%",
                    section = SettingsSection.X11,
                    categoryTitle = outCat,
                    targetRoute = SettingsRoute.X11Output,
                    controlType = SearchControlType.SLIDER,
                    keywords = "scale dpi zoom ölçek",
                ),
            )
        }
        if (x11.isDisplayExactVisible) {
            add(
                SettingsSearchEntry(
                    key = "displayResolutionExact",
                    title = context.getString(X11R.string.lorie_pref_displayResolutionExact),
                    summary = x11.displayResolutionExact,
                    section = SettingsSection.X11,
                    categoryTitle = outCat,
                    targetRoute = SettingsRoute.X11Output,
                    controlType = SearchControlType.CHOICE,
                    keywords = "resolution exact preset",
                ),
            )
        }
        if (x11.isDisplayCustomVisible) {
            add(
                SettingsSearchEntry(
                    key = "displayResolutionCustom",
                    title = context.getString(X11R.string.lorie_pref_displayResolutionCustom),
                    summary = x11.displayResolutionCustom,
                    section = SettingsSection.X11,
                    categoryTitle = outCat,
                    targetRoute = SettingsRoute.X11Output,
                    controlType = SearchControlType.TEXT_INPUT,
                    keywords = "custom resolution wxh özel",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "displayFilteringMode",
                title = context.getString(X11R.string.lorie_pref_displayFilteringMode),
                summary = x11.labelForChoice("displayFilteringMode", x11.displayFilteringMode),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.CHOICE,
                keywords = "bilinear nearest filter filtre",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "adjustResolution",
                title = context.getString(X11R.string.lorie_pref_adjustResolution),
                summary = context.getString(X11R.string.lorie_pref_summary_requiresExactOrCustom),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.adjustResolution,
                enabled = x11.isDisplayStretchEnabled,
                keywords = "orientation rotate yön",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "displayStretch",
                title = context.getString(X11R.string.lorie_pref_displayStretch),
                summary = context.getString(X11R.string.lorie_pref_summary_requiresExactOrCustom),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.displayStretch,
                enabled = x11.isDisplayStretchEnabled,
                keywords = "stretch aspect ratio genişlet",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "Reseed",
                title = context.getString(X11R.string.lorie_pref_Reseed),
                summary = context.getString(X11R.string.lorie_pref_Reseed_summary),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.reseed,
                keywords = "reseed keyboard resize yeniden boyutlandır",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "PIP",
                title = context.getString(X11R.string.lorie_pref_PIP),
                summary = context.getString(X11R.string.lorie_pref_PIP_summary),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.pip,
                keywords = "pip picture in picture pencere",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "fullscreen",
                title = context.getString(X11R.string.lorie_pref_fullscreen),
                summary = context.getString(X11R.string.lorie_pref_fullscreen_summary),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.fullscreen,
                keywords = "fullscreen immersive tam ekran",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "forceOrientation",
                title = context.getString(X11R.string.lorie_pref_forceOrientation),
                summary = x11.labelForChoice("forceOrientation", x11.forceOrientation),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.CHOICE,
                keywords = "orientation portrait landscape dikey yatay",
            ),
        )
        if (x11.hideCutoutAvailable) {
            add(
                SettingsSearchEntry(
                    key = "hideCutout",
                    title = context.getString(X11R.string.lorie_pref_hideCutout),
                    summary = "",
                    section = SettingsSection.X11,
                    categoryTitle = outCat,
                    targetRoute = SettingsRoute.X11Output,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.hideCutout,
                    keywords = "cutout notch çentik",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "screenIdleTimeout",
                title = context.getString(X11R.string.lorie_pref_screenIdleTimeout),
                summary = x11.labelForChoice("screenIdleTimeout", x11.screenIdleTimeout),
                section = SettingsSection.X11,
                categoryTitle = outCat,
                targetRoute = SettingsRoute.X11Output,
                controlType = SearchControlType.CHOICE,
                keywords = "timeout screen on wake sleep ekran zaman aşımı",
            ),
        )

        // X11 Pointer
        val pointerCat = context.getString(X11R.string.lorie_pref_pointer)
        add(
            SettingsSearchEntry(
                key = "touchMode",
                title = context.getString(X11R.string.lorie_pref_touchMode),
                summary = x11.labelForChoice("touchMode", x11.touchMode),
                section = SettingsSection.X11,
                categoryTitle = pointerCat,
                targetRoute = SettingsRoute.X11Pointer,
                controlType = SearchControlType.CHOICE,
                keywords = "touchpad trackpad touchscreen dokunmatik",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "scaleTouchpad",
                title = context.getString(X11R.string.lorie_pref_scaleTouchpad),
                summary = context.getString(X11R.string.lorie_pref_summary_requiresTrackpadAndNative),
                section = SettingsSection.X11,
                categoryTitle = pointerCat,
                targetRoute = SettingsRoute.X11Pointer,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.scaleTouchpad,
                enabled = x11.isScaleTouchpadEnabled,
                keywords = "touchpad scale ölçek",
            ),
        )
        if (x11.stylusAvailable) {
            add(
                SettingsSearchEntry(
                    key = "showStylusClickOverride",
                    title = context.getString(X11R.string.lorie_pref_showStylusClickOverride),
                    summary = context.getString(X11R.string.lorie_pref_showStylusClickOverride_summary),
                    section = SettingsSection.X11,
                    categoryTitle = pointerCat,
                    targetRoute = SettingsRoute.X11Pointer,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.showStylusClickOverride,
                    keywords = "stylus pen kalem",
                ),
            )
            add(
                SettingsSearchEntry(
                    key = "stylusIsMouse",
                    title = context.getString(X11R.string.lorie_pref_stylusIsMouse),
                    summary = context.getString(X11R.string.lorie_pref_stylusIsMouse_summary),
                    section = SettingsSection.X11,
                    categoryTitle = pointerCat,
                    targetRoute = SettingsRoute.X11Pointer,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.stylusIsMouse,
                    keywords = "stylus mouse kalem fare",
                ),
            )
            add(
                SettingsSearchEntry(
                    key = "stylusButtonContactModifierMode",
                    title = context.getString(X11R.string.lorie_pref_stylusButtonContactModifierMode),
                    summary = context.getString(X11R.string.lorie_pref_stylusButtonContactModifierMode_summary),
                    section = SettingsSection.X11,
                    categoryTitle = pointerCat,
                    targetRoute = SettingsRoute.X11Pointer,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.stylusButtonContactModifierMode,
                    keywords = "stylus button kalem",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "showMouseHelper",
                title = context.getString(X11R.string.lorie_pref_showMouseHelper),
                summary = context.getString(X11R.string.lorie_pref_showMouseHelper_summary),
                section = SettingsSection.X11,
                categoryTitle = pointerCat,
                targetRoute = SettingsRoute.X11Pointer,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.showMouseHelper,
                enabled = x11.isMouseHelperEnabled,
                keywords = "mouse helper overlay fare yardımcısı",
            ),
        )
        if (x11.pointerCaptureAvailable) {
            add(
                SettingsSearchEntry(
                    key = "pointerCapture",
                    title = context.getString(X11R.string.lorie_pref_pointerCapture),
                    summary = context.getString(X11R.string.lorie_pref_pointerCapture_summary),
                    section = SettingsSection.X11,
                    categoryTitle = pointerCat,
                    targetRoute = SettingsRoute.X11Pointer,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.pointerCapture,
                    keywords = "pointer capture mouse yakala",
                ),
            )
            add(
                SettingsSearchEntry(
                    key = "transformCapturedPointer",
                    title = context.getString(X11R.string.lorie_pref_transformCapturedPointer),
                    summary = x11.labelForChoice("transformCapturedPointer", x11.transformCapturedPointer),
                    section = SettingsSection.X11,
                    categoryTitle = pointerCat,
                    targetRoute = SettingsRoute.X11Pointer,
                    controlType = SearchControlType.CHOICE,
                    keywords = "pointer transform dönüştür",
                ),
            )
            add(
                SettingsSearchEntry(
                    key = "capturedPointerSpeedFactor",
                    title = context.getString(X11R.string.lorie_pref_capturedPointerSpeedFactor),
                    summary = "${x11.capturedPointerSpeedFactor}%",
                    section = SettingsSection.X11,
                    categoryTitle = pointerCat,
                    targetRoute = SettingsRoute.X11Pointer,
                    controlType = SearchControlType.SLIDER,
                    keywords = "mouse sensitivity speed hız çarpanı",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "tapToMove",
                title = context.getString(X11R.string.lorie_pref_tapToMove),
                summary = "",
                section = SettingsSection.X11,
                categoryTitle = pointerCat,
                targetRoute = SettingsRoute.X11Pointer,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.tapToMove,
                keywords = "tap drag touchpad taşı",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "ignoreGamepadEvents",
                title = context.getString(X11R.string.lorie_pref_ignoreGamepadEvents),
                summary = context.getString(X11R.string.lorie_pref_ignoreGamepadEvents_summary),
                section = SettingsSection.X11,
                categoryTitle = pointerCat,
                targetRoute = SettingsRoute.X11Pointer,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.ignoreGamepadEvents,
                keywords = "gamepad joystick oyun kolu",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "zoomFollowsCursor",
                title = context.getString(X11R.string.lorie_pref_zoomFollowsCursor),
                summary = "",
                section = SettingsSection.X11,
                categoryTitle = pointerCat,
                targetRoute = SettingsRoute.X11Pointer,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.zoomFollowsCursor,
                keywords = "zoom cursor imleç yakınlaştır",
            ),
        )

        // X11 Keyboard & Extra Keys
        val kbdCat = context.getString(X11R.string.lorie_pref_kbd)
        val ekCat = context.getString(X11R.string.lorie_pref_ekbar)
        add(
            SettingsSearchEntry(
                key = "showAdditionalKbd",
                title = context.getString(X11R.string.lorie_pref_showAdditionalKbd),
                summary = context.getString(X11R.string.lorie_pref_showAdditionalKbd_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.showAdditionalKbd,
                keywords = "extra keys ek tuş çubuğu",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "showIMEWhileExternalConnected",
                title = context.getString(X11R.string.lorie_pref_showIMEWhileExternalConnected),
                summary = context.getString(X11R.string.lorie_pref_showIMEWhileExternalConnected_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.showIMEWhileExternalConnected,
                keywords = "ime hardware keyboard harici klavye",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "preferScancodes",
                title = context.getString(X11R.string.lorie_pref_preferScancodes),
                summary = context.getString(X11R.string.lorie_pref_preferScancodes_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.preferScancodes,
                keywords = "scancode setxkbmap layout",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "hardwareKbdScancodesWorkaround",
                title = context.getString(X11R.string.lorie_pref_hardwareKbdScancodesWorkaround),
                summary = context.getString(X11R.string.lorie_pref_hardwareKbdScancodesWorkaround_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.hardwareKbdScancodesWorkaround,
                keywords = "scancode workaround",
            ),
        )
        if (x11.dexAvailable) {
            add(
                SettingsSearchEntry(
                    key = "dexMetaKeyCapture",
                    title = context.getString(X11R.string.lorie_pref_dexMetaKeyCapture),
                    summary = context.getString(X11R.string.lorie_pref_dexMetaKeyCapture_summary),
                    section = SettingsSection.X11,
                    categoryTitle = kbdCat,
                    targetRoute = SettingsRoute.X11Keyboard,
                    controlType = SearchControlType.SWITCH,
                    currentBoolean = x11.dexMetaKeyCapture,
                    enabled = x11.isDexCaptureEnabled,
                    keywords = "dex samsung meta alt+f4 kısayol",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "enableAccessibilityService",
                title = context.getString(X11R.string.lorie_pref_enableAccessibilityService),
                summary = context.getString(X11R.string.lorie_pref_enableAccessibilityService_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.ACTION,
                keywords = "accessibility erişilebilirlik",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "enableAccessibilityServiceAutomatically",
                title = context.getString(X11R.string.lorie_pref_enableAccessibilityServiceAutomatically),
                summary = context.getString(X11R.string.lorie_pref_enableAccessibilityServiceAutomatically_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.enableAccessibilityServiceAutomatically,
                enabled = x11.isAutoAccessibilityEnabled,
                keywords = "accessibility write_secure_settings adb",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "pauseKeyInterceptingWithEsc",
                title = context.getString(X11R.string.lorie_pref_pauseKeyInterceptingWithEsc),
                summary = context.getString(X11R.string.lorie_pref_summary_requiresIntercepting),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.pauseKeyInterceptingWithEsc,
                enabled = x11.isPauseEscEnabled,
                keywords = "esc pause intercept",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "filterOutWinkey",
                title = context.getString(X11R.string.lorie_pref_filterOutWinkey),
                summary = context.getString(X11R.string.lorie_pref_filterOutWinkey_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.filterOutWinkey,
                enabled = x11.isFilterWinKeyEnabled,
                keywords = "win meta mod4 super",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "enforceCharBasedInput",
                title = context.getString(X11R.string.lorie_pref_enforceCharBasedInput),
                summary = context.getString(X11R.string.lorie_pref_enforceCharBasedInput_summary),
                section = SettingsSection.X11,
                categoryTitle = kbdCat,
                targetRoute = SettingsRoute.X11Keyboard,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.enforceCharBasedInput,
                keywords = "char cjk predictive karakter",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "ekbar",
                title = context.getString(X11R.string.lorie_pref_ekbar),
                summary = "${x11.labelForChoice("ekbarPosition", x11.ekbarPosition)} · ${x11.opacityEKBar}%",
                section = SettingsSection.X11,
                categoryTitle = ekCat,
                targetRoute = SettingsRoute.X11ExtraKeysBar,
                controlType = SearchControlType.NAVIGATION,
                keywords = "extra keys bar ek tuş opacity position",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "extra_keys_config",
                title = context.getString(X11R.string.lorie_pref_extra_keys_config),
                summary = "",
                section = SettingsSection.X11,
                categoryTitle = ekCat,
                targetRoute = SettingsRoute.X11ExtraKeysBar,
                controlType = SearchControlType.TEXT_INPUT,
                keywords = "extra keys json config yapılandırma",
            ),
        )

        // X11 Other
        val otherCat = context.getString(X11R.string.lorie_pref_other)
        add(
            SettingsSearchEntry(
                key = "clipboardEnable",
                title = context.getString(X11R.string.lorie_pref_clipboardEnable),
                summary = context.getString(R.string.settings_clipboard_sync_summary),
                section = SettingsSection.X11,
                categoryTitle = otherCat,
                targetRoute = SettingsRoute.X11Other,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.clipboardEnable,
                keywords = "clipboard copy paste pano paylaşım",
            ),
        )
        if (x11.notificationPermissionVisible) {
            add(
                SettingsSearchEntry(
                    key = "requestNotificationPermission",
                    title = context.getString(X11R.string.lorie_pref_requestNotificationPermission),
                    summary = context.getString(R.string.settings_notification_permission_summary),
                    section = SettingsSection.X11,
                    categoryTitle = otherCat,
                    targetRoute = SettingsRoute.X11Other,
                    controlType = SearchControlType.PERMISSION,
                    keywords = "notification permission bildirim izni",
                ),
            )
        }
        add(
            SettingsSearchEntry(
                key = "configureResponseToUserActions",
                title = context.getString(X11R.string.lorie_pref_configureResponseToUserActions),
                summary = "",
                section = SettingsSection.X11,
                categoryTitle = otherCat,
                targetRoute = SettingsRoute.X11UserActions,
                controlType = SearchControlType.NAVIGATION,
                keywords = "swipe volume back gesture notification actions eylem",
            ),
        )
        add(
            SettingsSearchEntry(
                key = "storeSecondaryDisplayPreferencesSeparately",
                title = context.getString(X11R.string.lorie_pref_storeSecondaryDisplayPreferencesSeparately),
                summary = context.getString(X11R.string.lorie_pref_storeSecondaryDisplayPreferencesSeparately_summary),
                section = SettingsSection.X11,
                categoryTitle = otherCat,
                targetRoute = SettingsRoute.X11Other,
                controlType = SearchControlType.SWITCH,
                currentBoolean = x11.storeSecondaryDisplayPreferencesSeparately,
                keywords = "secondary display monitor external ikincil ekran",
            ),
        )
    }

    fun search(
        query: String,
        context: Context,
        termux: TermuxSettingsUiState,
        x11: X11SettingsUiState,
    ): List<SettingsSearchEntry> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()
        return buildVisibleEntries(context, termux, x11).filter { entry ->
            entry.title.lowercase().contains(trimmed) ||
                entry.summary.lowercase().contains(trimmed) ||
                entry.categoryTitle.lowercase().contains(trimmed) ||
                entry.keywords.lowercase().contains(trimmed) ||
                entry.key.lowercase().contains(trimmed)
        }
    }
}
