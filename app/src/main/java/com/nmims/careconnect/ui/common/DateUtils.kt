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

    /**
     * Normalizes a timestamp (such as UTC midnight from DatePicker) to local midnight.
     * 1. Reads year, month, day from a UTC Calendar
     * 2. Rebuilds local midnight using a default-timezone Calendar
     * 3. Returns local start-of-day millis
     */
    fun normalizeToDayStart(millis: Long): Long {
        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        val localCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }
}
