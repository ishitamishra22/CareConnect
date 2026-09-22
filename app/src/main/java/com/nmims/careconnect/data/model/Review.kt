package com.nmims.careconnect.data.model

/**
 * Model representing a verified patient review for a hospital or doctor.
 */
data class Review(
    val id: String,
    val targetId: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val dateMillis: Long
)
