package logsource

import (
	"bufio"
	"fmt"
	"io"
)

// maxLogBytes bounds how much log text a single Lines() call will hold in
// memory. This is a safety ceiling, not a real expected size: the plan
// calls for tens of utterances (V8/V10) up to a few hundred in an automated
// regression run (V11), which is KB-to-low-MB of text. Hitting this limit
// means --input was pointed at the wrong file (or an adb filter is missing)
// far more likely than a legitimate need to process 200MiB of trace log in
// one shot — fail with a clear message instead of risking the process
// getting OOM-killed partway through.
const maxLogBytes = 200 * 1024 * 1024 // 200 MiB

// scanLines reads every line from r, enforcing maxLogBytes.
func scanLines(r io.Reader) ([]string, error) {
	return scanLinesLimited(r, maxLogBytes)
}

// scanLinesLimited is scanLines with an injectable limit, so tests can
// exercise the "input too large" path without actually writing 200MiB.
func scanLinesLimited(r io.Reader, limitBytes int64) ([]string, error) {
	var lines []string
	var totalBytes int64

	scanner := bufio.NewScanner(r)
	// VIVA_TRACE_SUMMARY lines carry a full spoken utterance; default 64KiB
	// scanner buffer is comfortably enough, grown here for safety margin.
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)

	for scanner.Scan() {
		totalBytes += int64(len(scanner.Bytes())) + 1
		if totalBytes > limitBytes {
			return nil, fmt.Errorf("input exceeds %d MiB safety limit — split the log file or narrow the adb logcat filter", maxLogBytes/(1024*1024))
		}
		lines = append(lines, scanner.Text())
	}
	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("scan lines: %w", err)
	}
	return lines, nil
}
