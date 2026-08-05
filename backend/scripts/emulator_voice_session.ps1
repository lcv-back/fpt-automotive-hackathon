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
    HAI DIEU KIEN cho mic, thieu mot la moi luot ra Error:speech_end:

    1. Emulator phai khoi dong voi -allow-host-audio. Co nay chi CHO PHEP.
    2. Trong emulator: nut "..." (Extended Controls) -> muc Microphone ->
       bat "Virtual microphone uses host audio input". Cai nay moi NOI tieng
       vao may ao, va no MAC DINH TAT.

    Thieu buoc 2 thi log ghi peak=2/32767 suot ca luot — mic ao day im lang.
    Do 05/08 mat vai gio moi tim ra vi cho nay khong duoc ghi o dau.

    Sau khi bat: noi TO va GAN. Can peak > 5000/32767 moi du cho Vosk nhan
    dang; do bang `adb logcat -s VoskEngine:D` khi dang noi. Muc ~1700 la
    nghe thay tieng nhung khong doc ra chu nao.

    Xem evidence/emulator/README.md.
#>
[CmdletBinding()]
param(
    [switch]$Setup,
    # Bơm câu bằng text thay vì chờ người nói. Đo router -> guard -> skill,
    # KHÔNG đo mic/VAD/ASR/WER. Xem SimulatedUtteranceReceiver và vong2/25 §2 F7.
    [switch]$Inject,
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

# Trang thai xe bat buoc phai co truoc mot so cau — suite ghi trong cot `notes`
# bang van xuoi, khong may nao doc duoc, nen dat lai o day cho ro.
#
# B09 va B10 la CUNG mot cau "mo cua" nhung ky vong nguoc nhau, va thu phan
# biet chung la toc do. Khong dat truoc thi ket qua phu thuoc vao lan chay
# truoc do da ghim gi — dung mot lan chay khong tai lap duoc.
#
# Gia tri la m/s vi PERF_VEHICLE_SPEED dung m/s; 16.7 m/s = 60 km/h.
$PRECONDITION = @{
    'B09' = @{ unit = 'speed'; value = '0'    }  # xe dung yen -> phai HOI xac nhan
    'B10' = @{ unit = 'speed'; value = '16.7' }  # 60 km/h     -> phai TU CHOI
}

# Nhan mang tuong minh chu khong dung ValueFromRemainingArguments: cac lenh
# adb co tham so dang `-a`, `-n`, `--es`, `-d`, `-s` va PowerShell se coi chung
# la ten tham so cua chinh ham nay roi bao "A positional parameter cannot be
# found that accepts argument 'shell'".
function Invoke-Adb {
    param([Parameter(Mandatory = $true)][string[]]$AdbArgs)
    $full = @()
    if ($Serial) { $full += @('-s', $Serial) }
    $full += $AdbArgs
    & $adbExe @full
}

function Assert-Device {
    $devices = (Invoke-Adb -AdbArgs @('devices')) -join "`n"
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
    Invoke-Adb -AdbArgs @('install', '-r', '-t', $Apk) | Select-Object -Last 1

    Write-Host "== Cap quyen ==" -ForegroundColor Cyan
    Invoke-Adb -AdbArgs @('shell', 'pm', 'grant', $PKG, 'android.permission.RECORD_AUDIO')
    Invoke-Adb -AdbArgs @('shell', 'pm', 'grant', $PKG, 'android.permission.POST_NOTIFICATIONS')

    Write-Host "== Mo app ==" -ForegroundColor Cyan
    Invoke-Adb -AdbArgs @('shell', 'am', 'start', '-n', $ACTIVITY) | Out-Null
    Start-Sleep -Seconds 8

    # App mac dinh chay tieng Anh (VoiceLanguage.fromStorageKey -> ENGLISH).
    # Khong doi buoc nay thi Vosk nap model-en-us va moi cau tieng Viet deu sai.
    Write-Host "== Chon tieng Viet ==" -ForegroundColor Cyan
    Invoke-Adb -AdbArgs @('shell', 'input', 'tap', $TAP_SETTINGS[0], $TAP_SETTINGS[1])
    Start-Sleep -Seconds 2
    Invoke-Adb -AdbArgs @('shell', 'input', 'tap', $TAP_VIETNAMESE[0], $TAP_VIETNAMESE[1])
    Start-Sleep -Seconds 2

    # Ngon ngu GIAO DIEN, khac voi ngon ngu NHAN DANG o buoc tren.
    #
    # Ca 6 module deu da co values-vi day du, nhung app hien tieng Anh neu locale
    # thiet bi la en-US — va emulator AAOS mac dinh dung en-US. Ket qua la mot
    # ban demo "nua Anh nua Viet": tro ly tra loi tieng Viet trong khi nhan tren
    # man hinh van la Climate / Driver / Fan speed.
    #
    # Dat rieng cho app (Android 13+) thay vi doi locale ca may: khong dung toi
    # phan con lai cua he thong, va khong can reboot.
    Write-Host "== Dat giao dien tieng Viet ==" -ForegroundColor Cyan
    Invoke-Adb -AdbArgs @('shell', 'cmd', 'locale', 'set-app-locales', $PKG, '--user', '10', '--locales', 'vi-VN')
    Invoke-Adb -AdbArgs @('shell', 'am', 'force-stop', $PKG)
    Start-Sleep -Seconds 2
    Invoke-Adb -AdbArgs @('shell', 'am', 'start', '--user', '10', '-n', $ACTIVITY) | Out-Null
    Start-Sleep -Seconds 5

    Write-Host "Xong. Kiem lai bang mat: man hinh phai la 'Dieu hoa / Tai xe / Hanh khach'," -ForegroundColor Green
    Write-Host "va Cai dat -> Ngon ngu giong noi phai tick 'Tieng Viet'." -ForegroundColor Green
}

function Wait-ForTurn {
    <#  Cho den khi xuat hien mot dong VIVA_TRACE_SUMMARY moi.
        Tra ve cac dong trace cua luot do, hoac $null neu qua han.

        Hung ca tag VIVA_BENCH_INJECT: no mang cung trace id va danh dau luot
        nao la cau bom vao. Giu no trong capture de nguoi doc ve sau phan biet
        duoc, dung loc bo. #>
    param([int]$TimeoutSec = 30)

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $lines = Invoke-Adb -AdbArgs @('logcat', '-d', '-s', 'VIVA_TRACE:I', 'VIVA_BENCH_INJECT:I')
        if ($lines -match 'VIVA_TRACE_SUMMARY') { return $lines }
        Start-Sleep -Milliseconds 400
    }
    return $null
}

