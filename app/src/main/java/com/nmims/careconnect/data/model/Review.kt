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
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "targetId" to targetId,
            "reviewerName" to reviewerName,
            "rating" to rating,
            "comment" to comment,
            "dateMillis" to dateMillis
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Review {
            return Review(
                id = map["id"] as? String ?: "",
                targetId = map["targetId"] as? String ?: "",
                reviewerName = map["reviewerName"] as? String ?: "",
                rating = (map["rating"] as? Number)?.toInt() ?: 0,
                comment = map["comment"] as? String ?: "",
                dateMillis = (map["dateMillis"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
