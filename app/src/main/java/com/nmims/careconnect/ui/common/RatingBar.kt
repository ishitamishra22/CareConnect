package com.nmims.careconnect.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable RatingBar displaying filled, half, and outlined star icons.
 */
@Composable
fun RatingBar(
    rating: Double,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    size: Dp = 16.dp,
    tint: Color = Color(0xFFF59E0B) // Medical warm amber
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val clampedRating = rating.coerceIn(0.0, maxStars.toDouble())
        val fullStars = clampedRating.toInt()
        val remainder = clampedRating - fullStars
        val hasHalfStar = remainder >= 0.25 && remainder < 0.75
        val extraFull = if (remainder >= 0.75) 1 else 0
        val effectiveFullStars = fullStars + extraFull
        val emptyStars = (maxStars - effectiveFullStars - (if (hasHalfStar) 1 else 0)).coerceAtLeast(0)

        // Render filled stars
        repeat(effectiveFullStars.coerceAtMost(maxStars)) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size)
            )
        }

        // Render half star if applicable
        if (hasHalfStar && effectiveFullStars < maxStars) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.StarHalf,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size)
            )
        }

        // Render empty stars
        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Default.StarBorder,
                contentDescription = null,
                tint = tint.copy(alpha = 0.45f),
                modifier = Modifier.size(size)
            )
        }
    }
}

/**
 * Interactive RatingInput displaying five tappable star icons.
 */
@Composable
fun RatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    size: Dp = 32.dp,
    tint: Color = Color(0xFFF59E0B)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (star in 1..maxStars) {
            val isSelected = star <= rating
            androidx.compose.material3.IconButton(
                onClick = { onRatingChange(star) },
                modifier = Modifier.size(size + 8.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$star stars",
                    tint = if (isSelected) tint else tint.copy(alpha = 0.45f),
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}

