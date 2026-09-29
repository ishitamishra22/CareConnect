package com.nmims.careconnect.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.google.firebase.firestore.SetOptions
import com.nmims.careconnect.data.firebase.Collections
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.MedicalDocument
import com.nmims.careconnect.data.model.MedicalRecord
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.model.PortfolioSummary
import com.nmims.careconnect.data.storage.FileStorageManager
import com.nmims.careconnect.session.AppSession
import kotlinx.coroutines.tasks.await

/**
 * Repository orchestrating access to the patient profile, medical records,
 * and uploaded healthcare documents backed by Cloud Firestore and Firebase Storage.
 */
class PortfolioRepository(context: Context) {

    private val storage = FileStorageManager(context)

    suspend fun getProfile(): PatientProfile? {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return null

        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .get()
                .await()

            if (snapshot.exists()) {
                val data = snapshot.data
                if (data != null) PatientProfile.fromMap(data) else null
            } else {
                null
            }
        }
        return result.getOrNull()
    }

    suspend fun saveProfile(profile: PatientProfile): Boolean {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return false

        val result = FirebaseProvider.safeCall {
            val data = profile.toMap().toMutableMap()
            data["updatedAtMillis"] = System.currentTimeMillis()

            FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .set(data, SetOptions.merge())
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun getRecords(): List<MedicalRecord> {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return emptyList()

        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .collection(Collections.RECORDS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) MedicalRecord.fromMap(data) else null
            }.sortedByDescending { it.dateMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun addRecord(record: MedicalRecord): Boolean {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return false

        val result = FirebaseProvider.safeCall {
            FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .collection(Collections.RECORDS)
                .document(record.id)
                .set(record.toMap())
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun updateRecord(record: MedicalRecord): Boolean {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return false

        val result = FirebaseProvider.safeCall {
            FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .collection(Collections.RECORDS)
                .document(record.id)
                .set(record.toMap(), SetOptions.merge())
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun deleteRecord(recordId: String): Boolean {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return false

        val result = FirebaseProvider.safeCall {
            FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .collection(Collections.RECORDS)
                .document(recordId)
                .delete()
                .await()
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun getDocuments(): List<MedicalDocument> {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return emptyList()

        val result = FirebaseProvider.safeCall {
            val snapshot = FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .collection(Collections.DOCUMENTS)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) MedicalDocument.fromMap(data) else null
            }.sortedByDescending { it.addedOnMillis }
        }
        return result.getOrDefault(emptyList())
    }

    suspend fun addDocument(doc: MedicalDocument, imageBytes: ByteArray): Boolean {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return false

        val storagePath = doc.storagePath.ifBlank {
            "patients/$patientId/documents/${doc.id}.jpg"
        }
        val finalDoc = doc.copy(storagePath = storagePath)

        val result = FirebaseProvider.safeCall {
            val storageRef = FirebaseProvider.storage.reference.child(storagePath)
            storageRef.putBytes(imageBytes).await()

            try {
                FirebaseProvider.db
                    .collection(Collections.PATIENTS)
                    .document(patientId)
                    .collection(Collections.DOCUMENTS)
                    .document(finalDoc.id)
                    .set(finalDoc.toMap())
                    .await()
            } catch (e: Exception) {
                try {
                    storageRef.delete().await()
                } catch (_: Exception) {}
                throw e
            }

            try {
                val cache = storage.cacheFile(finalDoc.id)
                cache.writeBytes(imageBytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun addDocument(document: MedicalDocument): Boolean {
        val cache = storage.cacheFile(document.id)
        val bytes = if (cache.exists()) cache.readBytes() else ByteArray(0)
        return addDocument(document, bytes)
    }

    suspend fun loadDocumentBitmap(doc: MedicalDocument): Bitmap? {
        val cache = storage.cacheFile(doc.id)
        if (cache.exists()) {
            val bmp = storage.loadBitmap(cache.absolutePath)
            if (bmp != null) return bmp
        }

        val path = doc.storagePath
        if (path.isBlank()) return null

        val result = FirebaseProvider.safeCall {
            val storageRef = FirebaseProvider.storage.reference.child(path)
            val bytes = storageRef.getBytes(5L * 1024L * 1024L).await()
            try {
                cache.writeBytes(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            storage.loadBitmap(cache.absolutePath)
        }
        return result.getOrNull()
    }

    suspend fun deleteDocument(documentId: String): Boolean {
        val patientId = AppSession.currentPatientId
        if (patientId.isBlank()) return false

        val result = FirebaseProvider.safeCall {
            val docRef = FirebaseProvider.db
                .collection(Collections.PATIENTS)
                .document(patientId)
                .collection(Collections.DOCUMENTS)
                .document(documentId)

            val docSnap = docRef.get().await()
            val storagePath = docSnap.getString("storagePath")
                ?: "patients/$patientId/documents/$documentId.jpg"

            try {
                FirebaseProvider.storage.reference.child(storagePath).delete().await()
            } catch (_: Exception) {}

            docRef.delete().await()

            try {
                storage.cacheFile(documentId).delete()
            } catch (_: Exception) {}
            true
        }
        return result.getOrDefault(false)
    }

    suspend fun getSummary(): PortfolioSummary {
        val profile = try { getProfile() } catch (_: Exception) { null }
        val records = try { getRecords() } catch (_: Exception) { emptyList() }
        val docs = try { getDocuments() } catch (_: Exception) { emptyList() }

        return PortfolioSummary(
            patientName = profile?.name ?: "",
            age = profile?.age ?: 0,
            gender = profile?.gender ?: "",
            bloodGroup = profile?.bloodGroup ?: "",
            allergies = profile?.allergies ?: emptyList(),
            chronicConditions = profile?.chronicConditions ?: emptyList(),
            currentMedications = profile?.currentMedications ?: emptyList(),
            emergencyContact = if (profile != null && profile.emergencyContactName.isNotBlank()) {
                "${profile.emergencyContactName} (${profile.emergencyContactPhone})"
            } else "",
            recentRecords = records.take(5),
            documentTitles = docs.map { "${it.title} [${it.category.label}]" }
        )
    }

    fun getStorageManager(): FileStorageManager = storage
}

/**
 * Singleton provider ensuring a single instance of PortfolioRepository per application context.
 */
object PortfolioRepositoryProvider {
    @Volatile
    private var instance: PortfolioRepository? = null

    fun get(context: Context): PortfolioRepository {
        return instance ?: synchronized(this) {
            instance ?: PortfolioRepository(context.applicationContext).also { instance = it }
        }
    }
}
