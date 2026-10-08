package com.nmims.careconnect.data.repository

import android.content.Context
import com.nmims.careconnect.data.model.Booking
import com.nmims.careconnect.data.model.BookingStatus
import com.nmims.careconnect.data.model.BookingType
import com.nmims.careconnect.data.storage.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Exception thrown when attempting to book an OPD doctor slot that has already been taken.
 */
class SlotTakenException : Exception("The selected appointment slot has already been booked.")

/**
 * Repository managing OPD appointment and lab test bookings.
 * Temporarily uses local JSON storage ("bookings.json") via FileStorageManager.
 * Runs all I/O strictly on Dispatchers.IO.
 */
class BookingRepository(context: Context) {

    private val storage = FileStorageManager(context.applicationContext)
    private val mutex = Mutex()
    private val fileName = "bookings.json"

    private suspend fun readAllBookings(): MutableList<Booking> = withContext(Dispatchers.IO) {
        val jsonText = storage.readText(fileName) ?: return@withContext mutableListOf()
        try {
            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<Booking>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                list.add(Booking.fromJson(obj))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    private suspend fun saveAllBookings(list: List<Booking>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            for (booking in list) {
                jsonArray.put(booking.toJson())
            }
            storage.writeText(fileName, jsonArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAll(): List<Booking> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAllBookings().toList()
        }
    }

    suspend fun getById(id: String): Booking? = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAllBookings().find { it.id == id }
        }
    }

    suspend fun getForPatient(patientId: String): List<Booking> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAllBookings()
                .filter { it.patientId == patientId }
                .sortedByDescending { it.dateMillis }
        }
    }

    suspend fun getForDoctor(doctorId: String): List<Booking> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAllBookings()
                .filter { it.doctorId == doctorId }
                .sortedWith(compareBy({ it.dateMillis }, { it.timeSlot }))
        }
    }

    suspend fun getForHospital(hospitalId: String): List<Booking> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAllBookings()
                .filter { it.hospitalId == hospitalId }
                .sortedByDescending { it.dateMillis }
        }
    }

    suspend fun getBookedSlots(doctorId: String, dateMillis: Long): List<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readAllBookings()
                .filter {
                    it.type == BookingType.OPD_APPOINTMENT &&
                    it.doctorId == doctorId &&
                    it.dateMillis == dateMillis &&
                    it.status != BookingStatus.CANCELLED
                }
                .map { it.timeSlot }
        }
    }

    suspend fun add(booking: Booking): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentList = readAllBookings()

            // Check if OPD appointment slot is already taken
            if (booking.type == BookingType.OPD_APPOINTMENT && booking.doctorId != null) {
                val isSlotTaken = currentList.any {
                    it.type == BookingType.OPD_APPOINTMENT &&
                    it.doctorId == booking.doctorId &&
                    it.dateMillis == booking.dateMillis &&
                    it.timeSlot.equals(booking.timeSlot, ignoreCase = true) &&
                    it.status != BookingStatus.CANCELLED
                }
                if (isSlotTaken) {
                    throw SlotTakenException()
                }
            }

            currentList.add(booking)
            saveAllBookings(currentList)
        }
    }

    suspend fun updateStatus(id: String, status: BookingStatus, doctorNote: String? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentList = readAllBookings()
            val index = currentList.indexOfFirst { it.id == id }
            if (index != -1) {
                val existing = currentList[index]
                val updated = existing.copy(
                    status = status,
                    doctorNote = doctorNote ?: existing.doctorNote
                )
                currentList[index] = updated
                saveAllBookings(currentList)
            } else {
                false
            }
        }
    }

    suspend fun cancel(id: String): Boolean = withContext(Dispatchers.IO) {
        updateStatus(id, BookingStatus.CANCELLED)
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
