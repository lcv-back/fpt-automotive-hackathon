package com.viva.voice.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/**
 * [PcmSource] backed by `AudioRecord`. The only microphone code in the module.
 *
 * Requires `android.permission.RECORD_AUDIO`, which the host app must have
 * been granted before [start] - the library does not prompt, because on AAOS
 * the permission is normally pre-granted for a system-signed cockpit app and
 * a runtime dialog while driving is exactly the distraction the guideline
 * forbids.
 */
class AndroidPcmSource(
    private val config: AudioConfig = AudioConfig.DEFAULT,
    /**
     * VOICE_RECOGNITION, not MIC: it is the source that leaves the platform's
     * AEC/NS chain in place and skips the "media recording" tuning. In a cabin
     * with music playing, that difference is what decides whether ASR hears
     * the driver or the speakers.
     */
    private val audioSource: Int = MediaRecorder.AudioSource.VOICE_RECOGNITION,
) : PcmSource {

    private var record: AudioRecord? = null

    @SuppressLint("MissingPermission") // caller holds RECORD_AUDIO; see class doc
    override fun start() {
        if (record != null) return

        val minBuffer = AudioRecord.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        check(minBuffer > 0) {
            "AudioRecord.getMinBufferSize returned $minBuffer - device rejects " +
                "${config.sampleRate}Hz mono PCM16"
        }

        // Four chunks of headroom: a GC pause or a slow ASR hand-off must not
        // overrun the ring buffer, because an overrun is silently dropped
        // audio and shows up as a truncated transcript, not as an error.
        val bufferBytes = maxOf(minBuffer, config.chunkSamples * config.bytesPerSample * 4)

        val created = AudioRecord(
            audioSource,
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferBytes,
        )
        check(created.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialise (state=${created.state}) - " +
                "another app may hold the microphone"
        }
        created.startRecording()
        record = created
    }

    override fun read(into: ShortArray): Int {
        val active = record ?: return -1
        val n = active.read(into, 0, into.size)
        // Negative values are AudioRecord's error codes (ERROR_INVALID_OPERATION,
        // ERROR_DEAD_OBJECT...). Reported as end-of-stream so the recorder
        // stops cleanly and the turn is logged as Error:speech_end rather than
        // looping on a dead handle.
        return if (n < 0) -1 else n
    }

    override fun stop() {
        val active = record ?: return
        record = null
        runCatching { active.stop() }
        active.release()
    }
}
