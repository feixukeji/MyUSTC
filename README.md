# MyUSTC

An Android campus companion for students at the University of Science and Technology of China (USTC), bringing academic information and campus services together in one app.

Supports undergraduate and graduate student modes.

## Features

- Undergraduate course timetable synchronization and custom courses
- Exam schedules and notification reminders
- Personal tasks with due dates and completion tracking
- Home screen widgets for courses, exams, and tasks
- Quick access to university email, grades, the library, and other campus services
- Dark mode and custom backgrounds

Requires Android 9.0 (API 28) or later. University services require a USTC account and network access.

## Build

Open the project in Android Studio with JDK 21 and Android SDK Platform 37, then sync Gradle and run the `app` module.

To build a debug APK from the project root:

**Windows:**

```powershell
.\gradlew.bat :app:assembleDebug
```

**macOS / Linux:**

```bash
bash ./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.
