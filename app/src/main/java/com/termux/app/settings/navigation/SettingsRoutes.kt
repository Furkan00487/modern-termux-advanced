package com.termux.app.settings.navigation

import androidx.navigation3.runtime.NavKey
import com.termux.app.settings.data.PluginKind
import kotlinx.serialization.Serializable

sealed interface SettingsRoute : NavKey {
    @Serializable
    data object RootList : SettingsRoute

    @Serializable
    data object TermuxDebugging : SettingsRoute

    @Serializable
    data object TermuxTerminalIO : SettingsRoute

    @Serializable
    data object TermuxTerminalView : SettingsRoute

    @Serializable
    data class TermuxPlugin(val plugin: PluginKind) : SettingsRoute

    @Serializable
    data object X11Output : SettingsRoute

    @Serializable
    data object X11Pointer : SettingsRoute

    @Serializable
    data object X11Keyboard : SettingsRoute

    @Serializable
    data object X11ExtraKeysBar : SettingsRoute

    @Serializable
    data object X11Other : SettingsRoute

    @Serializable
    data object X11UserActions : SettingsRoute
}
