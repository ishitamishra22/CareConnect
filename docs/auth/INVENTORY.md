# CareConnect Authentication Architecture & Shared Core Inventory

## 1. Overview
The **Shared Authentication Core** is the foundational authentication layer for the CareConnect Android application. It provides unified session state, role validation, security boundaries, and Firebase integration across all specialized domains.

---

## 2. Domain & Ownership Boundaries
To maintain clean separation of concerns and prevent merge conflicts across team tracks:
- **Shared Authentication Core**:
  - `UserRole`: Canonical role definitions (`ADMIN`, `PATIENT`, `DOCTOR`, `HOSPITAL`).
  - `UserProfile`: Shared Firestore schema (`users/{uid}`).
  - `AuthRepository`: Authentication lifecycle and Firebase state management.
  - `AuthValidators`: Form and credential validators.
  - `AuthGate`: Declarative Jetpack Compose security gate wrapper.
  - `firestore.rules` & `storage.rules`: Section A core rules, and restrictive temporary rules for Sections B and C.
- **Tanishq's Domain (Patient)**:
  - Patient registration UI, medical portfolio, personal health dashboard (`PatientHomeScreen`), and medical records.
  - The shared core strictly **does not** implement or modify patient self-registration forms, patient home layouts, or portfolio logic.
- **Atharva's Domain (Doctor & Hospital)**:
  - Doctor consultation screen (`DoctorHomeScreen`), clinical appointment reviews, and hospital management.
- **Admin Domain (Part 2 - Pending explicit instruction)**:
  - System administrator dashboard, hospital admission approvals, and staff account provisioning.

---

## 3. Data Architecture: `users/{uid}`
All registered accounts store a corresponding profile document in Firestore:
- `uid`: Unique identifier matching Firebase Auth UID.
- `role`: Canonical role string (`ADMIN`, `PATIENT`, `DOCTOR`, `HOSPITAL`).
- `email`: User email address.
- `phone`: User phone number (nullable).
- `displayName`: User full name or staff title.
- `active`: Boolean flag indicating if the account is in good standing. Inactive/blocked accounts are rejected at login.
- `linkedDoctorId`: Hospital directory doctor reference for DOCTOR role.
- `linkedHospitalId`: Hospital ID reference for HOSPITAL role.
- `createdAt`: Timestamp of account initialization.
- `createdBy`: UID of the creator (`"self"` for patient, or Admin UID for provisioned staff).
- `lastLoginAt`: Timestamp updated on each successful sign-in.

---

## 4. State Machine: `AuthState`
The authentication session is modeled as a 4-state observable flow:
1. `AuthState.Loading`: Authentication or session restoration in progress.
2. `AuthState.SignedOut`: No active session exists.
3. `AuthState.SignedIn(UserProfile)`: User is actively authenticated and their account is `active == true`.
4. `AuthState.Blocked(reason)`: User account is inactive (`active == false`) or suspended; fails closed and prevents access.

---

## 5. Security Enforcements
1. **No Public Role Elevation**:
   - `firestore.rules` prohibits clients from creating accounts with `role: "ADMIN"`, `"DOCTOR"`, or `"HOSPITAL"`.
   - Users cannot modify their own `role`, `active` status, or linked IDs.
2. **Confidentiality of Patient Medical Records**:
   - Administrators do not have read or write access to confidential patient medical records (`records`) or medical documents (`documents`).
   - Only the patient owner and verified treating doctors can access clinical records.
3. **Storage Isolation**:
   - Medical document storage (`documents/{patientId}/*`) is locked strictly to the patient owner.
