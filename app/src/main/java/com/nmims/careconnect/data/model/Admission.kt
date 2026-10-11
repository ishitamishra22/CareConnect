package com.nmims.careconnect.data.model

/**
 * Status lifecycle of a hospital admission request.
 */
enum class AdmissionStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    DISCHARGED("Discharged")
}

/**
 * Medical insurance policy details provided by the patient during admission.
 */
data class InsuranceDetails(
    val providerName: String,
    val policyNumber: String,
    val policyholderName: String,
    val validityDateMillis: Long,
    val sumInsured: Int
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "providerName" to providerName,
            "policyNumber" to policyNumber,
            "policyholderName" to policyholderName,
            "validityDateMillis" to validityDateMillis,
            "sumInsured" to sumInsured
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): InsuranceDetails {
            return InsuranceDetails(
                providerName = map["providerName"] as? String ?: "",
                policyNumber = map["policyNumber"] as? String ?: "",
                policyholderName = map["policyholderName"] as? String ?: "",
                validityDateMillis = (map["validityDateMillis"] as? Number)?.toLong() ?: 0L,
                sumInsured = (map["sumInsured"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * Data model representing an inpatient hospital admission request and its lifecycle.
 * Stored in Cloud Firestore under the "admissions" collection.
 */
data class Admission(
    val id: String,
    val patientId: String,
    val hospitalId: String,
    val patientName: String,
    val patientAge: Int,
    val patientGender: String,
    val patientBloodGroup: String,
    val patientPhone: String,
    val patientAddress: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val reason: String,
    val referringDoctor: String? = null,
    val roomType: String,
    val roomPricePerDay: Int,
    val preferredDateMillis: Long,
    val expectedStayDays: Int,
    val estimatedCost: Int,
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList(),
    val sharedPortfolioSummary: String = "",
    val insuranceDetails: InsuranceDetails? = null,
    val status: AdmissionStatus = AdmissionStatus.PENDING,
    val assignedRoom: String? = null,
    val adminRemark: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun referenceCode(): String = id.take(8).uppercase()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "patientId" to patientId,
            "hospitalId" to hospitalId,
            "patientName" to patientName,
            "patientAge" to patientAge,
            "patientGender" to patientGender,
            "patientBloodGroup" to patientBloodGroup,
            "patientPhone" to patientPhone,
            "patientAddress" to patientAddress,
            "emergencyContactName" to emergencyContactName,
            "emergencyContactPhone" to emergencyContactPhone,
            "reason" to reason,
            "referringDoctor" to referringDoctor,
            "roomType" to roomType,
            "roomPricePerDay" to roomPricePerDay,
            "preferredDateMillis" to preferredDateMillis,
            "expectedStayDays" to expectedStayDays,
            "estimatedCost" to estimatedCost,
            "allergies" to allergies,
            "chronicConditions" to chronicConditions,
            "currentMedications" to currentMedications,
            "sharedPortfolioSummary" to sharedPortfolioSummary,
            "insuranceDetails" to insuranceDetails?.toMap(),
            "status" to status.name,
            "assignedRoom" to assignedRoom,
            "adminRemark" to adminRemark,
            "createdAtMillis" to createdAtMillis
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): Admission {
            val allergiesList = (map["allergies"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val conditionsList = (map["chronicConditions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val medicationsList = (map["currentMedications"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val insuranceMap = map["insuranceDetails"] as? Map<String, Any?>
            val insurance = insuranceMap?.let { InsuranceDetails.fromMap(it) }

            val statusEnum = try {
                val statusStr = map["status"] as? String ?: AdmissionStatus.PENDING.name
                AdmissionStatus.valueOf(statusStr)
            } catch (_: Exception) {
                AdmissionStatus.PENDING
            }

            return Admission(
                id = map["id"] as? String ?: "",
                patientId = map["patientId"] as? String ?: "",
                hospitalId = map["hospitalId"] as? String ?: "",
                patientName = map["patientName"] as? String ?: "",
                patientAge = (map["patientAge"] as? Number)?.toInt() ?: 0,
                patientGender = map["patientGender"] as? String ?: "",
                patientBloodGroup = map["patientBloodGroup"] as? String ?: "",
                patientPhone = map["patientPhone"] as? String ?: "",
                patientAddress = map["patientAddress"] as? String ?: "",
                emergencyContactName = map["emergencyContactName"] as? String ?: "",
                emergencyContactPhone = map["emergencyContactPhone"] as? String ?: "",
                reason = map["reason"] as? String ?: "",
                referringDoctor = map["referringDoctor"] as? String,
                roomType = map["roomType"] as? String ?: "",
                roomPricePerDay = (map["roomPricePerDay"] as? Number)?.toInt() ?: 0,
                preferredDateMillis = (map["preferredDateMillis"] as? Number)?.toLong() ?: 0L,
                expectedStayDays = (map["expectedStayDays"] as? Number)?.toInt() ?: 1,
                estimatedCost = (map["estimatedCost"] as? Number)?.toInt() ?: 0,
                allergies = allergiesList,
                chronicConditions = conditionsList,
                currentMedications = medicationsList,
                sharedPortfolioSummary = map["sharedPortfolioSummary"] as? String ?: "",
                insuranceDetails = insurance,
                status = statusEnum,
                assignedRoom = map["assignedRoom"] as? String,
                adminRemark = map["adminRemark"] as? String,
                createdAtMillis = (map["createdAtMillis"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
