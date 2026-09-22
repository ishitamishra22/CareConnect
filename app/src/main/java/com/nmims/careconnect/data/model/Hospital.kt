package com.nmims.careconnect.data.model

/**
 * Model representing a partner hospital and its facilities.
 */
data class Hospital(
    val id: String,
    val name: String,
    val area: String,
    val city: String = "Mumbai",
    val address: String,
    val phone: String,
    val rating: Double,
    val reviewCount: Int,
    val specialties: List<String>,
    val facilities: List<String>,
    val emergency24x7: Boolean,
    val roomTypes: List<RoomType>,
    val aboutHtml: String,
    val accentColorHex: String
)
