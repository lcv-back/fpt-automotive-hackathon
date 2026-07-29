package carsky

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"viva-tools/internal/config"
)

func testConfig(baseURL string) config.CarSky {
	return config.CarSky{
		BaseURL: baseURL,
		Token:   "test-token-123",
		Timeout: 5 * time.Second,
	}
}

func TestExportBlueprint_MethodPathAuthAndBody(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			t.Errorf("method = %s, want GET", r.Method)
		}
		if r.URL.Path != "/blueprints/bp-1/export" {
			t.Errorf("path = %s, want /blueprints/bp-1/export", r.URL.Path)
		}
		if got := r.Header.Get("Authorization"); got != "Bearer test-token-123" {
			t.Errorf("Authorization header = %q", got)
		}
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"id":"bp-1","nodes":[]}`))
	}))
	defer srv.Close()

	c := New(testConfig(srv.URL))
	raw, err := c.ExportBlueprint("bp-1")
	if err != nil {
		t.Fatalf("ExportBlueprint: %v", err)
	}
	var decoded map[string]any
	if err := json.Unmarshal(raw, &decoded); err != nil {
		t.Fatalf("response is not valid JSON passthrough: %v", err)
	}
	if decoded["id"] != "bp-1" {
		t.Errorf("decoded id = %v, want bp-1", decoded["id"])
	}
}

func TestCloneBlueprint_UsesPOST_NoRetryOn500(t *testing.T) {
	var calls int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		atomic.AddInt32(&calls, 1)
		if r.Method != http.MethodPost {
			t.Errorf("method = %s, want POST", r.Method)
		}
		w.WriteHeader(http.StatusInternalServerError)
		w.Write([]byte(`{"error":"boom"}`))
	}))
	defer srv.Close()

	c := New(testConfig(srv.URL))
	_, err := c.CloneBlueprint("bp-1")
	if err == nil {
		t.Fatal("expected error from 500 response")
	}
	if got := atomic.LoadInt32(&calls); got != 1 {
		t.Fatalf("server was called %d times, want exactly 1 (POST must never auto-retry)", got)
	}
}

func TestListNodes_RetriesOn5xxThenSucceeds(t *testing.T) {
	var calls int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		n := atomic.AddInt32(&calls, 1)
		if n < 3 {
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"nodes":["ccu-1"]}`))
	}))
	defer srv.Close()

	c := New(testConfig(srv.URL))
	raw, err := c.ListNodes("room-1")
	if err != nil {
		t.Fatalf("ListNodes: %v", err)
	}
	if !strings.Contains(string(raw), "ccu-1") {
		t.Fatalf("unexpected body: %s", raw)
	}
	if got := atomic.LoadInt32(&calls); got != 3 {
		t.Fatalf("server was called %d times, want 3 (2 failures + 1 success)", got)
	}
}

func TestAdbTunnelInfo_NoRetryOn4xx(t *testing.T) {
	var calls int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		atomic.AddInt32(&calls, 1)
		w.WriteHeader(http.StatusNotFound)
		w.Write([]byte(`{"error":"room not found"}`))
	}))
	defer srv.Close()

	c := New(testConfig(srv.URL))
	_, err := c.AdbTunnelInfo("missing-room")
	if err == nil {
		t.Fatal("expected error for 404")
	}
	if got := atomic.LoadInt32(&calls); got != 1 {
		t.Fatalf("server was called %d times, want exactly 1 (4xx must not retry)", got)
	}
	if !strings.Contains(err.Error(), "404") {
		t.Fatalf("error %v does not mention status 404", err)
	}
}

func TestListNodes_GivesUpAfterMaxRetries(t *testing.T) {
	var calls int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		atomic.AddInt32(&calls, 1)
		w.WriteHeader(http.StatusBadGateway)
	}))
	defer srv.Close()

	c := New(testConfig(srv.URL))
	_, err := c.ListNodes("room-1")
	if err == nil {
		t.Fatal("expected error, server always fails")
	}
	if got := atomic.LoadInt32(&calls); got != maxGetRetries {
		t.Fatalf("server was called %d times, want exactly %d", got, maxGetRetries)
	}
}
