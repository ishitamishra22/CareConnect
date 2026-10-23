package com.nmims.careconnect.data.repository

import android.content.Context
import com.nmims.careconnect.data.firebase.Collections
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.Admission
import com.nmims.careconnect.data.model.AdmissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Cloud Firestore repository for hospital admission requests.
 * Uses FirebaseProvider.safeCall for all network/database operations.
 * Performs all sorting and multi-field filtering strictly in Kotlin to avoid requiring composite indexes.
 */
class AdmissionRepository(context: Context) {

    suspend fun getAll(): List<Admission> {
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.ADMISSIONS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Admission.fromMap(it) }
            }.sortedByDescending { it.createdAtMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun getById(id: String): Admission? {
        if (id.isBlank()) return null
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.ADMISSIONS)
                .document(id)
                .get()
                .await()

            if (snapshot.exists()) {
                snapshot.data?.let { Admission.fromMap(it) }
            } else {
                null
            }
        }
        return result.getOrNull()
    }

    suspend fun getForPatient(patientId: String): List<Admission> {
        if (patientId.isBlank()) return emptyList()
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.ADMISSIONS)
                .whereEqualTo("patientId", patientId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Admission.fromMap(it) }
            }.sortedByDescending { it.createdAtMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun getForHospital(hospitalId: String): List<Admission> {
        if (hospitalId.isBlank()) return emptyList()
        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.ADMISSIONS)
                .whereEqualTo("hospitalId", hospitalId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Admission.fromMap(it) }
            }.sortedWith(
                compareBy<Admission> { it.status != AdmissionStatus.PENDING }
                    .thenBy { it.preferredDateMillis }
            )
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun add(admission: Admission): Boolean {
        val result = FirebaseProvider.safeCall {
            FirebaseProvider.db
                .collection(Collections.ADMISSIONS)
                .document(admission.id)
                .set(admission.toMap())
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun updateStatus(
        id: String,
        status: AdmissionStatus,
        roomNumber: String? = null,
        remark: String? = null
    ): Boolean {
        if (id.isBlank()) return false
        val result = FirebaseProvider.safeCall {
            val updates = mutableMapOf<String, Any?>(
                "status" to status.name
            )
            if (roomNumber != null) {
                updates["assignedRoomNumber"] = roomNumber
                updates["assignedRoom"] = roomNumber
            }
            if (remark != null) {
                updates["adminRemark"] = remark
            }

            FirebaseProvider.db
                .collection(Collections.ADMISSIONS)
                .document(id)
                .update(updates)
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    /**
     * Stubs for real-time observation to maintain project compilation.
     * Full Firestore snapshot listener implementation is deferred to Milestone 4.
     */
    fun observeForPatient(patientId: String): Flow<List<Admission>> = flow {
        emit(getForPatient(patientId))
    }

    fun observeForHospital(hospitalId: String): Flow<List<Admission>> = flow {
        emit(getForHospital(hospitalId))
    }
}

/**
 * Singleton provider for AdmissionRepository.
 */
object AdmissionRepositoryProvider {
    @Volatile
    private var instance: AdmissionRepository? = null

    fun get(context: Context): AdmissionRepository {
        return instance ?: synchronized(this) {
            instance ?: AdmissionRepository(context.applicationContext).also { instance = it }
        }
    }
}
