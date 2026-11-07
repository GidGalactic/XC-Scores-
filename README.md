# XC Scores — Android

Android app for the 2026 PIAA Cross Country State Championships.

Features: Girls/Boys A, AA, AAA; athlete and team search, ranking and A–Z sorting; direct HTTPS refresh; offline cache; background checks and notifications for newly posted races.

## Download APK
Open **Actions → Build Android APK → latest successful run → Artifacts → XC-Scores-debug-APK**. Download the artifact ZIP, extract `app-debug.apk` and install it on Android. Android may ask you to approve installation from Files/your browser.

## Local build
Open this project in Android Studio (JDK 17), sync Gradle, then **Build → Build APK(s)**.

Background updates use Android WorkManager with a 15-minute minimum cadence; battery-saving controls can delay checks. Source: https://www.runhigh.com/2026RESULTS/R110726AA.html. Not affiliated with RunHigh or PIAA.
