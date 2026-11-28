package com.termux.app.settings.data

import android.content.Context
import android.os.Environment
import com.termux.R
import com.termux.app.models.UserAction
import com.termux.shared.activities.ReportActivity
import com.termux.shared.android.AndroidUtils
import com.termux.shared.android.PackageUtils
import com.termux.shared.file.FileUtils
import com.termux.shared.interact.ShareUtils
import com.termux.shared.logger.Logger
import com.termux.shared.models.ReportInfo
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import com.termux.shared.termux.settings.preferences.TermuxAPIAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxFloatAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxTaskerAppSharedPreferences
import com.termux.shared.termux.settings.preferences.TermuxWidgetAppSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
enum class PluginKind {
    API,
    FLOAT,
    TASKER,
    WIDGET,
}

data class ChoiceOption(
    val value: String,
    val label: String,
)

data class InstalledPluginUi(
    val kind: PluginKind,
    val titleRes: Int,
    val summaryRes: Int,
    val logLevel: Int,
    val supportsKeyLogging: Boolean = false,
    val keyLoggingEnabled: Boolean = false,
)

data class TermuxSettingsUiState(
    val logLevel: Int = 1,
    val logLevelOptions: List<ChoiceOption> = emptyList(),
    val terminalViewKeyLogging: Boolean = false,
    val pluginErrorNotifications: Boolean = true,
    val crashReportNotifications: Boolean = true,
    val softKeyboardEnabled: Boolean = true,
    val softKeyboardOnlyIfNoHardware: Boolean = false,
    val terminalMarginAdjustment: Boolean = true,
    val installedPlugins: List<InstalledPluginUi> = emptyList(),
    val showDonate: Boolean = false,
)

class TermuxSettingsRepository(private val appContext: Context) {

    private val appPrefs: TermuxAppSharedPreferences? = TermuxAppSharedPreferences.build(appContext, true)
    private val _state = MutableStateFlow(readInitialState())
    val state: StateFlow<TermuxSettingsUiState> = _state.asStateFlow()

    private fun buildLogLevelOptions(context: Context): List<ChoiceOption> {
        val levels = Logger.getLogLevelsArray()
        val labels = Logger.getLogLevelLabelsArray(context, levels, true)
        return levels.indices.map { i ->
            ChoiceOption(value = levels[i].toString(), label = labels[i].toString())
        }
    }

    private fun readInitialState(): TermuxSettingsUiState {
        val p = appPrefs
        return TermuxSettingsUiState(
            logLevel = p?.getLogLevel() ?: 1,
            logLevelOptions = buildLogLevelOptions(appContext),
            terminalViewKeyLogging = p?.isTerminalViewKeyLoggingEnabled() ?: false,
            pluginErrorNotifications = p?.arePluginErrorNotificationsEnabled(false) ?: true,
            crashReportNotifications = p?.areCrashReportNotificationsEnabled(false) ?: true,
            softKeyboardEnabled = p?.isSoftKeyboardEnabled() ?: true,
            softKeyboardOnlyIfNoHardware = p?.isSoftKeyboardEnabledOnlyIfNoHardware() ?: false,
            terminalMarginAdjustment = p?.isTerminalMarginAdjustmentEnabled() ?: true,
        )
    }

    suspend fun refreshDynamicAvailability(localizedContext: Context) {
        val options = buildLogLevelOptions(localizedContext)
        val (plugins, donateVisible) = withContext(Dispatchers.IO) {
            val foundPlugins = buildList {
                TermuxAPIAppSharedPreferences.build(appContext, false)?.let { prefs ->
                    add(
                        InstalledPluginUi(
                            kind = PluginKind.API,
                            titleRes = R.string.termux_api_preferences_title,
                            summaryRes = R.string.termux_api_preferences_summary,
                            logLevel = prefs.getLogLevel(true),
                        ),
                    )
                }
                TermuxFloatAppSharedPreferences.build(appContext, false)?.let { prefs ->
                    add(
                        InstalledPluginUi(
                            kind = PluginKind.FLOAT,
                            titleRes = R.string.termux_float_preferences_title,
                            summaryRes = R.string.termux_float_preferences_summary,
                            logLevel = prefs.getLogLevel(true),
                            supportsKeyLogging = true,
                            keyLoggingEnabled = prefs.isTerminalViewKeyLoggingEnabled(true),
                        ),
                    )
                }
                TermuxTaskerAppSharedPreferences.build(appContext, false)?.let { prefs ->
                    add(
                        InstalledPluginUi(
                            kind = PluginKind.TASKER,
                            titleRes = R.string.termux_tasker_preferences_title,
                            summaryRes = R.string.termux_tasker_preferences_summary,
                            logLevel = prefs.getLogLevel(true),
                        ),
                    )
                }
                TermuxWidgetAppSharedPreferences.build(appContext, false)?.let { prefs ->
                    add(
                        InstalledPluginUi(
                            kind = PluginKind.WIDGET,
                            titleRes = R.string.termux_widget_preferences_title,
                            summaryRes = R.string.termux_widget_preferences_summary,
                            logLevel = prefs.getLogLevel(true),
                        ),
                    )
                }
            }

            val digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(appContext)
            val isPlayStore = if (digest != null) {
                val apkRelease = TermuxUtils.getAPKRelease(digest)
                apkRelease == null || apkRelease == TermuxConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST
            } else {
                false
            }
            foundPlugins to !isPlayStore
        }

        _state.update { current ->
            current.copy(
                logLevelOptions = options,
                installedPlugins = plugins,
                showDonate = donateVisible,
            )
        }
    }

