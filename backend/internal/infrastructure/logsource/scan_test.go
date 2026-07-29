package logsource

import (
	"strings"
	"testing"
)

func TestScanLinesLimited_UnderLimit(t *testing.T) {
	r := strings.NewReader("a\nb\nc\n")
	lines, err := scanLinesLimited(r, 1024)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(lines) != 3 {
		t.Fatalf("got %d lines, want 3", len(lines))
	}
}

func TestScanLinesLimited_OverLimit(t *testing.T) {
	// Each line is 4 bytes ("aaa\n"); limit of 10 bytes should trip after
	// the 2nd or 3rd line, well before all 100 are read.
	r := strings.NewReader(strings.Repeat("aaa\n", 100))
	_, err := scanLinesLimited(r, 10)
	if err == nil {
		t.Fatal("expected an error when input exceeds the byte limit")
	}
	if !strings.Contains(err.Error(), "safety limit") {
		t.Fatalf("error %q does not mention the safety limit", err.Error())
	}
}
