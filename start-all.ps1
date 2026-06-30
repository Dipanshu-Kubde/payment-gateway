# ============================================================
# Payment Gateway - Start All Services Locally
# ============================================================
# Strategy:
#   1. Start infrastructure (MySQL, Redis, Kafka) via Docker
#   2. Run Java services directly with low memory (-Xmx256m)
#   3. Start React dashboard dev server
# ============================================================

param(
    [switch]$SkipInfra,
    [switch]$StopAll
)

$ErrorActionPreference = "Continue"

# Stop everything
if ($StopAll) {
    Write-Host "`n[STOP] Stopping all services..." -ForegroundColor Red
    
    # Kill Java processes
    Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "  Stopping Java process $($_.Id)..." -ForegroundColor Yellow
        Stop-Process -Id $_.Id -Force
    }
    
    # Stop Docker infra
    docker-compose -f docker-compose-infra.yml down 2>$null
    
    # Kill node processes (dashboard)
    Get-Process -Name "node" -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "  Stopping Node process $($_.Id)..." -ForegroundColor Yellow
        Stop-Process -Id $_.Id -Force
    }
    
    Write-Host "[DONE] All services stopped." -ForegroundColor Green
    exit 0
}

Write-Host "`n======================================================" -ForegroundColor Cyan
Write-Host "  Payment Gateway — Starting All Services" -ForegroundColor Cyan  
Write-Host "======================================================`n" -ForegroundColor Cyan

# ============================================
# Step 1: Start Infrastructure via Docker
# ============================================
if (-not $SkipInfra) {
    Write-Host "[1/4] Starting infrastructure (MySQL, Redis, Kafka)..." -ForegroundColor Yellow
    docker-compose -f docker-compose-infra.yml up -d
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to start infrastructure. Is Docker Desktop running?" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Infrastructure starting..." -ForegroundColor Green
    
    # Wait for MySQL to be ready
    Write-Host "  Waiting for MySQL..." -ForegroundColor Gray
    $retries = 30
    while ($retries -gt 0) {
        $result = docker exec pg-mysql mysqladmin ping -h localhost -u root -proot123 2>$null
        if ($LASTEXITCODE -eq 0) { break }
        Start-Sleep -Seconds 2
        $retries--
    }
    if ($retries -eq 0) {
        Write-Host "[WARN] MySQL may not be ready yet, continuing anyway..." -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] MySQL is ready" -ForegroundColor Green
    }
    
    # Wait a bit for Redis & Kafka
    Write-Host "  Waiting for Redis & Kafka..." -ForegroundColor Gray
    Start-Sleep -Seconds 5
    Write-Host "  [OK] Infrastructure ready" -ForegroundColor Green
} else {
    Write-Host "[1/4] Skipping infrastructure (--SkipInfra)" -ForegroundColor Gray
}

# ============================================
# Step 2: Start Eureka Server
# ============================================
Write-Host "`n[2/4] Starting Eureka Server (:8761)..." -ForegroundColor Yellow

$eurekaJob = Start-Process -FilePath "java" `
    -ArgumentList "-Xmx256m", "-jar", "eureka-server\target\eureka-server-1.0.0.jar" `
    -WorkingDirectory "D:\Payment Gateway" `
    -PassThru -WindowStyle Minimized

Write-Host "  [OK] Eureka starting (PID: $($eurekaJob.Id))" -ForegroundColor Green

# Wait for Eureka to be ready
Write-Host "  Waiting for Eureka to start (20s)..." -ForegroundColor Gray
Start-Sleep -Seconds 20

# ============================================
# Step 3: Start All Microservices
# ============================================
Write-Host "`n[3/4] Starting microservices..." -ForegroundColor Yellow

$services = @(
    @{ Name = "API Gateway";         Jar = "api-gateway\target\api-gateway-1.0.0.jar";                     Port = 8080 },
    @{ Name = "Merchant Service";    Jar = "merchant-service\target\merchant-service-1.0.0.jar";           Port = 8081 },
    @{ Name = "Payment Service";     Jar = "payment-service\target\payment-service-1.0.0.jar";             Port = 8082 },
    @{ Name = "Transaction Service"; Jar = "transaction-service\target\transaction-service-1.0.0.jar";     Port = 8083 },
    @{ Name = "Fraud Detection";     Jar = "fraud-detection-service\target\fraud-detection-service-1.0.0.jar"; Port = 8084 },
    @{ Name = "Settlement Service";  Jar = "settlement-service\target\settlement-service-1.0.0.jar";       Port = 8085 },
    @{ Name = "Notification Service"; Jar = "notification-service\target\notification-service-1.0.0.jar";  Port = 8086 }
)

$pids = @()
foreach ($svc in $services) {
    Write-Host "  Starting $($svc.Name) (:$($svc.Port))..." -ForegroundColor Yellow
    
    $kafkaServer = "localhost:9094"  # External Kafka listener
    
    $proc = Start-Process -FilePath "java" `
        -ArgumentList "-Xmx256m",
            "-Dspring.kafka.bootstrap-servers=$kafkaServer",
            "-jar", $svc.Jar `
        -WorkingDirectory "D:\Payment Gateway" `
        -PassThru -WindowStyle Minimized
    
    $pids += $proc.Id
    Write-Host "  [OK] $($svc.Name) starting (PID: $($proc.Id))" -ForegroundColor Green
    
    # Stagger starts to reduce memory spike
    Start-Sleep -Seconds 3
}

# ============================================
# Step 4: Start React Dashboard
# ============================================
Write-Host "`n[4/4] Starting React Dashboard (:5173)..." -ForegroundColor Yellow

$dashProc = Start-Process -FilePath "cmd" `
    -ArgumentList "/c", "cd /d D:\Payment Gateway\dashboard && npm run dev" `
    -PassThru -WindowStyle Minimized

Write-Host "  [OK] Dashboard starting (PID: $($dashProc.Id))" -ForegroundColor Green

# ============================================
# Summary
# ============================================
Write-Host "`n======================================================" -ForegroundColor Cyan
Write-Host "  ALL SERVICES LAUNCHED!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Services will take 30-60 seconds to fully initialize." -ForegroundColor Gray
Write-Host ""
Write-Host "  Eureka Dashboard:  http://localhost:8761" -ForegroundColor White
Write-Host "  API Gateway:       http://localhost:8080" -ForegroundColor White
Write-Host "  Merchant Service:  http://localhost:8081" -ForegroundColor White
Write-Host "  Payment Service:   http://localhost:8082" -ForegroundColor White
Write-Host "  Transaction Svc:   http://localhost:8083" -ForegroundColor White
Write-Host "  Fraud Detection:   http://localhost:8084" -ForegroundColor White
Write-Host "  Settlement Svc:    http://localhost:8085" -ForegroundColor White
Write-Host "  Notification Svc:  http://localhost:8086" -ForegroundColor White
Write-Host "  React Dashboard:   http://localhost:5173" -ForegroundColor Cyan
Write-Host ""
Write-Host "  To stop everything: .\start-all.ps1 -StopAll" -ForegroundColor Yellow
Write-Host "======================================================`n" -ForegroundColor Cyan
