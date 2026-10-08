package com.nmims.careconnect.data.repository

import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.Review
import com.nmims.careconnect.data.model.SortOption
import com.nmims.careconnect.data.seed.HospitalSeedData

/**
 * In-memory repository providing static seed catalogue for Mumbai hospitals,
 * doctors, room accommodation rates, facilities, and patient reviews.
 */
object HospitalRepository {

    private val hospitals: List<Hospital> = HospitalSeedData.hospitals()
    private val doctors: List<Doctor> = HospitalSeedData.doctors()
    private val reviews: List<Review> = HospitalSeedData.reviews()

    fun getHospitals(): List<Hospital> = hospitals

    fun getHospital(id: String): Hospital? = hospitals.find { it.id == id }

    fun getDoctors(): List<Doctor> = doctors

    fun getDoctor(id: String): Doctor? = doctors.find { it.id == id }

    fun getDoctorsForHospital(hospitalId: String): List<Doctor> =
        doctors.filter { it.hospitalId == hospitalId }

    fun getReviewsFor(targetId: String): List<Review> =
        reviews.filter { it.targetId == targetId }.sortedByDescending { it.dateMillis }

    fun searchHospitals(
        query: String,
        specialty: String?,
        sort: SortOption
    ): List<Hospital> {
        val trimmedQuery = query.trim().lowercase()
        val filterSpecialty = specialty?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }

        var list = hospitals.filter { hospital ->
            val matchesSpecialty = if (filterSpecialty != null) {
                hospital.specialties.any { it.equals(filterSpecialty, ignoreCase = true) }
            } else true

            val matchesQuery = if (trimmedQuery.isNotBlank()) {
                hospital.name.lowercase().contains(trimmedQuery) ||
                hospital.area.lowercase().contains(trimmedQuery) ||
                hospital.specialties.any { it.lowercase().contains(trimmedQuery) }
            } else true

            matchesSpecialty && matchesQuery
        }

        list = when (sort) {
            SortOption.RATING_DESC -> list.sortedByDescending { it.rating }
            SortOption.FEE_ASC -> list.sortedBy { hosp ->
                hosp.roomTypes.minOfOrNull { it.pricePerDay } ?: 0
            }
            SortOption.FEE_DESC -> list.sortedByDescending { hosp ->
                hosp.roomTypes.minOfOrNull { it.pricePerDay } ?: 0
            }
            SortOption.EXPERIENCE_DESC -> list.sortedByDescending { it.reviewCount }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
        }

        return list
    }

    fun searchDoctors(
        query: String,
        specialty: String?,
        sort: SortOption
    ): List<Doctor> {
        val trimmedQuery = query.trim().lowercase()
        val filterSpecialty = specialty?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }

        var list = doctors.filter { doctor ->
            val hospital = getHospital(doctor.hospitalId)
            val hospitalName = hospital?.name?.lowercase().orEmpty()
            val hospitalArea = hospital?.area?.lowercase().orEmpty()

            val matchesSpecialty = if (filterSpecialty != null) {
                doctor.specialty.equals(filterSpecialty, ignoreCase = true)
            } else true

            val matchesQuery = if (trimmedQuery.isNotBlank()) {
                doctor.name.lowercase().contains(trimmedQuery) ||
                doctor.specialty.lowercase().contains(trimmedQuery) ||
                hospitalName.contains(trimmedQuery) ||
                hospitalArea.contains(trimmedQuery)
            } else true

            matchesSpecialty && matchesQuery
        }

        list = when (sort) {
            SortOption.RATING_DESC -> list.sortedByDescending { it.rating }
            SortOption.FEE_ASC -> list.sortedBy { it.consultationFee }
            SortOption.FEE_DESC -> list.sortedByDescending { it.consultationFee }
            SortOption.EXPERIENCE_DESC -> list.sortedByDescending { it.experienceYears }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
        }

        return list
    }
}

