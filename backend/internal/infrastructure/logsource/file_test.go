package logsource

import (
	"os"
	"path/filepath"
	"testing"
)

func TestFileSource_ReadsLines(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "log.txt")
	if err := os.WriteFile(path, []byte("line one\nline two\nline three\n"), 0o644); err != nil {
		t.Fatal(err)
	}

	lines, err := FileSource{Path: path}.Lines()
	if err != nil {
		t.Fatalf("Lines: %v", err)
	}
	want := []string{"line one", "line two", "line three"}
	if len(lines) != len(want) {
		t.Fatalf("got %d lines, want %d: %v", len(lines), len(want), lines)
	}
	for i := range want {
		if lines[i] != want[i] {
			t.Errorf("line %d = %q, want %q", i, lines[i], want[i])
		}
	}
}

func TestFileSource_MissingFile(t *testing.T) {
	_, err := FileSource{Path: filepath.Join(t.TempDir(), "does-not-exist.txt")}.Lines()
	if err == nil {
		t.Fatal("expected an error for a missing file")
	}
}

func TestFileSource_Stdin(t *testing.T) {
	r, w, err := os.Pipe()
	if err != nil {
		t.Fatal(err)
	}
	origStdin := os.Stdin
	os.Stdin = r
	defer func() { os.Stdin = origStdin }()

	go func() {
		w.WriteString("from stdin\n")
		w.Close()
	}()

	lines, err := FileSource{Path: "-"}.Lines()
	if err != nil {
		t.Fatalf("Lines: %v", err)
	}
	if len(lines) != 1 || lines[0] != "from stdin" {
		t.Fatalf("got %v, want [\"from stdin\"]", lines)
	}
}
