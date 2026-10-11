# CareConnect

CareConnect is a native Android healthcare application developed for the university Mobile Application Development course project at SVKM's NMIMS, MPSTME (MBA-Tech Computer Engineering, Semester V). CareConnect unifies every phase of a patient's medical journey—personal medical history, diagnostic report storage, hospital and specialist discovery, appointment and lab test scheduling, automated inpatient hospital admission, and hospital administrative bed management—into a single seamless digital ecosystem.

Built with intelligent health portfolio sharing, CareConnect automatically forwards a patient's health background, chronic conditions, and emergency contacts to appointment bookings and hospital admissions, eliminating repetitive paperwork and administrative delays.

---

## Technical Stack & Architecture

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3 (Compose BOM 2024.12.01)
- **Architecture**: Single Activity (`MainActivity`), custom back stack navigation controller (`AppNavController` with sealed `Screen` hierarchy), in-memory session manager (`AppSession`)
- **Backend & Cloud Persistence**:
  - **Firebase Anonymous Authentication**: Instant, frictionless identity initialization
  - **Cloud Firestore**: Real-time distributed NoSQL database for patients, records, hospitals, doctors, reviews, bookings, and admissions
  - **Firebase Cloud Storage**: Secure cloud storage for medical document scans and image attachments
- **Local Caching & Offline Fallback**: In-memory repository caching and built-in catalogue fallback in `HospitalRepository` for zero-downtime offline viewing
- **Image Optimization**: Custom `FileStorageManager` utilizing downscaled bitmap decoding (`inSampleSize`), JPEG 85 compression, and local caching
- **Compatibility**: Android SDK 24+ (Android 7.0 Nougat to Android 15, `compileSdk` 35, `targetSdk` 35)

---

## Modules & Features Overview

CareConnect is divided into 4 cohesive modules:

### Module 1: Medical Portfolio Management (Person 1)
- **Patient Profile**: Comprehensive demographic and health metrics management (name, age 1–120, gender, blood group, 10-digit phone, address, emergency contact, allergies, chronic conditions, current medications).
- **Medical Records**: Digital timeline of clinical encounters, consultations, prescriptions, and diagnosis notes.
- **Document Management**: Attachment of diagnostic laboratory reports and prescriptions with camera/gallery image picker, compressed thumbnail rendering, and full-screen zoomable viewer.
- **Forwardable Medical Summary**: Automated generation of formatted text summaries (`PortfolioSummary.toShareableText()`) for one-tap sharing with healthcare providers.

### Module 2: Hospital & Doctor Search and Comparison (Person 2)
- **Catalogue & Discovery**: Directory of top partner hospitals and specialist doctors across Mumbai with multi-factor search by name, area, specialty, and facilities.
- **Sorting & Filtering**: Instant sorting by rating, consultation fee, years of experience, and review counts.
- **Detailed Profiles**: Rich hospital detail screens with room categories, 24x7 emergency badges, doctor rosters, and doctor profile screens.
- **Side-by-Side Comparison**: Multi-specialist comparison matrix comparing qualifications, fees, experience, and patient ratings.
- **Ratings & Reviews**: Firestore-backed review submission system with dynamic aggregate rating calculation.

### Module 3: OPD Appointment & Lab Test Booking (Person 3)
- **Doctor Appointment Booking**: OPD specialist appointment scheduling with automated date selection, doctor available-day validation, time slot chips, reason capture, and portfolio consent.
- **Diagnostic Lab Test Booking**: Booking catalog of 15 diagnostic blood tests and scans (CBC, Lipid Profile, MRI, etc.) with fasting instruction dialogs and turnaround estimates.
- **Booking Broadcast Notifications**: Native Android broadcast receiver (`BookingBroadcastReceiver`) firing `com.nmims.careconnect.BOOKING_CONFIRMED` notifications.
- **Booking Management**: Tracking for Upcoming and Past bookings with cancellation capabilities.
- **Doctor Dashboard**: Specialist portal with daily schedule metrics and clinical consultation note logging.

### Module 4: Automated Hospital Admission & Admin Dashboard (Person 4)
- **Four-Step Inpatient Admission Wizard**:
  - *Step 1 (Hospital & Reason)*: Hospital selection from directory, validated admission reason (min 10 characters), optional referring doctor.
  - *Step 2 (Patient Auto-Fill)*: Automatic prefilling from Medical Portfolio with highlighted banner, editable inputs, and missing portfolio gate.
  - *Step 3 (Room & Duration)*: Hospital room category radio picker with INR/day prices, `DatePickerDialog` (today or later), stay duration (1–60 days), live stay cost calculation.
  - *Step 4 (Insurance & Review)*: Health insurance switch with 8–20 alphanumeric policy number validation, validity date check (must exceed admission date), sum insured capture, full summary review, and portfolio forwardable summary display.
