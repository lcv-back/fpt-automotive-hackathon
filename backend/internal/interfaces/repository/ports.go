// Package repository declares the ports (interfaces) usecases depend on.
// Concrete implementations live under internal/infrastructure/*; usecases
// never import infrastructure directly, only these interfaces, so the
// harness/devops logic stays testable without a real adb device or a live
// CarSky account.
package repository

import "encoding/json"

// LineSource yields the raw text lines of a log capture — from a file, from
// stdin, or from `adb logcat` — without the usecase layer knowing which.
type LineSource interface {
	Lines() ([]string, error)
}

// CarSkyGateway wraps the subset of the CarSky REST API
// (docs/Car-Sky-Platform.html, base path /api/v1) that the devops CLI needs.
//
// Responses are returned as raw JSON, not typed structs: the docs confirm
// the endpoint paths, method, and auth scheme (Authorization: Bearer
// <token>) but not the exact response body shape. Inventing field names
// here would be guessing, not implementing — decode into typed structs
// once someone has pulled the real schema from GET /api/v1/openapi.
type CarSkyGateway interface {
	// ExportBlueprint calls GET /api/v1/blueprints/:id/export.
	ExportBlueprint(id string) (json.RawMessage, error)
	// CloneBlueprint calls POST /api/v1/blueprints/:id/clone.
	CloneBlueprint(id string) (json.RawMessage, error)
	// ListNodes calls GET /api/v1/deployments/:roomId/nodes.
	ListNodes(roomID string) (json.RawMessage, error)
	// AdbTunnelInfo calls GET /api/v1/deployments/:roomId/adb-tunnel.
	AdbTunnelInfo(roomID string) (json.RawMessage, error)
}
