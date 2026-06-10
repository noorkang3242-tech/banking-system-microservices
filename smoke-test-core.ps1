# =====================================================================
#  Core-services smoke test  (auth + account + transfer) via the gateway
#  Run:  powershell -ExecutionPolicy Bypass -File .\smoke-test-core.ps1
#  Requires: discovery, auth, account, transaction, transfer, gateway UP.
# =====================================================================

$ErrorActionPreference = 'Stop'
$GW   = 'http://localhost:8090'
$pass = 0
$fail = 0

function Invoke-Api {
    param([string]$Method, [string]$Url, [hashtable]$Headers, $Body)
    $p = @{ Method = $Method; Uri = $Url; ContentType = 'application/json'; UseBasicParsing = $true }
    if ($Headers) { $p.Headers = $Headers }
    if ($null -ne $Body) {
        if ($Body -is [string]) { $p.Body = $Body } else { $p.Body = ($Body | ConvertTo-Json -Depth 6) }
    }
    try {
        $r = Invoke-WebRequest @p
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($resp -and $resp.StatusCode) {
            $content = ''
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream())
                $content = $sr.ReadToEnd()
            } catch {}
            return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $content }
        }
        return [pscustomobject]@{ Status = -1; Body = $_.Exception.Message }
    }
}

function Check {
    param([string]$Name, [int]$Expected, [int]$Actual)
    if ($Expected -eq $Actual) {
        Write-Host ("  PASS  {0,-45} {1}" -f $Name, $Actual) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("  FAIL  {0,-45} expected {1}, got {2}" -f $Name, $Expected, $Actual) -ForegroundColor Red
        $script:fail++
    }
}

function CheckS {
    param([string]$Name, [string]$Expected, [string]$Actual)
    if ($Expected -eq $Actual) {
        Write-Host ("  PASS  {0,-45} {1}" -f $Name, $Actual) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("  FAIL  {0,-45} expected '{1}', got '{2}'" -f $Name, $Expected, $Actual) -ForegroundColor Red
        $script:fail++
    }
}

function J($s) { if ($s) { $s | ConvertFrom-Json } }

$sfx = Get-Random -Maximum 999999
$custEmail = "alice$sfx@bank.test"
$pw = "secret123"

Write-Host "`n=== 1. AUTH ===" -ForegroundColor Cyan

$r = Invoke-Api POST "$GW/api/auth/register" $null @{ email = $custEmail; password = $pw }
Check "register customer -> 201" 201 $r.Status
$token  = (J $r.Body).accessToken
$userId = (J $r.Body).userId

$r = Invoke-Api POST "$GW/api/auth/login" $null @{ email = $custEmail; password = $pw }
Check "login -> 200" 200 $r.Status
if ((J $r.Body).accessToken) { $token = (J $r.Body).accessToken }

$r = Invoke-Api POST "$GW/api/auth/register" $null @{ email = $custEmail; password = $pw }
Check "duplicate register -> 409" 409 $r.Status

$r = Invoke-Api POST "$GW/api/auth/login" $null @{ email = $custEmail; password = "wrongpass" }
Check "login wrong password -> 401" 401 $r.Status

$r = Invoke-Api POST "$GW/api/auth/register" $null '{ "email": "broken", '   # malformed JSON
Check "malformed JSON -> 400" 400 $r.Status

$r = Invoke-Api POST "$GW/api/auth/register" $null @{ email = "x$sfx@bank.test"; password = "123" }
Check "short password (validation) -> 400" 400 $r.Status

$auth = @{ Authorization = "Bearer $token" }

Write-Host "`n=== 2. ACCOUNTS ===" -ForegroundColor Cyan

$r = Invoke-Api POST "$GW/api/accounts" $auth @{ accountType = "SAVINGS"; initialDeposit = 1000 }
Check "open account A (initial 1000) -> 201" 201 $r.Status
$accA = (J $r.Body).accountNumber

$r = Invoke-Api POST "$GW/api/accounts" $auth @{ accountType = "CURRENT" }
Check "open account B (no deposit) -> 201" 201 $r.Status
$accB = (J $r.Body).accountNumber

$r = Invoke-Api GET "$GW/api/accounts/me" $auth $null
Check "list my accounts -> 200" 200 $r.Status
Check "  ... has 2 accounts" 2 ((J $r.Body).Count)

$r = Invoke-Api POST "$GW/api/accounts/$accA/deposit" $auth @{ amount = 500 }
Check "deposit 500 -> 200" 200 $r.Status
Check "  ... balance now 1500" 1500 ([int](J $r.Body).balance)

$r = Invoke-Api POST "$GW/api/accounts/$accA/withdraw" $auth @{ amount = 200 }
Check "withdraw 200 -> 200" 200 $r.Status
Check "  ... balance now 1300" 1300 ([int](J $r.Body).balance)

