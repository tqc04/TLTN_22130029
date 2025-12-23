# Check health of all services only (no start)
param(
    [int]$WaitSeconds = 3,
    [int]$MaxRetries = 10
)

function Test-Health {
    param(
        [string]$Url
    )
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) { return "OK" }
        return "ERR:$($resp.StatusCode)"
    } catch { return "DOWN" }
}

$services = @(
    @{ name = "discovery";              health = "http://localhost:8761" },
    @{ name = "config";                 health = "http://localhost:8888/actuator/health" },
    @{ name = "gateway";                health = "http://localhost:8080/actuator/health" },
    @{ name = "python-ml-service (AI)"; health = "http://localhost:8000/health" },
    @{ name = "auth-service";           health = "http://localhost:8081/actuator/health" },
    @{ name = "user-service";           health = "http://localhost:8082/actuator/health" },
    @{ name = "product-service";        health = "http://localhost:8083/actuator/health" },
    @{ name = "order-service";          health = "http://localhost:8084/actuator/health" },
    @{ name = "payment-service";        health = "http://localhost:8085/actuator/health" },
    @{ name = "ai-service";             health = "http://localhost:8088/actuator/health" },
    @{ name = "admin-service";          health = "http://localhost:8087/actuator/health" },
    @{ name = "notification-service";   health = "http://localhost:8086/actuator/health" },
    @{ name = "category-service";       health = "http://localhost:8089/actuator/health" },
    @{ name = "brand-service";          health = "http://localhost:8090/actuator/health" },
    @{ name = "cart-service";           health = "http://localhost:8091/actuator/health" },
    @{ name = "voucher-service";        health = "http://localhost:8092/actuator/health" },
    @{ name = "inventory-service";      health = "http://localhost:8093/actuator/health" },
    @{ name = "recommendation-service"; health = "http://localhost:8094/actuator/health" }
)

for ($i = 0; $i -lt $MaxRetries; $i++) {
    $ok = 0
    Write-Host "Attempt $($i+1)/$MaxRetries" -ForegroundColor Yellow
    foreach ($s in $services) {
        $st = Test-Health -Url $s.health
        $color = if ($st -eq 'OK') { 'Green' } elseif ($st -eq 'DOWN') { 'Red' } else { 'Yellow' }
        if ($st -eq 'OK') { $ok++ }
        Write-Host ($s.name.PadRight(25) + " : " + $st) -ForegroundColor $color
    }
    Write-Host ("Summary: {0}/{1} services OK" -f $ok, $services.Count) -ForegroundColor Cyan
    if ($ok -eq $services.Count) { break }
    Start-Sleep -Seconds $WaitSeconds
    Write-Host ""  # blank line
}
