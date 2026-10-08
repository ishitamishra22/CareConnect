package com.nmims.careconnect.data.seed

import com.nmims.careconnect.data.model.Doctor
import com.nmims.careconnect.data.model.Hospital
import com.nmims.careconnect.data.model.Review
import com.nmims.careconnect.data.model.RoomType

/**
 * Built-in static seed catalogue for Mumbai hospitals, doctors, and reviews.
 * Used for initial Cloud Firestore seeding and offline fallback.
 */
object HospitalSeedData {

    private val commonSlots = listOf(
        "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM",
        "11:00 AM", "11:30 AM", "04:00 PM", "04:30 PM"
    )

    private val afternoonSlots = listOf(
        "11:00 AM", "11:30 AM", "02:00 PM", "02:30 PM",
        "03:00 PM", "03:30 PM", "05:00 PM", "05:30 PM"
    )

    private val eveningSlots = listOf(
        "10:00 AM", "10:30 AM", "01:00 PM", "01:30 PM",
        "04:00 PM", "04:30 PM", "06:00 PM", "06:30 PM"
    )

    private val hospitalsList: List<Hospital> = listOf(
        Hospital(
            id = "hosp_andheri",
            name = "MetroLife Super Speciality Hospital",
            area = "Andheri West",
            city = "Mumbai",
            address = "Plot 42, Link Road, Near Infinity Mall, Andheri West, Mumbai - 400053",
            phone = "+912226731100",
            rating = 4.6,
            reviewCount = 142,
            specialties = listOf("Cardiology", "Orthopaedics", "General Physician", "Neurology", "ENT"),
            facilities = listOf(
                "24x7 Emergency & Trauma",
                "Advanced ICU & Cardiac Care",
                "NABH Accredited Diagnostic Lab",
                "128-Slice CT & 3T MRI",
                "24x7 In-house Pharmacy",
                "Modular Operation Theatres",
                "Comprehensive Blood Bank",
                "Ambulance Fleet"
            ),
            emergency24x7 = true,
            roomTypes = listOf(
                RoomType("General Ward", 1500, "Shared air-cooled ward with 6 beds, nurse call bell, and daily vitals monitoring."),
                RoomType("Semi-Private", 3800, "Twin sharing room with central AC, television, attendant recliner, and attached washroom."),
                RoomType("Private", 7500, "Single private room with motorized bed, patient TV, sofa bed for attendant, and attached bath."),
                RoomType("Deluxe Suite", 14000, "Spacious luxury room with visitor lounge, mini-refrigerator, dedicated nurse, and city view."),
                RoomType("ICU", 24000, "Intensive Care Unit with 1:1 nurse-to-patient ratio, advanced ventilators, and hemodynamic monitoring.")
            ),
            aboutHtml = """
                <h2>About MetroLife Super Speciality Hospital</h2>
                <p>MetroLife Super Speciality Hospital is a flagship tertiary care healthcare centre located in Andheri West, Mumbai. Established with a commitment to patient-centred excellence, the hospital offers comprehensive clinical care, cutting-edge surgical infrastructure, and round-the-clock emergency services.</p>
                <p>Our multidisciplinary team of distinguished consultants and skilled nursing professionals deliver compassionate, outcome-focused treatments across diverse specialties including Cardiology, Orthopaedics, and Neurosciences.</p>
                <ul>
                    <li>NABH and NABL accredited hospital infrastructure</li>
                    <li>Full-fledged emergency trauma care with triage protocol</li>
                    <li>Advanced interventional cath lab with radial access expertise</li>
                    <li>Ultra-clean laminar airflow modular operation theatres</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#1F7A8C"
        ),
        Hospital(
            id = "hosp_bandra",
            name = "Apex Care Multi-Speciality Hospital",
            area = "Bandra",
            city = "Mumbai",
            address = "18 Waterfield Road, Off Linking Road, Bandra West, Mumbai - 400050",
            phone = "+912226402200",
            rating = 4.7,
            reviewCount = 188,
            specialties = listOf("Dermatology", "Paediatrics", "Gynaecology", "General Physician", "Dentistry", "Cardiology"),
            facilities = listOf(
                "24x7 Emergency Services",
                "Dedicated Mother & Child Pavilion",
                "NICU & PICU Critical Care",
                "Aesthetic & Dermatology Clinic",
                "Digital Dental Imaging Unit",
                "In-house Pharmacy & Pathology",
                "Valet Parking & Cafeteria"
            ),
            emergency24x7 = true,
            roomTypes = listOf(
                RoomType("General Ward", 1800, "Comfortable multi-bed ward with dedicated nursing staff and regular physician rounds."),
                RoomType("Semi-Private", 4200, "Two-patient sharing room with privacy curtains, cable TV, and attendant couch."),
                RoomType("Private", 8500, "Air-conditioned private room with smart TV, ensuite bathroom, and attendant bedding."),
                RoomType("Deluxe Suite", 16500, "Premium executive suite featuring living lounge, microwave, dedicated assistant, and pantry."),
                RoomType("ICU", 26000, "State-of-the-art Neonatal, Paediatric, and Adult Critical Care Unit with advanced life support.")
            ),
            aboutHtml = """
                <h2>About Apex Care Multi-Speciality Hospital</h2>
                <p>Apex Care Hospital stands as a premier medical destination in Bandra, offering specialized clinical programs in women and child wellness, aesthetic dermatology, and family medicine.</p>
                <p>Combining modern clinical technology with compassionate hospitality, the facility ensures comfortable inpatient recovery, seamless outpatient consultations, and round-the-clock emergency support.</p>
                <ul>
                    <li>Specialized Birthing suites and Level III Neonatal ICU</li>
                    <li>Advanced cosmetic and laser dermatology department</li>
                    <li>Painless pediatric care protocols and vaccination clinic</li>
                    <li>24x7 pharmacy and express diagnostic reporting</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#2E7D32"
        ),
        Hospital(
            id = "hosp_vileparle",
            name = "Parle Horizon Healthcare",
            area = "Vile Parle",
            city = "Mumbai",
            address = "55 Hanuman Road, Near Railway Station, Vile Parle East, Mumbai - 400057",
            phone = "+912226113300",
            rating = 4.5,
            reviewCount = 96,
            specialties = listOf("Orthopaedics", "General Physician", "ENT", "Ophthalmology", "Psychiatry"),
            facilities = listOf(
                "24x7 Urgent Care Unit",
                "Joint Replacement Center",
                "Microscopic ENT Surgery Unit",
                "Ophthalmic Laser Suite",
                "Comprehensive Physiotherapy Hub",
                "Automated Pathology Laboratory",
                "Diagnostic Ultrasound & X-Ray"
            ),
            emergency24x7 = true,
            roomTypes = listOf(
                RoomType("General Ward", 1300, "Well-ventilated shared recovery ward with continuous nurse observation."),
                RoomType("Semi-Private", 3500, "Spacious two-bed room with television, personal locker, and attendant seating."),
                RoomType("Private", 6800, "Independent single room equipped with electric hospital bed, refrigerator, and attached bath."),
                RoomType("Deluxe Suite", 13000, "Executive patient suite with adjoining private lounge, dining space, and nurse call system."),
                RoomType("ICU", 22000, "High Dependency and Intensive Care Unit with multichannel patient monitors and ventilator support.")
            ),
            aboutHtml = """
                <h2>About Parle Horizon Healthcare</h2>
                <p>Parle Horizon Healthcare has served the Vile Parle and western suburbs community with high ethical standards and affordable, quality medical interventions for over two decades.</p>
                <p>Known for its robust orthopaedic joint replacement program and minimally invasive microsurgical facilities for eye and ear ailments, the hospital continues to be a trusted family health sanctuary.</p>
                <ul>
                    <li>Centrally located near Vile Parle station for easy patient transit</li>
                    <li>Computer-navigated joint replacement and arthroscopy theatre</li>
                    <li>Day-care eye cataract surgery with premium foldable IOLs</li>
                    <li>Dedicated mental health and geriatric wellness counselling</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#0277BD"
        ),
        Hospital(
            id = "hosp_powai",
            name = "Lakeside Medicity & Research Centre",
            area = "Powai",
            city = "Mumbai",
            address = "Central Avenue, Hiranandani Gardens, Powai, Mumbai - 400076",
            phone = "+912225704400",
            rating = 4.8,
            reviewCount = 210,
            specialties = listOf("Cardiology", "Neurology", "Oncology", "Gynaecology", "Orthopaedics", "General Physician"),
            facilities = listOf(
                "24x7 Level 1 Trauma & ER",
                "Comprehensive Cancer Care Center",
                "Robotic Surgery Theatre",
                "Neuro-Intensive Care Unit (NICU)",
                "PET-CT & 3T MRI Suite",
                "Cardiovascular Cath Lab",
                "Dialysis & Organ Transplant Wing",
                "Helipad Access & Advanced Ambulances"
            ),
            emergency24x7 = true,
            roomTypes = listOf(
                RoomType("General Ward", 2000, "Air-conditioned 4-bed cubicle ward with automated beds and electronic health monitoring."),
                RoomType("Semi-Private", 4800, "Twin sharing room with smart infotainment systems, attendant couch, and privacy screens."),
                RoomType("Private", 9500, "Single private room overlooking the lake, motorized bed, attendant sofa bed, and ensuite bath."),
                RoomType("Deluxe Suite", 18000, "Presidential medical suite with private guest lounge, pantry, dedicated nurse, and luxury care."),
                RoomType("ICU", 28000, "Next-generation Critical Care Unit with isolation cubicles, ECMO support, and neuro-monitoring.")
            ),
            aboutHtml = """
                <h2>About Lakeside Medicity & Research Centre</h2>
                <p>Lakeside Medicity & Research Centre is an internationally acclaimed multi-organ transplant and quaternary healthcare institution situated alongside the scenic Powai Lake.</p>
                <p>With cutting-edge robotic surgical suites, integrated oncology treatment programmes, and comprehensive neurological sciences, Lakeside Medicity sets high benchmarks in medical outcomes.</p>
                <ul>
                    <li>Da Vinci XI Robotic Surgical System for precision oncology & urology</li>
                    <li>Comprehensive Cardiac Science Centre with electrophysiology unit</li>
                    <li>Dedicated Stroke Response Team with rapid thrombolysis protocol</li>
                    <li>Eco-friendly healing architecture with lake-view patient recovery wings</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#512DA8"
        ),
        Hospital(
            id = "hosp_dadar",
            name = "Shivam Central Medical Institute",
            area = "Dadar",
            city = "Mumbai",
            address = "102 Shivaji Park Road 3, Dadar West, Mumbai - 400028",
            phone = "+912224455500",
            rating = 4.4,
            reviewCount = 115,
            specialties = listOf("General Physician", "Paediatrics", "Dentistry", "Dermatology", "Psychiatry"),
            facilities = listOf(
                "Day Care Medical Center",
                "Pediatric Intensive Care Unit",
                "Comprehensive Dental Surgery Unit",
                "Dermatology & Cosmetology Lab",
                "Mental Wellness & Psychotherapy Clinic",
                "Diagnostic Laboratory & Ultrasound",
                "In-house Pharmacy"
            ),
            emergency24x7 = false,
            roomTypes = listOf(
                RoomType("General Ward", 1200, "Standard 6-bed ward with round-the-clock nursing supervision and hygienic amenities."),
                RoomType("Semi-Private", 3200, "Twin sharing room with television, storage cabinet, and comfortable attendant seating."),
                RoomType("Private", 6200, "Single room with attached bathroom, air conditioning, television, and attendant bed."),
                RoomType("Deluxe Suite", 12000, "Large patient suite with sofa set, mini-fridge, private dining area, and dedicated care."),
                RoomType("ICU", 20000, "Specialized medical ICU equipped with multi-parameter vitals monitors and emergency backup.")
            ),
            aboutHtml = """
                <h2>About Shivam Central Medical Institute</h2>
                <p>Shivam Central Medical Institute in Dadar West is known for community-rooted healthcare, personalized family doctor practices, and comprehensive outpatient specialty clinics.</p>
                <p>Located in the heritage vicinity of Shivaji Park, it provides easily accessible preventive health check-ups, paediatric vaccinations, clinical dermatology, and psychological wellness counseling.</p>
                <ul>
                    <li>Experienced panel of senior family physicians and paediatricians</li>
                    <li>Modern painless root canal and dental cosmetic implant procedures</li>
                    <li>Confidential psychiatric assessment and cognitive behavioral therapy</li>
                    <li>Same-day blood reports with automated digital delivery</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#C2185B"
        ),
        Hospital(
            id = "hosp_thane",
            name = "Highland Crest Multispeciality Hospital",
            area = "Thane",
            city = "Mumbai",
            address = "Ghodbunder Road, Near Majiwada Junction, Thane West - 400601",
            phone = "+912225446600",
            rating = 4.5,
            reviewCount = 130,
            specialties = listOf("Orthopaedics", "Cardiology", "Oncology", "ENT", "General Physician"),
            facilities = listOf(
                "24x7 Emergency Trauma Unit",
                "Digital Flat-Panel Cath Lab",
                "Oncology Day Care Chemotherapy",
                "Sports Medicine & Rehab Center",
                "Advanced ENT Endoscopy Clinic",
                "24x7 Pathology & Blood Storage",
                "Spacious Parking & Ambulance Service"
            ),
            emergency24x7 = true,
            roomTypes = listOf(
                RoomType("General Ward", 1400, "Economical 6-bed ward with continuous nurse monitoring and daily senior doctor rounds."),
                RoomType("Semi-Private", 3600, "Twin sharing room with AC, separate curtain cubicles, TV, and attendant chairs."),
                RoomType("Private", 7000, "Individual patient room with attached bathroom, patient entertainment TV, and sofa cum bed."),
                RoomType("Deluxe Suite", 13500, "Executive suite featuring private visitor room, kitchenette, tea-maker, and personalized care."),
                RoomType("ICU", 23000, "Advanced Critical Care Unit equipped with invasive hemodynamic monitors and high-end ventilators.")
            ),
            aboutHtml = """
                <h2>About Highland Crest Multispeciality Hospital</h2>
                <p>Highland Crest Multispeciality Hospital serves the rapidly expanding population of Thane and neighbouring central suburbs with multi-organ medical and surgical facilities.</p>
                <p>Strategically situated near Majiwada Junction on Ghodbunder Road, the hospital offers speedy accident trauma response, sports injury rehabilitation, and modern cancer chemotherapy.</p>
                <ul>
                    <li>Rapid response highway ambulance network with mobile telemetry</li>
                    <li>Minimally invasive arthroscopic surgery and spine endoscopy</li>
                    <li>Day-care chemotherapy lounge with experienced oncologists</li>
                    <li>24x7 emergency medical triage and critical care unit</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#E65100"
        ),
        Hospital(
            id = "hosp_vashi",
            name = "Palm Beach Memorial Hospital",
            area = "Vashi - Navi Mumbai",
            city = "Mumbai",
            address = "Sector 17, Palm Beach Road, Vashi, Navi Mumbai - 400703",
            phone = "+912227897700",
            rating = 4.6,
            reviewCount = 124,
            specialties = listOf("Paediatrics", "Gynaecology", "Ophthalmology", "General Physician", "Dermatology"),
            facilities = listOf(
                "24x7 Women & Child Care Emergency",
                "Retinal & Cataract Surgery Suite",
                "Level III NICU & Paediatric Wing",
                "Laser Skin & Hair Treatment Clinic",
                "Laparoscopic Gynaecology Theatre",
                "Full-Service In-house Pharmacy",
                "Dedicated Dialysis Center"
            ),
            emergency24x7 = true,
            roomTypes = listOf(
                RoomType("General Ward", 1450, "Well-maintained 6-bed shared recovery ward with round-the-clock nursing assistance."),
                RoomType("Semi-Private", 3700, "Twin sharing room with central AC, smart television, and comfortable attendant lounger."),
                RoomType("Private", 7200, "Private room with private washroom, motorized patient bed, refrigerator, and sofa bed."),
                RoomType("Deluxe Suite", 14500, "Spacious suite with separate living area, attendant couch, scenic palm-view, and nurse call."),
                RoomType("ICU", 24500, "Specialized Neonatal and Adult Intensive Care Unit with continuous patient parameters monitoring.")
            ),
            aboutHtml = """
                <h2>About Palm Beach Memorial Hospital</h2>
                <p>Palm Beach Memorial Hospital is a landmark healthcare institution situated on the picturesque Palm Beach corridor in Vashi, Navi Mumbai.</p>
                <p>The hospital specializes in advanced maternal-fetal medicine, neonatal intensive care, advanced retinal microsurgery, and aesthetic dermatological procedures with uncompromising hygiene.</p>
                <ul>
                    <li>NABH accredited healthcare setup designed for clinical excellence</li>
                    <li>High-risk pregnancy clinic with painless delivery suites</li>
                    <li>Advanced phacoemulsification and vitreoretinal surgical setups</li>
                    <li>Emergency pediatric ambulance with portable incubator support</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#00838F"
        ),
        Hospital(
            id = "hosp_borivali",
            name = "Borivali Premier Healthcare",
            area = "Borivali",
            city = "Mumbai",
            address = "SV Road, Near Shimpoli Signal, Borivali West, Mumbai - 400092",
            phone = "+912228988800",
            rating = 4.3,
            reviewCount = 89,
            specialties = listOf("Dentistry", "Psychiatry", "Neurology", "General Physician", "Cardiology"),
            facilities = listOf(
                "Outpatient Specialist Consultation Suites",
                "Neuro-Diagnostics (EEG & EMG)",
                "Digital Implant Dental Center",
                "Cognitive Rehab & Psychotherapy Wing",
                "Preventive Cardiology Health Check",
                "24x7 Clinical Laboratory",
                "Emergency Observation Beds"
            ),
            emergency24x7 = false,
            roomTypes = listOf(
                RoomType("General Ward", 1250, "Clean 6-bed shared ward with dedicated nursing staff and regular medical monitoring."),
                RoomType("Semi-Private", 3400, "Twin-sharing air-conditioned room with television, attendant seating, and privacy curtains."),
                RoomType("Private", 6500, "Single patient room with ensuite washroom, LCD television, and attendant couch."),
                RoomType("Deluxe Suite", 12500, "Executive room with attached lounge area, work desk, refrigerator, and personalized attention."),
                RoomType("ICU", 21000, "Modern Intensive Care Unit with multipara monitors, ventilator support, and emergency equipment.")
            ),
            aboutHtml = """
                <h2>About Borivali Premier Healthcare</h2>
                <p>Borivali Premier Healthcare brings together expert clinical specialists across Neurology, Psychiatry, Dentistry, and Family Medicine to deliver cohesive care in North Mumbai.</p>
                <p>With an emphasis on preventive lifestyle screening, cognitive and emotional wellness, and painless dental rehabilitation, the hospital caters to patients of all age groups.</p>
                <ul>
                    <li>Advanced neuro-electrophysiology lab for epilepsy and neuropathy evaluations</li>
                    <li>Comprehensive de-addiction, stress management, and memory clinic</li>
                    <li>Fully digitized 3D smile design and ceramic dental implants</li>
                    <li>Affordable executive health checkup packages for working professionals</li>
                </ul>
            """.trimIndent(),
            accentColorHex = "#D81B60"
        )
    )

    private val doctorsList: List<Doctor> = listOf(
        // Hospital 1: MetroLife Super Speciality Hospital (Andheri West)
        Doctor(
            id = "doc_andheri_1",
            hospitalId = "hosp_andheri",
            name = "Dr. Rajesh Kulkarni",
            specialty = "General Physician",
            qualifications = "MBBS, MD (Internal Medicine)",
            experienceYears = 18,
            consultationFee = 800,
            rating = 4.7,
            reviewCount = 92,
            availableDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Rajesh Kulkarni is a senior physician with over 18 years of clinical practice managing chronic diabetes, hypertension, infectious diseases, and preventive adult healthcare."
        ),
        Doctor(
            id = "doc_andheri_2",
            hospitalId = "hosp_andheri",
            name = "Dr. Sunita Mehta",
            specialty = "Cardiology",
            qualifications = "MBBS, MD, DM (Cardiology), FACC",
            experienceYears = 22,
            consultationFee = 1800,
            rating = 4.9,
            reviewCount = 134,
            availableDays = listOf("Mon", "Wed", "Fri", "Sat"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Gujarati"),
            about = "Dr. Sunita Mehta is a renowned interventional cardiologist specializing in complex coronary angioplasties, cardiac pacing, and preventive cardiovascular wellness."
        ),
        Doctor(
            id = "doc_andheri_3",
            hospitalId = "hosp_andheri",
            name = "Dr. Amit Deshmukh",
            specialty = "Orthopaedics",
            qualifications = "MBBS, MS (Orthopaedics), MCh (Ortho)",
            experienceYears = 14,
            consultationFee = 1200,
            rating = 4.6,
            reviewCount = 78,
            availableDays = listOf("Tue", "Thu", "Fri", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Amit Deshmukh specializes in computer-assisted knee and hip arthroplasty, arthroscopic ligament reconstructions, and complex sports injury rehabilitation."
        ),

        // Hospital 2: Apex Care Multi-Speciality Hospital (Bandra)
        Doctor(
            id = "doc_bandra_1",
            hospitalId = "hosp_bandra",
            name = "Dr. Pooja Singhania",
            specialty = "Dermatology",
            qualifications = "MBBS, MD (Dermatology & Venereology)",
            experienceYears = 11,
            consultationFee = 1000,
            rating = 4.8,
            reviewCount = 112,
            availableDays = listOf("Mon", "Tue", "Wed", "Fri"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi"),
            about = "Dr. Pooja Singhania is an expert medical and aesthetic dermatologist with extensive experience in clinical acne management, eczema, laser therapies, and anti-aging treatments."
        ),
        Doctor(
            id = "doc_bandra_2",
            hospitalId = "hosp_bandra",
            name = "Dr. Rohan Merchant",
            specialty = "Paediatrics",
            qualifications = "MBBS, DCH, DNB (Paediatrics)",
            experienceYears = 9,
            consultationFee = 900,
            rating = 4.7,
            reviewCount = 85,
            availableDays = listOf("Mon", "Wed", "Thu", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Gujarati"),
            about = "Dr. Rohan Merchant is a caring paediatrician known for his gentle bedside manner, managing newborn care, childhood respiratory infections, nutrition, and vaccination."
        ),
        Doctor(
            id = "doc_bandra_3",
            hospitalId = "hosp_bandra",
            name = "Dr. Neha Kapoor",
            specialty = "Gynaecology",
            qualifications = "MBBS, MS (Obstetrics & Gynaecology), DGO",
            experienceYears = 16,
            consultationFee = 1500,
            rating = 4.9,
            reviewCount = 145,
            availableDays = listOf("Tue", "Wed", "Fri", "Sat"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi", "Punjabi"),
            about = "Dr. Neha Kapoor is a senior consultant obstetrician and laparoscopic gynaecologist specializing in high-risk pregnancies, PCOS management, and minimally invasive surgeries."
        ),

        // Hospital 3: Parle Horizon Healthcare (Vile Parle)
        Doctor(
            id = "doc_parle_1",
            hospitalId = "hosp_vileparle",
            name = "Dr. Vikram Joshi",
            specialty = "Orthopaedics",
            qualifications = "MBBS, MS (Ortho), Fellowship in Spine Surgery",
            experienceYears = 15,
            consultationFee = 1100,
            rating = 4.5,
            reviewCount = 64,
            availableDays = listOf("Mon", "Tue", "Thu", "Fri"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Vikram Joshi brings 15 years of surgical expertise in degenerative spine conditions, herniated discs, osteoporosis management, and joint trauma."
        ),
        Doctor(
            id = "doc_parle_2",
            hospitalId = "hosp_vileparle",
            name = "Dr. Aditi Samant",
            specialty = "ENT",
            qualifications = "MBBS, MS (ENT), DLO",
            experienceYears = 12,
            consultationFee = 850,
            rating = 4.6,
            reviewCount = 70,
            availableDays = listOf("Mon", "Wed", "Fri", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Aditi Samant is a specialist ENT surgeon adept in micro-ear surgeries, endoscopic sinus surgery, vertigo assessment, and chronic allergic rhinitis treatments."
        ),
        Doctor(
            id = "doc_parle_3",
            hospitalId = "hosp_vileparle",
            name = "Dr. Sanjay Shah",
            specialty = "Ophthalmology",
            qualifications = "MBBS, MS (Ophthalmology), FICO",
            experienceYears = 20,
            consultationFee = 1000,
            rating = 4.8,
            reviewCount = 88,
            availableDays = listOf("Tue", "Wed", "Thu", "Sat"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi", "Gujarati"),
            about = "Dr. Sanjay Shah is an accomplished ophthalmic surgeon with over 10,000 successful cataract and LASIK surgeries, specializing in micro-incision phacoemulsification and glaucoma."
        ),

        // Hospital 4: Lakeside Medicity & Research Centre (Powai)
        Doctor(
            id = "doc_powai_1",
            hospitalId = "hosp_powai",
            name = "Dr. Arvind Chawla",
            specialty = "Neurology",
            qualifications = "MBBS, MD, DM (Neurology)",
            experienceYears = 24,
            consultationFee = 2200,
            rating = 4.9,
            reviewCount = 156,
            availableDays = listOf("Mon", "Tue", "Wed", "Thu"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi"),
            about = "Dr. Arvind Chawla is a nationally prominent neurologist specializing in acute stroke intervention, Parkinson's disease, epilepsy management, and neurological rehabilitation."
        ),
        Doctor(
            id = "doc_powai_2",
            hospitalId = "hosp_powai",
            name = "Dr. Meera Iyer",
            specialty = "Cardiology",
            qualifications = "MBBS, MD, DNB (Cardiology), FESC",
            experienceYears = 17,
            consultationFee = 1600,
            rating = 4.8,
            reviewCount = 105,
            availableDays = listOf("Tue", "Thu", "Fri", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Tamil"),
            about = "Dr. Meera Iyer is a distinguished non-invasive and preventive cardiologist with expertise in 3D echocardiography, heart failure management, and women's cardiac wellness."
        ),
        Doctor(
            id = "doc_powai_3",
            hospitalId = "hosp_powai",
            name = "Dr. Siddharth Menon",
            specialty = "Oncology",
            qualifications = "MBBS, MD, DM (Medical Oncology)",
            experienceYears = 19,
            consultationFee = 2500,
            rating = 4.9,
            reviewCount = 98,
            availableDays = listOf("Mon", "Wed", "Fri"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi", "Malayalam"),
            about = "Dr. Siddharth Menon is a medical oncologist with immense experience in targeted chemotherapy, immunotherapy, and precision cancer genomics."
        ),

        // Hospital 5: Shivam Central Medical Institute (Dadar)
        Doctor(
            id = "doc_dadar_1",
            hospitalId = "hosp_dadar",
            name = "Dr. Anand Patwardhan",
            specialty = "General Physician",
            qualifications = "MBBS, MD (Medicine)",
            experienceYears = 28,
            consultationFee = 600,
            rating = 4.6,
            reviewCount = 110,
            availableDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Anand Patwardhan is a revered senior family physician in Dadar with 28 years of dedicated community practice in adult geriatrics, fevers, and chronic illness."
        ),
        Doctor(
            id = "doc_dadar_2",
            hospitalId = "hosp_dadar",
            name = "Dr. Tanvi Parekh",
            specialty = "Dermatology",
            qualifications = "MBBS, DVD, DNB (Dermatology)",
            experienceYears = 8,
            consultationFee = 750,
            rating = 4.4,
            reviewCount = 52,
            availableDays = listOf("Mon", "Wed", "Thu", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Gujarati"),
            about = "Dr. Tanvi Parekh treats hair fall disorders, fungal skin conditions, psoriasis, and pediatric skin ailments with an empathetic and scientific approach."
        ),
        Doctor(
            id = "doc_dadar_3",
            hospitalId = "hosp_dadar",
            name = "Dr. Kunal Shinde",
            specialty = "Dentistry",
            qualifications = "BDS, MDS (Prosthodontics & Implantology)",
            experienceYears = 10,
            consultationFee = 500,
            rating = 4.5,
            reviewCount = 68,
            availableDays = listOf("Tue", "Thu", "Fri", "Sat"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Kunal Shinde is a dental surgeon skilled in single-sitting painless root canals, dental implants, teeth alignment, and digital cosmetic smile makeovers."
        ),

        // Hospital 6: Highland Crest Multispeciality Hospital (Thane)
        Doctor(
            id = "doc_thane_1",
            hospitalId = "hosp_thane",
            name = "Dr. Pradeep Nair",
            specialty = "Psychiatry",
            qualifications = "MBBS, MD (Psychiatry), DPM",
            experienceYears = 16,
            consultationFee = 1400,
            rating = 4.8,
            reviewCount = 84,
            availableDays = listOf("Mon", "Tue", "Thu", "Sat"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Malayalam"),
            about = "Dr. Pradeep Nair is a compassionate psychiatrist providing comprehensive clinical therapy for generalized anxiety, clinical depression, adult ADHD, and stress management."
        ),
        Doctor(
            id = "doc_thane_2",
            hospitalId = "hosp_thane",
            name = "Dr. Shalini Saxena",
            specialty = "ENT",
            qualifications = "MBBS, MS (ENT), DNB",
            experienceYears = 13,
            consultationFee = 900,
            rating = 4.5,
            reviewCount = 60,
            availableDays = listOf("Mon", "Wed", "Fri", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi"),
            about = "Dr. Shalini Saxena specializes in pediatric airway problems, snoring and sleep apnea evaluation, advanced tympanoplasty, and voice restoration therapies."
        ),
        Doctor(
            id = "doc_thane_3",
            hospitalId = "hosp_thane",
            name = "Dr. Vivek Agarwal",
            specialty = "Oncology",
            qualifications = "MBBS, MS (General Surgery), MCh (Surgical Oncology)",
            experienceYears = 21,
            consultationFee = 2000,
            rating = 4.7,
            reviewCount = 92,
            availableDays = listOf("Tue", "Wed", "Fri"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi"),
            about = "Dr. Vivek Agarwal is a surgical oncologist with international training in minimally invasive oncological resections, gastrointestinal tumors, and breast cancer conservation."
        ),

        // Hospital 7: Palm Beach Memorial Hospital (Vashi)
        Doctor(
            id = "doc_vashi_1",
            hospitalId = "hosp_vashi",
            name = "Dr. Geeta Nambiar",
            specialty = "Paediatrics",
            qualifications = "MBBS, MD (Paediatrics), Fellowship in Neonatology",
            experienceYears = 18,
            consultationFee = 1000,
            rating = 4.8,
            reviewCount = 118,
            availableDays = listOf("Mon", "Tue", "Wed", "Thu", "Sat"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Malayalam"),
            about = "Dr. Geeta Nambiar heads neonatal intensive care at Palm Beach Memorial, guiding parents through premature infant care, growth milestones, and pediatric immunology."
        ),
        Doctor(
            id = "doc_vashi_2",
            hospitalId = "hosp_vashi",
            name = "Dr. Snehal Patil",
            specialty = "Gynaecology",
            qualifications = "MBBS, DGO, DNB (Obstetrics & Gynaecology)",
            experienceYears = 14,
            consultationFee = 1100,
            rating = 4.7,
            reviewCount = 96,
            availableDays = listOf("Tue", "Wed", "Fri", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Marathi"),
            about = "Dr. Snehal Patil provides holistic prenatal care, normal birthing guidance, adolescent gynaecological counseling, and advanced hysteroscopic procedures."
        ),
        Doctor(
            id = "doc_vashi_3",
            hospitalId = "hosp_vashi",
            name = "Dr. Harish Bhatt",
            specialty = "Ophthalmology",
            qualifications = "MBBS, DO, DNB (Ophthalmology)",
            experienceYears = 23,
            consultationFee = 950,
            rating = 4.6,
            reviewCount = 74,
            availableDays = listOf("Mon", "Wed", "Thu", "Fri"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi", "Gujarati"),
            about = "Dr. Harish Bhatt is a senior eye specialist in Navi Mumbai with wide experience in diabetic retinopathy screening, macular disorders, and blade-free laser vision correction."
        ),

        // Hospital 8: Borivali Premier Healthcare (Borivali)
        Doctor(
            id = "doc_borivali_1",
            hospitalId = "hosp_borivali",
            name = "Dr. Ritu Trivedi",
            specialty = "Dentistry",
            qualifications = "BDS, MDS (Orthodontics & Dentofacial Orthopaedics)",
            experienceYears = 7,
            consultationFee = 600,
            rating = 4.4,
            reviewCount = 45,
            availableDays = listOf("Mon", "Tue", "Thu", "Sat"),
            availableSlots = commonSlots,
            languages = listOf("English", "Hindi", "Gujarati"),
            about = "Dr. Ritu Trivedi specializes in invisible clear aligners, metallic and ceramic braces, bite correction, and aesthetic smile enhancement for teenagers and adults."
        ),
        Doctor(
            id = "doc_borivali_2",
            hospitalId = "hosp_borivali",
            name = "Dr. Sameer Khan",
            specialty = "Neurology",
            qualifications = "MBBS, MD (Medicine), DM (Neurology)",
            experienceYears = 15,
            consultationFee = 1500,
            rating = 4.7,
            reviewCount = 82,
            availableDays = listOf("Tue", "Wed", "Fri", "Sat"),
            availableSlots = afternoonSlots,
            languages = listOf("English", "Hindi", "Urdu"),
            about = "Dr. Sameer Khan is a skilled clinical neurologist focusing on migraine and chronic headache management, neuromuscular disorders, peripheral neuropathies, and vertigo."
        ),
        Doctor(
            id = "doc_borivali_3",
            hospitalId = "hosp_borivali",
            name = "Dr. Deepa Sunder",
            specialty = "Psychiatry",
            qualifications = "MBBS, DNB (Psychiatry)",
            experienceYears = 12,
            consultationFee = 1300,
            rating = 4.6,
            reviewCount = 59,
            availableDays = listOf("Mon", "Wed", "Thu", "Fri"),
            availableSlots = eveningSlots,
            languages = listOf("English", "Hindi", "Tamil"),
            about = "Dr. Deepa Sunder provides psychotherapy and medical management for mood disorders, panic attacks, sleep disturbances, and work-related burnout syndromes."
        )
    )

    private val reviewsList: List<Review> = listOf(
        // Reviews for Hospital 1
        Review("rev_hosp_andheri_1", "hosp_andheri", "Amitabh Sen", 5, "Remarkable emergency response when my father had acute chest pain. The cardiology team attended within minutes.", 1716300000000L),
        Review("rev_hosp_andheri_2", "hosp_andheri", "Priyanka Gokhale", 4, "Clean and well-maintained private rooms. Attending nursing staff was very courteous and supportive.", 1718892000000L),
        Review("rev_hosp_andheri_3", "hosp_andheri", "Kiran Varma", 5, "Modern facilities, cashless insurance billing was processed without unnecessary delays. Very satisfied.", 1721570400000L),

        // Reviews for Hospital 2
        Review("rev_hosp_bandra_1", "hosp_bandra", "Sunil Advani", 5, "Had my daughter's treatment here. Paediatric staff were gentle and accommodating. High hygienic standards.", 1717000000000L),
        Review("rev_hosp_bandra_2", "hosp_bandra", "Farah Merchant", 4, "Good facilities and parking valet is very convenient in busy Bandra. Doctors are knowledgeable.", 1719500000000L),
        Review("rev_hosp_bandra_3", "hosp_bandra", "Nikhil Dsouza", 5, "Excellent outpatient experience. Digital appointment tracking saved us waiting time at the clinic.", 1722000000000L),

        // Reviews for Hospital 3
        Review("rev_hosp_vileparle_1", "hosp_vileparle", "Ramesh Solanki", 5, "Underwent knee replacement under Dr. Vikram Joshi. Physiotherapy department got me walking within days.", 1716500000000L),
        Review("rev_hosp_vileparle_2", "hosp_vileparle", "Smita Tendulkar", 4, "Reasonable consultation rates compared to other corporate hospitals. Doctors give ample time to patients.", 1719000000000L),
        Review("rev_hosp_vileparle_3", "hosp_vileparle", "Kailash Shah", 4, "Good hospital located very close to Vile Parle railway station. Clean wards and helpful staff.", 1721000000000L),

        // Reviews for Hospital 4
        Review("rev_hosp_powai_1", "hosp_powai", "Gautam Malhotra", 5, "World-class infrastructure in Powai. Lakeside view from deluxe rooms accelerates healing. Superb medical care.", 1717200000000L),
        Review("rev_hosp_powai_2", "hosp_powai", "Ranjana Pillai", 5, "The oncology team showed utmost dedication during my mother's treatment. Highly recommended hospital.", 1719700000000L),
        Review("rev_hosp_powai_3", "hosp_powai", "Vikramaditya Bose", 4, "Advanced diagnostics available under one roof. Automated test reporting directly to phone is helpful.", 1722200000000L),

        // Reviews for Hospital 5
        Review("rev_hosp_dadar_1", "hosp_dadar", "Sachin Tawde", 5, "Traditional warmth with modern medical capability. Dr. Anand Patwardhan is a true family doctor.", 1716800000000L),
        Review("rev_hosp_dadar_2", "hosp_dadar", "Vaishali Korde", 4, "Affordable rates and quick outpatient consultations. Dental care clinic has latest digital scanners.", 1719200000000L),
        Review("rev_hosp_dadar_3", "hosp_dadar", "Milind Chitnis", 4, "Centrally situated in Dadar near Shivaji Park. Staff is polite and the laboratory is fast.", 1721800000000L),

        // Reviews for Hospital 6
        Review("rev_hosp_thane_1", "hosp_thane", "Aniket Parab", 5, "Immediate trauma response for my brother after a road incident. Intensive care specialists were top notch.", 1717500000000L),
        Review("rev_hosp_thane_2", "hosp_thane", "Bhavna Jani", 4, "Comfortable semi-private ward and polite nurses. Ample parking on Ghodbunder road.", 1720000000000L),
        Review("rev_hosp_thane_3", "hosp_thane", "Siddhesh Rane", 5, "Oncology day care is very well run with zero wait times. Clean environment and empathetic caregivers.", 1722500000000L),

        // Reviews for Hospital 7
        Review("rev_hosp_vashi_1", "hosp_vashi", "Alok Srivastava", 5, "Best maternity and newborn care in Navi Mumbai. Our delivery was handled with utmost care and joy.", 1717800000000L),
        Review("rev_hosp_vashi_2", "hosp_vashi", "Manjiri Gupte", 4, "Pleasant hospital ambiance facing Palm Beach road. Ophthalmologist was patient in answering all questions.", 1720300000000L),
        Review("rev_hosp_vashi_3", "hosp_vashi", "Chetan Mhatre", 5, "Emergency pediatric unit is well equipped. Doctors treated our child's dehydration promptly at midnight.", 1722800000000L),

        // Reviews for Hospital 8
        Review("rev_hosp_borivali_1", "hosp_borivali", "Harshida Parekh", 4, "Quality dental and neurology consultation under one roof. Clean premises and systematic token system.", 1718000000000L),
        Review("rev_hosp_borivali_2", "hosp_borivali", "Dharmesh Soni", 5, "Dr. Sameer Khan resolved my chronic migraine with targeted therapy. Very grateful to the neurology clinic.", 1720500000000L),
        Review("rev_hosp_borivali_3", "hosp_borivali", "Pooja Vora", 4, "Good healthcare centre on SV Road. Reasonable fees and cooperative front desk team.", 1723000000000L),

        // Doctor Reviews (3 for each of 24 doctors = 72 reviews)
        // doc_andheri_1
        Review("rev_doc_andheri_1_1", "doc_andheri_1", "Ramesh K.", 5, "Very humble and thorough. Dr. Kulkarni adjusted my diabetic dosage properly.", 1716000000000L),
        Review("rev_doc_andheri_1_2", "doc_andheri_1", "Geeta S.", 4, "Accurate diagnosis and never prescribes unnecessary medicines.", 1718000000000L),
        Review("rev_doc_andheri_1_3", "doc_andheri_1", "Vijay M.", 5, "Excellent doctor who listens patiently to all health concerns.", 1720000000000L),

        // doc_andheri_2
        Review("rev_doc_andheri_2_1", "doc_andheri_2", "Devendra P.", 5, "Dr. Sunita Mehta is a lifesaver. Handled my stent placement with supreme confidence.", 1716100000000L),
        Review("rev_doc_andheri_2_2", "doc_andheri_2", "Sunita D.", 5, "Best cardiologist in Mumbai. Highly experienced and very reassuring.", 1718100000000L),
        Review("rev_doc_andheri_2_3", "doc_andheri_2", "Gopal N.", 4, "Explains ECG and 2D Echo results clearly in layman terms.", 1720100000000L),

        // doc_andheri_3
        Review("rev_doc_andheri_3_1", "doc_andheri_3", "Naveen T.", 5, "Rebuilt my torn ACL via arthroscopy. Back on the badminton court in 5 months.", 1716200000000L),
        Review("rev_doc_andheri_3_2", "doc_andheri_3", "Sangeeta R.", 4, "Good knee pain advice without rushing into immediate surgery.", 1718200000000L),
        Review("rev_doc_andheri_3_3", "doc_andheri_3", "Mahesh B.", 5, "Dr. Deshmukh is very professional and caring.", 1720200000000L),

        // doc_bandra_1
        Review("rev_doc_bandra_1_1", "doc_bandra_1", "Ayesha M.", 5, "Dr. Pooja cured my stubborn adult acne in 3 months. Fantastic results!", 1716300000000L),
        Review("rev_doc_bandra_1_2", "doc_bandra_1", "Kareena T.", 5, "Very subtle aesthetic approach, does not push cosmetic treatments.", 1718300000000L),
        Review("rev_doc_bandra_1_3", "doc_bandra_1", "Rohit J.", 4, "Clear instructions on skin routine and sun protection.", 1720300000000L),

        // doc_bandra_2
        Review("rev_doc_bandra_2_1", "doc_bandra_2", "Neha B.", 5, "Dr. Rohan is so kind with toddlers. My son did not cry during vaccination.", 1716400000000L),
        Review("rev_doc_bandra_2_2", "doc_bandra_2", "Karan V.", 4, "Available on emergency call during fever spikes. Very dependable paediatrician.", 1718400000000L),
        Review("rev_doc_bandra_2_3", "doc_bandra_2", "Simran K.", 5, "Wonderful pediatric advice regarding child nutrition and growth.", 1720400000000L),

        // doc_bandra_3
        Review("rev_doc_bandra_3_1", "doc_bandra_3", "Shweta G.", 5, "Dr. Neha delivered our twins safely. Her calm demeanor throughout labor was invaluable.", 1716500000000L),
        Review("rev_doc_bandra_3_2", "doc_bandra_3", "Ananya S.", 5, "Helped me manage severe PCOS with lifestyle and appropriate medicines.", 1718500000000L),
        Review("rev_doc_bandra_3_3", "doc_bandra_3", "Meenakshi L.", 4, "Extremely polite and experienced obstetrician.", 1720500000000L),

        // doc_parle_1
        Review("rev_doc_parle_1_1", "doc_parle_1", "Dilip K.", 5, "Dr. Joshi treated my chronic slip disc. Very ethical approach to spine care.", 1716600000000L),
        Review("rev_doc_parle_1_2", "doc_parle_1", "Usha P.", 4, "Clear explanation of joint pain exercises and posture corrections.", 1718600000000L),
        Review("rev_doc_parle_1_3", "doc_parle_1", "Mohan S.", 4, "Courteous doctor who avoids redundant imaging tests.", 1720600000000L),

        // doc_parle_2
        Review("rev_doc_parle_2_1", "doc_parle_2", "Ashok C.", 5, "Dr. Aditi solved my long-standing ear infection and tinnitus issue.", 1716700000000L),
        Review("rev_doc_parle_2_2", "doc_parle_2", "Kavita M.", 4, "Gentle ear examination with video otoscope. Recommended!", 1718700000000L),
        Review("rev_doc_parle_2_3", "doc_parle_2", "Prashant T.", 5, "Very effective sinus allergy treatment protocol.", 1720700000000L),

        // doc_parle_3
        Review("rev_doc_parle_3_1", "doc_parle_3", "Jayesh G.", 5, "Painless cataract surgery for both my mother's eyes. Clear 6/6 vision restored.", 1716800000000L),
        Review("rev_doc_parle_3_2", "doc_parle_3", "Bhavna P.", 5, "Dr. Sanjay Shah is extremely patient and competent in eye diagnostics.", 1718800000000L),
        Review("rev_doc_parle_3_3", "doc_parle_3", "Dhiren V.", 4, "Clean clinic, prompt dilated retina examination.", 1720800000000L),

        // doc_powai_1
        Review("rev_doc_powai_1_1", "doc_powai_1", "Arun K.", 5, "Dr. Arvind Chawla diagnosed my nerve conduction issue accurately. Top neurologist.", 1716900000000L),
        Review("rev_doc_powai_1_2", "doc_powai_1", "Radha N.", 5, "Outstanding expertise in Parkinson's and movement disorders.", 1718900000000L),
        Review("rev_doc_powai_1_3", "doc_powai_1", "Shekhar B.", 4, "Very deep clinical insight, gives clear recovery roadmap.", 1720900000000L),

        // doc_powai_2
        Review("rev_doc_powai_2_1", "doc_powai_2", "Lakshmi R.", 5, "Dr. Meera Iyer's cardiac preventive screening prevented a major coronary event.", 1717000000000L),
        Review("rev_doc_powai_2_2", "doc_powai_2", "Srinivas K.", 5, "Kind, reassuring, and answers every question about cholesterol and BP.", 1719000000000L),
        Review("rev_doc_powai_2_3", "doc_powai_2", "Padma V.", 4, "Great clinical follow-up for hypertensive heart disease.", 1721000000000L),

        // doc_powai_3
        Review("rev_doc_powai_3_1", "doc_powai_3", "Manoj S.", 5, "Dr. Siddharth Menon guided our family through chemo with immense empathy and science.", 1717100000000L),
        Review("rev_doc_powai_3_2", "doc_powai_3", "Deepak B.", 5, "Superb precision in modern cancer immunotherapy. Always reachable.", 1719100000000L),
        Review("rev_doc_powai_3_3", "doc_powai_3", "Vidya N.", 4, "Compassionate oncologist who instills real hope and confidence.", 1721100000000L),

        // doc_dadar_1
        Review("rev_doc_dadar_1_1", "doc_dadar_1", "Santosh K.", 5, "Dr. Patwardhan has been treating our entire family for 20 years. Gold standard doctor.", 1717200000000L),
        Review("rev_doc_dadar_1_2", "doc_dadar_1", "Girish P.", 4, "Affordable, experienced, and very reliable diagnosis.", 1719200000000L),
        Review("rev_doc_dadar_1_3", "doc_dadar_1", "Sushila J.", 5, "Very humble nature. Never makes patients panic over reports.", 1721200000000L),

        // doc_dadar_2
        Review("rev_doc_dadar_2_1", "doc_dadar_2", "Kavita S.", 5, "Dr. Tanvi Parekh solved my eczema outbreak with a mild cream. Very happy.", 1717300000000L),
        Review("rev_doc_dadar_2_2", "doc_dadar_2", "Hemant D.", 4, "Affordable skin clinic in Dadar with good hygiene.", 1719300000000L),
        Review("rev_doc_dadar_2_3", "doc_dadar_2", "Rupal T.", 4, "Effective hair fall consultation and vitamin guidelines.", 1721300000000L),

        // doc_dadar_3
        Review("rev_doc_dadar_3_1", "doc_dadar_3", "Nitin G.", 5, "Painless molar root canal done in a single sitting! Highly skilled dentist.", 1717400000000L),
        Review("rev_doc_dadar_3_2", "doc_dadar_3", "Madhuri R.", 4, "Very gentle tooth cleaning and cavity filling. No discomfort.", 1719400000000L),
        Review("rev_doc_dadar_3_3", "doc_dadar_3", "Parag C.", 4, "Modern dental equipment and clear explanation of treatment options.", 1721400000000L),

        // doc_thane_1
        Review("rev_doc_thane_1_1", "doc_thane_1", "Anil M.", 5, "Dr. Pradeep Nair helped me conquer debilitating panic attacks. Great therapy sessions.", 1717500000000L),
        Review("rev_doc_thane_1_2", "doc_thane_1", "Sujata D.", 5, "Very non-judgmental, warm, and insightful mental health consultant.", 1719500000000L),
        Review("rev_doc_thane_1_3", "doc_thane_1", "Rohan S.", 4, "Clear medication schedule with zero dependency side-effects.", 1721500000000L),

        // doc_thane_2
        Review("rev_doc_thane_2_1", "doc_thane_2", "Pankaj T.", 5, "Dr. Shalini treated my chronic throat infection effectively. Professional doctor.", 1717600000000L),
        Review("rev_doc_thane_2_2", "doc_thane_2", "Rekha K.", 4, "Great ENT care for my daughter's adenoid issue without unnecessary surgery.", 1719600000000L),
        Review("rev_doc_thane_2_3", "doc_thane_2", "Omkar P.", 4, "Good clinic setup and endoscopic examination was smooth.", 1721600000000L),

        // doc_thane_3
        Review("rev_doc_thane_3_1", "doc_thane_3", "Bhupendra J.", 5, "Dr. Vivek Agarwal performed my father's colon resection flawlessly. Remarkable surgeon.", 1717700000000L),
        Review("rev_doc_thane_3_2", "doc_thane_3", "Savitri N.", 4, "Explains oncological staging with immense clarity and optimism.", 1719700000000L),
        Review("rev_doc_thane_3_3", "doc_thane_3", "Dhiraj S.", 5, "Very experienced cancer surgeon. Gives patient and family great peace of mind.", 1721700000000L),

        // doc_vashi_1
        Review("rev_doc_vashi_1_1", "doc_vashi_1", "Preeti N.", 5, "Dr. Geeta Nambiar managed our premature baby in the NICU with miraculous care.", 1717800000000L),
        Review("rev_doc_vashi_1_2", "doc_vashi_1", "Harish S.", 5, "Navi Mumbai's best pediatrician. Always available when child is unwell.", 1719800000000L),
        Review("rev_doc_vashi_1_3", "doc_vashi_1", "Aparna M.", 4, "Empathetic with parents and very detailed vaccination tracking.", 1721800000000L),

        // doc_vashi_2
        Review("rev_doc_vashi_2_1", "doc_vashi_2", "Megha K.", 5, "Dr. Snehal Patil helped me through high-risk pregnancy. Normal delivery achieved!", 1717900000000L),
        Review("rev_doc_vashi_2_2", "doc_vashi_2", "Rashmi P.", 5, "Supportive gynaecologist who empowers women with clear lifestyle advice.", 1719900000000L),
        Review("rev_doc_vashi_2_3", "doc_vashi_2", "Bhakti T.", 4, "Pleasant doctor with very gentle examination technique.", 1721900000000L),

        // doc_vashi_3
        Review("rev_doc_vashi_3_1", "doc_vashi_3", "Jagdish R.", 5, "Dr. Harish Bhatt examined my diabetic retinopathy. Laser treatment was fast and effective.", 1718000000000L),
        Review("rev_doc_vashi_3_2", "doc_vashi_3", "Sharmila D.", 4, "Accurate eye power test and friendly staff at the eye clinic.", 1720000000000L),
        Review("rev_doc_vashi_3_3", "doc_vashi_3", "Vinay G.", 5, "Very experienced senior eye surgeon in Vashi.", 1722000000000L),

        // doc_borivali_1
        Review("rev_doc_borivali_1_1", "doc_borivali_1", "Nisha S.", 5, "Dr. Ritu Trivedi put clear aligners for my teeth. Fantastic alignment in 8 months.", 1718100000000L),
        Review("rev_doc_borivali_1_2", "doc_borivali_1", "Chirag M.", 4, "Modern dental clinic on SV Road. Very hygienic and transparent pricing.", 1720100000000L),
        Review("rev_doc_borivali_1_3", "doc_borivali_1", "Bhavik P.", 4, "Gentle braces adjustment and friendly doctor.", 1722100000000L),

        // doc_borivali_2
        Review("rev_doc_borivali_2_1", "doc_borivali_2", "Suresh P.", 5, "Dr. Sameer Khan resolved my chronic neuropathy pain. Very systematic neurologist.", 1718200000000L),
        Review("rev_doc_borivali_2_2", "doc_borivali_2", "Manjula D.", 5, "Excellent doctor for vertigo and dizziness diagnosis. Recommended!", 1720200000000L),
        Review("rev_doc_borivali_2_3", "doc_borivali_2", "Zaid Q.", 4, "Patient listener, explains brain imaging thoroughly.", 1722200000000L),

        // doc_borivali_3
        Review("rev_doc_borivali_3_1", "doc_borivali_3", "Komal J.", 5, "Dr. Deepa Sunder's counseling helped me recover from acute work stress and insomnia.", 1718300000000L),
        Review("rev_doc_borivali_3_2", "doc_borivali_3", "Siddharth K.", 4, "Very supportive and safe environment to discuss mental wellness.", 1720300000000L),
        Review("rev_doc_borivali_3_3", "doc_borivali_3", "Tina B.", 5, "Compassionate psychiatrist with holistic wellness emphasis.", 1722300000000L)
    )

    fun hospitals(): List<Hospital> = hospitalsList

    fun doctors(): List<Doctor> = doctorsList

    fun reviews(): List<Review> = reviewsList
}