    fun setLogLevel(level: Int) {
        appPrefs?.setLogLevel(appContext, level)
        _state.update { it.copy(logLevel = level) }
    }

    fun setTerminalViewKeyLogging(enabled: Boolean) {
        appPrefs?.setTerminalViewKeyLoggingEnabled(enabled)
        _state.update { it.copy(terminalViewKeyLogging = enabled) }
    }

    fun setPluginErrorNotifications(enabled: Boolean) {
        appPrefs?.setPluginErrorNotificationsEnabled(enabled)
        _state.update { it.copy(pluginErrorNotifications = enabled) }
    }

    fun setCrashReportNotifications(enabled: Boolean) {
        appPrefs?.setCrashReportNotificationsEnabled(enabled)
        _state.update { it.copy(crashReportNotifications = enabled) }
    }

    fun setSoftKeyboardEnabled(enabled: Boolean) {
        appPrefs?.setSoftKeyboardEnabled(enabled)
        _state.update { it.copy(softKeyboardEnabled = enabled) }
    }

    fun setSoftKeyboardOnlyIfNoHardware(enabled: Boolean) {
        appPrefs?.setSoftKeyboardEnabledOnlyIfNoHardware(enabled)
        _state.update { it.copy(softKeyboardOnlyIfNoHardware = enabled) }
    }

    fun setTerminalMarginAdjustment(enabled: Boolean) {
        appPrefs?.setTerminalMarginAdjustment(enabled)
        _state.update { it.copy(terminalMarginAdjustment = enabled) }
    }

    fun setPluginLogLevel(kind: PluginKind, level: Int) {
        when (kind) {
            PluginKind.API -> TermuxAPIAppSharedPreferences.build(appContext, true)?.setLogLevel(appContext, level, true)
            PluginKind.FLOAT -> TermuxFloatAppSharedPreferences.build(appContext, true)?.setLogLevel(appContext, level, true)
            PluginKind.TASKER -> TermuxTaskerAppSharedPreferences.build(appContext, true)?.setLogLevel(appContext, level, true)
            PluginKind.WIDGET -> TermuxWidgetAppSharedPreferences.build(appContext, true)?.setLogLevel(appContext, level, true)
        }
        _state.update { current ->
            current.copy(
                installedPlugins = current.installedPlugins.map { plugin ->
                    if (plugin.kind == kind) plugin.copy(logLevel = level) else plugin
                },
            )
        }
    }

    fun setPluginKeyLogging(kind: PluginKind, enabled: Boolean) {
        if (kind == PluginKind.FLOAT) {
            TermuxFloatAppSharedPreferences.build(appContext, true)?.setTerminalViewKeyLoggingEnabled(enabled, true)
        }
        _state.update { current ->
            current.copy(
                installedPlugins = current.installedPlugins.map { plugin ->
                    if (plugin.kind == kind && plugin.supportsKeyLogging) {
                        plugin.copy(keyLoggingEnabled = enabled)
                    } else {
                        plugin
                    }
                },
            )
        }
    }

    suspend fun openAboutReport(context: Context) {
        val reportInfo = withContext(Dispatchers.IO) {
            val title = context.getString(R.string.about_preference_title)
            val aboutString = buildString {
                append(TermuxUtils.getAppInfoMarkdownString(context, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES))
                append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true))
                append("\n\n").append(TermuxUtils.getImportantLinksMarkdownString(context))
            }
            val userActionName = UserAction.ABOUT.getName()
            ReportInfo(
                userActionName,
                TermuxConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME,
                title,
            ).apply {
                setReportString(aboutString)
                setReportSaveFileLabelAndPath(
                    userActionName,
                    Environment.getExternalStorageDirectory().toString() + "/" +
                        FileUtils.sanitizeFileName(TermuxConstants.TERMUX_APP_NAME + "-" + userActionName + ".log", true, true),
                )
            }
        }
        ReportActivity.startReportActivity(context, reportInfo)
    }

    fun openDonate(context: Context) {
        ShareUtils.openUrl(context, TermuxConstants.TERMUX_DONATE_URL)
    }
}
