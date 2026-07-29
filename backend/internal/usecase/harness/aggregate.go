// Package harness implements the benchmark harness use cases (V8/V10/V11/V12
// in vong2/06-PHAN-CONG-4-NGUOI.md): turn a captured VIVA_TRACE log into
// latency statistics.
package harness

import (
	"fmt"

	"viva-tools/internal/domain"
	"viva-tools/internal/interfaces/repository"
)

// ParseResult holds every trace assembled from a log capture, plus any
// malformed lines encountered. Malformed lines are surfaced as warnings,
// never silently dropped — a hackathon benchmark report that quietly
// discarded bad data would be worse than one that complains loudly.
type ParseResult struct {
	Traces   map[string]*domain.Trace
	Warnings []string
}

// Aggregate reads every line from src, extracts VIVA_TRACE / VIVA_TRACE_SUMMARY
// lines (per vong2/03-contracts.md §1), and groups them by traceId.
func Aggregate(src repository.LineSource) (*ParseResult, error) {
	lines, err := src.Lines()
	if err != nil {
		return nil, fmt.Errorf("read log lines: %w", err)
	}

	result := &ParseResult{Traces: make(map[string]*domain.Trace)}
	for i, line := range lines {
		if ev, found, perr := domain.ParseEventLine(line); found {
			if perr != nil {
				result.Warnings = append(result.Warnings, fmt.Sprintf("line %d: %v", i+1, perr))
				continue
			}
			t := result.trace(ev.TraceID)
			t.Marks[ev.Stage] = ev.ElapsedRealtimeNanos
			continue
		}
		if sum, found, perr := domain.ParseSummaryLine(line); found {
			if perr != nil {
				result.Warnings = append(result.Warnings, fmt.Sprintf("line %d: %v", i+1, perr))
				continue
			}
			t := result.trace(sum.TraceID)
			s := sum
			t.Summary = &s
			continue
		}
	}
	return result, nil
}

func (r *ParseResult) trace(id string) *domain.Trace {
	t, exists := r.Traces[id]
	if !exists {
		t = domain.NewTrace(id)
		r.Traces[id] = t
	}
	return t
}
