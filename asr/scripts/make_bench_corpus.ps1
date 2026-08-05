<#
.SYNOPSIS
    Render the 20 V12 benchmark utterances to 16 kHz mono WAV using a Vietnamese
    Windows voice.

.DESCRIPTION
    `backend/suites/benchmark_v1.csv` holds the 22 benchmark rows, of which 20 are
    distinct utterances (B09/B10 and B18/B19 repeat a sentence to test a second
    turn, not a second sound). Those 20 are what `15-QUYET-DINH-BENCHMARK-ASR.md`
    means by "20 utterance x 3 muc nhieu".

    Until now the ASR benchmark ran on `res/raw/tts_*.wav` instead — but those are
    the assistant's REPLIES ("Da dat quat muc 0"), not the driver's COMMANDS
    ("quat muc 2"). They were the only Vietnamese audio in the repo, so they were
    a reasonable stand-in for a smoke test and are the wrong corpus for a headline
    number: nothing about how well VIVA transcribes its own voice tells us how well
    it transcribes a driver.

    Two differences from generate_tts_assets.ps1, both deliberate:

      * Output is 16 kHz mono at the source (SAFT16kHz16BitMono = 18). The app's
        fallback clips are 22.05 kHz and every benchmark so far resampled them by
        linear interpolation, which the bench script itself flags as "cheaper and
        worse than a proper filter". Asking SAPI for 16 kHz removes that step —
        and with it a limitation the team would otherwise have to keep declaring.
      * Files land outside res/raw. These are test fixtures, not app assets; they
        must not ship inside the APK.

.NOTES
    Needs a Vietnamese voice ("Microsoft An" / any voice whose description matches
    Vietnamese). A machine with only English voices CANNOT produce this corpus —
    it will fail loudly here rather than emit English-accented audio that would
    quietly poison every WER number downstream.

.EXAMPLE
    pwsh asr/scripts/make_bench_corpus.ps1
    py  asr/scripts/noise_mix.py --source ../evidence/asr/corpus-src --pattern "cmd_*.wav" `
        --out-dir ../evidence/asr/corpus-v12
    py  asr/scripts/bench_noise_levels.py --corpus ../evidence/asr/corpus-v12 `
        --prompts ./scripts/bench_utterances.tsv --out-dir ../evidence/asr/v12
#>
param(
    [string]$PromptFile = (Join-Path $PSScriptRoot 'bench_utterances.tsv'),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\..\evidence\asr\corpus-src')
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $PromptFile)) {
    throw "Khong thay $PromptFile"
}
$prompts = Import-Csv -LiteralPath $PromptFile -Delimiter "`t" -Encoding UTF8
if ($prompts.Count -lt 20) {
    throw "Can it nhat 20 cau trong $PromptFile, dang co $($prompts.Count)"
}

$outputRoot = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($outputRoot) | Out-Null

$voice = New-Object -ComObject SAPI.SpVoice
$category = New-Object -ComObject SAPI.SpObjectTokenCategory
$category.SetId('HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech_OneCore\Voices', $false)
$tokens = $category.EnumerateTokens()
$vietnameseVoice = $null
for ($index = 0; $index -lt $tokens.Count; $index++) {
    $candidate = $tokens.Item($index)
    if ($candidate.GetDescription() -match 'Vietnamese|Microsoft An') {
        $vietnameseVoice = $candidate
        break
    }
}
if ($null -eq $vietnameseVoice) {
    $available = @()
    for ($index = 0; $index -lt $tokens.Count; $index++) {
        $available += $tokens.Item($index).GetDescription()
    }
    throw ("May nay khong co giong tieng Viet. Dang co: {0}. " -f ($available -join ', ')) +
          "Cai goi ngon ngu Tieng Viet (Settings > Time & language > Language) roi chay lai, " +
          "hoac chay script nay tren may da render duoc res/raw/tts_*.wav."
}

$voice.Voice = $vietnameseVoice
$voice.Rate = 0
$voice.Volume = 100

# SpAudioFormat 18 = SAFT16kHz16BitMono — the rate viva-asr accepts, so no
# resample stands between the microphone-equivalent signal and the model.
$format = New-Object -ComObject SAPI.SpAudioFormat
$format.Type = 18

foreach ($entry in $prompts) {
    if ($entry.raw_name -notmatch '^cmd_[a-z0-9_]+$') {
        throw "Ten file khong hop le: $($entry.raw_name) (can dang cmd_<...>)"
    }
    $path = Join-Path $outputRoot ($entry.raw_name + '.wav')
    $stream = New-Object -ComObject SAPI.SpFileStream
    $stream.Format = $format
    try {
        # 3 = SSFMCreateForWrite.
        $stream.Open($path, 3, $false)
        $voice.AudioOutputStream = $stream
        [void]$voice.Speak($entry.text_vi)
    }
    finally {
        $stream.Close()
        [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($stream)
    }
    $file = Get-Item -LiteralPath $path
    if ($file.Length -le 46) {
        throw "TTS khong sinh duoc am thanh cho $($entry.raw_name)"
    }
    Write-Output ("{0,-28} {1,7} bytes  <- {2}" -f $file.Name, $file.Length, $entry.text_vi)
}

[void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($voice)
[void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($category)
Write-Output ""
Write-Output ("Da sinh {0} clip lenh 16 kHz mono trong {1}" -f $prompts.Count, $outputRoot)
Write-Output "Giong doc: $($vietnameseVoice.GetDescription())"
Write-Output ""
Write-Output "Day van la giong TTS TONG HOP, khong phai giong nguoi trong cabin."
Write-Output "No sua duoc loi 'sai loai cau' (tra loi -> lenh), KHONG sua duoc loi 'khong phai nguoi that'."