function Set-VehicleState {
    <# Dat trang thai xe qua cong debug cua bien the mock. Gia tri duoc GHIM,
       vong mo phong khong ghi de nua — xem MockVehicleRepository.injectVehicleEvent. #>
    param([string]$Unit, [string]$Value)

    Invoke-Adb -AdbArgs @(
        'shell', 'am', 'broadcast',
        '-a', 'com.sopa.viva_automotive.mock.VSTATE',
        '--es', 'unit_type', $Unit,
        '--es', 'state_value', $Value,
        '-n', "$PKG/com.sopa.viva_automotive.debug.VehicleDummyReceiver"
    ) | Out-Null
}

function Send-Utterance {
    <# Bom mot cau qua receiver cua bien the mock. Base64 vi dau tieng Viet di
       qua shell Windows roi shell Android rat de hong, va mot cau hong dau se
       bi ghi nhan thanh intent sai — tuc benchmark bao loi cho thu khong loi. #>
    param([string]$Text)

    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Text))
    Invoke-Adb -AdbArgs @(
        'shell', 'am', 'broadcast',
        '-a', 'com.sopa.viva_automotive.mock.UTTERANCE',
        '--es', 'text_b64', $b64,
        '-n', "$PKG/com.sopa.viva_automotive.debug.SimulatedUtteranceReceiver"
    ) | Out-Null
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
if ($Inject) {
    Write-Host " $($rows.Count) cau, che do BOM TEXT — khong can noi." -ForegroundColor Yellow
    Write-Host " Do router -> guard -> skill. KHONG do mic/VAD/ASR/WER." -ForegroundColor Yellow
} else {
    Write-Host " $($rows.Count) cau. Voi moi cau: doc TO va RO khi thay chu 'NOI:'" -ForegroundColor Yellow
    Write-Host " Script tu bam mic va tu hung trace. Ctrl+C de dung." -ForegroundColor Yellow
}
Write-Host "=====================================================" -ForegroundColor Yellow
Write-Host ""

# Reset MOT LAN dau phien, khong phai moi cau.
#
# Vi sao can: DeliverySkill va MockVehicleRepository la @Singleton song trong
# process. Chay het B19 la don A12 da o trang thai DELIVERED, nen lan chay sau
# B18 tra ve "da xac nhan giao truoc do roi" (Allow) thay vi hoi xac nhan —
# ket qua phu thuoc lan chay truoc, dung thu khong duoc phep co trong mot bo
# benchmark.
#
# Vi sao khong `pm clear`: no xoa luon DataStore, tuc mat lua chon tieng Viet.
# `force-stop` du de dung process; state trong bo nho ve mac dinh, cai dat giu nguyen.
#
# Vi sao khong reset moi cau: B06->B07 va B18->B19 la nhung cap CO CHU DICH
# phu thuoc nhau (hoi roi tra loi). Reset giua chung la pha chinh thu dang do.
Write-Host "Reset process mot lan (giu cai dat ngon ngu)..." -ForegroundColor DarkGray
Invoke-Adb -AdbArgs @('shell', 'am', 'force-stop', $PKG)
Start-Sleep -Seconds 2

