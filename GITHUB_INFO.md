# GitHub Repository Metadata, Topics & Release Configuration

This document contains all official copy, metadata, tags, and settings needed to configure your GitHub repository at:
**`https://github.com/soms3r/silouder`**

---

## 📌 1. Repository "About" Section (Top-Right on GitHub)

To configure:
1. Go to your repository homepage: [https://github.com/soms3r/silouder](https://github.com/soms3r/silouder)
2. In the top-right sidebar under **About**, click the ⚙️ (gear icon).
3. Paste the following values:

### Description
```text
Offline-first multi-transport Android communicator. E2EE messaging & P2P voice over Bluetooth LE, local WiFi sockets, LoRa mesh (Meshtastic), and Tor onion routing without cell towers or internet.
```
*(Character count: ~180 chars, well within GitHub's 350-character limit).*

### Website URL
```text
https://soms3r.github.io/silouder/
```

### Include in Home Page
- [x] **Releases**
- [x] **Packages**
- [x] **Environments**

---

## 🏷️ 2. Repository Topics (Tags for Discoverability)

Adding these topics ensures Silouder appears in GitHub's search results, trending feeds, and topic exploration lists:

```text
android
kotlin
jetpack-compose
mesh-network
offline-first
e2ee
encryption
bluetooth-le
lora
meshtastic
tor
onion-routing
p2p
walkie-talkie
voip
decentralized
disaster-response
f-droid
fdroid
privacy
```

### How to apply them:
Under **About** &rarr; ⚙️ **Edit repository details** &rarr; paste the topics one by one into the **Topics** field.

---

## 🖼️ 3. Repository Social Preview (OpenGraph Card)

When someone shares your repository link on Twitter/X, Telegram, Discord, LinkedIn, or Reddit, GitHub displays this card preview.

1. Go to **Settings** &rarr; **General** &rarr; scroll down to **Social preview**.
2. Click **Edit** &rarr; **Upload an image...**.
3. File location in your repository:
   **`docs/assets/og-image.svg`** *(or convert to PNG 1280×640 px)*.

---

## 🚀 4. GitHub Releases — v1.0.0 Release Notes Template

When creating a new release or pushing the `v1.0.0` git tag, use the following release notes:

### Release Tag: `v1.0.0`
### Release Title: `Silouder v1.0.0 — Production-Ready Offline Multi-Transport Communicator`

```markdown
### 🛡️ Silouder v1.0.0: The Zero-Infrastructure Android Communicator

Silouder is a privacy-first, zero-cloud offline Android communicator engineered for emergency communication, remote expeditions, and disaster zones — as well as everyday off-grid scenarios (subways, concerts, flights, hiking).

#### 🌟 Key Highlights & Capabilities
* **E2EE Cryptographic Core**: Real AES-256-GCM AEAD encryption with unique 96-bit random nonces, 128-bit authentication tags, and HMAC-SHA256 signatures per envelope.
* **App-to-App Local Network**: Embedded multi-threaded TCP server on port 8888 with Android Network Service Discovery (`NsdManager` mDNS zero-conf) for instant auto-discovery across Wi-Fi and hotspots.
* **P2P Audio Calling & Tactical Walkie-Talkie**: Full-duplex 16kHz PCM voice streaming via UDP port 8899 and instant Push-to-Talk (PTT) voice bursts.
* **Encrypted Voice Notes & Media Attachments**: In-app AAC/M4A voice note recorder and inline player with waveforms; sandboxed internal storage for photos and documents.
* **Proximity Bluetooth LE P2P Sync**: Real Android `BluetoothLeScanner`, `BluetoothLeAdvertiser`, and GATT server for proximity sync.
* **Store-and-Forward Outbox**: Anti-entropy queue that automatically flushes messages when any transport peer becomes reachable.
* **100% Free & Open Source**: Licensed under GNU GPL-3.0 with zero proprietary blobs, zero telemetry, and zero tracking.

#### 📦 Download & Installation
* Download `app-debug.apk` directly from the release assets below.
* Android requirements: Android 7.0+ (API 24 to API 36).
* Fast, offline sideloading with no Google Play account required.

**Full Changelog**: https://github.com/soms3r/silouder/commits/v1.0.0
```

---

## 🌐 5. GitHub Pages Configuration

Ensure GitHub Pages is serving the documentation portal:
1. Navigate to: [https://github.com/soms3r/silouder/settings/pages](https://github.com/soms3r/silouder/settings/pages)
2. **Build and deployment**:
   - **Source**: Select **GitHub Actions**
3. Once selected, [`.github/workflows/pages.yml`](.github/workflows/pages.yml) will automatically publish the website from the `docs/` directory.
4. Live site URL: **`https://soms3r.github.io/silouder/`**

---

## 📱 6. F-Droid App Store Metadata Copy

When publishing to the official F-Droid app catalog:

### Short Description (Max 80 chars)
```text
Offline-first multi-transport mesh communicator with E2EE and P2P voice calling.
```

### Full Description (Markdown)
```markdown
Silouder is a decentralized, zero-infrastructure offline Android communicator designed for resilient messaging when cellular networks and the internet are unavailable.

Key Features:
• Proximity Bluetooth LE peer sync (Briar-inspired gossip vectors)
• Local LAN / direct IP sockets over Wi-Fi and hotspots (port 8888)
• Direct P2P duplex voice calling and Tactical Walkie-Talkie (PTT)
• Encrypted AAC voice notes and photo/document file sharing
• LoRa mesh radio integration (Meshtastic SX1262 modems over BLE)
• Tor v3 onion session routing
• Real E2EE cryptographic core (AES-256-GCM AEAD + HMAC-SHA256)
• 100% Free Software under GNU GPL-3.0 with zero trackers or cloud reliance
```

### Categories
- `Connectivity`
- `Security`

---

## ⚙️ 7. Recommended GitHub Settings Checklist

- [x] **Issues**: Enabled (Settings &rarr; General &rarr; Features &rarr; Issues).
- [x] **Discussions**: Enabled (Settings &rarr; General &rarr; Features &rarr; Discussions).
- [x] **Actions Permissions**: Read and write permissions enabled (Settings &rarr; Actions &rarr; General &rarr; Workflow permissions &rarr; *Read and write permissions*).
- [x] **Default Branch**: `main`.
