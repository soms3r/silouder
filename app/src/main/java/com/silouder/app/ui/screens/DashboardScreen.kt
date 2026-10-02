package com.silouder.app.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.TransportRouteHint
import com.silouder.app.transport.meshtastic.BleConnectionState
import com.silouder.app.transport.tor.TorStatus
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.myIdentity.collectAsStateWithLifecycle()
    val btPeers by viewModel.bluetoothPeerManager.discoveredPeers.collectAsStateWithLifecycle()
    val netPeers by viewModel.networkPeerTransport.networkPeers.collectAsStateWithLifecycle()
    val bleState by viewModel.bleTransceiver.connectionState.collectAsStateWithLifecycle()
    val connectedNode by viewModel.bleTransceiver.connectedRadioNode.collectAsStateWithLifecycle()
    val torStatus by viewModel.torSessionLayer.torStatus.collectAsStateWithLifecycle()
    val isInternetOnline by viewModel.torSessionLayer.isInternetReachable.collectAsStateWithLifecycle()
    val routerStats by viewModel.transportRouter.routerStats.collectAsStateWithLifecycle()
    val nodes by viewModel.allNodes.collectAsStateWithLifecycle()
    val queuedMessages by viewModel.queuedMessages.collectAsStateWithLifecycle()
    val recentPackets by viewModel.recentPackets.collectAsStateWithLifecycle()

    var showSosConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Hero Identity & Status Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_identity_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyanContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "Shield",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SILOUDER COMMUNICATOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = identity.nodeId,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }

                        AssistChip(
                            onClick = { viewModel.navigateTo(AppScreen.KEY_RING) },
                            label = { Text("Key Ring", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = AmberAlert)
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = TextPrimary
                            ),
                            border = null
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkSurfaceHighlight)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ONION ADDRESS", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = identity.onionAddress,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TorOnionPurple
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("LOCAL ENDPOINT", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = "192.168.1.105:8888",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = DirectWifiGreen
                            )
                        }
                    }
                }
            }
        }

        // Multi-Transport Engine 2x2 Grid
        item {
            Text(
                text = "MULTI-TRANSPORT LAYERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bluetooth P2P
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(AppScreen.BLUETOOTH_SYNC) }
                            .testTag("dashboard_bt_tile"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (btPeers.any { it.isConnected }) TacticalEmerald else AmberAlert)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Nearby Bluetooth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${btPeers.size} Briar peers found", fontSize = 10.sp, color = CyberCyan)
                        }
                    }

                    // Online App-to-App Network
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(AppScreen.NETWORK_PEERS) }
                            .testTag("dashboard_net_tile"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lan, contentDescription = null, tint = DirectWifiGreen, modifier = Modifier.size(20.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TacticalEmerald)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Online Network", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${netPeers.size} Socket peers online", fontSize = 10.sp, color = DirectWifiGreen)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // LoRa Mesh
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.navigateTo(AppScreen.RADIO_CONTROL) }
                            .testTag("lora_transport_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CellTower, contentDescription = null, tint = LoRaBlue, modifier = Modifier.size(20.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (bleState == BleConnectionState.CONNECTED_STREAMING) TacticalEmerald else SignalRed)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("LoRa SX1262 Mesh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("915 MHz • 4 Repeaters", fontSize = 10.sp, color = LoRaBlue)
                        }
                    }

                    // Tor Onion
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.torSessionLayer.toggleInternetConnectivity(!isInternetOnline) }
                            .testTag("tor_transport_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.VpnLock, contentDescription = null, tint = TorOnionPurple, modifier = Modifier.size(20.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isInternetOnline && torStatus == TorStatus.ONION_SERVICE_ONLINE) TacticalEmerald else AmberAlert)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Tor Onion Sockets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(if (isInternetOnline) "v3 Online" else "Offline", fontSize = 10.sp, color = TorOnionPurple)
                        }
                    }
                }
            }
        }

        // Live Telemetry Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    TelemetryItem(
                        icon = Icons.Filled.BluetoothSearching,
                        label = "BT Peers",
                        value = "${btPeers.size}",
                        color = CyberCyan
                    )
                    TelemetryItem(
                        icon = Icons.Filled.Speed,
                        label = "LAN Speed",
                        value = "1 Gbps",
                        color = DirectWifiGreen
                    )
                    TelemetryItem(
                        icon = Icons.Filled.CellTower,
                        label = "LoRa SNR",
                        value = "${connectedNode?.snr ?: 9.8}dB",
                        color = LoRaBlue
                    )
                    TelemetryItem(
                        icon = Icons.Filled.Outbox,
                        label = "Outbox",
                        value = "${queuedMessages.size} Queued",
                        color = if (queuedMessages.isEmpty()) TextMuted else AmberAlert
                    )
                }
            }
        }

        // Tactical Emergency SOS Action
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SignalRedContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.2.dp, SignalRed.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SignalRedContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = SignalRed, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Silouder Emergency SOS", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("Broadcast across BT, LAN & LoRa", fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = { showSosConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SignalRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("sos_broadcast_button")
                    ) {
                        Text(
                            text = "SOS BEACON",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Quick Navigation Grid
        item {
            Text(
                text = "COMMUNICATION CHANNELS & CONTROLS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickNavTile(
                    title = "Nearby BT",
                    subtitle = "Briar 1-Tap",
                    icon = Icons.Filled.Bluetooth,
                    color = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.BLUETOOTH_SYNC) }
                )
                QuickNavTile(
                    title = "Online Net",
                    subtitle = "App-to-App",
                    icon = Icons.Filled.Lan,
                    color = DirectWifiGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.NETWORK_PEERS) }
                )
                QuickNavTile(
                    title = "Mesh Radar",
                    subtitle = "Topology Map",
                    icon = Icons.Filled.Radar,
                    color = LoRaBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.MESH_TOPOLOGY) }
                )
                QuickNavTile(
                    title = "Packets",
                    subtitle = "Sniffer Log",
                    icon = Icons.Filled.Terminal,
                    color = TacticalEmerald,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.PACKET_INSPECTOR) }
                )
            }
        }

        // Settings & App Experience Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.SETTINGS) }
                    .testTag("dashboard_settings_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberCyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Settings & Feature Customizer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Switch to Standard Messenger or Tactical Mode • Hide/Show Features", fontSize = 10.sp, color = CyberCyan)
                        }
                    }

                    Icon(Icons.Filled.ChevronRight, contentDescription = "Open Settings", tint = TextSecondary)
                }
            }
        }

        // About App & User Manual Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.ABOUT_APP) }
                    .testTag("dashboard_about_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.dp, AmberAlert.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberAlertContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("About Silouder & User Manual", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Developer Credit: Somser Ali • Open Source Systems", fontSize = 10.sp, color = AmberAlert)
                        }
                    }

                    Icon(Icons.Filled.ChevronRight, contentDescription = "View", tint = TextSecondary)
                }
            }
        }

        // Recent Packet Trace Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE WIRE ACTIVITY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
                TextButton(onClick = { viewModel.navigateTo(AppScreen.PACKET_INSPECTOR) }) {
                    Text("View All (${recentPackets.size})", fontSize = 12.sp, color = CyberCyan)
                }
            }
        }

        items(recentPackets.take(4)) { packet ->
            PacketTraceMiniRow(packet = packet)
        }
    }

    if (showSosConfirmation) {
        AlertDialog(
            onDismissRequest = { showSosConfirmation = false },
            title = { Text("Broadcast Emergency SOS?") },
            text = {
                Text("This will broadcast an emergency beacon with GPS coordinates across Bluetooth LE nearby peers, Online LAN sockets, and all LoRa mesh hops.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendQuickSosBroadcast()
                        showSosConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed)
                ) {
                    Text("BROADCAST NOW")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TelemetryItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String? = null,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (subValue != null) {
            Text(text = subValue, fontSize = 10.sp, color = TextMuted)
        }
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
fun QuickNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(96.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            Column {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text(text = subtitle, fontSize = 10.sp, color = TextMuted, maxLines = 1)
            }
        }
    }
}

@Composable
fun PacketTraceMiniRow(packet: com.silouder.app.model.PacketTrace) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (packet.direction == com.silouder.app.model.PacketDirection.TX_OUTGOING) AmberAlert else CyberCyan)
                )
                Text(
                    text = "${packet.fromNode} -> ${packet.toNode}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text(
                text = packet.decodedSummary.take(28),
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${packet.snr}dB",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = TacticalEmerald
            )
        }
    }
}
