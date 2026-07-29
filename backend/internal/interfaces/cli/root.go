// Package cli is the composition root and interface-adapter layer: it
// parses arguments, wires infrastructure implementations to usecase ports,
// and formats results for a terminal. No business rule lives here.
package cli

import (
	"fmt"
	"os"
)

// Run dispatches to a subcommand and returns a process exit code.
func Run(args []string) int {
	if len(args) < 1 {
		printUsage()
		return 2
	}
	switch args[0] {
	case "harness":
		return runHarness(args[1:])
	case "carsky":
		return runCarsky(args[1:])
	case "-h", "--help", "help":
		printUsage()
		return 0
	default:
		fmt.Fprintf(os.Stderr, "unknown command %q\n\n", args[0])
		printUsage()
		return 2
	}
}

func printUsage() {
	fmt.Fprintln(os.Stderr, `viva-tools — VIVA benchmark harness + CarSky devops CLI

Usage:
  viva-tools harness report --input <path|-> [--out report.csv]
  viva-tools harness report --adb [--serial <id>] [--out report.csv]

  viva-tools carsky blueprint export --id <blueprintId> --out backup.json
  viva-tools carsky blueprint clone  --id <blueprintId> --backup-out backup.json --clone-out clone.json
  viva-tools carsky nodes            --room <roomId> [--out nodes.json]
  viva-tools carsky adb-tunnel       --room <roomId>

Env (see .env.example): CARSKY_BASE_URL, CARSKY_API_TOKEN, CARSKY_ROOM_ID`)
}
