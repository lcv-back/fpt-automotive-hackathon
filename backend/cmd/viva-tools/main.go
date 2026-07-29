// Command viva-tools is the entrypoint. It contains no logic of its own —
// see internal/interfaces/cli for the composition root.
package main

import (
	"os"

	"viva-tools/internal/interfaces/cli"
)

func main() {
	os.Exit(cli.Run(os.Args[1:]))
}
