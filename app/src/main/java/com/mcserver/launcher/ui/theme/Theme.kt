package com.mcserver.launcher.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class ExtendedColorScheme(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val online: Color,
    val offline: Color
)

private val LightExtendedColors = ExtendedColorScheme(
    success = LightColors.Success,
    onSuccess = LightColors.OnSuccess,
    successContainer = LightColors.SuccessContainer,
    onSuccessContainer = LightColors.OnSuccessContainer,
    warning = LightColors.Warning,
    onWarning = LightColors.OnWarning,
    warningContainer = LightColors.WarningContainer,
    onWarningContainer = LightColors.OnWarningContainer,
    info = LightColors.Info,
    onInfo = LightColors.OnInfo,
    infoContainer = LightColors.InfoContainer,
    onInfoContainer = LightColors.OnInfoContainer,
    online = LightColors.Online,
    offline = LightColors.Offline
)

private val DarkExtendedColors = ExtendedColorScheme(
    success = DarkColors.Success,
    onSuccess = DarkColors.OnSuccess,
    successContainer = DarkColors.SuccessContainer,
    onSuccessContainer = DarkColors.OnSuccessContainer,
    warning = DarkColors.Warning,
    onWarning = DarkColors.OnWarning,
    warningContainer = DarkColors.WarningContainer,
    onWarningContainer = DarkColors.OnWarningContainer,
    info = DarkColors.Info,
    onInfo = DarkColors.OnInfo,
    infoContainer = DarkColors.InfoContainer,
    onInfoContainer = DarkColors.OnInfoContainer,
    online = DarkColors.Online,
    offline = DarkColors.Offline
)

private val AmoledExtendedColors = ExtendedColorScheme(
    success = AmoledColors.Success,
    onSuccess = AmoledColors.OnSuccess,
    successContainer = AmoledColors.SuccessContainer,
    onSuccessContainer = AmoledColors.OnSuccessContainer,
    warning = AmoledColors.Warning,
    onWarning = AmoledColors.OnWarning,
    warningContainer = AmoledColors.WarningContainer,
    onWarningContainer = AmoledColors.OnWarningContainer,
    info = AmoledColors.Info,
    onInfo = AmoledColors.OnInfo,
    infoContainer = AmoledColors.InfoContainer,
    onInfoContainer = AmoledColors.OnInfoContainer,
    online = AmoledColors.Online,
    offline = AmoledColors.Offline
)

val LocalExtendedColorScheme = compositionLocalOf { LightExtendedColors }

@Composable
fun extendedColorScheme(): ExtendedColorScheme = LocalExtendedColorScheme.current

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

@Composable
fun McServerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme = when {
        isDynamicColorAvailable -> {
            when (themeMode) {
                ThemeMode.LIGHT -> dynamicLightColorScheme(context)
                ThemeMode.DARK -> dynamicDarkColorScheme(context)
                ThemeMode.AMOLED -> dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceDim = Color.Black,
                    surfaceVariant = Color(0xFF1A1A1A),
                    outline = Color(0xFF404040),
                    outlineVariant = Color(0xFF2A2A2A)
                )
            }
        }
        else -> {
            when (themeMode) {
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
        }
    }

    val extendedColors = when (themeMode) {
        ThemeMode.LIGHT -> LightExtendedColors
        ThemeMode.DARK -> DarkExtendedColors
        ThemeMode.AMOLED -> AmoledExtendedColors
    }

    val animSpec = tween<Color>(durationMillis = 300)
    val animatedScheme = baseScheme.copy(
        primary = animateColorAsState(baseScheme.primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(baseScheme.onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(baseScheme.primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(baseScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(baseScheme.secondary, animSpec, label = "secondary").value,
        onSecondary = animateColorAsState(baseScheme.onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(baseScheme.secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(baseScheme.onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(baseScheme.tertiary, animSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(baseScheme.onTertiary, animSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(baseScheme.tertiaryContainer, animSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(baseScheme.onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(baseScheme.background, animSpec, label = "background").value,
        onBackground = animateColorAsState(baseScheme.onBackground, animSpec, label = "onBackground").value,
        surface = animateColorAsState(baseScheme.surface, animSpec, label = "surface").value,
        onSurface = animateColorAsState(baseScheme.onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(baseScheme.surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(baseScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        surfaceDim = animateColorAsState(baseScheme.surfaceDim, animSpec, label = "surfaceDim").value,
        error = animateColorAsState(baseScheme.error, animSpec, label = "error").value,
        onError = animateColorAsState(baseScheme.onError, animSpec, label = "onError").value,
        errorContainer = animateColorAsState(baseScheme.errorContainer, animSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(baseScheme.onErrorContainer, animSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(baseScheme.outline, animSpec, label = "outline").value,
        outlineVariant = animateColorAsState(baseScheme.outlineVariant, animSpec, label = "outlineVariant").value
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
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
    }

    CompositionLocalProvider(LocalExtendedColorScheme provides extendedColors) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = AppTypography,
            content = content
        )
    }
}