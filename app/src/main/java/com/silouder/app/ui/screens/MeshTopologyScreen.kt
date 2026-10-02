package com.silouder.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.MeshNode
import com.silouder.app.model.NodeRole
import com.silouder.app.ui.AegisViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MeshTopologyScreen(
    viewModel: AegisViewModel,
    modifier: Modifier = Modifier
) {
    val nodes by viewModel.allNodes.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncEngine.syncStatus.collectAsStateWithLifecycle()

    var selectedNode by remember { mutableStateOf<MeshNode?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Radar Visualization Canvas
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .testTag("mesh_radar_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MeshRadarCanvas(
                        nodes = nodes,
                        onNodeClicked = { selectedNode = it }
                    )

                    // Overlay stats
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "MESH TOPOLOGY RADAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            text = "${nodes.size} Nodes in RF Range • Multi-hop Active",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Gossip Syncs: ${syncStatus.totalGossipExchanges}",
                            fontSize = 10.sp,
                            color = TacticalEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "DISCOVERED LORA NODES & ONION PEERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        items(nodes) { node ->
            NodeDetailCard(
                node = node,
                onInitiateSync = { viewModel.triggerAntiEntropySyncWithPeer(node.nodeId) },
                onDirectChat = {
                    viewModel.openConversation("chan_0_primary")
                }
            )
        }
    }
}

@Composable
fun MeshRadarCanvas(
    nodes: List<MeshNode>,
    onNodeClicked: (MeshNode) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(centerX, centerY) - 20

        // Draw concentric range rings
        for (i in 1..3) {
            val r = maxRadius * (i / 3f)
            drawCircle(
                color = Color(0xFF1E293B),
                radius = r,
                center = Offset(centerX, centerY),
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // Draw crosshairs
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(centerX, centerY - maxRadius),
            end = Offset(centerX, centerY + maxRadius),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(centerX - maxRadius, centerY),
            end = Offset(centerX + maxRadius, centerY),
            strokeWidth = 1f
        )

        // Draw Center Master Node
        drawCircle(
            color = CyberCyan,
            radius = 7.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = CyberCyan.copy(alpha = 0.3f),
            radius = 12.dp.toPx(),
            center = Offset(centerX, centerY)
        )

        // Draw surrounding mesh nodes
        nodes.forEachIndexed { index, node ->
            if (!node.isBleConnectedRadio) {
                val angleDeg = (index * (360f / maxOf(1, nodes.size - 1)) + 45f)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val distanceRatio = when (node.hopCount) {
                    0 -> 0.25f
                    1 -> 0.55f
                    else -> 0.85f
                }
                val nodeX = centerX + (maxRadius * distanceRatio * cos(angleRad)).toFloat()
                val nodeY = centerY + (maxRadius * distanceRatio * sin(angleRad)).toFloat()

                // Draw hop line to center or neighboring repeater
                drawLine(
                    color = CyberCyan.copy(alpha = 0.35f),
                    start = Offset(centerX, centerY),
                    end = Offset(nodeX, nodeY),
                    strokeWidth = 1.5f
                )

                // Node circle
                val nodeColor = when (node.role) {
                    NodeRole.ROUTER, NodeRole.REPEATER -> AmberAlert
                    NodeRole.CLIENT -> TacticalEmerald
                    else -> LoRaBlue
                }

                drawCircle(
                    color = nodeColor,
                    radius = 5.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }
        }
    }
}

@Composable
fun NodeDetailCard(
    node: MeshNode,
    onInitiateSync: () -> Unit,
    onDirectChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("node_card_${node.nodeId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (node.isBleConnectedRadio) CyberCyanContainer else DarkSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (node.isBleConnectedRadio) Icons.Filled.BluetoothConnected else Icons.Filled.Hub,
                            contentDescription = null,
                            tint = if (node.isBleConnectedRadio) CyberCyan else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = node.longName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (node.isBleConnectedRadio) {
                                Surface(shape = RoundedCornerShape(4.dp), color = CyberCyanContainer) {
                                    Text("LOCAL RADIO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyberCyan, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(text = "${node.nodeId} • ${node.hardwareModel.displayName.take(24)}", fontSize = 10.sp, color = TextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (node.role) {
                        NodeRole.ROUTER, NodeRole.REPEATER -> AmberAlertContainer
                        else -> TacticalEmeraldContainer
                    }
                ) {
                    Text(
                        text = node.role.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (node.role) {
                            NodeRole.ROUTER, NodeRole.REPEATER -> AmberAlert
                            else -> TacticalEmerald
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkSurfaceHighlight)
            Spacer(modifier = Modifier.height(10.dp))

            // Node Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SNR / RSSI", fontSize = 9.sp, color = TextMuted)
                    Text("${node.snr}dB / ${node.rssi}dBm", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberCyan)
                }
                Column {
                    Text("BATTERY", fontSize = 9.sp, color = TextMuted)
                    Text("${node.batteryLevel}% (${node.voltage}V)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TacticalEmerald)
                }
                Column {
                    Text("HOPS", fontSize = 9.sp, color = TextMuted)
                    Text(if (node.isBleConnectedRadio) "Direct (0)" else "${node.hopCount} Hop(s)", fontSize = 11.sp, color = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("AIRTIME DUTY", fontSize = 9.sp, color = TextMuted)
                    Text("${node.channelUtilization}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AmberAlert)
                }
            }

            if (!node.isBleConnectedRadio) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onInitiateSync,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalEmerald)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GOSSIP SYNC", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDirectChat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("MESSAGE", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
