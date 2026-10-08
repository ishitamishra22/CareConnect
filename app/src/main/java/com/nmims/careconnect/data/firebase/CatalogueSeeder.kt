package com.nmims.careconnect.data.firebase

import com.nmims.careconnect.data.seed.HospitalSeedData
import kotlinx.coroutines.tasks.await

/**
 * Utility to seed initial hospital, doctor, and review catalogue data to Cloud Firestore.
 * Checks if the "hospitals" collection is empty, and if so, performs a batch upload.
 */
object CatalogueSeeder {

    suspend fun seedIfEmpty(): Boolean {
        return FirebaseProvider.safeCall {
            val db = FirebaseProvider.db
            val existing = db.collection(Collections.HOSPITALS)
                .limit(1)
                .get()
                .await()

            if (existing.isEmpty) {
                val batch = db.batch()

                // Upload 8 hospitals
                for (hospital in HospitalSeedData.hospitals()) {
                    val docRef = db.collection(Collections.HOSPITALS).document(hospital.id)
                    batch.set(docRef, hospital.toMap())
                }

                // Upload 24 doctors
                for (doctor in HospitalSeedData.doctors()) {
                    val docRef = db.collection(Collections.DOCTORS).document(doctor.id)
                    batch.set(docRef, doctor.toMap())
                }

                // Upload seed reviews
                for (review in HospitalSeedData.reviews()) {
                    val docRef = db.collection(Collections.REVIEWS).document(review.id)
                    batch.set(docRef, review.toMap())
                }

                batch.commit().await()
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }
}
