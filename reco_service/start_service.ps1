# PowerShell script to start the Recommendation Service
# Run this in PowerShell: .\start_service.ps1

Write-Host "=" -ForegroundColor Cyan -NoNewline; Write-Host "=" * 59
Write-Host "🚀 Starting Recommendation Service" -ForegroundColor Green
Write-Host "=" -ForegroundColor Cyan -NoNewline; Write-Host "=" * 59

# Check if Python is installed
Write-Host "`n📌 Checking Python installation..." -ForegroundColor Yellow
$pythonCmd = $null
if (Get-Command python -ErrorAction SilentlyContinue) {
    $pythonCmd = "python"
} elseif (Get-Command python3 -ErrorAction SilentlyContinue) {
    $pythonCmd = "python3"
} else {
    Write-Host "❌ Python not found! Please install Python 3.8+" -ForegroundColor Red
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

# Check if model files exist
Write-Host "`n📌 Checking model files..." -ForegroundColor Yellow
$modelPath = "..\project\project\src\main\resources\recommendation_model.pkl"
$metadataPath = "..\project\project\src\main\resources\model_metadata.json"

if (Test-Path $modelPath) {
    $modelSize = (Get-Item $modelPath).Length / 1MB
    Write-Host "✅ Model found: $([math]::Round($modelSize, 2)) MB" -ForegroundColor Green
} else {
    Write-Host "⚠️  Model not found at: $modelPath" -ForegroundColor Yellow
    Write-Host "   Service will run with fallback mode" -ForegroundColor Yellow
}

if (Test-Path $metadataPath) {
    Write-Host "✅ Metadata found" -ForegroundColor Green
} else {
    Write-Host "⚠️  Metadata not found at: $metadataPath" -ForegroundColor Yellow
}

# Check if dependencies are installed
Write-Host "`n📌 Checking dependencies..." -ForegroundColor Yellow
$packagesNeeded = @("fastapi", "uvicorn", "joblib", "surprise")
$missingPackages = @()

foreach ($package in $packagesNeeded) {
    $installed = & $pythonCmd -m pip show $package 2>$null
    if ($installed) {
        Write-Host "✅ $package installed" -ForegroundColor Green
    } else {
        Write-Host "❌ $package not installed" -ForegroundColor Red
        $missingPackages += $package
    }
}

if ($missingPackages.Count -gt 0) {
    Write-Host "`n⚠️  Missing packages detected!" -ForegroundColor Yellow
    Write-Host "Install them with:" -ForegroundColor Yellow
    Write-Host "   pip install -r requirements.txt" -ForegroundColor Cyan
    
    $response = Read-Host "`nDo you want to install now? (y/N)"
    if ($response -eq "y" -or $response -eq "Y") {
        Write-Host "`n📦 Installing dependencies..." -ForegroundColor Yellow
        & $pythonCmd -m pip install -r requirements.txt
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Dependencies installed successfully!" -ForegroundColor Green
        } else {
            Write-Host "❌ Failed to install dependencies" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "❌ Cannot start without required packages" -ForegroundColor Red
        exit 1
    }
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
Write-Host "   - Run test_service.py to verify it works" -ForegroundColor Gray
Write-Host "   - Check logs below for any errors" -ForegroundColor Gray

Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
Write-Host ""

# Set PORT if needed
$env:PORT = "8000"

# Start the service
& $pythonCmd app.py

