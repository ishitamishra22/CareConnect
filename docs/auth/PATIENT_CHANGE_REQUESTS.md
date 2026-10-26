# CareConnect Authentication Change Requests & Audit Log

This document records architectural clarifications, migrations, and compatibility decisions made during the implementation of the CareConnect Shared Authentication Core.

---

## 1. Role Migration: `HOSPITAL_ADMIN` -> `HOSPITAL` and `ADMIN`
- **Context**: The original codebase defined `UserRole` as `enum class UserRole { PATIENT, DOCTOR, HOSPITAL_ADMIN }`. The shared authentication specification defines standard roles as `ADMIN`, `PATIENT`, `DOCTOR`, and `HOSPITAL`.
- **Decision & Resolution**:
  - `UserRole` enumerates:
    - `ADMIN("Admin")`
    - `PATIENT("Patient")`
    - `DOCTOR("Doctor")`
    - `HOSPITAL("Hospital Admin")`
  - A compatibility alias `val HOSPITAL_ADMIN = HOSPITAL` is preserved so existing screens (`AdminHomeScreen`, `AdminAdmissionReviewScreen`, `AdmissionDetailScreen`) compile without breaking changes.
  - Serialization to Firestore parses both `"HOSPITAL"` and `"HOSPITAL_ADMIN"` as `UserRole.HOSPITAL`.

---

## 2. Anonymous Authentication Decommissioning & Credential Sign-In
- **Context**: Previous releases used `auth.signInAnonymously()` on first load without credentials.
- **Decision & Resolution**:
  - Removed anonymous sign-in from `FirebaseProvider`.
  - Replaced with credential-based authentication via `AuthRepository.signInWithEmail`.
  - Public registration was stripped from `LoginScreen.kt` to preserve Tanishq's ownership of patient registration and prevent public creation of Doctor, Hospital, or Admin accounts.
  - Inactive users (`active == false`) fail closed into `AuthState.Blocked`.

---

## 3. Strict Privilege Escalation & Medical Privacy Lockdown
- **Context**: Audit identified that previous Firestore rules allowed any user to self-assign `role: 'ADMIN'` and allowed Admins access to patient medical records.
- **Decision & Resolution**:
  - `firestore.rules` now strictly forbids clients from creating accounts with `role: "ADMIN"`, `"DOCTOR"`, or `"HOSPITAL"`.
  - Users are strictly forbidden from modifying `role`, `active`, `linkedDoctorId`, or `linkedHospitalId` during updates.
  - Removed `isAdmin()` from `patients/{patientId}/records` and `patients/{patientId}/documents`. Confidential medical records are strictly reserved for the patient owner and treating doctors.
  - Storage rules (`firebase/storage.rules`) restrict patient medical document access strictly to the owning patient.

---

## 4. Shared API & Model Alignment
- **Decision & Resolution**:
  - `UserProfile` fields aligned to the contract: `uid`, `role`, `email`, `phone`, `displayName`, `active`, `linkedDoctorId`, `linkedHospitalId`, `createdAt`, `createdBy`, `lastLoginAt`.
  - Backward compatibility getters (`doctorId`, `hospitalId`, `phoneNumber`, `isApproved`) are provided to maintain compatibility with existing legacy screens.
  - `AuthRepository` provides the agreed methods: `signInWithEmail`, `signOut`, `sendPasswordReset`, `reauthenticate`, `changePassword`, and `loadProfile`.

---

## 5. Part 2 — System Administrator Architecture & Security Lockdown
- **Context**: Part 2 requires full System Administrator portal capabilities while maintaining zero-trust boundaries, medical privacy, and preventing self-privilege modification.
- **Decision & Resolution**:
  - **Inactivity Timeout**: 15-minute inactivity watchdog implemented in `SystemAdminDashboard`.
  - **Non-Disturbing Staff Provisioning**: `AdminRepository.provisionStaffAccount` uses an isolated secondary `FirebaseApp` instance (`FirebaseApp.initializeApp(context, options, name)`) to provision DOCTOR and HOSPITAL accounts. This guarantees the currently active Administrator session is never signed out or replaced.
  - **Zero Credential Exposure**: Temporary passwords are generated in memory and never displayed, stored, or logged. Accounts receive a secure password reset link upon creation.
  - **Safety Guards**:
    - Admins cannot deactivate their own account or modify another Administrator's account.
    - Admins cannot alter account roles or delete user profiles via the client UI.
  - **Audit Logging**: All administrative operations (status changes, profile updates, password resets, staff provisioning) write immutable audit logs to `adminAuditLogs/{logId}`.
  - **Medical Privacy**: `isAdmin()` is strictly barred from patient medical records (`records`), clinical documents (`documents`), admissions, and bookings.


---

## 6. Part 3 — Patient Authentication Architecture & Domain Integration
- **Context**: Implementation of production-style Patient Authentication (`feature/auth-patient`) while strictly protecting Ishita's Shared Authentication Core and Admin domain.
- **Architectural Implementation & Decisions**:
  1. **Strict Non-Privilege Role Hardcoding**:
     - `AuthRepository.signUpPatient` strictly assigns `role = UserRole.PATIENT`.
     - Patient registration explicitly sets `linkedDoctorId = null`, `linkedHospitalId = null`, `active = true`, `createdBy = "self"`.
     - Matches `firebase/firestore.rules` Section A create-rule for `users/{userId}` without requiring any rule modifications or privilege elevation.
  2. **Indian Mobile Number Support & Formatting**:
     - `AuthValidators.validateIndianMobile` validates 10-digit Indian numbers starting with 6, 7, 8, 9 with optional `+91` or `91` prefixes.
     - `AuthValidators.formatIndianMobile` canonicalizes valid numbers to E.164-compatible `+91XXXXXXXXXX`.
     - Stored under `phone` in `users/{uid}` and `patients/{uid}` for hospital bookings, admissions, and SMS communications.
  3. **Mobile/Password vs Email/Password Login Decision**:
     - In Firebase Authentication, email/password is the primary credential provider.
     - Unauthenticated phone lookups against Firestore are strictly prevented by `firestore.rules` line 44 (`allow read: if isAuthenticated() && (request.auth.uid == userId || isAdmin())`), which ensures patient phone numbers and emails cannot be enumerated or scraped by unauthenticated attackers.
     - Therefore, email/password remains the secure primary sign-in credential, with verified phone numbers permanently bound to the user's healthcare profile.
  4. **Email Verification Lifecycle**:
     - Upon patient signup, Firebase Auth dispatches an initial email verification link.
     - A dedicated `EmailVerificationScreen` allows checking status (`user.reload()`), resending links with a 30-second cooldown timer, or proceeding to the dashboard.
     - `PatientHomeScreen` renders a non-intrusive verification reminder banner if the email is unverified, with a direct 1-tap verification shortcut.
  5. **Medical Records & Portfolio UID Binding**:
     - Upon login or registration, `AppSession.currentPatientId = user.uid` and `AppSession.currentUserName = user.displayName`.
     - Directly binds `PortfolioRepository`, `BookingRepository`, and `AdmissionRepository` to the authenticated Firebase UID.
     - Ensures full zero-trust privacy: each patient only accesses their own medical history, documents, and bookings under `/patients/{uid}` and `/bookings`.
  6. **In-App Account Security & Password Management**:
     - A secure "Change Password" dialog in `PatientHomeScreen` allows authenticated patients to update their credentials using `AuthRepository.changePassword` (requiring current password reauthentication first).
     - "Forgot Password?" dialog in `LoginScreen` sends secure password reset emails via `AuthRepository.sendPasswordReset`.
