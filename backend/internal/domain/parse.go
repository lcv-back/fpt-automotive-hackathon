package domain

import (
	"fmt"
	"strconv"
	"strings"
)

// Log line markers, verbatim from vong2/03-contracts.md §1:
//
//	VIVA_TRACE|<traceId>|<stage>|<elapsedRealtimeNanos>
//	VIVA_TRACE_SUMMARY|<traceId>|<utterance>|<intent>|<verdict>|e2e_ms=<số>
const (
	eventMarker   = "VIVA_TRACE|"
	summaryMarker = "VIVA_TRACE_SUMMARY|"
)

// ParseEventLine parses one "VIVA_TRACE|..." line. The input may be a raw
// logcat line with an arbitrary prefix (timestamp, pid, tag, level) — we
// search for the marker rather than requiring it at column 0, since the
// exact `adb logcat -v <format>` invocation the team ends up using isn't
// pinned down anywhere in the contracts.
//
// ok is false when the line doesn't contain the event marker at all (a
// normal, expected case for most log lines — not an error). err is non-nil
// only when the marker IS present but the payload after it is malformed,
// which should surface to the operator.
func ParseEventLine(line string) (ev TraceEvent, ok bool, err error) {
	idx := strings.Index(line, eventMarker)
	if idx < 0 {
		return TraceEvent{}, false, nil
	}
	payload := line[idx+len(eventMarker):]
	parts := strings.SplitN(strings.TrimRight(payload, "\r\n"), "|", 3)
	if len(parts) != 3 {
		return TraceEvent{}, true, fmt.Errorf("malformed VIVA_TRACE line, want 3 fields after marker, got %d: %q", len(parts), payload)
	}
	nanos, convErr := strconv.ParseInt(parts[2], 10, 64)
	if convErr != nil {
		return TraceEvent{}, true, fmt.Errorf("malformed VIVA_TRACE elapsedRealtimeNanos %q: %w", parts[2], convErr)
	}
	return TraceEvent{
		TraceID:              parts[0],
		Stage:                parts[1],
		ElapsedRealtimeNanos: nanos,
	}, true, nil
}

// ParseSummaryLine parses one "VIVA_TRACE_SUMMARY|..." line. Same
// found-vs-malformed distinction as ParseEventLine.
func ParseSummaryLine(line string) (sum TraceSummary, ok bool, err error) {
	idx := strings.Index(line, summaryMarker)
	if idx < 0 {
		return TraceSummary{}, false, nil
	}
	payload := line[idx+len(summaryMarker):]
	parts := strings.SplitN(strings.TrimRight(payload, "\r\n"), "|", 5)
	if len(parts) != 5 {
		return TraceSummary{}, true, fmt.Errorf("malformed VIVA_TRACE_SUMMARY line, want 5 fields after marker, got %d: %q", len(parts), payload)
	}
	const e2ePrefix = "e2e_ms="
	if !strings.HasPrefix(parts[4], e2ePrefix) {
		return TraceSummary{}, true, fmt.Errorf("malformed VIVA_TRACE_SUMMARY last field, want prefix %q, got %q", e2ePrefix, parts[4])
	}
	e2e, convErr := strconv.ParseFloat(strings.TrimPrefix(parts[4], e2ePrefix), 64)
	if convErr != nil {
		return TraceSummary{}, true, fmt.Errorf("malformed VIVA_TRACE_SUMMARY e2e_ms value %q: %w", parts[4], convErr)
	}
	return TraceSummary{
		TraceID:   parts[0],
		Utterance: parts[1],
		Intent:    parts[2],
		Verdict:   parts[3],
		E2EMs:     e2e,
	}, true, nil
}
