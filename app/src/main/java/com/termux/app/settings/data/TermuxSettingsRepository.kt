package com.termux.app.settings.data

import android.content.Context
import android.os.Build
import com.termux.BuildConfig
import com.termux.R
import com.termux.shared.android.AndroidUtils
import com.termux.shared.android.PackageUtils
import com.termux.shared.android.SELinuxUtils
import com.termux.shared.interact.ShareUtils
import com.termux.shared.logger.Logger
import com.termux.shared.termux.TermuxBootstrap
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxUtils
import com.termux.shared.termux.file.TermuxFileUtils
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

data class TermuxAboutInfo(
    val variantName: String,
    val versionName: String,
    val versionCode: String,
    val packageName: String,
    val packageVariant: String,
    val packageManager: String,
    val uid: String,
    val targetSdk: String,
    val debuggable: Boolean,
    val apkRelease: String,
    val signingSha256: String,
    val seContext: String,
    val deviceModel: String,
    val androidVersion: String,
    val securityPatch: String?,
    val abiList: String,
    val primaryAbi: String,
    val filesDirWarning: String?,
    val markdownReport: String,
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

    suspend fun loadAboutInfo(context: Context): TermuxAboutInfo = withContext(Dispatchers.IO) {
        val sha256 = runCatching {
            PackageUtils.getSigningCertificateSHA256DigestForPackage(context)
        }.getOrNull().orEmpty().ifBlank { "unknown" }

        val versionName = runCatching {
            PackageUtils.getVersionNameForPackage(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME

        val versionCode = runCatching {
            PackageUtils.getVersionCodeForPackage(context)?.toString()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_CODE.toString()

        val packageName = runCatching {
            PackageUtils.getPackageNameForPackage(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: context.packageName

        val uid = runCatching {
            PackageUtils.getUidForPackage(context).toString()
        }.getOrNull() ?: android.os.Process.myUid().toString()

        val targetSdk = runCatching {
            PackageUtils.getTargetSDKForPackage(context).toString()
        }.getOrNull() ?: context.applicationInfo?.targetSdkVersion?.toString().orEmpty()

        val debuggable = runCatching {
            PackageUtils.isAppForPackageADebuggableBuild(context)
        }.getOrDefault(BuildConfig.DEBUG)

        val apkRelease = runCatching {
            TermuxUtils.getAPKRelease(sha256)
        }.getOrNull().orEmpty().ifBlank { "unknown" }

        val seContext = runCatching {
            SELinuxUtils.getContext()
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"

        val abis = Build.SUPPORTED_ABIS?.filterNotNull().orEmpty()
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val deviceModel = when {
            manufacturer.isNotEmpty() && model.startsWith(manufacturer, ignoreCase = true) -> model
            manufacturer.isNotEmpty() && model.isNotEmpty() -> "$manufacturer $model"
            model.isNotEmpty() -> model
            else -> Build.DEVICE?.trim().orEmpty().ifEmpty { "Android" }
        }

        val androidRelease = Build.VERSION.RELEASE?.trim().orEmpty().ifEmpty { Build.VERSION.SDK_INT.toString() }
        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        val filesDirWarning = runCatching {
            TermuxFileUtils.isTermuxFilesDirectoryAccessible(context, false, false)?.message
        }.getOrNull()?.takeIf { it.isNotBlank() }

        val markdownReport = runCatching {
            buildString {
                append(TermuxUtils.getAppInfoMarkdownString(context, TermuxUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES))
                append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true))
                append("\n\n").append(TermuxUtils.getImportantLinksMarkdownString(context))
            }
        }.getOrElse {
            "${TermuxConstants.MODERN_TERMUX_VARIANT_NAME} v$versionName ($versionCode)"
        }

        TermuxAboutInfo(
            variantName = TermuxConstants.MODERN_TERMUX_VARIANT_NAME,
            versionName = versionName,
            versionCode = versionCode,
            packageName = packageName,
            packageVariant = TermuxBootstrap.TERMUX_APP_PACKAGE_VARIANT?.getName() ?: "unknown",
            packageManager = TermuxBootstrap.TERMUX_APP_PACKAGE_MANAGER?.getName() ?: "unknown",
            uid = uid,
            targetSdk = targetSdk,
            debuggable = debuggable,
            apkRelease = apkRelease,
            signingSha256 = sha256,
            seContext = seContext,
            deviceModel = deviceModel,
            androidVersion = "Android $androidRelease (API ${Build.VERSION.SDK_INT})",
            securityPatch = securityPatch,
            abiList = abis.joinToString(", ").ifEmpty { "unknown" },
            primaryAbi = abis.firstOrNull() ?: "unknown",
            filesDirWarning = filesDirWarning,
            markdownReport = markdownReport,
        )
    }

    fun openExternalUrl(context: Context, url: String) {
        ShareUtils.openUrl(context, url)
    }

    fun shareAboutReport(context: Context, title: String, report: String) {
        ShareUtils.shareText(context, title, report)
    }

    fun openDonate(context: Context) {
        ShareUtils.openUrl(context, TermuxConstants.TERMUX_DONATE_URL)
    }
}
