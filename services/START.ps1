#!/usr/bin/env pwsh
# ONE-CLICK STARTUP with .ENV auto-load
$ErrorActionPreference = 'Continue'
$root = $PSScriptRoot

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  STARTING ALL MICROSERVICES" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Load .env
$envFile = "$root\.env"
if (Test-Path $envFile) {
    Write-Host "[1/3] Loading .env file..." -ForegroundColor Yellow
    $envCount = 0
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^([^=#\s]+)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $val = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $val, 'Process')
            $envCount++
        }
    }
    Write-Host "    Loaded $envCount variables" -ForegroundColor Green
} else {
    Write-Host "[ERROR] .env file not found!" -ForegroundColor Red
    Write-Host "Creating from template..." -ForegroundColor Yellow
    Copy-Item "$root\env.template" $envFile -Force
}

# Ensure JWT_SECRET
if (-not $env:JWT_SECRET) {
    $env:JWT_SECRET = 'MyVeryLongSecretKeyForJWTTokenGeneration256BitsMinimumRequiredForHS256Algorithm'
}
$env:SECURITY_JWT_SECRET = $env:JWT_SECRET

Write-Host "`n[2/3] Starting services..." -ForegroundColor Yellow

# Infrastructure
Write-Host "  Starting infrastructure..." -ForegroundColor Cyan
Start-Process mvn -ArgumentList "spring-boot:run" -WorkingDirectory "$root\discovery-server" -WindowStyle Minimized
Start-Process mvn -ArgumentList "spring-boot:run" -WorkingDirectory "$root\config-server" -WindowStyle Minimized
Write-Host "    discovery-server, config-server" -ForegroundColor Gray
Start-Sleep 10

# Gateway
Write-Host "  Starting gateway..." -ForegroundColor Cyan
Start-Process mvn -ArgumentList "spring-boot:run" -WorkingDirectory "$root\gateway" -WindowStyle Minimized
Write-Host "    gateway" -ForegroundColor Gray
Start-Sleep 5

# Python ML Service (AI Product Recommendation)
Write-Host "  Starting Python ML Service (AI Recommendations)..." -ForegroundColor Cyan
$recoServicePath = "$root\..\reco_service"
if (Test-Path $recoServicePath) {
    # Check if Python is available
    $pythonCmd = $null
    if (Get-Command python -ErrorAction SilentlyContinue) {
        $pythonCmd = "python"
    } elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
        $pythonCmd = "python3"
    }
    
    if ($pythonCmd) {
        # Check if model exists
        $modelPath = "$recoServicePath\recommendation_model.pkl"
        if (Test-Path $modelPath) {
            Write-Host "    [OK] AI Recommendation Service (port 8000)" -ForegroundColor Green
            Write-Host "       - Collaborative Filtering (ML-based)" -ForegroundColor Gray
            Write-Host "       - Content-Based Filtering" -ForegroundColor Gray
            Write-Host "       - Model: Simple CF (trained from real data)" -ForegroundColor Gray
        } else {
            Write-Host "    [WARN] Model not found - using fallback mode" -ForegroundColor Yellow
            Write-Host "       Service will still work with deterministic scoring" -ForegroundColor Gray
        }
        
        $env:PORT = "8000"
        Start-Process $pythonCmd -ArgumentList "app.py" -WorkingDirectory $recoServicePath -WindowStyle Minimized
        Start-Sleep -Seconds 3
    } else {
        Write-Host "    [WARN] Python not found - ML Service will not start" -ForegroundColor Yellow
        Write-Host "       Recommendation system will use Redis-based CF fallback" -ForegroundColor Gray
        Write-Host "       To enable AI: Install Python and run 'pip install -r requirements.txt'" -ForegroundColor Gray
    }
} else {
    Write-Host "    [WARN] reco_service directory not found at: $recoServicePath" -ForegroundColor Yellow
    Write-Host "       Recommendation system will use fallback methods" -ForegroundColor Gray
}

# Business services
Write-Host "  Starting business services..." -ForegroundColor Cyan
$services = @(
    "auth-service", "user-service", "product-service", "category-service",
    "brand-service", "cart-service", "order-service", "payment-service",
    "inventory-service", "voucher-service", "ai-service", "admin-service",
    "notification-service", "recommendation-service", "review-service", "favorites-service", "warranty-service"
)

foreach ($svc in $services) {
    $svcPath = "$root\$svc"
    if (Test-Path $svcPath) {
        Write-Host "    $svc" -ForegroundColor Gray
        Start-Process mvn -ArgumentList "spring-boot:run" -WorkingDirectory $svcPath -WindowStyle Minimized
        Start-Sleep -Milliseconds 300
    }
}

Write-Host "`n[3/3] Complete!" -ForegroundColor Green
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  ALL SERVICES STARTED!" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

Write-Host "Services Overview:" -ForegroundColor Cyan
Write-Host "  - Infrastructure: Config Server, Discovery, Gateway" -ForegroundColor Green
Write-Host "  - Python ML Service: AI Product Recommendations (port 8000)" -ForegroundColor Green
Write-Host "  - Business Services: 17 microservices" -ForegroundColor Green
Write-Host ""
Write-Host "Services are starting (wait 30-60 seconds)..." -ForegroundColor Yellow
Write-Host ""
Write-Host "Quick Commands:" -ForegroundColor Cyan
Write-Host "  - Check all services: .\check-health.ps1" -ForegroundColor White
Write-Host "  - Test AI Recommendations: .\test-ai-recommendation.bat" -ForegroundColor White
Write-Host "  - Stop all services: .\STOP.bat" -ForegroundColor White
Write-Host ""
Write-Host "Access Points:" -ForegroundColor Cyan
Write-Host "  - Gateway: http://localhost:8080" -ForegroundColor White
Write-Host "  - Discovery: http://localhost:8761" -ForegroundColor White
Write-Host "  - AI Service: http://localhost:8000/health" -ForegroundColor White
Write-Host ""
