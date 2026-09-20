package com.nmims.careconnect.data.model

import org.json.JSONObject

/**
 * Categories for uploaded healthcare documents.
 */
enum class DocumentCategory(val label: String) {
    LAB_REPORT("Lab Report"),
    PRESCRIPTION("Prescription"),
    SCAN_XRAY("Scan / X-Ray"),
    DISCHARGE_SUMMARY("Discharge Summary"),
    INSURANCE("Insurance"),
    ID_PROOF("ID Proof"),
    OTHER("Other")
}

/**
 * Data model for a stored medical document.
 */
data class MedicalDocument(
    val id: String,
    val title: String,
    val category: DocumentCategory,
    val addedOnMillis: Long,
    val imageFilePath: String,
    val notes: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category.name)
            put("addedOnMillis", addedOnMillis)
            put("imageFilePath", imageFilePath)
            put("notes", notes)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): MedicalDocument {
            val categoryName = json.optString("category", DocumentCategory.OTHER.name)
            val category = try {
                DocumentCategory.valueOf(categoryName)
            } catch (_: Exception) {
                DocumentCategory.OTHER
            }

            return MedicalDocument(
                id = json.optString("id", ""),
                title = json.optString("title", ""),
                category = category,
                addedOnMillis = json.optLong("addedOnMillis", System.currentTimeMillis()),
                imageFilePath = json.optString("imageFilePath", ""),
                notes = json.optString("notes", "")
            )
        }
    }
}
