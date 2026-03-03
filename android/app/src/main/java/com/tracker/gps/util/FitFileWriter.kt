package com.tracker.gps.util

import android.location.Location
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A simplified FIT file writer for GPS tracks.
 * Implements a subset of the FIT protocol to generate activity files.
 */
class FitFileWriter {

    companion object {
        private const val SEMICIRCLES_FACTOR = (2147483648.0 / 180.0)
        
        // Message Numbers
        private const val MSG_FILE_ID = 0
        private const val MSG_DEVICE_INFO = 23
        private const val MSG_RECORD = 20
        private const val MSG_EVENT = 21
        private const val MSG_LAP = 19
        private const val MSG_SESSION = 18
        private const val MSG_ACTIVITY = 34

        // FIT Base Types
        private const val BASE_TYPE_UINT8 = 0x00
        private const val BASE_TYPE_SINT8 = 0x01
        private const val BASE_TYPE_UINT16 = 0x84
        private const val BASE_TYPE_SINT16 = 0x83
        private const val BASE_TYPE_UINT32 = 0x8C
        private const val BASE_TYPE_SINT32 = 0x85
        private const val BASE_TYPE_FLOAT32 = 0x88
        private const val BASE_TYPE_UINT32Z = 0x8D
    }

    private var crc = 0

    fun writeFitFile(outputStream: OutputStream, track: List<Pair<Location, Long>>, userName: String) {
        val buffer = mutableListOf<Byte>()
        
        // 1. Write File ID Definition & Data
        writeFileId(buffer)
        
        // 2. Write Device Info
        writeDeviceInfo(buffer)
        
        // 3. Write Records (Trackpoints)
        if (track.isNotEmpty()) {
            writeRecordDefinition(buffer)
            track.forEach { (location, timestamp) ->
                writeRecordData(buffer, location, timestamp)
            }
        }
        
        // 4. Write Activity/Session/Lap (simplified)
        // For simplicity in this implementation, we focus on the records which are the main part of the track.
        
        val dataSize = buffer.size
        
        // 5. Write Header
        val header = createHeader(dataSize)
        outputStream.write(header)
        
        // 6. Write Data
        val dataArray = buffer.toByteArray()
        outputStream.write(dataArray)
        
        // 7. Calculate and Write CRC
        val fullDataForCrc = header + dataArray
        val finalCrc = calculateCrc(fullDataForCrc) 
        
        val crcBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        crcBuffer.putShort(finalCrc.toShort())
        outputStream.write(crcBuffer.array())
    }

    private fun createHeader(dataSize: Int): ByteArray {
        val buffer = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(14.toByte()) // Size
        buffer.put(0x10.toByte()) // Protocol Version
        buffer.putShort(2135.toShort()) // Profile Version
        buffer.putInt(dataSize) // Data Size
        buffer.put(".FIT".toByteArray()) // Data Type
        
        val headerCrc = calculateCrc(buffer.array().sliceArray(0..11))
        buffer.putShort(headerCrc.toShort())
        
        return buffer.array()
    }

    private fun writeFileId(buffer: MutableList<Byte>) {
        // Definition
        buffer.add(0x40.toByte()) // Reserved | Definition | Local Message Type 0
        buffer.add(0.toByte()) // Reserved
        buffer.add(0.toByte()) // Big Endian (0 = Little)
        buffer.add(MSG_FILE_ID.toByte()) // Global Message Number 0 (File ID)
        buffer.add(2.toByte()) // Number of fields
        
        buffer.add(0.toByte()) // Field 0: Type
        buffer.add(1.toByte()) // Size 1
        buffer.add(BASE_TYPE_UINT8.toByte()) // Base Type
        
        buffer.add(3.toByte()) // Field 3: Serial Number
        buffer.add(4.toByte()) // Size 4
        buffer.add(BASE_TYPE_UINT32.toByte()) // Base Type
        
        // Data
        buffer.add(0x00.toByte()) // Message Header (Local 0)
        buffer.add(4.toByte()) // Type: Activity
        val serial = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(12345).array()
        serial.forEach { buffer.add(it) }
    }

    private fun writeDeviceInfo(buffer: MutableList<Byte>) {
        // Definition
        buffer.add(0x41.toByte()) // Local Message Type 1
        buffer.add(0.toByte())
        buffer.add(0.toByte())
        buffer.add(MSG_DEVICE_INFO.toByte())
        buffer.add(1.toByte()) // 1 field
        
        buffer.add(3.toByte()) // Manufacturer
        buffer.add(2.toByte())
        buffer.add(BASE_TYPE_UINT16.toByte())
        
        // Data
        buffer.add(0x01.toByte()) // Local 1
        buffer.add(0xFF.toByte()) // Unknown manufacturer
        buffer.add(0x00.toByte())
    }

    private fun writeRecordDefinition(buffer: MutableList<Byte>) {
        buffer.add(0x42.toByte()) // Local Message Type 2
        buffer.add(0.toByte())
        buffer.add(0.toByte())
        buffer.add(MSG_RECORD.toByte())
        buffer.add(5.toByte()) // 5 fields: timestamp, lat, long, distance, speed
        
        // Timestamp
        buffer.add(253.toByte())
        buffer.add(4.toByte())
        buffer.add(BASE_TYPE_UINT32.toByte())
        
        // Lat
        buffer.add(0.toByte())
        buffer.add(4.toByte())
        buffer.add(BASE_TYPE_SINT32.toByte())
        
        // Long
        buffer.add(1.toByte())
        buffer.add(4.toByte())
        buffer.add(BASE_TYPE_SINT32.toByte())
        
        // Distance
        buffer.add(5.toByte())
        buffer.add(4.toByte())
        buffer.add(BASE_TYPE_UINT32.toByte())
        
        // Speed
        buffer.add(6.toByte())
        buffer.add(2.toByte())
        buffer.add(BASE_TYPE_UINT16.toByte())
    }

    private fun writeRecordData(buffer: MutableList<Byte>, location: Location, timestamp: Long) {
        buffer.add(0x02.toByte()) // Local 2
        
        // 31st Dec 1989 offset for FIT timestamps
        val fitTimestamp = (timestamp / 1000 - 631065600L).toInt()
        
        val lat = (location.latitude * SEMICIRCLES_FACTOR).toInt()
        val lon = (location.longitude * SEMICIRCLES_FACTOR).toInt()
        val speed = (location.speed * 1000).toInt() // mm/s
        val dist = 0 // simplified

        val b = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN)
        b.putInt(fitTimestamp)
        b.putInt(lat)
        b.putInt(lon)
        b.putInt(dist)
        b.putShort(speed.toShort())
        
        b.array().forEach { buffer.add(it) }
    }

    private fun calculateCrc(data: ByteArray): Int {
        val crcTable = intArrayOf(
            0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401,
            0xA001, 0x6C00, 0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400
        )
        var crc = 0
        for (b in data) {
            var byte = b.toInt() and 0xFF
            var tmp = crcTable[crc and 0xF]
            crc = (crc shr 4) and 0x0FFF
            crc = crc xor tmp xor crcTable[byte and 0xF]
            
            tmp = crcTable[crc and 0xF]
            crc = (crc shr 4) and 0x0FFF
            crc = crc xor tmp xor crcTable[(byte shr 4) and 0xF]
        }
        return crc
    }
}
