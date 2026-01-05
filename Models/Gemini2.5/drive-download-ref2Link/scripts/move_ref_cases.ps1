<#
Usage examples:
  # Move files (default)
  powershell -ExecutionPolicy Bypass -File Models\Gemini2.5\drive-download-ref2Link\scripts\move_ref_cases.ps1

  # Dry run (no changes)
  powershell -ExecutionPolicy Bypass -File Models\Gemini2.5\drive-download-ref2Link\scripts\move_ref_cases.ps1 -DryRun

  # Copy instead of move
  powershell -ExecutionPolicy Bypass -File Models\Gemini2.5\drive-download-ref2Link\scripts\move_ref_cases.ps1 -Copy

  # Copy + dry run
  powershell -ExecutionPolicy Bypass -File Models\Gemini2.5\drive-download-ref2Link\scripts\move_ref_cases.ps1 -Copy -DryRun
#>
﻿param(
  [string]$RefCasesPath = "d:\project\OCR2Markup\Models\Gemini2.5\dila-ai-markup-plugin\xml-w-link\ref-cases.json",
  [string]$SourceRoot = "G:\Shared drives\CBETA參照偵測\Journal-Data\日文期刊\JIBS 印度學佛教學研究\work\xml-顯知\第二次校對",
  [string]$DestinationRoot = "d:\project\OCR2Markup\Models\Gemini2.5\drive-download-ref2Link\Checked",
  [switch]$Copy,
  [switch]$DryRun
)

if (-not (Test-Path -LiteralPath $RefCasesPath)) {
  throw "ref-cases.json not found: $RefCasesPath"
}
if (-not (Test-Path -LiteralPath $SourceRoot)) {
  throw "Source root not found: $SourceRoot"
}

$data = Get-Content -LiteralPath $RefCasesPath -Raw -Encoding UTF8 | ConvertFrom-Json

$pairs = New-Object System.Collections.Generic.HashSet[string]
foreach ($prop in $data.PSObject.Properties) {
  $entries = $prop.Value
  if ($null -eq $entries) { continue }
  foreach ($entry in $entries) {
    $file = $entry.file
    if ([string]::IsNullOrWhiteSpace($file)) { continue }
    $location = $entry.location
    $pairs.Add("$location|$file") | Out-Null
  }
}

function Resolve-LocationDir([string]$root, [string]$location) {
  if ([string]::IsNullOrWhiteSpace($location)) {
    return $root
  }
  $exact = Join-Path $root $location
  if (Test-Path -LiteralPath $exact) { return $exact }

  $matches = Get-ChildItem -LiteralPath $root -Directory | Where-Object {
    $_.Name.EndsWith("_$location") -or $_.Name.EndsWith($location)
  }
  if ($matches) {
    return ($matches | Sort-Object LastWriteTime | Select-Object -Last 1).FullName
  }
  return $null
}

$copied = 0
$missing = @()

foreach ($pair in ($pairs | Sort-Object)) {
  $parts = $pair -split '\|', 2
  $location = $parts[0]
  $file = $parts[1]

  $srcDir = Resolve-LocationDir -root $SourceRoot -location $location
  if ($null -eq $srcDir) {
    $missing += (Join-Path (Join-Path $SourceRoot $location) $file)
    continue
  }

  $srcPath = Join-Path $srcDir $file
  $destDir = if ([string]::IsNullOrWhiteSpace($location)) {
    $DestinationRoot
  } else {
    Join-Path $DestinationRoot $location
  }
  New-Item -ItemType Directory -Path $destDir -Force | Out-Null

  if (Test-Path -LiteralPath $srcPath) {
    if ($DryRun) {
      $action = if ($Copy) { "copy" } else { "move" }
      Write-Output "Would $action: $srcPath -> $destDir"
    } else {
      if ($Copy) {
        Copy-Item -LiteralPath $srcPath -Destination $destDir -Force
      } else {
        Move-Item -LiteralPath $srcPath -Destination $destDir -Force
      }
    }
    $copied++
  } else {
    $missing += $srcPath
  }
}

Write-Output "Total unique files: $($pairs.Count)"
Write-Output "Moved: $copied"
Write-Output "Missing: $($missing.Count)"
if ($missing.Count -gt 0) {
  Write-Output "Missing files:"
  $missing | ForEach-Object { Write-Output $_ }
}
