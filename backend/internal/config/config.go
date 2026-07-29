// Package config loads configuration from environment variables (and
// optionally a local .env file) only. Per the project's CLAUDE.md
// convention: no hard-coded host, room id, or API token/key in code.
package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

// defaultCarSkyTimeout applies when CARSKY_TIMEOUT_SECONDS is unset or
// invalid. The CarSky VM/room can be slow under load (VM restart, cold
// container); 30s is generous enough to avoid false timeouts without
// hanging a CI run forever on a truly dead endpoint.
const defaultCarSkyTimeout = 30 * time.Second

// CarSky holds connection details for the CarSky platform API
// (docs/Car-Sky-Platform.html, base path /api/v1).
type CarSky struct {
	// BaseURL, e.g. https://hackathon-2.carsky.io/api/v1 — CONFIRM the exact
	// API host with the team. docs/link.md only records the web UI URL
	// (https://hackathon-2.carsky.io/), which is not necessarily the API
	// host; do not assume they're the same without checking GET /api/v1/openapi
	// or asking a teammate who already has a working credential.
	BaseURL string
	// Token is sent as "Authorization: Bearer <token>" (confirmed in
	// docs/Car-Sky-Platform.html, "Xác thực Qua REST API").
	Token string
	// RoomID is the default room/deployment id for devops commands that
	// don't take --room explicitly.
	RoomID string
	// Timeout applies to every individual HTTP request. Configurable via
	// CARSKY_TIMEOUT_SECONDS because "slow VM under load" and "dead
	// endpoint" need different tolerances depending on who's running this
	// and when (interactive use vs. an automated regression loop like V11).
	Timeout time.Duration
}

// LoadCarSky reads CARSKY_BASE_URL / CARSKY_API_TOKEN / CARSKY_ROOM_ID /
// CARSKY_TIMEOUT_SECONDS from the environment. Call LoadDotEnv first if you
// want .env file support.
func LoadCarSky() (CarSky, error) {
	cfg := CarSky{
		BaseURL: strings.TrimRight(os.Getenv("CARSKY_BASE_URL"), "/"),
		Token:   os.Getenv("CARSKY_API_TOKEN"),
		RoomID:  os.Getenv("CARSKY_ROOM_ID"),
		Timeout: defaultCarSkyTimeout,
	}
	if cfg.BaseURL == "" {
		return CarSky{}, fmt.Errorf("CARSKY_BASE_URL is not set (see .env.example)")
	}
	if cfg.Token == "" {
		return CarSky{}, fmt.Errorf("CARSKY_API_TOKEN is not set (see .env.example)")
	}
	if v := os.Getenv("CARSKY_TIMEOUT_SECONDS"); v != "" {
		secs, err := strconv.Atoi(v)
		if err != nil || secs <= 0 {
			return CarSky{}, fmt.Errorf("CARSKY_TIMEOUT_SECONDS must be a positive integer, got %q", v)
		}
		cfg.Timeout = time.Duration(secs) * time.Second
	}
	return cfg, nil
}

// LoadDotEnv reads simple KEY=VALUE lines from path and applies them via
// os.Setenv for any key not already present in the process environment, so
// real environment variables (CI secrets, shell exports) always win over
// the file. Missing file is not an error — .env is optional.
//
// This is deliberately not a full .env spec implementation: no quoting, no
// multiline values, "#" only comments a whole trimmed line.
func LoadDotEnv(path string) error {
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return fmt.Errorf("read %s: %w", path, err)
	}
	for i, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		key, value, found := strings.Cut(line, "=")
		if !found {
			return fmt.Errorf("%s:%d: expected KEY=VALUE, got %q", path, i+1, line)
		}
		key = strings.TrimSpace(key)
		value = strings.TrimSpace(value)
		if _, alreadySet := os.LookupEnv(key); !alreadySet {
			if err := os.Setenv(key, value); err != nil {
				return fmt.Errorf("set %s: %w", key, err)
			}
		}
	}
	return nil
}
