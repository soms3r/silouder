package com.silouder.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.media.ActiveCallSession
import com.silouder.app.media.CallState
import com.silouder.app.media.CallType
import com.silouder.app.media.FileManager
import com.silouder.app.model.ChannelType
import com.silouder.app.model.MessageStatus
import com.silouder.app.model.TransportRouteHint
import com.silouder.app.model.UnifiedMessage
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatConversationScreen(
    viewModel: SilouderViewModel,
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

    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val voiceDuration by viewModel.voiceRecordingDuration.collectAsStateWithLifecycle()
    val callSession by viewModel.callSession.collectAsStateWithLifecycle()
    val isPttActive by viewModel.isPttActive.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showRouteMenu by remember { mutableStateOf(false) }
    var showCipherModalForMessage by remember { mutableStateOf<UnifiedMessage?>(null) }
    var showUrgentAlertModal by remember { mutableStateOf(false) }
    var urgentAlertText by remember { mutableStateOf("") }
    var showBurnConfirmModal by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }

    val isEmergency = currentChannel?.channelType == ChannelType.EMERGENCY_GROUP || (currentChannel?.isEmergencyGroup == true)

    // Activity Launchers for Attachments and Permissions
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedFile = viewModel.fileManager.copyUriToInternalStorage(uri, "photo_${System.currentTimeMillis()}.jpg")
            if (copiedFile != null) {
                viewModel.sendMessageWithAttachment("IMAGE", copiedFile)
                Toast.makeText(context, "Sending encrypted photo...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val copiedFile = viewModel.fileManager.copyUriToInternalStorage(uri, "doc_${System.currentTimeMillis()}")
            if (copiedFile != null) {
                viewModel.sendMessageWithAttachment("DOCUMENT", copiedFile)
                Toast.makeText(context, "Sending encrypted file...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = viewModel.startVoiceRecording()
            if (!started) {
                Toast.makeText(context, "Could not start voice recording", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val callAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && currentChannel != null) {
            val peerIp = currentChannel.peerNodeId?.removePrefix("!") ?: "192.168.1.120"
            viewModel.initiateCall(
                peerId = currentChannel.peerNodeId ?: "peer",
                peerName = currentChannel.name,
                peerIp = peerIp,
                isVideo = false
            )
        } else {
            Toast.makeText(context, "Microphone permission required for P2P calling", Toast.LENGTH_SHORT).show()
        }
    }

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
                        // P2P Audio Call Button
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val peerIp = currentChannel?.peerNodeId?.removePrefix("!") ?: "192.168.1.120"
                                    viewModel.initiateCall(
                                        peerId = currentChannel?.peerNodeId ?: "peer",
                                        peerName = currentChannel?.name ?: "Peer",
                                        peerIp = peerIp,
                                        isVideo = false
                                    )
                                } else {
                                    callAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("audio_call_button")
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = "P2P Audio Call", tint = TacticalEmerald)
                        }

                        // P2P Video Call Button
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val peerIp = currentChannel?.peerNodeId?.removePrefix("!") ?: "192.168.1.120"
                                    viewModel.initiateCall(
                                        peerId = currentChannel?.peerNodeId ?: "peer",
                                        peerName = currentChannel?.name ?: "Peer",
                                        peerIp = peerIp,
                                        isVideo = true
                                    )
                                } else {
                                    callAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.testTag("video_call_button")
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = "P2P Video Call", tint = CyberCyan)
                        }

                        // Simulate RX Button
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

                    if (isRecordingVoice) {
                        // Voice recording active bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SignalRed)
                            )
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d", voiceDuration / 60, voiceDuration % 60),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = SignalRed
                            )
                            Text(
                                text = "Recording Voice Note...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )

                            // Cancel voice note button
                            IconButton(
                                onClick = { viewModel.cancelVoiceRecording() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Cancel Recording", tint = SignalRed, modifier = Modifier.size(18.dp))
                            }

                            // Send voice note button
                            IconButton(
                                onClick = {
                                    viewModel.stopVoiceRecordingAndSend()
                                    coroutineScope.launch {
                                        if (conversationMessages.isNotEmpty()) {
                                            listState.animateScrollToItem(conversationMessages.size - 1)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(TacticalEmerald)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice Note", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        // Standard input row with Attach + Text Input + Mic / Send
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Attachment button
                            IconButton(
                                onClick = { showAttachmentPicker = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = "Attach File", tint = CyberCyan, modifier = Modifier.size(22.dp))
                            }

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

                            if (draft.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        viewModel.sendMessage()
                                        coroutineScope.launch {
                                            if (conversationMessages.isNotEmpty()) {
                                                listState.animateScrollToItem(conversationMessages.size - 1)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isEmergency) SignalRed else CyberCyan)
                                        .testTag("send_message_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = if (isEmergency) Color.White else Color(0xFF050B14),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                // Mic Button for Voice Note
                                IconButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            val started = viewModel.startVoiceRecording()
                                            if (!started) {
                                                Toast.makeText(context, "Could not start voice recorder", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceElevated)
                                        .testTag("voice_record_button")
                                ) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        contentDescription = "Record Voice Note",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attachment Picker Modal
    if (showAttachmentPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentPicker = false },
            containerColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Share Encrypted Media",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Send Photo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showAttachmentPicker = false
                                photoPickerLauncher.launch("image/*")
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CyberCyanContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "Photo", tint = CyberCyan, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Photo", fontSize = 12.sp, color = TextPrimary)
                    }

                    // Send Document
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showAttachmentPicker = false
                                documentPickerLauncher.launch("*/*")
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(TacticalEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.InsertDriveFile, contentDescription = "File", tint = TacticalEmerald, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("File / Doc", fontSize = 12.sp, color = TextPrimary)
                    }

                    // Push-to-Talk (PTT Walkie Talkie)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showAttachmentPicker = false
                                val targetIp = currentChannel?.peerNodeId?.removePrefix("!") ?: "192.168.1.120"
                                if (isPttActive) {
                                    viewModel.stopPtt()
                                    Toast.makeText(context, "Walkie-Talkie burst ended", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.startPtt(targetIp)
                                    Toast.makeText(context, "Walkie-Talkie burst active!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (isPttActive) SignalRedContainer else AmberAlert.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Radio,
                                contentDescription = "PTT Walkie-Talkie",
                                tint = if (isPttActive) SignalRed else AmberAlert,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isPttActive) "Stop PTT" else "PTT Walkie", fontSize = 12.sp, color = TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // In-Call Overlay / Dialog
    if (callSession != null) {
        InCallOverlay(
            session = callSession!!,
            onAnswer = { viewModel.answerCall() },
            onDecline = { viewModel.declineCall() },
            onEndCall = { viewModel.endCall() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() }
        )
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

                // Render Attachment if present
                when (message.attachmentType) {
                    "IMAGE" -> {
                        PhotoAttachmentView(filePath = message.attachmentPath)
                    }
                    "AUDIO_VOICE" -> {
                        VoiceNotePlayer(filePath = message.attachmentPath, durationMs = message.durationMs)
                    }
                    "DOCUMENT" -> {
                        DocumentAttachmentView(
                            fileName = message.attachmentName,
                            fileSize = message.attachmentSize,
                            filePath = message.attachmentPath
                        )
                    }
                }

                // Plaintext content (if not voice note)
                if (message.attachmentType != "AUDIO_VOICE") {
                    Text(
                        text = message.plaintext,
                        fontSize = 13.sp,
                        fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal,
                        color = TextPrimary
                    )
                }

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

                        // Status Delivery Icon (Signal / Telegram style receipts)
                        if (isMe) {
                            when (message.status) {
                                MessageStatus.QUEUED -> {
                                    Icon(Icons.Filled.Schedule, contentDescription = "Queued", tint = AmberAlert, modifier = Modifier.size(12.dp))
                                }
                                MessageStatus.TRANSMITTING -> {
                                    Icon(Icons.Filled.Sync, contentDescription = "Sending", tint = CyberCyan, modifier = Modifier.size(12.dp))
                                }
                                MessageStatus.DELIVERED_BLUETOOTH, MessageStatus.DELIVERED_NETWORK, MessageStatus.DELIVERED_MESH, MessageStatus.DELIVERED_TOR -> {
                                    Icon(Icons.Filled.Check, contentDescription = "Sent", tint = TacticalEmerald, modifier = Modifier.size(13.dp))
                                }
                                MessageStatus.ACK_RECEIVED -> {
                                    Icon(Icons.Filled.DoneAll, contentDescription = "Delivered", tint = CyberCyan, modifier = Modifier.size(14.dp))
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceNotePlayer(filePath: String?, durationMs: Long) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(filePath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                if (filePath.isNullOrBlank()) {
                    Toast.makeText(context, "Voice note audio data not loaded", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                val f = File(filePath)
                if (!f.exists()) {
                    Toast.makeText(context, "Voice note file not found", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }

                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        try {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(filePath)
                                prepare()
                                setOnCompletionListener {
                                    isPlaying = false
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return@IconButton
                        }
                    }
                    mediaPlayer?.start()
                    isPlaying = true
                }
            },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(TacticalEmerald)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val heights = listOf(4, 8, 12, 16, 10, 14, 8, 6, 12, 14, 16, 10, 6, 8, 14, 12, 6, 10, 8, 4)
                for (h in heights) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(h.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isPlaying) TacticalEmerald else TextSecondary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val secs = if (durationMs > 0) durationMs / 1000 else 0
            Text(
                text = String.format(java.util.Locale.US, "%02d:%02d • Encrypted Voice Note", secs / 60, secs % 60),
                fontSize = 9.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun PhotoAttachmentView(filePath: String?) {
    val bitmap = remember(filePath) {
        if (!filePath.isNullOrBlank()) {
            try {
                val f = File(filePath)
                if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Encrypted Photo",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground.copy(alpha = 0.5f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
            Text("📷 Photo Attachment", fontSize = 12.sp, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun DocumentAttachmentView(fileName: String?, fileSize: Long, filePath: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CyberCyanContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName ?: "Encrypted File",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${FileManager.formatFileSize(fileSize)} • E2EE",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
        Icon(Icons.Filled.FileDownload, contentDescription = "Download/Open", tint = TacticalEmerald, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun InCallOverlay(
    session: ActiveCallSession,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Modal active */ },
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (session.callType == CallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Call,
                    contentDescription = null,
                    tint = if (session.callState == CallState.CONNECTED) TacticalEmerald else AmberAlert
                )
                Text(
                    text = when (session.callState) {
                        CallState.INCOMING_RINGING -> "Incoming ${session.callType.name} Call"
                        CallState.OUTGOING_RINGING -> "Calling..."
                        CallState.CONNECTED -> "P2P ${session.callType.name} Call Active"
                        else -> "Call"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CyberCyanContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (session.callType == CallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Person,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = session.peerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Direct Peer IP: ${session.peerIp}",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                if (session.callState == CallState.CONNECTED) {
                    Text(
                        text = String.format(java.util.Locale.US, "%02d:%02d", session.durationSeconds / 60, session.durationSeconds % 60),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalEmerald
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute button
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (session.isMuted) SignalRedContainer else DarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = if (session.isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Mute",
                                tint = if (session.isMuted) SignalRed else TextPrimary
                            )
                        }

                        // Speaker button
                        IconButton(
                            onClick = onToggleSpeaker,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (session.isSpeakerOn) CyberCyanContainer else DarkSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = if (session.isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                                contentDescription = "Speaker",
                                tint = if (session.isSpeakerOn) CyberCyan else TextPrimary
                            )
                        }
                    }
                } else if (session.callState == CallState.OUTGOING_RINGING) {
                    Text(
                        text = "Establishing AES-256 UDP stream...",
                        fontSize = 12.sp,
                        color = AmberAlert
                    )
                }
            }
        },
        confirmButton = {
            if (session.callState == CallState.INCOMING_RINGING) {
                Button(
                    onClick = onAnswer,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalEmerald)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Answer")
                }
            } else {
                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed)
                ) {
                    Icon(Icons.Filled.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("End Call")
                }
            }
        },
        dismissButton = {
            if (session.callState == CallState.INCOMING_RINGING) {
                TextButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.textButtonColors(contentColor = SignalRed)
                ) {
                    Text("Decline")
                }
            }
        }
    )
}
