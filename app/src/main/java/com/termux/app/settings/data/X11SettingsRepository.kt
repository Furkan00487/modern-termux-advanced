package com.termux.app.settings.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import com.termux.x11.BuildConfig as X11BuildConfig
import com.termux.x11.LorieApp
import com.termux.x11.LoriePreferences
import com.termux.x11.Prefs
import com.termux.x11.R as X11R
import com.termux.x11.X11PreferenceBridge
import com.termux.x11.utils.TermuxX11ExtraKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Pure validation and conditional visibility/enablement rules for X11 preferences,
 * preserving parity with {@code LoriePreferences.java} without {@code PreferenceFragmentCompat}.
 */
object X11SettingsRules {
    const val DISPLAY_SCALE_MIN = 30
    const val DISPLAY_SCALE_MAX = 300
    const val POINTER_SPEED_MIN = 1
    const val POINTER_SPEED_MAX = 300
    const val EKBAR_OPACITY_MIN = 1
    const val EKBAR_OPACITY_MAX = 100

    fun snapDisplayScale(rawValue: Int): Int =
        ((rawValue / 10.0f).roundToInt() * 10).coerceIn(DISPLAY_SCALE_MIN, DISPLAY_SCALE_MAX)

    fun isDisplayScaleVisible(displayResolutionMode: String): Boolean = displayResolutionMode == "scaled"
    fun isDisplayExactVisible(displayResolutionMode: String): Boolean = displayResolutionMode == "exact"
    fun isDisplayCustomVisible(displayResolutionMode: String): Boolean = displayResolutionMode == "custom"
    fun isDisplayStretchEnabled(displayResolutionMode: String): Boolean =
        displayResolutionMode == "exact" || displayResolutionMode == "custom"

    fun isScaleTouchpadEnabled(touchMode: String, displayResolutionMode: String): Boolean =
        touchMode == "1" && displayResolutionMode != "native"

    fun isMouseHelperEnabled(touchMode: String): Boolean = touchMode == "1"

    fun isDexCaptureEnabled(enableAccessibilityAuto: Boolean): Boolean = !enableAccessibilityAuto

    fun isAutoAccessibilityEnabled(
        dexMetaKeyCapture: Boolean,
        enableAccessibilityAuto: Boolean,
        keyInterceptorLaunched: Boolean,
    ): Boolean = !dexMetaKeyCapture && (enableAccessibilityAuto || keyInterceptorLaunched || true)

    fun isPauseEscEnabled(
        dexMetaKeyCapture: Boolean,
        enableAccessibilityAuto: Boolean,
        keyInterceptorLaunched: Boolean,
    ): Boolean = dexMetaKeyCapture || enableAccessibilityAuto || keyInterceptorLaunched

    fun isFilterWinKeyEnabled(
        enableAccessibilityAuto: Boolean,
        keyInterceptorLaunched: Boolean,
    ): Boolean = enableAccessibilityAuto || keyInterceptorLaunched

    fun hasScreenIdleTimeoutConflict(mode: String, systemTimeoutMs: Long): Boolean {
        if (mode == "never" || mode == "system") return false
        val minutes = mode.toLongOrNull() ?: return false
        return systemTimeoutMs > minutes * 60_000L
    }

    fun isValidCustomResolution(value: String?): Boolean = X11PreferenceBridge.isValidCustomResolution(value)
}

sealed interface ResolutionUpdateResult {
    data object Saved : ResolutionUpdateResult
    data object InvalidFormat : ResolutionUpdateResult
}

sealed interface AccessibilityToggleResult {
    data object Updated : AccessibilityToggleResult
    data object RequiresWriteSecureSettings : AccessibilityToggleResult
}

