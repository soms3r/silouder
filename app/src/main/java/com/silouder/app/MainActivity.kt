package com.silouder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.NatureAvatars
import com.silouder.app.transport.meshtastic.BleConnectionState
import com.silouder.app.transport.tor.TorStatus
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.screens.*
import com.silouder.app.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: SilouderViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilouderTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val btPeers by viewModel.bluetoothPeerManager.discoveredPeers.collectAsStateWithLifecycle()
                val netPeers by viewModel.networkPeerTransport.networkPeers.collectAsStateWithLifecycle()
                val bleState by viewModel.bleTransceiver.connectionState.collectAsStateWithLifecycle()
                val torStatus by viewModel.torSessionLayer.torStatus.collectAsStateWithLifecycle()
                val isInternetOnline by viewModel.torSessionLayer.isInternetReachable.collectAsStateWithLifecycle()
                val queuedMessages by viewModel.queuedMessages.collectAsStateWithLifecycle()
                val showLoRaMesh by viewModel.showLoRaMesh.collectAsStateWithLifecycle()
                val showBluetoothSyncTab by viewModel.showBluetoothSyncTab.collectAsStateWithLifecycle()
                val showOnlineNetTab by viewModel.showOnlineNetTab.collectAsStateWithLifecycle()
                val showOutboxTab by viewModel.showOutboxTab.collectAsStateWithLifecycle()
                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val currentAvatar = NatureAvatars.getById(userProfile.avatarId)

                var showOverflowMenu by remember { mutableStateOf(false) }

                val isSubScreen = currentScreen in listOf(
                    AppScreen.SETTINGS,
                    AppScreen.PROFILE,
                    AppScreen.RADIO_CONTROL,
                    AppScreen.ABOUT_APP,
                    AppScreen.SPECIFICATION_DOCS,
                    AppScreen.KEY_RING,
                    AppScreen.PACKET_INSPECTOR,
                    AppScreen.MESH_TOPOLOGY
                )

                val subScreenTitle = when (currentScreen) {
                    AppScreen.SETTINGS -> "Settings"
                    AppScreen.PROFILE -> "User Profile"
                    AppScreen.RADIO_CONTROL -> "LoRa Radio Control"
                    AppScreen.ABOUT_APP -> "About Silouder"
                    AppScreen.SPECIFICATION_DOCS -> "Protocol Specs"
                    AppScreen.KEY_RING -> "Key Ring & Identity"
                    AppScreen.PACKET_INSPECTOR -> "Packet Sniffer"
                    AppScreen.MESH_TOPOLOGY -> "Mesh Topology Radar"
                    else -> "Silouder"
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (currentScreen != AppScreen.CONVERSATION) {
                            TopAppBar(
                                navigationIcon = {
                                    if (isSubScreen) {
                                        IconButton(
                                            onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                            modifier = Modifier.testTag("top_bar_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = TextPrimary
                                            )
                                        }
                                    }
                                },
                                title = {
                                    if (isSubScreen) {
                                        Text(
                                            text = subScreenTitle,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "SILOUDER",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = CyberCyan,
                                                letterSpacing = 1.2.sp
                                            )

                                            // Unified Clean Security Status Pill
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = TacticalEmeraldContainer
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(TacticalEmerald)
                                                    )
                                                    Text(
                                                        text = when {
                                                            btPeers.any { it.isConnected } -> "BT ONLINE"
                                                            bleState == BleConnectionState.CONNECTED_STREAMING -> "LORA MESH"
                                                            netPeers.any { it.isOnline } -> "LAN ONLINE"
                                                            else -> "E2EE ACTIVE"
                                                        },
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = TacticalEmerald
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                actions = {
                                    // 1. Profile Nature Avatar Button
                                    IconButton(
                                        onClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                                        modifier = Modifier.testTag("open_profile_button")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(currentAvatar.bgGradientColor)
                                                .border(1.2.dp, currentAvatar.tintColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = currentAvatar.icon,
                                                contentDescription = "Profile",
                                                tint = currentAvatar.tintColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // 2. Clean 3-Dots Overflow Menu
                                    Box {
                                        IconButton(
                                            onClick = { showOverflowMenu = true },
                                            modifier = Modifier.testTag("top_bar_more_menu_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.MoreVert,
                                                contentDescription = "More Options",
                                                tint = TextPrimary
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showOverflowMenu,
                                            onDismissRequest = { showOverflowMenu = false },
                                            modifier = Modifier.background(DarkSurfaceElevated)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = CyberCyan) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.navigateTo(AppScreen.SETTINGS)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("User Profile", fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = currentAvatar.tintColor) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.navigateTo(AppScreen.PROFILE)
                                                }
                                            )
                                            if (showLoRaMesh) {
                                                DropdownMenuItem(
                                                    text = { Text("LoRa Radio Control", fontWeight = FontWeight.SemiBold) },
                                                    leadingIcon = { Icon(Icons.Filled.SettingsInputAntenna, contentDescription = null, tint = LoRaBlue) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        viewModel.navigateTo(AppScreen.RADIO_CONTROL)
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Key Ring & Identity", fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = EncryptedPurple) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.navigateTo(AppScreen.KEY_RING)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Architecture Specs", fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.MenuBook, contentDescription = null, tint = TextPrimary) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.navigateTo(AppScreen.SPECIFICATION_DOCS)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("About Silouder", fontWeight = FontWeight.SemiBold) },
                                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null, tint = AmberAlert) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.navigateTo(AppScreen.ABOUT_APP)
                                                }
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen != AppScreen.CONVERSATION) {
                            NavigationBar(
                                containerColor = DarkSurface,
                                contentColor = TextPrimary
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.DASHBOARD,
                                    onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Dashboard") },
                                    label = { Text("Status", fontSize = 9.sp) },
                                    modifier = Modifier.testTag("nav_tab_dashboard")
                                )

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.CHATS,
                                    onClick = { viewModel.navigateTo(AppScreen.CHATS) },
                                    icon = { Icon(Icons.Filled.Forum, contentDescription = "Messages") },
                                    label = { Text("Messages", fontSize = 9.sp) },
                                    modifier = Modifier.testTag("nav_tab_chats")
                                )

                                if (showBluetoothSyncTab) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.BLUETOOTH_SYNC,
                                        onClick = { viewModel.navigateTo(AppScreen.BLUETOOTH_SYNC) },
                                        icon = {
                                            BadgedBox(badge = {
                                                if (btPeers.isNotEmpty()) {
                                                    Badge(containerColor = CyberCyan, contentColor = Color(0xFF050B14)) {
                                                        Text("${btPeers.size}", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Filled.BluetoothSearching, contentDescription = "Nearby BT")
                                            }
                                        },
                                        label = { Text("Nearby BT", fontSize = 9.sp) },
                                        modifier = Modifier.testTag("nav_tab_bluetooth")
                                    )
                                }

                                if (showOnlineNetTab) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.NETWORK_PEERS,
                                        onClick = { viewModel.navigateTo(AppScreen.NETWORK_PEERS) },
                                        icon = {
                                            BadgedBox(badge = {
                                                if (netPeers.isNotEmpty()) {
                                                    Badge(containerColor = DirectWifiGreen, contentColor = Color(0xFF050B14)) {
                                                        Text("${netPeers.size}", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Filled.Lan, contentDescription = "Online Net")
                                            }
                                        },
                                        label = { Text("Online Net", fontSize = 9.sp) },
                                        modifier = Modifier.testTag("nav_tab_network")
                                    )
                                }

                                if (showOutboxTab) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.OUTBOX_QUEUE,
                                        onClick = { viewModel.navigateTo(AppScreen.OUTBOX_QUEUE) },
                                        icon = {
                                            BadgedBox(badge = {
                                                if (queuedMessages.isNotEmpty()) {
                                                    Badge(containerColor = AmberAlert, contentColor = Color(0xFF050B14)) {
                                                        Text("${queuedMessages.size}", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Filled.Outbox, contentDescription = "Outbox")
                                            }
                                        },
                                        label = { Text("Outbox", fontSize = 9.sp) },
                                        modifier = Modifier.testTag("nav_tab_outbox")
                                    )
                                }

                                NavigationBarItem(
                                    selected = currentScreen == AppScreen.SETTINGS,
                                    onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings", fontSize = 9.sp) },
                                    modifier = Modifier.testTag("nav_tab_settings")
                                )
                            }
                        }
                    },
                    containerColor = DarkBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                            AppScreen.CHATS -> ChatListScreen(viewModel = viewModel)
                            AppScreen.CONVERSATION -> ChatConversationScreen(viewModel = viewModel)
                            AppScreen.BLUETOOTH_SYNC -> BluetoothSyncScreen(viewModel = viewModel)
                            AppScreen.NETWORK_PEERS -> NetworkPeersScreen(viewModel = viewModel)
                            AppScreen.MESH_TOPOLOGY -> MeshTopologyScreen(viewModel = viewModel)
                            AppScreen.RADIO_CONTROL -> RadioControlScreen(viewModel = viewModel)
                            AppScreen.OUTBOX_QUEUE -> OutboxQueueScreen(viewModel = viewModel)
                            AppScreen.KEY_RING -> KeyRingScreen(viewModel = viewModel)
                            AppScreen.PACKET_INSPECTOR -> PacketInspectorScreen(viewModel = viewModel)
                            AppScreen.SPECIFICATION_DOCS -> SpecificationDocsScreen(viewModel = viewModel)
                            AppScreen.ABOUT_APP -> AboutScreen(viewModel = viewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
