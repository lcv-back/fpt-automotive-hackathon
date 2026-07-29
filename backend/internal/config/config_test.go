package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadDotEnv_MissingFileIsNotAnError(t *testing.T) {
	if err := LoadDotEnv(filepath.Join(t.TempDir(), "does-not-exist.env")); err != nil {
		t.Fatalf("missing .env should not error, got: %v", err)
	}
}

func TestLoadDotEnv_SetsUnsetVars(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, ".env")
	os.WriteFile(path, []byte("# comment\nCARSKY_BASE_URL=https://example.test/api/v1\n\nCARSKY_ROOM_ID=room-1\n"), 0o644)

	t.Setenv("CARSKY_BASE_URL", "")
	os.Unsetenv("CARSKY_BASE_URL")
	os.Unsetenv("CARSKY_ROOM_ID")

	if err := LoadDotEnv(path); err != nil {
		t.Fatalf("LoadDotEnv: %v", err)
	}
	if got := os.Getenv("CARSKY_BASE_URL"); got != "https://example.test/api/v1" {
		t.Errorf("CARSKY_BASE_URL = %q", got)
	}
	if got := os.Getenv("CARSKY_ROOM_ID"); got != "room-1" {
		t.Errorf("CARSKY_ROOM_ID = %q", got)
	}
}

func TestLoadDotEnv_RealEnvWinsOverFile(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, ".env")
	os.WriteFile(path, []byte("CARSKY_API_TOKEN=from-file\n"), 0o644)

	t.Setenv("CARSKY_API_TOKEN", "from-real-env")

	if err := LoadDotEnv(path); err != nil {
		t.Fatalf("LoadDotEnv: %v", err)
	}
	if got := os.Getenv("CARSKY_API_TOKEN"); got != "from-real-env" {
		t.Errorf("CARSKY_API_TOKEN = %q, want real env var to win", got)
	}
}

func TestLoadDotEnv_MalformedLine(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, ".env")
	os.WriteFile(path, []byte("NOT_A_VALID_LINE_NO_EQUALS\n"), 0o644)

	if err := LoadDotEnv(path); err == nil {
		t.Fatal("expected an error for a line with no '='")
	}
}

func TestLoadCarSky_MissingRequiredVars(t *testing.T) {
	os.Unsetenv("CARSKY_BASE_URL")
	os.Unsetenv("CARSKY_API_TOKEN")

	if _, err := LoadCarSky(); err == nil {
		t.Fatal("expected error when CARSKY_BASE_URL/CARSKY_API_TOKEN are unset")
	}
}
