# PowerShell script to validate Buddhist citation completions
param(
    [Parameter(Mandatory=$true)]
    [string]$JsonFile
)

function Test-Completion {
    param($completion, $prompt)
    
    if (-not $completion.tags -or -not $completion.values) {
        return @{ Valid = $false; Error = "Missing tags or values" }
    }
    
    $tagCount = $completion.tags.Count
    $valueCount = $completion.values.Count
    
    if ($tagCount -ne $valueCount) {
        return @{ 
            Valid = $false
            Error = "Length mismatch: $tagCount tags vs $valueCount values"
        }
    }
    
    # Check reconstruction
    $reconstructed = $completion.values -join ""
    if ($reconstructed -ne $prompt) {
        return @{
            Valid = $false
            Error = "Reconstruction mismatch: '$reconstructed' != '$prompt'"
        }
    }
    
    return @{ Valid = $true }
}

try {
    $jsonContent = Get-Content $JsonFile -Raw | ConvertFrom-Json
    
    Write-Host "Validating: $JsonFile" -ForegroundColor Cyan
    Write-Host ("=" * 50)
    
    if ($jsonContent -is [array]) {
        foreach ($item in $jsonContent) {
            if ($item.examples) {
                for ($i = 0; $i -lt $item.examples.Count; $i++) {
                    $example = $item.examples[$i]
                    $result = Test-Completion $example.completion $example.prompt
                    
                    $status = if ($result.Valid) { "✅" } else { "❌" }
                    $tagCount = $example.completion.tags.Count
                    $valueCount = $example.completion.values.Count
                    
                    Write-Host "$status Example $($i+1): `"$($example.prompt)`""
                    Write-Host "   Tags: $tagCount, Values: $valueCount"
                    
                    if (-not $result.Valid) {
                        Write-Host "   Error: $($result.Error)" -ForegroundColor Red
                    }
                    Write-Host ""
                }
            }
        }
    }
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
}
