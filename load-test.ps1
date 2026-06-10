# =====================================================================
#  load-test.ps1 - STRESS test (crash tak). Har service ke endpoints par
#  concurrent load. Record karta hai: kaunsi service kab crashi, aur
#  per-service kitni request ok/fail hui.
#  Run:  powershell -ExecutionPolicy Bypass -File .\load-test.ps1 -Concurrency 50 -Seconds 90
#  WARNING: ye jaan-boojh kar services ko load se gira sakta hai (dev only).
# =====================================================================
param([int]$Concurrency = 50, [int]$Seconds = 90)

$GW = 'http://localhost:8090'
$ErrorActionPreference = 'SilentlyContinue'

Write-Host "=== SETUP: load ke liye users + accounts banao ===" -ForegroundColor Cyan
$users = @()
for ($i = 0; $i -lt 6; $i++) {
    try {
        $r = Invoke-RestMethod -Method Post -Uri "$GW/api/auth/register" -ContentType 'application/json' -Body (@{ email = "lt$($i)_$(Get-Random)@bank.test"; password = 'secret123' } | ConvertTo-Json) -TimeoutSec 10
        $tok = $r.accessToken
        $a = Invoke-RestMethod -Method Post -Uri "$GW/api/accounts" -Headers @{ Authorization = "Bearer $tok" } -ContentType 'application/json' -Body (@{ accountType = 'SAVINGS'; initialDeposit = 100000000 } | ConvertTo-Json) -TimeoutSec 10
        Invoke-RestMethod -Method Post -Uri "$GW/api/customers" -Headers @{ Authorization = "Bearer $tok" } -ContentType 'application/json' -Body (@{ firstName = 'LT'; lastName = 'User'; phone = '03001112222'; address = 'City'; dateOfBirth = '1996-02-02' } | ConvertTo-Json) -TimeoutSec 10 | Out-Null
        $users += @{ tok = $tok; uid = $r.userId; acct = $a.accountNumber }
    } catch {}
}
if ($users.Count -lt 2) { Write-Host "Setup fail - gateway/auth down? Ruk gaya." -ForegroundColor Red; exit 1 }
Write-Host ("{0} users ready. Ab {1} concurrent workers x {2} sec FULL LOAD..." -f $users.Count, $Concurrency, $Seconds) -ForegroundColor Green

# ---- worker: deadline tak random endpoints hammer karta hai, local stats rakhta hai ----
$worker = {
    param($GW, $users, $deadline)
    $rnd = New-Object Random ([Environment]::TickCount + [Threading.Thread]::CurrentThread.ManagedThreadId)
    $st = @{}
    foreach ($s in 'auth','customer','account','transaction','transfer','loan','card','notification') { $st[$s] = @{ req = 0; ok = 0; fail = 0 } }
    while ((Get-Date) -lt $deadline) {
        $u = $users[$rnd.Next(0, $users.Count)]
        $h = @{ Authorization = "Bearer $($u.tok)" }
        $svc = 'account'; $m = 'GET'; $url = "$GW/api/accounts/me"; $body = $null
        switch ($rnd.Next(0, 10)) {
            0 { $svc='account'; $m='POST'; $url="$GW/api/accounts/$($u.acct)/deposit"; $body='{"amount":10}' }
            1 { $svc='account'; $m='POST'; $url="$GW/api/accounts/$($u.acct)/deposit"; $body='{"amount":5}' }
            2 { $svc='account'; $m='POST'; $url="$GW/api/accounts/$($u.acct)/withdraw"; $body='{"amount":5}' }
            3 { $d=$users[$rnd.Next(0,$users.Count)]; $tries=0; while($d.acct -eq $u.acct -and $tries -lt 6){ $d=$users[$rnd.Next(0,$users.Count)]; $tries++ }; $svc='transfer'; $m='POST'; $url="$GW/api/transfers"; $body=(@{fromAccount=$u.acct;toAccount=$d.acct;amount=1}|ConvertTo-Json) }
            4 { $svc='loan'; $m='POST'; $url="$GW/api/loans"; $body=(@{accountNumber=$u.acct;principal=2000;termMonths=6;purpose='lt'}|ConvertTo-Json) }
            5 { $svc='card'; $m='POST'; $url="$GW/api/cards"; $body=(@{cardType='CREDIT';cardholderName='LT';creditLimit=1000}|ConvertTo-Json) }
            6 { $svc='transaction'; $m='GET'; $url="$GW/api/transactions/me" }
            7 { $svc='notification'; $m='GET'; $url="$GW/api/notifications/me" }
            8 { $svc='customer'; $m='GET'; $url="$GW/api/customers/me" }
            9 { $svc='auth'; $m='GET'; $url="$GW/api/auth/validate" }
        }
        $st[$svc].req++
        try {
            $p = @{ Method = $m; Uri = $url; Headers = $h; UseBasicParsing = $true; TimeoutSec = 8 }
            if ($body) { $p.ContentType = 'application/json'; $p.Body = $body }
            Invoke-WebRequest @p | Out-Null
            $st[$svc].ok++
        } catch { $st[$svc].fail++ }
    }
    return $st
}

