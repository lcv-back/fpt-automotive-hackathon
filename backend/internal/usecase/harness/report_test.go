package harness

import (
	"testing"

	"viva-tools/internal/domain"
)

func TestBuildReport(t *testing.T) {
	traces := map[string]*domain.Trace{
		"t1": {
			TraceID: "t1",
			Marks: map[string]int64{
				domain.StageSpeechStart: 0,
				domain.StageSpeechEnd:   200_000_000, // +200ms
				domain.StageAsrSent:     205_000_000,
				domain.StageAsrDone:     600_000_000, // asr_processing = 395ms
				domain.StageExecDone:    650_000_000,
				domain.StageRenderDone:  700_000_000,
			},
			Summary: &domain.TraceSummary{TraceID: "t1", E2EMs: 700},
		},
		"t2": {
			TraceID: "t2",
			Marks: map[string]int64{
				domain.StageSpeechStart: 0,
				domain.StageSpeechEnd:   150_000_000,
				domain.StageAsrSent:     155_000_000,
				domain.StageAsrDone:     500_000_000, // asr_processing = 345ms
				domain.StageExecDone:    540_000_000,
				domain.StageRenderDone:  600_000_000,
			},
			Summary: &domain.TraceSummary{TraceID: "t2", E2EMs: 600},
		},
		"t3_incomplete": {
			TraceID: "t3_incomplete",
			Marks: map[string]int64{
				domain.StageSpeechStart: 0,
				// nothing else — this turn never finished; must not crash
				// stats for segments it has no data for.
			},
		},
	}

	stats := BuildReport(traces)

	byLabel := make(map[string]Stat, len(stats))
	for _, s := range stats {
		byLabel[s.Label] = s
	}

	asrStat, ok := byLabel["asr_processing"]
	if !ok {
		t.Fatal("missing asr_processing stat")
	}
	if asrStat.SampleSize != 2 {
		t.Fatalf("asr_processing SampleSize = %d, want 2", asrStat.SampleSize)
	}
	if asrStat.MinMs != 345 || asrStat.MaxMs != 395 {
		t.Fatalf("asr_processing min/max = %v/%v, want 345/395", asrStat.MinMs, asrStat.MaxMs)
	}

	e2eStat, ok := byLabel["e2e_ms_reported_by_app"]
	if !ok {
		t.Fatal("missing e2e_ms_reported_by_app stat")
	}
	if e2eStat.SampleSize != 2 {
		t.Fatalf("e2e_ms_reported_by_app SampleSize = %d, want 2 (t3_incomplete has no Summary)", e2eStat.SampleSize)
	}

	// vad_capture never fires for t3_incomplete (only speech_start present),
	// but must still be zero-crashing and correctly counted for t1/t2.
	vadStat, ok := byLabel["vad_capture"]
	if !ok {
		t.Fatal("missing vad_capture stat")
	}
	if vadStat.SampleSize != 2 {
		t.Fatalf("vad_capture SampleSize = %d, want 2", vadStat.SampleSize)
	}
}

func TestBuildReport_NoData(t *testing.T) {
	stats := BuildReport(map[string]*domain.Trace{})
	for _, s := range stats {
		if s.SampleSize != 0 {
			t.Fatalf("expected SampleSize=0 for label %s with no traces, got %d", s.Label, s.SampleSize)
		}
	}
}

func TestPercentile(t *testing.T) {
	sorted := []float64{100, 200, 300, 400, 500}
	if p50 := percentile(sorted, 0.50); p50 != 300 {
		t.Errorf("p50 = %v, want 300", p50)
	}
	if p95 := percentile(sorted, 0.95); p95 != 500 {
		t.Errorf("p95 = %v, want 500", p95)
	}
	if single := percentile([]float64{42}, 0.95); single != 42 {
		t.Errorf("single-sample percentile = %v, want 42", single)
	}
}
