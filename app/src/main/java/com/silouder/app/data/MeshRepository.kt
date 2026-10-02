package com.silouder.app.data

import com.silouder.app.model.MeshChannel
import com.silouder.app.model.MeshNode
import com.silouder.app.model.PacketTrace
import com.silouder.app.model.UnifiedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeshRepository(private val database: AppDatabase) {

    val allMessages: Flow<List<UnifiedMessage>> = database.messageDao().getAllMessages()
        .map { list -> list.map { it.toDomain() } }

    val queuedMessages: Flow<List<UnifiedMessage>> = database.messageDao().getQueuedMessages()
        .map { list -> list.map { it.toDomain() } }

    val allNodes: Flow<List<MeshNode>> = database.nodeDao().getAllNodes()
        .map { list -> list.map { it.toDomain() } }

    val allChannels: Flow<List<MeshChannel>> = database.channelDao().getAllChannels()
        .map { list -> list.map { it.toDomain() } }

    val recentPackets: Flow<List<PacketTrace>> = database.packetLogDao().getRecentPackets()
        .map { list -> list.map { it.toDomain() } }

    fun getMessagesForConversation(convId: String): Flow<List<UnifiedMessage>> =
        database.messageDao().getMessagesForConversation(convId)
            .map { list -> list.map { it.toDomain() } }

    suspend fun insertMessage(msg: UnifiedMessage) {
        database.messageDao().insertMessage(MessageEntity.fromDomain(msg))
        database.channelDao().updateLastMessage(msg.conversationId, msg.plaintext, msg.timestamp)
    }

    suspend fun updateMessageStatus(id: String, status: String, actualTransport: String?, hops: Int, snr: Float, rssi: Int) {
        database.messageDao().updateMessageStatus(id, status, actualTransport, hops, snr, rssi)
    }

    suspend fun getQueuedMessagesSync(): List<UnifiedMessage> {
        return database.messageDao().getQueuedMessagesSync().map { it.toDomain() }
    }

    suspend fun insertOrUpdateNode(node: MeshNode) {
        database.nodeDao().insertOrUpdateNode(NodeEntity.fromDomain(node))
    }

    suspend fun insertOrUpdateNodes(nodes: List<MeshNode>) {
        database.nodeDao().insertOrUpdateNodes(nodes.map { NodeEntity.fromDomain(it) })
    }

    suspend fun setBleConnectedNode(nodeId: String, connected: Boolean) {
        database.nodeDao().setBleConnected(nodeId, connected)
    }

    suspend fun updateNodeTelemetry(nodeId: String, snr: Float, rssi: Int, battery: Int) {
        database.nodeDao().updateTelemetry(nodeId, System.currentTimeMillis(), snr, rssi, battery)
    }

    suspend fun insertOrUpdateChannel(channel: MeshChannel) {
        database.channelDao().insertOrUpdate(ChannelEntity.fromDomain(channel))
    }

    suspend fun insertOrUpdateChannels(channels: List<MeshChannel>) {
        database.channelDao().insertOrUpdateAll(channels.map { ChannelEntity.fromDomain(it) })
    }

    suspend fun getChannelByJoinCode(code: String): MeshChannel? {
        return database.channelDao().getChannelByJoinCode(code.trim().uppercase())?.toDomain()
    }

    suspend fun incrementMemberCount(channelId: String) {
        database.channelDao().incrementMemberCount(channelId)
    }

    suspend fun deleteChannelAndMessages(channelId: String) {
        database.channelDao().deleteChannel(channelId)
        database.messageDao().deleteMessagesForConversation(channelId)
    }

    suspend fun markChannelRead(channelId: String) {
        database.channelDao().markAsRead(channelId)
    }

    suspend fun incrementUnread(channelId: String) {
        database.channelDao().incrementUnread(channelId)
    }

    suspend fun logPacket(packet: PacketTrace) {
        database.packetLogDao().insertPacket(PacketLogEntity.fromDomain(packet))
    }

    suspend fun clearPacketLogs() {
        database.packetLogDao().clearLogs()
    }
}