data class X11SettingsUiState(
    // Capabilities / environment
    val hasHardwareAccelerationFeature: Boolean = false,
    val hideCutoutAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
    val pointerCaptureAvailable: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
    val dexAvailable: Boolean = false,
    val stylusAvailable: Boolean = false,
    val keyInterceptorLaunched: Boolean = false,
    val notificationPermissionVisible: Boolean = false,
    val systemTimeoutMs: Long = 0L,
    val systemTimeoutFormatted: String = "",
    val x11VersionName: String = X11BuildConfig.VERSION_NAME,

    // Output
    val hardwareAcceleration: Boolean = true,
    val displayResolutionMode: String = "native",
    val displayScale: Int = 100,
    val displayResolutionExact: String = "1280x1024",
    val displayResolutionCustom: String = "1280x1024",
    val displayFilteringMode: String = "bilinear",
    val adjustResolution: Boolean = false,
    val displayStretch: Boolean = false,
    val reseed: Boolean = false,
    val pip: Boolean = false,
    val fullscreen: Boolean = false,
    val forceOrientation: String = "auto",
    val hideCutout: Boolean = false,
    val screenIdleTimeout: String = "system",

    // Pointer
    val touchMode: String = "1",
    val scaleTouchpad: Boolean = true,
    val showStylusClickOverride: Boolean = false,
    val stylusIsMouse: Boolean = false,
    val stylusButtonContactModifierMode: Boolean = false,
    val showMouseHelper: Boolean = false,
    val pointerCapture: Boolean = false,
    val transformCapturedPointer: String = "no",
    val capturedPointerSpeedFactor: Int = 100,
    val tapToMove: Boolean = true,
    val ignoreGamepadEvents: Boolean = false,
    val zoomFollowsCursor: Boolean = true,

    // Keyboard
    val showAdditionalKbd: Boolean = true,
    val additionalKbdVisible: Boolean = true,
    val showIMEWhileExternalConnected: Boolean = true,
    val preferScancodes: Boolean = false,
    val hardwareKbdScancodesWorkaround: Boolean = true,
    val dexMetaKeyCapture: Boolean = false,
    val enableAccessibilityServiceAutomatically: Boolean = false,
    val pauseKeyInterceptingWithEsc: Boolean = false,
    val filterOutWinkey: Boolean = false,
    val enforceCharBasedInput: Boolean = false,

    // Other
    val clipboardEnable: Boolean = true,
    val storeSecondaryDisplayPreferencesSeparately: Boolean = false,

    // EkBar
    val adjustHeightForEK: Boolean = true,
    val useTermuxEKBarBehaviour: Boolean = true,
    val ekbarPosition: String = "bottom",
    val ekbarPositionIgnoreOrientation: Boolean = false,
    val opacityEKBar: Int = 100,
    val extraKeysConfig: String = TermuxX11ExtraKeys.DEFAULT_IVALUE_EXTRA_KEYS,

    // User Actions
    val swipeUpAction: String = "no action",
    val swipeDownAction: String = "toggle additional key bar",
    val volumeUpAction: String = "no action",
    val volumeDownAction: String = "no action",
    val backButtonAction: String = "toggle soft keyboard",
    val notificationTapAction: String = "open preferences",
    val notificationButton0Action: String = "open preferences",
    val notificationButton1Action: String = "exit",
    val mediaKeysAction: String = "no action",

    // Localized Choice Option Lists (key -> choices)
    val choiceOptionsByKey: Map<String, List<ChoiceOption>> = emptyMap(),
) {
    val isDisplayScaleVisible: Boolean get() = X11SettingsRules.isDisplayScaleVisible(displayResolutionMode)
    val isDisplayExactVisible: Boolean get() = X11SettingsRules.isDisplayExactVisible(displayResolutionMode)
    val isDisplayCustomVisible: Boolean get() = X11SettingsRules.isDisplayCustomVisible(displayResolutionMode)
    val isDisplayStretchEnabled: Boolean get() = X11SettingsRules.isDisplayStretchEnabled(displayResolutionMode)
    val isScaleTouchpadEnabled: Boolean get() = X11SettingsRules.isScaleTouchpadEnabled(touchMode, displayResolutionMode)
    val isMouseHelperEnabled: Boolean get() = X11SettingsRules.isMouseHelperEnabled(touchMode)
    val isDexCaptureEnabled: Boolean get() = !enableAccessibilityServiceAutomatically
    val isAutoAccessibilityEnabled: Boolean get() = !dexMetaKeyCapture
    val isPauseEscEnabled: Boolean get() =
        X11SettingsRules.isPauseEscEnabled(dexMetaKeyCapture, enableAccessibilityServiceAutomatically, keyInterceptorLaunched)
    val isFilterWinKeyEnabled: Boolean get() =
        X11SettingsRules.isFilterWinKeyEnabled(enableAccessibilityServiceAutomatically, keyInterceptorLaunched)
    val hasScreenIdleConflict: Boolean get() =
        X11SettingsRules.hasScreenIdleTimeoutConflict(screenIdleTimeout, systemTimeoutMs)

    fun labelForChoice(key: String, currentValue: String): String =
        choiceOptionsByKey[key]?.firstOrNull { it.value == currentValue }?.label ?: currentValue
}

