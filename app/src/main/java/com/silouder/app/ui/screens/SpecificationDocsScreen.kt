package com.silouder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silouder.app.ui.AegisViewModel
import com.silouder.app.ui.theme.*

@Composable
fun SpecificationDocsScreen(
    viewModel: AegisViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("architecture_spec_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = CyberCyan)
                        Text("SYSTEM ARCHITECTURE SPECIFICATION", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Text("Silouder: Decentralized Multi-Transport Communicator (Easy Bluetooth + App-to-App Online + LoRa Mesh + Tor)", fontSize = 12.sp, color = TextPrimary)
                }
            }
        }

        item {
            SpecSectionCard(
                title = "1. Easy Bluetooth P2P (Briar-Inspired)",
                content = """
                    • 1-Tap Nearby Peer Discovery:
                      Scans for nearby Silouder phones within 10-30 meters over Bluetooth Low Energy (BLE).
                      
                    • Zero-Metadata Offline Sync:
                      When in proximity, phones execute an ephemeral ECDH handshake and sync pending offline messages without leaking identity vectors.
                      
                    • Bluetooth Beacon Mode:
                      Broadcasts a local beacon advertisement so nearby peers can automatically detect and pair with your device.
                """.trimIndent()
            )
        }

        item {
            SpecSectionCard(
                title = "2. App-to-App Direct Online Network Transport",
                content = """
                    • Embedded Local Socket/HTTP Server:
                      Listens on port 8888 for incoming encrypted packets directly over local WiFi, LAN, or public IP connections.
                      
                    • Instant Low-Latency Delivery:
                      Bypasses RF airtime and limits, delivering full-fidelity encrypted envelopes with real-time ping verification and subnet auto-discovery.
                """.trimIndent()
            )
        }

        item {
            SpecSectionCard(
                title = "3. Off-Grid LoRa Mesh (Meshtastic SX1262)",
                content = """
                    • Hardware Node Integration:
                      Connects to external LoRa transceivers (Heltec V3, T-Beam, RAK4631) over Bluetooth LE GATT (ToRadio/FromRadio).
                      
                    • MTU Chunking & CRC32 Reassembly:
                      Splits payloads exceeding the ~220 byte over-the-air limit into sequence-indexed fragments with CRC checksums.
                """.trimIndent()
            )
        }

        item {
            SpecSectionCard(
                title = "4. Tor v3 Onion Session & Zero-Knowledge Cryptography",
                content = """
                    • End-to-End Encryption (E2EE):
                      AES-256-GCM authenticated encryption paired with X25519 DH shared secret derivation.
                      
                    • Local SQLite/SQLCipher Store-and-Forward:
                      Outbox queues undelivered messages and automatically flushes whenever any transport link becomes reachable.
                """.trimIndent()
            )
        }
    }
}

@Composable
fun SpecSectionCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = content, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}
