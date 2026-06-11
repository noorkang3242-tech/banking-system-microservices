# =====================================================================
#  seed-data.ps1 - Full-load demo data across ALL services.
#  Har service ka role use hota hai: users, profiles, accounts,
#  ~1000 transactions, transfers, loans, cards, notifications.
#  Run:  powershell -ExecutionPolicy Bypass -File .\seed-data.ps1 -Users 10 -TargetTxn 1000
#  Resilient: agar system distress mein aaye to ruk kar summary deta hai.
# =====================================================================
param([int]$Users = 10, [int]$TargetTxn = 1000)

$GW = 'http://localhost:8090'

function Call($m, $u, $tok, $b) {
    $p = @{ Method = $m; Uri = $u; ContentType = 'application/json'; UseBasicParsing = $true; TimeoutSec = 15 }
    if ($tok) { $p.Headers = @{ Authorization = "Bearer $tok" } }
    if ($null -ne $b) { $p.Body = ($b | ConvertTo-Json) }
    try {
        $r = Invoke-WebRequest @p
        $data = $null; try { $data = $r.Content | ConvertFrom-Json } catch {}
        return @{ ok = $true; code = [int]$r.StatusCode; data = $data }
    } catch {
        $resp = $_.Exception.Response
        return @{ ok = $false; code = $(if ($resp) { [int]$resp.StatusCode } else { -1 }); data = $null }
    }
}

$s = [ordered]@{ users=0; profiles=0; accounts=0; kyc=0; deposits=0; withdrawals=0;
                 transfers=0; loansApplied=0; loansApproved=0; loansRepaid=0;
                 cardsIssued=0; cardsBlocked=0; notifsSent=0; txnTotal=0; errors=0 }
$script:consecFail = 0
function Dead($r) {
    if (-not $r.ok -and ($r.code -eq -1 -or $r.code -eq 503)) { $script:consecFail++ } else { $script:consecFail = 0 }
    return ($script:consecFail -ge 20)
}

Write-Host "=== ADMIN login ===" -ForegroundColor Cyan
$adm = Call POST "$GW/api/auth/login" $null @{ email='admin@bank.local'; password='Admin@12345' }
$adminTok = $adm.data.accessToken
if (-not $adminTok) { Write-Host "Admin login fail (auth/gateway down?). Ruk gaya." -ForegroundColor Red; exit 1 }
Write-Host "admin token mil gaya." -ForegroundColor Green

# ---------- 1) USERS + PROFILES + ACCOUNTS ----------
Write-Host ("`n=== USERS / PROFILES / ACCOUNTS ({0} users) ===" -f $Users) -ForegroundColor Cyan
$people = @()
for ($i = 1; $i -le $Users; $i++) {
    $em = "user{0}_{1}@bank.test" -f $i, (Get-Random -Maximum 99999)
    $r = Call POST "$GW/api/auth/register" $null @{ email=$em; password='secret123' }
    if (-not $r.ok) { $s.errors++; continue }
    $s.users++
    $tok = $r.data.accessToken; $uid = $r.data.userId

    Call POST "$GW/api/customers" $tok @{ firstName=("User{0}" -f $i); lastName="Khan"; phone=("0300000{0}" -f $i); address=("City {0}" -f $i); dateOfBirth="1995-01-15" } | Out-Null
    $s.profiles++

    $a = Call POST "$GW/api/accounts" $tok @{ accountType='SAVINGS'; initialDeposit=1000000 }
    $b = Call POST "$GW/api/accounts" $tok @{ accountType='CURRENT'; initialDeposit=0 }
    $acctA = $a.data.accountNumber; $acctB = $b.data.accountNumber
    if ($acctA) { $s.accounts++; $s.txnTotal++ }
    if ($acctB) { $s.accounts++ }

    if ($i % 2 -eq 0) { $k = Call PATCH "$GW/api/customers/$uid/kyc" $adminTok @{ kycStatus='VERIFIED' }; if ($k.ok) { $s.kyc++ } }

    $people += @{ tok=$tok; uid=$uid; acctA=$acctA; acctB=$acctB }
    Write-Host ("  user {0} ready (A={1})" -f $i, $acctA) -ForegroundColor DarkGray
}
if ($people.Count -eq 0) { Write-Host "Koi user nahi bana - system down. Ruk gaya." -ForegroundColor Red; exit 1 }

