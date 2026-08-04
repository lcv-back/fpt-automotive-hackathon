# SafetyGuard integration checklist

## Task 1: Enforce live speed on door unlock

**Acceptance criteria:**
- [x] Door unlock reads `PERF_VEHICLE_SPEED` from the underlying repository.
- [x] Speed above 5 km/h returns `Deny:G1_SPEED_LOCK` without calling the setter.
- [x] Missing, unavailable, non-numeric, or non-finite speed returns `Deny:G1_STALE_STATE` without calling the setter.

**Verification:**
- [x] `automotive/gradlew :vehicle-service:impl:testDebugUnitTest`

**Dependencies:** None

**Estimated scope:** Medium (2-3 files)

## Task 2: Wire the decorator into app variants

**Acceptance criteria:**
- [x] Real and mock variants resolve `VehicleRepository` through `GuardedVehicleRepository`.
- [x] Existing repository implementations remain the delegate and retain singleton scope.

**Verification:**
- [x] `automotive/gradlew test`
- [x] Hilt/app compilation included in the repository's test/build gate.

**Dependencies:** Task 1

**Estimated scope:** Medium (2-4 files)

## Task 3: Record remaining evidence boundary

**Acceptance criteria:**
- [x] No document claims Device execution from JVM tests.
- [x] Confirmation and confidence propagation remain explicitly open if not implemented.

**Verification:**
- [x] Review final diff and current claim/evidence map.

**Remaining follow-ups (not completed by this slice):**

- [ ] Route the second voice turn into `VehicleWriteContext(isConfirmed = true)`.
- [ ] Propagate calibrated ASR confidence into `VehicleWriteContext.confidence`.
- [ ] Capture Device evidence before changing C-SAFETY/E09 from red.

**Dependencies:** Tasks 1-2

**Estimated scope:** Small (0-1 file)
