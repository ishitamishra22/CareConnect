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
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "area" to area,
            "city" to city,
            "address" to address,
            "phone" to phone,
            "rating" to rating,
            "reviewCount" to reviewCount,
            "specialties" to specialties,
            "facilities" to facilities,
            "emergency24x7" to emergency24x7,
            "roomTypes" to roomTypes.map { it.toMap() },
            "aboutHtml" to aboutHtml,
            "accentColorHex" to accentColorHex
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Hospital {
            val specialtiesList = (map["specialties"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val facilitiesList = (map["facilities"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val roomTypesList = (map["roomTypes"] as? List<*>)?.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                (item as? Map<String, Any?>)?.let { RoomType.fromMap(it) }
            } ?: emptyList()

            return Hospital(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                area = map["area"] as? String ?: "",
                city = map["city"] as? String ?: "Mumbai",
                address = map["address"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                rating = (map["rating"] as? Number)?.toDouble() ?: 0.0,
                reviewCount = (map["reviewCount"] as? Number)?.toInt() ?: 0,
                specialties = specialtiesList,
                facilities = facilitiesList,
                emergency24x7 = map["emergency24x7"] as? Boolean ?: false,
                roomTypes = roomTypesList,
                aboutHtml = map["aboutHtml"] as? String ?: "",
                accentColorHex = map["accentColorHex"] as? String ?: ""
            )
        }
    }
}
