package com.silouder.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silouder.app.model.HardwareModel
import com.silouder.app.transport.meshtastic.BleConnectionState
import com.silouder.app.transport.meshtastic.RadioModemConfig
import com.silouder.app.ui.SilouderViewModel
import com.silouder.app.ui.theme.*

@Composable
fun RadioControlScreen(
    viewModel: SilouderViewModel,
    modifier: Modifier = Modifier
) {
    val bleState by viewModel.bleTransceiver.connectionState.collectAsStateWithLifecycle()
    val connectedNode by viewModel.bleTransceiver.connectedRadioNode.collectAsStateWithLifecycle()
    val modemConfig by viewModel.bleTransceiver.modemConfig.collectAsStateWithLifecycle()
    val isSimulated by viewModel.bleTransceiver.isSimulatedHardware.collectAsStateWithLifecycle()

    var freqText by remember(modemConfig.frequencyMhz) { mutableStateOf(modemConfig.frequencyMhz.toString()) }
    var selectedSf by remember(modemConfig.spreadingFactor) { mutableStateOf(modemConfig.spreadingFactor) }
    var txPower by remember(modemConfig.txPowerDbm) { mutableStateOf(modemConfig.txPowerDbm.toFloat()) }
    var hopLimit by remember(modemConfig.hopLimit) { mutableStateOf(modemConfig.hopLimit.toFloat()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
    ) {
        // BLE Connection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ble_radio_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = LoRaBlue, modifier = Modifier.size(24.dp))
                            Column {
                                Text("MESHTASTIC BLE GATT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LoRaBlue)
                                Text(
                                    text = if (bleState == BleConnectionState.CONNECTED_STREAMING) (connectedNode?.longName ?: "Connected Node") else "Radio Disconnected",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (bleState == BleConnectionState.CONNECTED_STREAMING) {
                                    viewModel.bleTransceiver.disconnect()
                                } else {
                                    viewModel.bleTransceiver.connectToDevice("!7a9f1b2c", "Heltec Tracker V3", HardwareModel.HELTEC_V3)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bleState == BleConnectionState.CONNECTED_STREAMING) SignalRed else CyberCyan,
                                contentColor = if (bleState == BleConnectionState.CONNECTED_STREAMING) Color.White else Color(0xFF050B14)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("ble_connect_toggle_button")
                        ) {
                            Text(
                                text = if (bleState == BleConnectionState.CONNECTED_STREAMING) "DISCONNECT" else "CONNECT BLE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkSurfaceHighlight)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Service UUID:", fontSize = 9.sp, color = TextMuted)
                            Text("6ba1b218-15a8-4e1f-9fa8-5d73d73b0d47", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hardware Simulation Driver", fontSize = 12.sp, color = TextPrimary)
                        Switch(
                            checked = isSimulated,
                            onCheckedChange = { viewModel.bleTransceiver.toggleHardwareSimulation(it) }
                        )
                    }
                }
            }
        }

        // LoRa RF Modem Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lora_modem_settings_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "LORA RF MODEM PARAMETERS (SX1262)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        letterSpacing = 0.8.sp
                    )

                    // Frequency
                    Column {
                        Text("Carrier Frequency (MHz)", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(915.0f, 868.0f, 433.0f).forEach { freq ->
                                FilterChip(
                                    selected = modemConfig.frequencyMhz == freq,
                                    onClick = {
                                        viewModel.bleTransceiver.updateModemConfig(modemConfig.copy(frequencyMhz = freq))
                                    },
                                    label = { Text("${freq} MHz") }
                                )
                            }
                        }
                    }

                    // Spreading Factor
                    Column {
                        Text("Spreading Factor (SF: ${selectedSf})", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = selectedSf.toFloat(),
                            onValueChange = {
                                selectedSf = it.toInt()
                                viewModel.bleTransceiver.updateModemConfig(modemConfig.copy(spreadingFactor = selectedSf))
                            },
                            valueRange = 7f..12f,
                            steps = 4
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SF7 (High Speed)", fontSize = 9.sp, color = TextMuted)
                            Text("SF12 (Max Distance / Penetration)", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    // TX Power
                    Column {
                        Text("TX Power Output: ${txPower.toInt()} dBm (${if (txPower >= 26) "High Power" else "Standard"})", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = txPower,
                            onValueChange = {
                                txPower = it
                                viewModel.bleTransceiver.updateModemConfig(modemConfig.copy(txPowerDbm = txPower.toInt()))
                            },
                            valueRange = 20f..30f,
                            steps = 9
                        )
                    }

                    // Hop Limit
                    Column {
                        Text("Mesh Max Hop Limit: ${hopLimit.toInt()} Hops", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = hopLimit,
                            onValueChange = {
                                hopLimit = it
                                viewModel.bleTransceiver.updateModemConfig(modemConfig.copy(hopLimit = hopLimit.toInt()))
                            },
                            valueRange = 1f..7f,
                            steps = 5
                        )
                    }
                }
            }
        }
    }
}
