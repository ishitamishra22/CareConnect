package com.nmims.careconnect.data.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model for patient profile.
 * Serializes to and deserializes from JSONObject.
 */
data class PatientProfile(
    val name: String,
    val age: Int,
    val gender: String,
    val bloodGroup: String,
    val phone: String,
    val email: String,
    val address: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "age" to age,
            "gender" to gender,
            "bloodGroup" to bloodGroup,
            "phone" to phone,
            "email" to email,
            "address" to address,
            "emergencyContactName" to emergencyContactName,
            "emergencyContactPhone" to emergencyContactPhone,
            "allergies" to allergies,
            "chronicConditions" to chronicConditions,
            "currentMedications" to currentMedications
        )
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("age", age)
            put("gender", gender)
            put("bloodGroup", bloodGroup)
            put("phone", phone)
            put("email", email)
            put("address", address)
            put("emergencyContactName", emergencyContactName)
            put("emergencyContactPhone", emergencyContactPhone)
            put("allergies", JSONArray(allergies))
            put("chronicConditions", JSONArray(chronicConditions))
            put("currentMedications", JSONArray(currentMedications))
        }
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): PatientProfile {
            val allergiesList = (map["allergies"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val conditionsList = (map["chronicConditions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val medicationsList = (map["currentMedications"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            return PatientProfile(
                name = map["name"] as? String ?: "",
                age = (map["age"] as? Number)?.toInt() ?: 0,
                gender = map["gender"] as? String ?: "",
                bloodGroup = map["bloodGroup"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                email = map["email"] as? String ?: "",
                address = map["address"] as? String ?: "",
                emergencyContactName = map["emergencyContactName"] as? String ?: "",
                emergencyContactPhone = map["emergencyContactPhone"] as? String ?: "",
                allergies = allergiesList,
                chronicConditions = conditionsList,
                currentMedications = medicationsList
            )
        }

        fun fromJson(json: JSONObject): PatientProfile {
            val map = mutableMapOf<String, Any?>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val v = json.opt(key)
                if (v is JSONArray) {
                    val list = mutableListOf<String>()
                    for (i in 0 until v.length()) list.add(v.getString(i))
                    map[key] = list
                } else {
                    map[key] = v
                }
            }
            return fromMap(map)
        }
    }
}