class X11SettingsRepository(
    private val context: Context,
    private val displayId: Int = Display.DEFAULT_DISPLAY,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs: Prefs = resolveOrCreatePrefs()
    private val _state = MutableStateFlow(readState(context))
    val state: StateFlow<X11SettingsUiState> = _state.asStateFlow()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == LoriePreferences.ACTION_PREFERENCES_CHANGED &&
                intent.getBooleanExtra("fromBroadcast", false)
            ) {
                refresh()
            }
        }
    }

    private val refreshRunnable = Runnable { refresh() }
    private val accessibilityObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            mainHandler.removeCallbacks(refreshRunnable)
            mainHandler.postDelayed(refreshRunnable, 150)
        }
    }

    init {
        val touchModeInt = prefs.touchMode.get().toIntOrNull() ?: 1
        if (touchModeInt - 1 > 2 || touchModeInt < 1) {
            prefs.touchMode.put("1")
        }
        refresh()
    }

    private fun resolveOrCreatePrefs(): Prefs {
        val resolved = X11PreferenceBridge.resolvePrefs(context, displayId)
        if (resolved != null) return resolved
        return Prefs().apply {
            attach(context, context.getSharedPreferences(LorieApp.X11_PREFS_NAME, Context.MODE_PRIVATE))
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun startObserving() {
        val filter = IntentFilter(LoriePreferences.ACTION_PREFERENCES_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, filter)
        }
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            true,
            accessibilityObserver,
        )
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
            true,
            accessibilityObserver,
        )
        refresh()
    }

    fun stopObserving() {
        runCatching { context.unregisterReceiver(broadcastReceiver) }
        runCatching { context.contentResolver.unregisterContentObserver(accessibilityObserver) }
        mainHandler.removeCallbacks(refreshRunnable)
    }

    fun refresh(localizedContext: Context = context) {
        _state.value = readState(localizedContext)
    }

    private fun buildChoiceMap(res: Resources, systemTimeoutText: String, localizedContext: Context): Map<String, List<ChoiceOption>> {
        val map = mutableMapOf<String, List<ChoiceOption>>()
        for ((key, pref) in prefs.keys) {
            if (pref is LoriePreferences.PrefsProto.ListPreference) {
                val values = pref.getValues(res)
                val entries = pref.getEntries(res)
                val options = values.indices.map { i ->
                    val v = values[i]
                    var label = entries.getOrElse(i) { v }
                    if (key == "screenIdleTimeout" && v == "system") {
                        label = localizedContext.getString(X11R.string.lorie_pref_screenIdleTimeoutSystem, systemTimeoutText)
                    } else if (v == "no action") {
                        label = when (key) {
                            "volumeUpAction", "volumeDownAction" -> localizedContext.getString(
                                X11R.string.lorie_no_action_with_default,
                                localizedContext.getString(X11R.string.lorie_default_action_volume),
                            )
                            "mediaKeysAction" -> localizedContext.getString(
                                X11R.string.lorie_no_action_with_default,
                                localizedContext.getString(X11R.string.lorie_default_action_media),
                            )
                            else -> label
                        }
                    }
                    ChoiceOption(value = v, label = label)
                }
                map[key] = options
            }
        }
        return map
    }

    private fun readState(localizedContext: Context): X11SettingsUiState {
        val systemTimeoutMs = X11PreferenceBridge.readSystemScreenOffTimeoutMs(context)
        val systemTimeoutText = X11PreferenceBridge.formatTimeout(localizedContext, systemTimeoutMs)
        return X11SettingsUiState(
            hasHardwareAccelerationFeature = true,
            hardwareAcceleration = prefs.hardwareAcceleration.get(),
            dexAvailable = X11PreferenceBridge.isDexAvailable(),
            stylusAvailable = X11PreferenceBridge.hasStylusInputDevice(),
            keyInterceptorLaunched = X11PreferenceBridge.isKeyInterceptorLaunched(),
            notificationPermissionVisible = !X11PreferenceBridge.hasPostNotifications(context),
            systemTimeoutMs = systemTimeoutMs,
            systemTimeoutFormatted = systemTimeoutText,
            displayResolutionMode = prefs.displayResolutionMode.get(),
            displayScale = prefs.displayScale.get(),
            displayResolutionExact = prefs.displayResolutionExact.get(),
            displayResolutionCustom = prefs.displayResolutionCustom.get(),
            displayFilteringMode = prefs.displayFilteringMode.get(),
            adjustResolution = prefs.adjustResolution.get(),
            displayStretch = prefs.displayStretch.get(),
            reseed = prefs.Reseed.get(),
            pip = prefs.PIP.get(),
            fullscreen = prefs.fullscreen.get(),
            forceOrientation = prefs.forceOrientation.get(),
            hideCutout = prefs.hideCutout.get(),
            screenIdleTimeout = prefs.screenIdleTimeout.get(),
            touchMode = prefs.touchMode.get(),
            scaleTouchpad = prefs.scaleTouchpad.get(),
            showStylusClickOverride = prefs.showStylusClickOverride.get(),
            stylusIsMouse = prefs.stylusIsMouse.get(),
            stylusButtonContactModifierMode = prefs.stylusButtonContactModifierMode.get(),
            showMouseHelper = prefs.showMouseHelper.get(),
            pointerCapture = prefs.pointerCapture.get(),
            transformCapturedPointer = prefs.transformCapturedPointer.get(),
            capturedPointerSpeedFactor = prefs.capturedPointerSpeedFactor.get(),
            tapToMove = prefs.tapToMove.get(),
            ignoreGamepadEvents = prefs.ignoreGamepadEvents.get(),
            zoomFollowsCursor = prefs.zoomFollowsCursor.get(),
            showAdditionalKbd = prefs.showAdditionalKbd.get(),
            additionalKbdVisible = prefs.additionalKbdVisible.get(),
            showIMEWhileExternalConnected = prefs.showIMEWhileExternalConnected.get(),
            preferScancodes = prefs.preferScancodes.get(),
            hardwareKbdScancodesWorkaround = prefs.hardwareKbdScancodesWorkaround.get(),
            dexMetaKeyCapture = prefs.dexMetaKeyCapture.get(),
            enableAccessibilityServiceAutomatically = prefs.enableAccessibilityServiceAutomatically.get(),
            pauseKeyInterceptingWithEsc = prefs.pauseKeyInterceptingWithEsc.get(),
            filterOutWinkey = prefs.filterOutWinkey.get(),
            enforceCharBasedInput = prefs.enforceCharBasedInput.get(),
            clipboardEnable = prefs.clipboardEnable.get(),
            storeSecondaryDisplayPreferencesSeparately = prefs.storeSecondaryDisplayPreferencesSeparately.get(),
            adjustHeightForEK = prefs.adjustHeightForEK.get(),
            useTermuxEKBarBehaviour = prefs.useTermuxEKBarBehaviour.get(),
            ekbarPosition = prefs.ekbarPosition.get(),
            ekbarPositionIgnoreOrientation = prefs.ekbarPositionIgnoreOrientation.get(),
            opacityEKBar = prefs.opacityEKBar.get(),
            extraKeysConfig = prefs.extra_keys_config.get(),
            swipeUpAction = prefs.swipeUpAction.get(),
            swipeDownAction = prefs.swipeDownAction.get(),
            volumeUpAction = prefs.volumeUpAction.get(),
            volumeDownAction = prefs.volumeDownAction.get(),
            backButtonAction = prefs.backButtonAction.get(),
            notificationTapAction = prefs.notificationTapAction.get(),
            notificationButton0Action = prefs.notificationButton0Action.get(),
            notificationButton1Action = prefs.notificationButton1Action.get(),
            mediaKeysAction = prefs.mediaKeysAction.get(),
            choiceOptionsByKey = buildChoiceMap(localizedContext.resources, systemTimeoutText, localizedContext),
        )
    }

    fun setBoolean(key: String, value: Boolean) {
        val pref = prefs.keys[key] ?: return
        if (pref is LoriePreferences.PrefsProto.BooleanPreference) {
            pref.put(value)
        } else {
            prefs.putBoolean(key, value)
        }
        if (key == "showAdditionalKbd" && value) {
            prefs.additionalKbdVisible.put(true)
        }
        X11PreferenceBridge.notifyPreferenceChanged(context, key)
        refresh()
    }

    fun setListOrString(key: String, value: String) {
        val pref = prefs.keys[key]
        when (pref) {
            is LoriePreferences.PrefsProto.ListPreference -> pref.put(value)
            is LoriePreferences.PrefsProto.StringPreference -> pref.put(value)
            else -> prefs.putString(key, value)
        }
        X11PreferenceBridge.notifyPreferenceChanged(context, key)
        refresh()
    }

    fun setDisplayScale(rawValue: Int): Int {
        val snapped = X11SettingsRules.snapDisplayScale(rawValue)
        prefs.displayScale.put(snapped)
        X11PreferenceBridge.notifyPreferenceChanged(context, "displayScale")
        refresh()
        return snapped
    }

    fun setInt(key: String, value: Int) {
        val pref = prefs.keys[key]
        if (pref is LoriePreferences.PrefsProto.IntPreference) {
            pref.put(value)
        } else {
            prefs.putInt(key, value)
        }
        X11PreferenceBridge.notifyPreferenceChanged(context, key)
        refresh()
    }

    fun setCustomResolution(rawValue: String): ResolutionUpdateResult {
        val trimmed = rawValue.trim()
        if (!X11SettingsRules.isValidCustomResolution(trimmed)) {
            return ResolutionUpdateResult.InvalidFormat
        }
        prefs.displayResolutionCustom.put(trimmed)
        X11PreferenceBridge.notifyPreferenceChanged(context, "displayResolutionCustom")
        refresh()
        return ResolutionUpdateResult.Saved
    }

    fun setAccessibilityAuto(enabled: Boolean): AccessibilityToggleResult {
        if (!enabled) {
            X11PreferenceBridge.shutdownKeyInterceptor(false)
            prefs.enableAccessibilityServiceAutomatically.put(false)
            X11PreferenceBridge.notifyPreferenceChanged(context, "enableAccessibilityServiceAutomatically")
            refresh()
            return AccessibilityToggleResult.Updated
        }
        if (!X11PreferenceBridge.hasWriteSecureSettings(context)) {
            return AccessibilityToggleResult.RequiresWriteSecureSettings
        }
        prefs.enableAccessibilityServiceAutomatically.put(true)
        X11PreferenceBridge.notifyPreferenceChanged(context, "enableAccessibilityServiceAutomatically")
        refresh()
        return AccessibilityToggleResult.Updated
    }

    fun setExtraKeysConfig(rawValue: String) {
        val valueToSave = rawValue.ifEmpty { TermuxX11ExtraKeys.DEFAULT_IVALUE_EXTRA_KEYS }
        prefs.extra_keys_config.put(valueToSave)
        X11PreferenceBridge.notifyPreferenceChanged(context, "extra_keys_config")
        refresh()
    }

    fun resetExtraKeysConfig() {
        prefs.extra_keys_config.put(TermuxX11ExtraKeys.DEFAULT_IVALUE_EXTRA_KEYS)
        X11PreferenceBridge.notifyPreferenceChanged(context, "extra_keys_config")
        refresh()
    }
}
