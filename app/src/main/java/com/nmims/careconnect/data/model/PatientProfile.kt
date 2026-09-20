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
        fun fromJson(json: JSONObject): PatientProfile {
            val allergiesList = mutableListOf<String>()
            val allergiesArray = json.optJSONArray("allergies")
            if (allergiesArray != null) {
                for (i in 0 until allergiesArray.length()) {
                    allergiesList.add(allergiesArray.getString(i))
                }
            }

            val conditionsList = mutableListOf<String>()
            val conditionsArray = json.optJSONArray("chronicConditions")
            if (conditionsArray != null) {
                for (i in 0 until conditionsArray.length()) {
                    conditionsList.add(conditionsArray.getString(i))
                }
            }

            val medicationsList = mutableListOf<String>()
            val medicationsArray = json.optJSONArray("currentMedications")
            if (medicationsArray != null) {
                for (i in 0 until medicationsArray.length()) {
                    medicationsList.add(medicationsArray.getString(i))
                }
            }

            return PatientProfile(
                name = json.optString("name", ""),
                age = json.optInt("age", 0),
                gender = json.optString("gender", ""),
                bloodGroup = json.optString("bloodGroup", ""),
                phone = json.optString("phone", ""),
                email = json.optString("email", ""),
                address = json.optString("address", ""),
                emergencyContactName = json.optString("emergencyContactName", ""),
                emergencyContactPhone = json.optString("emergencyContactPhone", ""),
                allergies = allergiesList,
                chronicConditions = conditionsList,
                currentMedications = medicationsList
            )
        }
    }
}
