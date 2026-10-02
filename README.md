<div align="center">

# Silouder

**Offline-first multi-transport communicator — Bluetooth P2P · LAN/Internet sockets · LoRa mesh · Tor onion routing**

A free-software (GPL-3.0) Android project exploring decentralized, zero-infrastructure
messaging for situations where cell towers and internet are unavailable.

<br>

[![Build Status](https://img.shields.io/badge/CI-GitHub%20Actions-22D3EE?logo=github-actions&logoColor=white)](https://github.com/soms3r/silouder/actions)
[![Official Website](https://img.shields.io/badge/Website-Silouder%20Pages-00F0FF?logo=googlechrome&logoColor=white)](https://soms3r.github.io/silouder/)
[![Download APK](https://img.shields.io/badge/Download-APK%20(v1.0.0)-10B981?logo=android&logoColor=white)](https://github.com/soms3r/silouder/releases/latest)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)

<br>

[**🌐 Live Website**](https://soms3r.github.io/silouder/) &nbsp;|&nbsp; [**⬇️ Download Latest APK**](https://github.com/soms3r/silouder/releases/latest) &nbsp;|&nbsp; [**📖 Documentation**](docs/index.html) &nbsp;|&nbsp; [**🛠️ Maintainer Guide**](MAINTAINER_GUIDE.md)

</div>

---

## 🚀 Project Status: Production-Ready Offline & Local Communicator

Silouder is a privacy-first, zero-infrastructure offline Android communicator combining real cryptographic security, local-network peer discovery, Bluetooth LE proximity sync, store-and-forward gossip, and direct peer-to-peer audio calling.

## Features & Implementation Status

| Feature / Subsystem | Status | Description |
|---|---|---|
| **E2EE Crypto Core** | 🟢 **Real** | AES-256-GCM AEAD encryption + HMAC-SHA256 integrity verification + SHA-256 key derivation. |
| **App-to-App Local Network** | 🟢 **Real** | Embedded multi-threaded TCP server on port 8888, mDNS / Android `NsdManager` service discovery (`_silouder._tcp`), real encrypted packet exchange. |
| **P2P Audio Calling & Walkie-Talkie** | 🟢 **Real** | Direct local socket signaling, 16kHz PCM full-duplex voice streaming via `AudioRecord`/`AudioTrack`, and push-to-talk (PTT) tactical voice bursts. |
| **Encrypted File & Voice Notes** | 🟢 **Real** | Sandboxed internal storage, AES-256-GCM media encryption, AAC/M4A voice note recorder and inline player with waveforms. |
| **Bluetooth LE P2P Sync** | 🟢 **Real** | Real Android BLE Scanner (`BluetoothLeScanner`), BLE Advertiser (`BluetoothLeAdvertiser`), and GATT Server for proximity peer discovery. |
| **Store-and-Forward Outbox** | 🟢 **Real** | Room-persisted anti-entropy queue that automatically flushes when any transport peer becomes reachable. |
| **LoRa Mesh (Meshtastic)** | 🟡 **Hardware Ready** | SX1262 BLE GATT service framework (`BleMeshTransceiver.kt`) with live hardware connection management and manual simulation mode. |
| **Tor Onion Routing** | 🟡 **Simulated Architecture** | v3 onion address derivation from local node identity and isolated route layer. |

## Architecture

- **UI:** Jetpack Compose, Material 3, cyber tactical theme with Dual-Mode switcher (Standard Messenger vs. Tactical Operator)
- **Audio & Media:** Real-time 16kHz PCM duplex voice streaming, AAC voice note recording, sandboxed file attachments
- **Persistence:** Room (SQLite) — messages, channels, nodes, packet traces
- **Transport router:** Prioritized dispatch across Bluetooth LE / Network / LoRa / Tor
- **Sync engine:** Briar-inspired anti-entropy gossip with hashed sync vectors
- **Crypto engine:** Local identity generation and envelope encryption (AES-256-GCM + HMAC-SHA256)

Source layout:

```
app/src/main/java/com/silouder/app/
├── crypto/        # CryptoEngine — AES-256-GCM, HMAC-SHA256
├── data/          # Room database, DAO, entities, MeshRepository
├── media/         # P2PCallManager (audio/video call stream), FileManager (encrypted media)
├── model/         # Domain models (messages, channels, nodes, packets, user profile)
├── service/       # SilouderMeshService (foreground service for background mesh & calls)
├── sync/          # BriarSyncEngine — anti-entropy gossip & vector hashing
├── transport/     # Bluetooth LE, Network (HTTP/TCP/mDNS), LoRa Meshtastic, Tor, Router
└── ui/            # Compose screens, SilouderViewModel, SilouderTheme
```

## Build

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (or any JDK 17+ with the Android SDK), Android SDK 36.

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Signed release (reads keystore from environment variables)
KEYSTORE_PATH=/path/to/release.jks STORE_PASSWORD=... KEY_ALIAS=upload KEY_PASSWORD=... \
  ./gradlew assembleRelease
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Roadmap

- [x] Embedded HTTP/TCP socket server on port 8888 (`NetworkPeerTransport.kt`)
- [x] Android Network Service Discovery (`NsdManager`) mDNS peer discovery
- [x] Bluetooth LE scanning, advertising, and GATT server (`BluetoothPeerManager.kt`)
- [x] P2P VoIP duplex voice calling and Tactical Walkie-Talkie (PTT)
- [x] Encrypted file sharing and voice note recording
- [ ] Protobuf framing for Meshtastic SX1262 modems over physical BLE
- [ ] Tor daemon binary embedding (e.g. via Tor Android service)
- [ ] SQLCipher at-rest database encryption

## Credits & inspiration

Silouder takes protocol inspiration from great FOSS projects — it is not affiliated
with or endorsed by them:

- [Meshtastic](https://meshtastic.org) (GPL-3.0) — LoRa mesh protocol and BLE UUIDs
- [Briar](https://briarproject.org) (AGPL-3.0) — anti-entropy sync and BT architecture
- [The Tor Project](https://torproject.org) (BSD-3) — onion routing concepts
- Android Jetpack, Kotlin, OkHttp, Room (Apache-2.0)

## License

Copyright © 2026 Somser Ali

This program is free software: you can redistribute it and/or modify it under the
terms of the **GNU General Public License as published by the Free Software
Foundation, version 3** of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the [LICENSE](LICENSE) file for the full license text.
