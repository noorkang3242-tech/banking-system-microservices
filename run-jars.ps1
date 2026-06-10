# run-jars.ps1 - LEAN production-style runner for low-RAM machines.
# Runs each service as ONE `java -jar` JVM with a capped heap (no Maven overhead),
# started SEQUENTIALLY (waits for each port) so an 8 GB box shared with
# MySQL/Oracle/IDE does not spike and starve Eureka.
#
# Build jars first:  .\build-jars.ps1
# Override heap:     $env:SERVICE_MAX_HEAP='512m'; .\run-jars.ps1
# Secrets: set via environment (see .env.example); local-dev fallbacks used otherwise.

$ErrorActionPreference = 'Stop'
$root = "C:\Users\Lenovo\Desktop\cash\banking-system-microservices"
$java = "C:\Program Files\Java\jdk-17\bin\java.exe"
$heap = if ($env:SERVICE_MAX_HEAP) { $env:SERVICE_MAX_HEAP } else { '256m' }

# Resolve secrets (environment first, local-dev fallback otherwise).
if (-not $env:DB_USERNAME)            { $env:DB_USERNAME = 'root' }
if (-not $env:DB_PASSWORD)            { $env:DB_PASSWORD = 'your_mysql_password' }
if (-not $env:JWT_SECRET)             { $env:JWT_SECRET = 'ChangeThisSecretToA64CharacterLongRandomStringForProductionUse12345' }
if (-not $env:INTERNAL_SHARED_SECRET) { $env:INTERNAL_SHARED_SECRET = 'local-dev-internal-secret-change-me' }
if (-not $env:ADMIN_EMAIL)            { $env:ADMIN_EMAIL = 'admin@bank.local' }
if (-not $env:ADMIN_PASSWORD)         { $env:ADMIN_PASSWORD = 'Admin@12345' }

# Child java processes inherit these environment variables from this shell.
function Start-Jar($folder, $port, $waitSec) {
    $jar = Get-ChildItem "$root\$folder\target\*-0.0.1-SNAPSHOT.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) { Write-Host ("  !! {0}: jar not found (run build-jars.ps1)" -f $folder) -ForegroundColor Red; return }
    Start-Process -FilePath $java -ArgumentList "-Xmx$heap", "-jar", $jar.FullName -WindowStyle Minimized
    $dl = (Get-Date).AddSeconds($waitSec)
    do {
        Start-Sleep -Seconds 3
        $up = [bool](Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue)
    } while (-not $up -and (Get-Date) -lt $dl)
    if ($up) { Write-Host ("  OK  {0} on {1}" -f $folder, $port) -ForegroundColor Green }
    else     { Write-Host ("  !!  {0} did not bind {1} in {2}s" -f $folder, $port, $waitSec) -ForegroundColor Red }
}

Write-Host "Starting services as lean jars (heap $heap each)..." -ForegroundColor Cyan

Start-Jar "discovery-server"     8761 60
Start-Jar "auth-service"         8081 90   # uses JWT_SECRET + ADMIN_EMAIL/PASSWORD (seeds admin)
Start-Jar "customer-service"     8082 90
Start-Jar "account-service"      8083 90
Start-Jar "transaction-service"  8084 90
Start-Jar "transfer-service"     8085 90
Start-Jar "loan-service"         8086 90
Start-Jar "card-service"         8087 90
Start-Jar "notification-service" 8088 90

$env:SERVER_PORT = '8090'   # only the gateway uses this
Start-Jar "api-gateway"          8090 90

Write-Host ("`nDone. Admin: {0}  Gateway: http://localhost:8090" -f $env:ADMIN_EMAIL) -ForegroundColor Green
