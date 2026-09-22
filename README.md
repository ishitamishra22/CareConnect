# CareConnect - Hospital Appointment Booking & Healthcare Document Manager

CareConnect is an Android application developed for the university Mobile Application Development course project (SVKM's NMIMS, MBA-Tech Computer Engineering, Semester V). CareConnect brings every part of a patient's healthcare journey—medical history records, document storage, hospital and doctor discovery, appointment and lab test booking, and hospital admission—into a unified platform, allowing patient medical portfolios to be reused automatically so forms are never filled repeatedly.

## Tech Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3 (Compose BOM 2024.12.01)
- **Architecture**: Single Activity (`MainActivity`), custom back stack navigation (`AppNavController` + sealed `Screen` hierarchy), in-memory session (`AppSession`)
- **Persistence**: Android Internal File Storage (`context.filesDir`) with standard `org.json` serialization (Zero external DB, zero networking libraries)
- **Image Handling**: Native Android `BitmapFactory` with sample size downscaling and internal app document storage
- **Compatibility**: Android SDK 24+ (Android 7.0 Nougat to Android 15, compileSdk 35)

## Modules & Ownership
CareConnect is divided into 4 cohesive modules developed across project phases:

| Module | Scope | Status | Owner |
| :--- | :--- | :--- | :--- |
| **Module 1** | **Medical Portfolio Management & Foundation** (Profile, Medical Records, Documents, Sharing Summary) | Completed (Phase 1) | Person 1 |
| **Module 2** | **Hospital & Doctor Search and Comparison** (Discovery, Specialties, Filtering, Doctor Profiles, Comparison Table) | Completed (Phase 2) | Person 2 |
| **Module 3** | **OPD Appointment & Test Booking** (Slot Selection, Lab Tests, Booking Management, Rescheduling) | Upcoming (Phase 3) | Person 3 |
| **Module 4** | **Automated Hospital Admission & Admin Dashboard** (Direct Admission, Bed Allotment, Admin Analytics) | Upcoming (Phase 4) | Person 4 |

## Project Team
- **Course**: Mobile Application Development (702AI0E002)
- **Institution**: SVKM's NMIMS, Mukesh Patel School of Technology Management & Engineering (MPSTME)
- **Program**: MBA-Tech Computer Engineering, Semester V, Batch D2

| Team Member | Role | Module | Email |
| :--- | :--- | :--- | :--- |
| **Person 1** | Project Foundation & Module 1 Lead | Medical Portfolio Management | ISHITA.MISHRA30@nmims.in |
| **Person 2** | Module 2 Developer | Hospital & Doctor Search | mangluniakrtanish@gmail.com |
| **Teammate 3** | Module 3 Developer | OPD Appointment & Test Booking | teammate3@nmims.edu |
| **Teammate 4** | Module 4 Developer | Automated Hospital Admission & Admin | teammate4@nmims.edu |

## How to Run
1. Clone this repository:
   ```bash
   git clone https://github.com/ishitamishra22/CareConnect.git
   ```
2. Open the project root in **Android Studio** (Ladybug / Koala / Hedgehog or newer).
3. Allow Gradle to sync dependencies automatically using Gradle Wrapper 8.9.
4. Select an Android Emulator or physical device running **API 24 (Android 7.0) or higher**.
5. Click **Run 'app'** (`Shift + F10`).
