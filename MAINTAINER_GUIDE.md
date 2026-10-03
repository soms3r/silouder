# Silouder — Master Project Deployment, Maintenance & Release Guide

This document is the master operational guide for **Silouder** (`https://github.com/soms3r/silouder`). It covers everything needed to manage, build, deploy, and distribute the project across GitHub, GitHub Codespaces, GitHub Pages, and F-Droid.

---

## 📁 Workspace Folder Structure

The project root consists strictly of two purpose-built workspaces:

| Folder / File | Purpose | Description |
|---|---|---|
| **`silouder_app/`** | **Android App Workspace** | The primary development project. Open this folder directly in Android Studio to build, test, and debug on physical devices or emulators. |
| **`silouder_github/`** | **GitHub Repository Staging** | The clean, version-controlled repository folder mirroring `https://github.com/soms3r/silouder`. Contains CI/CD workflows, docs, website, and clean source code. |
| **`silouder_github_ready.zip`** | **Release & Upload Bundle** | Pre-packaged, production-ready ZIP archive ready for 1-click upload to GitHub Codespaces or GitHub Web. |

---

## Table of Contents

1. [Uploading Code via GitHub Codespaces Using ZIP Archive](#1-uploading-code-via-github-codespaces-using-zip-archive)
2. [Uploading Code via Direct Local Git CLI](#2-uploading-code-via-direct-local-git-cli)
3. [Building APKs with Android Studio (Debug & Signed Release)](#3-building-apks-with-android-studio-debug--signed-release)
4. [Publishing Silouder to F-Droid](#4-publishing-silouder-to-f-droid)
5. [Enabling & Configuring GitHub Pages](#5-enabling--configuring-github-pages)
6. [Setting Up Repository Social Preview (OG Image) on GitHub](#6-setting-up-repository-social-preview-og-image-on-github)
7. [Creating Tagged Releases via GitHub Actions](#7-creating-tagged-releases-via-github-actions)
8. [App Security Architecture & Privacy Guarantee](#8-app-security-architecture--privacy-guarantee)
9. [Repository Tree & Parity](#9-repository-tree--parity)

---

## 1. Uploading Code via GitHub Codespaces Using ZIP Archive

If you do not have Git or Android Studio configured locally, GitHub Codespaces provides an instant, high-speed cloud development environment.

### Step-by-Step Walkthrough

1. **Open Your Repository in Codespaces**:
   - Go to [https://github.com/soms3r/silouder](https://github.com/soms3r/silouder).
   - Click the green **`<> Code`** button.
   - Switch to the **Codespaces** tab and click **Create codespace on main**.
   - Wait a few moments for the browser-based VS Code environment to load.

2. **Upload Your ZIP Archive**:
   - In the left-hand Explorer sidebar of Codespaces, drag and drop `silouder_github_ready.zip` into the file tree.

3. **Extract and Overwrite Files via Terminal**:
   - Open the built-in terminal (`Ctrl + ~` or Terminal &rarr; New Terminal).
   - Run the following commands:

   ```bash
   # 1. Unpack the zip directly into the repository root
   unzip -o silouder_github_ready.zip -d .

   # 2. Clean up the zip file
   rm -f silouder_github_ready.zip

   # 3. Ensure the Gradle wrapper has executable permissions
   chmod +x gradlew
   ```

4. **Verify, Commit, and Push**:
   ```bash
   # Check status of modified and new files
   git status

   # Stage all changes
   git add .

   # Commit
   git commit -m "feat: production release with full messaging, P2P calling and verified transports"

   # Push to GitHub
   git push origin main
   ```

---

## 2. Uploading Code via Direct Local Git CLI

If you have Git installed on your computer, you can push directly from `silouder_github/`:

```bash
# Navigate to the GitHub repo folder
cd "d:\Vibe code\Silouder_app\silouder_github"

# Initialize git if not already initialized
git init

# Add remote
git remote add origin https://github.com/soms3r/silouder.git

# Switch to main branch
git branch -M main

# Stage and commit
git add .
git commit -m "feat: production release with full messaging, P2P calling and verified transports"

# Push to GitHub
git push -u origin main
```

---

## 3. Building APKs with Android Studio (Debug & Signed Release)

### Prerequisites
- **Android Studio**: Ladybug (2024.2+) or newer.
- **JDK**: Version 21 (bundled with Android Studio or Eclipse Temurin 21).
- **Android SDK**: API 36 (Android 16 Developer Preview) with Build-Tools `36.0.0`.

### A. Building a Debug APK
1. Open Android Studio &rarr; **Open** &rarr; Select `silouder_app`.
2. Allow Gradle sync to complete.
3. From the menu: **`Build` &rarr; `Build Bundle(s) / APK(s)` &rarr; `Build APK(s)`**.
4. The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

*Alternatively, from the command line:*
```bash
./gradlew assembleDebug
```

---

### B. Building a Signed Release APK
1. In Android Studio: **`Build` &rarr; `Generate Signed Bundle / APK...`**.
2. Select **APK** &rarr; click **Next**.
3. Choose or create a keystore (e.g. `silouder-release.jks` with alias `upload`).
4. Select **release** as the Build Variant.
5. Check **V1 (Jar Signature)** and **V2 (Full APK Signature)** &rarr; click **Finish**.
6. The signed APK will be output to: `app/release/app-release.apk`.

*Or build via command line using environment variables:*
```bash
KEYSTORE_PATH="/path/to/keystore.jks" \
STORE_PASSWORD="password" \
KEY_ALIAS="upload" \
KEY_PASSWORD="password" \
./gradlew assembleRelease
```

---

## 4. Publishing Silouder to F-Droid

Silouder is 100% compliant with F-Droid inclusion policies:
- Free software under the **GNU General Public License v3.0**.
- **Zero proprietary dependencies** (no Google Play Services, Firebase, or closed-source SDKs).
- `dependenciesInfo.includeInApk = false` prevents proprietary Play Store tracking blobs.

### Step 1: Create Fastlane Metadata (Optional but Recommended)
```
fastlane/
└── metadata/
    └── android/
        └── en-US/
            ├── title.txt                  # "Silouder"
            ├── short_description.txt      # Max 80 characters
            ├── full_description.txt       # Markdown description
            ├── images/
            │   ├── icon.png               # 512x512 app icon
            │   └── phoneScreenshots/      # 2 to 8 PNG screenshots
            └── changelogs/
                └── 1.txt                  # Changelog for versionCode 1
```

### Step 2: Prepare F-Droid Recipe (`metadata/com.silouder.app.yml`)
1. Fork [https://gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. Create `metadata/com.silouder.app.yml` with:

```yaml
Categories:
  - Connectivity
  - Security
License: GPL-3.0-or-later
AuthorName: Somser Ali
SourceCode: https://github.com/soms3r/silouder
IssueTracker: https://github.com/soms3r/silouder/issues
WebSite: https://soms3r.github.io/silouder/

AutoName: Silouder
Summary: Offline-first multi-transport mesh communicator
Description: |-
  Silouder is a decentralized, zero-infrastructure offline Android communicator
  exploring resilient messaging when cellular networks and the internet are unavailable.

  Features:
  * Proximity Bluetooth P2P sync (Briar-inspired gossip vectors)
  * Local LAN / direct IP sockets over Wi-Fi and hotspots (port 8888)
  * LoRa mesh radio integration (Meshtastic SX1262 modems over BLE)
  * Tor v3 onion session routing
  * Real E2EE cryptographic core (AES-256-GCM AEAD + HMAC-SHA256)
  * P2P voice calling, walkie-talkie (PTT), and encrypted file sharing
  * 100% free and open source with zero telemetry or tracking

RepoType: git
Repo: https://github.com/soms3r/silouder.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: v1.0.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

3. Submit a Merge Request (MR) to `fdroid/fdroiddata`. Once approved, Silouder will be built automatically by F-Droid servers.

---

## 5. Enabling & Configuring GitHub Pages

The project website is pre-built in [`docs/`](docs/).

1. Navigate to your repository settings:
   [https://github.com/soms3r/silouder/settings/pages](https://github.com/soms3r/silouder/settings/pages)
2. Under **Build and deployment**:
   - **Source**: Select **GitHub Actions** *(Uses our pre-configured [`.github/workflows/pages.yml`](.github/workflows/pages.yml))*.
   - *(Alternative Classic Mode)*: Select **Deploy from a branch** &rarr; Branch: **`main`** &rarr; Folder: **`/docs`** &rarr; **Save**.
3. Within 1–2 minutes, your website is live at:
   **[https://soms3r.github.io/silouder/](https://soms3r.github.io/silouder/)**

---

## 6. Setting Up Repository Social Preview (OG Image) on GitHub

When you share your repository on Twitter/X, Discord, Reddit, or Telegram, an OpenGraph preview card is displayed.

1. Open [https://github.com/soms3r/silouder/settings](https://github.com/soms3r/silouder/settings).
2. Under the **General** tab, scroll to **"Social preview"**.
3. Click **Edit** &rarr; **Upload an image...**.
4. Upload `docs/assets/og-image.svg` (or export it to PNG).
5. GitHub will now display this branded card across social platforms.

---

## 7. Creating Tagged Releases via GitHub Actions

[`.github/workflows/build.yml`](.github/workflows/build.yml) automatically builds and publishes a release whenever a version tag is pushed:

```bash
# Ensure you are on the latest main branch
git checkout main
git pull origin main

# Create an annotated git tag
git tag -a v1.0.0 -m "Release v1.0.0: Silouder Multi-Transport Mesh Communicator"

# Push the tag to GitHub
git push origin v1.0.0
```

GitHub Actions will automatically compile `app-debug.apk` and attach it to:
**[https://github.com/soms3r/silouder/releases](https://github.com/soms3r/silouder/releases)**

---

## 8. App Security Architecture & Privacy Guarantee

- **E2EE Core**: AES-256-GCM AEAD encryption with unique 96-bit random nonces and 128-bit authentication tags on every message envelope.
- **Direct P2P Sockets**: Embedded TCP server on port 8888; voice calls stream directly via 16kHz PCM on UDP port 8899 without central relays.
- **OS Sandboxing**: `android:allowBackup="false"` prevents cloud/ADB backup extraction; attachments are isolated in `context.filesDir/attachments`.
- **Zero Location Harvesting**: `neverForLocation` flag ensures Bluetooth LE scanning is exclusively used for peer proximity discovery.
- **Zero Telemetry**: Completely devoid of Google Play Services, Firebase, trackers, or analytics SDKs.

---

## 9. Repository Tree & Parity

```
silouder/
├── .github/
│   └── workflows/
│       ├── build.yml               # Automated Android build & GitHub Release CI
│       └── pages.yml               # Automated GitHub Pages deployment
├── app/
│   ├── build.gradle.kts            # App dependencies, SDK targets, signing rules
│   ├── proguard-rules.pro          # Keep rules for Room, OkHttp, Models
│   └── src/
│       ├── main/                   # Kotlin source, Compose UI, Room DB, Assets
│       ├── test/                   # JVM unit tests
│       └── androidTest/            # Instrumentation tests
├── docs/                           # Official GitHub Pages Website
│   ├── assets/
│   │   └── og-image.svg            # OpenGraph Social Preview Vector Card
│   ├── css/
│   │   └── style.css               # Tactical cyber & everyday use styling
│   ├── js/
│   │   └── main.js                 # Dynamic release link handler & packet simulator
│   └── index.html                  # Landing page with everyday & tactical features
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar      # Gradle binary
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml          # Centralized version catalog
├── CONTRIBUTING.md                 # Contribution guidelines
├── LICENSE                         # GNU General Public License v3.0
├── MAINTAINER_GUIDE.md             # This comprehensive operational guide
├── README.md                       # Project overview, badges & documentation
├── build.gradle.kts                # Root project build file
├── gradle.properties               # Memory & JVM args
├── gradlew                         # Linux/macOS wrapper script
├── gradlew.bat                     # Windows wrapper script
└── settings.gradle.kts             # Plugin repositories and module includes
```

---

*For bug reports, pull requests, or questions, visit [https://github.com/soms3r/silouder](https://github.com/soms3r/silouder).*
