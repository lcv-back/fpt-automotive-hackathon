// Package report writes harness.Stat results out to a file format — CSV
// today, matching what V8/V10-V12 in 06-PHAN-CONG-4-NGUOI.md ask for.
package report

import (
	"encoding/csv"
	"fmt"
	"os"
	"strconv"

	"viva-tools/internal/usecase/harness"
)

// WriteCSV writes stats to path with a fixed column order:
// label, sample_size, p50_ms, p95_ms, min_ms, max_ms.
//
// A metric with zero samples prints "-" in the numeric columns rather than
// "0.00", so it reads as "no data" instead of "measured zero latency".
func WriteCSV(stats []harness.Stat, path string) error {
	f, err := os.Create(path)
	if err != nil {
		return fmt.Errorf("create %s: %w", path, err)
	}
	defer f.Close()

	w := csv.NewWriter(f)
	defer w.Flush()

	header := []string{"label", "sample_size", "p50_ms", "p95_ms", "min_ms", "max_ms"}
	if err := w.Write(header); err != nil {
		return fmt.Errorf("write header: %w", err)
	}
	for _, s := range stats {
		row := []string{
			s.Label,
			strconv.Itoa(s.SampleSize),
			fmtOrDash(s.SampleSize, s.P50Ms),
			fmtOrDash(s.SampleSize, s.P95Ms),
			fmtOrDash(s.SampleSize, s.MinMs),
			fmtOrDash(s.SampleSize, s.MaxMs),
		}
		if err := w.Write(row); err != nil {
			return fmt.Errorf("write row for %s: %w", s.Label, err)
		}
	}
	w.Flush()
	if err := w.Error(); err != nil {
		return fmt.Errorf("flush csv: %w", err)
	}
	return nil
}

func fmtOrDash(n int, v float64) string {
	if n == 0 {
		return "-"
	}
	return strconv.FormatFloat(v, 'f', 2, 64)
}
