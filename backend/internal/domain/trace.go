// Package domain holds entities and business rules with zero dependency on
// any framework, transport, or storage detail. Nothing here imports
// net/http, os, or any infrastructure package.
package domain

// Stage names are fixed by vong2/03-contracts.md §1 ("Tên chặng chuẩn —
// không đặt tên khác"). Do not rename without updating the contract and
// telling the team, since Long's Kotlin code emits these exact strings.
const (
	StageSpeechStart = "speech_start" // VadSegmenter: bắt đầu có tiếng nói
	StageSpeechEnd   = "speech_end"   // VadSegmenter: endpoint (nói xong)
	StageAsrSent     = "asr_sent"     // AsrClient: đã gửi audio đi
	StageAsrDone     = "asr_done"     // AsrClient: đã nhận text về
	StageNluDone     = "nlu_done"     // IntentRouter: đã ra intent
	StageGuardDone   = "guard_done"   // SafetyGuard: đã có phán quyết
	StageExecDone    = "exec_done"    // Skill: hành động đã thực thi xong
	StageRenderDone  = "render_done"  // HMI: frame đầu tiên phản ánh trạng thái mới
	StageTtsStart    = "tts_start"    // TtsSpeaker: bắt đầu phát tiếng
)

// CanonicalStageOrder is the sequence stages occur in during one turn, per
// the pipeline diagram in 03-contracts.md §0. Used to derive adjacent
// latency segments (e.g. asr_sent -> asr_done = ASR processing time).
var CanonicalStageOrder = []string{
	StageSpeechStart,
	StageSpeechEnd,
	StageAsrSent,
	StageAsrDone,
	StageNluDone,
	StageGuardDone,
	StageExecDone,
	StageRenderDone,
	StageTtsStart,
}

// TraceEvent is one parsed "VIVA_TRACE|<traceId>|<stage>|<elapsedRealtimeNanos>" line.
type TraceEvent struct {
	TraceID              string
	Stage                string
	ElapsedRealtimeNanos int64
}

// TraceSummary is one parsed
// "VIVA_TRACE_SUMMARY|<traceId>|<utterance>|<intent>|<verdict>|e2e_ms=<số>" line.
//
// NOTE: 03-contracts.md does not enumerate the exact string values Verdict
// takes in this log line (the Kotlin sealed class Verdict has Allow/Deny/Confirm,
// each carrying extra fields like "rule"). Treat Verdict as an opaque string
// here; confirm the exact serialization with Long before relying on it for
// pass/fail classification.
type TraceSummary struct {
	TraceID   string
	Utterance string
	Intent    string
	Verdict   string
	E2EMs     float64
}

// Trace aggregates every mark seen for one traceId, plus its summary line
// if one arrived (a trace with marks but no summary means the turn never
// completed, or the summary line hasn't been read yet).
type Trace struct {
	TraceID string
	Marks   map[string]int64 // stage name -> elapsedRealtimeNanos
	Summary *TraceSummary
}

// NewTrace returns an empty Trace ready to accumulate marks.
func NewTrace(traceID string) *Trace {
	return &Trace{TraceID: traceID, Marks: make(map[string]int64)}
}

// MS returns the elapsed milliseconds between two marks, mirroring the
// Kotlin LatencyTrace.ms(from, to) helper in 03-contracts.md §1. Returns
// false if either mark is missing (e.g. that stage never fired for this turn).
func (t *Trace) MS(from, to string) (float64, bool) {
	f, ok1 := t.Marks[from]
	toV, ok2 := t.Marks[to]
	if !ok1 || !ok2 {
		return 0, false
	}
	return float64(toV-f) / 1_000_000.0, true
}
