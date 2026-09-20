# OpticalBillingSQLite

Offline optical shop billing Android application.

## Stack
- Kotlin
- Jetpack Compose
- Native SQLiteOpenHelper
- Android PdfDocument
- MVVM-ready repository structure

## Database
`optical_billing.db`

No Room, Firebase, backend, or internet is required.

## Build
Open the project in Android Studio and allow Gradle sync.

The current starter implementation creates a customer, invoice, invoice item, calculates GST, stores data in SQLite, and generates a PDF invoice.

## GitHub Actions APK Build

Workflow:
`.github/workflows/build-apk.yml`

Push the project to GitHub, then:
1. Open **Actions**.
2. Select **Android APK Build**.
3. Click **Run workflow** for a manual build, or push to `main`/`master`.
4. Download `optical-billing-debug-apk` from the workflow artifacts.

The repository should include the standard Gradle wrapper files (`gradlew`, `gradlew.bat`, and `gradle/wrapper/*`) before running the workflow.
