package com.silouder.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.silouder.app.model.HardwareModel
import com.silouder.app.model.MeshNode
import com.silouder.app.model.NodeRole
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "mesh_nodes")
data class NodeEntity(
    @PrimaryKey val nodeId: String,
    val longName: String,
    val shortName: String,
    val role: String,
    val hardwareModel: String,
    val publicKeyHex: String,
    val onionAddress: String,
    val lastHeardTimestamp: Long,
    val snr: Float,
    val rssi: Int,
    val batteryLevel: Int,
    val voltage: Float,
    val channelUtilization: Float,
    val hopCount: Int,
    val isDirectNeighbor: Boolean,
    val isBleConnectedRadio: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Int?,
    val isOnline: Boolean,
    val isFavorited: Boolean
) {
    fun toDomain(): MeshNode = MeshNode(
        nodeId = nodeId,
        longName = longName,
        shortName = shortName,
        role = runCatching { NodeRole.valueOf(role) }.getOrDefault(NodeRole.CLIENT),
        hardwareModel = runCatching { HardwareModel.valueOf(hardwareModel) }.getOrDefault(HardwareModel.HELTEC_V3),
        publicKeyHex = publicKeyHex,
        onionAddress = onionAddress,
        lastHeardTimestamp = lastHeardTimestamp,
        snr = snr,
        rssi = rssi,
        batteryLevel = batteryLevel,
        voltage = voltage,
        channelUtilization = channelUtilization,
        hopCount = hopCount,
        isDirectNeighbor = isDirectNeighbor,
        isBleConnectedRadio = isBleConnectedRadio,
        latitude = latitude,
        longitude = longitude,
        altitude = altitude,
        isOnline = isOnline,
        isFavorited = isFavorited
    )

    companion object {
        fun fromDomain(node: MeshNode): NodeEntity = NodeEntity(
            nodeId = node.nodeId,
            longName = node.longName,
            shortName = node.shortName,
            role = node.role.name,
            hardwareModel = node.hardwareModel.name,
            publicKeyHex = node.publicKeyHex,
            onionAddress = node.onionAddress,
            lastHeardTimestamp = node.lastHeardTimestamp,
            snr = node.snr,
            rssi = node.rssi,
            batteryLevel = node.batteryLevel,
            voltage = node.voltage,
            channelUtilization = node.channelUtilization,
            hopCount = node.hopCount,
            isDirectNeighbor = node.isDirectNeighbor,
            isBleConnectedRadio = node.isBleConnectedRadio,
            latitude = node.latitude,
            longitude = node.longitude,
            altitude = node.altitude,
            isOnline = node.isOnline,
            isFavorited = node.isFavorited
        )
    }
}

@Dao
interface NodeDao {
    @Query("SELECT * FROM mesh_nodes ORDER BY isBleConnectedRadio DESC, lastHeardTimestamp DESC")
    fun getAllNodes(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM mesh_nodes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun getNodeById(nodeId: String): NodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNode(node: NodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNodes(nodes: List<NodeEntity>)

    @Query("UPDATE mesh_nodes SET isBleConnectedRadio = :connected WHERE nodeId = :nodeId")
    suspend fun setBleConnected(nodeId: String, connected: Boolean)

    @Query("UPDATE mesh_nodes SET lastHeardTimestamp = :time, snr = :snr, rssi = :rssi, batteryLevel = :battery, isOnline = 1 WHERE nodeId = :nodeId")
    suspend fun updateTelemetry(nodeId: String, time: Long, snr: Float, rssi: Int, battery: Int)

    @Query("DELETE FROM mesh_nodes WHERE nodeId = :nodeId")
    suspend fun deleteNode(nodeId: String)
}
