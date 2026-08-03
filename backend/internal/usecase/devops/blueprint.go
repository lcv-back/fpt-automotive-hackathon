// Package devops implements the CarSky devops use cases (V2/V3/V5 in
// vong2/06-PHAN-CONG-4-NGUOI.md): blueprint backup/clone and node lookup.
package devops

import (
	"encoding/json"
	"fmt"

	"viva-tools/internal/interfaces/repository"
)

// SafeClone implements the safe blueprint-editing procedure documented in
// vong2/04-KE-HOACH-CAP-NHAT-28-07.md ("An toàn khi sửa blueprint"):
//  1. GET .../export → save JSON as backup BEFORE touching anything
//  2. Clone the blueprint; the team edits only the clone, never the
//     "Started pack" original in place
//
// If the export fails, we refuse to clone at all — cloning without a
// backup in hand defeats the point. If the export succeeds but the clone
// call fails, we still return the backup so the caller can persist it: the
// operator can safely retry the clone with no risk of having lost the
// backup in the process.
// cloneName is required by the API; an empty one is rejected here rather than
// letting CarSky answer 400 after the backup has already been taken.
func SafeClone(gw repository.CarSkyGateway, blueprintID, cloneName string) (backup, cloneResp json.RawMessage, err error) {
	if cloneName == "" {
		return nil, nil, fmt.Errorf("clone name is required by the CarSky API")
	}
	backup, err = gw.ExportBlueprint(blueprintID)
	if err != nil {
		return nil, nil, fmt.Errorf("export backup before clone (refusing to clone without one): %w", err)
	}
	cloneResp, err = gw.CloneBlueprint(blueprintID, cloneName)
	if err != nil {
		return backup, nil, fmt.Errorf("clone blueprint (backup already saved, safe to retry): %w", err)
	}
	return backup, cloneResp, nil
}
