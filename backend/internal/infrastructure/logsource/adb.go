package logsource

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"os/exec"
	"time"
)

// adbTimeout bounds how long we'll wait on the `adb` subprocess. Without
// this, a device that's wedged (tunnel open but not responding) would hang
// the CLI forever instead of failing with a message the operator can act on.
const adbTimeout = 30 * time.Second

// AdbSource pulls the current logcat buffer straight off a connected
// device/emulator via the standard `adb` CLI, filtered to the VIVA_TRACE tag
// (see vong2/03-contracts.md §1: "adb logcat tag = VIVA_TRACE" — the summary
// line is emitted under the same tag). `-d` dumps the buffer and exits
// instead of streaming, which is what a one-shot report generator wants.
//
// Requires `adb` on PATH and a device already reachable (adb connect / adb
// tunnel already opened — see V5 in 06-PHAN-CONG-4-NGUOI.md). This does not
// open the tunnel itself.
type AdbSource struct {
	// Serial optionally targets one device (adb -s <serial> ...). Leave
	// empty to use whatever `adb` picks by default (fails if more than one
	// device is attached).
	Serial string
}

func (a AdbSource) Lines() ([]string, error) {
	args := []string{}
	if a.Serial != "" {
		args = append(args, "-s", a.Serial)
	}
	args = append(args, "logcat", "-d", "-s", "VIVA_TRACE:I")

	ctx, cancel := context.WithTimeout(context.Background(), adbTimeout)
	defer cancel()

	cmd := exec.CommandContext(ctx, "adb", args...)
	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr
	if err := cmd.Run(); err != nil {
		if errors.Is(ctx.Err(), context.DeadlineExceeded) {
			return nil, fmt.Errorf("adb %v: timed out after %s — is the device/tunnel still responsive?", args, adbTimeout)
		}
		return nil, fmt.Errorf("adb %v: %w (stderr: %s)", args, err, stderr.String())
	}

	return scanLines(&stdout)
}