- **Duplicate Prevention & Server Error Handling**: Safe Firestore persistence through `AdmissionRepository.add()` with submit-lock states and user-friendly connection error alerts.
- **Live Patient Admissions Tracking**: Real-time observation of admission requests in `MyAdmissionsScreen` via `observeForPatient()`, tracking `PENDING`, `APPROVED`, `REJECTED`, and `DISCHARGED` states with assigned bed numbers.
- **Admission Detail View**: Authorized admission inspection screen with patient ownership validation.
- **Hospital Admin Portal**:
  - *Hospital Admin Login*: Direct hospital selection via `ExposedDropdownMenuBox` during onboarding.
  - *Management Console*: Live statistics cards for Pending Admissions, Approved Admissions, OPD Appointments Today, and Lab Tests Today.
  - *Admissions Tab*: Real-time stream of inpatient admission requests for the administered hospital.
  - *Appointments Tab*: Hospital OPD bookings grouped and sorted by date.
  - *Lab Tests Tab*: Diagnostic scan tracking with "Mark Completed" workflow.
  - *Adjudication & Room Allotment*: Formal approval dialog with room/bed assignment (e.g., "Ward 3, Bed 305-B"), rejection with mandatory remark, and discharge status updates.

---

## Application Screen List

1. `Screen.RoleSelection`: Role picker (Patient, Doctor, Hospital Admin) + link to About screen.
2. `Screen.Login`: Role-tailored onboarding and identity selection (Patient Name, Doctor Directory Picker, Hospital Admin Picker).
3. `Screen.PatientHome`: Patient central dashboard with 6 integrated feature cards.
4. `Screen.Portfolio`: Medical Portfolio overview hub.
5. `Screen.EditProfile`: Patient profile editor with live validation.
6. `Screen.MedicalRecords`: Timeline of medical consultations and diagnoses.
7. `Screen.AddEditRecord`: Create or update clinical history records.
8. `Screen.Documents`: Gallery of uploaded medical reports and prescriptions.
9. `Screen.AddDocument`: Camera and gallery document upload screen.
10. `Screen.DocumentViewer`: High-resolution document viewer with metadata.
11. `Screen.Search`: Hospital and doctor discovery and search with filtering and sorting.
12. `Screen.HospitalDetail`: Hospital facilities, accommodation room rates, and doctor roster.
13. `Screen.DoctorDetail`: Specialist credentials, fees, schedule, and review history.
14. `Screen.Compare`: Side-by-side comparison screen for multiple doctors.
15. `Screen.BookingHub`: Central launchpad for doctor appointments and lab scans.
16. `Screen.BookAppointment`: Specialist OPD appointment booking with slot selection.
17. `Screen.BookLabTest`: Diagnostic test catalog with fasting instructions.
18. `Screen.BookingSuccess`: Appointment confirmation screen with booking reference code.
19. `Screen.MyBookings`: Patient appointment history with Upcoming/Past tabs and cancellation.
20. `Screen.BookingDetail`: Detailed view of a single appointment or lab test booking.
21. `Screen.DoctorHome`: Doctor consultation portal with daily metrics and appointment queues.
22. `Screen.DoctorAppointmentDetail`: Doctor clinical record and consultation notes screen.
23. `Screen.AdmissionForm`: 4-step wizard for automated hospital admission requests.
24. `Screen.AdmissionSuccess`: Admission request confirmation screen with reference code.
25. `Screen.MyAdmissions`: Real-time list of patient admission requests and assigned rooms.
26. `Screen.AdmissionDetail`: Comprehensive view of an admission request and insurance details.
27. `Screen.AdminHome`: Hospital Admin console with live metrics and 3-tab management (Admissions, Appointments, Lab Tests).
28. `Screen.AdminAdmissionReview`: Admin admission review screen with Approve (room assignment), Reject, and Discharge actions.
29. `Screen.About`: Academic course details, team roster, and module breakdown.

---

## Role-Based User Flows

