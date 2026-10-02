package com.silouder.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.silouder.app.transport.network.NetworkPeer
import com.silouder.app.transport.network.NetworkServerStatus
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPeersScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val peers by viewModel.networkPeerTransport.networkPeers.collectAsStateWithLifecycle()
    val localIp by viewModel.networkPeerTransport.localIpAddress.collectAsStateWithLifecycle()
    val localPort by viewModel.networkPeerTransport.localPort.collectAsStateWithLifecycle()
    val serverStatus by viewModel.networkPeerTransport.serverStatus.collectAsStateWithLifecycle()
    val logs by viewModel.networkPeerTransport.networkLogs.collectAsStateWithLifecycle()

    var showAddPeerDialog by remember { mutableStateOf(false) }
    var newPeerAlias by remember { mutableStateOf("") }
    var newPeerIp by remember { mutableStateOf("") }
    var newPeerPort by remember { mutableStateOf("8888") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Embedded Server Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("network_server_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
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
                                    .background(TacticalEmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Lan, contentDescription = null, tint = TacticalEmerald, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text("APP-TO-APP ONLINE NETWORK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalEmerald, letterSpacing = 1.sp)
                                Text("Direct Socket Transport", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TacticalEmeraldContainer
                        ) {
                            Text(
                                text = "LISTENING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TacticalEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    // Local Endpoint display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LOCAL ENDPOINT (IP:PORT)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            Text(
                                text = "$localIp:$localPort",
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberCyan
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DirectWifiGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "PORT 8888 OPEN",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = DirectWifiGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Full-Width Action Buttons (never cramped or wrapped)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.networkPeerTransport.scanLocalSubnet() },
                            border = BorderStroke(1.2.dp, CyberCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).testTag("scan_subnet_button")
                        ) {
                            Icon(Icons.Filled.WifiFind, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCAN SUBNET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { showAddPeerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f).testTag("add_net_peer_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ADD PEER", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // Online Peers List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ONLINE NETWORK PEERS (${peers.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "High Speed • 0 Airtime",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DirectWifiGreen
                )
            }
        }

        items(peers) { peer ->
            NetworkPeerCard(
                peer = peer,
                onPing = { viewModel.networkPeerTransport.pingPeer(peer.ipAddress) },
                onChat = { viewModel.openConversation("chan_direct_net") },
                onSimulateMsg = {
                    viewModel.simulateIncomingNetworkMessage(peer.alias, peer.fullAddress, "Direct app-to-app message delivered via WiFi socket! Latency: ${peer.latencyMs}ms")
                }
            )
        }

        // Network Activity Log
        item {
            Text(
                text = "TCP SOCKET & HTTP REST LOGS",
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
                            color = if (line.contains("delivery")) DirectWifiGreen else CyberCyan
                        )
                    }
                }
            }
        }
    }

    if (showAddPeerDialog) {
        AlertDialog(
            onDismissRequest = { showAddPeerDialog = false },
            title = { Text("Connect to Remote Silouder App", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the IP address and port of another phone running Silouder.", fontSize = 12.sp, color = TextPrimary)

                    OutlinedTextField(
                        value = newPeerAlias,
                        onValueChange = { newPeerAlias = it },
                        label = { Text("Peer Name / Alias") },
                        placeholder = { Text("e.g. Charlie HQ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_peer_alias_input")
                    )

                    OutlinedTextField(
                        value = newPeerIp,
                        onValueChange = { newPeerIp = it },
                        label = { Text("IP Address / Hostname") },
                        placeholder = { Text("e.g. 192.168.1.150") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_peer_ip_input")
                    )

                    OutlinedTextField(
                        value = newPeerPort,
                        onValueChange = { newPeerPort = it },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_peer_port_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPeerIp.isNotBlank()) {
                            viewModel.networkPeerTransport.addNetworkPeer(
                                alias = newPeerAlias,
                                ipAddress = newPeerIp,
                                port = newPeerPort.toIntOrNull() ?: 8888
                            )
                            newPeerAlias = ""
                            newPeerIp = ""
                            newPeerPort = "8888"
                            showAddPeerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14))
                ) {
                    Text("CONNECT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPeerDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun NetworkPeerCard(
    peer: NetworkPeer,
    onPing: () -> Unit,
    onChat: () -> Unit,
    onSimulateMsg: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("net_peer_card_${peer.ipAddress}"),
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
                            .background(if (peer.isOnline) TacticalEmeraldContainer else DarkSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = null,
                            tint = if (peer.isOnline) TacticalEmerald else TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = peer.alias, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (peer.isSubnetDiscovered) {
                                Surface(shape = RoundedCornerShape(4.dp), color = CyberCyanContainer) {
                                    Text("LAN AUTO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyberCyan, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(
                            text = peer.fullAddress,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyan
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(TacticalEmerald))
                        Text("ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalEmerald)
                    }
                    Text(text = "${peer.latencyMs}ms", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons with High Contrast
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPing,
                    modifier = Modifier.weight(1f).testTag("ping_peer_${peer.ipAddress}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.2.dp, CyberCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                }

                OutlinedButton(
                    onClick = onSimulateMsg,
                    modifier = Modifier.weight(1.1f).testTag("sim_msg_peer_${peer.ipAddress}"),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.2.dp, DirectWifiGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DirectWifiGreen)
                ) {
                    Icon(Icons.Filled.CallReceived, contentDescription = null, modifier = Modifier.size(14.dp), tint = DirectWifiGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TEST MSG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DirectWifiGreen)
                }

                Button(
                    onClick = onChat,
                    modifier = Modifier.weight(1f).testTag("chat_net_peer_${peer.ipAddress}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = TextPrimary)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CHAT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}
