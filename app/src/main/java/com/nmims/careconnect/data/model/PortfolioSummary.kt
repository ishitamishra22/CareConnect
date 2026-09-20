package com.nmims.careconnect.data.model

import com.nmims.careconnect.ui.common.DateUtils

/**
 * Summary view of the patient's portfolio.
 * Designed to be automatically forwarded and attached to bookings,
 * doctor consultations, and hospital admissions in later modules.
 */
data class PortfolioSummary(
    val patientName: String,
    val age: Int,
    val gender: String,
    val bloodGroup: String,
    val allergies: List<String>,
    val chronicConditions: List<String>,
    val currentMedications: List<String>,
    val emergencyContact: String,
    val recentRecords: List<MedicalRecord>,
    val documentTitles: List<String>
) {
    /**
     * Produces a clean, formatted multi-line summary string.
     */
    fun toShareableText(): String {
        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("       CARECONNECT MEDICAL SUMMARY     ")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("Patient: ${patientName.ifBlank { "Not Specified" }}")
        sb.appendLine("Age / Gender: ${if (age > 0) "$age yrs" else "—"}, ${gender.ifBlank { "—" }}")
        sb.appendLine("Blood Group: ${bloodGroup.ifBlank { "—" }}")
        sb.appendLine("Emergency Contact: ${emergencyContact.ifBlank { "—" }}")
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("Allergies: ${if (allergies.isNotEmpty()) allergies.joinToString(", ") else "None Reported"}")
        sb.appendLine("Chronic Conditions: ${if (chronicConditions.isNotEmpty()) chronicConditions.joinToString(", ") else "None Reported"}")
        sb.appendLine("Current Medications: ${if (currentMedications.isNotEmpty()) currentMedications.joinToString(", ") else "None Reported"}")
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("Recent Medical Records (${recentRecords.size}):")
        if (recentRecords.isEmpty()) {
            sb.appendLine("  (No recorded history)")
        } else {
            recentRecords.forEach { record ->
                val dateStr = DateUtils.format(record.dateMillis)
                val docHosp = listOf(record.doctorName, record.hospitalName).filter { it.isNotBlank() }.joinToString(" @ ")
                val detail = if (docHosp.isNotBlank()) " [$docHosp]" else ""
                sb.appendLine("  • [${record.type.label}] ${record.title} ($dateStr)$detail")
            }
        }
        sb.appendLine("───────────────────────────────────────")
        sb.appendLine("Available Documents (${documentTitles.size}):")
        if (documentTitles.isEmpty()) {
            sb.appendLine("  (No documents attached)")
        } else {
            documentTitles.forEach { title ->
                sb.appendLine("  • $title")
            }
        }
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("Generated via CareConnect App")
        return sb.toString()
    }
}
