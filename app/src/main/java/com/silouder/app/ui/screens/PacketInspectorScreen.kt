package com.silouder.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.PacketDirection
import com.silouder.app.model.PacketTrace
import com.silouder.app.model.PacketType
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PacketInspectorScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val packets by viewModel.recentPackets.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selectedPacket by remember { mutableStateOf<PacketTrace?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PACKET PROTOCOL ANALYZER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Text("${packets.size} Raw Framed Packets Captured", fontSize = 11.sp, color = TextSecondary)
                }

                IconButton(
                    onClick = { scope.launch { viewModel.repository.clearPacketLogs() } },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear", tint = TextMuted)
                }
            }
        }

        items(packets) { packet ->
            PacketTraceCard(
                packet = packet,
                onClick = { selectedPacket = packet }
            )
        }
    }

    if (selectedPacket != null) {
        val p = selectedPacket!!
        AlertDialog(
            onDismissRequest = { selectedPacket = null },
            title = {
                Text("Packet Trace #${p.packetId}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type: ${p.packetType.name}", fontSize = 11.sp, color = CyberCyan, fontWeight = FontWeight.Bold)
                    Text("Summary: ${p.decodedSummary}", fontSize = 12.sp, color = TextPrimary)

                    Text("Raw Hex Dump:", fontSize = 10.sp, color = TextMuted)
                    Surface(color = DarkBackground, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = p.rawHex,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalEmerald,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SNR: ${p.snr}dB", fontSize = 11.sp, color = TextSecondary)
                        Text("RSSI: ${p.rssi}dBm", fontSize = 11.sp, color = TextSecondary)
                        Text("CRC: Valid", fontSize = 11.sp, color = TacticalEmerald)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedPacket = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PacketTraceCard(
    packet: PacketTrace,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("packet_trace_${packet.packetId}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (packet.direction == PacketDirection.TX_OUTGOING) AmberAlertContainer else CyberCyanContainer
                    ) {
                        Text(
                            text = if (packet.direction == PacketDirection.TX_OUTGOING) "TX" else "RX",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (packet.direction == PacketDirection.TX_OUTGOING) AmberAlert else CyberCyan,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "${packet.fromNode} -> ${packet.toNode}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "${packet.snr}dB",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TacticalEmerald
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = packet.decodedSummary,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = packet.rawHex,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
            )
        }
    }
}
