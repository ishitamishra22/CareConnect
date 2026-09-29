package com.nmims.careconnect.data.model


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
    val storagePath: String,
    val notes: String
) {
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
                storagePath = path,
                notes = map["notes"] as? String ?: ""
            )
        }
    }
}