$done = 0
$missed = 0

foreach ($row in $rows) {
    Invoke-Adb -AdbArgs @('logcat', '-c')
    # Bat buoc ca o che do bom: Android 14 chan khoi dong foreground service tu
    # background, nen app phai dang o tien canh (uidState TOP) thi
    # startForegroundService trong receiver moi duoc phep. Bo dong nay thi moi
    # luot bom deu qua han vi service khong bao gio chay.
    Invoke-Adb -AdbArgs @('shell', 'am', 'start', '-n', $ACTIVITY) | Out-Null
    Start-Sleep -Milliseconds 800

    $pre = $PRECONDITION[$row.id]
    if ($pre) {
        Set-VehicleState -Unit $pre.unit -Value $pre.value
        Start-Sleep -Milliseconds 600
        Write-Host ("[{0}] " -f $row.id) -NoNewline -ForegroundColor DarkGray
        Write-Host ("dat truoc: {0}={1}" -f $pre.unit, $pre.value) -ForegroundColor DarkGray
    }

    Write-Host ("[{0}] " -f $row.id) -NoNewline -ForegroundColor DarkGray
    Write-Host $(if ($Inject) { "BOM: " } else { "NOI: " }) -NoNewline -ForegroundColor Cyan
    Write-Host $row.utterance -ForegroundColor White
    if ($row.notes) { Write-Host ("      ({0})" -f $row.notes) -ForegroundColor DarkGray }

    if ($Inject) {
        Send-Utterance -Text $row.utterance
    } else {
        Invoke-Adb -AdbArgs @('shell', 'input', 'tap', $TAP_MIC[0], $TAP_MIC[1])
    }
    $lines = Wait-ForTurn -TimeoutSec $(if ($Inject) { 20 } else { 30 })

    if (-not $lines) {
        Write-Host "      -> qua han, khong co SUMMARY. Bo qua cau nay." -ForegroundColor Red
        $missed++
        continue
    }

    $lines | Add-Content -Path $capture -Encoding utf8
    # Dinh dang: VIVA_TRACE_SUMMARY|<traceId>|<utterance>|<intent>|<verdict>|e2e_ms=<so>
    # nen sau khi cat marker thi 0=traceId, 1=utterance, 2=intent, 3=verdict.
    $summary = ($lines | Select-String 'VIVA_TRACE_SUMMARY' | Select-Object -Last 1).ToString()
    $fields = ($summary -split 'VIVA_TRACE_SUMMARY\|')[-1] -split '\|'
    $gotIntent = if ($fields.Count -gt 3) { $fields[2] } else { '?' }
    $gotVerdict = if ($fields.Count -gt 4) { $fields[3] } else { '?' }

    $ok = ($gotIntent -eq $row.expect_intent) -and ($gotVerdict -eq $row.expect_verdict)
    $mark = if ($ok) { 'PASS' } else { 'DIFF' }
    $colour = if ($ok) { 'Green' } else { 'Yellow' }
    Write-Host ("      -> {0}  intent={1} verdict={2}  (mong doi: {3} / {4})" -f `
        $mark, $gotIntent, $gotVerdict, $row.expect_intent, $row.expect_verdict) -ForegroundColor $colour
    $done++

    # Cho luot truoc dong han. `startPipeline` bo qua lenh moi khi job cu con
    # song, nen ban khong cho se lam cau ke tiep bi nuot im lang va bao "qua
    # han" — mot loi cua khung chay bi ghi nham thanh loi cua san pham.
    # RESULT_DISPLAY_MS = 3s roi service moi stopSelf.
    Start-Sleep -Milliseconds 4500
}

$modeNote = if ($Inject) {
    @"
che do      : BOM TEXT (khong qua mic)
              Do duoc : intent + verdict, tuc router -> guard -> skill.
              KHONG do : mic, VAD, ASR, WER, do on, do tre dau-cuoi.
              Moi luot co mot dong VIVA_BENCH_INJECT cung trace id trong
              capture. Cac luot nay KHONG co speech_end nen tu dong nam
              ngoai moi thong ke p50/p95 cua harness — dung co go rieng
              de "cuu" chung.
"@
} else {
    @"
che do      : NGUOI NOI THAT vao mic
              Do duoc : ca chang ASR. So lieu do tre moi co nghia o che do nay.
"@
}

@"
emulator voice session
======================
thoi diem   : $(Get-Date -Format o)
commit      : $(git -C "$PSScriptRoot\..\.." rev-parse HEAD)
suite       : $Suite
so cau chay : $done (qua han: $missed)
thiet bi    : $((Invoke-Adb -AdbArgs @('shell', 'getprop', 'ro.build.fingerprint')) -join '')
apk         : $Apk
sha256      : $((Get-FileHash $Apk -Algorithm SHA256).Hash)
$modeNote
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
