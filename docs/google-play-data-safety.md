# Google Play Data Safety Answers

App: `FaceShot-BuildingBlock`  
Package: `com.n0tez.app`  
Prepared: `2026-05-03`

This file records the Play Console Data safety answers that match the current Android codebase and SDK usage.

## High-Level Answers

- Does the app collect or share any required user data types: `Yes`
- Is all collected data encrypted in transit: `Yes`
- Do you provide a way for users to request deletion of their data: `Yes`
- Privacy policy URL: `https://applesaucetoaboss1413.github.io/FaceShot-BuildingBlock-Formerly-known-as-n0tez-/privacy-policy.html`

## Why The Answer Is "Yes"

The app initializes Firebase Crashlytics in `app/src/main/java/com/n0tez/app/N0tezApplication.kt` and explicitly enables crash collection. The app does not initialize Firebase Analytics, authentication, ads, location, contacts, or payments.

Local-only user content such as notes, edited photos, edited videos, recordings, and captured text is stored on device for app functionality. That local storage does not need to be declared as off-device collection in the Data safety form unless it is transmitted off the device.

## Data Types To Declare

### 1. App info and performance

- Crash logs: `Collected`
- Diagnostics: `Collected`
- Shared: `No`
- Processed ephemerally: `No`
- Required for app to function: `No`
- Collection optional: `No`
- Purpose: `Analytics`

Reasoning:
- Firebase Crashlytics automatically collects crash reports and diagnostics for stability monitoring.

### 2. Device or other IDs

- Device or other IDs: `Collected`
- Shared: `No`
- Processed ephemerally: `No`
- Required for app to function: `No`
- Collection optional: `No`
- Purpose: `Analytics`

Reasoning:
- Firebase Crashlytics generates and stores a Crashlytics installation UUID and collects device metadata relevant to crash reporting.

## Data Types Not Declared As Collected Or Shared

Based on the current workspace, do not mark these as collected or shared in Play Console unless the implementation changes:

- Name
- Email address
- User IDs tied to an account
- Address
- Phone number
- Financial info
- Health or fitness data
- Messages
- Photos and videos
- Audio files
- Files and docs
- Calendar
- Contacts
- Location
- Web browsing
- App interactions for analytics SDKs
- Advertising ID

Notes:
- The app accesses photos, videos, audio, microphone, camera, overlay permission, and accessibility service data for user-facing functionality, but the current code does not send that content to a developer backend.
- User-initiated exports or shares do not count as "sharing" for the Data safety section when initiated by the user and reasonably expected.
- Service-provider processing by Firebase Crashlytics does not need to be declared as "shared" for the Play Data safety section.

## Evidence In Code

- Crashlytics init: `app/src/main/java/com/n0tez/app/N0tezApplication.kt`
- API 35 target: `app/build.gradle`
- AAB build in CI: `.github/workflows/android-build.yml`
- In-app privacy policy link: `app/src/main/java/com/n0tez/app/AboutActivity.kt`
- Public privacy policy page source: `docs/privacy-policy.html`

## Deletion Request Handling

Use the contact mechanism in the privacy policy:

- `https://github.com/applesaucetoaboss1413/FaceShot-BuildingBlock-Formerly-known-as-n0tez-/issues`

If this app later adds backend sync, sign-in, analytics, ads, or remote media upload, this form must be updated before the next Play submission.
