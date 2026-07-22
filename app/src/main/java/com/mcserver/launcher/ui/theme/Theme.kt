package com.mcserver.launcher.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun buildDarkScheme(
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color, tertiaryContainer: Color, onTertiaryContainer: Color,
    background: Color, onBackground: Color,
    surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    surfaceDim: Color,
    error: Color, onError: Color, errorContainer: Color, onErrorContainer: Color,
    outline: Color, outlineVariant: Color
): ColorScheme = darkColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    surfaceDim = surfaceDim,
    error = error, onError = onError,
    errorContainer = errorContainer, onErrorContainer = onErrorContainer,
    outline = outline, outlineVariant = outlineVariant
)

private fun buildLightScheme(
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, onTertiary: Color, tertiaryContainer: Color, onTertiaryContainer: Color,
    background: Color, onBackground: Color,
    surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    surfaceDim: Color,
    error: Color, onError: Color, errorContainer: Color, onErrorContainer: Color,
    outline: Color, outlineVariant: Color
): ColorScheme = lightColorScheme(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = secondary, onSecondary = onSecondary,
    secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary, onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
    background = background, onBackground = onBackground,
    surface = surface, onSurface = onSurface,
    surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
    surfaceDim = surfaceDim,
    error = error, onError = onError,
    errorContainer = errorContainer, onErrorContainer = onErrorContainer,
    outline = outline, outlineVariant = outlineVariant
)

@Composable
fun McServerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val targetScheme = when (themeMode) {
        ThemeMode.LIGHT -> buildLightScheme(
            LightColors.Primary, LightColors.OnPrimary,
            LightColors.PrimaryContainer, LightColors.OnPrimaryContainer,
            LightColors.Secondary, LightColors.OnSecondary,
            LightColors.SecondaryContainer, LightColors.OnSecondaryContainer,
            LightColors.Tertiary, LightColors.OnTertiary,
            LightColors.TertiaryContainer, LightColors.OnTertiaryContainer,
            LightColors.Background, LightColors.OnBackground,
            LightColors.Surface, LightColors.OnSurface,
            LightColors.SurfaceVariant, LightColors.OnSurfaceVariant,
            LightColors.SurfaceDim,
            LightColors.Error, LightColors.OnError,
            LightColors.ErrorContainer, LightColors.OnErrorContainer,
            LightColors.Outline, LightColors.OutlineVariant
        )
        ThemeMode.DARK -> buildDarkScheme(
            DarkColors.Primary, DarkColors.OnPrimary,
            DarkColors.PrimaryContainer, DarkColors.OnPrimaryContainer,
            DarkColors.Secondary, DarkColors.OnSecondary,
            DarkColors.SecondaryContainer, DarkColors.OnSecondaryContainer,
            DarkColors.Tertiary, DarkColors.OnTertiary,
            DarkColors.TertiaryContainer, DarkColors.OnTertiaryContainer,
            DarkColors.Background, DarkColors.OnBackground,
            DarkColors.Surface, DarkColors.OnSurface,
            DarkColors.SurfaceVariant, DarkColors.OnSurfaceVariant,
            DarkColors.SurfaceDim,
            DarkColors.Error, DarkColors.OnError,
            DarkColors.ErrorContainer, DarkColors.OnErrorContainer,
            DarkColors.Outline, DarkColors.OutlineVariant
        )
        ThemeMode.AMOLED -> buildDarkScheme(
            AmoledColors.Primary, AmoledColors.OnPrimary,
            AmoledColors.PrimaryContainer, AmoledColors.OnPrimaryContainer,
            AmoledColors.Secondary, AmoledColors.OnSecondary,
            AmoledColors.SecondaryContainer, AmoledColors.OnSecondaryContainer,
            AmoledColors.Tertiary, AmoledColors.OnTertiary,
            AmoledColors.TertiaryContainer, AmoledColors.OnTertiaryContainer,
            AmoledColors.Background, AmoledColors.OnBackground,
            AmoledColors.Surface, AmoledColors.OnSurface,
            AmoledColors.SurfaceVariant, AmoledColors.OnSurfaceVariant,
            AmoledColors.SurfaceDim,
            AmoledColors.Error, AmoledColors.OnError,
            AmoledColors.ErrorContainer, AmoledColors.OnErrorContainer,
            AmoledColors.Outline, AmoledColors.OutlineVariant
        )
    }

    // 主题切换时颜色平滑过渡（300ms）
    val animSpec = tween<Color>(durationMillis = 300)
    val animatedScheme = targetScheme.copy(
        primary = animateColorAsState(targetScheme.primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetScheme.onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetScheme.primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(targetScheme.secondary, animSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetScheme.onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetScheme.tertiary, animSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetScheme.onTertiary, animSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetScheme.tertiaryContainer, animSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetScheme.onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(targetScheme.background, animSpec, label = "background").value,
        onBackground = animateColorAsState(targetScheme.onBackground, animSpec, label = "onBackground").value,
        surface = animateColorAsState(targetScheme.surface, animSpec, label = "surface").value,
        onSurface = animateColorAsState(targetScheme.onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        surfaceDim = animateColorAsState(targetScheme.surfaceDim, animSpec, label = "surfaceDim").value,
        error = animateColorAsState(targetScheme.error, animSpec, label = "error").value,
        onError = animateColorAsState(targetScheme.onError, animSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetScheme.errorContainer, animSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetScheme.onErrorContainer, animSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetScheme.outline, animSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetScheme.outlineVariant, animSpec, label = "outlineVariant").value
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = animatedScheme.surface.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = animatedScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
                isAppearanceLightNavigationBars = themeMode == ThemeMode.LIGHT
            }
        }
    }

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = AppTypography,
        content = content
    )
}
