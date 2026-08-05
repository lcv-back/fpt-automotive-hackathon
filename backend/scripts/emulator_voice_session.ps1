<#
.SYNOPSIS
    Lai mot phien doc 22 cau benchmark vao emulator AAOS, thu VIVA_TRACE.

.DESCRIPTION
    Thu nay ton tai vi mot ly do rat cu the: app KHONG co duong bom cau bang
    text. `VoiceAssistantService` chi nhan `ACTION_START_LISTENING`, va
    `run_benchmark.ps1 -Adb` chi HUNG logcat chu khong doc cau vao. Nghia la bo
    22 cau bat buoc phai co nguoi noi that.

    Script nay lo phan may moc quanh viec do: cai app, dat quyen, chon tieng
    Viet, roi voi tung cau thi in cau len man hinh, bam mic ho, cho luot ket
    thuc, va ghi lai trace. Nguoi ngoi truoc may chi phai doc to cau dang hien.

    Ket qua ghi ra mot capture.log dung dinh dang ma `run_benchmark.ps1` va
    `viva-tools harness` da doc duoc.

.PARAMETER Setup
    Chi cai dat va chuan bi may (cai APK, cap quyen, chon tieng Viet) roi dung.

.PARAMETER Utterances
    So cau chay. Mac dinh 0 = ca 22 cau trong suite.

.PARAMETER OutDir
    Thu muc ghi ket qua. Mac dinh: evidence/emulator/session-<stamp>.

.EXAMPLE
    .\emulator_voice_session.ps1 -Setup
    .\emulator_voice_session.ps1 -Utterances 3      # chay thu 3 cau truoc

.NOTES
    Emulator PHAI chay voi -allow-host-audio, neu khong dau vao mic bi zero
    va moi luot deu ra Error:speech_end. Xem evidence/emulator/README.md.
#>
[CmdletBinding()]
param(
    [switch]$Setup,
    [int]$Utterances = 0,
    [string]$OutDir,
    [string]$Serial,
    [string]$Apk = "$PSScriptRoot\..\..\automotive\app\build\outputs\apk\mock\debug\app-mock-debug.apk",
    [string]$Suite = "$PSScriptRoot\..\suites\benchmark_v1.csv"
)

$ErrorActionPreference = 'Stop'
$PKG = 'com.sopa.viva_automotive.mock'
$ACTIVITY = "$PKG/com.sopa.viva_automotive.MainActivity"

# Toa do tren profile automotive_1080p_landscape (1080x600). Doi profile la
# phai do lai bang `adb shell input tap` roi chup man hinh kiem chung.
$TAP_MIC       = @(111, 483)
$TAP_SETTINGS  = @(29, 183)
$TAP_VIETNAMESE = @(806, 365)

$adbExe = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path $adbExe)) { $adbExe = 'adb' }

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)
    $prefix = @()
    if ($Serial) { $prefix += @('-s', $Serial) }
    & $adbExe @prefix @Args
}

function Assert-Device {
    $devices = (Invoke-Adb devices) -join "`n"
    if ($devices -notmatch 'device\s*$' -and $devices -notmatch "device`n") {
        throw @"
Khong thay thiet bi nao. Khoi dong emulator truoc, VA PHAI co -allow-host-audio:

  & "`$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd viva_aaos34 ``
      -no-snapshot -no-boot-anim -gpu auto -allow-host-audio
"@
    }
}

function Initialize-App {
    Write-Host "== Cai APK ==" -ForegroundColor Cyan
    if (-not (Test-Path $Apk)) {
        throw "Khong thay APK: $Apk`nChay truoc: automotive\gradlew :app:assembleMockDebug"
    }
    Invoke-Adb install -r -t $Apk | Select-Object -Last 1

    Write-Host "== Cap quyen ==" -ForegroundColor Cyan
    Invoke-Adb shell pm grant $PKG android.permission.RECORD_AUDIO
    Invoke-Adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS

    Write-Host "== Mo app ==" -ForegroundColor Cyan
    Invoke-Adb shell am start -n $ACTIVITY | Out-Null
    Start-Sleep -Seconds 8

    # App mac dinh chay tieng Anh (VoiceLanguage.fromStorageKey -> ENGLISH).
    # Khong doi buoc nay thi Vosk nap model-en-us va moi cau tieng Viet deu sai.
    Write-Host "== Chon tieng Viet ==" -ForegroundColor Cyan
    Invoke-Adb shell input tap $TAP_SETTINGS[0] $TAP_SETTINGS[1]
    Start-Sleep -Seconds 2
    Invoke-Adb shell input tap $TAP_VIETNAMESE[0] $TAP_VIETNAMESE[1]
    Start-Sleep -Seconds 2

    Write-Host "Xong. Kiem lai bang mat: Settings -> Voice language phai tick 'Tieng Viet'." -ForegroundColor Green
}

