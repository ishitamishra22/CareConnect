package com.nmims.careconnect.data.repository

import android.content.Context
import com.nmims.careconnect.data.model.MedicalDocument
import com.nmims.careconnect.data.model.MedicalRecord
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.model.PortfolioSummary
import com.nmims.careconnect.data.storage.FileStorageManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository orchestrating access to the patient profile, medical records,
 * and uploaded healthcare documents via JSON files in internal storage.
 */
class PortfolioRepository(context: Context) {

    private val storage = FileStorageManager(context)

    companion object {
        private const val FILE_PROFILE = "profile.json"
        private const val FILE_RECORDS = "records.json"
        private const val FILE_DOCUMENTS = "documents.json"
    }

    suspend fun getProfile(): PatientProfile? {
        val jsonString = storage.readText(FILE_PROFILE) ?: return null
        return try {
            val json = JSONObject(jsonString)
            PatientProfile.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveProfile(profile: PatientProfile): Boolean {
        val jsonString = profile.toJson().toString(2)
        return storage.writeText(FILE_PROFILE, jsonString)
    }

    suspend fun getRecords(): List<MedicalRecord> {
        val jsonString = storage.readText(FILE_RECORDS) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MedicalRecord>()
            for (i in 0 until jsonArray.length()) {
                list.add(MedicalRecord.fromJson(jsonArray.getJSONObject(i)))
            }
            list.sortedByDescending { it.dateMillis }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addRecord(record: MedicalRecord): Boolean {
        val currentRecords = getRecords().toMutableList()
        currentRecords.add(0, record)
        return saveRecords(currentRecords)
    }

    suspend fun updateRecord(record: MedicalRecord): Boolean {
        val currentRecords = getRecords().toMutableList()
        val index = currentRecords.indexOfFirst { it.id == record.id }
        if (index != -1) {
            currentRecords[index] = record
            return saveRecords(currentRecords)
        }
        return false
    }

    suspend fun deleteRecord(recordId: String): Boolean {
        val currentRecords = getRecords().toMutableList()
        val removed = currentRecords.removeAll { it.id == recordId }
        return if (removed) saveRecords(currentRecords) else false
    }

    private suspend fun saveRecords(records: List<MedicalRecord>): Boolean {
        val array = JSONArray()
        records.forEach { array.put(it.toJson()) }
        return storage.writeText(FILE_RECORDS, array.toString(2))
    }

    suspend fun getDocuments(): List<MedicalDocument> {
        val jsonString = storage.readText(FILE_DOCUMENTS) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<MedicalDocument>()
            for (i in 0 until jsonArray.length()) {
                list.add(MedicalDocument.fromJson(jsonArray.getJSONObject(i)))
            }
            list.sortedByDescending { it.addedOnMillis }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addDocument(document: MedicalDocument): Boolean {
        val currentDocs = getDocuments().toMutableList()
        currentDocs.add(0, document)
        return saveDocuments(currentDocs)
    }

    suspend fun deleteDocument(documentId: String): Boolean {
        val currentDocs = getDocuments().toMutableList()
        val docToDelete = currentDocs.find { it.id == documentId }
        if (docToDelete != null) {
            // Delete actual image from internal storage
            if (docToDelete.imageFilePath.isNotBlank()) {
                storage.deleteImage(docToDelete.imageFilePath)
            }
            currentDocs.remove(docToDelete)
            return saveDocuments(currentDocs)
        }
        return false
    }

    private suspend fun saveDocuments(docs: List<MedicalDocument>): Boolean {
        val array = JSONArray()
        docs.forEach { array.put(it.toJson()) }
        return storage.writeText(FILE_DOCUMENTS, array.toString(2))
    }

    suspend fun getSummary(): PortfolioSummary {
        val profile = getProfile()
        val records = getRecords()
        val docs = getDocuments()

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
