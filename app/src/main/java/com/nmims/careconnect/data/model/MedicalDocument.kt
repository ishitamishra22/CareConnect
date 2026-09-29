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
    val storagePath: String get() = imageFilePath

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "category" to category.name,
            "addedOnMillis" to addedOnMillis,
            "storagePath" to storagePath,
            "notes" to notes
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category.name)
            put("addedOnMillis", addedOnMillis)
            put("storagePath", storagePath)
            put("imageFilePath", imageFilePath)
            put("notes", notes)
        }
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): MedicalDocument {
            val categoryName = map["category"] as? String ?: DocumentCategory.OTHER.name
            val category = try {
                DocumentCategory.valueOf(categoryName)
            } catch (_: Exception) {
                DocumentCategory.OTHER
            }

            val path = (map["storagePath"] as? String)
                ?: (map["imageFilePath"] as? String)
                ?: ""

            return MedicalDocument(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "",
                category = category,
                addedOnMillis = (map["addedOnMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                imageFilePath = path,
                notes = map["notes"] as? String ?: ""
            )
        }

        fun fromJson(json: JSONObject): MedicalDocument {
            val categoryName = json.optString("category", DocumentCategory.OTHER.name)
            val category = try {
                DocumentCategory.valueOf(categoryName)
            } catch (_: Exception) {
                DocumentCategory.OTHER
            }

            val path = json.optString("storagePath", json.optString("imageFilePath", ""))

            return MedicalDocument(
                id = json.optString("id", ""),
                title = json.optString("title", ""),
                category = category,
                addedOnMillis = json.optLong("addedOnMillis", System.currentTimeMillis()),
                imageFilePath = path,
                notes = json.optString("notes", "")
            )
        }
    }
}
