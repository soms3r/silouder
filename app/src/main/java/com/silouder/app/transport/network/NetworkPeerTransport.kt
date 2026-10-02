package com.silouder.app.transport.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.silouder.app.model.UnifiedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

data class NetworkPeer(
    val peerId: String,             // Node ID
    val alias: String,              // Display Name
    val ipAddress: String,          // e.g. "192.168.1.45"
    val port: Int = 8888,
    val isOnline: Boolean = true,
    val latencyMs: Long = 0,
    val lastPingTimestamp: Long = System.currentTimeMillis(),
    val isSubnetDiscovered: Boolean = true
) {
    val fullAddress: String get() = "$ipAddress:$port"
}

enum class NetworkServerStatus {
    STOPPED,
    STARTING,
    LISTENING
}

/**
 * Real App-to-App Online Network Transport for Silouder.
 * Real Features:
 * 1. Embedded HTTP/Socket ServerSocket listening on port 8888.
 * 2. Real Android NsdManager (mDNS) discovery across local Wi-Fi / Hotspot.
 * 3. Real OkHttp client sending encrypted E2EE envelopes to peer endpoints.
 * 4. Manual IP/port peer entry with live ping latency verification.
 */
class NetworkPeerTransport(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val SERVICE_TYPE = "_silouder._tcp."
        const val DEFAULT_PORT = 8888
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _serverStatus = MutableStateFlow(NetworkServerStatus.STOPPED)
    val serverStatus: StateFlow<NetworkServerStatus> = _serverStatus.asStateFlow()

    private val _localPort = MutableStateFlow(DEFAULT_PORT)
    val localPort: StateFlow<Int> = _localPort.asStateFlow()

    private val _localIpAddress = MutableStateFlow(getLocalIpAddress())
    val localIpAddress: StateFlow<String> = _localIpAddress.asStateFlow()

    private val _networkPeers = MutableStateFlow<List<NetworkPeer>>(emptyList())
    val networkPeers: StateFlow<List<NetworkPeer>> = _networkPeers.asStateFlow()

    private val _incomingNetworkMessages = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val incomingNetworkMessages: SharedFlow<JSONObject> = _incomingNetworkMessages.asSharedFlow()

    private val _networkLogs = MutableStateFlow<List<String>>(
        listOf(
            "[NET-INIT] Real Direct P2P Network Transport initializing...",
            "[NET-DISCOVERY] Ready to discover peers via Wi-Fi mDNS / NSD."
        )
    )
    val networkLogs: StateFlow<List<String>> = _networkLogs.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    init {
        startServer()
        startNsdServices()
    }

    fun logNetEvent(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _networkLogs.value = listOf("[$timestamp] $msg") + _networkLogs.value.take(40)
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled) {
            _networkPeers.value = listOf(
                NetworkPeer("!9c4d12ef", "Station Echo (Local WiFi)", "192.168.1.120", 8888, true, 12, System.currentTimeMillis(), true),
                NetworkPeer("!3b7e41fa", "Bravo Node (Home Lab)", "192.168.1.185", 8888, true, 18, System.currentTimeMillis(), true),
                NetworkPeer("!7a9f1b2c", "Cloud Relay Peer", "10.0.0.42", 9090, true, 45, System.currentTimeMillis(), false)
            )
            logNetEvent("Demo / Testing Sandbox enabled: Mock peers loaded.")
        } else {
            _networkPeers.value = emptyList()
            scanLocalSubnet()
            logNetEvent("Live Production Mode active: Scanning real local network peers.")
        }
    }

    /**
     * Starts the embedded TCP/HTTP message server.
     */
    fun startServer() {
        if (serverJob?.isActive == true) return

        _serverStatus.value = NetworkServerStatus.STARTING
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                var port = DEFAULT_PORT
                try {
                    serverSocket = ServerSocket(port)
                } catch (e: Exception) {
                    // Port 8888 busy: bind to any available ephemeral port
                    serverSocket = ServerSocket(0)
                    port = serverSocket?.localPort ?: DEFAULT_PORT
                }

                _localPort.value = port
                _localIpAddress.value = getLocalIpAddress()
                _serverStatus.value = NetworkServerStatus.LISTENING
                logNetEvent("Embedded P2P Server listening on ${_localIpAddress.value}:$port")

                while (_serverStatus.value == NetworkServerStatus.LISTENING && serverSocket != null && !serverSocket!!.isClosed) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch(Dispatchers.IO) {
                        handleClientConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                _serverStatus.value = NetworkServerStatus.STOPPED
                logNetEvent("Server stopped or error: ${e.message}")
            }
        }
    }

    fun stopServer() {
        _serverStatus.value = NetworkServerStatus.STOPPED
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverJob?.cancel()
        serverSocket = null
        logNetEvent("Embedded P2P Server stopped.")
    }

    /**
     * Handles incoming HTTP connection from a remote Silouder peer.
     */
    private fun handleClientConnection(socket: Socket) {
        try {
            socket.soTimeout = 5000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0) ?: ""
            val path = parts.getOrNull(1) ?: ""

            // Read HTTP headers
            var contentLength = 0
            var headerLine = reader.readLine()
            while (!headerLine.isNullOrBlank()) {
                if (headerLine.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                headerLine = reader.readLine()
            }

            // Read Body
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(1024)
                var bytesRead = 0
                var totalRead = 0
                while (totalRead < contentLength && reader.read(buffer, 0, (contentLength - totalRead).coerceAtMost(buffer.size)).also { bytesRead = it } != -1) {
                    bodyBuilder.append(buffer, 0, bytesRead)
                    totalRead += bytesRead
                }
            }

            val body = bodyBuilder.toString()

            when {
                path == "/api/ping" -> {
                    writer.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n{\"status\":\"PONG\"}")
                    writer.flush()
                }
                path == "/api/message" && method == "POST" -> {
                    val json = JSONObject(body)
                    scope.launch {
                        _incomingNetworkMessages.emit(json)
                    }
                    val msgId = json.optString("messageId", "ack")
                    val response = "{\"status\":\"ACK\",\"messageId\":\"$msgId\"}"
                    writer.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${response.length}\r\n\r\n$response")
                    writer.flush()
                    logNetEvent("Received message envelope from ${socket.inetAddress.hostAddress} (ID: ${msgId.take(6)})")
                }
                else -> {
                    writer.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n")
                    writer.flush()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {}
        }
    }

    /**
     * Transmits an encrypted message directly app-to-app via real HTTP POST.
     */
    suspend fun transmitAppToAppOnline(
        targetIp: String,
        port: Int,
        payloadCiphertext: String,
        messageId: String,
        senderId: String = "!me",
        recipientId: String = "!peer",
        conversationId: String = "!conv",
        plaintext: String = "",
        ivBase64: String = "",
        macSignature: String = "",
        attachmentType: String? = null,
        attachmentName: String? = null,
        attachmentSize: Long = 0L,
        durationMs: Long = 0L,
        attachmentData: String? = null
    ): Boolean {
        return try {
            logNetEvent("Transmitting message to $targetIp:$port (Msg ID: ${messageId.take(6)})...")

            val json = JSONObject().apply {
                put("messageId", messageId)
                put("senderId", senderId)
                put("recipientId", recipientId)
                put("conversationId", conversationId)
                put("plaintext", plaintext)
                put("payloadCiphertext", payloadCiphertext)
                put("ivBase64", ivBase64)
                put("macSignature", macSignature)
                put("timestamp", System.currentTimeMillis())
                if (attachmentType != null) {
                    put("attachmentType", attachmentType)
                    put("attachmentName", attachmentName ?: "")
                    put("attachmentSize", attachmentSize)
                    put("durationMs", durationMs)
                    if (attachmentData != null) {
                        put("attachmentData", attachmentData)
                    }
                }
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("http://$targetIp:$port/api/message")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val isSuccess = response.isSuccessful
            response.close()

            if (isSuccess) {
                logNetEvent("Direct App-to-App delivery confirmed by $targetIp:$port. HTTP 200 OK ACK.")
                true
            } else {
                logNetEvent("Delivery rejected by $targetIp:$port (HTTP ${response.code}).")
                false
            }
        } catch (e: Exception) {
            logNetEvent("Delivery failed to $targetIp:$port: ${e.message}")
            false
        }
    }

    /**
     * Pings a peer to verify connectivity and real roundtrip latency.
     */
    fun pingPeer(ipAddress: String, port: Int = 8888) {
        scope.launch(Dispatchers.IO) {
            logNetEvent("Pinging peer at $ipAddress:$port...")
            val startTime = System.currentTimeMillis()
            try {
                val request = Request.Builder()
                    .url("http://$ipAddress:$port/api/ping")
                    .get()
                    .build()
                val response = httpClient.newCall(request).execute()
                val latency = System.currentTimeMillis() - startTime
                val isOk = response.isSuccessful
                response.close()

                if (isOk) {
                    updatePeerStatus(ipAddress, isOnline = true, latencyMs = latency)
                    logNetEvent("Ping ACK from $ipAddress: Online (${latency}ms)")
                } else {
                    updatePeerStatus(ipAddress, isOnline = false, latencyMs = 0)
                }
            } catch (e: Exception) {
                updatePeerStatus(ipAddress, isOnline = false, latencyMs = 0)
                logNetEvent("Ping failed to $ipAddress: Peer unreachable.")
            }
        }
    }

    /**
     * Scans local Wi-Fi subnet using mDNS / NSD.
     */
    fun scanLocalSubnet() {
        scope.launch(Dispatchers.IO) {
            _localIpAddress.value = getLocalIpAddress()
            logNetEvent("Refreshing local IP: ${_localIpAddress.value}:${_localPort.value}")
            restartNsdDiscovery()
        }
    }

    /**
     * Adds a new network peer manually by IP/hostname and Port.
     */
    fun addNetworkPeer(alias: String, ipAddress: String, port: Int = DEFAULT_PORT) {
        val cleanIp = ipAddress.trim()
        val safePort = if (port in 1..65535) port else DEFAULT_PORT
        val newPeer = NetworkPeer(
            peerId = "!net_${cleanIp.replace(".", "").takeLast(6)}",
            alias = alias.ifBlank { "Peer ($cleanIp)" },
            ipAddress = cleanIp,
            port = safePort,
            isOnline = true,
            latencyMs = 0,
            isSubnetDiscovered = false
        )

        val current = _networkPeers.value.filterNot { it.ipAddress == cleanIp }
        _networkPeers.value = current + newPeer
        logNetEvent("Added network peer $alias at $cleanIp:$safePort")
        pingPeer(cleanIp, safePort)
    }

    fun removeNetworkPeer(ipAddress: String) {
        _networkPeers.value = _networkPeers.value.filterNot { it.ipAddress == ipAddress }
        logNetEvent("Removed network peer at $ipAddress")
    }

    private fun updatePeerStatus(ipAddress: String, isOnline: Boolean, latencyMs: Long) {
        val updated = _networkPeers.value.map {
            if (it.ipAddress == ipAddress) {
                it.copy(
                    isOnline = isOnline,
                    latencyMs = latencyMs,
                    lastPingTimestamp = System.currentTimeMillis()
                )
            } else it
        }
        _networkPeers.value = updated
    }

    private fun startNsdServices() {
        registerNsdService()
        restartNsdDiscovery()
    }

    private fun registerNsdService() {
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "Silouder-${_localPort.value}"
                serviceType = SERVICE_TYPE
                port = _localPort.value
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    logNetEvent("mDNS registered: ${serviceInfo.serviceName} on port ${serviceInfo.port}")
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    logNetEvent("mDNS registration failed (Code: $errorCode)")
                }
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            }

            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restartNsdDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {}

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                logNetEvent("mDNS discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("silouder", ignoreCase = true)) {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress ?: return
                            val port = serviceInfo.port
                            // Do not add self
                            if (host == _localIpAddress.value && port == _localPort.value) return

                            val peer = NetworkPeer(
                                peerId = "!net_${host.replace(".", "").takeLast(6)}",
                                alias = serviceInfo.serviceName ?: "Peer ($host)",
                                ipAddress = host,
                                port = port,
                                isOnline = true,
                                latencyMs = 0,
                                isSubnetDiscovered = true
                            )
                            val current = _networkPeers.value.filterNot { it.ipAddress == host }
                            _networkPeers.value = current + peer
                            logNetEvent("Discovered Silouder peer via mDNS: $host:$port")
                            pingPeer(host, port)
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                try { nsdManager?.stopServiceDiscovery(this) } catch (e: Exception) {}
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {}
        return "127.0.0.1"
    }
}
