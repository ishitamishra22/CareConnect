package com.nmims.careconnect.receiver

/*
 * BookingBroadcastReceiver:
 * Broadcast receiver listening for com.nmims.careconnect.BOOKING_CONFIRMED.
 * Displays a confirmation Toast with the title and message provided in the intent extras.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BookingBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_BOOKING_CONFIRMED = "com.nmims.careconnect.BOOKING_CONFIRMED"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"

        fun sendBroadcast(context: Context, title: String, message: String) {
            val intent = Intent(ACTION_BOOKING_CONFIRMED).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == ACTION_BOOKING_CONFIRMED) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Booking Confirmed"
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
            val toastText = if (message.isNotBlank()) "$title: $message" else title
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }
    }
}
