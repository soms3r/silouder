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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.transport.bluetooth.BluetoothPeer
import com.silouder.app.transport.bluetooth.BluetoothScanState
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSyncScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val peers by viewModel.bluetoothPeerManager.discoveredPeers.collectAsStateWithLifecycle()
    val scanState by viewModel.bluetoothPeerManager.scanState.collectAsStateWithLifecycle()
    val isDiscoverable by viewModel.bluetoothPeerManager.isDiscoverable.collectAsStateWithLifecycle()
    val logs by viewModel.bluetoothPeerManager.bluetoothLogs.collectAsStateWithLifecycle()
    val myIdentity by viewModel.myIdentity.collectAsStateWithLifecycle()

    var showQrModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Hero Card: Easy 1-Tap Bluetooth Discovery
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bluetooth_sync_hero_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                    .background(CyberCyanContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.BluetoothSearching, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("EASY BLUETOOTH SYNC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan, letterSpacing = 1.sp)
                                Text("Nearby Silouder Phones", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Button(
                            onClick = { viewModel.bluetoothPeerManager.startEasyScan() },
                            enabled = scanState != BluetoothScanState.SCANNING,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = Color(0xFF050B14),
                                disabledContainerColor = CyberCyan.copy(alpha = 0.4f),
                                disabledContentColor = Color(0xFF050B14)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("bluetooth_scan_button")
                        ) {
                            if (scanState == BluetoothScanState.SCANNING) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF050B14), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SCANNING...", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            } else {
                                Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SCAN NOW", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    // Beacon & QR Share Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bluetooth Beacon Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Make phone discoverable to nearby peers", fontSize = 11.sp, color = TextSecondary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { showQrModal = true },
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(DarkSurfaceHighlight)
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = "QR Handshake", tint = AmberAlert, modifier = Modifier.size(22.dp))
                            }

                            Switch(
                                checked = isDiscoverable,
                                onCheckedChange = { viewModel.bluetoothPeerManager.toggleDiscoverableBeacon(it) }
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DISCOVERED BLUETOOTH PEERS (${peers.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Offline Range: ~20m",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TacticalEmerald
                )
            }
        }

        // Discovered Peer Cards
        items(peers) { peer ->
            BluetoothPeerCard(
                peer = peer,
                onPair = { viewModel.bluetoothPeerManager.quickPairPeer(peer.peerId) },
                onSync = { viewModel.bluetoothPeerManager.syncWithPeer(peer.peerId) },
                onChat = { viewModel.openConversation("chan_direct_bt") }
            )
        }

        // Bluetooth Activity Log
        item {
            Text(
                text = "BLUETOOTH RFCOMM & BLE LOGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    logs.take(6).forEach { line ->
                        Text(
                            text = line,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.contains("Sync")) TacticalEmerald else CyberCyan
                        )
                    }
                }
            }
        }
    }

    if (showQrModal) {
        AlertDialog(
            onDismissRequest = { showQrModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.QrCode2, contentDescription = null, tint = AmberAlert)
                    Text("In-Person Bluetooth Handshake", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Scan this QR code with another Silouder phone to instantly pair and exchange E2EE session keys via Bluetooth.", fontSize = 12.sp, color = TextPrimary)

                    // Simulated QR representation
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(110.dp))
                                Text(myIdentity.nodeId, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Node ID: ${myIdentity.nodeId}", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14))
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun BluetoothPeerCard(
    peer: BluetoothPeer,
    onPair: () -> Unit,
    onSync: () -> Unit,
    onChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bt_peer_card_${peer.peerId}"),
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (peer.isConnected) TacticalEmeraldContainer else DarkSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (peer.isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.Bluetooth,
                            contentDescription = null,
                            tint = if (peer.isConnected) TacticalEmerald else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = peer.alias, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (peer.isConnected) {
                                Surface(shape = RoundedCornerShape(4.dp), color = TacticalEmeraldContainer) {
                                    Text("CONNECTED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = TacticalEmerald, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(text = "${peer.deviceName} • ${peer.peerId}", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "~${peer.distanceMeters}m away",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (peer.distanceMeters < 3f) TacticalEmerald else AmberAlert
                    )
                    Text(text = "${peer.rssi} dBm", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons with High Contrast
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!peer.isPaired) {
                    Button(
                        onClick = onPair,
                        modifier = Modifier.weight(1f).testTag("quick_pair_btn_${peer.peerId}"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QUICK PAIR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSync,
                        modifier = Modifier.weight(1f).testTag("bt_sync_btn_${peer.peerId}"),
                        border = BorderStroke(1.5.dp, TacticalEmerald),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-TAP SYNC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onChat,
                        modifier = Modifier.weight(1f).testTag("bt_chat_btn_${peer.peerId}"),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(15.dp), tint = CyberCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CHAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}
