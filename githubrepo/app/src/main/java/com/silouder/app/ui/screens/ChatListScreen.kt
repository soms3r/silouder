package com.silouder.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.ChannelType
import com.silouder.app.model.MeshChannel
import com.silouder.app.ui.AegisViewModel
import com.silouder.app.ui.theme.*

enum class ChatCategoryTab {
    DIRECT_CHATS,
    MESH_AND_GROUPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: AegisViewModel,
    modifier: Modifier = Modifier
) {
    val channels by viewModel.allChannels.collectAsStateWithLifecycle()
    val showCreateEmergencyModal by viewModel.showCreateEmergencyGroupModal.collectAsStateWithLifecycle()
    val showJoinModal by viewModel.showJoinGroupModal.collectAsStateWithLifecycle()
    val shareChannelModal by viewModel.shareGroupModalChannel.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(ChatCategoryTab.DIRECT_CHATS) }
    var searchQuery by remember { mutableStateOf("") }
    var showNewDirectChatDialog by remember { mutableStateOf(false) }

    // New Direct Chat Dialog fields
    var newContactName by remember { mutableStateOf("") }
    var newContactKey by remember { mutableStateOf("") }
    var newContactInitialMsg by remember { mutableStateOf("") }

    // Create Emergency Group State
    var emgGroupName by remember { mutableStateOf("") }
    var emgCustomCode by remember { mutableStateOf("") }
    var emgLocation by remember { mutableStateOf("") }
    var emgAutoWipeHours by remember { mutableIntStateOf(12) }
    var emgBleBeacon by remember { mutableStateOf(true) }

    // Join Group State
    var joinCodeInput by remember { mutableStateOf("") }
    var joinErrorMessage by remember { mutableStateOf<String?>(null) }

    val directChannels = remember(channels, searchQuery) {
        channels.filter { it.channelType == ChannelType.DIRECT_E2EE || it.channelType == ChannelType.DIRECT_BLUETOOTH_PEER || it.channelType == ChannelType.DIRECT_NETWORK_PEER }
            .filter { if (searchQuery.isBlank()) true else it.name.contains(searchQuery, ignoreCase = true) }
    }

    val meshAndGroupChannels = remember(channels, searchQuery) {
        channels.filter { it.channelType == ChannelType.EMERGENCY_GROUP || it.channelType == ChannelType.BROADCAST_PRIMARY || it.channelType == ChannelType.TACTICAL_SECONDARY }
            .filter { if (searchQuery.isBlank()) true else it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            if (selectedTab == ChatCategoryTab.DIRECT_CHATS) {
                FloatingActionButton(
                    onClick = { showNewDirectChatDialog = true },
                    containerColor = CyberCyan,
                    contentColor = Color(0xFF050B14),
                    modifier = Modifier.testTag("fab_new_direct_chat")
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "New Direct Chat")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search conversations & contacts...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chat_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkSurfaceHighlight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Top Category Tabs: "Direct Chats (1-on-1)" vs "Mesh & Groups"
            item {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = DarkSurface,
                    contentColor = CyberCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = if (selectedTab == ChatCategoryTab.DIRECT_CHATS) CyberCyan else SignalRed
                        )
                    },
                    divider = { HorizontalDivider(color = DarkSurfaceHighlight) }
                ) {
                    Tab(
                        selected = selectedTab == ChatCategoryTab.DIRECT_CHATS,
                        onClick = { selectedTab = ChatCategoryTab.DIRECT_CHATS },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Direct Chats (${directChannels.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = CyberCyan,
                        unselectedContentColor = TextSecondary
                    )

                    Tab(
                        selected = selectedTab == ChatCategoryTab.MESH_AND_GROUPS,
                        onClick = { selectedTab = ChatCategoryTab.MESH_AND_GROUPS },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Mesh & Groups (${meshAndGroupChannels.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        },
                        selectedContentColor = SignalRed,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            // TAB 1: DIRECT 1-ON-1 CHATS (REGULAR MESSAGING APP EXPERIENCE)
            if (selectedTab == ChatCategoryTab.DIRECT_CHATS) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = TacticalEmeraldContainer.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, TacticalEmerald.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(TacticalEmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = TacticalEmerald, modifier = Modifier.size(18.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("End-to-End Encrypted 1-on-1 Chats", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Signal Double-Ratchet protocol with forward secrecy & multi-transport routing", fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                if (directChannels.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(36.dp))
                                Text("No direct chats found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Start a secure conversation with a friend or colleague.", fontSize = 11.sp, color = TextSecondary)
                                Button(
                                    onClick = { showNewDirectChatDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14))
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("NEW SECURE CHAT", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(directChannels) { channel ->
                        ChannelItemCard(
                            channel = channel,
                            onClick = { viewModel.openConversation(channel.channelId) },
                            onShare = { viewModel.setShareGroupModal(channel) }
                        )
                    }
                }
            } else {
                // TAB 2: MESH & EMERGENCY CELLS
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_group_hub_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SignalRedContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.5.dp, SignalRed.copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                            .background(SignalRedContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Groups, contentDescription = null, tint = SignalRed, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text(
                                            text = "EMERGENCY & PROTEST GROUPS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = SignalRed,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = "Ad-Hoc Crowd Cells (Zero Infra)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SignalRedContainer
                                ) {
                                    Text(
                                        text = "AES-256 MESH",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Form instant encrypted communication cells in protests, concerts, or disasters. Anyone can join with a short secret code or QR scan.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 16.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.setShowCreateEmergencyGroupModal(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("create_emergency_group_btn")
                                ) {
                                    Icon(Icons.Filled.AddModerator, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CREATE GROUP", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setShowJoinGroupModal(true) },
                                    border = BorderStroke(1.5.dp, CyberCyan),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("join_by_code_btn")
                                ) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("JOIN BY CODE/QR", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = CyberCyan)
                                }
                            }
                        }
                    }
                }

                items(meshAndGroupChannels) { channel ->
                    ChannelItemCard(
                        channel = channel,
                        onClick = { viewModel.openConversation(channel.channelId) },
                        onShare = { viewModel.setShareGroupModal(channel) }
                    )
                }
            }
        }
    }

    // Modal: New Direct 1-on-1 Chat
    if (showNewDirectChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewDirectChatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = CyberCyan)
                    Text("New Direct E2EE Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Start a private conversation with another Silouder user.", fontSize = 12.sp, color = TextPrimary)

                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Contact Name / Nickname") },
                        placeholder = { Text("e.g. Emma Watson / Bob") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_contact_name_input")
                    )

                    OutlinedTextField(
                        value = newContactKey,
                        onValueChange = { newContactKey = it },
                        label = { Text("Silouder ID / Public Key / Onion") },
                        placeholder = { Text("e.g. !7a9f1b2c or node ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_contact_id_input")
                    )

                    OutlinedTextField(
                        value = newContactInitialMsg,
                        onValueChange = { newContactInitialMsg = it },
                        label = { Text("First Message (Optional)") },
                        placeholder = { Text("e.g. Hello! Starting our secure chat.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_contact_msg_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContactName.isNotBlank()) {
                            viewModel.createDirectContactChat(newContactName, newContactKey, newContactInitialMsg.ifBlank { null })
                            newContactName = ""
                            newContactKey = ""
                            newContactInitialMsg = ""
                            showNewDirectChatDialog = false
                            Toast.makeText(context, "Direct chat created!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14))
                ) {
                    Text("START CHAT", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDirectChatDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Modal 1: Create Emergency Group
    if (showCreateEmergencyModal) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowCreateEmergencyGroupModal(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = SignalRed)
                    Text("Create Emergency Group Cell", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Creates an ad-hoc group for crowds or tactical operations. An AES-256 group encryption key will be derived from the secret join code.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = emgGroupName,
                        onValueChange = { emgGroupName = it },
                        label = { Text("Group Name / Purpose") },
                        placeholder = { Text("e.g. Protest Medic Support / Gate B Cell") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("emg_name_input")
                    )

                    OutlinedTextField(
                        value = emgLocation,
                        onValueChange = { emgLocation = it },
                        label = { Text("Incident Location (Optional)") },
                        placeholder = { Text("e.g. North Plaza / Stage Left") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("emg_location_input")
                    )

                    OutlinedTextField(
                        value = emgCustomCode,
                        onValueChange = { emgCustomCode = it },
                        label = { Text("Secret Join Code (Auto-generates if empty)") },
                        placeholder = { Text("e.g. SILO-9824 or MED-77") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("emg_code_input")
                    )

                    // Auto Wipe Expiry Selection
                    Column {
                        Text("Auto-Wipe & Burn Timer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 12, 24, 0).forEach { hours ->
                                FilterChip(
                                    selected = emgAutoWipeHours == hours,
                                    onClick = { emgAutoWipeHours = hours },
                                    label = { Text(if (hours == 0) "Never" else "${hours}h", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bluetooth Beacon Invite", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Let nearby phones discover group", fontSize = 10.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = emgBleBeacon,
                            onCheckedChange = { emgBleBeacon = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createEmergencyGroup(
                            name = emgGroupName,
                            customJoinCode = emgCustomCode.ifBlank { null },
                            location = emgLocation,
                            autoWipeHours = emgAutoWipeHours,
                            enableBleBeacon = emgBleBeacon
                        )
                        emgGroupName = ""
                        emgCustomCode = ""
                        emgLocation = ""
                        viewModel.setShowCreateEmergencyGroupModal(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed, contentColor = Color.White)
                ) {
                    Text("INITIALIZE CELL", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowCreateEmergencyGroupModal(false) }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Modal 2: Join Group by Code or QR
    if (showJoinModal) {
        AlertDialog(
            onDismissRequest = {
                viewModel.setShowJoinGroupModal(false)
                joinErrorMessage = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = CyberCyan)
                    Text("Join Emergency Group Cell", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter the Secret Join Code shared by the group host or paste/scan a Silouder invite link.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = {
                            joinCodeInput = it
                            joinErrorMessage = null
                        },
                        label = { Text("Secret Join Code") },
                        placeholder = { Text("e.g. SILO-7721 or PROTEST-MED-77") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("join_code_input")
                    )

                    if (joinErrorMessage != null) {
                        Text(text = joinErrorMessage!!, color = SignalRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Quick Simulation Buttons for Demo / Protest Testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { joinCodeInput = "SILO-7721" },
                            label = { Text("Use Code: SILO-7721", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.joinGroupByCodeOrQr(joinCodeInput) { success, msg ->
                            if (success) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                joinCodeInput = ""
                                viewModel.setShowJoinGroupModal(false)
                            } else {
                                joinErrorMessage = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14))
                ) {
                    Text("JOIN GROUP", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setShowJoinGroupModal(false)
                    joinErrorMessage = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Modal 3: Share Emergency Group Invite (QR Code & Secret Code)
    if (shareChannelModal != null) {
        val ch = shareChannelModal!!
        val inviteUrl = "silouder://join-group?code=${ch.joinCode}&name=${java.net.URLEncoder.encode(ch.name, "UTF-8")}"

        AlertDialog(
            onDismissRequest = { viewModel.setShareGroupModal(null) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = AmberAlert)
                    Text("Share Group Invite", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Show this QR code or share the Secret Join Code with volunteers or friends.", fontSize = 12.sp, color = TextPrimary)

                    // Simulated QR Code Display
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(170.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(120.dp))
                                Text(ch.joinCode.ifEmpty { "SILOUDER-GROUP" }, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // Large Join Code Banner
                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, AmberAlert)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SECRET JOIN CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberAlert, letterSpacing = 1.sp)
                            Text(
                                text = ch.joinCode.ifEmpty { "OPEN-BROADCAST" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = AmberAlert
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = ClipData.newPlainText("Join Code", ch.joinCode)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, "Join Code copied!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.2.dp, AmberAlert),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COPY CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                        }

                        Button(
                            onClick = {
                                val clip = ClipData.newPlainText("Silouder Invite Link", inviteUrl)
                                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                Toast.makeText(context, "Invite link copied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAlert, contentColor = Color(0xFF050B14)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COPY LINK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.setShareGroupModal(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = TextPrimary)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ChannelItemCard(
    channel: MeshChannel,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    val isEmergency = channel.channelType == ChannelType.EMERGENCY_GROUP || channel.isEmergencyGroup
    val isDirectE2ee = channel.channelType == ChannelType.DIRECT_E2EE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("channel_card_${channel.channelId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated
        ),
        border = if (isEmergency) BorderStroke(1.5.dp, SignalRed.copy(alpha = 0.8f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (icon, iconColor, bgColor) = when (channel.channelType) {
                    ChannelType.EMERGENCY_GROUP -> Triple(Icons.Filled.Shield, SignalRed, SignalRedContainer)
                    ChannelType.BROADCAST_PRIMARY -> Triple(Icons.Filled.CellTower, LoRaBlue, CyberCyanContainer)
                    ChannelType.TACTICAL_SECONDARY -> Triple(Icons.Filled.Lock, EncryptedPurple, EncryptedPurpleContainer)
                    ChannelType.DIRECT_BLUETOOTH_PEER -> Triple(Icons.Filled.Bluetooth, CyberCyan, CyberCyanContainer)
                    ChannelType.DIRECT_NETWORK_PEER -> Triple(Icons.Filled.Lan, DirectWifiGreen, TacticalEmeraldContainer)
                    ChannelType.DIRECT_E2EE -> Triple(Icons.Filled.Person, CyberCyan, CyberCyanContainer)
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDirectE2ee) {
                        Text(
                            text = channel.name.take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyberCyan
                        )
                    } else {
                        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = channel.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isDirectE2ee) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = "E2EE Verified", tint = TacticalEmerald, modifier = Modifier.size(14.dp))
                            }
                        }

                        if (isEmergency) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SignalRedContainer,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "EMERGENCY CELL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (channel.channelType == ChannelType.BROADCAST_PRIMARY) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyberCyanContainer,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "PRIMARY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberCyan,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (isDirectE2ee) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TacticalEmeraldContainer,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "E2EE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TacticalEmerald,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (channel.lastMessagePreview.isNotEmpty()) channel.lastMessagePreview else "No messages yet",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (channel.unreadCount > 0) {
                    Badge(
                        containerColor = if (isEmergency) SignalRed else CyberCyan,
                        contentColor = if (isEmergency) Color.White else Color(0xFF050B14)
                    ) {
                        Text(text = "${channel.unreadCount}", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }

            if (isEmergency && channel.joinCode.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkSurfaceHighlight)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AmberAlertContainer,
                            border = BorderStroke(1.dp, AmberAlert)
                        ) {
                            Text(
                                text = "CODE: ${channel.joinCode}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (channel.memberCount > 0) {
                            Text(
                                text = "${channel.memberCount} Members in Cell",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp).testTag("share_group_btn_${channel.channelId}")
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "Share Invite", tint = AmberAlert, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
