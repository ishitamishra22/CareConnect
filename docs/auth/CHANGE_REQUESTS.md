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