$r = Invoke-Api POST "$GW/api/accounts/$accA/withdraw" $auth @{ amount = 999999 }
Check "withdraw over balance -> 422" 422 $r.Status

$r = Invoke-Api POST "$GW/api/accounts/$accA/deposit" $auth @{ amount = -5 }
Check "deposit negative (validation) -> 400" 400 $r.Status

$r = Invoke-Api GET "$GW/api/accounts/$accA" $auth $null
Check "get account A -> 200" 200 $r.Status

$r = Invoke-Api GET "$GW/api/accounts" $auth $null
Check "customer lists ALL accounts -> 403" 403 $r.Status

Write-Host "`n=== 3. TRANSFERS ===" -ForegroundColor Cyan

$r = Invoke-Api POST "$GW/api/transfers" $auth @{ fromAccount = $accA; toAccount = $accB; amount = 300 }
Check "transfer 300 A->B -> 201" 201 $r.Status

$r = Invoke-Api GET "$GW/api/accounts/$accA" $auth $null
Check "  ... A balance 1000" 1000 ([int](J $r.Body).balance)
$r = Invoke-Api GET "$GW/api/accounts/$accB" $auth $null
Check "  ... B balance 300"  300  ([int](J $r.Body).balance)

$r = Invoke-Api POST "$GW/api/transfers" $auth @{ fromAccount = $accA; toAccount = $accA; amount = 10 }
Check "self-transfer -> 400" 400 $r.Status

$r = Invoke-Api POST "$GW/api/transfers" $auth @{ fromAccount = $accA; toAccount = "0000000000000000"; amount = 10 }
Check "transfer to missing dest -> 400" 400 $r.Status

$r = Invoke-Api POST "$GW/api/transfers" $auth @{ fromAccount = $accA; toAccount = $accB; amount = 999999 }
Check "transfer over balance -> 422" 422 $r.Status

$r = Invoke-Api GET "$GW/api/transfers/me" $auth $null
Check "my transfers -> 200" 200 $r.Status
Check "  ... has 1 completed transfer" 1 ((J $r.Body).Count)

Write-Host "`n=== 4. SECURITY (gateway) ===" -ForegroundColor Cyan

$r = Invoke-Api GET "$GW/api/accounts/me" $null $null
Check "no token -> 401" 401 $r.Status

$r = Invoke-Api GET "$GW/api/accounts/me" @{ Authorization = "Bearer not.a.real.token" } $null
Check "garbage token -> 401" 401 $r.Status

Write-Host "`n=== 5. PRODUCTION HARDENING ===" -ForegroundColor Cyan

# Registration is locked to CUSTOMER: even asking for ADMIN is ignored.
$r = Invoke-Api POST "$GW/api/auth/register" $null @{ email = "lock$sfx@bank.test"; password = $pw; role = "ADMIN" }
Check "register (asking for ADMIN) -> 201" 201 $r.Status
CheckS "  ... role forced to CUSTOMER" "CUSTOMER" (J $r.Body).role

# Seeded admin can log in and use staff-only endpoints.
$r = Invoke-Api POST "$GW/api/auth/login" $null @{ email = "admin@bank.local"; password = "Admin@12345" }
Check "seeded admin login -> 200" 200 $r.Status
$adminTok = (J $r.Body).accessToken
CheckS "  ... admin role == ADMIN" "ADMIN" (J $r.Body).role
$adminAuth = @{ Authorization = "Bearer $adminTok" }
$r = Invoke-Api GET "$GW/api/accounts" $adminAuth $null
Check "admin lists ALL accounts (staff) -> 200" 200 $r.Status

# Direct service-port access (bypassing the gateway, no shared secret) is rejected,
# even with forged identity headers — closes the header-trust gap.
$r = Invoke-Api GET "http://localhost:8083/api/accounts/me" @{ 'X-User-Id' = 'hacker'; 'X-User-Role' = 'ADMIN' } $null
Check "direct account-port + forged headers -> 401" 401 $r.Status
$r = Invoke-Api POST "http://localhost:8081/api/auth/register" $null @{ email = "bypass$sfx@bank.test"; password = $pw }
Check "direct auth-port register (no secret) -> 401" 401 $r.Status
# Health endpoint stays reachable directly (exempt) for probes.
$r = Invoke-Api GET "http://localhost:8083/actuator/health" $null $null
Check "direct actuator/health (exempt) -> 200" 200 $r.Status

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host ("  RESULT:  {0} passed, {1} failed" -f $pass, $fail) -ForegroundColor $(if ($fail) { 'Red' } else { 'Green' })
Write-Host "========================================`n" -ForegroundColor Cyan
if ($fail) { exit 1 } else { exit 0 }
