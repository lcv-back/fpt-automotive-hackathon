// Package logsource provides repository.LineSource implementations that
// read raw log text from a file, stdin, or a live adb device.
package logsource

import (
	"fmt"
	"io"
	"os"
)

// FileSource reads every line from a file path, or from stdin when path is
// "-". This is the primary, zero-guessing input mode: capture logcat output
// however you like (adb logcat > file.log, or the CarSky
// GET /deployments/:roomId/logs/:nodeKey endpoint), then feed the file in.
type FileSource struct {
	Path string
}

func (f FileSource) Lines() ([]string, error) {
	var r io.Reader
	if f.Path == "-" {
		r = os.Stdin
	} else {
		file, err := os.Open(f.Path)
		if err != nil {
			return nil, fmt.Errorf("open %s: %w", f.Path, err)
		}
		defer file.Close()
		r = file
	}
	return scanLines(r)
}
