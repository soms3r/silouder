# Silouder — Project Deployment, Maintenance & Release Guide

This document is the master operational guide for **Silouder** (`https://github.com/soms3r/silouder`). It covers everything needed to manage, build, deploy, and distribute the project across GitHub, GitHub Codespaces, GitHub Pages, and F-Droid.

---

## Table of Contents

1. [Uploading Code via GitHub Codespaces Using a ZIP Archive](#1-uploading-code-via-github-codespaces-using-a-zip-archive)
2. [Building APKs with Android Studio (Debug & Signed Release)](#2-building-apks-with-android-studio-debug--signed-release)
3. [Publishing Silouder to F-Droid](#3-publishing-silouder-to-f-droid)
4. [Enabling & Configuring GitHub Pages](#4-enabling--configuring-github-pages)
5. [Setting Up Repository Social Preview (OG Image) on GitHub](#5-setting-up-repository-social-preview-og-image-on-github)
6. [Creating Tagged Releases via GitHub Actions](#6-creating-tagged-releases-via-github-actions)
7. [Repository Parity & Folder Structure](#7-repository-parity--folder-structure)

---

## 1. Uploading Code via GitHub Codespaces Using a ZIP Archive

If you developed or received an updated project as a `.zip` archive (e.g. `silouder_app.zip`) and want to push it directly to your GitHub repository without installing Git or Android Studio locally, GitHub Codespaces provides an instant, high-speed cloud development environment.

### Step-by-Step Walkthrough

1. **Open Your Repository in Codespaces**:
   * Navigate to [https://github.com/soms3r/silouder](https://github.com/soms3r/silouder).
   * Click the green **`<> Code`** button.
   * Switch to the **Codespaces** tab and click **Create codespace on main**.
   * Wait a few seconds for the browser-based VS Code environment to load.

2. **Upload Your ZIP Archive**:
   * In the left-hand Explorer sidebar of Codespaces, simply **drag and drop** your `silouder_app.zip` into the file tree.

3. **Extract and Overwrite Files via Terminal**:
   * Open the built-in terminal (`Ctrl + ~` or Terminal &rarr; New Terminal).
   * Run the following commands:

   ```bash
   # 1. Create a temporary folder and unpack the zip
   mkdir -p /tmp/silouder_unpack
   unzip -o silouder_app.zip -d /tmp/silouder_unpack/

   # 2. Sync unpacked files into your repository root
   # (If your zip contains a nested silouder_app/ folder, copy from inside it)
   if [ -d "/tmp/silouder_unpack/silouder_app" ]; then
     cp -r /tmp/silouder_unpack/silouder_app/* .
   else
     cp -r /tmp/silouder_unpack/* .
   fi

   # 3. Clean up temporary files
   rm -rf /tmp/silouder_unpack silouder_app.zip

   # 4. Ensure the Gradle wrapper has executable permissions
   chmod +x gradlew
   ```

4. **Verify, Commit, and Push**:
   ```bash
   # Check modified and newly added files
   git status

   # Stage all changes
   git add .

   # Commit
   git commit -m "feat: sync latest project files from archive"

   # Push to main branch
   git push origin main
   ```

---

## 2. Building APKs with Android Studio (Debug & Signed Release)

### Prerequisites
* **Android Studio**: Ladybug (2024.2+) or later recommended.
* **JDK**: Version 21 (bundled with modern Android Studio or Eclipse Temurin 21).
* **Android SDK**: API level 36 (Android 16 Developer Preview / Baklava) and Build-Tools `36.0.0`.

### A. Building a Debug APK

1. Launch Android Studio and choose **Open**, then select the project directory (`silouder_app` or `githubrepo`).
2. Allow Gradle sync to complete.
3. From the top menu, click:
   **`Build` &rarr; `Build Bundle(s) / APK(s)` &rarr; `Build APK(s)`**
4. Once completed, a notification bubble will appear with a clickable **"locate"** link.
5. The debug APK is generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

*Alternatively, from the command line:*
```bash
./gradlew assembleDebug
```

---

### B. Building a Signed Release APK

To distribute on F-Droid, GitHub Releases, or for users to install with tamper-proof signatures, generate a signed release APK:

1. In Android Studio, select:
   **`Build` &rarr; `Generate Signed Bundle / APK...`**
2. Choose **APK** and click **Next**.
3. **Key store path**:
   * If you have an existing keystore, click **Choose existing...**.
   * If creating a new one, click **Create new...**:
     * Choose a secure destination (e.g. `~/silouder-release-key.jks`).
     * Set a strong password for both the Keystore and Key.
     * Alias: `upload` or `silouder`.
     * Validity: At least **25 years** (standard requirement for Android).
     * Fill in Certificate Name/Org details.
4. Select **release** as the Build Variant.
5. Ensure signature versions **V1 (Jar Signature)** and **V2 (Full APK Signature)** are checked.
6. Click **Finish**. The output signed APK will be in:
   `app/release/app-release.apk`

*Alternatively, build from CI using environment variables:*
```bash
KEYSTORE_PATH="/path/to/silouder.jks" \
STORE_PASSWORD="your_keystore_password" \
KEY_ALIAS="upload" \
KEY_PASSWORD="your_key_password" \
./gradlew assembleRelease
```

---

## 3. Publishing Silouder to F-Droid

Silouder is purposefully architected to comply with F-Droid inclusion policies:
* 100% Free Software under the **GNU General Public License v3.0**.
* **Zero proprietary dependencies**: No Google Play Services, Firebase, or proprietary analytics SDKs.
* **`dependenciesInfo.includeInApk = false`** is enabled in `app/build.gradle.kts` (disables proprietary Play Store tracking blobs).

### Step 1: Create Fastlane Metadata in the Repository

F-Droid uses Fastlane structure to automatically fetch app descriptions, icons, and screenshots:

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

### Step 2: Prepare the F-Droid Metadata Recipe

F-Droid builds all applications directly from source using their official build server.

1. Fork the official [fdroiddata repository](https://gitlab.com/fdroid/fdroiddata) on GitLab.
2. In your fork, create a new recipe file:
   `metadata/com.silouder.app.yml`
3. Add the following content:

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

### Step 3: Test and Submit

1. (Optional) Test building locally with `fdroidserver`:
   ```bash
   fdroid checkupdates com.silouder.app
   fdroid build -v -s com.silouder.app
   ```
2. Commit your new file and submit a **Merge Request (MR)** to `fdroid/fdroiddata`.
3. F-Droid reviewers will test the recipe. Once merged, Silouder will appear in the official F-Droid app store catalog.

---

## 4. Enabling & Configuring GitHub Pages

The project includes a website in the [`docs/`](docs/) folder.

### How to Turn On GitHub Pages

1. Navigate to your repository settings:
   [https://github.com/soms3r/silouder/settings/pages](https://github.com/soms3r/silouder/settings/pages)
2. Under **Build and deployment**:
   * **Source**: Choose **GitHub Actions** *(Recommended, uses our pre-configured [`.github/workflows/pages.yml`](.github/workflows/pages.yml))*.
   * *Alternative (Classic)*: Select **Deploy from a branch** &rarr; Branch: **`main`** &rarr; Folder: **`/docs`** &rarr; Click **Save**.
3. Within 1–2 minutes, your website will be live at:
   **[https://soms3r.github.io/silouder/](https://soms3r.github.io/silouder/)**

---

## 5. Setting Up Repository Social Preview (OG Image) on GitHub

When you share your repository on GitHub, Twitter/X, Discord, Reddit, or Telegram, an OpenGraph preview card is displayed.

### Image Specifications
* **Recommended Dimensions**: `1280 × 640 pixels` (2:1 ratio) or `1200 × 630 pixels`.
* **Format**: PNG, JPG, or SVG (under 1 MB).
* **Location in Repo**: [`docs/assets/og-image.svg`](docs/assets/og-image.svg) is provided with full tactical styling and badges.

### How to Set It on GitHub:
1. Open [https://github.com/soms3r/silouder/settings](https://github.com/soms3r/silouder/settings).
2. Under the **General** tab, scroll down to the **"Social preview"** section.
3. Click **Edit** &rarr; **Upload an image...**.
4. Select `og-image.png` (or convert `docs/assets/og-image.svg` to PNG using any browser/tool).
5. GitHub will now display this branded card across social platforms.

---

## 6. Creating Tagged Releases via GitHub Actions

The repository includes [`.github/workflows/build.yml`](.github/workflows/build.yml), which automatically:
1. Validates all code and tests.
2. Compiles the release-ready `app-debug.apk`.
3. Generates a new **GitHub Release** and attaches the APK whenever a version tag is pushed.

### How to Publish a Release:
In your terminal or GitHub Codespaces:

```bash
# 1. Ensure you are on the latest main branch
git checkout main
git pull origin main

# 2. Create an annotated git tag (match your versionName)
git tag -a v1.0.0 -m "Release v1.0.0: Silouder Multi-Transport Prototype"

# 3. Push the tag to GitHub
git push origin v1.0.0
```

GitHub Actions will automatically build the APK and publish the release at:
**[https://github.com/soms3r/silouder/releases](https://github.com/soms3r/silouder/releases)**

Users visiting your website can immediately click **"Download APK"** to get this file.

---

## 7. Repository Parity & Folder Structure

```
silouder/
├── .github/
│   └── workflows/
│       ├── build.yml               # Automated Android build & GitHub Release CI
│       └── pages.yml               # Automated GitHub Pages deployment
├── app/
│   ├── build.gradle.kts            # App dependencies, SDK targets, signing rules
│   ├── proguard-rules.pro          # ProGuard rules
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
├── MAINTAINER_GUIDE.md             # This guide
├── README.md                       # Project overview, badges & documentation
├── build.gradle.kts                # Root project build file
├── gradle.properties               # Memory & JVM args
├── gradlew                         # Linux/macOS wrapper script
├── gradlew.bat                     # Windows wrapper script
└── settings.gradle.kts             # Plugin repositories and module includes
```

---

*For bug reports or feature suggestions, open an issue at [https://github.com/soms3r/silouder/issues](https://github.com/soms3r/silouder/issues).*
