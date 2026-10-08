package com.nmims.careconnect.data.repository

import com.nmims.careconnect.data.firebase.CatalogueSeeder
import com.nmims.careconnect.data.firebase.Collections
import com.nmims.careconnect.data.firebase.FirebaseProvider
import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.Review
import com.nmims.careconnect.data.model.SortOption
import com.nmims.careconnect.data.seed.HospitalSeedData
import kotlinx.coroutines.tasks.await
import kotlin.math.round

/**
 * Repository providing Mumbai hospitals, doctors, room accommodation rates,
 * facilities, and patient reviews.
 * Backed by Cloud Firestore with an in-memory cache and offline fallback.
 */
object HospitalRepository {

    private val cachedHospitals = mutableListOf<Hospital>()
    private val cachedDoctors = mutableListOf<Doctor>()
    private val cachedReviews = mutableListOf<Review>()

    var isLoaded: Boolean = false
        private set

    var loadedFromCloud: Boolean = false
        private set

    suspend fun load(): Boolean {
        val result = FirebaseProvider.safeCall {
            CatalogueSeeder.seedIfEmpty()

            val db = FirebaseProvider.db

            val hospitalDocs = db.collection(Collections.HOSPITALS).get().await()
            val loadedHospitals = hospitalDocs.documents.mapNotNull { doc ->
                doc.data?.let { Hospital.fromMap(it) }
            }

            val doctorDocs = db.collection(Collections.DOCTORS).get().await()
            val loadedDoctors = doctorDocs.documents.mapNotNull { doc ->
                doc.data?.let { Doctor.fromMap(it) }
            }

            val reviewDocs = db.collection(Collections.REVIEWS).get().await()
            val loadedReviews = reviewDocs.documents.mapNotNull { doc ->
                doc.data?.let { Review.fromMap(it) }
            }

            synchronized(this) {
                cachedHospitals.clear()
                cachedHospitals.addAll(loadedHospitals)

                cachedDoctors.clear()
                cachedDoctors.addAll(loadedDoctors)

                cachedReviews.clear()
                cachedReviews.addAll(loadedReviews)
            }
            true
        }

        return if (result.isSuccess && result.getOrDefault(false)) {
            isLoaded = true
            loadedFromCloud = true
            true
        } else {
            if (cachedHospitals.isEmpty()) {
                synchronized(this) {
                    cachedHospitals.clear()
                    cachedHospitals.addAll(HospitalSeedData.hospitals())

                    cachedDoctors.clear()
                    cachedDoctors.addAll(HospitalSeedData.doctors())

                    cachedReviews.clear()
                    cachedReviews.addAll(HospitalSeedData.reviews())
                }
            }
            isLoaded = true
            loadedFromCloud = false
            false
        }
    }

    suspend fun addReview(review: Review): Boolean {
        return FirebaseProvider.safeCall {
            val db = FirebaseProvider.db

            // 1. Check if patient has already reviewed this target in cache or Firestore
            val existingIndex = cachedReviews.indexOfFirst { it.id == review.id }
            val existingReview = if (existingIndex >= 0) cachedReviews[existingIndex] else null

            // 2. Write review to Firestore
            val reviewDocRef = db.collection(Collections.REVIEWS).document(review.id)
            reviewDocRef.set(review.toMap()).await()

            // 3. Calculate new aggregate rating and review count
            val isHospital = cachedHospitals.any { it.id == review.targetId }
            val isDoctor = cachedDoctors.any { it.id == review.targetId }

            if (isHospital) {
                val hospIndex = cachedHospitals.indexOfFirst { it.id == review.targetId }
                if (hospIndex >= 0) {
                    val curr = cachedHospitals[hospIndex]
                    val (newRating, newCount) = calculateAggregate(
                        currentRating = curr.rating,
                        currentCount = curr.reviewCount,
                        existingReviewRating = existingReview?.rating,
                        newRating = review.rating
                    )
                    // Update Firestore document
                    db.collection(Collections.HOSPITALS).document(curr.id)
                        .update(
                            mapOf(
                                "rating" to newRating,
                                "reviewCount" to newCount
                            )
                        ).await()

                    // Update memory cache
                    cachedHospitals[hospIndex] = curr.copy(rating = newRating, reviewCount = newCount)
                }
            } else if (isDoctor) {
                val docIndex = cachedDoctors.indexOfFirst { it.id == review.targetId }
                if (docIndex >= 0) {
                    val curr = cachedDoctors[docIndex]
                    val (newRating, newCount) = calculateAggregate(
                        currentRating = curr.rating,
                        currentCount = curr.reviewCount,
                        existingReviewRating = existingReview?.rating,
                        newRating = review.rating
                    )
                    // Update Firestore document
                    db.collection(Collections.DOCTORS).document(curr.id)
                        .update(
                            mapOf(
                                "rating" to newRating,
                                "reviewCount" to newCount
                            )
                        ).await()

                    // Update memory cache
                    cachedDoctors[docIndex] = curr.copy(rating = newRating, reviewCount = newCount)
                }
            }

            // 4. Update cachedReviews list
            synchronized(this) {
                if (existingIndex >= 0) {
                    cachedReviews[existingIndex] = review
                } else {
                    cachedReviews.add(review)
                }
            }

            true
        }.getOrDefault(false)
    }

    private fun calculateAggregate(
        currentRating: Double,
        currentCount: Int,
        existingReviewRating: Int?,
        newRating: Int
    ): Pair<Double, Int> {
        return if (existingReviewRating == null) {
            val total = (currentRating * currentCount) + newRating
            val newCount = currentCount + 1
            val computed = if (newCount > 0) total / newCount else 0.0
            val rounded = round(computed * 10.0) / 10.0
            Pair(rounded, newCount)
        } else {
            val total = (currentRating * currentCount) - existingReviewRating + newRating
            val newCount = currentCount
            val computed = if (newCount > 0) total / newCount else 0.0
            val rounded = round(computed * 10.0) / 10.0
            Pair(rounded, newCount)
        }
    }

    fun getHospitals(): List<Hospital> = cachedHospitals

    fun getHospital(id: String): Hospital? = cachedHospitals.find { it.id == id }

    fun getDoctors(): List<Doctor> = cachedDoctors

    fun getDoctor(id: String): Doctor? = cachedDoctors.find { it.id == id }

    fun getDoctorsForHospital(hospitalId: String): List<Doctor> =
        cachedDoctors.filter { it.hospitalId == hospitalId }

    fun getReviewsFor(targetId: String): List<Review> =
        cachedReviews.filter { it.targetId == targetId }.sortedByDescending { it.dateMillis }

    fun searchHospitals(
        query: String,
        specialty: String?,
        sort: SortOption
    ): List<Hospital> {
        val trimmedQuery = query.trim().lowercase()
        val filterSpecialty = specialty?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }

        var list = cachedHospitals.filter { hospital ->
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

        var list = cachedDoctors.filter { doctor ->
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
