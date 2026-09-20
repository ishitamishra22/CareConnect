package com.nmims.careconnect.data.model

import org.json.JSONObject

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
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("type", type.name)
            put("title", title)
            put("dateMillis", dateMillis)
            put("doctorName", doctorName)
            put("hospitalName", hospitalName)
            put("notes", notes)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): MedicalRecord {
            val typeName = json.optString("type", RecordType.OTHER.name)
            val recordType = try {
                RecordType.valueOf(typeName)
            } catch (_: Exception) {
                RecordType.OTHER
            }

            return MedicalRecord(
                id = json.optString("id", ""),
                type = recordType,
                title = json.optString("title", ""),
                dateMillis = json.optLong("dateMillis", System.currentTimeMillis()),
                doctorName = json.optString("doctorName", ""),
                hospitalName = json.optString("hospitalName", ""),
                notes = json.optString("notes", "")
            )
        }
    }
}
