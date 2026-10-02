package com.silouder.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.silouder.app.model.ChannelType
import com.silouder.app.model.MeshChannel
import com.silouder.app.model.PacketDirection
import com.silouder.app.model.PacketTrace
import com.silouder.app.model.PacketType
import com.silouder.app.model.TransportRouteHint
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val channelId: String,
    val channelIndex: Int,
    val name: String,
    val channelType: String,
    val pskBase64: String,
    val modemPreset: String,
    val frequencyMhz: Float,
    val spreadingFactor: Int,
    val bandwidthKhz: Float,
    val codingRate: String,
    val txPowerDbm: Int,
    val unreadCount: Int,
    val lastMessagePreview: String,
    val lastMessageTimestamp: Long,
    val isPrimary: Boolean,
    val peerNodeId: String?,
    val joinCode: String = "",
    val memberCount: Int = 1,
    val expiresAt: Long = 0L,
    val isEmergencyGroup: Boolean = false,
    val incidentLocation: String = "",
    val isBleBeaconBroadcast: Boolean = false
) {
    fun toDomain(): MeshChannel = MeshChannel(
        channelIndex = channelIndex,
        channelId = channelId,
        name = name,
        channelType = runCatching { ChannelType.valueOf(channelType) }.getOrDefault(ChannelType.BROADCAST_PRIMARY),
        pskBase64 = pskBase64,
        modemPreset = modemPreset,
        frequencyMhz = frequencyMhz,
        spreadingFactor = spreadingFactor,
        bandwidthKhz = bandwidthKhz,
        codingRate = codingRate,
        txPowerDbm = txPowerDbm,
        unreadCount = unreadCount,
        lastMessagePreview = lastMessagePreview,
        lastMessageTimestamp = lastMessageTimestamp,
        isPrimary = isPrimary,
        peerNodeId = peerNodeId,
        joinCode = joinCode,
        memberCount = memberCount,
        expiresAt = expiresAt,
        isEmergencyGroup = isEmergencyGroup,
        incidentLocation = incidentLocation,
        isBleBeaconBroadcast = isBleBeaconBroadcast
    )

    companion object {
        fun fromDomain(c: MeshChannel): ChannelEntity = ChannelEntity(
            channelId = c.channelId,
            channelIndex = c.channelIndex,
            name = c.name,
            channelType = c.channelType.name,
            pskBase64 = c.pskBase64,
            modemPreset = c.modemPreset,
            frequencyMhz = c.frequencyMhz,
            spreadingFactor = c.spreadingFactor,
            bandwidthKhz = c.bandwidthKhz,
            codingRate = c.codingRate,
            txPowerDbm = c.txPowerDbm,
            unreadCount = c.unreadCount,
            lastMessagePreview = c.lastMessagePreview,
            lastMessageTimestamp = c.lastMessageTimestamp,
            isPrimary = c.isPrimary,
            peerNodeId = c.peerNodeId,
            joinCode = c.joinCode,
            memberCount = c.memberCount,
            expiresAt = c.expiresAt,
            isEmergencyGroup = c.isEmergencyGroup,
            incidentLocation = c.incidentLocation,
            isBleBeaconBroadcast = c.isBleBeaconBroadcast
        )
    }
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY isEmergencyGroup DESC, channelIndex ASC, lastMessageTimestamp DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE channelId = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE joinCode = :code LIMIT 1")
    suspend fun getChannelByJoinCode(code: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET lastMessagePreview = :preview, lastMessageTimestamp = :time WHERE channelId = :id")
    suspend fun updateLastMessage(id: String, preview: String, time: Long)

    @Query("UPDATE channels SET unreadCount = 0 WHERE channelId = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE channels SET memberCount = memberCount + 1 WHERE channelId = :id")
    suspend fun incrementMemberCount(id: String)

    @Query("DELETE FROM channels WHERE channelId = :id")
    suspend fun deleteChannel(id: String)
}

@Entity(tableName = "packet_logs")
data class PacketLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packetId: Long,
    val timestamp: Long,
    val direction: String,
    val packetType: String,
    val transport: String,
    val fromNode: String,
    val toNode: String,
    val channelNum: Int,
    val hopLimit: Int,
    val hopStart: Int,
    val snr: Float,
    val rssi: Int,
    val rawHex: String,
    val decodedSummary: String,
    val isCrcValid: Boolean,
    val fragmentIndex: Int?,
    val totalFragments: Int?
) {
    fun toDomain(): PacketTrace = PacketTrace(
        packetId = packetId,
        timestamp = timestamp,
        direction = runCatching { PacketDirection.valueOf(direction) }.getOrDefault(PacketDirection.RX_INCOMING),
        packetType = runCatching { PacketType.valueOf(packetType) }.getOrDefault(PacketType.MESH_BROADCAST_TEXT),
        transport = runCatching { TransportRouteHint.valueOf(transport) }.getOrDefault(TransportRouteHint.LORA_MESH),
        fromNode = fromNode,
        toNode = toNode,
        channelNum = channelNum,
        hopLimit = hopLimit,
        hopStart = hopStart,
        snr = snr,
        rssi = rssi,
        rawHex = rawHex,
        decodedSummary = decodedSummary,
        isCrcValid = isCrcValid,
        fragmentIndex = fragmentIndex,
        totalFragments = totalFragments
    )

    companion object {
        fun fromDomain(trace: PacketTrace): PacketLogEntity = PacketLogEntity(
            packetId = trace.packetId,
            timestamp = trace.timestamp,
            direction = trace.direction.name,
            packetType = trace.packetType.name,
            transport = trace.transport.name,
            fromNode = trace.fromNode,
            toNode = trace.toNode,
            channelNum = trace.channelNum,
            hopLimit = trace.hopLimit,
            hopStart = trace.hopStart,
            snr = trace.snr,
            rssi = trace.rssi,
            rawHex = trace.rawHex,
            decodedSummary = trace.decodedSummary,
            isCrcValid = trace.isCrcValid,
            fragmentIndex = trace.fragmentIndex,
            totalFragments = trace.totalFragments
        )
    }
}

@Dao
interface PacketLogDao {
    @Query("SELECT * FROM packet_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentPackets(): Flow<List<PacketLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacket(packet: PacketLogEntity)

    @Query("DELETE FROM packet_logs")
    suspend fun clearLogs()
}
