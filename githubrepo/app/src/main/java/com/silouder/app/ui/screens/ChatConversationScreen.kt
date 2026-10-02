package com.silouder.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.ChannelType
import com.silouder.app.model.MessageStatus
import com.silouder.app.model.TransportRouteHint
import com.silouder.app.model.UnifiedMessage
import com.silouder.app.ui.AegisViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    viewModel: AegisViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.CHATS)
    }

    val context = LocalContext.current
    val convId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val nodes by viewModel.allNodes.collectAsStateWithLifecycle()
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val draft by viewModel.messageDraft.collectAsStateWithLifecycle()
    val selectedRoute by viewModel.selectedRouteHint.collectAsStateWithLifecycle()

    val currentChannel = channels.find { it.channelId == convId } ?: channels.firstOrNull()
    val conversationMessages = remember(allMessages, convId) {
        allMessages.filter { it.conversationId == convId }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showRouteMenu by remember { mutableStateOf(false) }
    var showCipherModalForMessage by remember { mutableStateOf<UnifiedMessage?>(null) }
    var showUrgentAlertModal by remember { mutableStateOf(false) }
    var urgentAlertText by remember { mutableStateOf("") }
    var showBurnConfirmModal by remember { mutableStateOf(false) }

    val isEmergency = currentChannel?.channelType == ChannelType.EMERGENCY_GROUP || (currentChannel?.isEmergencyGroup == true)

    LaunchedEffect(conversationMessages.size) {
        if (conversationMessages.isNotEmpty()) {
            listState.animateScrollToItem(conversationMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = currentChannel?.name ?: "Secure Channel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = if (isEmergency) Icons.Filled.Shield else Icons.Filled.Lock,
                                contentDescription = "E2EE",
                                tint = if (isEmergency) SignalRed else TacticalEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = when {
                                isEmergency -> "Join Code: ${currentChannel?.joinCode} • ${currentChannel?.memberCount} Members in Cell"
                                currentChannel?.channelType == ChannelType.BROADCAST_PRIMARY -> "LoRa Primary • LongFast • 915.0 MHz"
                                currentChannel?.channelType == ChannelType.DIRECT_BLUETOOTH_PEER -> "Nearby Direct Bluetooth • 1-Tap Offline Sync"
                                currentChannel?.channelType == ChannelType.DIRECT_NETWORK_PEER -> "App-to-App Online Network • TCP Socket"
                                currentChannel?.channelType == ChannelType.TACTICAL_SECONDARY -> "Tactical AES-256 • Pre-Shared Key"
                                currentChannel?.channelType == ChannelType.DIRECT_E2EE -> "Briar Onion E2EE • Ratchet Session"
                                else -> "Silouder Secure Channel"
                            },
                            fontSize = 11.sp,
                            color = if (isEmergency) AmberAlert else TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CHATS) },
                        modifier = Modifier.testTag("conversation_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (isEmergency) {
                        // Share QR / Join Code
                        IconButton(
                            onClick = { viewModel.setShareGroupModal(currentChannel) },
                            modifier = Modifier.testTag("emg_share_invite_top_btn")
                        ) {
                            Icon(Icons.Filled.QrCode2, contentDescription = "Share Invite", tint = AmberAlert)
                        }

                        // Urgent Alert broadcast
                        IconButton(
                            onClick = { showUrgentAlertModal = true },
                            modifier = Modifier.testTag("emg_urgent_alert_btn")
                        ) {
                            Icon(Icons.Filled.NotificationImportant, contentDescription = "Urgent Alert", tint = SignalRed)
                        }

                        // Burn & Leave Group
                        IconButton(
                            onClick = { showBurnConfirmModal = true },
                            modifier = Modifier.testTag("emg_burn_group_btn")
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Burn Group", tint = TextMuted)
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (currentChannel?.channelType == ChannelType.DIRECT_NETWORK_PEER) {
                                    viewModel.simulateIncomingNetworkMessage(
                                        senderAlias = "Station Echo",
                                        senderIp = "192.168.1.120:8888",
                                        text = "Online socket ACK: Message received with 12ms latency."
                                    )
                                } else {
                                    val peer = nodes.firstOrNull { !it.isBleConnectedRadio } ?: nodes.first()
                                    val replies = listOf(
                                        "Roger that. Silouder packet verified over direct link.",
                                        "Sync vector confirmed. 0 missing delta packets.",
                                        "Direct delivery confirmed. Encrypted payload intact."
                                    )
                                    viewModel.simulateIncomingMeshMessage(peer, replies.random())
                                }
                            },
                            modifier = Modifier.testTag("simulate_reply_button")
                        ) {
                            Icon(Icons.Filled.Sensors, contentDescription = "Simulate RX", tint = CyberCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Channel Info Strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isEmergency) SignalRedContainer.copy(alpha = 0.3f) else DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isEmergency) SignalRed else TacticalEmerald)
                        )
                        Text(
                            text = if (isEmergency) "Ad-Hoc Emergency Group • AES-256 Mesh" else "Zero-Metadata Local Storage (SQLCipher Model)",
                            fontSize = 10.sp,
                            fontWeight = if (isEmergency) FontWeight.Bold else FontWeight.Normal,
                            color = if (isEmergency) SignalRed else TacticalEmerald
                        )
                    }
                    Text(
                        text = if (isEmergency) "Broadcast: Bluetooth+LAN+LoRa" else "Auto-Store & Forward",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            // Message stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (conversationMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isEmergency) Icons.Filled.Groups else Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (isEmergency) SignalRed else TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isEmergency) "Emergency Group Ready" else "End-to-End Encrypted Session",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEmergency) "Share Join Code ${currentChannel?.joinCode} to invite crowd members." else "Send a message via Bluetooth, Online LAN, LoRa, or Tor.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                items(conversationMessages) { msg ->
                    MessageBubble(
                        message = msg,
                        onViewCiphertext = { showCipherModalForMessage = msg }
                    )
                }
            }

            // Message Composer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Route Selector Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceElevated)
                                .clickable { showRouteMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("route_selector_button")
                        ) {
                            Icon(
                                imageVector = when (selectedRoute) {
                                    TransportRouteHint.AUTO_BEST -> Icons.Filled.AutoAwesome
                                    TransportRouteHint.BLUETOOTH_P2P -> Icons.Filled.Bluetooth
                                    TransportRouteHint.ONLINE_NETWORK -> Icons.Filled.Lan
                                    TransportRouteHint.LORA_MESH -> Icons.Filled.CellTower
                                    TransportRouteHint.TOR_ONION -> Icons.Filled.VpnLock
                                    TransportRouteHint.STORE_FORWARD -> Icons.Filled.Outbox
                                    else -> Icons.Filled.Route
                                },
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = when (selectedRoute) {
                                    TransportRouteHint.BLUETOOTH_P2P -> CyberCyan
                                    TransportRouteHint.ONLINE_NETWORK -> DirectWifiGreen
                                    TransportRouteHint.LORA_MESH -> LoRaBlue
                                    TransportRouteHint.TOR_ONION -> TorOnionPurple
                                    else -> CyberCyan
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Route: ${selectedRoute.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberCyan
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondary)
                        }

                        DropdownMenu(
                            expanded = showRouteMenu,
                            onDismissRequest = { showRouteMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("AUTO_BEST (Smart Multi-Transport)") },
                                onClick = {
                                    viewModel.setSelectedRouteHint(TransportRouteHint.AUTO_BEST)
                                    showRouteMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("BLUETOOTH_P2P (Nearby Briar BT)") },
                                onClick = {
                                    viewModel.setSelectedRouteHint(TransportRouteHint.BLUETOOTH_P2P)
                                    showRouteMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ONLINE_NETWORK (App-to-App LAN)") },
                                onClick = {
                                    viewModel.setSelectedRouteHint(TransportRouteHint.ONLINE_NETWORK)
                                    showRouteMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("LORA_MESH (Meshtastic SX1262)") },
                                onClick = {
                                    viewModel.setSelectedRouteHint(TransportRouteHint.LORA_MESH)
                                    showRouteMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("TOR_ONION (v3 Hidden Socket)") },
                                onClick = {
                                    viewModel.setSelectedRouteHint(TransportRouteHint.TOR_ONION)
                                    showRouteMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("STORE_FORWARD (Offline Queue)") },
                                onClick = {
                                    viewModel.setSelectedRouteHint(TransportRouteHint.STORE_FORWARD)
                                    showRouteMenu = false
                                }
                            )
                        }

                        if (isEmergency) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SignalRed))
                                Text("CROWD BROADCAST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SignalRed)
                            }
                        } else {
                            Text(
                                text = "${draft.length}/230B",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (draft.length > 200) AmberAlert else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { viewModel.setMessageDraft(it) },
                            placeholder = {
                                Text(
                                    if (isEmergency) "Write to emergency group..." else "Write encrypted message...",
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated,
                                focusedBorderColor = if (isEmergency) SignalRed else CyberCyan,
                                unfocusedBorderColor = DarkSurfaceHighlight
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )

                        IconButton(
                            onClick = {
                                viewModel.sendMessage()
                                coroutineScope.launch {
                                    if (conversationMessages.isNotEmpty()) {
                                        listState.animateScrollToItem(conversationMessages.size - 1)
                                    }
                                }
                            },
                            enabled = draft.isNotBlank(),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (draft.isNotBlank()) (if (isEmergency) SignalRed else CyberCyan) else DarkSurfaceHighlight)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (draft.isNotBlank()) (if (isEmergency) Color.White else Color(0xFF050B14)) else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Urgent Group Alert Broadcast
    if (showUrgentAlertModal) {
        AlertDialog(
            onDismissRequest = { showUrgentAlertModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.NotificationImportant, contentDescription = null, tint = SignalRed)
                    Text("Broadcast Urgent Alert", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This alert will be broadcasted with maximum priority across all nearby Bluetooth devices, LAN sockets, and LoRa mesh repeaters.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = urgentAlertText,
                        onValueChange = { urgentAlertText = it },
                        label = { Text("Emergency Notice / Evac Route") },
                        placeholder = { Text("e.g. Evacuate North Gate, First Aid at Plaza B") },
                        modifier = Modifier.fillMaxWidth().testTag("urgent_alert_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (urgentAlertText.isNotBlank()) {
                            viewModel.sendUrgentGroupAlert(urgentAlertText)
                            urgentAlertText = ""
                            showUrgentAlertModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed, contentColor = Color.White)
                ) {
                    Text("BROADCAST ALERT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrgentAlertModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Burn & Leave Confirmation
    if (showBurnConfirmModal) {
        AlertDialog(
            onDismissRequest = { showBurnConfirmModal = false },
            title = { Text("Burn & Leave Emergency Group?") },
            text = {
                Text("This will cryptographically erase all messages, session keys, and history for this group from your local device storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentChannel != null) {
                            viewModel.leaveAndBurnGroup(currentChannel.channelId)
                            Toast.makeText(context, "Group securely burned from storage", Toast.LENGTH_SHORT).show()
                        }
                        showBurnConfirmModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed)
                ) {
                    Text("BURN & LEAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBurnConfirmModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal inspecting raw cryptographic payload
    if (showCipherModalForMessage != null) {
        val msg = showCipherModalForMessage!!
        AlertDialog(
            onDismissRequest = { showCipherModalForMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = CyberCyan)
                    Text("Cryptographic Envelope", fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AEAD Cipher: AES-256-GCM + HMAC-SHA256", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalEmerald)

                    Text("Decrypted Plaintext:", fontSize = 10.sp, color = TextMuted)
                    Surface(color = DarkBackground, shape = RoundedCornerShape(6.dp)) {
                        Text(msg.plaintext, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.padding(8.dp))
                    }

                    Text("Ciphertext (Base64):", fontSize = 10.sp, color = TextMuted)
                    Surface(color = DarkBackground, shape = RoundedCornerShape(6.dp)) {
                        Text(msg.payloadCiphertext, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberCyan, modifier = Modifier.padding(8.dp))
                    }

                    Text("Initialization Vector (IV):", fontSize = 10.sp, color = TextMuted)
                    Text(msg.ivBase64, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = AmberAlert)

                    Text("Auth Signature Tag:", fontSize = 10.sp, color = TextMuted)
                    Text(msg.macSignature.take(24) + "...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = EncryptedPurple)
                }
            },
            confirmButton = {
                Button(onClick = { showCipherModalForMessage = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: UnifiedMessage,
    onViewCiphertext: () -> Unit
) {
    val isMe = !message.isIncoming
    val isUrgent = message.isUrgentAlert

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isMe) "YOU" else message.senderName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUrgent) SignalRed else if (isMe) CyberCyan else LoRaBlue
            )
            Text(
                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(message.timestamp)),
                fontSize = 9.sp,
                color = TextMuted
            )
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isUrgent -> SignalRedContainer.copy(alpha = 0.85f)
                    isMe -> CyberCyanContainer.copy(alpha = 0.85f)
                    else -> DarkSurfaceElevated
                }
            ),
            border = if (isUrgent) androidx.compose.foundation.BorderStroke(1.5.dp, SignalRed) else null,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .testTag("message_bubble_${message.messageId}")
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (isUrgent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Filled.NotificationImportant, contentDescription = null, tint = SignalRed, modifier = Modifier.size(16.dp))
                        Text("HIGH PRIORITY ALERT", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = SignalRed)
                    }
                }

                Text(
                    text = message.plaintext,
                    fontSize = 13.sp,
                    fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Transport Badges & Crypto Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val transport = message.actualTransport ?: message.transportRouteHint
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (transport) {
                                TransportRouteHint.BLUETOOTH_P2P -> CyberCyan.copy(alpha = 0.2f)
                                TransportRouteHint.ONLINE_NETWORK -> DirectWifiGreen.copy(alpha = 0.2f)
                                TransportRouteHint.LORA_MESH -> LoRaBlue.copy(alpha = 0.2f)
                                TransportRouteHint.TOR_ONION -> TorOnionPurple.copy(alpha = 0.2f)
                                else -> AmberAlert.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when (transport) {
                                    TransportRouteHint.BLUETOOTH_P2P -> "Bluetooth P2P"
                                    TransportRouteHint.ONLINE_NETWORK -> "Online Net"
                                    TransportRouteHint.LORA_MESH -> "LoRa Mesh"
                                    TransportRouteHint.TOR_ONION -> "Tor Onion"
                                    TransportRouteHint.STORE_FORWARD -> "Outbox Queued"
                                    else -> "Auto Route"
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (transport) {
                                    TransportRouteHint.BLUETOOTH_P2P -> CyberCyan
                                    TransportRouteHint.ONLINE_NETWORK -> DirectWifiGreen
                                    TransportRouteHint.LORA_MESH -> LoRaBlue
                                    TransportRouteHint.TOR_ONION -> TorOnionPurple
                                    else -> AmberAlert
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (message.hopsTraveled > 0) {
                            Text(
                                text = "${message.hopsTraveled} Hop${if (message.hopsTraveled > 1) "s" else ""}",
                                fontSize = 8.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Inspect crypto button
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = "Inspect Cipher",
                            tint = if (isUrgent) SignalRed else TacticalEmerald,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable(onClick = onViewCiphertext)
                        )

                        // Status Icon
                        when (message.status) {
                            MessageStatus.QUEUED -> Icon(Icons.Filled.HourglassEmpty, contentDescription = "Queued", tint = AmberAlert, modifier = Modifier.size(12.dp))
                            MessageStatus.TRANSMITTING -> Icon(Icons.Filled.Sync, contentDescription = "Sending", tint = CyberCyan, modifier = Modifier.size(12.dp))
                            MessageStatus.DELIVERED_BLUETOOTH, MessageStatus.DELIVERED_NETWORK, MessageStatus.DELIVERED_MESH, MessageStatus.DELIVERED_TOR -> Icon(Icons.Filled.Check, contentDescription = "Delivered", tint = TacticalEmerald, modifier = Modifier.size(12.dp))
                            MessageStatus.ACK_RECEIVED -> Icon(Icons.Filled.DoneAll, contentDescription = "ACK", tint = CyberCyan, modifier = Modifier.size(12.dp))
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
