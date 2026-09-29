package com.nmims.careconnect.data.model

/**
 * Categorization of medical history events.
 */
enum class RecordType(val label: String) {
    DIAGNOSIS("Diagnosis"),
    PRESCRIPTION("Prescription"),
    SURGERY("Surgery"),
    VACCINATION("Vaccination"),
    LAB_RESULT("Lab Result"),
    OTHER("Other")
}

/**
 * Data model for a medical history entry.
 */
data class MedicalRecord(
    val id: String,
    val type: RecordType,
    val title: String,
    val dateMillis: Long,
    val doctorName: String,
    val hospitalName: String,
    val notes: String
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "type" to type.name,
            "title" to title,
            "dateMillis" to dateMillis,
            "doctorName" to doctorName,
            "hospitalName" to hospitalName,
            "notes" to notes
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): MedicalRecord {
            val typeName = map["type"] as? String ?: RecordType.OTHER.name
            val recordType = try {
                RecordType.valueOf(typeName)
            } catch (_: Exception) {
                RecordType.OTHER
            }

            return MedicalRecord(
                id = map["id"] as? String ?: "",
                type = recordType,
                title = map["title"] as? String ?: "",
                dateMillis = (map["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                doctorName = map["doctorName"] as? String ?: "",
                hospitalName = map["hospitalName"] as? String ?: "",
                notes = map["notes"] as? String ?: ""
            )
        }
    }
}
