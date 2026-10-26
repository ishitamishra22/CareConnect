package com.nmims.careconnect.data.model

import com.google.firebase.Timestamp

/**
 * Audit log entry recorded whenever a sensitive administrative action is performed.
 * Stored in `adminAuditLogs/{logId}`. Immutable (create-only for admins).
 */
data class AdminAuditLog(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "", // e.g. "USER_DEACTIVATED", "USER_ACTIVATED", "USER_EDITED", "DOCTOR_PROVISIONED"
    val targetUid: String = "",
    val details: Map<String, Any?> = emptyMap(),
    val timestamp: Timestamp = Timestamp.now()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "adminUid" to adminUid,
            "adminEmail" to adminEmail,
            "action" to action,
            "targetUid" to targetUid,
            "details" to details,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): AdminAuditLog {
            @Suppress("UNCHECKED_CAST")
            return AdminAuditLog(
                id = id,
                adminUid = (map["adminUid"] as? String).orEmpty(),
                adminEmail = (map["adminEmail"] as? String).orEmpty(),
                action = (map["action"] as? String).orEmpty(),
                targetUid = (map["targetUid"] as? String).orEmpty(),
                details = (map["details"] as? Map<String, Any?>).orEmpty(),
                timestamp = (map["timestamp"] as? Timestamp) ?: Timestamp.now()
            )
        }
    }
}
