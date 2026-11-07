# XC Scores — Android

A searchable Android app for the 2026 PIAA cross-country championship results (Girls A/AA/AAA, Boys A/AA/AAA). RunHigh is the source of the race results; this app is not affiliated with RunHigh or PIAA.

## Public app updates

Version 1.3+ checks the public **GitHub Releases** API when the app starts, through **Check for updates**, and periodically via Android WorkManager. When a newer APK is available, the app prompts users or posts a notification. No GitHub account is needed. Android must ask the user to confirm APK installation; the app does not silently install updates.

**Important:** An APK must be signed with the **same signing certificate** as the installed APK for an in-place update. Previous GitHub Actions debug builds used ephemeral debug keys; Android may require uninstalling an older debug build before installing the first production-signed version. Uninstalling removes locally stored app data. Once the permanent release signing key is set up, keep it safe and reuse it for all releases.

## Set up permanent signing (repo owner, one time)

1. Generate a release signing key **on your own computer**, never in the public GitHub repository. You need Java JDK `keytool`:

   ```sh
   keytool -genkeypair -v -keystore xc-release.jks -alias xc_release -keyalg RSA -keysize 3072 -validity 10000
   ```

2. Make a safe, **private offline backup** of `xc-release.jks` and its passwords. Losing the key makes future in-place updates impossible. Do not commit it or share it.
3. In GitHub, visit **Settings → Secrets and variables → Actions → New repository secret**. Set:
   - `XC_KEYSTORE_BASE64` — the Base64 encoding of the **entire .jks file**, without line breaks.
   - `XC_KEYSTORE_PASSWORD` — keystore password.
   - `XC_KEY_ALIAS` — `xc_release`.
   - `XC_KEY_PASSWORD` — alias password (may equal the store password).

   On Windows PowerShell, obtain Base64 using:
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("xc-release.jks"))
   ```
   Copy its output only into the private GitHub Actions secret field.

4. Make the first signed release by creating a new tag `v1.3` **after these secrets exist**. From Git on your computer:
   ```sh
   git tag v1.3
   git push origin v1.3
   ```
   Alternatively, use GitHub's web interface to create and publish a tag `v1.3` at the current main commit. The **Publish signed Android update** workflow builds `XC-Scores.apk` and publishes it as a public release.
5. To publish **v1.4**, bump `versionCode` to `5` and `versionName` to `1.4` in `app/build.gradle`, push those changes, and then tag `v1.4`. Release tags must match the app version, and be of the form `v1.N`.

Anyone can download the resulting APK at https://github.com/GidGalactic/XC-Scores-/releases/latest . Older versions check that URL's GitHub API equivalent automatically.

## CI builds

Regular commits to `main` run **Build Android APK** and produce a *debug* artifact for development testing. The **Publish signed Android update** workflow handles permanent-signed distributable builds. Do **not** share a debug build as the official install if you plan to deliver updates in place.

## Results

The app checks six RunHigh pages directly and caches the last successful raw pages. The athlete table shows name, final time, pace, overall placement, team and grade. Sorting is available by table headings or the sort menu.

Android may defer background checks to save battery. Errors in reading or parsing RunHigh pages do not mean that the race is unpublished.
