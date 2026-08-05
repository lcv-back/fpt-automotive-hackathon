package cli

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"

	"viva-tools/internal/config"
	"viva-tools/internal/infrastructure/carsky"
	"viva-tools/internal/interfaces/repository"
	"viva-tools/internal/usecase/devops"
)

func runCarsky(args []string) int {
	if len(args) < 1 {
		fmt.Fprintln(os.Stderr, "usage: viva-tools carsky <blueprint export|blueprint clone|nodes|adb-tunnel> ...")
		return 2
	}

	if err := config.LoadDotEnv(".env"); err != nil {
		fmt.Fprintf(os.Stderr, "error loading .env: %v\n", err)
		return 1
	}
	cfg, err := config.LoadCarSky()
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}
	gw := carsky.New(cfg)

	switch args[0] {
	case "blueprint":
		return runCarskyBlueprint(gw, args[1:])
	case "nodes":
		return runCarskyNodes(gw, cfg, args[1:])
	case "adb-tunnel":
		return runCarskyAdbTunnel(gw, cfg, args[1:])
	default:
		fmt.Fprintf(os.Stderr, "unknown carsky subcommand %q\n", args[0])
		return 2
	}
}

func runCarskyBlueprint(gw repository.CarSkyGateway, args []string) int {
	if len(args) < 1 {
		fmt.Fprintln(os.Stderr, "usage: viva-tools carsky blueprint <export|clone> ...")
		return 2
	}
	switch args[0] {
	case "export":
		fs := flag.NewFlagSet("carsky blueprint export", flag.ContinueOnError)
		id := fs.String("id", "", "blueprint id (required)")
		out := fs.String("out", "", "output JSON path (required)")
		if err := fs.Parse(args[1:]); err != nil {
			return 2
		}
		if *id == "" || *out == "" {
			fmt.Fprintln(os.Stderr, "error: --id and --out are required")
			return 2
		}
		raw, err := gw.ExportBlueprint(*id)
		if err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			return 1
		}
		if err := writeJSONFile(*out, raw); err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			return 1
		}
		fmt.Printf("blueprint %s exported to %s\n", *id, *out)
		return 0

	case "clone":
		fs := flag.NewFlagSet("carsky blueprint clone", flag.ContinueOnError)
		id := fs.String("id", "", "blueprint id (required)")
		name := fs.String("name", "", "name for the new blueprint (required by the CarSky API)")
		backupOut := fs.String("backup-out", "", "path to save the pre-clone backup JSON (required — SafeClone refuses to clone without one)")
		cloneOut := fs.String("clone-out", "", "path to save the clone response JSON (required)")
		if err := fs.Parse(args[1:]); err != nil {
			return 2
		}
		if *id == "" || *name == "" || *backupOut == "" || *cloneOut == "" {
			fmt.Fprintln(os.Stderr, "error: --id, --name, --backup-out, and --clone-out are required")
			return 2
		}
		backup, cloneResp, err := devops.SafeClone(gw, *id, *name)
		if backup != nil {
			if werr := writeJSONFile(*backupOut, backup); werr != nil {
				fmt.Fprintf(os.Stderr, "error writing backup: %v\n", werr)
				return 1
			}
			fmt.Printf("backup saved to %s\n", *backupOut)
		}
		if err != nil {
			fmt.Fprintf(os.Stderr, "error: %v\n", err)
			return 1
		}
		if werr := writeJSONFile(*cloneOut, cloneResp); werr != nil {
			fmt.Fprintf(os.Stderr, "error writing clone response: %v\n", werr)
			return 1
		}
		fmt.Printf("blueprint %s cloned, response saved to %s\n", *id, *cloneOut)
		return 0

	default:
		fmt.Fprintf(os.Stderr, "unknown blueprint subcommand %q\n", args[0])
		return 2
	}
}

func runCarskyNodes(gw repository.CarSkyGateway, cfg config.CarSky, args []string) int {
	fs := flag.NewFlagSet("carsky nodes", flag.ContinueOnError)
	room := fs.String("room", cfg.RoomID, "room id (defaults to CARSKY_ROOM_ID)")
	out := fs.String("out", "", "optional output JSON path; prints to stdout if omitted")
	if err := fs.Parse(args); err != nil {
		return 2
	}
	if *room == "" {
		fmt.Fprintln(os.Stderr, "error: --room is required (or set CARSKY_ROOM_ID)")
		return 2
	}
	raw, err := gw.ListNodes(*room)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}
	return outputJSON(raw, *out)
}

func runCarskyAdbTunnel(gw repository.CarSkyGateway, cfg config.CarSky, args []string) int {
	fs := flag.NewFlagSet("carsky adb-tunnel", flag.ContinueOnError)
	room := fs.String("room", cfg.RoomID, "room id (defaults to CARSKY_ROOM_ID)")
	out := fs.String("out", "", "optional output JSON path; prints to stdout if omitted")
	if err := fs.Parse(args); err != nil {
		return 2
	}
	if *room == "" {
		fmt.Fprintln(os.Stderr, "error: --room is required (or set CARSKY_ROOM_ID)")
		return 2
	}
	raw, err := gw.AdbTunnelInfo(*room)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}
	return outputJSON(raw, *out)
}

func writeJSONFile(path string, raw json.RawMessage) error {
	pretty, err := prettyJSON(raw)
	if err != nil {
		return err
	}
	return os.WriteFile(path, pretty, 0o644)
}

func outputJSON(raw json.RawMessage, outPath string) int {
	pretty, err := prettyJSON(raw)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}
	if outPath == "" {
		fmt.Println(string(pretty))
		return 0
	}
	if err := os.WriteFile(outPath, pretty, 0o644); err != nil {
		fmt.Fprintf(os.Stderr, "error: %v\n", err)
		return 1
	}
	fmt.Printf("written to %s\n", outPath)
	return 0
}

func prettyJSON(raw json.RawMessage) ([]byte, error) {
	var buf []byte
	buf, err := json.MarshalIndent(json.RawMessage(raw), "", "  ")
	if err != nil {
		return nil, fmt.Errorf("format response as JSON: %w", err)
	}
	return buf, nil
}
