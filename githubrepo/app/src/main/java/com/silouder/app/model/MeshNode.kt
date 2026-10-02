package com.silouder.app.model

enum class NodeRole {
    CLIENT,
    CLIENT_MUTE,
    ROUTER,
    ROUTER_LATE,
    REPEATER,
    TRACKER,
    SENSOR
}

enum class HardwareModel(val displayName: String) {
    HELTEC_V3("Heltec Wireless Tracker V3 (ESP32-S3 + SX1262)"),
    T_BEAM_SUPREME("LilyGO T-Beam Supreme (SX1262 + GPS)"),
    RAK_4631("RAK Wireless WisBlock RAK4631 (nRF52840)"),
    T_ECHO("LilyGO T-Echo (E-Paper + nRF52840)"),
    STATION_G2("Unit V G2 Tactical Base Station"),
    TOR_VIRTUAL_PEER("Tor Onion Peer Node (v3 Hidden Svc)")
}

data class MeshNode(
    val nodeId: String,                  // e.g. "!2a4b89ef" or "vanguard49...onion"
    val longName: String,                // e.g. "Echo 4 Recon Node"
    val shortName: String,               // e.g. "ECH4"
    val role: NodeRole = NodeRole.CLIENT,
    val hardwareModel: HardwareModel = HardwareModel.HELTEC_V3,
    val publicKeyHex: String = "",
    val onionAddress: String = "",
    val lastHeardTimestamp: Long = System.currentTimeMillis(),
    val snr: Float = 6.5f,
    val rssi: Int = -85,
    val batteryLevel: Int = 88,          // Percentage 0..100
    val voltage: Float = 4.12f,
    val channelUtilization: Float = 14.2f, // % Airtime duty cycle
    val hopCount: Int = 1,
    val isDirectNeighbor: Boolean = true,
    val isBleConnectedRadio: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Int? = null,
    val isOnline: Boolean = true,
    val isFavorited: Boolean = false
)
