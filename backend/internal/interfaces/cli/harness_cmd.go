package cli

import (
	"flag"
	"fmt"
	"os"

	"viva-tools/internal/infrastructure/logsource"
	"viva-tools/internal/infrastructure/report"
	"viva-tools/internal/interfaces/repository"
	"viva-tools/internal/usecase/harness"
)

func runHarness(args []string) int {
	if len(args) < 1 || args[0] != "report" {
		fmt.Fprintln(os.Stderr, `usage: viva-tools harness report --input <path|-> [--out report.csv]
   or: viva-tools harness report --adb [--serial <id>] [--out report.csv]`)
		return 2
	}

	fs := flag.NewFlagSet("harness report", flag.ContinueOnError)
	input := fs.String("input", "", "path to a captured log file, or - for stdin")
	useAdb := fs.Bool("adb", false, "pull the log directly from a connected device via `adb logcat -d -s VIVA_TRACE:I`")
	serial := fs.String("serial", "", "adb device serial (only used with --adb, when more than one device is attached)")
	out := fs.String("out", "report.csv", "output CSV path")
	if err := fs.Parse(args[1:]); err != nil {
		return 2
	}

	var src repository.LineSource
	switch {
	case *useAdb:
		src = logsource.AdbSource{Serial: *serial}
	case *input != "":
		src = logsource.FileSource{Path: *input}
	default:
		fmt.Fprintln(os.Stderr, "error: need --input <path|-> or --adb")
		return 2
	}

	result, err := harness.Aggregate(src)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}
	for _, w := range result.Warnings {
		fmt.Fprintf(os.Stderr, "warning: %s\n", w)
	}
	if len(result.Traces) == 0 {
		fmt.Fprintln(os.Stderr, "warning: no VIVA_TRACE lines found — check --input/--adb and the log format")
	}

	stats := harness.BuildReport(result.Traces)
	if err := report.WriteCSV(stats, *out); err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}

	fmt.Printf("%d traces parsed, report written to %s\n", len(result.Traces), *out)
	for _, s := range stats {
		if s.SampleSize == 0 {
			fmt.Printf("  %-24s no data\n", s.Label)
			continue
		}
		fmt.Printf("  %-24s n=%-4d p50=%.1fms p95=%.1fms\n", s.Label, s.SampleSize, s.P50Ms, s.P95Ms)
	}
	return 0
}
