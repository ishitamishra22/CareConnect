package com.nmims.careconnect.data.model

/**
 * Model representing an accommodation room category in a hospital.
 */
data class RoomType(
    val name: String,
    val pricePerDay: Int,
    val description: String
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "pricePerDay" to pricePerDay,
            "description" to description
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): RoomType {
            return RoomType(
                name = map["name"] as? String ?: "",
                pricePerDay = (map["pricePerDay"] as? Number)?.toInt() ?: 0,
                description = map["description"] as? String ?: ""
            )
        }
    }
}
