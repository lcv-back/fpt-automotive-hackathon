# Implementation Plan: SafetyGuard integration readiness

## Overview

Turn the existing `feature/safety-guard` policy classes into an actually enforced vehicle-write boundary without claiming Device evidence. The first slice covers the safety-critical door-unlock path only: obtain current speed from the underlying repository, deny before any setter at unsafe speed, and fail closed when speed cannot be read.

## Architecture Decisions

- Keep `SafetyGuard` in `vehicle-service` and preserve the repository decorator boundary so all vehicle writes share one enforcement point.
- Read safety state inside the suspend write operation instead of accepting a synchronous caller-provided snapshot that production DI cannot currently supply.
- Treat missing/invalid speed as a denial for door unlock; do not block unrelated HVAC or cabin-light writes on speed availability.
- Keep confirmation UX and voice confidence propagation out of this slice. They need an explicit command context/two-turn contract and must not be implied by wiring alone.

## Task List

### Phase 1: Safety-critical repository path

- [x] Add failing tests for live speed lookup and fail-closed behavior.
- [x] Make `GuardedVehicleRepository` build its door safety snapshot from the delegate.
- [x] Verify focused vehicle-service tests.

### Checkpoint: Repository boundary

- [x] Unsafe or unknown-speed door unlock never reaches the delegate setter.
- [x] Safe unrelated property writes still reach the delegate.

### Phase 2: Production wiring

- [x] Provide the decorated repository in both `real` and `mock` app variants.
- [x] Verify Hilt compilation and the full JVM test suite.

### Checkpoint: Complete

- [x] APK code path resolves `VehicleRepository` to the guarded decorator.
- [x] All automated tests pass.
- [x] Remaining confirmation/confidence limitations are documented without overclaiming.

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Speed unit mismatch | Unsafe command allowed or denied incorrectly | Convert repository m/s value to km/h in one tested location |
| Vehicle speed unavailable | Door unlock could fail open | Deny with `G1_STALE_STATE` before setter |
| Decorator not selected by Hilt | No runtime behavior change | Replace direct variant bindings with explicit provider tests/build verification |
| Confirmation loops | Door unlock unusable after prompt | Do not claim confirmation complete; leave explicit follow-up task |

## Open Questions

- Which UI/voice component will own the confirmation token for `G2_CONFIRM_DOOR`?
- How will ASR confidence be carried across the existing `VehicleRepository` API?
