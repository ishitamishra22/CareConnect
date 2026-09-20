package com.nmims.careconnect.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility functions for date formatting and validation.
 */
object DateUtils {
    private val standardFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    fun format(millis: Long): String {
        return standardFormat.format(Date(millis))
    }

    fun formatWithDay(millis: Long): String {
        return dayFormat.format(Date(millis))
    }

    fun todayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun isFuture(millis: Long): Boolean {
        return millis > System.currentTimeMillis()
    }
}
