package harness

import (
	"math"
	"sort"

	"viva-tools/internal/domain"
)

// segment is one adjacent stage-to-stage latency derivable from the
// pipeline order in vong2/03-contracts.md §0. From/To are the contract's
// exact stage name strings; Label is our own naming for the report.
type segment struct {
	Label    string
	From, To string
}

// standardSegments covers every adjacent pair in domain.CanonicalStageOrder,
// plus two team-meaningful aggregates:
//   - action_latency: speech_start -> exec_done (command understood and acted on)
//   - edge_pipeline_total: speech_start -> render_done (matches the "Action +
//     HMI update" budget line in CLAUDE.md's latency table, target ~0.8-1.0s,
//     hard cap 1.5s)
var standardSegments = []segment{
	{"vad_capture", domain.StageSpeechStart, domain.StageSpeechEnd},
	{"asr_dispatch", domain.StageSpeechEnd, domain.StageAsrSent},
	{"asr_processing", domain.StageAsrSent, domain.StageAsrDone},
	{"intent_routing", domain.StageAsrDone, domain.StageNluDone},
	{"safety_guard", domain.StageNluDone, domain.StageGuardDone},
	{"skill_exec", domain.StageGuardDone, domain.StageExecDone},
	{"hmi_render", domain.StageExecDone, domain.StageRenderDone},
	{"tts_kickoff", domain.StageRenderDone, domain.StageTtsStart},
	{"action_latency", domain.StageSpeechStart, domain.StageExecDone},
	{"edge_pipeline_total", domain.StageSpeechStart, domain.StageRenderDone},
}

// Stat is a latency distribution over one metric (a stage segment, or the
// app-reported end-to-end figure) across every trace that had data for it.
type Stat struct {
	Label      string
	SampleSize int
	P50Ms      float64
	P95Ms      float64
	MinMs      float64
	MaxMs      float64
}

// BuildReport reduces a set of traces into one Stat per standardSegments
// entry, plus one Stat for the app-reported e2e_ms (taken directly from
// VIVA_TRACE_SUMMARY lines, not recomputed) so the two can be cross-checked
// against each other.
func BuildReport(traces map[string]*domain.Trace) []Stat {
	report := make([]Stat, 0, len(standardSegments)+1)
	for _, seg := range standardSegments {
		var samples []float64
		for _, t := range traces {
			if ms, ok := t.MS(seg.From, seg.To); ok {
				samples = append(samples, ms)
			}
		}
		report = append(report, statFrom(seg.Label, samples))
	}

	var reportedE2E []float64
	for _, t := range traces {
		if t.Summary != nil {
			reportedE2E = append(reportedE2E, t.Summary.E2EMs)
		}
	}
	report = append(report, statFrom("e2e_ms_reported_by_app", reportedE2E))
	return report
}

func statFrom(label string, samples []float64) Stat {
	if len(samples) == 0 {
		return Stat{Label: label}
	}
	sort.Float64s(samples)
	return Stat{
		Label:      label,
		SampleSize: len(samples),
		P50Ms:      percentile(samples, 0.50),
		P95Ms:      percentile(samples, 0.95),
		MinMs:      samples[0],
		MaxMs:      samples[len(samples)-1],
	}
}

// percentile uses nearest-rank on an already-sorted slice — adequate for
// benchmark reporting on the ~20+ utterance samples the plan calls for, not
// a statistically rigorous interpolation method.
func percentile(sorted []float64, p float64) float64 {
	if len(sorted) == 1 {
		return sorted[0]
	}
	rank := int(math.Ceil(p*float64(len(sorted)))) - 1
	if rank < 0 {
		rank = 0
	}
	if rank >= len(sorted) {
		rank = len(sorted) - 1
	}
	return sorted[rank]
}