### 1. Patient Flow
```
Role Selection -> Patient Login -> Patient Home
  ├── My Medical Portfolio -> Profile / Records / Documents / Forwardable Summary
  ├── Find Hospitals & Doctors -> Hospital Detail / Doctor Detail / Compare
  ├── Book Appointment / Lab Test -> Booking Hub -> Book OPD or Lab -> Booking Success
  ├── My Bookings -> Upcoming / Past Bookings -> Booking Detail (Cancel Option)
  ├── Hospital Admission -> 4-Step Wizard (Auto-Fill & Room Select) -> Admission Success
  └── My Admissions -> Real-Time Status Stream -> Admission Detail (Assigned Room)
```

### 2. Doctor Flow
```
Role Selection -> Doctor Login (Select Profile from Directory) -> Doctor Dashboard
  ├── Today's Appointments Queue -> Doctor Appointment Detail (Add Clinical Notes & Complete)
  ├── Upcoming Appointments Queue
  └── Completed Appointments Archive
```

### 3. Hospital Admin Flow
```
Role Selection -> Admin Login (Select Managed Hospital & Enter Name) -> Admin Console
  ├── Live Statistics (Pending Admissions, Approved, Today's OPD, Today's Labs)
  ├── Admissions Tab -> Select Request -> Admin Review (Approve & Assign Bed / Reject / Discharge)
  ├── Appointments Tab (Grouped by Date) -> Review Scheduled Doctor Visits
  └── Lab Tests Tab -> Review Diagnostics -> Mark Completed
```

---

## Firebase Configuration & Data Architecture

### Firebase Anonymous Authentication
- Initialization is managed automatically through `FirebaseProvider.ensureSignedIn()`.
- On first launch, an anonymous Firebase authentication session is established (`auth.signInAnonymously()`).
- All subsequent Cloud Firestore and Storage transactions are securely authorized under the active anonymous token.

### Cloud Firestore Collections & Document Models

| Collection Path | Description | Key Fields |
| :--- | :--- | :--- |
| `patients/{patientId}` | Patient profile records | `name`, `age`, `gender`, `bloodGroup`, `phone`, `email`, `address`, `emergencyContactName`, `emergencyContactPhone`, `allergies`, `chronicConditions`, `currentMedications`, `updatedAtMillis` |
| `patients/{patientId}/records/{recordId}` | Medical history subcollection | `id`, `title`, `dateMillis`, `doctorName`, `hospitalName`, `type`, `diagnosis`, `notes`, `prescription` |
| `patients/{patientId}/documents/{docId}` | Medical report metadata | `id`, `title`, `category`, `dateMillis`, `doctorOrLab`, `notes`, `storagePath` |
| `hospitals/{hospitalId}` | Partner hospitals catalogue | `id`, `name`, `area`, `city`, `address`, `phone`, `rating`, `reviewCount`, `specialties`, `facilities`, `emergency24x7`, `roomTypes` (`name`, `pricePerDay`, `description`), `aboutHtml`, `accentColorHex` |
| `doctors/{doctorId}` | Specialists catalogue | `id`, `hospitalId`, `name`, `specialty`, `qualifications`, `experienceYears`, `consultationFee`, `rating`, `reviewCount`, `availableDays`, `availableSlots`, `languages`, `about` |
| `reviews/{reviewId}` | Patient ratings and reviews | `id`, `targetId`, `targetType`, `patientId`, `patientName`, `rating`, `comment`, `dateMillis` |
| `bookings/{bookingId}` | OPD & lab bookings | `id`, `type`, `patientId`, `patientName`, `hospitalId`, `doctorId`, `labTestId`, `dateMillis`, `timeSlot`, `reason`, `sharedPortfolioText`, `status`, `createdAtMillis`, `doctorNote` |
| `slotLocks/{lockId}` | Appointment slot locking | `doctorId`, `dateMillis`, `timeSlot`, `lockedAt` |
| `admissions/{admissionId}` | Inpatient hospital admissions | `id`, `patientId`, `hospitalId`, `patientName`, `patientAge`, `patientGender`, `patientBloodGroup`, `patientPhone`, `patientAddress`, `emergencyContactName`, `emergencyContactPhone`, `reason`, `referringDoctor`, `roomType`, `roomPricePerDay`, `preferredDateMillis`, `expectedStayDays`, `estimatedCost`, `allergies`, `chronicConditions`, `currentMedications`, `sharedPortfolioSummary`, `insuranceDetails` (`providerName`, `policyNumber`, `policyholderName`, `validityDateMillis`, `sumInsured`), `status`, `assignedRoom`, `adminRemark`, `createdAtMillis` |

