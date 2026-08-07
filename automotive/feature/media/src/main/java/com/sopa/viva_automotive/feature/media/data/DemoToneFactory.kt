package com.sopa.viva_automotive.feature.media.data

import com.sopa.viva_automotive.feature.media.domain.MediaTrack
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object DemoToneFactory {

    fun ensureTracks(cacheDir: File): List<MediaTrack> {
        val dir = File(cacheDir, "viva_demo_tracks").apply { mkdirs() }
        return SPECS.map { spec ->
            val file = File(dir, "${spec.id}.wav")
            if (!file.exists() || file.length() < 44L) {
                writeSineWav(file, frequencyHz = spec.frequencyHz, durationMs = TRACK_DURATION_MS)
            }
            MediaTrack(
                id = spec.id,
                title = spec.title,
                artist = spec.artist,
                file = file,
            )
        }
    }

    private fun writeSineWav(file: File, frequencyHz: Double, durationMs: Int) {
        val sampleCount = SAMPLE_RATE * durationMs / 1000
        val dataSize = sampleCount * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(1) // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        val twoPiF = 2.0 * PI * frequencyHz / SAMPLE_RATE
        for (i in 0 until sampleCount) {
            val envelope = when {
                i < FADE_SAMPLES -> i.toDouble() / FADE_SAMPLES
                i > sampleCount - FADE_SAMPLES -> (sampleCount - i).toDouble() / FADE_SAMPLES
                else -> 1.0
            }
            val sample = (sin(twoPiF * i) * AMPLITUDE * envelope).toInt().toShort()
            buffer.putShort(sample)
        }

        file.outputStream().use { it.write(buffer.array()) }
    }

    private data class TrackSpec(
        val id: String,
        val title: String,
        val artist: String,
        val frequencyHz: Double,
    )

    private val SPECS = listOf(
        TrackSpec("cruise", "Coastal Cruise", "VIVA Demo", 392.0),
        TrackSpec("highway", "Highway Lights", "VIVA Demo", 523.25),
        TrackSpec("midnight", "Midnight Cabin", "VIVA Demo", 329.63),
    )

    private const val SAMPLE_RATE = 16_000
    private const val TRACK_DURATION_MS = 4_000
    private const val FADE_SAMPLES = 800
    private const val AMPLITUDE = 12_000.0
}
