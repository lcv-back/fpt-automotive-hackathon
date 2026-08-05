package com.sopa.viva_automotive.feature.media.data

import com.sopa.viva_automotive.feature.media.domain.MediaTrack
import com.sopa.viva_automotive.feature.media.domain.RadioStation
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object RadioStationCatalog {

    val stations: List<RadioStation> = listOf(
        RadioStation("voh_fm", "VOH FM", 99.9f, "TP.HCM"),
        RadioStation("giao_thong", "Giao thông 91.5", 91.5f, "TP.HCM"),
        RadioStation("vov1", "VOV1", 100.0f, "Hà Nội"),
        RadioStation("vov3", "VOV3", 104.5f, "Hà Nội"),
        RadioStation("fresh_fm", "Fresh FM", 98.3f, "Demo"),
        RadioStation("coast_fm", "Coast FM", 102.7f, "Demo"),
    )

    fun ensureTracks(cacheDir: File): List<MediaTrack> {
        val dir = File(cacheDir, "viva_radio_stations").apply { mkdirs() }
        return stations.map { station ->
            val file = File(dir, "${station.id}.wav")
            if (!file.exists() || file.length() < 44L) {
                val toneHz = 220.0 + (station.frequencyMhz - 88f) * 18.0
                writeLoopableTone(file, frequencyHz = toneHz, durationMs = STATION_DURATION_MS)
            }
            station.toTrack(file)
        }
    }

    private fun writeLoopableTone(file: File, frequencyHz: Double, durationMs: Int) {
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val dataSize = sampleCount * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        val twoPiF = 2.0 * PI * frequencyHz / SAMPLE_RATE
        for (i in 0 until sampleCount) {
            val sample = (sin(twoPiF * i) * AMPLITUDE).toInt().toShort()
            buffer.putShort(sample)
        }
        file.outputStream().use { it.write(buffer.array()) }
    }

    private const val SAMPLE_RATE = 16_000
    private const val STATION_DURATION_MS = 8_000
    private const val AMPLITUDE = 9_000.0
}
