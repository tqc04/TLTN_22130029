# Comprehensive Service Health Check Script
# Kiem tra tat ca services co dang chay va healthy khong

$ErrorActionPreference = 'SilentlyContinue'

Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  CHECKING ALL MICROSERVICES HEALTH" -ForegroundColor Cyan
Write-Host "================================================================`n" -ForegroundColor Cyan

# Service definitions with ports
$serviceList = @(
    @{Name="Discovery Server"; Port=8761; Path="/"; Type="Infrastructure"},
    @{Name="Config Server"; Port=8888; Path="/actuator/health"; Type="Infrastructure"},
    @{Name="Gateway"; Port=8080; Path="/actuator/health"; Type="Infrastructure"},
    @{Name="Auth Service"; Port=8081; Path="/api/auth/health"; Type="Core"},
    @{Name="User Service"; Port=8082; Path="/api/users/health"; Type="Core"},
    @{Name="Product Service"; Port=8083; Path="/actuator/health"; Type="Business"},
    @{Name="Category Service"; Port=8084; Path="/actuator/health"; Type="Business"},
    @{Name="Brand Service"; Port=8085; Path="/actuator/health"; Type="Business"},
    @{Name="Notification Service"; Port=8086; Path="/actuator/health"; Type="Business"},
    @{Name="Cart Service"; Port=8087; Path="/actuator/health"; Type="Business"},
    @{Name="Order Service"; Port=8088; Path="/actuator/health"; Type="Business"},
    @{Name="Payment Service"; Port=8089; Path="/actuator/health"; Type="Business"},
    @{Name="Inventory Service"; Port=8090; Path="/actuator/health"; Type="Business"},
    @{Name="Voucher Service"; Port=8091; Path="/actuator/health"; Type="Business"},
    @{Name="AI Service"; Port=8092; Path="/actuator/health"; Type="Business"},
    @{Name="Admin Service"; Port=8093; Path="/api/admin/health"; Type="Business"},
    @{Name="Recommendation Service"; Port=8094; Path="/actuator/health"; Type="Business"},
    @{Name="Review Service"; Port=8095; Path="/actuator/health"; Type="Business"},
    @{Name="Favorites Service"; Port=8096; Path="/actuator/health"; Type="Business"}
)

$results = @()
$totalServices = $serviceList.Count
$runningServices = 0
$healthyServices = 0

Write-Host "Checking $totalServices services...`n" -ForegroundColor Yellow

foreach ($service in $serviceList) {
    $serviceName = $service.Name
    $port = $service.Port
    $path = $service.Path
    $type = $service.Type
    
    Write-Host "[$type] $serviceName (Port $port)" -NoNewline
    
    # Check if port is listening
    $portListening = $false
    try {
        $tcpConnection = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue -InformationLevel Quiet
        $portListening = $tcpConnection
    } catch {
        $portListening = $false
    }
    
    if (-not $portListening) {
        Write-Host " - NOT RUNNING" -ForegroundColor Red
        $results += @{
            Name = $serviceName
            Port = $port
            Type = $type
            Status = "Not Running"
            Health = "N/A"
            Color = "Red"
        }
        continue
    }
    
    $runningServices++
    
    # Check health endpoint
    $url = "http://localhost:$port$path"
    try {
        $response = Invoke-WebRequest -Uri $url -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            Write-Host " - HEALTHY OK" -ForegroundColor Green
            $healthyServices++
            $results += @{
                Name = $serviceName
                Port = $port
                Type = $type
                Status = "Running"
                Health = "Healthy"
                Color = "Green"
            }
        } else {
            Write-Host " - RUNNING (Status: $($response.StatusCode))" -ForegroundColor Yellow
            $results += @{
                Name = $serviceName
                Port = $port
                Type = $type
                Status = "Running"
                Health = "Status: $($response.StatusCode)"
                Color = "Yellow"
            }
        }
    } catch {
        Write-Host " - RUNNING (Health check failed)" -ForegroundColor Yellow
        $results += @{
            Name = $serviceName
            Port = $port
            Type = $type
            Status = "Running"
            Health = "Check Failed"
            Color = "Yellow"
        }
    }
    
    Start-Sleep -Milliseconds 100
}

# Summary
Write-Host "`n================================================================" -ForegroundColor Cyan
Write-Host "  SUMMARY" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

Write-Host "`nTotal Services:    $totalServices"
Write-Host "Running:           " -NoNewline
if ($runningServices -eq $totalServices) {
    Write-Host "$runningServices / $totalServices" -ForegroundColor Green
} elseif ($runningServices -gt 0) {
    Write-Host "$runningServices / $totalServices" -ForegroundColor Yellow
} else {
    Write-Host "$runningServices / $totalServices" -ForegroundColor Red
}

Write-Host "Healthy:           " -NoNewline
if ($healthyServices -eq $totalServices) {
    Write-Host "$healthyServices / $totalServices" -ForegroundColor Green
} elseif ($healthyServices -gt 0) {
    Write-Host "$healthyServices / $totalServices" -ForegroundColor Yellow
} else {
    Write-Host "$healthyServices / $totalServices" -ForegroundColor Red
}

# Not running services
$notRunning = $results | Where-Object { $_.Status -eq "Not Running" }
if ($notRunning.Count -gt 0) {
    Write-Host "`n================================================================" -ForegroundColor Red
    Write-Host "  SERVICES NOT RUNNING ($($notRunning.Count))" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    
    foreach ($svc in $notRunning) {
        Write-Host "  - $($svc.Name) (Port $($svc.Port))" -ForegroundColor Red
    }
    
    Write-Host "`nTo start missing services:" -ForegroundColor Yellow
    Write-Host "  1. Run .\START.ps1 to start all services" -ForegroundColor Gray
    Write-Host "  2. Or manually start specific services" -ForegroundColor Gray
}

# Final status
Write-Host "`n================================================================" -ForegroundColor Cyan

if ($healthyServices -eq $totalServices) {
    Write-Host "  STATUS: ALL SYSTEMS OPERATIONAL OK" -ForegroundColor Green
} elseif ($runningServices -eq $totalServices -and $healthyServices -gt ($totalServices * 0.8)) {
    Write-Host "  STATUS: MOSTLY OPERATIONAL WARNING" -ForegroundColor Yellow
} elseif ($runningServices -gt 0) {
    Write-Host "  STATUS: PARTIALLY OPERATIONAL WARNING" -ForegroundColor Yellow
} else {
    Write-Host "  STATUS: SYSTEM DOWN ERROR" -ForegroundColor Red
}

Write-Host "================================================================`n" -ForegroundColor Cyan

Write-Host "Press any key to exit..." -ForegroundColor Gray
[void][System.Console]::ReadKey($true)
