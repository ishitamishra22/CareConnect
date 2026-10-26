package com.nmims.careconnect

import com.nmims.careconnect.data.model.DocumentCategory
import com.nmims.careconnect.data.model.MedicalDocument
import com.nmims.careconnect.data.model.MedicalRecord
import com.nmims.careconnect.data.model.PatientProfile
import com.nmims.careconnect.data.model.PortfolioSummary
import com.nmims.careconnect.data.model.RecordType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit tests for data models, JSON serialization, and summary formatting.
 */
class PortfolioModelUnitTest {

    @Test
    fun testPatientProfileProperties() {
        val original = PatientProfile(
            name = "Ishita Mishra",
            age = 21,
            gender = "Female",
            bloodGroup = "B+",
            phone = "9876543210",
            email = "ishita@test.com",
            address = "Mumbai, India",
            emergencyContactName = "Parent",
            emergencyContactPhone = "9123456780",
            allergies = listOf("Penicillin", "Peanuts"),
            chronicConditions = listOf("Asthma"),
            currentMedications = listOf("Inhaler")
        )

        assertEquals("Ishita Mishra", original.name)
        assertEquals(21, original.age)
        assertEquals("B+", original.bloodGroup)
        assertEquals(2, original.allergies.size)
        assertTrue(original.allergies.contains("Penicillin"))
    }

    @Test
    fun testMedicalRecordProperties() {
        val record = MedicalRecord(
            id = "rec-123",
            type = RecordType.DIAGNOSIS,
            title = "Seasonal Allergy",
            dateMillis = 1700000000000L,
            doctorName = "Dr. Mehta",
            hospitalName = "Nanavati Hospital",
            notes = "Mild symptoms"
        )

        assertEquals("rec-123", record.id)
        assertEquals(RecordType.DIAGNOSIS, record.type)
        assertEquals("Seasonal Allergy", record.title)
        assertEquals("Nanavati Hospital", record.hospitalName)
    }

    @Test
    fun testMedicalDocumentProperties() {
        val doc = MedicalDocument(
            id = "doc-999",
            title = "Blood Test CBC",
            category = DocumentCategory.LAB_REPORT,
            addedOnMillis = 1700000000000L,
            storagePath = "/data/user/0/documents/doc-999.jpg",
            notes = "Routine health checkup"
        )

        assertEquals("doc-999", doc.id)
        assertEquals(DocumentCategory.LAB_REPORT, doc.category)
        assertEquals("Blood Test CBC", doc.title)
    }

    @Test
    fun testPortfolioSummaryFormatting() {
        val summary = PortfolioSummary(
            patientName = "Test Patient",
            age = 30,
            gender = "Male",
            bloodGroup = "O+",
            allergies = listOf("Dust"),
            chronicConditions = emptyList(),
            currentMedications = listOf("Vitamin C"),
            emergencyContact = "Contact (9999999999)",
            recentRecords = listOf(
                MedicalRecord(
                    id = "r1",
                    type = RecordType.PRESCRIPTION,
                    title = "Eye Drops",
                    dateMillis = 1700000000000L,
                    doctorName = "Dr. Ray",
                    hospitalName = "Eye Clinic",
                    notes = ""
                )
            ),
            documentTitles = listOf("Eye Scan [Scan / X-Ray]")
        )

        val text = summary.toShareableText()
        assertTrue(text.contains("CARECONNECT MEDICAL SUMMARY"))
        assertTrue(text.contains("Test Patient"))
        assertTrue(text.contains("Eye Drops"))
        assertTrue(text.contains("Eye Scan [Scan / X-Ray]"))
    }
}
