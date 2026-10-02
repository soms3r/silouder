package com.silouder.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*

data class FeatureGuideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val steps: List<String>,
    val tacticalTip: String
)

data class OpenSourceCredit(
    val name: String,
    val role: String,
    val description: String,
    val license: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val devGithubUrl = "https://github.com/soms3r"

    var expandedGuideId by remember { mutableStateOf<String?>("emg_groups") }

    val featureGuides = remember {
        listOf(
            FeatureGuideItem(
                id = "emg_groups",
                title = "Emergency & Protest Groups",
                subtitle = "Ad-Hoc Crowd Cells with QR & Secret Join Codes",
                icon = Icons.Filled.Shield,
                iconColor = SignalRed,
                steps = listOf(
                    "1. Tap '+ CREATE GROUP' in the Messages tab.",
                    "2. Enter a cell name (e.g. 'Protest Medic Support' or 'Gate B Safety Cell').",
                    "3. An AES-256 group key is automatically derived from the Secret Join Code (e.g. 'SILO-7721').",
                    "4. Share the Secret Join Code or high-contrast QR Code with crowd members.",
                    "5. Anyone can join with 1 tap via 'JOIN BY CODE / QR'.",
                    "6. Broadcast high-priority emergency alerts or panic 'Burn & Leave' to wipe local storage instantly."
                ),
                tacticalTip = "Ideal for protests, rallies, music festivals, and disaster relief where cell towers are jammed or down."
            ),
            FeatureGuideItem(
                id = "bt_sync",
                title = "Nearby Bluetooth Peer Sync",
                subtitle = "Briar-Style 1-Tap Offline Proximity Sync",
                icon = Icons.Filled.BluetoothSearching,
                iconColor = CyberCyan,
                steps = listOf(
                    "1. Open the 'Nearby BT' tab.",
                    "2. Tap 'SCAN NOW' to detect nearby Silouder phones within 10–30 meters.",
                    "3. Enable 'Bluetooth Beacon Mode' so other phones can discover you.",
                    "4. Tap 'QUICK PAIR' to perform an automatic mutual X25519 ECDH key exchange.",
                    "5. Tap '1-TAP SYNC' to synchronize pending offline messages and vector clocks with zero metadata leakage."
                ),
                tacticalTip = "No internet, cellular network, or external radio hardware required. Operates phone-to-phone."
            ),
            FeatureGuideItem(
                id = "net_peers",
                title = "App-to-App Online Network",
                subtitle = "Direct High-Speed LAN & Internet Socket Transport",
                icon = Icons.Filled.Lan,
                iconColor = DirectWifiGreen,
                steps = listOf(
                    "1. Open the 'Online Net' tab to view your local listening endpoint (e.g. 192.168.1.105:8888).",
                    "2. Tap 'SCAN SUBNET' to auto-discover other Silouder apps on the same WiFi network.",
                    "3. Or tap 'ADD PEER' and enter any reachable IP Address / Port.",
                    "4. Tap 'PING' to verify sub-50ms roundtrip latency.",
                    "5. Messages sent over Online Net deliver immediately with zero radio airtime congestion."
                ),
                tacticalTip = "Great for local mesh networks, home lab relays, campus WiFi, and remote VPN tunnels."
            ),
            FeatureGuideItem(
                id = "lora_mesh",
                title = "Off-Grid LoRa SX1262 Mesh",
                subtitle = "Meshtastic Long-Range Hardware Node Transceiver",
                icon = Icons.Filled.CellTower,
                iconColor = LoRaBlue,
                steps = listOf(
                    "1. Go to 'Radio' settings and connect your phone to an external LoRa node via Bluetooth LE.",
                    "2. Supports Heltec V3, LilyGO T-Beam, RAK4631, and T-Echo hardware.",
                    "3. Select your regional frequency (915 MHz US / 868 MHz EU / 433 MHz AS).",
                    "4. Configure Spreading Factor (SF7 for speed, SF12 for maximum penetration across hills/cities).",
                    "5. Payloads exceeding the 220-byte MTU limit are automatically fragmented and reassembled with CRC32 integrity checks."
                ),
                tacticalTip = "Enables multi-kilometer communication across mountain ranges and urban areas through repeater hops."
            ),
            FeatureGuideItem(
                id = "tor_onion",
                title = "Tor v3 Onion Sessions",
                subtitle = "Anonymous Hidden Services & Zero Metadata",
                icon = Icons.Filled.VpnLock,
                iconColor = TorOnionPurple,
                steps = listOf(
                    "1. Your phone derives a unique Tor v3 Onion address (e.g. silouder...onion) from your public key.",
                    "2. Tor circuits provide anonymous end-to-end encrypted sockets without centralized servers.",
                    "3. Intermediate nodes only see cryptographic hashes, never contact identities or chat topics."
                ),
                tacticalTip = "Provides global reach when internet is available while maintaining strict location privacy."
            ),
            FeatureGuideItem(
                id = "store_forward",
                title = "Store-and-Forward Outbox",
                subtitle = "Opportunistic Anti-Entropy Gossip Propagation",
                icon = Icons.Filled.Outbox,
                iconColor = AmberAlert,
                steps = listOf(
                    "1. If you send a message while completely air-gapped, it queues safely in your local SQLite/SQLCipher Outbox.",
                    "2. The Transport Router continuously monitors transport states (Bluetooth, LAN, LoRa, Tor).",
                    "3. When you walk into range of a peer or repeater, the outbox automatically flushes and delivers pending payloads."
                ),
                tacticalTip = "Tap 'FORCE FLUSH' in the Outbox tab anytime to manually trigger message propagation."
            )
        )
    }

    val openSourceCredits = remember {
        listOf(
            OpenSourceCredit(
                name = "Meshtastic Project",
                role = "LoRa Mesh Radio Protocol",
                description = "Protobuf wire framing (ToRadio / FromRadio), multi-hop mesh routing, SX1262 modem parameter specifications, and BLE GATT profiles.",
                license = "GPL v3",
                icon = Icons.Filled.CellTower,
                color = LoRaBlue
            ),
            OpenSourceCredit(
                name = "Briar Project",
                role = "Anti-Entropy P2P Sync & Bluetooth Architecture",
                description = "Decentralized zero-metadata gossip protocols, blinded vector clock exchanges, and direct Bluetooth RFCOMM sync paradigms.",
                license = "GPL v3",
                icon = Icons.Filled.Bluetooth,
                color = CyberCyan
            ),
            OpenSourceCredit(
                name = "The Tor Project",
                role = "Onion Routing v3 Hidden Services",
                description = "Specifications for decentralized rendezvous points, v3 onion address derivation, and censorship-resistant socket transport.",
                license = "3-Clause BSD",
                icon = Icons.Filled.VpnLock,
                color = TorOnionPurple
            ),
            OpenSourceCredit(
                name = "Libsodium & BouncyCastle",
                role = "Cryptographic Primitives",
                description = "X25519 Elliptic Curve Diffie-Hellman (ECDH), AES-256-GCM authenticated encryption (AEAD), and HMAC-SHA256 message authentication.",
                license = "ISC & MIT License",
                icon = Icons.Filled.Key,
                color = TacticalEmerald
            ),
            OpenSourceCredit(
                name = "Android Jetpack & Kotlin",
                role = "Modern Native Runtime",
                description = "Jetpack Compose UI framework, Room Database local persistence, Kotlin Coroutines, Flow, and Material Design 3 system.",
                license = "Apache 2.0",
                icon = Icons.Filled.Android,
                color = DirectWifiGreen
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // App Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_app_hero_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyanContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
                            }
                            Column {
                                Text(
                                    text = "SILOUDER",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberCyan,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "Version 1.0.0 • Offline-First Mesh",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TacticalEmeraldContainer
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    Text(
                        text = "Silouder is a high-security, decentralized, zero-infrastructure communicator engineered to keep people connected when internet, cell towers, and power grids fail.",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = CyberCyan.copy(alpha = 0.15f)) {
                            Text("Bluetooth P2P", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = DirectWifiGreen.copy(alpha = 0.15f)) {
                            Text("Online LAN Sockets", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DirectWifiGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = LoRaBlue.copy(alpha = 0.15f)) {
                            Text("LoRa Mesh", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = LoRaBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = TorOnionPurple.copy(alpha = 0.15f)) {
                            Text("Tor Onion", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TorOnionPurple, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // Developer Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_credit_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AmberAlert.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(AmberAlertContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("DEVELOPER & ARCHITECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAlert, letterSpacing = 1.sp)
                                Text("Somser Ali", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AmberAlertContainer
                        ) {
                            Text(
                                text = "LEAD ENGINEER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAlert,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Designed and architected by Somser Ali. Specialized in decentralized mesh architectures, offline-first peer-to-peer protocols, and privacy-preserving cryptographic communication systems.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.DataObject, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = "github.com/soms3r",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Developer GitHub", devGithubUrl)
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                    Toast.makeText(context, "GitHub URL copied!", Toast.LENGTH_SHORT).show()
                                },
                                border = BorderStroke(1.2.dp, AmberAlert),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(devGithubUrl))
                                    runCatching { context.startActivity(intent) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberAlert, contentColor = Color(0xFF050B14)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("dev_github_btn")
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GITHUB", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Feature How-To Guide Section
        item {
            Text(
                text = "HOW TO USE EVERY FEATURE (USER MANUAL)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        items(featureGuides) { guide ->
            val isExpanded = expandedGuideId == guide.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedGuideId = if (isExpanded) null else guide.id }
                    .testTag("feature_guide_${guide.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(guide.iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = guide.icon, contentDescription = null, tint = guide.iconColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(text = guide.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = guide.subtitle, fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = DarkSurfaceHighlight)

                            guide.steps.forEach { step ->
                                Text(
                                    text = step,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }

                            Surface(
                                color = DarkSurface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = guide.tacticalTip,
                                        fontSize = 10.sp,
                                        color = AmberAlert,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Open Source Implementations & Credits
        item {
            Text(
                text = "OPEN SOURCE SYSTEMS IMPLEMENTED & CREDITS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        items(openSourceCredits) { credit ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = credit.icon, contentDescription = null, tint = credit.color, modifier = Modifier.size(18.dp))
                            Text(text = credit.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceElevated
                        ) {
                            Text(
                                text = credit.license,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Module: ${credit.role}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = credit.color
                    )

                    Text(
                        text = credit.description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