# ---------- 2) TRANSACTIONS (volume loop) ----------
Write-Host ("`n=== TRANSACTIONS - target {0} ===" -f $TargetTxn) -ForegroundColor Cyan
$idx = 0
while ($s.txnTotal -lt $TargetTxn) {
    $p = $people[$idx % $people.Count]; $idx++
    if (-not $p.acctA) { continue }
    if ($idx % 3 -eq 0) {
        $r = Call POST "$GW/api/accounts/$($p.acctA)/withdraw" $p.tok @{ amount=50 }
        if ($r.ok) { $s.withdrawals++; $s.txnTotal++ } else { $s.errors++ }
    } else {
        $r = Call POST "$GW/api/accounts/$($p.acctA)/deposit" $p.tok @{ amount=100 }
        if ($r.ok) { $s.deposits++; $s.txnTotal++ } else { $s.errors++ }
    }
    if (Dead $r) { Write-Host "`nSystem distress (lagataar fail) - volume loop ruk raha hai." -ForegroundColor Yellow; break }
    if ($s.txnTotal % 250 -eq 0) { Write-Host ("  ... {0}/{1} transactions" -f $s.txnTotal, $TargetTxn) -ForegroundColor DarkGray }
    Start-Sleep -Milliseconds 4
}

# ---------- 3) TRANSFERS (each = 2 txns: out+in) ----------
Write-Host "`n=== TRANSFERS ===" -ForegroundColor Cyan
foreach ($p in $people) {
    if ($p.acctA -and $p.acctB) {
        $r = Call POST "$GW/api/transfers" $p.tok @{ fromAccount=$p.acctA; toAccount=$p.acctB; amount=200 }
        if ($r.ok) { $s.transfers++; $s.txnTotal += 2 } else { $s.errors++ }
        if (Dead $r) { break }
        Start-Sleep -Milliseconds 25
    }
}

# ---------- 4) LOANS (apply -> admin approve -> repay) ----------
Write-Host "`n=== LOANS ===" -ForegroundColor Cyan
foreach ($p in $people) {
    if (-not $p.acctA) { continue }
    $r = Call POST "$GW/api/loans" $p.tok @{ accountNumber=$p.acctA; principal=50000; termMonths=12; interestRate=10; purpose='Demo loan' }
    if (-not $r.ok) { $s.errors++; if (Dead $r) { break }; continue }
    $s.loansApplied++; $loanId = $r.data.loanId
    $ap = Call PATCH "$GW/api/loans/$loanId/approve" $adminTok $null
    if ($ap.ok) { $s.loansApproved++; $s.txnTotal++ }
    $rp = Call POST "$GW/api/loans/$loanId/repay" $p.tok @{ amount=5000 }
    if ($rp.ok) { $s.loansRepaid++; $s.txnTotal++ }
    Start-Sleep -Milliseconds 25
}

# ---------- 5) CARDS (issue -> block half) ----------
Write-Host "`n=== CARDS ===" -ForegroundColor Cyan
$ci = 0
foreach ($p in $people) {
    if (-not $p.acctA) { continue }
    $r = Call POST "$GW/api/cards" $p.tok @{ cardType='DEBIT'; accountNumber=$p.acctA; cardholderName='USER KHAN' }
    if (-not $r.ok) { $s.errors++; if (Dead $r) { break }; continue }
    $s.cardsIssued++; $cardId = $r.data.cardId; $ci++
    if ($ci % 2 -eq 0) { $bk = Call POST "$GW/api/cards/$cardId/block" $p.tok $null; if ($bk.ok) { $s.cardsBlocked++ } }
    Start-Sleep -Milliseconds 25
}

# ---------- 6) NOTIFICATIONS (admin sends welcome to each) ----------
Write-Host "`n=== NOTIFICATIONS ===" -ForegroundColor Cyan
foreach ($p in $people) {
    $r = Call POST "$GW/api/notifications/send" $adminTok @{ recipientUserId=$p.uid; type='GENERAL'; title='Welcome'; message='Aapka account active hai. Shukriya!' }
    if ($r.ok) { $s.notifsSent++ } else { $s.errors++ }
    if (Dead $r) { break }
    Start-Sleep -Milliseconds 25
}

# ---------- SUMMARY ----------
Write-Host "`n=====================  SUMMARY  =====================" -ForegroundColor Cyan
foreach ($k in $s.Keys) { Write-Host ("  {0,-14} : {1}" -f $k, $s[$k]) }
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ("TRANSACTIONS banaye (approx): {0}" -f $s.txnTotal) -ForegroundColor Green
