package com.silouder.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.NatureAvatars
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.AppDisplayMode
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val appDisplayMode by viewModel.appDisplayMode.collectAsStateWithLifecycle()
    val showLoRaMesh by viewModel.showLoRaMesh.collectAsStateWithLifecycle()
    val showBluetoothSyncTab by viewModel.showBluetoothSyncTab.collectAsStateWithLifecycle()
    val showOnlineNetTab by viewModel.showOnlineNetTab.collectAsStateWithLifecycle()
    val showOutboxTab by viewModel.showOutboxTab.collectAsStateWithLifecycle()
    val showPacketInspector by viewModel.showPacketInspector.collectAsStateWithLifecycle()
    val showTopologyRadar by viewModel.showTopologyRadar.collectAsStateWithLifecycle()
    val showTechnicalMetricsInChat by viewModel.showTechnicalMetricsInChat.collectAsStateWithLifecycle()
    val disappearingTimerSeconds by viewModel.disappearingTimerSeconds.collectAsStateWithLifecycle()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val myIdentity by viewModel.myIdentity.collectAsStateWithLifecycle()

    val currentAvatar = NatureAvatars.getById(profile.avatarId)

    var showPanicWipeConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // User Profile Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.PROFILE) }
                    .testTag("settings_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.2.dp, currentAvatar.tintColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(currentAvatar.bgGradientColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(currentAvatar.icon, contentDescription = null, tint = currentAvatar.tintColor, modifier = Modifier.size(28.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(profile.displayName, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            Text(profile.customNumberOrTag, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = currentAvatar.tintColor)
                        }
                        Text(
                            text = profile.bioStatus,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Avatar: ${currentAvatar.name} • Tap to edit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyan
                        )
                    }

                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("EDIT", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        // Hero Section: App Display Preset Mode
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_mode_hero_card"),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyanContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("APP EXPERIENCE & PRESETS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberCyan, letterSpacing = 1.sp)
                                Text("Choose UI Complexity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (appDisplayMode) {
                                AppDisplayMode.STANDARD_MESSENGER -> TacticalEmeraldContainer
                                AppDisplayMode.TACTICAL_OPERATOR -> CyberCyanContainer
                                AppDisplayMode.CUSTOM -> AmberAlertContainer
                            }
                        ) {
                            Text(
                                text = when (appDisplayMode) {
                                    AppDisplayMode.STANDARD_MESSENGER -> "MESSENGER"
                                    AppDisplayMode.TACTICAL_OPERATOR -> "TACTICAL"
                                    AppDisplayMode.CUSTOM -> "CUSTOM"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (appDisplayMode) {
                                    AppDisplayMode.STANDARD_MESSENGER -> TacticalEmerald
                                    AppDisplayMode.TACTICAL_OPERATOR -> CyberCyan
                                    AppDisplayMode.CUSTOM -> AmberAlert
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Switch between a clean, everyday messaging app feel or a full tactical radio operator cockpit.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    // Preset Mode Cards
                    ModeSelectionCard(
                        title = "Standard Messenger Mode",
                        subtitle = "Clean everyday 1-on-1 & group messaging (Signal style). Hides complex radio/packet tech while maintaining full background E2EE security.",
                        icon = Icons.Filled.Forum,
                        iconColor = TacticalEmerald,
                        isSelected = appDisplayMode == AppDisplayMode.STANDARD_MESSENGER,
                        onClick = { viewModel.setAppDisplayMode(AppDisplayMode.STANDARD_MESSENGER) }
                    )

                    ModeSelectionCard(
                        title = "Tactical Mesh Operator Mode",
                        subtitle = "Full cockpit: LoRa RF modem controls, nearby Bluetooth sync, LAN socket scanner, topology radar, and wire packet sniffer.",
                        icon = Icons.Filled.CellTower,
                        iconColor = CyberCyan,
                        isSelected = appDisplayMode == AppDisplayMode.TACTICAL_OPERATOR,
                        onClick = { viewModel.setAppDisplayMode(AppDisplayMode.TACTICAL_OPERATOR) }
                    )
                }
            }
        }

        // Granular Feature Visibility Toggles
        item {
            Text(
                text = "FEATURE VISIBILITY CONTROLS (SHOW / HIDE TABS)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeatureToggleRow(
                        title = "Hardware LoRa Mesh Radio",
                        subtitle = "Show LoRa SX1262 modem settings & antenna icons in TopBar",
                        icon = Icons.Filled.CellTower,
                        iconColor = LoRaBlue,
                        checked = showLoRaMesh,
                        onCheckedChange = { viewModel.toggleLoRaMesh(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "Nearby Bluetooth Sync Tab",
                        subtitle = "Show Briar-style 1-tap offline proximity sync tab in bottom bar",
                        icon = Icons.Filled.BluetoothSearching,
                        iconColor = CyberCyan,
                        checked = showBluetoothSyncTab,
                        onCheckedChange = { viewModel.toggleBluetoothSyncTab(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "App-to-App Online Network Tab",
                        subtitle = "Show direct WiFi & LAN socket listener tab in bottom bar",
                        icon = Icons.Filled.Lan,
                        iconColor = DirectWifiGreen,
                        checked = showOnlineNetTab,
                        onCheckedChange = { viewModel.toggleOnlineNetTab(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "Store-and-Forward Outbox Tab",
                        subtitle = "Show offline queued messages tab in bottom bar",
                        icon = Icons.Filled.Outbox,
                        iconColor = AmberAlert,
                        checked = showOutboxTab,
                        onCheckedChange = { viewModel.toggleOutboxTab(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "Mesh Topology Radar Map",
                        subtitle = "Enable visual node topology radar in navigation",
                        icon = Icons.Filled.Radar,
                        iconColor = LoRaBlue,
                        checked = showTopologyRadar,
                        onCheckedChange = { viewModel.toggleTopologyRadar(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "Live Packet Wire Sniffer",
                        subtitle = "Show Wireshark-style raw hexadecimal packet inspector",
                        icon = Icons.Filled.Terminal,
                        iconColor = TacticalEmerald,
                        checked = showPacketInspector,
                        onCheckedChange = { viewModel.togglePacketInspector(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "Technical Metrics in Chat",
                        subtitle = "Display SNR dB, RSSI dBm, and mesh hop counters in message bubbles",
                        icon = Icons.Filled.Analytics,
                        iconColor = EncryptedPurple,
                        checked = showTechnicalMetricsInChat,
                        onCheckedChange = { viewModel.toggleTechnicalMetricsInChat(it) }
                    )
                }
            }
        }

        // Security, Encryption & Privacy Settings
        item {
            Text(
                text = "SECURITY, ENCRYPTION & PRIVACY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Disappearing Messages Timer
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(20.dp))
                            Text("Default Disappearing Messages", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text("Automatically burn messages after the recipient reads them", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                0 to "Off",
                                3600 to "1 Hour",
                                86400 to "24 Hours",
                                604800 to "7 Days"
                            ).forEach { (secs, label) ->
                                FilterChip(
                                    selected = disappearingTimerSeconds == secs,
                                    onClick = { viewModel.setDisappearingTimer(secs) },
                                    label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    FeatureToggleRow(
                        title = "Biometric & Screen Lock",
                        subtitle = "Require fingerprint or PIN when returning to Silouder",
                        icon = Icons.Filled.Fingerprint,
                        iconColor = CyberCyan,
                        checked = biometricLockEnabled,
                        onCheckedChange = { viewModel.toggleBiometricLock(it) }
                    )

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    // Keypair Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Identity / Node ID", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(myIdentity.nodeId, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                            Text("Onion: ${myIdentity.onionAddress}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TorOnionPurple)
                        }

                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.KEY_RING) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHighlight, contentColor = TextPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("MANAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Panic Emergency Wipe
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SignalRedContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.2.dp, SignalRed.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Dangerous, contentDescription = null, tint = SignalRed, modifier = Modifier.size(24.dp))
                        Text("EMERGENCY PANIC DATA WIPE", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = SignalRed)
                    }

                    Text(
                        text = "Instantly shred all local cryptographic keys, chats, contacts, outbox queues, and database records with zero recovery possibility.",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = { showPanicWipeConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SignalRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("panic_wipe_all_button")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WIPE & SHRED ALL LOCAL DATA", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showPanicWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showPanicWipeConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = SignalRed)
                    Text("Confirm Complete Data Wipe?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you absolutely sure? All messages, private keys, emergency channels, and offline packets will be deleted permanently.",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllLocalData {
                            Toast.makeText(context, "All data wiped successfully. Identity regenerated.", Toast.LENGTH_LONG).show()
                            showPanicWipeConfirm = false
                            viewModel.navigateTo(AppScreen.DASHBOARD)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed, contentColor = Color.White)
                ) {
                    Text("YES, WIPE EVERYTHING", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicWipeConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ModeSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) iconColor.copy(alpha = 0.12f) else DarkSurface,
        border = if (isSelected) BorderStroke(1.5.dp, iconColor) else BorderStroke(1.dp, DarkSurfaceHighlight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) iconColor.copy(alpha = 0.2f) else DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) iconColor else TextSecondary, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) iconColor else TextPrimary)
                Text(text = subtitle, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = iconColor)
            )
        }
    }
}

@Composable
fun FeatureToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (checked) iconColor.copy(alpha = 0.15f) else DarkSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = if (checked) iconColor else TextSecondary, modifier = Modifier.size(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(text = subtitle, fontSize = 10.sp, color = TextSecondary, lineHeight = 13.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
