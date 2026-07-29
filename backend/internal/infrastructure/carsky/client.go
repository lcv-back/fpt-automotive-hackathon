// Package carsky implements repository.CarSkyGateway over the CarSky REST
// API documented in docs/Car-Sky-Platform.html (base path /api/v1, auth
// "Authorization: Bearer <token>").
//
// Response bodies are returned as raw JSON on purpose: the docs confirm
// method, path, and auth for each endpoint below, not the response schema.
// A live GET /api/v1/openapi exists on the platform — once someone has
// pulled it, replace the json.RawMessage returns with real structs.
package carsky

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"time"

	"viva-tools/internal/config"
)

// maxGetRetries bounds how many times a GET request is retried on a
// network-level error or a 5xx response before giving up. GET is idempotent
// (safe to repeat); POST (blueprint clone) is deliberately never retried
// here — see SafeClone's doc comment in usecase/devops for why a duplicate
// clone is worse than a failed one the operator retries by hand.
const maxGetRetries = 3

type Client struct {
	cfg        config.CarSky
	httpClient *http.Client
}

func New(cfg config.CarSky) *Client {
	return &Client{
		cfg:        cfg,
		httpClient: &http.Client{Timeout: cfg.Timeout},
	}
}

// StatusError is returned when CarSky responds with an HTTP error status.
// Exposed as a type (not just a formatted string) so callers — currently
// just the internal retry logic — can tell a client error (4xx: retrying
// won't help, e.g. bad token or bad id) apart from a server/network error
// (5xx or transport failure: worth retrying).
type StatusError struct {
	Method, URL string
	StatusCode  int
	Body        string
}

func (e *StatusError) Error() string {
	return fmt.Sprintf("%s %s: HTTP %d: %s", e.Method, e.URL, e.StatusCode, e.Body)
}

// ExportBlueprint calls GET /api/v1/blueprints/:id/export.
func (c *Client) ExportBlueprint(id string) (json.RawMessage, error) {
	return c.do(http.MethodGet, fmt.Sprintf("/blueprints/%s/export", id), nil)
}

// CloneBlueprint calls POST /api/v1/blueprints/:id/clone.
func (c *Client) CloneBlueprint(id string) (json.RawMessage, error) {
	return c.do(http.MethodPost, fmt.Sprintf("/blueprints/%s/clone", id), nil)
}

// ListNodes calls GET /api/v1/deployments/:roomId/nodes.
func (c *Client) ListNodes(roomID string) (json.RawMessage, error) {
	return c.do(http.MethodGet, fmt.Sprintf("/deployments/%s/nodes", roomID), nil)
}

// AdbTunnelInfo calls GET /api/v1/deployments/:roomId/adb-tunnel.
func (c *Client) AdbTunnelInfo(roomID string) (json.RawMessage, error) {
	return c.do(http.MethodGet, fmt.Sprintf("/deployments/%s/adb-tunnel", roomID), nil)
}

// do routes GET requests through the retry wrapper (safe: idempotent, and
// every GET call in this client sends a nil body) and executes every other
// method exactly once.
func (c *Client) do(method, path string, body io.Reader) (json.RawMessage, error) {
	if method == http.MethodGet {
		return c.doWithRetry(method, path, maxGetRetries)
	}
	return c.doOnce(method, path, body)
}

func (c *Client) doWithRetry(method, path string, attempts int) (json.RawMessage, error) {
	var lastErr error
	for attempt := 0; attempt < attempts; attempt++ {
		if attempt > 0 {
			// Small linear backoff — this is a hackathon devops CLI making
			// at most a handful of calls per invocation, not a service
			// under load; no need for jitter/exponential complexity.
			time.Sleep(time.Duration(attempt) * 500 * time.Millisecond)
		}
		raw, err := c.doOnce(method, path, nil)
		if err == nil {
			return raw, nil
		}
		lastErr = err

		var statusErr *StatusError
		if errors.As(err, &statusErr) && statusErr.StatusCode < 500 {
			// Client error (401/403/404/...) — retrying the same request
			// won't change the outcome. Fail fast instead of burning the
			// retry budget and making the operator wait for nothing.
			return nil, err
		}
		// Otherwise: network-level error or 5xx — worth retrying.
	}
	return nil, fmt.Errorf("giving up after %d attempts: %w", attempts, lastErr)
}

func (c *Client) doOnce(method, path string, body io.Reader) (json.RawMessage, error) {
	url := c.cfg.BaseURL + path
	req, err := http.NewRequest(method, url, body)
	if err != nil {
		return nil, fmt.Errorf("build request %s %s: %w", method, url, err)
	}
	req.Header.Set("Authorization", "Bearer "+c.cfg.Token)
	req.Header.Set("Accept", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("%s %s: %w", method, url, err)
	}
	defer resp.Body.Close()

	raw, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("read response body for %s %s: %w", method, url, err)
	}
	if resp.StatusCode >= 400 {
		return nil, &StatusError{Method: method, URL: url, StatusCode: resp.StatusCode, Body: string(raw)}
	}
	return json.RawMessage(raw), nil
}
