package com.silouder.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.theme.*

@Composable
fun KeyRingScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val identity by viewModel.myIdentity.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var newAlias by remember { mutableStateOf(identity.shortName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keyring_identity_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(24.dp))
                            Text("CRYPTOGRAPHIC KEY RING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberAlert)
                        }
                    }

                    // Node ID
                    Column {
                        Text("LoRa Mesh Node ID (Hex)", fontSize = 10.sp, color = TextMuted)
                        Text(identity.nodeId, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }

                    // Tor Onion Address
                    Column {
                        Text("Tor Onion Hidden Service (v3)", fontSize = 10.sp, color = TextMuted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(identity.onionAddress, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TorOnionPurple)
                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText("Onion Address", identity.onionAddress)
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                    Toast.makeText(context, "Onion address copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // X25519 Public Key
                    Column {
                        Text("X25519 Public Key (32-byte ECDH)", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = identity.publicKeyHex,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalEmerald,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // Regenerate Identity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("IDENTITY MANAGEMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Generate a new ephemeral cryptographic keypair and derived v3 onion address for stealth rotation.", fontSize = 11.sp, color = TextSecondary)

                    OutlinedTextField(
                        value = newAlias,
                        onValueChange = { newAlias = it },
                        label = { Text("Radio Call Sign / Alias") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("callsign_input")
                    )

                    Button(
                        onClick = { viewModel.regenerateIdentity(newAlias) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAlert, contentColor = Color(0xFF050B14)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("regenerate_keys_button")
                    ) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null, tint = Color(0xFF050B14), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REGENERATE SECURE KEYPAIR", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
