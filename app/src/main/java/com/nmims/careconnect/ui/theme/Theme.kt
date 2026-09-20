package com.nmims.careconnect.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = MedicalTealPrimary,
    onPrimary = MedicalTealOnPrimary,
    primaryContainer = MedicalTealContainer,
    onPrimaryContainer = MedicalTealOnContainer,
    secondary = SoftBlueSecondary,
    onSecondary = SoftBlueOnSecondary,
    secondaryContainer = SoftBlueContainer,
    onSecondaryContainer = SoftBlueOnContainer,
    tertiary = MintTertiary,
    onTertiary = MintOnTertiary,
    tertiaryContainer = MintTertiaryContainer,
    onTertiaryContainer = MintOnTertiaryContainer,
    error = MedicalErrorRed,
    onError = MedicalOnErrorRed,
    errorContainer = MedicalErrorContainer,
    onErrorContainer = MedicalOnErrorContainer,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = MedicalTealPrimaryDark,
    onPrimary = MedicalTealOnPrimaryDark,
    primaryContainer = MedicalTealContainerDark,
    onPrimaryContainer = MedicalTealOnContainerDark,
    secondary = SoftBlueSecondaryDark,
    onSecondary = SoftBlueOnSecondaryDark,
    secondaryContainer = SoftBlueContainerDark,
    onSecondaryContainer = SoftBlueOnContainerDark,
    tertiary = MintTertiaryDark,
    onTertiary = MintOnTertiaryDark,
    tertiaryContainer = MintTertiaryContainerDark,
    onTertiaryContainer = MintOnTertiaryContainerDark,
    error = MedicalErrorRed,
    onError = MedicalOnErrorRed,
    errorContainer = MedicalErrorContainerDark,
    onErrorContainer = MedicalOnErrorContainer,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

@Composable
fun CareConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
