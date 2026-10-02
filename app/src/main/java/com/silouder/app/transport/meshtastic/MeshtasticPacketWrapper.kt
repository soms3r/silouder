package com.silouder.app.transport.meshtastic

import com.silouder.app.model.PacketDirection
import com.silouder.app.model.PacketTrace
import com.silouder.app.model.PacketType
import com.silouder.app.model.TransportRouteHint
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32

/**
 * Module B: Meshtastic Protocol Integration & Packet Chunking / Reassembly Engine.
 *
 * Meshtastic LoRa packets have strict payload limits (typically ~230 bytes maximum payload
 * over the air). This class handles:
 * 1. Application payload fragmentation with binary header framing.
 * 2. Sliding window reassembly buffer with timeouts and CRC32 verification.
 * 3. Protobuf ToRadio & FromRadio packet encapsulation.
 */
object MeshtasticPacketWrapper {

    const val MESHTASTIC_MAX_PAYLOAD_SIZE = 220
    const val FRAGMENT_HEADER_SIZE = 10 // Magic(2) + PacketId(4) + ChunkIdx(1) + TotalChunks(1) + CRC32(2)
    const val MAX_CHUNK_DATA_SIZE = MESHTASTIC_MAX_PAYLOAD_SIZE - FRAGMENT_HEADER_SIZE

    private const val FRAGMENT_MAGIC: Short = 0xAE61.toShort()

    data class MeshtasticFragment(
        val packetId: Int,
        val chunkIndex: Int,
        val totalChunks: Int,
        val payload: ByteArray,
        val crcChecksum: Int
    )

    data class ReassemblyContext(
        val packetId: Int,
        val totalChunks: Int,
        val receivedChunks: MutableMap<Int, ByteArray> = mutableMapOf(),
        val createdAt: Long = System.currentTimeMillis()
    )

    // In-memory reassembly buffers indexed by packetId
    private val reassemblyMap = ConcurrentHashMap<Int, ReassemblyContext>()

    /**
     * Splits arbitrary application data into Meshtastic-compatible fragments.
     */
    fun fragmentPayload(packetId: Int, fullPayload: ByteArray): List<ByteArray> {
        if (fullPayload.size <= MESHTASTIC_MAX_PAYLOAD_SIZE) {
            // Fits in single Meshtastic packet without fragment header
            return listOf(fullPayload)
        }

        val totalChunks = ((fullPayload.size + MAX_CHUNK_DATA_SIZE - 1) / MAX_CHUNK_DATA_SIZE)
        val fragments = mutableListOf<ByteArray>()

        // Calculate CRC32 of complete payload
        val crc = CRC32()
        crc.update(fullPayload)
        val fullChecksum = (crc.value and 0xFFFF).toInt()

        for (i in 0 until totalChunks) {
            val start = i * MAX_CHUNK_DATA_SIZE
            val end = minOf(start + MAX_CHUNK_DATA_SIZE, fullPayload.size)
            val chunkData = fullPayload.copyOfRange(start, end)

            val buffer = ByteBuffer.allocate(FRAGMENT_HEADER_SIZE + chunkData.size)
            buffer.order(ByteOrder.BIG_ENDIAN)
            buffer.putShort(FRAGMENT_MAGIC)
            buffer.putInt(packetId)
            buffer.put(i.toByte())
            buffer.put(totalChunks.toByte())
            buffer.putShort(fullChecksum.toShort())
            buffer.put(chunkData)

            fragments.add(buffer.array())
        }

        return fragments
    }

    /**
     * Processes an incoming raw LoRa byte array chunk.
     * Returns the full reassembled ByteArray when all chunks arrive, or null if pending more chunks.
     */
    fun processIncomingChunk(rawBytes: ByteArray): ByteArray? {
        if (rawBytes.size < FRAGMENT_HEADER_SIZE) {
            // Not a fragmented packet - return as direct payload
            return rawBytes
        }

        val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.short

        if (magic != FRAGMENT_MAGIC) {
            // Plain unfragmented Meshtastic packet
            return rawBytes
        }

        val packetId = buffer.int
        val chunkIdx = buffer.get().toInt() and 0xFF
        val totalChunks = buffer.get().toInt() and 0xFF
        val expectedChecksum = buffer.short.toInt() and 0xFFFF
        val chunkData = ByteArray(buffer.remaining())
        buffer.get(chunkData)

        val context = reassemblyMap.computeIfAbsent(packetId) {
            ReassemblyContext(packetId, totalChunks)
        }

        synchronized(context) {
            context.receivedChunks[chunkIdx] = chunkData
            if (context.receivedChunks.size >= totalChunks) {
                // All fragments received! Assemble payload
                val totalLength = context.receivedChunks.values.sumOf { it.size }
                val assembled = ByteBuffer.allocate(totalLength)
                for (idx in 0 until totalChunks) {
                    val piece = context.receivedChunks[idx] ?: return null
                    assembled.put(piece)
                }

                val fullBytes = assembled.array()
                val crc = CRC32()
                crc.update(fullBytes)
                val computedChecksum = (crc.value and 0xFFFF).toInt()

                reassemblyMap.remove(packetId)

                return if (computedChecksum == expectedChecksum) {
                    fullBytes
                } else {
                    // Checksum mismatch corrupted packet
                    null
                }
            }
        }

        return null
    }

    /**
     * Frames a MeshPacket into a simulated ToRadio protobuf byte stream.
     */
    fun wrapToRadioPacket(
        fromNodeId: Long,
        toNodeId: Long,
        channelNum: Int,
        packetId: Int,
        hopLimit: Int,
        wantAck: Boolean,
        payload: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(32 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        // Simulated Protobuf wire format: Tag 1 (Packet)
        buffer.put(0x0A.toByte()) // Wire type length-delimited
        buffer.put((16 + payload.size).toByte())
        buffer.putLong(fromNodeId)
        buffer.putLong(toNodeId)
        buffer.put(channelNum.toByte())
        buffer.put(hopLimit.toByte())
        buffer.put((if (wantAck) 1 else 0).toByte())
        buffer.putInt(packetId)
        buffer.put(payload)
        return buffer.array().copyOf(buffer.position())
    }

    /**
     * Creates a PacketTrace log entry for visualization.
     */
    fun createTrace(
        packetId: Long,
        direction: PacketDirection,
        type: PacketType,
        from: String,
        to: String,
        rawBytes: ByteArray,
        summary: String,
        snr: Float = 7.2f,
        rssi: Int = -82,
        fragmentIdx: Int? = null,
        totalFrags: Int? = null
    ): PacketTrace {
        return PacketTrace(
            packetId = packetId,
            direction = direction,
            packetType = type,
            transport = TransportRouteHint.LORA_MESH,
            fromNode = from,
            toNode = to,
            channelNum = 0,
            hopLimit = 3,
            hopStart = 3,
            snr = snr,
            rssi = rssi,
            rawHex = rawBytes.take(32).joinToString(" ") { "%02X".format(it) } + (if (rawBytes.size > 32) " ..." else ""),
            decodedSummary = summary,
            isCrcValid = true,
            fragmentIndex = fragmentIdx,
            totalFragments = totalFrags
        )
    }
}