function Wait-ForTurn {
    <#  Cho den khi xuat hien mot dong VIVA_TRACE_SUMMARY moi.
        Tra ve cac dong trace cua luot do, hoac $null neu qua han. #>
    param([int]$TimeoutSec = 30)

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $lines = Invoke-Adb logcat -d -s VIVA_TRACE:I
        if ($lines -match 'VIVA_TRACE_SUMMARY') { return $lines }
        Start-Sleep -Milliseconds 700
    }
    return $null
}

Assert-Device

if ($Setup) { Initialize-App; return }

if (-not $OutDir) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $OutDir = Join-Path $PSScriptRoot "..\..\evidence\emulator\session-$stamp"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$capture = Join-Path $OutDir 'capture.log'
$manifest = Join-Path $OutDir 'run_manifest.txt'

$rows = Import-Csv $Suite
if ($Utterances -gt 0) { $rows = $rows | Select-Object -First $Utterances }

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Yellow
Write-Host " $($rows.Count) cau. Voi moi cau: doc TO va RO khi thay chu 'NOI:'" -ForegroundColor Yellow
Write-Host " Script tu bam mic va tu hung trace. Ctrl+C de dung." -ForegroundColor Yellow
Write-Host "=====================================================" -ForegroundColor Yellow
Write-Host ""

$done = 0
$missed = 0

foreach ($row in $rows) {
    Invoke-Adb logcat -c
    Invoke-Adb shell am start -n $ACTIVITY | Out-Null
    Start-Sleep -Milliseconds 800

    Write-Host ("[{0}] " -f $row.id) -NoNewline -ForegroundColor DarkGray
    Write-Host "NOI: " -NoNewline -ForegroundColor Cyan
    Write-Host $row.utterance -ForegroundColor White
    if ($row.notes) { Write-Host ("      ({0})" -f $row.notes) -ForegroundColor DarkGray }

    Invoke-Adb shell input tap $TAP_MIC[0] $TAP_MIC[1]
    $lines = Wait-ForTurn -TimeoutSec 30

    if (-not $lines) {
        Write-Host "      -> qua han, khong co SUMMARY. Bo qua cau nay." -ForegroundColor Red
        $missed++
        continue
    }

    $lines | Add-Content -Path $capture -Encoding utf8
    $summary = ($lines | Select-String 'VIVA_TRACE_SUMMARY' | Select-Object -Last 1).ToString()
    $fields = ($summary -split 'VIVA_TRACE_SUMMARY\|')[-1] -split '\|'
    $gotIntent = if ($fields.Count -gt 2) { $fields[1] } else { '?' }
    $gotVerdict = if ($fields.Count -gt 3) { $fields[2] } else { '?' }

    $ok = ($gotIntent -eq $row.expect_intent) -and ($gotVerdict -eq $row.expect_verdict)
    $mark = if ($ok) { 'PASS' } else { 'DIFF' }
    $colour = if ($ok) { 'Green' } else { 'Yellow' }
    Write-Host ("      -> {0}  intent={1} verdict={2}  (mong doi: {3} / {4})" -f `
        $mark, $gotIntent, $gotVerdict, $row.expect_intent, $row.expect_verdict) -ForegroundColor $colour
    $done++
}

@"
emulator voice session
======================
thoi diem   : $(Get-Date -Format o)
commit      : $(git -C "$PSScriptRoot\..\.." rev-parse HEAD)
suite       : $Suite
so cau chay : $done (qua han: $missed)
thiet bi    : $((Invoke-Adb shell getprop ro.build.fingerprint) -join '')
apk         : $Apk
sha256      : $((Get-FileHash $Apk -Algorithm SHA256).Hash)
nhan        : EMULATOR AAOS tren may dev — KHONG phai Device CarSky.
              Khong duoc dung file nay de dong E03/E04.
"@ | Set-Content -Path $manifest -Encoding utf8

Write-Host ""
Write-Host "capture : $capture" -ForegroundColor Green
Write-Host "manifest: $manifest" -ForegroundColor Green
Write-Host ""
Write-Host "Buoc tiep theo — lap bang tu capture nay:" -ForegroundColor Cyan
Write-Host "  cd backend" -ForegroundColor Gray
Write-Host "  go run ./cmd/viva-tools harness verify --suite suites\benchmark_v1.csv --input `"$capture`"" -ForegroundColor Gray
