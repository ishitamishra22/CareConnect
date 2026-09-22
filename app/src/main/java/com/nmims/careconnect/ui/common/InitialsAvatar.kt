package com.nmims.careconnect.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Coloured avatar showing initials extracted from a hospital or doctor name.
 */
@Composable
fun InitialsAvatar(
    name: String,
    colorHex: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val initials = extractInitials(name)
    val fontSize = (size.value * 0.4f).sp

    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun extractInitials(name: String): String {
    val cleanName = name
        .replace("Dr.", "", ignoreCase = true)
        .replace("Dr", "", ignoreCase = true)
        .trim()

    val parts = cleanName.split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "CC"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
    }
}
