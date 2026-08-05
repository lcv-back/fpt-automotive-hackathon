package com.viva.voice.trace

import android.os.SystemClock
import android.util.Log
import java.util.UUID

/**
 * The Android bindings for the trace package.
 *
 * This is the ONLY file here that imports `android.*`. Everything else -
 * [LatencyTrace], [TraceVerdict], the wire format, the sanitizer - is plain
 * Kotlin and is unit tested on the JVM without a device or Robolectric.
 * Keep it that way: the moment a framework type leaks into [LatencyTrace],
 * the tests need an emulator and stop being run.
 */

/** [NanoClock] backed by monotonic elapsed-realtime, per 03-contracts.md §1. */
object SystemNanoClock : NanoClock {
    override fun nanos(): Long = SystemClock.elapsedRealtimeNanos()
}

/**
 * Writes trace lines to logcat under the `VIVA_TRACE` tag.
 *
 * Vi's harness reads them with `adb logcat -s VIVA_TRACE` (V8). The tag is
 * also repeated at the start of the message by [LatencyTrace] itself, so the
 * line still parses if somebody dumps the whole buffer without a filter -
 * parse.go searches for the marker rather than anchoring at column 0.
 */
object LogcatTraceSink : TraceSink {
    override fun emit(line: String) {
        Log.i(LatencyTrace.LOG_TAG, line)
    }
}

/**
 * Diagnostics go to `VIVA_VOICE`, never to `VIVA_TRACE`.
 *
 * A warning written to the trace tag would end up as a row in the benchmark
 * CSV, which is how a harness starts reporting latencies for turns that
 * never happened.
 */
object LogcatDiagnostics : TraceDiagnostics {
    override fun warn(message: String) {
        Log.w(VOICE_TAG, message)
    }

    const val VOICE_TAG = "VIVA_VOICE"
}

/**
 * Opens a trace for one voice turn.
 *
 * Called by the VAD the moment speech onset is detected (03-contracts.md §1:
 * "UUID, sinh khi VAD phat hien bat dau noi"). Pass [nanos] when the onset
 * timestamp is already known and older than now - see [LatencyTrace.markAt].
 */
fun startVoiceTrace(nanos: Long? = null): LatencyTrace {
    val trace = startSilentTrace()
    if (nanos != null) trace.markAt(Stage.SPEECH_START, nanos) else trace.mark(Stage.SPEECH_START)
    return trace
}

/**
 * Mở trace cho một lượt **không có tiếng nói** — ví dụ câu bơm vào từ bộ
 * benchmark.
 *
 * Khác [startVoiceTrace] đúng một điểm: không đánh dấu `speech_start`. Điểm đó
 * quyết định tính trung thực của số đo — [LatencyTrace.e2eMs] tính từ
 * `speech_end` hoặc `speech_start`, nên một lượt không hề có ai nói mà vẫn mang
 * `speech_start` sẽ đẻ ra `e2e_ms` trông như độ trễ đầu-cuối thật, trong khi
 * chặng ASR còn chưa từng chạy. Thà để trống còn hơn có một con số đẹp mà sai.
 */
fun startSilentTrace(): LatencyTrace = LatencyTrace(
    traceId = UUID.randomUUID().toString(),
    clock = SystemNanoClock,
    sink = LogcatTraceSink,
    diagnostics = LogcatDiagnostics,
)
