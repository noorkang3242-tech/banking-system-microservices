# build-jars.ps1 — har service ka executable (fat) jar banata hai (tests skip).
# Iske baad run-jars.ps1 se lean tareeke se chalayein.
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$mvn  = "E:\tools\apache-maven-3.9.9\bin\mvn.cmd"
$root = "C:\Users\Lenovo\Desktop\cash\banking-system-microservices"
$mods = @("discovery-server","auth-service","customer-service","account-service","transaction-service",
          "transfer-service","loan-service","card-service","notification-service","api-gateway")
$fail = @()
foreach ($m in $mods) {
    Write-Host "packaging $m ..." -ForegroundColor Cyan
    & $mvn -q -DskipTests package -f "$root\$m\pom.xml"
    if ($LASTEXITCODE -ne 0) { $fail += $m }
}
if ($fail.Count) { Write-Host ("FAILED: " + ($fail -join ', ')) -ForegroundColor Red }
else { Write-Host "All 10 jars built. Ab run-jars.ps1 chalayein." -ForegroundColor Green }
