package com.silouder.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.silouder.app.model.MessageStatus
import com.silouder.app.model.TransportRouteHint
import com.silouder.app.model.UnifiedMessage
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String,
    val timestamp: Long,
    val plaintext: String,
    val payloadCiphertext: String,
    val ivBase64: String,
    val macSignature: String,
    val transportRouteHint: String,
    val actualTransport: String?,
    val hopLimit: Int,
    val hopsTraveled: Int,
    val snr: Float,
    val rssi: Int,
    val status: String,
    val syncVectorSeq: Long,
    val isIncoming: Boolean,
    val isBroadcast: Boolean,
    val fragmentCount: Int,
    val deliveryAttempts: Int,
    val isUrgentAlert: Boolean = false,
    val attachmentType: String? = null,
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val attachmentSize: Long = 0L,
    val durationMs: Long = 0L
) {
    fun toDomain(): UnifiedMessage = UnifiedMessage(
        messageId = messageId,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        recipientId = recipientId,
        timestamp = timestamp,
        plaintext = plaintext,
        payloadCiphertext = payloadCiphertext,
        ivBase64 = ivBase64,
        macSignature = macSignature,
        transportRouteHint = runCatching { TransportRouteHint.valueOf(transportRouteHint) }.getOrDefault(TransportRouteHint.AUTO_BEST),
        actualTransport = actualTransport?.let { runCatching { TransportRouteHint.valueOf(it) }.getOrNull() },
        hopLimit = hopLimit,
        hopsTraveled = hopsTraveled,
        snr = snr,
        rssi = rssi,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.QUEUED),
        syncVectorSeq = syncVectorSeq,
        isIncoming = isIncoming,
        isBroadcast = isBroadcast,
        fragmentCount = fragmentCount,
        deliveryAttempts = deliveryAttempts,
        isUrgentAlert = isUrgentAlert,
        attachmentType = attachmentType,
        attachmentPath = attachmentPath,
        attachmentName = attachmentName,
        attachmentSize = attachmentSize,
        durationMs = durationMs
    )

    companion object {
        fun fromDomain(msg: UnifiedMessage): MessageEntity = MessageEntity(
            messageId = msg.messageId,
            conversationId = msg.conversationId,
            senderId = msg.senderId,
            senderName = msg.senderName,
            recipientId = msg.recipientId,
            timestamp = msg.timestamp,
            plaintext = msg.plaintext,
            payloadCiphertext = msg.payloadCiphertext,
            ivBase64 = msg.ivBase64,
            macSignature = msg.macSignature,
            transportRouteHint = msg.transportRouteHint.name,
            actualTransport = msg.actualTransport?.name,
            hopLimit = msg.hopLimit,
            hopsTraveled = msg.hopsTraveled,
            snr = msg.snr,
            rssi = msg.rssi,
            status = msg.status.name,
            syncVectorSeq = msg.syncVectorSeq,
            isIncoming = msg.isIncoming,
            isBroadcast = msg.isBroadcast,
            fragmentCount = msg.fragmentCount,
            deliveryAttempts = msg.deliveryAttempts,
            isUrgentAlert = msg.isUrgentAlert,
            attachmentType = msg.attachmentType,
            attachmentPath = msg.attachmentPath,
            attachmentName = msg.attachmentName,
            attachmentSize = msg.attachmentSize,
            durationMs = msg.durationMs
        )
    }
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = 'QUEUED' OR status = 'TRANSMITTING' ORDER BY timestamp ASC")
    fun getQueuedMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = 'QUEUED' OR status = 'TRANSMITTING' ORDER BY timestamp ASC")
    suspend fun getQueuedMessagesSync(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE messageId = :id LIMIT 1")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status, actualTransport = :actualTransport, hopsTraveled = :hops, snr = :snr, rssi = :rssi WHERE messageId = :id")
    suspend fun updateMessageStatus(id: String, status: String, actualTransport: String?, hops: Int, snr: Float, rssi: Int)

    @Query("DELETE FROM messages WHERE messageId = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteMessagesForConversation(convId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
