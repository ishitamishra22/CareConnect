package com.nmims.careconnect.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Custom back stack navigation controller.
 * Manages an in-memory stack of Screen states without external navigation libraries.
 */
class AppNavController(
    initialScreen: Screen = Screen.RoleSelection
) {
    private val backStack: SnapshotStateList<Screen> = mutableStateListOf(initialScreen)

    val current: Screen
        get() = backStack.lastOrNull() ?: Screen.RoleSelection

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun back(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
            false
        }
    }

    fun popTo(screen: Screen) {
        val index = backStack.indexOfLast { it::class == screen::class }
        if (index != -1) {
            while (backStack.size > index + 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        } else {
            navigate(screen)
        }
    }

    fun resetTo(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }
}

@Composable
fun rememberAppNavController(
    initialScreen: Screen = Screen.RoleSelection
): AppNavController {
    return remember { AppNavController(initialScreen) }
}
