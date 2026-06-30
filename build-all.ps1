$env:MAVEN_OPTS = "-Xmx512m"
$ErrorActionPreference = "Stop"

$services = @(
    "common-lib",
    "eureka-server",
    "api-gateway",
    "merchant-service",
    "payment-service",
    "transaction-service",
    "fraud-detection-service",
    "settlement-service",
    "notification-service"
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Payment Gateway - Build All Services" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Install parent POM first
Write-Host "[0/9] Installing parent POM..." -ForegroundColor Yellow
mvn install -N -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: parent POM" -ForegroundColor Red; exit 1 }
Write-Host "[OK] Parent POM installed" -ForegroundColor Green

# Build common-lib first (dependency for all services)
Write-Host "`n[1/9] Building common-lib..." -ForegroundColor Yellow
mvn clean install -DskipTests -pl common-lib -q
if ($LASTEXITCODE -ne 0) { Write-Host "FAILED: common-lib" -ForegroundColor Red; exit 1 }
Write-Host "[OK] common-lib installed" -ForegroundColor Green

# Build remaining services
$i = 2
foreach ($svc in $services | Select-Object -Skip 1) {
    Write-Host "`n[$i/9] Building $svc..." -ForegroundColor Yellow
    mvn clean package -DskipTests -pl $svc -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED: $svc" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] $svc packaged" -ForegroundColor Green
    $i++
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  ALL 9 MODULES BUILT SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

# Verify JARs exist
Write-Host "Verifying JAR files..." -ForegroundColor Yellow
foreach ($svc in $services | Select-Object -Skip 0) {
    $jar = Get-ChildItem -Path "$svc\target\*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jar) {
        $size = [math]::Round($jar.Length / 1MB, 1)
        Write-Host "  [OK] $svc -> $($jar.Name) (${size}MB)" -ForegroundColor Green
    } else {
        Write-Host "  [MISSING] $svc - no JAR found!" -ForegroundColor Red
    }
}

Write-Host "`nReady for: docker-compose up --build" -ForegroundColor Cyan
