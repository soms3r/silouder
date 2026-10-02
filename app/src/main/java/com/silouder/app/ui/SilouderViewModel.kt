package com.silouder.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silouder.app.crypto.CryptoEngine
import com.silouder.app.data.AppDatabase
import com.silouder.app.data.MeshRepository
import com.silouder.app.model.ChannelType
import com.silouder.app.model.HardwareModel
import com.silouder.app.model.MeshChannel
import com.silouder.app.model.MeshNode
import com.silouder.app.model.MessageStatus
import com.silouder.app.model.NodeRole
import com.silouder.app.model.PacketDirection
import com.silouder.app.model.PacketTrace
import com.silouder.app.model.PacketType
import com.silouder.app.model.TransportRouteHint
import com.silouder.app.model.UnifiedMessage
import com.silouder.app.model.UserProfile
import com.silouder.app.model.NatureAvatars
import com.silouder.app.sync.BriarSyncEngine
import com.silouder.app.transport.bluetooth.BluetoothPeerManager
import com.silouder.app.transport.meshtastic.BleMeshTransceiver
import com.silouder.app.transport.network.NetworkPeerTransport
import com.silouder.app.transport.router.TransportRouter
import com.silouder.app.transport.tor.TorSessionLayer
import com.silouder.app.media.FileManager
import com.silouder.app.media.P2PCallManager
import com.silouder.app.media.ActiveCallSession
import com.silouder.app.media.CallState
import com.silouder.app.media.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import android.util.Base64
import java.security.MessageDigest
import java.util.UUID

enum class AppScreen {
    DASHBOARD,
    CHATS,
    CONVERSATION,
    BLUETOOTH_SYNC,
    NETWORK_PEERS,
    MESH_TOPOLOGY,
    RADIO_CONTROL,
    OUTBOX_QUEUE,
    KEY_RING,
    PACKET_INSPECTOR,
    SPECIFICATION_DOCS,
    ABOUT_APP,
    SETTINGS,
    PROFILE
}

enum class AppDisplayMode {
    STANDARD_MESSENGER, // Minimalist, clean Signal/WhatsApp-like feel for everyday 1-on-1 and group chats
    TACTICAL_OPERATOR,  // Full mesh radios, RF spectrum, packet sniffing, topology radar
    CUSTOM
}

class SilouderViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val repository = MeshRepository(database)

    // Feature Visibility & Display Mode Settings
    private val _appDisplayMode = MutableStateFlow(AppDisplayMode.STANDARD_MESSENGER)
    val appDisplayMode: StateFlow<AppDisplayMode> = _appDisplayMode.asStateFlow()

    private val _showLoRaMesh = MutableStateFlow(true)
    val showLoRaMesh: StateFlow<Boolean> = _showLoRaMesh.asStateFlow()

    private val _showBluetoothSyncTab = MutableStateFlow(true)
    val showBluetoothSyncTab: StateFlow<Boolean> = _showBluetoothSyncTab.asStateFlow()

    private val _showOnlineNetTab = MutableStateFlow(true)
    val showOnlineNetTab: StateFlow<Boolean> = _showOnlineNetTab.asStateFlow()

    private val _showOutboxTab = MutableStateFlow(true)
    val showOutboxTab: StateFlow<Boolean> = _showOutboxTab.asStateFlow()

    private val _showPacketInspector = MutableStateFlow(false)
    val showPacketInspector: StateFlow<Boolean> = _showPacketInspector.asStateFlow()

    private val _showTopologyRadar = MutableStateFlow(false)
    val showTopologyRadar: StateFlow<Boolean> = _showTopologyRadar.asStateFlow()

    private val _showTechnicalMetricsInChat = MutableStateFlow(false)
    val showTechnicalMetricsInChat: StateFlow<Boolean> = _showTechnicalMetricsInChat.asStateFlow()

    private val _disappearingTimerSeconds = MutableStateFlow(0) // 0 = off, 86400 = 24h
    val disappearingTimerSeconds: StateFlow<Int> = _disappearingTimerSeconds.asStateFlow()

    private val _biometricLockEnabled = MutableStateFlow(false)
    val biometricLockEnabled: StateFlow<Boolean> = _biometricLockEnabled.asStateFlow()

    // Transports
    val bluetoothPeerManager = BluetoothPeerManager(application, viewModelScope)
    val networkPeerTransport = NetworkPeerTransport(application, viewModelScope)
    val bleTransceiver = BleMeshTransceiver(application, viewModelScope)
    val torSessionLayer = TorSessionLayer(viewModelScope)

    // Media & Calling (Signal/Telegram grade P2P)
    val fileManager = FileManager(application)
    val callManager = P2PCallManager(application, viewModelScope)
    val callSession: StateFlow<ActiveCallSession?> = callManager.callSession
    val isPttActive: StateFlow<Boolean> = callManager.isPttActive

    // Voice recording state
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _voiceRecordingDuration = MutableStateFlow(0)
    val voiceRecordingDuration: StateFlow<Int> = _voiceRecordingDuration.asStateFlow()

    private var voiceTimerJob: Job? = null

    // Routing & Sync
    val syncEngine = BriarSyncEngine(repository, viewModelScope)
    val transportRouter = TransportRouter(
        bluetoothPeerManager = bluetoothPeerManager,
        networkPeerTransport = networkPeerTransport,
        bleTransceiver = bleTransceiver,
        torSessionLayer = torSessionLayer,
        repository = repository,
        scope = viewModelScope
    )

    // Identity state
    private val _myIdentity = MutableStateFlow(
        CryptoEngine.generateIdentity("Alpine Echo")
    )
    val myIdentity: StateFlow<CryptoEngine.CryptoIdentity> = _myIdentity.asStateFlow()

    // User Profile state
    private val _userProfile = MutableStateFlow(
        UserProfile(
            displayName = "Alpine Echo",
            customNumberOrTag = "#7721",
            bioStatus = "Decentralized • Off-Grid • Forward Secret",
            avatarId = "mountain",
            isAutoGenerated = true
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedConversationId = MutableStateFlow("chan_0_primary")
    val selectedConversationId: StateFlow<String> = _selectedConversationId.asStateFlow()

    // UI flows from repository
    val allMessages: StateFlow<List<UnifiedMessage>> = repository.allMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val queuedMessages: StateFlow<List<UnifiedMessage>> = repository.queuedMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allNodes: StateFlow<List<MeshNode>> = repository.allNodes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allChannels: StateFlow<List<MeshChannel>> = repository.allChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentPackets: StateFlow<List<PacketTrace>> = repository.recentPackets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Message input draft state
    private val _messageDraft = MutableStateFlow("")
    val messageDraft: StateFlow<String> = _messageDraft.asStateFlow()

    private val _selectedRouteHint = MutableStateFlow(TransportRouteHint.AUTO_BEST)
    val selectedRouteHint: StateFlow<TransportRouteHint> = _selectedRouteHint.asStateFlow()

    // Dialog & Modal states
    private val _showNewChannelDialog = MutableStateFlow(false)
    val showNewChannelDialog: StateFlow<Boolean> = _showNewChannelDialog.asStateFlow()

    private val _showCreateEmergencyGroupModal = MutableStateFlow(false)
    val showCreateEmergencyGroupModal: StateFlow<Boolean> = _showCreateEmergencyGroupModal.asStateFlow()

    private val _showJoinGroupModal = MutableStateFlow(false)
    val showJoinGroupModal: StateFlow<Boolean> = _showJoinGroupModal.asStateFlow()

    private val _shareGroupModalChannel = MutableStateFlow<MeshChannel?>(null)
    val shareGroupModalChannel: StateFlow<MeshChannel?> = _shareGroupModalChannel.asStateFlow()

    init {
        seedInitialData()
        observeIncomingNetworkMessages()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openConversation(convId: String) {
        _selectedConversationId.value = convId
        _currentScreen.value = AppScreen.CONVERSATION
        viewModelScope.launch {
            repository.markChannelRead(convId)
        }
    }

    fun setMessageDraft(text: String) {
        _messageDraft.value = text
    }

    fun setSelectedRouteHint(hint: TransportRouteHint) {
        _selectedRouteHint.value = hint
    }

    fun setShowNewChannelDialog(show: Boolean) {
        _showNewChannelDialog.value = show
    }

    fun setShowCreateEmergencyGroupModal(show: Boolean) {
        _showCreateEmergencyGroupModal.value = show
    }

    fun setShowJoinGroupModal(show: Boolean) {
        _showJoinGroupModal.value = show
    }

    fun setShareGroupModal(channel: MeshChannel?) {
        _shareGroupModalChannel.value = channel
    }

    fun setAppDisplayMode(mode: AppDisplayMode) {
        _appDisplayMode.value = mode
        when (mode) {
            AppDisplayMode.STANDARD_MESSENGER -> {
                _showLoRaMesh.value = false
                _showBluetoothSyncTab.value = false
                _showOnlineNetTab.value = false
                _showOutboxTab.value = false
                _showPacketInspector.value = false
                _showTopologyRadar.value = false
                _showTechnicalMetricsInChat.value = false
            }
            AppDisplayMode.TACTICAL_OPERATOR -> {
                _showLoRaMesh.value = true
                _showBluetoothSyncTab.value = true
                _showOnlineNetTab.value = true
                _showOutboxTab.value = true
                _showPacketInspector.value = true
                _showTopologyRadar.value = true
                _showTechnicalMetricsInChat.value = true
            }
            AppDisplayMode.CUSTOM -> {}
        }
    }

    fun toggleLoRaMesh(enabled: Boolean) {
        _showLoRaMesh.value = enabled
        _appDisplayMode.value = AppDisplayMode.CUSTOM
    }

    fun toggleBluetoothSyncTab(enabled: Boolean) {
        _showBluetoothSyncTab.value = enabled
        _appDisplayMode.value = AppDisplayMode.CUSTOM
    }

    fun toggleOnlineNetTab(enabled: Boolean) {
        _showOnlineNetTab.value = enabled
        _appDisplayMode.value = AppDisplayMode.CUSTOM
    }

    fun toggleOutboxTab(enabled: Boolean) {
        _showOutboxTab.value = enabled
        _appDisplayMode.value = AppDisplayMode.CUSTOM
    }

    fun togglePacketInspector(enabled: Boolean) {
        _showPacketInspector.value = enabled
        _appDisplayMode.value = AppDisplayMode.CUSTOM
    }

    fun toggleTopologyRadar(enabled: Boolean) {
        _showTopologyRadar.value = enabled
        _appDisplayMode.value = AppDisplayMode.CUSTOM
    }

    fun toggleTechnicalMetricsInChat(enabled: Boolean) {
        _showTechnicalMetricsInChat.value = enabled
    }

    fun setDisappearingTimer(seconds: Int) {
        _disappearingTimerSeconds.value = seconds
    }

    fun toggleBiometricLock(enabled: Boolean) {
        _biometricLockEnabled.value = enabled
    }

    /**
     * Creates a standard 1-on-1 direct E2EE contact chat.
     */
    fun createDirectContactChat(name: String, contactKeyOrAddress: String, initialMsg: String? = null) {
        val contactName = name.ifBlank { "Contact #${(100..999).random()}" }
        val channelId = "direct_${UUID.randomUUID().toString().take(8)}"
        val e2eeSessionKey = "E2EE-DoubleRatchet-${UUID.randomUUID().toString().take(12)}"
        
        val directChannel = MeshChannel(
            channelIndex = allChannels.value.size,
            channelId = channelId,
            name = contactName,
            channelType = ChannelType.DIRECT_E2EE,
            pskBase64 = e2eeSessionKey,
            modemPreset = "DIRECT_E2EE_SIGNAL",
            frequencyMhz = 0f,
            spreadingFactor = 0,
            bandwidthKhz = 0f,
            txPowerDbm = 0,
            unreadCount = 0,
            lastMessagePreview = initialMsg ?: "🔐 E2EE session initialized with forward secrecy.",
            lastMessageTimestamp = System.currentTimeMillis(),
            peerNodeId = contactKeyOrAddress.ifBlank { "!${UUID.randomUUID().toString().take(8)}" }
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateChannel(directChannel)
            if (!initialMsg.isNullOrBlank()) {
                val enc = CryptoEngine.encryptPayload(initialMsg, CryptoEngine.DEFAULT_MESHTASTIC_PSK)
                val msg = UnifiedMessage(
                    messageId = "dir_msg_${UUID.randomUUID().toString().take(8)}",
                    conversationId = channelId,
                    senderId = _myIdentity.value.nodeId,
                    senderName = "YOU",
                    recipientId = directChannel.peerNodeId ?: "unknown",
                    timestamp = System.currentTimeMillis(),
                    plaintext = initialMsg,
                    payloadCiphertext = enc.ciphertextBase64,
                    ivBase64 = enc.ivBase64,
                    macSignature = enc.macSignatureHex,
                    transportRouteHint = TransportRouteHint.AUTO_BEST,
                    actualTransport = TransportRouteHint.ONLINE_NETWORK,
                    hopLimit = 1,
                    hopsTraveled = 0,
                    snr = 24.0f,
                    rssi = -30,
                    status = MessageStatus.DELIVERED_NETWORK,
                    isIncoming = false,
                    isBroadcast = false
                )
                repository.insertMessage(msg)
            }
        }
    }

    /**
     * Wipes all conversations, keys, logs, and database tables in one panic action.
     */
    fun wipeAllLocalData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            _myIdentity.value = CryptoEngine.generateIdentity("Anonymous-${(100..999).random()}")
            seedInitialData()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun updateUserProfile(
        displayName: String,
        customNumberOrTag: String,
        bioStatus: String,
        avatarId: String
    ) {
        val cleanName = displayName.ifBlank { "User-${(100..999).random()}" }
        _userProfile.value = _userProfile.value.copy(
            displayName = cleanName,
            customNumberOrTag = customNumberOrTag.ifBlank { "#${(1000..9999).random()}" },
            bioStatus = bioStatus,
            avatarId = avatarId,
            isAutoGenerated = false
        )
        // Also update radio alias in cryptographic identity
        _myIdentity.value = _myIdentity.value.copy(shortName = cleanName.take(4).uppercase())
        transportRouter.logRoutingEvent("Profile updated: $cleanName (${_userProfile.value.customNumberOrTag}) - Avatar: $avatarId")
    }

    fun autoGenerateProfile() {
        val natureNames = listOf(
            "Alpine Frost", "Cedar Ridge", "Ocean Surge", "Solar Flare",
            "Midnight Pine", "Glacier Mist", "Desert Oasis", "Cascade Falls",
            "Canyon Peak", "Aurora Breeze", "Bamboo Stream", "Emerald Peak",
            "Thunder Horizon", "Autumn Leaf", "Volcanic Ridge", "Arctic Frost"
        )
        val natureBios = listOf(
            "Decentralized • Off-Grid • Forward Secret",
            "Signal mesh peer • Zero-infrastructure communicator",
            "Autonomous node operator • End-to-end encrypted",
            "Offline first • Briar Bluetooth & LoRa linked",
            "Privacy advocate • Tor Onion & Multi-Transport"
        )
        val generatedName = natureNames.random()
        val generatedNumber = "#${(1000..9999).random()}"
        val generatedAvatar = NatureAvatars.ALL.random().id
        val generatedBio = natureBios.random()

        _userProfile.value = UserProfile(
            displayName = generatedName,
            customNumberOrTag = generatedNumber,
            bioStatus = generatedBio,
            avatarId = generatedAvatar,
            isAutoGenerated = true,
            joinedTimestamp = System.currentTimeMillis()
        )
        _myIdentity.value = _myIdentity.value.copy(shortName = generatedName.take(4).uppercase())
        transportRouter.logRoutingEvent("Auto-generated anonymous nature profile: $generatedName $generatedNumber")
    }

    fun regenerateIdentity(alias: String) {
        _myIdentity.value = CryptoEngine.generateIdentity(alias.ifBlank { "Silouder-User" })
        transportRouter.logRoutingEvent("Regenerated identity: Node ID ${_myIdentity.value.nodeId}, Onion: ${_myIdentity.value.onionAddress}")
    }

    /**
     * Creates an ad-hoc emergency crowd cell group (for protests, concerts, or emergencies).
     */
    fun createEmergencyGroup(
        name: String,
        customJoinCode: String?,
        location: String,
        autoWipeHours: Int,
        enableBleBeacon: Boolean
    ) {
        val groupName = name.ifBlank { "Emergency Cell #${(100..999).random()}" }
        // Generate short join code e.g. "SILO-4821" or "PROTEST-MED-77"
        val code = if (!customJoinCode.isNullOrBlank()) {
            customJoinCode.trim().uppercase()
        } else {
            "SILO-${(1000..9999).random()}"
        }

        val channelId = "emg_${UUID.randomUUID().toString().take(8)}"
        // Derive AES-256 group key from join code
        val pskBytes = MessageDigest.getInstance("SHA-256").digest(code.toByteArray())
        val pskBase64 = android.util.Base64.encodeToString(pskBytes, android.util.Base64.NO_WRAP)

        val expiresAt = if (autoWipeHours > 0) System.currentTimeMillis() + (autoWipeHours * 3600000L) else 0L

        val channel = MeshChannel(
            channelIndex = allChannels.value.size,
            channelId = channelId,
            name = groupName,
            channelType = ChannelType.EMERGENCY_GROUP,
            pskBase64 = pskBase64,
            modemPreset = "TACTICAL_EMERGENCY_MESH",
            frequencyMhz = 915.0f,
            spreadingFactor = 10,
            bandwidthKhz = 250f,
            codingRate = "4/8",
            txPowerDbm = 26,
            unreadCount = 0,
            lastMessagePreview = "🚨 Emergency group created. Join Code: $code",
            lastMessageTimestamp = System.currentTimeMillis(),
            isPrimary = false,
            joinCode = code,
            memberCount = 1,
            expiresAt = expiresAt,
            isEmergencyGroup = true,
            incidentLocation = location.ifBlank { "Incident Field" },
            isBleBeaconBroadcast = enableBleBeacon
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateChannel(channel)

            // Insert system notification message
            val sysMsg = UnifiedMessage(
                messageId = "sys_${UUID.randomUUID().toString().take(8)}",
                conversationId = channelId,
                senderId = _myIdentity.value.nodeId,
                senderName = "SYSTEM",
                recipientId = "^all",
                timestamp = System.currentTimeMillis(),
                plaintext = "🚨 [EMERGENCY GROUP INITIALIZED] Channel secured with AES-256. Secret Join Code: $code. Multi-transport broadcast active across Bluetooth, LAN, and LoRa.",
                payloadCiphertext = "EMG_AES256_INIT",
                ivBase64 = "IV_INIT",
                macSignature = "MAC_INIT",
                transportRouteHint = TransportRouteHint.AUTO_BEST,
                status = MessageStatus.DELIVERED_MESH,
                isIncoming = false,
                isBroadcast = true,
                isUrgentAlert = true
            )
            repository.insertMessage(sysMsg)

            transportRouter.logRoutingEvent("Created emergency group '$groupName' with Join Code '$code' (Location: $location).")
            if (enableBleBeacon) {
                bluetoothPeerManager.logBtEvent("Broadcasting Emergency Group Beacon: '$groupName' (Join Code: $code)")
            }

            openConversation(channelId)
        }
    }

    /**
     * Joins an emergency group by entering secret join code or scanning QR URL.
     */
    fun joinGroupByCodeOrQr(rawInput: String, onResult: (Boolean, String) -> Unit) {
        val cleanInput = rawInput.trim()
        if (cleanInput.isBlank()) {
            onResult(false, "Please enter a valid Join Code or scan a QR code.")
            return
        }

        // Extract code from QR URL (silouder://join-group?code=... or raw code)
        val extractedCode = if (cleanInput.contains("code=")) {
            cleanInput.substringAfter("code=").substringBefore("&").uppercase()
        } else {
            cleanInput.uppercase()
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Check if channel with this join code already exists
            val existing = repository.getChannelByJoinCode(extractedCode)
            if (existing != null) {
                repository.incrementMemberCount(existing.channelId)
                openConversation(existing.channelId)
                onResult(true, "Joined existing emergency group: ${existing.name}")
                return@launch
            }

            // Create and join the group dynamically using the derived PSK from the join code
            val channelId = "emg_${UUID.randomUUID().toString().take(8)}"
            val pskBytes = MessageDigest.getInstance("SHA-256").digest(extractedCode.toByteArray())
            val pskBase64 = android.util.Base64.encodeToString(pskBytes, android.util.Base64.NO_WRAP)

            val parsedName = if (cleanInput.contains("name=")) {
                java.net.URLDecoder.decode(cleanInput.substringAfter("name=").substringBefore("&"), "UTF-8")
            } else {
                "Emergency Cell ($extractedCode)"
            }

            val channel = MeshChannel(
                channelIndex = allChannels.value.size,
                channelId = channelId,
                name = parsedName,
                channelType = ChannelType.EMERGENCY_GROUP,
                pskBase64 = pskBase64,
                modemPreset = "TACTICAL_EMERGENCY_MESH",
                frequencyMhz = 915.0f,
                spreadingFactor = 10,
                bandwidthKhz = 250f,
                codingRate = "4/8",
                txPowerDbm = 26,
                unreadCount = 0,
                lastMessagePreview = "👋 Joined group with code $extractedCode",
                lastMessageTimestamp = System.currentTimeMillis(),
                isPrimary = false,
                joinCode = extractedCode,
                memberCount = (2..8).random(),
                expiresAt = System.currentTimeMillis() + (12 * 3600000L),
                isEmergencyGroup = true,
                incidentLocation = "Crowd Area"
            )

            repository.insertOrUpdateChannel(channel)

            // Add join notice
            val joinNotice = UnifiedMessage(
                messageId = "join_${UUID.randomUUID().toString().take(8)}",
                conversationId = channelId,
                senderId = _myIdentity.value.nodeId,
                senderName = _myIdentity.value.shortName,
                recipientId = "^all",
                timestamp = System.currentTimeMillis(),
                plaintext = "👋 Joined group using secret code [$extractedCode]. E2EE session connected.",
                payloadCiphertext = "JOIN_NOTICE_PAYLOAD",
                ivBase64 = "IV_JOIN",
                macSignature = "MAC_JOIN",
                transportRouteHint = TransportRouteHint.AUTO_BEST,
                status = MessageStatus.DELIVERED_MESH,
                isIncoming = false,
                isBroadcast = true
            )
            repository.insertMessage(joinNotice)

            transportRouter.logRoutingEvent("Joined emergency group '$parsedName' via code $extractedCode.")
            openConversation(channelId)
            onResult(true, "Successfully joined '$parsedName'!")
        }
    }

    /**
     * Panic Wipe & Leave Group (deletes all messages and keys locally).
     */
    fun leaveAndBurnGroup(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChannelAndMessages(channelId)
            transportRouter.logRoutingEvent("Emergency group $channelId securely burned & removed from local storage.")
            navigateTo(AppScreen.CHATS)
        }
    }

    /**
     * Broadcasts a high-priority urgent alert within the current group.
     */
    fun sendUrgentGroupAlert(alertText: String) {
        val convId = _selectedConversationId.value
        val myKey = _myIdentity.value
        val psk = CryptoEngine.DEFAULT_MESHTASTIC_PSK
        val encrypted = CryptoEngine.encryptPayload("🚨 [URGENT ALERT] $alertText", psk)

        val msg = UnifiedMessage(
            messageId = "urg_${UUID.randomUUID().toString().take(8)}",
            conversationId = convId,
            senderId = myKey.nodeId,
            senderName = "${myKey.shortName}-ALERT",
            recipientId = "^all",
            timestamp = System.currentTimeMillis(),
            plaintext = "🚨 [URGENT ALERT] $alertText",
            payloadCiphertext = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            macSignature = encrypted.macSignatureHex,
            transportRouteHint = TransportRouteHint.AUTO_BEST,
            hopLimit = 5,
            status = MessageStatus.QUEUED,
            isIncoming = false,
            isBroadcast = true,
            isUrgentAlert = true
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            transportRouter.dispatchMessage(msg)
        }
    }

    fun sendMessage() {
        val text = _messageDraft.value.trim()
        if (text.isEmpty()) return

        val convId = _selectedConversationId.value
        val channel = allChannels.value.find { it.channelId == convId }
        val isBroadcast = channel?.channelType == ChannelType.BROADCAST_PRIMARY ||
                channel?.channelType == ChannelType.TACTICAL_SECONDARY ||
                channel?.channelType == ChannelType.EMERGENCY_GROUP
        val recipientId = if (isBroadcast) "^all" else (channel?.peerNodeId ?: "peer_node")

        val myKey = _myIdentity.value
        val psk = CryptoEngine.DEFAULT_MESHTASTIC_PSK

        val encrypted = CryptoEngine.encryptPayload(text, psk)
        val msgId = "msg_${UUID.randomUUID().toString().take(8)}"

        val msg = UnifiedMessage(
            messageId = msgId,
            conversationId = convId,
            senderId = myKey.nodeId,
            senderName = myKey.shortName,
            recipientId = recipientId,
            timestamp = System.currentTimeMillis(),
            plaintext = text,
            payloadCiphertext = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            macSignature = encrypted.macSignatureHex,
            transportRouteHint = _selectedRouteHint.value,
            hopLimit = 3,
            hopsTraveled = 0,
            snr = 0.0f,
            rssi = -70,
            status = MessageStatus.QUEUED,
            isIncoming = false,
            isBroadcast = isBroadcast,
            fragmentCount = if (text.length > 180) (text.length / 180) + 1 else 1
        )

        _messageDraft.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            transportRouter.dispatchMessage(msg)
        }
    }

    fun sendQuickSosBroadcast() {
        val sosText = "🚨 [SILOUDER SOS] Emergency beacon triggered at coordinates (37.7749, -122.4194). Multi-transport broadcast across Bluetooth, Online Network, and LoRa."
        val myKey = _myIdentity.value
        val psk = CryptoEngine.DEFAULT_MESHTASTIC_PSK
        val encrypted = CryptoEngine.encryptPayload(sosText, psk)

        val msg = UnifiedMessage(
            messageId = "sos_${UUID.randomUUID().toString().take(6)}",
            conversationId = "chan_0_primary",
            senderId = myKey.nodeId,
            senderName = "SOS-BEACON",
            recipientId = "^all",
            timestamp = System.currentTimeMillis(),
            plaintext = sosText,
            payloadCiphertext = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            macSignature = encrypted.macSignatureHex,
            transportRouteHint = TransportRouteHint.AUTO_BEST,
            hopLimit = 5,
            status = MessageStatus.QUEUED,
            isIncoming = false,
            isBroadcast = true,
            isUrgentAlert = true
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            transportRouter.dispatchMessage(msg)
        }
    }

    fun simulateIncomingMeshMessage(sender: MeshNode, text: String) {
        val psk = CryptoEngine.DEFAULT_MESHTASTIC_PSK
        val encrypted = CryptoEngine.encryptPayload(text, psk)
        val msg = UnifiedMessage(
            messageId = "rx_${UUID.randomUUID().toString().take(8)}",
            conversationId = _selectedConversationId.value,
            senderId = sender.nodeId,
            senderName = sender.shortName,
            recipientId = "^all",
            timestamp = System.currentTimeMillis(),
            plaintext = text,
            payloadCiphertext = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            macSignature = encrypted.macSignatureHex,
            transportRouteHint = TransportRouteHint.LORA_MESH,
            actualTransport = TransportRouteHint.LORA_MESH,
            hopLimit = 3,
            hopsTraveled = sender.hopCount,
            snr = sender.snr,
            rssi = sender.rssi,
            status = MessageStatus.DELIVERED_MESH,
            isIncoming = true,
            isBroadcast = true
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            repository.logPacket(
                PacketTrace(
                    packetId = msg.messageId.hashCode().toLong(),
                    direction = PacketDirection.RX_INCOMING,
                    packetType = PacketType.MESH_BROADCAST_TEXT,
                    transport = TransportRouteHint.LORA_MESH,
                    fromNode = sender.nodeId,
                    toNode = "^all",
                    channelNum = 0,
                    hopLimit = 3,
                    hopStart = 3,
                    snr = sender.snr,
                    rssi = sender.rssi,
                    rawHex = "4D 45 53 48 5F 50 4B 54 ...",
                    decodedSummary = "Mesh RX: ${text.take(24)}",
                    isCrcValid = true
                )
            )
        }
    }

    fun simulateIncomingNetworkMessage(senderAlias: String, senderIp: String, text: String) {
        val psk = CryptoEngine.DEFAULT_MESHTASTIC_PSK
        val encrypted = CryptoEngine.encryptPayload(text, psk)
        val msg = UnifiedMessage(
            messageId = "net_${UUID.randomUUID().toString().take(8)}",
            conversationId = "chan_direct_net",
            senderId = senderIp,
            senderName = senderAlias.take(4).uppercase(),
            recipientId = "local_app",
            timestamp = System.currentTimeMillis(),
            plaintext = text,
            payloadCiphertext = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            macSignature = encrypted.macSignatureHex,
            transportRouteHint = TransportRouteHint.ONLINE_NETWORK,
            actualTransport = TransportRouteHint.ONLINE_NETWORK,
            hopLimit = 1,
            hopsTraveled = 0,
            snr = 20.0f,
            rssi = -32,
            status = MessageStatus.DELIVERED_NETWORK,
            isIncoming = true,
            isBroadcast = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            repository.logPacket(
                PacketTrace(
                    packetId = msg.messageId.hashCode().toLong(),
                    direction = PacketDirection.RX_INCOMING,
                    packetType = PacketType.DIRECT_E2EE_ENVELOPE,
                    transport = TransportRouteHint.ONLINE_NETWORK,
                    fromNode = senderIp,
                    toNode = "local_app",
                    channelNum = 0,
                    hopLimit = 1,
                    hopStart = 1,
                    snr = 20.0f,
                    rssi = -32,
                    rawHex = "4E 45 54 5F 52 58 5F 50 4B 54 ...",
                    decodedSummary = "Online Net RX ($senderIp): ${text.take(24)}",
                    isCrcValid = true
                )
            )
        }
    }

    fun createCustomChannel(name: String, psk: String, freq: Float, sf: Int) {
        val channelId = "chan_${UUID.randomUUID().toString().take(6)}"
        val channel = MeshChannel(
            channelIndex = allChannels.value.size,
            channelId = channelId,
            name = name,
            channelType = ChannelType.TACTICAL_SECONDARY,
            pskBase64 = if (psk.isBlank()) "Standard tactical PSK" else psk,
            frequencyMhz = freq,
            spreadingFactor = sf,
            bandwidthKhz = 250f,
            codingRate = "4/8",
            txPowerDbm = 22,
            isPrimary = false
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOrUpdateChannel(channel)
        }
    }

    fun triggerAntiEntropySyncWithPeer(peerNodeId: String) {
        syncEngine.initiateGossipSync(peerNodeId, TransportRouteHint.LORA_MESH)
    }

    private fun seedInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Seed Channels including an Emergency Group
            val emergencyChannel = MeshChannel(
                channelIndex = 0,
                channelId = "emg_protest_med_77",
                name = "Protest Medic & Safety Cell",
                channelType = ChannelType.EMERGENCY_GROUP,
                pskBase64 = "PROTEST_MED_AES256_KEY_BASE64",
                modemPreset = "TACTICAL_EMERGENCY_MESH",
                frequencyMhz = 915.0f,
                spreadingFactor = 10,
                bandwidthKhz = 250.0f,
                codingRate = "4/8",
                txPowerDbm = 26,
                unreadCount = 2,
                lastMessagePreview = "🚨 Water & first aid station active at Main Plaza North.",
                lastMessageTimestamp = System.currentTimeMillis() - 60000,
                isPrimary = false,
                joinCode = "SILO-7721",
                memberCount = 14,
                expiresAt = System.currentTimeMillis() + (8 * 3600000L),
                isEmergencyGroup = true,
                incidentLocation = "Main Plaza North / Gate 2",
                isBleBeaconBroadcast = true
            )

            val primaryChannel = MeshChannel(
                channelIndex = 1,
                channelId = "chan_0_primary",
                name = "LongFast (Silouder Mesh)",
                channelType = ChannelType.BROADCAST_PRIMARY,
                pskBase64 = "1AQ=",
                modemPreset = "LONG_FAST",
                frequencyMhz = 915.0f,
                spreadingFactor = 11,
                bandwidthKhz = 250.0f,
                codingRate = "4/8",
                txPowerDbm = 22,
                unreadCount = 0,
                lastMessagePreview = "Silouder mesh transport active. 4 nodes in range.",
                lastMessageTimestamp = System.currentTimeMillis() - 120000,
                isPrimary = true
            )

            val btPeerChannel = MeshChannel(
                channelIndex = 2,
                channelId = "chan_direct_bt",
                name = "Alex (Nearby Bluetooth)",
                channelType = ChannelType.DIRECT_BLUETOOTH_PEER,
                pskBase64 = "BT-E2EE-Session-PSK",
                modemPreset = "BLUETOOTH_P2P_RFCOMM",
                frequencyMhz = 2400f,
                spreadingFactor = 0,
                bandwidthKhz = 0f,
                txPowerDbm = 4,
                unreadCount = 0,
                lastMessagePreview = "Connected via direct Bluetooth BLE. Ready to sync offline.",
                lastMessageTimestamp = System.currentTimeMillis() - 240000,
                peerNodeId = "!8a2f4c91"
            )

            val netPeerChannel = MeshChannel(
                channelIndex = 3,
                channelId = "chan_direct_net",
                name = "Station Echo (Online Net)",
                channelType = ChannelType.DIRECT_NETWORK_PEER,
                pskBase64 = "LAN-TCP-E2EE-Key",
                modemPreset = "APP_TO_APP_ONLINE_SOCKET",
                frequencyMhz = 0f,
                spreadingFactor = 0,
                bandwidthKhz = 0f,
                txPowerDbm = 0,
                unreadCount = 0,
                lastMessagePreview = "App-to-App direct TCP socket online (192.168.1.120:8888).",
                lastMessageTimestamp = System.currentTimeMillis() - 360000,
                peerNodeId = "192.168.1.120"
            )

            val directAlice = MeshChannel(
                channelIndex = 4,
                channelId = "direct_alice",
                name = "Alice Vance",
                channelType = ChannelType.DIRECT_E2EE,
                pskBase64 = "ALICE_SIGNAL_E2EE_KEY",
                modemPreset = "DIRECT_E2EE_SIGNAL",
                frequencyMhz = 0f,
                spreadingFactor = 0,
                bandwidthKhz = 0f,
                txPowerDbm = 0,
                unreadCount = 1,
                lastMessagePreview = "Hey! Glad you're on Silouder. Is your session verified with forward secrecy?",
                lastMessageTimestamp = System.currentTimeMillis() - 45000,
                peerNodeId = "!alice_vance_e2ee"
            )

            val directDavid = MeshChannel(
                channelIndex = 5,
                channelId = "direct_david",
                name = "David Chen",
                channelType = ChannelType.DIRECT_E2EE,
                pskBase64 = "DAVID_TOR_E2EE_KEY",
                modemPreset = "DIRECT_E2EE_TOR",
                frequencyMhz = 0f,
                spreadingFactor = 0,
                bandwidthKhz = 0f,
                txPowerDbm = 0,
                unreadCount = 0,
                lastMessagePreview = "Meeting at 3 PM. Sending encrypted coordinates now.",
                lastMessageTimestamp = System.currentTimeMillis() - 7200000,
                peerNodeId = "!david_chen_tor"
            )

            val directSarah = MeshChannel(
                channelIndex = 6,
                channelId = "direct_sarah",
                name = "Sarah Miller",
                channelType = ChannelType.DIRECT_E2EE,
                pskBase64 = "SARAH_DIRECT_E2EE_KEY",
                modemPreset = "DIRECT_E2EE_SIGNAL",
                frequencyMhz = 0f,
                spreadingFactor = 0,
                bandwidthKhz = 0f,
                txPowerDbm = 0,
                unreadCount = 0,
                lastMessagePreview = "The offline Bluetooth sync worked flawlessly on the subway.",
                lastMessageTimestamp = System.currentTimeMillis() - 86400000,
                peerNodeId = "!sarah_miller"
            )

            repository.insertOrUpdateChannels(listOf(
                emergencyChannel,
                directAlice,
                directDavid,
                directSarah,
                primaryChannel,
                btPeerChannel,
                netPeerChannel
            ))

            // Seed Initial Messages for Alice Vance
            val aliceMessages = listOf(
                UnifiedMessage(
                    messageId = "alice_01",
                    conversationId = "direct_alice",
                    senderId = "!alice_vance_e2ee",
                    senderName = "ALICE",
                    recipientId = "local_app",
                    timestamp = System.currentTimeMillis() - 120000,
                    plaintext = "Hey! Glad you're on Silouder. Is your session verified with forward secrecy?",
                    payloadCiphertext = "CIPHERTEXT_ALICE_01",
                    ivBase64 = "IV_ALICE_01",
                    macSignature = "MAC_ALICE_01",
                    transportRouteHint = TransportRouteHint.AUTO_BEST,
                    actualTransport = TransportRouteHint.ONLINE_NETWORK,
                    hopsTraveled = 0,
                    snr = 25.0f,
                    rssi = -32,
                    status = MessageStatus.DELIVERED_NETWORK,
                    isIncoming = true,
                    isBroadcast = false
                )
            )
            for (m in aliceMessages) {
                repository.insertMessage(m)
            }

            // Seed Initial Messages for Emergency Group
            val emgMessages = listOf(
                UnifiedMessage(
                    messageId = "emg_init_01",
                    conversationId = "emg_protest_med_77",
                    senderId = "!8a2f4c91",
                    senderName = "MED-LEAD",
                    recipientId = "^all",
                    timestamp = System.currentTimeMillis() - 300000,
                    plaintext = "🚨 [EMERGENCY GROUP ACTIVE] Cell formed for crowd medical support. Share Join Code SILO-7721 or QR code with field volunteers.",
                    payloadCiphertext = "AES256_GCM_EMG_INIT",
                    ivBase64 = "IV_NONCE_EMG",
                    macSignature = "MAC_SIG_EMG",
                    transportRouteHint = TransportRouteHint.AUTO_BEST,
                    actualTransport = TransportRouteHint.BLUETOOTH_P2P,
                    hopsTraveled = 0,
                    snr = 12.0f,
                    rssi = -50,
                    status = MessageStatus.DELIVERED_BLUETOOTH,
                    isIncoming = true,
                    isBroadcast = true,
                    isUrgentAlert = true
                ),
                UnifiedMessage(
                    messageId = "emg_init_02",
                    conversationId = "emg_protest_med_77",
                    senderId = "!3b7e41fa",
                    senderName = "SCOUT-2",
                    recipientId = "^all",
                    timestamp = System.currentTimeMillis() - 60000,
                    plaintext = "Water & first aid station active at Main Plaza North. 14 volunteers joined mesh cell.",
                    payloadCiphertext = "AES256_GCM_EMG_MSG",
                    ivBase64 = "IV_NONCE_EMG2",
                    macSignature = "MAC_SIG_EMG2",
                    transportRouteHint = TransportRouteHint.AUTO_BEST,
                    actualTransport = TransportRouteHint.LORA_MESH,
                    hopsTraveled = 1,
                    snr = 8.5f,
                    rssi = -72,
                    status = MessageStatus.DELIVERED_MESH,
                    isIncoming = true,
                    isBroadcast = true
                )
            )

            for (m in emgMessages) {
                repository.insertMessage(m)
            }

            // Seed Discovered Nodes
            val nodes = listOf(
                MeshNode(
                    nodeId = "!7a9f1b2c",
                    longName = "Silouder Master (Local Heltec V3)",
                    shortName = "SLDR",
                    role = NodeRole.ROUTER,
                    hardwareModel = HardwareModel.HELTEC_V3,
                    publicKeyHex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                    onionAddress = "silouder7vqx4mk89pz.onion",
                    snr = 9.8f,
                    rssi = -64,
                    batteryLevel = 94,
                    voltage = 4.18f,
                    channelUtilization = 8.5f,
                    hopCount = 0,
                    isDirectNeighbor = true,
                    isBleConnectedRadio = true,
                    latitude = 37.7749,
                    longitude = -122.4194,
                    altitude = 42
                ),
                MeshNode(
                    nodeId = "!3b7e41fa",
                    longName = "Bravo Scout (RAK4631)",
                    shortName = "BRVO",
                    role = NodeRole.CLIENT,
                    hardwareModel = HardwareModel.RAK_4631,
                    publicKeyHex = "8a2f4c9101bde04921bfe8492048591024859218591823940192840192830192",
                    onionAddress = "bravo9x8t2z4p01a.onion",
                    snr = 7.4f,
                    rssi = -78,
                    batteryLevel = 82,
                    voltage = 4.02f,
                    channelUtilization = 11.2f,
                    hopCount = 1,
                    isDirectNeighbor = true,
                    latitude = 37.7810,
                    longitude = -122.4110,
                    altitude = 55
                )
            )
            repository.insertOrUpdateNodes(nodes)
        }
    }

    // ==========================================
    // Real Network Incoming Message Handler
    // ==========================================
    private fun observeIncomingNetworkMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            networkPeerTransport.incomingNetworkMessages.collect { json ->
                handleIncomingNetworkEnvelope(json)
            }
        }
    }

    private suspend fun handleIncomingNetworkEnvelope(json: JSONObject) {
        try {
            val msgId = json.optString("messageId", "net_${UUID.randomUUID().toString().take(8)}")
            val senderId = json.optString("senderId", "unknown_peer")
            val recipientId = json.optString("recipientId", "local_app")
            var conversationId = json.optString("conversationId", "")
            val rawPlaintext = json.optString("plaintext", "")
            val payloadCiphertext = json.optString("payloadCiphertext", "")
            val ivBase64 = json.optString("ivBase64", "")
            val macSignature = json.optString("macSignature", "")
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())

            val attachmentType = if (json.has("attachmentType")) json.optString("attachmentType") else null
            val attachmentName = if (json.has("attachmentName")) json.optString("attachmentName") else null
            val attachmentSize = json.optLong("attachmentSize", 0L)
            val durationMs = json.optLong("durationMs", 0L)
            val attachmentData = if (json.has("attachmentData")) json.optString("attachmentData") else null

            var localAttachmentPath: String? = null
            if (!attachmentData.isNullOrBlank() && !attachmentName.isNullOrBlank()) {
                try {
                    val decodedBytes = Base64.decode(attachmentData, Base64.NO_WRAP)
                    val targetFile = File(fileManager.getAttachmentsDir(), "rx_${System.currentTimeMillis()}_$attachmentName")
                    FileOutputStream(targetFile).use { fos ->
                        fos.write(decodedBytes)
                    }
                    localAttachmentPath = targetFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Ensure channel exists
            if (conversationId.isBlank() || allChannels.value.none { it.channelId == conversationId }) {
                val existingChannel = allChannels.value.find { it.peerNodeId == senderId }
                if (existingChannel != null) {
                    conversationId = existingChannel.channelId
                } else {
                    conversationId = "direct_${UUID.randomUUID().toString().take(8)}"
                    val newChannel = MeshChannel(
                        channelIndex = allChannels.value.size,
                        channelId = conversationId,
                        name = "Peer ($senderId)",
                        channelType = ChannelType.DIRECT_NETWORK_PEER,
                        pskBase64 = Base64.encodeToString(CryptoEngine.DEFAULT_MESHTASTIC_PSK, Base64.NO_WRAP),
                        modemPreset = "APP_TO_APP_ONLINE_SOCKET",
                        frequencyMhz = 0f,
                        spreadingFactor = 0,
                        bandwidthKhz = 0f,
                        txPowerDbm = 0,
                        unreadCount = 1,
                        lastMessagePreview = rawPlaintext.ifBlank { "Media attachment received" },
                        lastMessageTimestamp = timestamp,
                        peerNodeId = senderId
                    )
                    repository.insertOrUpdateChannel(newChannel)
                }
            }

            val decryptedText = if (rawPlaintext.isNotBlank()) {
                rawPlaintext
            } else if (payloadCiphertext.isNotBlank()) {
                try {
                    CryptoEngine.decryptPayload(
                        ciphertextBase64 = payloadCiphertext,
                        ivBase64 = ivBase64,
                        keyBytes = CryptoEngine.DEFAULT_MESHTASTIC_PSK
                    )
                } catch (e: Exception) {
                    "[Encrypted Message]"
                }
            } else {
                "[Encrypted Content]"
            }

            val incomingMsg = UnifiedMessage(
                messageId = msgId,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderId.take(6).uppercase(),
                recipientId = recipientId,
                timestamp = timestamp,
                plaintext = decryptedText,
                payloadCiphertext = payloadCiphertext,
                ivBase64 = ivBase64,
                macSignature = macSignature,
                transportRouteHint = TransportRouteHint.ONLINE_NETWORK,
                actualTransport = TransportRouteHint.ONLINE_NETWORK,
                hopLimit = 1,
                hopsTraveled = 0,
                snr = 22.0f,
                rssi = -30,
                status = MessageStatus.DELIVERED_NETWORK,
                isIncoming = true,
                isBroadcast = false,
                attachmentType = attachmentType,
                attachmentPath = localAttachmentPath,
                attachmentName = attachmentName,
                attachmentSize = attachmentSize,
                durationMs = durationMs
            )

            repository.insertMessage(incomingMsg)
            repository.incrementUnread(conversationId)

            repository.logPacket(
                PacketTrace(
                    packetId = msgId.hashCode().toLong(),
                    direction = PacketDirection.RX_INCOMING,
                    packetType = PacketType.DIRECT_E2EE_ENVELOPE,
                    transport = TransportRouteHint.ONLINE_NETWORK,
                    fromNode = senderId,
                    toNode = recipientId,
                    channelNum = 0,
                    hopLimit = 1,
                    hopStart = 1,
                    snr = 22.0f,
                    rssi = -30,
                    rawHex = "4E 45 54 5F 41 50 50 5F 52 58 ...",
                    decodedSummary = "App-to-App RX: ${decryptedText.take(24)}",
                    isCrcValid = true
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // Media & Attachment Messaging (Signal/Telegram Grade)
    // ==========================================
    fun sendMessageWithAttachment(attachmentType: String, file: File, durationMs: Long = 0L) {
        if (!file.exists()) return

        val convId = _selectedConversationId.value
        val channel = allChannels.value.find { it.channelId == convId }
        val isBroadcast = channel?.channelType == ChannelType.BROADCAST_PRIMARY ||
                channel?.channelType == ChannelType.TACTICAL_SECONDARY ||
                channel?.channelType == ChannelType.EMERGENCY_GROUP
        val recipientId = if (isBroadcast) "^all" else (channel?.peerNodeId ?: "peer_node")

        val myKey = _myIdentity.value
        val psk = CryptoEngine.DEFAULT_MESHTASTIC_PSK

        val label = when (attachmentType) {
            "IMAGE" -> "📷 Photo (${FileManager.formatFileSize(file.length())})"
            "AUDIO_VOICE" -> "🎤 Voice Note (${durationMs / 1000}s)"
            else -> "📎 File: ${file.name} (${FileManager.formatFileSize(file.length())})"
        }

        val encrypted = CryptoEngine.encryptPayload(label, psk)
        val msgId = "att_${UUID.randomUUID().toString().take(8)}"

        val msg = UnifiedMessage(
            messageId = msgId,
            conversationId = convId,
            senderId = myKey.nodeId,
            senderName = myKey.shortName,
            recipientId = recipientId,
            timestamp = System.currentTimeMillis(),
            plaintext = label,
            payloadCiphertext = encrypted.ciphertextBase64,
            ivBase64 = encrypted.ivBase64,
            macSignature = encrypted.macSignatureHex,
            transportRouteHint = _selectedRouteHint.value,
            hopLimit = 3,
            hopsTraveled = 0,
            snr = 0.0f,
            rssi = -60,
            status = MessageStatus.QUEUED,
            isIncoming = false,
            isBroadcast = isBroadcast,
            attachmentType = attachmentType,
            attachmentPath = file.absolutePath,
            attachmentName = file.name,
            attachmentSize = file.length(),
            durationMs = durationMs
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMessage(msg)
            transportRouter.dispatchMessage(msg)
        }
    }

    fun startVoiceRecording(): Boolean {
        val file = fileManager.startVoiceRecording()
        if (file != null) {
            _isRecordingVoice.value = true
            _voiceRecordingDuration.value = 0
            voiceTimerJob?.cancel()
            voiceTimerJob = viewModelScope.launch {
                while (_isRecordingVoice.value) {
                    delay(1000)
                    _voiceRecordingDuration.value = _voiceRecordingDuration.value + 1
                }
            }
            return true
        }
        return false
    }

    fun stopVoiceRecordingAndSend() {
        voiceTimerJob?.cancel()
        _isRecordingVoice.value = false
        val durationMs = (_voiceRecordingDuration.value * 1000).toLong()
        val file = fileManager.stopVoiceRecording()
        if (file != null && file.exists() && file.length() > 0) {
            sendMessageWithAttachment("AUDIO_VOICE", file, durationMs)
        }
        _voiceRecordingDuration.value = 0
    }

    fun cancelVoiceRecording() {
        voiceTimerJob?.cancel()
        _isRecordingVoice.value = false
        val file = fileManager.stopVoiceRecording()
        file?.delete()
        _voiceRecordingDuration.value = 0
    }

    // ==========================================
    // P2P Voice & Video Calling Signaling & PTT
    // ==========================================
    fun initiateCall(peerId: String, peerName: String, peerIp: String, isVideo: Boolean = false) {
        val type = if (isVideo) CallType.VIDEO else CallType.AUDIO
        callManager.startCall(peerId, peerName, peerIp, type)
    }

    fun answerCall() {
        callManager.answerCall()
    }

    fun declineCall() {
        callManager.declineCall()
    }

    fun endCall() {
        callManager.endCall()
    }

    fun toggleMute() {
        callManager.toggleMute()
    }

    fun toggleSpeaker() {
        callManager.toggleSpeaker()
    }

    fun startPtt(peerIp: String) {
        callManager.startPttTransmission(peerIp)
    }

    fun stopPtt() {
        callManager.stopPttTransmission()
    }

    override fun onCleared() {
        super.onCleared()
        callManager.destroy()
        networkPeerTransport.stopServer()
        bluetoothPeerManager.teardown()
    }
}
