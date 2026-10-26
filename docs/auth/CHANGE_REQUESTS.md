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

## 6. Batch 1 — Doctor & Hospital Staff Pending Self-Registration
- **Context**: Doctors and Hospital Staff need registration endpoints accessible from their respective login screens while preventing unverified role activation or privilege self-assignment.
- **Decision & Resolution**:
  - Added `DoctorSignupScreen` and `HospitalStaffSignupScreen` accessible via "Apply for Account" links on `LoginScreen`.
  - Added `ApprovalPendingScreen` to display clear verification and pending-status messaging to applicants.
  - Added pending applicant methods `registerDoctorPending` and `registerHospitalStaffPending` in `AuthRepository`:
    - Dispatches Firebase Authentication user creation.
    - Sends email verification via `firebaseUser.sendEmailVerification()`.
    - Creates profile with `active = false` (blocked from dashboard access until Admin reviews and activates).
    - Stores `createdBy = "self_registration"`, `linkedDoctorId = null`, and `linkedHospitalId = null`.
    - Immediately signs the applicant out to prevent unapproved session elevation.
    - Updated `signInWithEmail` to show an informative "pending approval" message for unapproved staff applicants rather than a generic blocked notification.
  - Navigation integrated cleanly via `Screen.DoctorSignup`, `Screen.HospitalStaffSignup`, and `Screen.ApprovalPending`.


