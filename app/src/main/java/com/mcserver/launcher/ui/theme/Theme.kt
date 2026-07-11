package com.mcserver.launcher.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun darkColorScheme(
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    background: Color, onBackground: Color,
    surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    error: Color, onError: Color,
    outline: Color
): darkColorScheme {
    // Using Material3 darkColorScheme builder
    return androidx.compose.material3.darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        outline = outline
    )
}

private fun lightColorScheme(
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    background: Color, onBackground: Color,
    surface: Color, onSurface: Color,
    surfaceVariant: Color, onSurfaceVariant: Color,
    error: Color, onError: Color,
    outline: Color
): lightColorScheme {
    return androidx.compose.material3.lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        outline = outline
    )
}

@Composable
fun McServerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> lightColorScheme(
            primary = LightColors.Primary,
            onPrimary = LightColors.OnPrimary,
            primaryContainer = LightColors.PrimaryContainer,
            onPrimaryContainer = LightColors.OnPrimaryContainer,
            secondary = LightColors.Secondary,
            onSecondary = LightColors.OnSecondary,
            secondaryContainer = LightColors.SecondaryContainer,
            onSecondaryContainer = LightColors.OnSecondaryContainer,
            background = LightColors.Background,
            onBackground = LightColors.OnBackground,
            surface = LightColors.Surface,
            onSurface = LightColors.OnSurface,
            surfaceVariant = LightColors.SurfaceVariant,
            onSurfaceVariant = LightColors.OnSurfaceVariant,
            error = LightColors.Error,
            onError = LightColors.OnError,
            outline = LightColors.Outline
        )
        ThemeMode.DARK -> darkColorScheme(
            primary = DarkColors.Primary,
            onPrimary = DarkColors.OnPrimary,
            primaryContainer = DarkColors.PrimaryContainer,
            onPrimaryContainer = DarkColors.OnPrimaryContainer,
            secondary = DarkColors.Secondary,
            onSecondary = DarkColors.OnSecondary,
            secondaryContainer = DarkColors.SecondaryContainer,
            onSecondaryContainer = DarkColors.OnSecondaryContainer,
            background = DarkColors.Background,
            onBackground = DarkColors.OnBackground,
            surface = DarkColors.Surface,
            onSurface = DarkColors.OnSurface,
            surfaceVariant = DarkColors.SurfaceVariant,
            onSurfaceVariant = DarkColors.OnSurfaceVariant,
            error = DarkColors.Error,
            onError = DarkColors.OnError,
            outline = DarkColors.Outline
        )
        ThemeMode.AMOLED -> darkColorScheme(
            primary = AmoledColors.Primary,
            onPrimary = AmoledColors.OnPrimary,
            primaryContainer = AmoledColors.PrimaryContainer,
            onPrimaryContainer = AmoledColors.OnPrimaryContainer,
            secondary = AmoledColors.Secondary,
            onSecondary = AmoledColors.OnSecondary,
            secondaryContainer = AmoledColors.SecondaryContainer,
            onSecondaryContainer = AmoledColors.OnSecondaryContainer,
            background = AmoledColors.Background,
            onBackground = AmoledColors.OnBackground,
            surface = AmoledColors.Surface,
            onSurface = AmoledColors.OnSurface,
            surfaceVariant = AmoledColors.SurfaceVariant,
            onSurfaceVariant = AmoledColors.OnSurfaceVariant,
            error = AmoledColors.Error,
            onError = AmoledColors.OnError,
            outline = AmoledColors.Outline
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = themeMode == ThemeMode.LIGHT
                isAppearanceLightNavigationBars = themeMode == ThemeMode.LIGHT
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