$deadline = (Get-Date).AddSeconds($Seconds)
$pool = [runspacefactory]::CreateRunspacePool(1, $Concurrency); $pool.Open()
$jobs = @()
for ($w = 0; $w -lt $Concurrency; $w++) {
    $ps = [powershell]::Create(); $ps.RunspacePool = $pool
    [void]$ps.AddScript($worker).AddArgument($GW).AddArgument($users).AddArgument($deadline)
    $jobs += @{ ps = $ps; h = $ps.BeginInvoke() }
}

# ---- monitor: har 2 sec ports check, crash timeline record ----
$svcPorts = [ordered]@{ discovery=8761; auth=8081; customer=8082; account=8083; transaction=8084; transfer=8085; loan=8086; card=8087; notification=8088; gateway=8090 }
$dead = [ordered]@{}
Write-Host "`n=== LOAD CHAL RAHA HAI (live crash monitor) ===" -ForegroundColor Cyan
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
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
}

# ---- collect worker stats ----
$total = @{}
foreach ($s in 'auth','customer','account','transaction','transfer','loan','card','notification') { $total[$s] = @{ req=0; ok=0; fail=0 } }
foreach ($j in $jobs) {
    try { $res = $j.ps.EndInvoke($j.h); foreach ($r in $res) { if ($r -is [hashtable]) { foreach ($s in $r.Keys) { $total[$s].req += $r[$s].req; $total[$s].ok += $r[$s].ok; $total[$s].fail += $r[$s].fail } } } } catch {}
    $j.ps.Dispose()
}
$pool.Close()

# ---- final port state ----
$aliveNow = 0; $downNow = @()
foreach ($k in $svcPorts.Keys) { if (Get-NetTCPConnection -State Listen -LocalPort $svcPorts[$k] -EA SilentlyContinue) { $aliveNow++ } else { $downNow += $k } }

Write-Host "`n=========  PER-SERVICE ENDPOINT RESULTS (load ke neeche)  =========" -ForegroundColor Cyan
$grand = 0
foreach ($s in $total.Keys) {
    $t = $total[$s]; $grand += $t.req
    $rate = if ($t.req) { [math]::Round(100 * $t.ok / $t.req) } else { 0 }
    Write-Host ("  {0,-13} req={1,-7} ok={2,-7} fail={3,-7} ({4}% ok)" -f $s, $t.req, $t.ok, $t.fail, $rate)
}
Write-Host ("`n  TOTAL requests fired: {0}" -f $grand) -ForegroundColor Green

Write-Host "`n=========  CRASH REPORT  =========" -ForegroundColor Cyan
if ($dead.Count) { foreach ($k in $dead.Keys) { Write-Host ("  {0,-13} crashed at {1}" -f $k, $dead[$k]) -ForegroundColor Red } }
else { Write-Host "  Koi service crash nahi hui (load kam tha ya system tik gaya)." -ForegroundColor Green }
Write-Host ("`n  Ab alive: {0}/10. Down: {1}" -f $aliveNow, $(if ($downNow) { $downNow -join ', ' } else { 'koi nahi' })) -ForegroundColor Yellow
