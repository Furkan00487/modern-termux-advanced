package com.termux.app.settings.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primitive AMOLED & Neon Cyan tokens
val OledBlack = Color(0xFF000000)
val SurfaceLowest = Color(0xFF05070A)
val SurfaceLow = Color(0xFF0B1016)
val SurfaceDefault = Color(0xFF111821)
val SurfaceHigh = Color(0xFF17212D)
val SurfaceHighest = Color(0xFF1E2A38)

val NeonCyan = Color(0xFF00E5FF)
val NeonCyanDeep = Color(0xFF001F26)
val NeonCyanContainer = Color(0xFF003844)
val NeonCyanSoft = Color(0xFFA2F2FF)

val TealSecondary = Color(0xFF5EEAD4)
val TealSecondaryDeep = Color(0xFF042F2E)
val TealSecondaryContainer = Color(0xFF0F3D3C)
val TealSecondarySoft = Color(0xFFCCFBF1)

val CobaltTertiary = Color(0xFF7C9EFF)
val CobaltTertiaryDeep = Color(0xFF0C1D4A)
val CobaltTertiaryContainer = Color(0xFF1A2F66)
val CobaltTertiarySoft = Color(0xFFDCE6FF)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val OutlineSubtle = Color(0xFF334155)
val OutlineHairline = Color(0xFF1E293B)

val SemanticError = Color(0xFFFF6B6B)
val SemanticOnError = Color(0xFF3B0909)
val SemanticErrorContainer = Color(0xFF5C1414)
val SemanticOnErrorContainer = Color(0xFFFFDAD6)

val SemanticWarning = Color(0xFFFFB74D)
val SemanticOnWarning = Color(0xFF331C00)
val SemanticWarningContainer = Color(0xFF472A00)

val ModernTermuxDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = NeonCyanDeep,
    primaryContainer = NeonCyanContainer,
    onPrimaryContainer = NeonCyanSoft,
    secondary = TealSecondary,
    onSecondary = TealSecondaryDeep,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = TealSecondarySoft,
    tertiary = CobaltTertiary,
    onTertiary = CobaltTertiaryDeep,
    tertiaryContainer = CobaltTertiaryContainer,
    onTertiaryContainer = CobaltTertiarySoft,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = OledBlack,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = SurfaceDefault,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    outline = OutlineSubtle,
    outlineVariant = OutlineHairline,
    error = SemanticError,
    onError = SemanticOnError,
    errorContainer = SemanticErrorContainer,
    onErrorContainer = SemanticOnErrorContainer,
)

@Immutable
data class ModernTermuxExtendedColors(
    val warning: Color = SemanticWarning,
    val onWarning: Color = SemanticOnWarning,
    val warningContainer: Color = SemanticWarningContainer,
    val cyanGlow: Color = NeonCyan.copy(alpha = 0.14f),
    val sectionBadgeContainer: Color = SurfaceHigh,
    val sectionBadgeContent: Color = NeonCyanSoft,
)

val LocalExtendedColors = staticCompositionLocalOf { ModernTermuxExtendedColors() }
