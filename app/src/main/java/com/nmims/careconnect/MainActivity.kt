package com.nmims.careconnect

import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.nmims.careconnect.receiver.BookingBroadcastReceiver
import com.nmims.careconnect.ui.theme.CareConnectTheme

class MainActivity : ComponentActivity() {

    private val bookingReceiver = BookingBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CareConnectTheme {
                CareConnectApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BookingBroadcastReceiver.ACTION_BOOKING_CONFIRMED)
        ContextCompat.registerReceiver(
            this,
            bookingReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(bookingReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver not registered
        }
    }
}
