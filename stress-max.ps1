# =====================================================================
#  stress-max.ps1 - MULTI-PROCESS stress. Ek PowerShell ~130 req/s pe
#  cap ho jata hai, isliye kai parallel load-test processes launch karke
#  aggregate load badhate hain, aur crash monitor karte hain.
#  Run:  powershell -ExecutionPolicy Bypass -File .\stress-max.ps1 -Procs 6 -Workers 50 -Seconds 100
# =====================================================================
param([int]$Procs = 6, [int]$Workers = 50, [int]$Seconds = 100)

$root = "C:\Users\Lenovo\Desktop\cash\banking-system-microservices"

Write-Host ("Launching {0} parallel load processes x {1} workers (~{2} aggregate)..." -f $Procs, $Workers, ($Procs * $Workers)) -ForegroundColor Cyan
for ($i = 0; $i -lt $Procs; $i++) {
    Start-Process powershell -ArgumentList "-ExecutionPolicy","Bypass","-File","$root\load-test.ps1","-Concurrency","$Workers","-Seconds","$Seconds" -WindowStyle Minimized
    Start-Sleep -Milliseconds 300
}

Write-Host "`n=== CRASH MONITOR (har 3s) ===" -ForegroundColor Cyan
$svcPorts = [ordered]@{ discovery=8761; auth=8081; customer=8082; account=8083; transaction=8084; transfer=8085; loan=8086; card=8087; notification=8088; gateway=8090 }
$dead = [ordered]@{}
$deadline = (Get-Date).AddSeconds($Seconds + 20)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    foreach ($k in $svcPorts.Keys) {
        if (-not $dead.Contains($k)) {
            if (-not (Get-NetTCPConnection -State Listen -LocalPort $svcPorts[$k] -EA SilentlyContinue)) {
                $dead[$k] = (Get-Date).ToString('HH:mm:ss')
                Write-Host ("  [{0}] CRASH -> {1} ({2}) gir gayi!" -f $dead[$k], $k, $svcPorts[$k]) -ForegroundColor Red
            }
        }
    }
    $os = Get-CimInstance Win32_OperatingSystem
    Write-Host ("  alive {0}/10  |  free RAM {1} GB" -f (10 - $dead.Count), [math]::Round($os.FreePhysicalMemory/1MB,2)) -ForegroundColor DarkGray
    if ($dead.Count -ge 10) { Write-Host "  SAB GIR GAYE!" -ForegroundColor Red; break }
}

Write-Host "`n=========  CRASH TIMELINE  =========" -ForegroundColor Cyan
if ($dead.Count) { foreach ($k in $dead.Keys) { Write-Host ("  {0,-13} crashed at {1}" -f $k, $dead[$k]) -ForegroundColor Red } }
else { Write-Host "  Koi service crash nahi hui - system ne ye bhari load bhi jhel liya." -ForegroundColor Green }
$aliveNow = 0; $downNow = @()
foreach ($k in $svcPorts.Keys) { if (Get-NetTCPConnection -State Listen -LocalPort $svcPorts[$k] -EA SilentlyContinue) { $aliveNow++ } else { $downNow += $k } }
Write-Host ("`n  Final alive: {0}/10. Down: {1}" -f $aliveNow, $(if ($downNow) { $downNow -join ', ' } else { 'koi nahi' })) -ForegroundColor Yellow
