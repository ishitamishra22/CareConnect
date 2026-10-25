package com.nmims.careconnect.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Supported user theme preferences for CareConnect.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromString(value: String?): ThemeMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

/**
 * Controller and state holder for app theme preferences.
 * Persists the selected mode across app launches using Android SharedPreferences.
 */
object ThemePreferences {
    private const val PREFS_NAME = "careconnect_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    private var sharedPreferences: SharedPreferences? = null

    val currentMode: MutableState<ThemeMode> = mutableStateOf(ThemeMode.SYSTEM)

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        currentMode.value = ThemeMode.fromString(saved)
    }

    fun setThemeMode(mode: ThemeMode) {
        currentMode.value = mode
        sharedPreferences?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }
}

/**
 * CompositionLocal to read and update ThemeMode within Composables.
 */
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
val LocalSetThemeMode = staticCompositionLocalOf<(ThemeMode) -> Unit> { {} }
