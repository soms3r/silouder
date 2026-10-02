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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.NatureAvatar
import com.silouder.app.model.NatureAvatars
import com.silouder.app.ui.AegisViewModel
import com.silouder.app.ui.AppScreen
import com.silouder.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AegisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val myIdentity by viewModel.myIdentity.collectAsStateWithLifecycle()

    var nameInput by remember(profile) { mutableStateOf(profile.displayName) }
    var numberInput by remember(profile) { mutableStateOf(profile.customNumberOrTag) }
    var bioInput by remember(profile) { mutableStateOf(profile.bioStatus) }
    var selectedAvatarId by remember(profile) { mutableStateOf(profile.avatarId) }

    var showQrDialog by remember { mutableStateOf(false) }

    val currentAvatar = remember(selectedAvatarId) {
        NatureAvatars.getById(selectedAvatarId)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // Hero Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_hero_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = BorderStroke(1.2.dp, currentAvatar.tintColor.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar Icon with Glowing Gradient Circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(currentAvatar.bgGradientColor, DarkSurface)
                                )
                            )
                            .border(2.dp, currentAvatar.tintColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentAvatar.icon,
                            contentDescription = currentAvatar.name,
                            tint = currentAvatar.tintColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = nameInput.ifBlank { "Anonymous User" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Icon(Icons.Filled.VerifiedUser, contentDescription = "Verified E2EE", tint = TacticalEmerald, modifier = Modifier.size(16.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = numberInput.ifBlank { "#7721" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentAvatar.tintColor,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("•", color = TextMuted)
                            Text(
                                text = currentAvatar.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bioInput.ifBlank { "Decentralized • Off-Grid • Forward Secret" },
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    HorizontalDivider(color = DarkSurfaceHighlight)

                    // Node ID & In-Person QR Code Handshake Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PUBLIC CRYPTO ID", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextMuted, letterSpacing = 1.sp)
                            Text(
                                text = myIdentity.nodeId,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showQrDialog = true },
                                border = BorderStroke(1.2.dp, AmberAlert),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MY QR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                            }

                            Button(
                                onClick = { viewModel.autoGenerateProfile() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("randomize_profile_btn")
                            ) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RANDOMIZE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Edit Details
        item {
            Text(
                text = "USER PROFILE DETAILS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        // Edit Profile Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name / Alias") },
                        placeholder = { Text("e.g. Alpine Frost / Emma") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = CyberCyan) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceHighlight
                        )
                    )

                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = { numberInput = it },
                        label = { Text("Custom Number / Radio Handle / Callsign") },
                        placeholder = { Text("e.g. +1-987-555-0149 or #7721") },
                        leadingIcon = { Icon(Icons.Filled.Tag, contentDescription = null, tint = AmberAlert) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_number_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceHighlight
                        )
                    )

                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("Bio / Status Quote") },
                        placeholder = { Text("e.g. Decentralized • Off-Grid • Forward Secret") },
                        leadingIcon = { Icon(Icons.Filled.Notes, contentDescription = null, tint = TacticalEmerald) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("profile_bio_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = DarkSurfaceHighlight
                        )
                    )
                }
            }
        }

        // Section Title: Premade Nature Avatar Icons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHOOSE NATURE AVATAR ICON",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Pure Nature • No Animals",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TacticalEmerald
                )
            }
        }

        // Nature Avatars Grid (12 curated nature elements)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val rows = NatureAvatars.ALL.chunked(3)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { avatar ->
                                val isSelected = avatar.id == selectedAvatarId
                                Surface(
                                    onClick = { selectedAvatarId = avatar.id },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) avatar.bgGradientColor else DarkSurface,
                                    border = if (isSelected) BorderStroke(2.dp, avatar.tintColor) else BorderStroke(1.dp, DarkSurfaceHighlight),
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = avatar.icon,
                                            contentDescription = avatar.name,
                                            tint = if (isSelected) avatar.tintColor else TextSecondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = avatar.name,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save Changes Button
        item {
            Button(
                onClick = {
                    viewModel.updateUserProfile(
                        displayName = nameInput,
                        customNumberOrTag = numberInput,
                        bioStatus = bioInput,
                        avatarId = selectedAvatarId
                    )
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE PROFILE CHANGES", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }
    }

    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = AmberAlert)
                    Text("In-Person Identity QR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Scan to add ${profile.displayName} (${profile.customNumberOrTag}) as a verified E2EE contact.", fontSize = 12.sp, color = TextPrimary)

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(170.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(10.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.QrCode, contentDescription = null, tint = Color.Black, modifier = Modifier.size(120.dp))
                                Text(myIdentity.nodeId, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Node ID: ${myIdentity.nodeId}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF050B14))
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
