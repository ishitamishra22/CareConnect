package com.nmims.careconnect.data.repository

import android.content.Context
import com.nmims.careconnect.data.firebase.Collections
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import kotlinx.coroutines.tasks.await

/**
 * Exception thrown when attempting to book an OPD doctor slot that has already been taken.
 */
class SlotTakenException : Exception("The selected appointment slot has already been booked.")

/**
 * Repository managing OPD appointment and lab test bookings backed by Cloud Firestore.
 * Routes all database operations through FirebaseProvider.safeCall.
 * Sorts and filters multi-field queries in Kotlin to avoid requiring composite indexes.
 */
class BookingRepository(context: Context) {

    suspend fun getAll(): List<Booking> {
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Booking.fromMap(it) }
            }.sortedByDescending { it.dateMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun getById(id: String): Booking? {
        if (id.isBlank()) return null
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .document(id)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.data?.let { Booking.fromMap(it) }
            } else {
                null
            }
        }
        return result.getOrNull()
    }

    suspend fun getForPatient(patientId: String): List<Booking> {
        if (patientId.isBlank()) return emptyList()
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .whereEqualTo("patientId", patientId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Booking.fromMap(it) }
            }.sortedByDescending { it.dateMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun getForDoctor(doctorId: String): List<Booking> {
        if (doctorId.isBlank()) return emptyList()
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Booking.fromMap(it) }
            }.sortedWith(compareBy({ it.dateMillis }, { it.timeSlot }))
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun getForHospital(hospitalId: String): List<Booking> {
        if (hospitalId.isBlank()) return emptyList()
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .whereEqualTo("hospitalId", hospitalId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Booking.fromMap(it) }
            }.sortedByDescending { it.dateMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun getBookedSlots(doctorId: String, dateMillis: Long): List<String> {
        if (doctorId.isBlank()) return emptyList()
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Booking.fromMap(it) }
            }.filter {
                it.type == BookingType.OPD_APPOINTMENT &&
                        it.dateMillis == dateMillis &&
                        it.status != BookingStatus.CANCELLED
            }.map { it.timeSlot }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun add(booking: Booking): Boolean {
        // Slot collision verification for OPD doctor appointments
        if (booking.type == BookingType.OPD_APPOINTMENT && booking.doctorId != null) {
            val currentBookedSlots = getBookedSlots(booking.doctorId, booking.dateMillis)
            val isSlotTaken = currentBookedSlots.any { it.equals(booking.timeSlot, ignoreCase = true) }
            if (isSlotTaken) {
                throw SlotTakenException()
            }
        }

        val result = FirebaseProvider.safeCall {
            FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .document(booking.id)
                .set(booking.toMap())
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun updateStatus(id: String, status: BookingStatus, doctorNote: String? = null): Boolean {
        if (id.isBlank()) return false
        val result = FirebaseProvider.safeCall {
            val updates = mutableMapOf<String, Any?>("status" to status.name)
            if (doctorNote != null) {
                updates["doctorNote"] = doctorNote
            }
            FirebaseProvider.db
                .collection(Collections.BOOKINGS)
                .document(id)
                .update(updates)
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun cancel(id: String): Boolean {
        return updateStatus(id, BookingStatus.CANCELLED)
    }
}

/**
 * Singleton provider for BookingRepository.
 */
object BookingRepositoryProvider {
    @Volatile
    private var instance: BookingRepository? = null

    fun get(context: Context): BookingRepository {
        return instance ?: synchronized(this) {
            instance ?: BookingRepository(context.applicationContext).also { instance = it }
        }
    }
}
