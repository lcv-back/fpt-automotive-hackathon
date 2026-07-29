# viva-tools

Go CLI for Team VIVA's backend surface: the **benchmark harness** and the
**CarSky devops helper**. This is *not* a Command Dispatcher / skills API —
that logic now lives in-app (Kotlin, on Android/AAOS). See
[Scope](#scope-why-this-is-not-a-command-dispatcher) below.

## What it does

### `viva-tools harness report`

Parses a captured VIVA_TRACE log (see `vong2/03-contracts.md` §1 for the
exact line format) into per-stage latency stats — p50/p95/min/max over every
adjacent pipeline segment (`speech_start`→`speech_end`, `asr_sent`→`asr_done`,
...), plus the app's own reported `e2e_ms` for cross-checking. Outputs a CSV
and a stdout summary.

```
viva-tools harness report --input path/to/log.txt --out report.csv
viva-tools harness report --adb --serial <device-serial> --out report.csv
```

Try it against the bundled fixture:

```
go run ./cmd/viva-tools harness report --input testdata/sample_trace.log --out /tmp/report.csv
```

### `viva-tools carsky ...`

Thin wrapper over the confirmed CarSky REST endpoints
(`docs/Car-Sky-Platform.html`, base path `/api/v1`, auth
`Authorization: Bearer <token>`):

```
viva-tools carsky blueprint export --id <blueprintId> --out backup.json
viva-tools carsky blueprint clone  --id <blueprintId> --backup-out backup.json --clone-out clone.json
viva-tools carsky nodes            --room <roomId> [--out nodes.json]
viva-tools carsky adb-tunnel       --room <roomId>
```

`blueprint clone` implements the safe-editing procedure from
`vong2/04-KE-HOACH-CAP-NHAT-28-07.md` ("An toàn khi sửa blueprint"): it
**always exports a backup before cloning**, and refuses to clone if the
backup export fails.

Response bodies for `nodes`/`adb-tunnel`/blueprint export/clone are printed
or saved as **raw JSON**, not typed structs — the docs confirm the endpoint
paths and auth scheme but not the exact response schema. There's a live
`GET /api/v1/openapi` on the platform; once someone has pulled the real
schema, tighten `internal/infrastructure/carsky` to use typed responses
instead of guessing field names.

**Fault tolerance:** GET requests (`nodes`, `adb-tunnel`, `blueprint export`)
retry up to 3 times with linear backoff on network errors or 5xx responses,
and fail immediately (no retry) on 4xx — a bad token or bad id won't get
fixed by asking again. `blueprint clone` (POST) never auto-retries, since a
duplicate clone on CarSky is worse than a failed call you retry by hand.
Every HTTP call has a configurable timeout (`CARSKY_TIMEOUT_SECONDS`,
default 30s) and `adb logcat` calls time out after 30s so a wedged
device/tunnel can't hang the CLI. See `client_test.go` for the tests that
pin this behavior down (retry counts, no-retry-on-4xx, no-retry-on-POST).

## Configuration

Copy `.env.example` to `.env` and fill in `CARSKY_API_TOKEN` (and
`CARSKY_ROOM_ID` if you want a default room). `.env` is git-ignored.

`CARSKY_BASE_URL` in `.env.example` is a **best guess** — `docs/link.md`
only records the CarSky web UI URL, not necessarily the API host. Confirm
before relying on it.

## Build & test

```
go build ./...       # or: make build
go vet ./...
go test ./...         # or: make test
```

No external dependencies — stdlib only (deliberate, for a hackathon: no
`go mod tidy` network dependency, no vendoring concerns, single static
binary).

## Architecture (clean architecture layering)

```
cmd/viva-tools/            entrypoint — wires nothing itself, just calls cli.Run
internal/domain/           entities + pure parsing rules, zero external deps
internal/usecase/          application logic (harness aggregation/report, devops SafeClone)
internal/interfaces/
  repository/               ports (interfaces) usecases depend on
  cli/                       composition root + presentation (arg parsing, stdout/CSV/JSON formatting)
internal/infrastructure/
  logsource/                 LineSource: file/stdin, or live `adb logcat`
  carsky/                    CarSkyGateway: HTTP client over the CarSky API
  report/                     CSV writer
internal/config/            env-var (+ optional .env) configuration loading
```

Dependency direction is inward only: `domain` knows nothing about anyone;
`usecase` depends on `domain` and on the `repository` interfaces (never on
`infrastructure` directly); `infrastructure` implements those interfaces;
`cli` is the only place that imports concrete infrastructure types and
wires them into usecases.

## Scope: why this is not a Command Dispatcher

The root `PLAN.md`/`CLAUDE.md` in this repo describe an older architecture
(Python/Node "Command Dispatcher" backend, 3-tier intent router with a cloud
LLM fallback). That was cut in the 28/07 replan — see
`vong2/04-KE-HOACH-CAP-NHAT-28-07.md` (Phần 4) and `vong2/03-contracts.md`.
Dispatch, the safety guard, and all four skills (including delivery) now run
in-app as Kotlin. The only backend surface left is: `viva-asr` (a separate
Python service — deliberately not built in Go, see the ASR note below),
this benchmark harness, and CarSky devops plumbing.

**ASR (`viva-asr`) is intentionally not part of this Go project.** Serving
whisper-tiny/PhoWhisper INT8 inference is far better supported in Python
today (`faster-whisper`/CTranslate2) than via Go ONNX bindings, and there's
no time budget left in the hackathon to de-risk an unusual toolchain choice.