### Firebase Cloud Storage Structure
- Patient medical document images are stored under:
  ```
  patients/{patientId}/documents/{documentId}.jpg
  ```
- Uploads are compressed via `FileStorageManager.compressToJpegBytes()` to max dimensions of 1280px at 80% JPEG quality prior to storage transfer.

### Catalogue Seeding & Offline Cache
- On cold start, `HospitalRepository.load()` triggers `CatalogueSeeder.seedIfEmpty()`.
- If Firestore collections `hospitals` or `doctors` are empty, they are seeded from `HospitalSeedData.kt`.
- Data is cached in-memory. If offline, the app seamlessly falls back to pre-bundled seed data with zero crashes.

---

## Setup & Execution Guide

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+), Koala, or Hedgehog
- **JDK**: Java 17 or Java 21 LTS
- **Android SDK**: API level 24 through 35

### Firebase Setup Steps
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name: `com.nmims.careconnect`.
3. Download the generated `google-services.json` file.
4. **Place `google-services.json` into the `app/` directory** of this project (`CareConnect/app/google-services.json`).
5. In the Firebase Console, navigate to **Build > Authentication > Sign-in method** and enable **Anonymous Authentication**.
6. Navigate to **Build > Firestore Database** and click **Create Database**.
7. Deploy the security rules located in the repository under:
   - Firestore rules: [`firebase/firestore.rules`](file:///d:/CareConnect-connected/firebase/firestore.rules)
   - Storage rules: [`firebase/storage.rules`](file:///d:/CareConnect-connected/firebase/storage.rules)

### Building and Running the App
1. Open the project in Android Studio.
2. Allow Gradle sync to complete using the wrapper (`gradle-8.9`).
3. Connect an Android device with Developer Mode / USB Debugging enabled, or launch an Android Virtual Device (AVD).
4. Run the application:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Known Limitations & Future Work

- **Demo Role Authentication**: The current release uses lightweight, demo role selection without password or biometric authentication. Production-ready password/OTP authentication is planned for future phases.
- **Payment Gateway Integration**: Estimated costs and insurance summaries are calculated live, but actual financial transactions are modeled as cashless hospital desk settlements.

---

## Project Team & Responsibilities

- **Course**: Mobile Application Development (702AI0E002)
- **Institution**: SVKM's NMIMS, MPSTME
- **Program**: MBA-Tech Computer Engineering, Semester V, Batch D2

| Team Member | Role | Module Responsibility | Contact |
| :--- | :--- | :--- | :--- |
| **Person 1** | Project Lead & Developer | **Module 1**: Medical Portfolio Management & Core Foundation | ISHITA.MISHRA30@nmims.in |
| **Person 2** | Developer | **Module 2**: Hospital & Doctor Search and Comparison | mangluniakrtanish@gmail.com |
| **Person 3** | Developer | **Module 3**: OPD Appointment & Diagnostic Test Booking | atharva.bapardekar@gmail.com |
| **Person 4** | Developer | **Module 4**: Automated Hospital Admission & Admin Dashboard | teammate4@nmims.edu |

---

## Screenshots

*(Screenshots can be placed in `docs/screenshots/`)*

| Screen | Preview |
| :--- | :--- |
| **Patient Dashboard** | `![Patient Dashboard](docs/screenshots/01_patient_home.png)` |
| **Medical Portfolio** | `![Medical Portfolio](docs/screenshots/02_medical_portfolio.png)` |
| **Hospital & Doctor Search** | `![Hospital Search](docs/screenshots/03_search_hospitals.png)` |
| **Doctor Comparison** | `![Doctor Comparison](docs/screenshots/04_compare_doctors.png)` |
| **Book Appointment** | `![Book Appointment](docs/screenshots/05_book_appointment.png)` |
| **Admission Form (Step 1-4)** | `![Admission Form](docs/screenshots/06_admission_form.png)` |
| **Admission Success** | `![Admission Success](docs/screenshots/07_admission_success.png)` |
| **My Admissions** | `![My Admissions](docs/screenshots/08_my_admissions.png)` |
| **Admission Details** | `![Admission Details](docs/screenshots/09_admission_detail.png)` |
| **Hospital Admin Console** | `![Admin Dashboard](docs/screenshots/10_admin_dashboard.png)` |
| **Admin Admission Review** | `![Admin Review](docs/screenshots/11_admin_review.png)` |
