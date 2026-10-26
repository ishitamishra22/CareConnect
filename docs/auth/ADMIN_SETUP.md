# CareConnect System Admin Bootstrap & Setup Guide

This document outlines the secure procedure for bootstrapping initial System Administrator accounts in CareConnect.

---

## 1. Security Overview
To maintain zero-trust security and prevent privilege escalation:
- There is **no public Admin registration** and **no UI endpoint** to create or elevate an Admin account within the client app.
- System Admin accounts must be bootstrapped out-of-band directly through the **Firebase Console** or authorized backend scripts with service account privileges.
- Admin credentials must **never** be hardcoded in application source code, repositories, logs, or local storage.

---

## 2. Bootstrapping a System Admin Account

### Step 1: Create the Firebase Auth User
1. Open the [Firebase Console](https://console.firebase.google.com/).
2. Select your CareConnect Firebase project.
3. In the left navigation, go to **Build > Authentication > Users**.
4. Click **Add user**.
5. Enter the administrator's designated institutional email address (e.g., `admin@careconnect.nmims.edu`) and generate a strong initial password.
6. Copy the generated **User UID** (e.g., `ADM_7x92KpqL...`).

### Step 2: Provision the Firestore Profile Document
1. In the Firebase Console, go to **Build > Firestore Database**.
2. Navigate to the `users` collection (or start the collection if not yet created).
3. Click **Add document** and set the **Document ID** to the exact **User UID** copied from Step 1.
4. Populate the document with the following fields:

| Field Name | Type | Value | Notes |
| :--- | :--- | :--- | :--- |
| `uid` | `string` | *(The exact Auth User UID)* | Must match `request.auth.uid` |
| `role` | `string` | `"ADMIN"` | Must be uppercase `"ADMIN"` |
| `email` | `string` | `"admin@careconnect.nmims.edu"` | Administrator's email |
| `displayName` | `string` | `"System Administrator"` | Admin staff name |
| `active` | `boolean` | `true` | Inactive accounts fail closed |
| `linkedDoctorId` | `null` | `null` | Reserved for doctor profiles |
| `linkedHospitalId` | `null` | `null` | Reserved for hospital admins |
| `createdAt` | `timestamp` | *(Current Server Timestamp)* | Initial creation date |
| `createdBy` | `string` | `"bootstrap"` | Marked as console bootstrap |
| `lastLoginAt` | `timestamp` | *(Current Server Timestamp)* | Updated on sign-in |

5. Click **Save**.

---

## 3. Administrator Security Controls
1. **Inactivity Timeout**: The CareConnect System Admin portal enforces a **15-minute inactivity timeout**, after which the session is terminated and re-authentication is required.
2. **Reauthentication**: Sensitive user modifications (such as deactivating a user account or linking IDs) require entering current credentials if the session is stale.
3. **Immutability of Admin Roles**:
   - `firestore.rules` prohibits clients from updating account roles.
   - Admins cannot deactivate their own account or modify other Admin profiles.
4. **Audit Logging**:
   - Every administrative action is automatically written to `adminAuditLogs/{logId}`.
   - Audit logs are immutable (create-only by Admins; client update/delete is forbidden).
