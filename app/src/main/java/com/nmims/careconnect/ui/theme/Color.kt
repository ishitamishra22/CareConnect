package com.nmims.careconnect.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================================
// Light Theme Palette
// =========================================================================
val BackgroundLight = Color(0xFFF7F9FC)
val SurfaceLight = Color(0xFFFFFFFF)
val ElevatedSurfaceLight = Color(0xFFE5F5F2)
val PrimaryLight = Color(0xFF087F8C)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE5F5F2)
val OnPrimaryContainerLight = Color(0xFF00383F)

val TextPrimaryLight = Color(0xFF16324F)
val TextSecondaryLight = Color(0xFF60758A)
val OutlineLight = Color(0xFFE6ECF2)
val OutlineVariantLight = Color(0xFFCBD5E1)

val SecondaryLight = Color(0xFF16324F)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE1E8F0)
val OnSecondaryContainerLight = Color(0xFF0C1F33)

val TertiaryLight = Color(0xFF2E8B75)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD2F0E7)
val OnTertiaryContainerLight = Color(0xFF002019)

val MedicalErrorRed = Color(0xFFBA1A1A)
val MedicalOnErrorRed = Color(0xFFFFFFFF)
val MedicalErrorContainer = Color(0xFFFFDAD6)
val MedicalOnErrorContainer = Color(0xFF410002)

// =========================================================================
// Dark Theme Palette
// =========================================================================
val BackgroundDark = Color(0xFF0B1220)
val SurfaceDark = Color(0xFF162235)
val ElevatedSurfaceDark = Color(0xFF1E3044)
val PrimaryDark = Color(0xFF42D6C4)
val OnPrimaryDark = Color(0xFF003734)
val PrimaryContainerDark = Color(0xFF1E3044)
val OnPrimaryContainerDark = Color(0xFFA1F2E7)

val TextPrimaryDark = Color(0xFFF1F5F9)
val TextSecondaryDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF28384C)
val OutlineVariantDark = Color(0xFF33465E)

val SecondaryDark = Color(0xFF90CDF4)
val OnSecondaryDark = Color(0xFF003444)
val SecondaryContainerDark = Color(0xFF1E3044)
val OnSecondaryContainerDark = Color(0xFFD6E4EB)

val TertiaryDark = Color(0xFF6AD8B8)
val OnTertiaryDark = Color(0xFF00382B)
val TertiaryContainerDark = Color(0xFF135040)
val OnTertiaryContainerDark = Color(0xFF88F6D4)

val MedicalErrorContainerDark = Color(0xFF93000A)

// =========================================================================
// Backward Compatibility Aliases for Unrelated Modules
// =========================================================================
val MedicalTealPrimary = PrimaryLight
val MedicalTealOnPrimary = OnPrimaryLight
val MedicalTealContainer = PrimaryContainerLight
val MedicalTealOnContainer = OnPrimaryContainerLight

val SoftBlueSecondary = SecondaryLight
val SoftBlueOnSecondary = OnSecondaryLight
val SoftBlueContainer = SecondaryContainerLight
val SoftBlueOnContainer = OnSecondaryContainerLight

val MintTertiary = TertiaryLight
val MintOnTertiary = OnTertiaryLight
val MintTertiaryContainer = TertiaryContainerLight
val MintOnTertiaryContainer = OnTertiaryContainerLight

val SurfaceVariantLight = OutlineLight
val OnSurfaceLight = TextPrimaryLight
val OnSurfaceVariantLight = TextSecondaryLight

val MedicalTealPrimaryDark = PrimaryDark
val MedicalTealOnPrimaryDark = OnPrimaryDark
val MedicalTealContainerDark = PrimaryContainerDark
val MedicalTealOnContainerDark = OnPrimaryContainerDark

val SoftBlueSecondaryDark = SecondaryDark
val SoftBlueOnSecondaryDark = OnSecondaryDark
val SoftBlueContainerDark = SecondaryContainerDark
val SoftBlueOnContainerDark = OnSecondaryContainerDark

val MintTertiaryDark = TertiaryDark
val MintOnTertiaryDark = OnTertiaryDark
val MintTertiaryContainerDark = TertiaryContainerDark
val MintOnTertiaryContainerDark = OnTertiaryContainerDark

val SurfaceVariantDark = ElevatedSurfaceDark
val OnSurfaceDark = TextPrimaryDark
val OnSurfaceVariantDark = TextSecondaryDark

/**
 * CompositionLocal providing elevated surface color tokens for light & dark themes.
 * Light: #E5F5F2, Dark: #1E3044
 */
val LocalElevatedSurface = staticCompositionLocalOf { ElevatedSurfaceLight }
