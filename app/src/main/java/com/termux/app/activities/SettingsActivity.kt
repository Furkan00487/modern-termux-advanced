package com.termux.app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.termux.app.settings.navigation.SettingsNavHost
import com.termux.app.settings.theme.ModernTermuxTheme
import com.termux.x11.X11PreferenceBridge

enum class SettingsSection {
    TERMUX,
    X11,
}

/**
 * Unified Compose Expressive Settings Hub hosting both Termux and X11 settings cleanly separated.
 * When launched via the isolated {@code X11SettingsActivity} alias or with {@code extra_initial_section="x11"},
 * it opens directly on the X11 section and keeps back navigation anchored to X11.
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        val launchTarget = resolveLaunchTarget(intent)

        setContent {
            ModernTermuxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SettingsContent(
                        launchTarget = launchTarget,
                        onNavigateUp = { finish() },
                    )
                }
            }
        }
    }

    data class SettingsLaunchTarget(
        val section: SettingsSection,
        val subscreen: String?,
        val displayId: Int,
    )

    companion object {
        fun resolveLaunchTarget(intent: Intent?): SettingsLaunchTarget {
            val componentName = intent?.component?.className.orEmpty()
            val explicitSection = intent?.getStringExtra(X11PreferenceBridge.EXTRA_INITIAL_SECTION)
            val isX11 = explicitSection == X11PreferenceBridge.SECTION_X11 ||
                componentName.endsWith("X11SettingsActivity")
            val subscreen = intent?.getStringExtra(X11PreferenceBridge.EXTRA_INITIAL_SUBSCREEN)
            val displayId = intent?.getIntExtra(X11PreferenceBridge.EXTRA_DISPLAY_ID, 0) ?: 0
            return SettingsLaunchTarget(
                section = if (isX11) SettingsSection.X11 else SettingsSection.TERMUX,
                subscreen = subscreen,
                displayId = displayId,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    launchTarget: SettingsActivity.SettingsLaunchTarget,
    onNavigateUp: () -> Unit,
) {
    SettingsNavHost(
        launchTarget = launchTarget,
        onNavigateUp = onNavigateUp,
    )
}
