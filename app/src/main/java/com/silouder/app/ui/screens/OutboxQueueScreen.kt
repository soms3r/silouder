package com.silouder.app.ui.screens

import androidx.compose.foundation.background
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
import com.silouder.app.model.UnifiedMessage
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.theme.*

@Composable
fun OutboxQueueScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val queuedMessages by viewModel.queuedMessages.collectAsStateWithLifecycle()
    val syncLog by viewModel.syncEngine.syncLog.collectAsStateWithLifecycle()
    val routingLog by viewModel.transportRouter.routingLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Outbox Flush Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("outbox_status_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Outbox, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(24.dp))
                            Column {
                                Text("STORE-AND-FORWARD BUFFER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                                Text("${queuedMessages.size} Messages Pending Route", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Button(
                            onClick = { viewModel.transportRouter.drainOutboxQueue() },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald, contentColor = Color(0xFF050B14)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("flush_outbox_button")
                        ) {
                            Text("FORCE FLUSH", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "PENDING OUTBOX PAYLOADS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        if (queuedMessages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TacticalEmerald, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Outbox buffer is empty", fontSize = 13.sp, color = TextPrimary)
                            Text("All messages transmitted & acknowledged", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        } else {
            items(queuedMessages) { msg ->
                QueuedMessageCard(msg = msg)
            }
        }

        item {
            Text(
                text = "ANTI-ENTROPY SYNC & ROUTING LOGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (routingLog.take(4) + syncLog.take(4)).forEach { line ->
                        Text(
                            text = line,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (line.contains("ROUTER")) CyberCyan else TacticalEmerald
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueuedMessageCard(msg: UnifiedMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: ${msg.messageId}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AmberAlert
                )
                Text(
                    text = "Target: ${msg.recipientId}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = msg.plaintext, fontSize = 12.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Route Hint: ${msg.transportRouteHint.name}", fontSize = 10.sp, color = CyberCyan)
                Text("Fragments: ${msg.fragmentCount}", fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}
