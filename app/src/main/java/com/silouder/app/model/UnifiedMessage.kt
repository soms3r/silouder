package com.silouder.app.model

enum class TransportRouteHint {
    AUTO_BEST,          // Transport router chooses best available link
    BLUETOOTH_P2P,      // Easy Briar-like Direct Bluetooth connection to nearby phone
    ONLINE_NETWORK,     // Direct App-to-App online TCP/HTTP socket across LAN/WiFi/Internet
    LORA_MESH,          // Off-grid LoRa hardware node (BLE Meshtastic)
    TOR_ONION,          // Tor hidden service peer socket
    DIRECT_P2P,         // Direct WiFi-Direct / Local socket
    STORE_FORWARD       // Queued for opportunistic gossip relay
}

enum class MessageStatus {
    QUEUED,                 // Stored in outbox waiting for route
    TRANSMITTING,           // In-flight over transport
    DELIVERED_BLUETOOTH,    // Delivered directly via nearby Bluetooth P2P
    DELIVERED_NETWORK,      // Delivered directly via app-to-app online network socket
    DELIVERED_MESH,         // Acknowledged across LoRa mesh
    DELIVERED_TOR,          // Delivered directly over Tor onion socket
    ACK_RECEIVED,           // End-to-end cryptographic ACK verified
    RELAYED,                // Relayed on behalf of another node
    FAILED                  // Timed out or out of hops
}

enum class ChannelType {
    BROADCAST_PRIMARY,      // Primary Mesh / Local Broadcast
    TACTICAL_SECONDARY,     // Tactical Encrypted Group (Custom PSK)
    EMERGENCY_GROUP,        // Ad-Hoc Emergency Group (Protest / Concert / Incident cell with QR/Join Code)
    DIRECT_BLUETOOTH_PEER,  // Easy 1-on-1 Bluetooth paired peer
    DIRECT_NETWORK_PEER,    // App-to-app direct online network peer
    DIRECT_E2EE             // End-to-end encrypted session (Briar/X25519)
}

/**
 * Unified Data Schema for Silouder Hybrid Offline/Online Mesh & P2P Architecture.
 */
data class UnifiedMessage(
    val messageId: String,               // UUID or packet hash
    val conversationId: String,          // Channel ID or Peer Node ID
    val senderId: String,                // Silouder Peer ID (!7a9f... / onion / IP)
    val senderName: String,              // Handle (e.g. ALPHA_1)
    val recipientId: String,             // Broadcast ("^all") or Peer ID
    val timestamp: Long,                 // Unix epoch millis
    val plaintext: String,               // Decrypted content
    val payloadCiphertext: String,       // Base64 encrypted payload
    val ivBase64: String,                // Cryptographic Nonce/IV
    val macSignature: String,            // AEAD auth tag / HMAC
    val transportRouteHint: TransportRouteHint,
    val actualTransport: TransportRouteHint? = null,
    val hopLimit: Int = 3,               // Max hops
    val hopsTraveled: Int = 0,           // Relays traversed
    val snr: Float = 0.0f,               // Signal-to-Noise Ratio (dB)
    val rssi: Int = -70,                 // Received Signal Strength (dBm)
    val status: MessageStatus = MessageStatus.QUEUED,
    val syncVectorSeq: Long = 0L,        // Anti-entropy sequence number
    val isIncoming: Boolean = false,
    val isBroadcast: Boolean = false,
    val fragmentCount: Int = 1,          // Chunk count
    val deliveryAttempts: Int = 0,
    val isUrgentAlert: Boolean = false   // High-priority emergency broadcast banner
)
