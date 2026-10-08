package com.nmims.careconnect.data.model

/**
 * Model representing a diagnostic lab test or scan.
 */
data class LabTest(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val requiresFasting: Boolean,
    val reportTimeHours: Int,
    val description: String
)
