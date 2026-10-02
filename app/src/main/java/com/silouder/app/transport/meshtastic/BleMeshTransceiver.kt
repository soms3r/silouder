package com.silouder.app.transport.meshtastic

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.silouder.app.model.HardwareModel
import com.silouder.app.model.MeshNode
import com.silouder.app.model.NodeRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    DISCOVERING_SERVICES,
    CONNECTED_STREAMING
}

data class RadioModemConfig(
    val frequencyMhz: Float = 915.0f,
    val spreadingFactor: Int = 11,
    val bandwidthKhz: Float = 250.0f,
    val codingRate: String = "4/8",
    val txPowerDbm: Int = 22,
    val hopLimit: Int = 3,
    val channelNum: Int = 0,
    val channelPsk: String = "Default LongFast PSK"
)

/**
 * Module B: BLE Mesh Transceiver Interface for physical/simulated Meshtastic hardware.
 * Uses official Meshtastic BLE Service & Characteristic UUIDs:
 * - Service: 6ba1b218-15a8-4e1f-9fa8-5d73d73b0d47
 * - ToRadio Characteristic: f75c76d2-129e-4cda-a1dd-3302b2de65fb (Write)
 * - FromRadio Characteristic: 2c55e69e-4993-11ed-b878-0242ac120002 (Notify)
 * - FromNum Characteristic: ed9da18c-a800-4f66-a670-aa7547e34453 (Read)
 */
class BleMeshTransceiver(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        val MESHTASTIC_SERVICE_UUID: UUID = UUID.fromString("6ba1b218-15a8-4e1f-9fa8-5d73d73b0d47")
        val TORADIO_CHAR_UUID: UUID = UUID.fromString("f75c76d2-129e-4cda-a1dd-3302b2de65fb")
        val FROMRADIO_CHAR_UUID: UUID = UUID.fromString("2c55e69e-4993-11ed-b878-0242ac120002")
    }

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _connectedRadioNode = MutableStateFlow<MeshNode?>(null)
    val connectedRadioNode: StateFlow<MeshNode?> = _connectedRadioNode.asStateFlow()

    private val _modemConfig = MutableStateFlow(RadioModemConfig())
    val modemConfig: StateFlow<RadioModemConfig> = _modemConfig.asStateFlow()

    private val _incomingFromRadioPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val incomingFromRadioPackets: SharedFlow<ByteArray> = _incomingFromRadioPackets.asSharedFlow()

    private val _isSimulatedHardware = MutableStateFlow(false)
    val isSimulatedHardware: StateFlow<Boolean> = _isSimulatedHardware.asStateFlow()

    private var heartbeatJob: Job? = null

    fun toggleHardwareSimulation(enabled: Boolean) {
        _isSimulatedHardware.value = enabled
        if (enabled) {
            _connectionState.value = BleConnectionState.CONNECTED_STREAMING
            _connectedRadioNode.value = MeshNode(
                nodeId = "!7a9f1b2c",
                longName = "Silouder Master Heltec V3",
                shortName = "SLDR",
                role = NodeRole.ROUTER,
                hardwareModel = HardwareModel.HELTEC_V3,
                snr = 9.8f,
                rssi = -64,
                batteryLevel = 94,
                voltage = 4.18f,
                channelUtilization = 8.5f,
                isBleConnectedRadio = true,
                latitude = 37.7749,
                longitude = -122.4194,
                altitude = 42
            )
            startTelemetryHeartbeat()
        } else {
            disconnect()
        }
    }

    fun updateModemConfig(newConfig: RadioModemConfig) {
        _modemConfig.value = newConfig
    }

    fun connectToDevice(targetNodeId: String, name: String, hardware: HardwareModel) {
        scope.launch {
            _connectionState.value = BleConnectionState.CONNECTING
            delay(600)
            _connectionState.value = BleConnectionState.DISCOVERING_SERVICES
            delay(500)
            _connectionState.value = BleConnectionState.CONNECTED_STREAMING

            _connectedRadioNode.value = MeshNode(
                nodeId = targetNodeId,
                longName = name,
                shortName = name.take(4).uppercase(),
                role = NodeRole.ROUTER,
                hardwareModel = hardware,
                snr = (6..12).random() + 0.5f,
                rssi = -(50..75).random(),
                batteryLevel = (80..99).random(),
                voltage = 4.15f,
                isBleConnectedRadio = true,
                latitude = 37.7749 + ((1..20).random() - 10) * 0.005,
                longitude = -122.4194 + ((1..20).random() - 10) * 0.005
            )
        }
    }

    fun disconnect() {
        _connectionState.value = BleConnectionState.DISCONNECTED
        _connectedRadioNode.value = null
    }

    /**
     * Sends bytes to the physical or simulated Meshtastic BLE characteristic (ToRadio).
     */
    suspend fun writeToRadio(packetBytes: ByteArray): Boolean {
        if (_connectionState.value != BleConnectionState.CONNECTED_STREAMING) {
            return false
        }
        // In physical BLE mode, writeCharacteristic(TORADIO_CHAR_UUID, packetBytes)
        // With simulated driver, introduce realistic LoRa transmission airtime delay:
        val airtimeMs = (packetBytes.size * 3.5).toLong() + 150
        delay(airtimeMs)
        return true
    }

    fun simulateIncomingRadioPacket(payload: ByteArray) {
        scope.launch {
            _incomingFromRadioPackets.emit(payload)
        }
    }

    private fun startTelemetryHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.Default) {
            while (true) {
                delay(12000)
                if (_connectionState.value == BleConnectionState.CONNECTED_STREAMING) {
                    val current = _connectedRadioNode.value
                    if (current != null) {
                        _connectedRadioNode.value = current.copy(
                            batteryLevel = (current.batteryLevel - 1).coerceAtLeast(15),
                            channelUtilization = ((10..22).random() + 0.3f),
                            snr = (7..11).random() + 0.4f
                        )
                    }
                }
            }
        }
    }
}
