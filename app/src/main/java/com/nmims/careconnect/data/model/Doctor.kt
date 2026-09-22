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
)
