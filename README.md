# Rent

Android app (Kotlin, Jetpack Compose, Room/SQLite) for tracking tenants and monthly rent.
Three tabs: **Home**, **Tenants**, **Ledger**. Mac OS Aqua look. All data stays on the phone.

## Build the APK on GitHub (no Android Studio needed)

1. **Create a repository.** On github.com click **New repository**, name it `Rent`, leave it empty
   (no README), and click **Create repository**.
2. **Upload the project.** Unzip `Rent.zip`. Then either:
   - *Command line* (inside the unzipped `Rent` folder):
     ```
     git init
     git add .
     git commit -m "Rent app"
     git branch -M main
     git remote add origin https://github.com/<your-username>/Rent.git
     git push -u origin main
     ```
   - *Browser*: on the empty repo page click **uploading an existing file** and drag in the
     *contents* of the `Rent` folder (`app`, `build.gradle.kts`, `settings.gradle.kts`, ...).
     The `.github` folder starts with a dot and may be hidden by your file manager; if it did not
     upload, click **Add file → Create new file**, type `.github/workflows/build-apk.yml` as the name
     and paste the contents of that file from the zip.
3. **Run the build.** Pushing to `main` starts it automatically. Open the **Actions** tab, click the
   **Build Rent APK** run and wait for the green tick (about 4-6 minutes the first time).
   To re-run manually: Actions → Build Rent APK → **Run workflow**.
4. **Download the APK.** Open the finished run, scroll to **Artifacts** and click
   **Rent-debug-apk**. It downloads as a zip; unzip it to get `app-debug.apk`.
5. **Install on your phone.** Copy `app-debug.apk` to the phone (USB, Drive, WhatsApp to self ...),
   tap it, and allow **Install unknown apps** for the app you opened it from when asked.
   The debug APK is signed automatically, so it installs directly.

If the build fails, open the failed step in the Actions log and send me the red error lines.

## How the month rollover works

Each time the app opens (or resumes, or once a minute while open) it checks whether a month has
ended since the last check. For every tenant whose rent was not recorded in that month, a red
button is added under **Arrears**. Paid (grey) buttons become blue again for the new month.
Paying an arrears button writes a Ledger row for the month it was owed and removes the button.

## Tables

`Tenant`, `TenantsArchive`, `Ledger`, `ActivityLog` (plus an internal `Arrear` table behind the
red buttons).
