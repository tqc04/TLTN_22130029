# PowerShell script to run Python ML Recommendation Service
# Run this in PowerShell: .\RUN.ps1

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  STARTING PYTHON ML SERVICE" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Check if Python is installed
Write-Host "[1/3] Checking Python installation..." -ForegroundColor Yellow
$pythonCmd = $null
if (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonCmd = "python"
} elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
    $pythonCmd = "python3"
} else {
    Write-Host "❌ Python not found!" -ForegroundColor Red
    Write-Host "Please install Python 3.8+ from https://www.python.org/" -ForegroundColor Yellow
    exit 1
}

$pythonVersion = & $pythonCmd --version
Write-Host "✅ Found: $pythonVersion" -ForegroundColor Green

# Check if we're in the right directory
if (-not (Test-Path "app.py")) {
    Write-Host "`n❌ app.py not found!" -ForegroundColor Red
    Write-Host "Please run this script from the reco_service directory" -ForegroundColor Yellow
    exit 1
}

# Check dependencies
Write-Host "`n[2/3] Checking dependencies..." -ForegroundColor Yellow
$packagesNeeded = @("fastapi", "uvicorn", "joblib", "surprise", "requests", "pydantic")
$missingPackages = @()

foreach ($package in $packagesNeeded) {
    $installed = & $pythonCmd -m pip show $package 2>$null
    if ($installed) {
        Write-Host "  ✅ $package installed" -ForegroundColor Green
    } else {
        Write-Host "  ❌ $package not installed" -ForegroundColor Red
        $missingPackages += $package
    }
}

if ($missingPackages.Count -gt 0) {
    Write-Host "`n⚠️  Missing packages detected!" -ForegroundColor Yellow
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    & $pythonCmd -m pip install -r requirements.txt
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Failed to install dependencies" -ForegroundColor Red
        exit 1
    }
    Write-Host "✅ Dependencies installed successfully!" -ForegroundColor Green
}

# Check model files (optional)
Write-Host "`n[3/3] Checking model files..." -ForegroundColor Yellow
$modelPath = "..\recommendation_model.pkl"
if (Test-Path $modelPath) {
    $modelSize = (Get-Item $modelPath).Length / 1MB
    Write-Host "✅ Model found: $([math]::Round($modelSize, 2)) MB" -ForegroundColor Green
} else {
    Write-Host "⚠️  Model not found (service will use fallback mode)" -ForegroundColor Yellow
}

# Start the service
Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
Write-Host "🚀 STARTING SERVICE..." -ForegroundColor Green
Write-Host ("=" * 60) -ForegroundColor Cyan

Write-Host "`n📡 Service will be available at:" -ForegroundColor Yellow
Write-Host "   http://localhost:8000" -ForegroundColor Cyan
Write-Host "   http://localhost:8000/health" -ForegroundColor Cyan
Write-Host "   http://localhost:8000/docs" -ForegroundColor Cyan

Write-Host "`n💡 Tips:" -ForegroundColor Yellow
Write-Host "   - Press Ctrl+C to stop the service" -ForegroundColor Gray
Write-Host "   - Check http://localhost:8000/health to verify it's running" -ForegroundColor Gray
Write-Host "   - API documentation: http://localhost:8000/docs" -ForegroundColor Gray

Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
Write-Host ""

# Set PORT environment variable
$env:PORT = "8000"

# Start the service
& $pythonCmd app.py

