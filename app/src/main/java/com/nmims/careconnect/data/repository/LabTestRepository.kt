package com.nmims.careconnect.data.repository

import com.nmims.careconnect.data.model.LabTest

/**
 * Static catalogue repository containing 15 diagnostic laboratory tests and scans.
 * In-memory catalogue with zero networking or Firebase dependencies.
 */
object LabTestRepository {

    private val tests = listOf(
        LabTest(
            id = "test_cbc",
            name = "CBC",
            category = "Blood",
            price = 450,
            requiresFasting = false,
            reportTimeHours = 12,
            description = "Complete Blood Count measuring red blood cells, white blood cells, hemoglobin, and platelets to evaluate overall health."
        ),
        LabTest(
            id = "test_lipid",
            name = "Lipid Profile",
            category = "Blood",
            price = 850,
            requiresFasting = true,
            reportTimeHours = 24,
            description = "Measures total cholesterol, HDL, LDL, VLDL, and triglycerides to assess cardiovascular risk profile."
        ),
        LabTest(
            id = "test_hba1c",
            name = "HbA1c",
            category = "Blood",
            price = 650,
            requiresFasting = false,
            reportTimeHours = 12,
            description = "Glycated hemoglobin test reflecting average blood glucose levels over the past 2 to 3 months for diabetes monitoring."
        ),
        LabTest(
            id = "test_thyroid",
            name = "Thyroid Profile",
            category = "Blood",
            price = 750,
            requiresFasting = true,
            reportTimeHours = 24,
            description = "Measures thyroid stimulating hormone (TSH), total T3, and total T4 to evaluate thyroid gland performance."
        ),
        LabTest(
            id = "test_vit_d",
            name = "Vitamin D",
            category = "Blood",
            price = 1200,
            requiresFasting = false,
            reportTimeHours = 24,
            description = "Evaluates 25-hydroxy vitamin D levels crucial for bone density, calcium metabolism, and immune resilience."
        ),
        LabTest(
            id = "test_vit_b12",
            name = "Vitamin B12",
            category = "Blood",
            price = 950,
            requiresFasting = true,
            reportTimeHours = 24,
            description = "Measures cobalamin concentration in blood to diagnose pernicious anemia and neuropathy symptoms."
        ),
        LabTest(
            id = "test_xray_chest",
            name = "Chest X-Ray",
            category = "Imaging",
            price = 600,
            requiresFasting = false,
            reportTimeHours = 6,
            description = "Digital PA view radiographic scan of the chest cavity assessing lungs, cardiac silhouette, and rib framework."
        ),
        LabTest(
            id = "test_mri_brain",
            name = "MRI Brain",
            category = "Imaging",
            price = 6500,
            requiresFasting = false,
            reportTimeHours = 24,
            description = "High-resolution 3 Tesla magnetic resonance imaging of brain parenchyma and vascular structures."
        ),
        LabTest(
            id = "test_ct_abdomen",
            name = "CT Scan Abdomen",
            category = "Imaging",
            price = 4800,
            requiresFasting = true,
            reportTimeHours = 24,
            description = "Computed tomography cross-sectional scan examining abdominal visceral organs with oral/IV contrast protocols."
        ),
        LabTest(
            id = "test_usg_abdomen",
            name = "Ultrasound Abdomen",
            category = "Imaging",
            price = 1500,
            requiresFasting = true,
            reportTimeHours = 6,
            description = "High-frequency sonic imaging evaluating hepatobiliary system, kidneys, spleen, pancreas, and pelvis."
        ),
        LabTest(
            id = "test_ecg",
            name = "ECG",
            category = "Cardiac",
            price = 350,
            requiresFasting = false,
            reportTimeHours = 2,
            description = "Standard 12-lead resting electrocardiogram recording electrical conductivity and rhythm of the heart."
        ),
        LabTest(
            id = "test_2d_echo",
            name = "2D Echo",
            category = "Cardiac",
            price = 2200,
            requiresFasting = false,
            reportTimeHours = 6,
            description = "2-Dimensional transthoracic echocardiogram with Color Doppler inspecting heart valves and ejection fraction."
        ),
        LabTest(
            id = "test_urine_routine",
            name = "Urine Routine",
            category = "Pathology",
            price = 250,
            requiresFasting = false,
            reportTimeHours = 6,
            description = "Macroscopic, chemical, and microscopic examination of urine sample for infection markers, protein, and crystals."
        ),
        LabTest(
            id = "test_lft",
            name = "Liver Function Test",
            category = "Pathology",
            price = 800,
            requiresFasting = true,
            reportTimeHours = 12,
            description = "Measures SGOT, SGPT, ALP, bilirubin, and albumin to evaluate hepatic metabolism and liver integrity."
        ),
        LabTest(
            id = "test_kft",
            name = "Kidney Function Test",
            category = "Pathology",
            price = 750,
            requiresFasting = true,
            reportTimeHours = 12,
            description = "Measures blood urea nitrogen, serum creatinine, uric acid, and electrolytes for renal clearance assessment."
        )
    )

    fun getAll(): List<LabTest> = tests

    fun getTest(id: String): LabTest? = tests.find { it.id == id }

    fun getByCategory(category: String): List<LabTest> =
        tests.filter { it.category.equals(category, ignoreCase = true) }
}
