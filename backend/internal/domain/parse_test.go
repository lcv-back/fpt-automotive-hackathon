package domain

import "testing"

func TestParseEventLine(t *testing.T) {
	cases := []struct {
		name       string
		line       string
		wantFound  bool
		wantErr    bool
		wantTrace  string
		wantStage  string
		wantNanos  int64
	}{
		{
			name:      "bare line",
			line:      "VIVA_TRACE|abc-123|speech_start|123456789",
			wantFound: true,
			wantTrace: "abc-123",
			wantStage: "speech_start",
			wantNanos: 123456789,
		},
		{
			name:      "with logcat prefix",
			line:      "07-29 10:00:00.123  1234  5678 I VIVA_TRACE: VIVA_TRACE|abc-123|asr_done|987654321",
			wantFound: true,
			wantTrace: "abc-123",
			wantStage: "asr_done",
			wantNanos: 987654321,
		},
		{
			name:      "ordinary line, no marker",
			line:      "07-29 10:00:00.123  1234  5678 I SomeOtherTag: nothing to see here",
			wantFound: false,
		},
		{
			name:      "summary line must not be picked up as event",
			line:      "VIVA_TRACE_SUMMARY|abc-123|hạ điều hòa|hvac_set_temp|Allow|e2e_ms=812",
			wantFound: false,
		},
		{
			name:      "wrong field count",
			line:      "VIVA_TRACE|abc-123|speech_start",
			wantFound: true,
			wantErr:   true,
		},
		{
			name:      "non-numeric nanos",
			line:      "VIVA_TRACE|abc-123|speech_start|not-a-number",
			wantFound: true,
			wantErr:   true,
		},
	}

	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			ev, found, err := ParseEventLine(tc.line)
			if found != tc.wantFound {
				t.Fatalf("found = %v, want %v", found, tc.wantFound)
			}
			if !found {
				return
			}
			if (err != nil) != tc.wantErr {
				t.Fatalf("err = %v, wantErr %v", err, tc.wantErr)
			}
			if tc.wantErr {
				return
			}
			if ev.TraceID != tc.wantTrace || ev.Stage != tc.wantStage || ev.ElapsedRealtimeNanos != tc.wantNanos {
				t.Fatalf("got %+v, want traceID=%s stage=%s nanos=%d", ev, tc.wantTrace, tc.wantStage, tc.wantNanos)
			}
		})
	}
}

func TestParseSummaryLine(t *testing.T) {
	line := "VIVA_TRACE_SUMMARY|abc-123|hạ điều hòa xuống 22 độ|hvac_set_temp|Allow|e2e_ms=812.5"
	sum, found, err := ParseSummaryLine(line)
	if !found || err != nil {
		t.Fatalf("found=%v err=%v, want found=true err=nil", found, err)
	}
	if sum.TraceID != "abc-123" {
		t.Errorf("TraceID = %q, want abc-123", sum.TraceID)
	}
	if sum.Utterance != "hạ điều hòa xuống 22 độ" {
		t.Errorf("Utterance = %q", sum.Utterance)
	}
	if sum.Intent != "hvac_set_temp" {
		t.Errorf("Intent = %q", sum.Intent)
	}
	if sum.Verdict != "Allow" {
		t.Errorf("Verdict = %q", sum.Verdict)
	}
	if sum.E2EMs != 812.5 {
		t.Errorf("E2EMs = %v, want 812.5", sum.E2EMs)
	}
}

func TestParseSummaryLine_MissingE2EPrefix(t *testing.T) {
	_, found, err := ParseSummaryLine("VIVA_TRACE_SUMMARY|abc-123|utterance|intent|Allow|812.5")
	if !found {
		t.Fatal("expected marker to be found")
	}
	if err == nil {
		t.Fatal("expected error for missing e2e_ms= prefix")
	}
}

func TestParseSummaryLine_NotFound(t *testing.T) {
	_, found, err := ParseSummaryLine("VIVA_TRACE|abc-123|speech_start|123")
	if found {
		t.Fatal("event line should not be picked up as summary")
	}
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestTrace_MS(t *testing.T) {
	tr := NewTrace("abc")
	tr.Marks[StageSpeechStart] = 1_000_000_000
	tr.Marks[StageSpeechEnd] = 1_500_000_000

	ms, ok := tr.MS(StageSpeechStart, StageSpeechEnd)
	if !ok {
		t.Fatal("expected ok=true, both marks present")
	}
	if ms != 500 {
		t.Fatalf("ms = %v, want 500", ms)
	}

	if _, ok := tr.MS(StageSpeechStart, StageAsrDone); ok {
		t.Fatal("expected ok=false, asr_done mark missing")
	}
}
