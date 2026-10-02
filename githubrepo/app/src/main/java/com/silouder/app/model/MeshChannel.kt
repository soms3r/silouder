package com.silouder.app.model

data class MeshChannel(
    val channelIndex: Int,               // 0 = Primary, 1..7 = Secondary
    val channelId: String,               // Unique ID
    val name: String,                    // e.g. "LongFast", "Protest Medic Cell", "Concert Squad"
    val channelType: ChannelType,
    val pskBase64: String,               // Pre-Shared Key
    val modemPreset: String = "LONG_FAST", // LONG_FAST, MEDIUM_FAST, SHORT_FAST, VERY_LONG_SLOW
    val frequencyMhz: Float = 915.0f,    // 915 MHz (US) or 868 MHz (EU)
    val spreadingFactor: Int = 11,       // SF7 .. SF12
    val bandwidthKhz: Float = 250.0f,    // 125, 250, 500
    val codingRate: String = "4/8",      // 4/5, 4/8
    val txPowerDbm: Int = 22,            // 20 .. 30 dBm
    val unreadCount: Int = 0,
    val lastMessagePreview: String = "",
    val lastMessageTimestamp: Long = 0L,
    val isPrimary: Boolean = false,
    val peerNodeId: String? = null,      // Only for DIRECT_E2EE
    val joinCode: String = "",           // Secret Join Code (e.g. "SILO-9824" or "PROTEST-MED-77")
    val memberCount: Int = 1,            // Active participant count in cell
    val expiresAt: Long = 0L,            // 0 = Never, or epoch millis for auto-wipe
    val isEmergencyGroup: Boolean = false, // Highlights group with emergency tactical badge
    val incidentLocation: String = "",   // e.g. "Gate 4 North", "Main Plaza"
    val isBleBeaconBroadcast: Boolean = false // Broadcasts BLE beacon for 1-tap crowd discovery
)
