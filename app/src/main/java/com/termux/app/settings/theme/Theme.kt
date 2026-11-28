package com.termux.app.settings.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

object ModernTermuxTheme {
    val extendedColors: ModernTermuxExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernTermuxTheme(
    content: @Composable () -> Unit,
) {
    val extendedColors = ModernTermuxExtendedColors()
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialExpressiveTheme(
            colorScheme = ModernTermuxDarkColorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = ModernTermuxShapes,
            typography = ModernTermuxTypography,
            content = content,
        )
    }
}
