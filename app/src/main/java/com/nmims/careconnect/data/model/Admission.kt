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
    val policyHolderName: String,
    val validTillMillis: Long,
    val sumInsured: Int
) {
    // Backwards-compatibility aliases
    val policyholderName: String get() = policyHolderName
    val validityDateMillis: Long get() = validTillMillis

    constructor(
        providerName: String,
        policyNumber: String,
        policyholderName: String,
        validityDateMillis: Long,
        sumInsured: Int,
        @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit
    ) : this(
        providerName = providerName,
        policyNumber = policyNumber,
        policyHolderName = policyholderName,
        validTillMillis = validityDateMillis,
        sumInsured = sumInsured
    )

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "providerName" to providerName,
            "policyNumber" to policyNumber,
            "policyHolderName" to policyHolderName,
            "validTillMillis" to validTillMillis,
            "sumInsured" to sumInsured
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): InsuranceDetails {
            return InsuranceDetails(
                providerName = map["providerName"] as? String ?: "",
                policyNumber = map["policyNumber"] as? String ?: "",
                policyHolderName = map["policyHolderName"] as? String
                    ?: map["policyholderName"] as? String
                    ?: "",
                validTillMillis = (map["validTillMillis"] as? Number)?.toLong()
                    ?: (map["validityDateMillis"] as? Number)?.toLong()
                    ?: 0L,
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
    val patientName: String,
    val hospitalId: String,
    val reasonForAdmission: String,
    val referringDoctor: String? = null,
    val preferredDateMillis: Long,
    val expectedStayDays: Int,
    val roomTypeName: String,
    val pricePerDay: Int,
    val estimatedCost: Int,
    val patientAge: Int,
    val gender: String,
    val bloodGroup: String,
    val phone: String,
    val address: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList(),
    val sharedPortfolioText: String = "",
    val insurance: InsuranceDetails? = null,
    val status: AdmissionStatus = AdmissionStatus.PENDING,
    val assignedRoomNumber: String? = null,
    val adminRemark: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    // Backwards-compatibility aliases for existing callers
    val reason: String get() = reasonForAdmission
    val roomType: String get() = roomTypeName
    val roomPricePerDay: Int get() = pricePerDay
    val patientGender: String get() = gender
    val patientBloodGroup: String get() = bloodGroup
    val patientPhone: String get() = phone
    val patientAddress: String get() = address
    val sharedPortfolioSummary: String get() = sharedPortfolioText
    val insuranceDetails: InsuranceDetails? get() = insurance
    val assignedRoom: String? get() = assignedRoomNumber

    constructor(
        id: String,
        patientId: String,
        hospitalId: String,
        patientName: String,
        patientAge: Int,
        patientGender: String,
        patientBloodGroup: String,
        patientPhone: String,
        patientAddress: String,
        emergencyContactName: String,
        emergencyContactPhone: String,
        reason: String,
        referringDoctor: String? = null,
        roomType: String,
        roomPricePerDay: Int,
        preferredDateMillis: Long,
        expectedStayDays: Int,
        estimatedCost: Int,
        allergies: List<String> = emptyList(),
        chronicConditions: List<String> = emptyList(),
        currentMedications: List<String> = emptyList(),
        sharedPortfolioSummary: String = "",
        insuranceDetails: InsuranceDetails? = null,
        status: AdmissionStatus = AdmissionStatus.PENDING,
        assignedRoom: String? = null,
        adminRemark: String? = null,
        createdAtMillis: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        patientId = patientId,
        patientName = patientName,
        hospitalId = hospitalId,
        reasonForAdmission = reason,
        referringDoctor = referringDoctor,
        preferredDateMillis = preferredDateMillis,
        expectedStayDays = expectedStayDays,
        roomTypeName = roomType,
        pricePerDay = roomPricePerDay,
        estimatedCost = estimatedCost,
        patientAge = patientAge,
        gender = patientGender,
        bloodGroup = patientBloodGroup,
        phone = patientPhone,
        address = patientAddress,
        emergencyContactName = emergencyContactName,
        emergencyContactPhone = emergencyContactPhone,
        allergies = allergies,
        chronicConditions = chronicConditions,
        currentMedications = currentMedications,
        sharedPortfolioText = sharedPortfolioSummary,
        insurance = insuranceDetails,
        status = status,
        assignedRoomNumber = assignedRoom,
        adminRemark = adminRemark,
        createdAtMillis = createdAtMillis
    )

    fun referenceCode(): String = id.take(8).uppercase()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "patientId" to patientId,
            "patientName" to patientName,
            "hospitalId" to hospitalId,
            "reasonForAdmission" to reasonForAdmission,
            "referringDoctor" to referringDoctor,
            "preferredDateMillis" to preferredDateMillis,
            "expectedStayDays" to expectedStayDays,
            "roomTypeName" to roomTypeName,
            "pricePerDay" to pricePerDay,
            "estimatedCost" to estimatedCost,
            "patientAge" to patientAge,
            "gender" to gender,
            "bloodGroup" to bloodGroup,
            "phone" to phone,
            "address" to address,
            "emergencyContactName" to emergencyContactName,
            "emergencyContactPhone" to emergencyContactPhone,
            "allergies" to allergies,
            "chronicConditions" to chronicConditions,
            "currentMedications" to currentMedications,
            "sharedPortfolioText" to sharedPortfolioText,
            "insurance" to insurance?.toMap(),
            "status" to status.name,
            "assignedRoomNumber" to assignedRoomNumber,
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
            val insuranceMap = (map["insurance"] as? Map<String, Any?>)
                ?: (map["insuranceDetails"] as? Map<String, Any?>)
            val insuranceObj = insuranceMap?.let { InsuranceDetails.fromMap(it) }

            val statusEnum = try {
                val statusStr = map["status"] as? String ?: AdmissionStatus.PENDING.name
                AdmissionStatus.valueOf(statusStr)
            } catch (_: Exception) {
                AdmissionStatus.PENDING
            }

            return Admission(
                id = map["id"] as? String ?: "",
                patientId = map["patientId"] as? String ?: "",
                patientName = map["patientName"] as? String ?: "",
                hospitalId = map["hospitalId"] as? String ?: "",
                reasonForAdmission = map["reasonForAdmission"] as? String
                    ?: map["reason"] as? String
                    ?: "",
                referringDoctor = map["referringDoctor"] as? String,
                preferredDateMillis = (map["preferredDateMillis"] as? Number)?.toLong() ?: 0L,
                expectedStayDays = (map["expectedStayDays"] as? Number)?.toInt() ?: 1,
                roomTypeName = map["roomTypeName"] as? String
                    ?: map["roomType"] as? String
                    ?: "",
                pricePerDay = (map["pricePerDay"] as? Number)?.toInt()
                    ?: (map["roomPricePerDay"] as? Number)?.toInt()
                    ?: 0,
                estimatedCost = (map["estimatedCost"] as? Number)?.toInt() ?: 0,
                patientAge = (map["patientAge"] as? Number)?.toInt() ?: 0,
                gender = map["gender"] as? String
                    ?: map["patientGender"] as? String
                    ?: "",
                bloodGroup = map["bloodGroup"] as? String
                    ?: map["patientBloodGroup"] as? String
                    ?: "",
                phone = map["phone"] as? String
                    ?: map["patientPhone"] as? String
                    ?: "",
                address = map["address"] as? String
                    ?: map["patientAddress"] as? String
                    ?: "",
                emergencyContactName = map["emergencyContactName"] as? String ?: "",
                emergencyContactPhone = map["emergencyContactPhone"] as? String ?: "",
                allergies = allergiesList,
                chronicConditions = conditionsList,
                currentMedications = medicationsList,
                sharedPortfolioText = map["sharedPortfolioText"] as? String
                    ?: map["sharedPortfolioSummary"] as? String
                    ?: "",
                insurance = insuranceObj,
                status = statusEnum,
                assignedRoomNumber = map["assignedRoomNumber"] as? String
                    ?: map["assignedRoom"] as? String,
                adminRemark = map["adminRemark"] as? String,
                createdAtMillis = (map["createdAtMillis"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}
