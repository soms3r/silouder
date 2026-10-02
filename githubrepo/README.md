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

## ⚠️ Project status: prototype

Silouder is currently a **UI and architecture prototype**. The cryptography core
(AES-256-GCM, HMAC-SHA256, X25519 key derivation) is real and runs locally, but the
transports — Bluetooth peer discovery/sync, LAN socket delivery, LoRa/Meshtastic
radio, and Tor onion circuits — are **simulated with demo data** and do not yet
exchange real packets.

**Do not rely on this app for real emergencies until the transports are implemented.**

## Features (planned & simulated in the current build)

| Transport | Status | Description |
|---|---|---|
| Bluetooth P2P sync | 🟡 Simulated | Briar-style 1-tap proximity discovery, pairing and offline message sync |
| App-to-app online network | 🟡 Simulated | Direct peer sockets over WiFi/LAN or reachable IPs (port 8888) |
| LoRa mesh (Meshtastic) | 🟡 Simulated | SX1262 modem control, ToRadio/FromRadio BLE framing, MTU fragmentation |
| Tor onion sessions | 🟡 Simulated | v3 onion address derivation from node identity, anonymous circuits |
| Store-and-forward outbox | 🟡 Simulated | Anti-entropy gossip queue that flushes when any transport is reachable |
| E2EE crypto core | 🟢 Real | AES-256-GCM AEAD + HMAC-SHA256 MAC + X25519-style shared-secret derivation |

## Architecture

- **UI:** Jetpack Compose, Material 3, dark tactical theme
- **Persistence:** Room (SQLite) — messages, channels, nodes, packet traces
- **Transport router:** prioritized dispatch across Bluetooth / Network / LoRa / Tor
- **Sync engine:** Briar-inspired anti-entropy gossip with hashed sync vectors
- **Crypto engine:** local identity generation and envelope encryption

Source layout:

```
app/src/main/java/com/silouder/app/
├── crypto/        # CryptoEngine — AES-256-GCM, HMAC, key derivation
├── data/          # Room database, entities, repository
├── model/         # Domain models (messages, channels, nodes, packets)
├── sync/          # BriarSyncEngine — anti-entropy gossip
├── transport/     # bluetooth / network / meshtastic / tor / router
└── ui/            # Compose screens, view model, theme
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

- [ ] Real BLE scanning/advertising and RFCOMM sync for the Bluetooth transport
- [ ] Real TCP socket listener/sender for the app-to-app network transport
- [ ] Meshtastic protobuf framing over real BLE GATT to SX1262 hardware
- [ ] Tor integration (e.g. via a FOSS onion-routing library) for real onion circuits
- [ ] SQLCipher at-rest encryption for the message database
- [ ] Ratchet-based forward secrecy per session

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
