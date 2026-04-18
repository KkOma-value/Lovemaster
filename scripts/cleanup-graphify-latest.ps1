param(
    [string]$OutDir = "graphify-out",
    [switch]$IncludeTmpCleanup,
    [string]$TmpDir = "tmp",
    [string]$KeepTmpScript = "graphify_arch_focus_v3.py",
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-AbsolutePath {
    param([string]$Path)

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $Path))
}

function Get-ReportSuffix {
    param([string]$ReportName)

    if ($ReportName -eq "GRAPH_REPORT.md") {
        return ""
    }

    if ($ReportName -match "^GRAPH_REPORT(?<Suffix>.*)\.md$") {
        return $Matches["Suffix"]
    }

    throw "Cannot parse report suffix from file name: $ReportName"
}

function Get-DirSizeBytes {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return 0L
    }

    $sum = (Get-ChildItem -LiteralPath $Path -Recurse -File | Measure-Object -Property Length -Sum).Sum
    if ($null -eq $sum) {
        return 0L
    }
    return [int64]$sum
}

$outDirAbs = Resolve-AbsolutePath -Path $OutDir
if (-not (Test-Path -LiteralPath $outDirAbs)) {
    throw "Output directory not found: $outDirAbs"
}

$beforeBytes = Get-DirSizeBytes -Path $outDirAbs

$reportCandidates = @(Get-ChildItem -LiteralPath $outDirAbs -File -Filter "GRAPH_REPORT*.md" |
    Sort-Object LastWriteTimeUtc -Descending)

if ($reportCandidates.Count -eq 0) {
    throw "No GRAPH_REPORT*.md found in: $outDirAbs"
}

$latestReport = $reportCandidates[0]
$suffix = Get-ReportSuffix -ReportName $latestReport.Name

$expected = @(
    $latestReport.Name,
    ("graph{0}.json" -f $suffix),
    ("graph{0}.html" -f $suffix)
)

$keepSet = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
foreach ($name in $expected) {
    $candidatePath = Join-Path $outDirAbs $name
    if (Test-Path -LiteralPath $candidatePath) {
        [void]$keepSet.Add($name)
    }
}

if ($keepSet.Count -eq 0) {
    throw "No keep targets resolved. Aborting to avoid destructive cleanup."
}

$toRemove = @(Get-ChildItem -LiteralPath $outDirAbs -Force |
    Where-Object { -not $keepSet.Contains($_.Name) })

Write-Host "Target directory : $outDirAbs" -ForegroundColor Cyan
Write-Host "Latest report    : $($latestReport.Name)" -ForegroundColor Cyan
Write-Host "Resolved suffix  : '$suffix'" -ForegroundColor Cyan
Write-Host "Keep files       :" -ForegroundColor Green
$keepSet | Sort-Object | ForEach-Object { Write-Host "  - $_" }

if ($toRemove.Count -gt 0) {
    Write-Host "Remove targets   : $($toRemove.Count)" -ForegroundColor Yellow
    $toRemove | ForEach-Object { Write-Host "  - $($_.Name)" }
} else {
    Write-Host "Remove targets   : 0" -ForegroundColor Yellow
}

if (-not $DryRun) {
    foreach ($item in $toRemove) {
        Remove-Item -LiteralPath $item.FullName -Recurse -Force
    }

    if ($IncludeTmpCleanup) {
        $tmpAbs = Resolve-AbsolutePath -Path $TmpDir
        if (Test-Path -LiteralPath $tmpAbs) {
            $oldTmpScripts = @(Get-ChildItem -LiteralPath $tmpAbs -File -Filter "graphify_*.py" |
                Where-Object { $_.Name -ne $KeepTmpScript })

            foreach ($script in $oldTmpScripts) {
                Remove-Item -LiteralPath $script.FullName -Force
            }

            Write-Host "Tmp cleanup      : removed $($oldTmpScripts.Count) old graphify scripts" -ForegroundColor Yellow
        } else {
            Write-Host "Tmp cleanup      : tmp dir not found ($tmpAbs), skipped" -ForegroundColor Yellow
        }
    }
}

$afterBytes = Get-DirSizeBytes -Path $outDirAbs
$beforeMb = [math]::Round($beforeBytes / 1MB, 2)
$afterMb = [math]::Round($afterBytes / 1MB, 2)

Write-Host "Size before      : $beforeMb MB" -ForegroundColor Magenta
Write-Host "Size after       : $afterMb MB" -ForegroundColor Magenta

if ($DryRun) {
    Write-Host "Mode             : DryRun (no files deleted)" -ForegroundColor Yellow
} else {
    Write-Host "Mode             : Apply (cleanup done)" -ForegroundColor Green
}
