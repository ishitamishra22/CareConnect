package com.nmims.careconnect.data.model

import org.json.JSONObject

/**
 * Type of healthcare booking.
 */
enum class BookingType(val label: String) {
    OPD_APPOINTMENT("Doctor Appointment"),
    LAB_TEST("Lab Test / Scan")
}

/**
 * Lifecycle status of a booking.
 */
enum class BookingStatus(val label: String) {
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

/**
 * Data model representing an appointment or laboratory test booking.
 */
data class Booking(
    val id: String,
    val type: BookingType,
    val patientId: String,
    val patientName: String,
    val hospitalId: String,
    val doctorId: String?,
    val labTestId: String?,
    val dateMillis: Long,
    val timeSlot: String,
    val reason: String,
    val sharedPortfolioText: String,
    val status: BookingStatus,
    val createdAtMillis: Long,
    val doctorNote: String = ""
) {
    fun referenceCode(): String = id.take(8).uppercase()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "type" to type.name,
            "patientId" to patientId,
            "patientName" to patientName,
            "hospitalId" to hospitalId,
            "doctorId" to doctorId,
            "labTestId" to labTestId,
            "dateMillis" to dateMillis,
            "timeSlot" to timeSlot,
            "reason" to reason,
            "sharedPortfolioText" to sharedPortfolioText,
            "status" to status.name,
            "createdAtMillis" to createdAtMillis,
            "doctorNote" to doctorNote
        )
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("type", type.name)
        json.put("patientId", patientId)
        json.put("patientName", patientName)
        json.put("hospitalId", hospitalId)
        json.put("doctorId", doctorId ?: JSONObject.NULL)
        json.put("labTestId", labTestId ?: JSONObject.NULL)
        json.put("dateMillis", dateMillis)
        json.put("timeSlot", timeSlot)
        json.put("reason", reason)
        json.put("sharedPortfolioText", sharedPortfolioText)
        json.put("status", status.name)
        json.put("createdAtMillis", createdAtMillis)
        json.put("doctorNote", doctorNote)
        return json
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Booking {
            val typeEnum = try {
                val str = map["type"] as? String ?: BookingType.OPD_APPOINTMENT.name
                BookingType.valueOf(str)
            } catch (_: Exception) {
                BookingType.OPD_APPOINTMENT
            }

            val statusEnum = try {
                val str = map["status"] as? String ?: BookingStatus.CONFIRMED.name
                BookingStatus.valueOf(str)
            } catch (_: Exception) {
                BookingStatus.CONFIRMED
            }

            return Booking(
                id = map["id"] as? String ?: "",
                type = typeEnum,
                patientId = map["patientId"] as? String ?: "",
                patientName = map["patientName"] as? String ?: "",
                hospitalId = map["hospitalId"] as? String ?: "",
                doctorId = map["doctorId"] as? String,
                labTestId = map["labTestId"] as? String,
                dateMillis = (map["dateMillis"] as? Number)?.toLong() ?: 0L,
                timeSlot = map["timeSlot"] as? String ?: "",
                reason = map["reason"] as? String ?: "",
                sharedPortfolioText = map["sharedPortfolioText"] as? String ?: "",
                status = statusEnum,
                createdAtMillis = (map["createdAtMillis"] as? Number)?.toLong() ?: 0L,
                doctorNote = map["doctorNote"] as? String ?: ""
            )
        }

        fun fromJson(json: JSONObject): Booking {
            val doctorIdStr = if (json.isNull("doctorId")) null else json.optString("doctorId").takeIf { it.isNotBlank() }
            val labTestIdStr = if (json.isNull("labTestId")) null else json.optString("labTestId").takeIf { it.isNotBlank() }

            val typeEnum = try {
                BookingType.valueOf(json.optString("type", BookingType.OPD_APPOINTMENT.name))
            } catch (_: Exception) {
                BookingType.OPD_APPOINTMENT
            }

            val statusEnum = try {
                BookingStatus.valueOf(json.optString("status", BookingStatus.CONFIRMED.name))
            } catch (_: Exception) {
                BookingStatus.CONFIRMED
            }

            return Booking(
                id = json.optString("id", ""),
                type = typeEnum,
                patientId = json.optString("patientId", ""),
                patientName = json.optString("patientName", ""),
                hospitalId = json.optString("hospitalId", ""),
                doctorId = doctorIdStr,
                labTestId = labTestIdStr,
                dateMillis = json.optLong("dateMillis", 0L),
                timeSlot = json.optString("timeSlot", ""),
                reason = json.optString("reason", ""),
                sharedPortfolioText = json.optString("sharedPortfolioText", ""),
                status = statusEnum,
                createdAtMillis = json.optLong("createdAtMillis", 0L),
                doctorNote = json.optString("doctorNote", "")
            )
        }
    }
}
