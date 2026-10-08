package com.nmims.careconnect.data.model

/**
 * Model representing a specialist doctor practicing at a partner hospital.
 */
data class Doctor(
    val id: String,
    val hospitalId: String,
    val name: String,
    val specialty: String,
    val qualifications: String,
    val experienceYears: Int,
    val consultationFee: Int,
    val rating: Double,
    val reviewCount: Int,
    val availableDays: List<String>,
    val availableSlots: List<String>,
    val languages: List<String>,
    val about: String
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "hospitalId" to hospitalId,
            "name" to name,
            "specialty" to specialty,
            "qualifications" to qualifications,
            "experienceYears" to experienceYears,
            "consultationFee" to consultationFee,
            "rating" to rating,
            "reviewCount" to reviewCount,
            "availableDays" to availableDays,
            "availableSlots" to availableSlots,
            "languages" to languages,
            "about" to about
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Doctor {
            val daysList = (map["availableDays"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val slotsList = (map["availableSlots"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val languagesList = (map["languages"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            return Doctor(
                id = map["id"] as? String ?: "",
                hospitalId = map["hospitalId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                specialty = map["specialty"] as? String ?: "",
                qualifications = map["qualifications"] as? String ?: "",
                experienceYears = (map["experienceYears"] as? Number)?.toInt() ?: 0,
                consultationFee = (map["consultationFee"] as? Number)?.toInt() ?: 0,
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                reviewCount = (map["reviewCount"] as? Number)?.toInt() ?: 0,
                availableDays = daysList,
                availableSlots = slotsList,
                languages = languagesList,
                about = map["about"] as? String ?: ""
            )
        }
    }
}
