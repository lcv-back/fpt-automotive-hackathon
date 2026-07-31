param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\src\main\res\raw')
)

$ErrorActionPreference = 'Stop'

$moduleRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$outputRoot = [System.IO.Path]::GetFullPath($OutputDirectory)
if (-not $outputRoot.StartsWith($moduleRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Output directory must stay inside $moduleRoot"
}
[System.IO.Directory]::CreateDirectory($outputRoot) | Out-Null

$promptFile = Join-Path $PSScriptRoot 'tts_prompts.tsv'
$prompts = Import-Csv -LiteralPath $promptFile -Delimiter "`t" -Encoding UTF8
if ($prompts.Count -lt 30) {
    throw "Expected at least 30 prompts in $promptFile"
}

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
    throw 'No Vietnamese Windows speech voice was found.'
}

$voice.Voice = $vietnameseVoice
$voice.Rate = 0
$voice.Volume = 100

foreach ($entry in $prompts) {
    if ($entry.raw_name -notmatch '^tts_[a-z0-9_]+$') {
        throw "Invalid Android raw resource name: $($entry.raw_name)"
    }
    $path = Join-Path $outputRoot ($entry.raw_name + '.wav')
    $stream = New-Object -ComObject SAPI.SpFileStream
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
        throw "TTS engine produced no audio for $($entry.raw_name)"
    }
    Write-Output ("{0} ({1} bytes)" -f $file.Name, $file.Length)
}

[void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($voice)
[void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($category)
Write-Output ("Generated {0} Vietnamese fallback clips in {1}" -f $prompts.Count, $outputRoot)
