#!/usr/bin/env pwsh
# Build every Maven module/service sequentially with status summary

param(
    [switch]$SkipTests,
    [switch]$StopOnError
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  RUNNING mvn clean install FOR ALL SERVICES" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

function Invoke-ServiceBuild {
    param(
        [string]$Name,
        [string]$RelativePath
    )

    $fullPath = Join-Path $root $RelativePath

    if (-not (Test-Path $fullPath)) {
        Write-Host "  [$Name] SKIPPED - path not found ($RelativePath)" -ForegroundColor Yellow
        return @{
            Name = $Name
            Path = $RelativePath
            Status = 'Skipped'
            Duration = [TimeSpan]::Zero
        }
    }

    Write-Host ""
    Write-Host "  Building $Name ..." -ForegroundColor Yellow
    Write-Host "    Path: $RelativePath" -ForegroundColor DarkGray

    $arguments = @('clean', 'install')
    if ($SkipTests) {
        $arguments += '-DskipTests'
    }

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

    Push-Location $fullPath
    & mvn @arguments
    $exitCode = $LASTEXITCODE
    Pop-Location

    $stopwatch.Stop()

    if ($exitCode -eq 0) {
        Write-Host "  [$Name] SUCCESS ($($stopwatch.Elapsed.ToString()))" -ForegroundColor Green
        return @{
            Name = $Name
            Path = $RelativePath
            Status = 'Success'
            Duration = $stopwatch.Elapsed
        }
    } else {
        Write-Host "  [$Name] FAILED (exit code $exitCode)" -ForegroundColor Red
        return @{
            Name = $Name
            Path = $RelativePath
            Status = "Failed (code $exitCode)"
            Duration = $stopwatch.Elapsed
        }
    }
}

$modules = @(
    @{ Name = 'shared'; Path = 'shared' },
    @{ Name = 'discovery-server'; Path = 'discovery-server' },
    @{ Name = 'config-server'; Path = 'config-server' },
    @{ Name = 'gateway'; Path = 'gateway' },
    @{ Name = 'auth-service'; Path = 'auth-service' },
    @{ Name = 'user-service'; Path = 'user-service' },
    @{ Name = 'product-service'; Path = 'product-service' },
    @{ Name = 'category-service'; Path = 'category-service' },
    @{ Name = 'brand-service'; Path = 'brand-service' },
    @{ Name = 'cart-service'; Path = 'cart-service' },
    @{ Name = 'order-service'; Path = 'order-service' },
    @{ Name = 'payment-service'; Path = 'payment-service' },
    @{ Name = 'voucher-service'; Path = 'voucher-service' },
    @{ Name = 'inventory-service'; Path = 'inventory-service' },
    @{ Name = 'notification-service'; Path = 'notification-service' },
    @{ Name = 'ai-service'; Path = 'ai-service' },
    @{ Name = 'admin-service'; Path = 'admin-service' },
    @{ Name = 'recommendation-service'; Path = 'recommendation-service' },
    @{ Name = 'review-service'; Path = 'review-service' },
    @{ Name = 'favorites-service'; Path = 'favorites-service' },
    @{ Name = 'warranty-service'; Path = 'warranty-service' }
)

$results = @()

foreach ($module in $modules) {
    $result = Invoke-ServiceBuild -Name $module.Name -RelativePath $module.Path
    $results += $result

    if ($result.Status -like 'Failed*' -and $StopOnError) {
        Write-Host ""
        Write-Host "Stopping because -StopOnError was specified." -ForegroundColor Red
        break
    }
}

$total = $results.Count
$successful = ($results | Where-Object { $_.Status -eq 'Success' }).Count
$failed = ($results | Where-Object { $_.Status -like 'Failed*' }).Count
$skipped = ($results | Where-Object { $_.Status -eq 'Skipped' }).Count

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  BUILD SUMMARY" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Total modules : $total"
Write-Host "  Success       : $successful" -ForegroundColor Green

if ($failed -gt 0) {
    Write-Host "  Failed        : $failed" -ForegroundColor Red
} else {
    Write-Host "  Failed        : $failed"
}

if ($skipped -gt 0) {
    Write-Host "  Skipped       : $skipped" -ForegroundColor Yellow
}

if ($failed -gt 0) {
    Write-Host ""
    Write-Host "FAILED MODULES:" -ForegroundColor Red
    $results | Where-Object { $_.Status -like 'Failed*' } | ForEach-Object {
        Write-Host "  - $($_.Name): $($_.Status)"
    }
}

Write-Host ""
Write-Host "Done. Review logs above for details." -ForegroundColor Cyan

