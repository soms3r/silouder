package com.silouder.app.model

enum class PacketDirection {
    TX_OUTGOING,
    RX_INCOMING,
    RELAY_HOP
}

enum class PacketType {
    MESH_BROADCAST_TEXT,
    DIRECT_E2EE_ENVELOPE,
    ROUTING_SYNC_VECTOR,
    NODE_DISCOVERY_INFO,
    RADIO_TELEMETRY,
    LORA_FRAGMENT_CHUNK,
    E2EE_ACK
}

data class PacketTrace(
    val packetId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: PacketDirection,
    val packetType: PacketType,
    val transport: TransportRouteHint,
    val fromNode: String,
    val toNode: String,
    val channelNum: Int,
    val hopLimit: Int,
    val hopStart: Int,
    val snr: Float,
    val rssi: Int,
    val rawHex: String,
    val decodedSummary: String,
    val isCrcValid: Boolean = true,
    val fragmentIndex: Int? = null,
    val totalFragments: Int? = null
)
