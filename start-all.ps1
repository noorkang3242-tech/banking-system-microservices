# start-all.ps1 — Banking System ke saare services sahi order + env ke saath start karta hai.
# Chalane ka tareeka (PowerShell):  .\start-all.ps1
# Har service apni alag window mein khulegi. Band karne ke liye us window ko close karein.
#
# ─────────────────────────────────────────────────────────────────────────────
#  SECRETS / CONFIG
#  Production mein in sab ko ENVIRONMENT VARIABLES (ya ek secrets manager) se set
#  karein — yahan diye gaye fallbacks SIRF local dev ke liye hain. Real secrets ko
#  source control mein commit NA karein. Reference ke liye .env.example dekhein.
# ─────────────────────────────────────────────────────────────────────────────

$ErrorActionPreference = 'Stop'

$mvn      = "E:\tools\apache-maven-3.9.9\bin\mvn.cmd"
$root     = "C:\Users\Lenovo\Desktop\cash\banking-system-microservices"
$javaHome = "C:\Program Files\Java\jdk-17"

# Har value pehle environment se uthti hai; warna local-dev fallback use hota hai.
$dbUser         = if ($env:DB_USERNAME)           { $env:DB_USERNAME }           else { 'root' }
$dbPass         = if ($env:DB_PASSWORD)           { $env:DB_PASSWORD }           else { 'your_mysql_password' }
$jwtSecret      = if ($env:JWT_SECRET)            { $env:JWT_SECRET }            else { 'ChangeThisSecretToA64CharacterLongRandomStringForProductionUse12345' }
$internalSecret = if ($env:INTERNAL_SHARED_SECRET){ $env:INTERNAL_SHARED_SECRET }else { 'local-dev-internal-secret-change-me' }
$adminEmail     = if ($env:ADMIN_EMAIL)           { $env:ADMIN_EMAIL }           else { 'admin@bank.local' }
$adminPass      = if ($env:ADMIN_PASSWORD)        { $env:ADMIN_PASSWORD }        else { 'Admin@12345' }

# env fragments jo har service window mein inject hote hain
$internal   = "`$env:INTERNAL_SHARED_SECRET='$internalSecret';"
$db         = "`$env:DB_USERNAME='$dbUser'; `$env:DB_PASSWORD='$dbPass'; $internal"
$authEnv    = "$db `$env:JWT_SECRET='$jwtSecret'; `$env:ADMIN_EMAIL='$adminEmail'; `$env:ADMIN_PASSWORD='$adminPass';"
$gatewayEnv = "`$env:SERVER_PORT='8090'; `$env:JWT_SECRET='$jwtSecret'; $internal"

# This machine has 8 GB RAM shared with MySQL/Oracle/IDE. Running each service as a
# separate Maven + forked app JVM (and uncapped heaps) exhausts RAM and makes Eureka
# unresponsive. So we run the app IN the Maven JVM (fork=false) with a capped heap —
# one lean JVM per service. Override the cap with $env:SERVICE_MAX_HEAP if you have more RAM.
$maxHeap = if ($env:SERVICE_MAX_HEAP) { $env:SERVICE_MAX_HEAP } else { '384m' }

function Start-Service-Window($folder, $title, $extra) {
    $dir = Join-Path $root $folder
    $cmd = "`$host.UI.RawUI.WindowTitle = '$title';" +
           "Set-Location '$dir';" +
           "`$env:JAVA_HOME = '$javaHome';" +
           "`$env:MAVEN_OPTS = '-Xmx$maxHeap';" +
           "$extra" +
           "& '$mvn' -Dspring-boot.run.fork=false spring-boot:run"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
}

Write-Host "1/10  discovery-server (8761) start ho rahi hai... 35s wait" -ForegroundColor Cyan
Start-Service-Window "discovery-server" "discovery-8761" ""
Start-Sleep -Seconds 35

Write-Host "2/10  auth-service (8081)  [+ admin seed, jwt secret]..." -ForegroundColor Cyan
Start-Service-Window "auth-service" "auth-8081" $authEnv
Start-Sleep -Seconds 5

Write-Host "3/10  customer-service (8082)..." -ForegroundColor Cyan
Start-Service-Window "customer-service" "customer-8082" $db
Start-Sleep -Seconds 5

Write-Host "4/10  account-service (8083)..." -ForegroundColor Cyan
Start-Service-Window "account-service" "account-8083" $db
Start-Sleep -Seconds 5

Write-Host "5/10  transaction-service (8084)..." -ForegroundColor Cyan
Start-Service-Window "transaction-service" "transaction-8084" $db
Start-Sleep -Seconds 5

Write-Host "6/10  transfer-service (8085)..." -ForegroundColor Cyan
Start-Service-Window "transfer-service" "transfer-8085" $db
Start-Sleep -Seconds 5

Write-Host "7/10  loan-service (8086)..." -ForegroundColor Cyan
Start-Service-Window "loan-service" "loan-8086" $db
Start-Sleep -Seconds 5

Write-Host "8/10  card-service (8087)..." -ForegroundColor Cyan
Start-Service-Window "card-service" "card-8087" $db
Start-Sleep -Seconds 5

Write-Host "9/10  notification-service (8088)..." -ForegroundColor Cyan
Start-Service-Window "notification-service" "notification-8088" $db
Start-Sleep -Seconds 20

Write-Host "10/10 api-gateway (8090)  [jwt + internal secret]..." -ForegroundColor Cyan
Start-Service-Window "api-gateway" "gateway-8090" $gatewayEnv

Write-Host ""
Write-Host "Sab start ho gaye. ~30-40s baad test karein: http://localhost:8090" -ForegroundColor Green
Write-Host "Admin login: $adminEmail  (password env ADMIN_PASSWORD se)" -ForegroundColor Green
Write-Host "Swagger: http://localhost:8081/swagger-ui.html" -ForegroundColor Green
