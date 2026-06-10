# 🏦 Banking System — Step by Step Complete Guide (har service, role, URL, JSON)

> Ye file **upar se neeche follow** karne ke liye hai. Ek customer ke pure safar ke order mein har service ka **kaam (role)**, uski **URLs + JSON data**, aur **kaise chalana hai** — sab.
> **Base URL hamesha:** `http://localhost:8090` (gateway). **8080 NAHI** (wo Oracle hai).

---

## 📋 Saare services ek nazar mein (role table)

| # | Service | Port | Kaam (Role) | Kis pe depend |
|---|---|---|---|---|
| — | **discovery** (Eureka) | 8761 | Sabka address book — har service yahan register hoti hai | — |
| — | **gateway** | **8090** | Sabka darwaza — token check, security, sahi service tak bhejna | discovery |
| 1 | **auth** | 8081 | Register/login, **token (JWT)** deti hai | discovery |
| 2 | **customer** | 8082 | Customer **profile + KYC** | gateway |
| 3 | **account** | 8083 | **Account** kholna, balance, deposit, withdraw | + transaction |
| 4 | **transaction** | 8084 | **History/ledger** (har len-den ka record) | — |
| 5 | **transfer** | 8085 | Ek account se doosre **paisa bhejna** (saga) | account, notification |
| 6 | **loan** | 8086 | **Loan** apply/approve/repay | account, notification |
| 7 | **card** | 8087 | **Debit/Credit card** issue/block | account, notification |
| 8 | **notification** | 8088 | User ko **alerts** bhejna | — |

> **Auto-magic:** deposit/withdraw karte hi transaction **khud** ban jaati hai; transfer/loan-approve/card-issue pe notification **khud** ban jaati hai. Tumhe manually nahi banani.

---

## 🟢 PART 0 — System start karo (pehle ye)

CRUD karne ke liye kam se kam **4 services** chahiye: discovery + auth + gateway + jis service ka kaam hai.
Har service apni terminal window mein (jar se):

```powershell
# Window 1 — discovery (pehle, ~30s ruko)
cd C:\Users\Lenovo\Desktop\cash\banking-system-microservices\discovery-server
java -Xmx256m -jar target\discovery-server-0.0.1-SNAPSHOT.jar

# Window 2 — auth (DB + admin seed)
cd C:\Users\Lenovo\Desktop\cash\banking-system-microservices\auth-service
$env:DB_USERNAME="root"; $env:DB_PASSWORD="your_mysql_password"
$env:ADMIN_EMAIL="admin@bank.local"; $env:ADMIN_PASSWORD="Admin@12345"
java -Xmx256m -jar target\auth-service-0.0.1-SNAPSHOT.jar

# Window 3..N — jis service ka kaam (DB)
cd C:\Users\Lenovo\Desktop\cash\banking-system-microservices\<service-folder>
$env:DB_USERNAME="root"; $env:DB_PASSWORD="your_mysql_password"
java -Xmx256m -jar target\<service>-0.0.1-SNAPSHOT.jar

# Aakhri Window — gateway (port 8090)
cd C:\Users\Lenovo\Desktop\cash\banking-system-microservices\api-gateway
$env:SERVER_PORT="8090"
java -Xmx256m -jar target\api-gateway-0.0.1-SNAPSHOT.jar
```
> RAM kam (8 GB) hai — saare 10 ek saath mat chalao. Jis service par kaam hai sirf wahi + (discovery, auth, gateway).
> ⚠️ Service ko VS Code ke ▶ **Run button se mat chalao** — sirf **terminal mein `java -jar`** se (warna "main class not found" error).

---

## 🔑 PART 0.5 — Postman basics (3 cheezein, ek baar samajh lo)

1. **Method** → URL bar ke **left dropdown** se choose karo (GET/POST/PUT/PATCH). *(Method ko URL mein mat likho!)*
2. **URL** → box mein sirf URL: `http://localhost:8090/...`
3. **Token** → **Headers** tab → Key: `Authorization`, Value: `Bearer <token>`
4. **JSON body** → **Body** tab → **raw** → dropdown **JSON** → box mein JSON paste.

**Do tarah ke token:**
- **customer token** → apne kaam ke liye (Register/Login se milta hai).
- **admin token** → staff kaam ke liye → login `admin@bank.local` / `Admin@12345`.

---

# 1️⃣ AUTH SERVICE — "Pehchaan + token"
**Role:** User ko register/login karti hai aur ek **JWT token** deti hai jo har dusri request mein lagता hai. Bina token kuch nahi chalta.

### Step 1.1 — Register (naya customer + token)
- **Kaam:** Naya account banata hai (hamesha CUSTOMER) aur token deta hai.
- `POST http://localhost:8090/api/auth/register`
- **Token:** nahi chahiye
- **Body (raw → JSON):**
```json
{
  "email": "ali@example.com",
  "password": "secret123"
}
```
- **Response (201):**
```json
{ "userId": "abc-123", "email": "ali@example.com", "role": "CUSTOMER",
  "accessToken": "eyJhbGciOiJI...", "tokenType": "Bearer" }
```
👉 **`accessToken` copy karo = ye tumhara customer token hai. `userId` bhi note karo.**

### Step 1.2 — Login (dobara token chahiye to)
- `POST http://localhost:8090/api/auth/login`
- **Body (raw → JSON):**
```json
{ "email": "ali@example.com", "password": "secret123" }
```
- **Response (200):** wahi `accessToken`. Galat password = `401`.

### Step 1.3 — Admin token (staff kaam ke liye)
- `POST http://localhost:8090/api/auth/login`
- **Body:**
```json
{ "email": "admin@bank.local", "password": "Admin@12345" }
```
- **Response:** `accessToken` with `"role": "ADMIN"` 👉 **ye admin token hai.**

### Step 1.4 — Validate (token sahi hai?)
- `GET http://localhost:8090/api/auth/validate`  · Header: `Authorization: Bearer <token>`
- Response: `{ "valid": true, ... }`

---

# 2️⃣ CUSTOMER SERVICE — "Profile + KYC"
**Role:** User ki personal profile (naam, phone, address) aur **KYC** (verification) sambhalti hai.
**Pehle chahiye:** customer token (Step 1.1).

### Step 2.1 — Create my profile *(CREATE)*
- `POST http://localhost:8090/api/customers`
- **Token:** customer
- **Body (raw → JSON):**
```json
{
  "firstName": "Ali",
  "lastName": "Khan",
  "phone": "03001234567",
  "address": "Lahore",
  "dateOfBirth": "1998-05-10"
}
```
- **Response (201):** profile + `"kycStatus": "PENDING"`.

### Step 2.2 — Get my profile *(READ)*
- `GET http://localhost:8090/api/customers/me`  · Token: customer

### Step 2.3 — Update my profile *(UPDATE)*
- `PUT http://localhost:8090/api/customers/me`  · Token: customer
- **Body (sirf jo badalna ho):**
```json
{ "phone": "03009998888", "address": "Karachi" }
```

### Step 2.4 — (STAFF) All customers / one / KYC
- `GET http://localhost:8090/api/customers` · Token: **admin** → saare customers
- `GET http://localhost:8090/api/customers/<userId>` · Token: **admin**
- `PATCH http://localhost:8090/api/customers/<userId>/kyc` · Token: **admin** · Body:
```json
{ "kycStatus": "VERIFIED" }
```
> `kycStatus` = PENDING / VERIFIED / REJECTED. Customer token se ye = **403**.

---

# 3️⃣ ACCOUNT SERVICE — "Paisa rakhne ki jagah"
**Role:** Bank account kholti hai, **balance** rakhti hai, **deposit/withdraw** karti hai. Bank ka dil. (Har deposit/withdraw pe transaction **khud** record hoti hai.)
**Pehle chahiye:** customer token.

### Step 3.1 — Open account *(account kholo)*
- `POST http://localhost:8090/api/accounts`  · Token: customer
- **Body:**
```json
{ "accountType": "SAVINGS", "initialDeposit": 1000 }
```
> `accountType` = SAVINGS / CURRENT. `initialDeposit` optional.
- **Response (201):**
```json
{ "accountNumber": "4821009377451180", "balance": 1000, "status": "ACTIVE", ... }
```
👉 **`accountNumber` copy karo — aage deposit/transfer/loan/card sab mein chahiye.**

### Step 3.2 — My accounts *(READ)*
- `GET http://localhost:8090/api/accounts/me`  · Token: customer

### Step 3.3 — Balance / one account
- `GET http://localhost:8090/api/accounts/<accountNumber>`  · Token: customer

### Step 3.4 — Deposit *(paisa daalo)*
- `POST http://localhost:8090/api/accounts/<accountNumber>/deposit`  · Token: customer
- **Body:** `{ "amount": 500 }`  → balance badh jayega.

### Step 3.5 — Withdraw *(paisa nikalo)*
- `POST http://localhost:8090/api/accounts/<accountNumber>/withdraw`  · Token: customer
- **Body:** `{ "amount": 200 }`  → balance kam, **balance se zyada = 422**.

### Step 3.6 — (STAFF) All accounts
- `GET http://localhost:8090/api/accounts`  · Token: **admin**

---

# 4️⃣ TRANSACTION SERVICE — "History / ledger"
**Role:** Har len-den ka **record** rakhti hai. Tum khud kuch nahi banate — account/transfer khud record karte hain. Tum sirf **dekhte** ho.
**Pehle chahiye:** customer token + kuch deposit/withdraw kar liya ho.

### Step 4.1 — My transactions
- `GET http://localhost:8090/api/transactions/me`  · Token: customer
- **Response:** list — har ek mein `type` (DEPOSIT/WITHDRAWAL/TRANSFER_IN/TRANSFER_OUT), `amount`, `balanceAfter`.

### Step 4.2 — Ek account ki transactions
- `GET http://localhost:8090/api/transactions/account/<accountNumber>`  · Token: customer

### Step 4.3 — (STAFF) All transactions
- `GET http://localhost:8090/api/transactions`  · Token: **admin**

---

# 5️⃣ TRANSFER SERVICE — "Paisa bhejna"
**Role:** Ek account se doosre mein paisa bhejti hai. **Saga:** pehle source se nikalti hai, phir destination mein daalti hai; agar beech mein fail ho to **source ko wapas** kar deti hai (paisa kabhi gum nahi hota).
**Pehle chahiye:** customer token + 2 account numbers (Step 3.1 do baar karke 2 account banao).

### Step 5.1 — Transfer money
- `POST http://localhost:8090/api/transfers`  · Token: customer (jiske paas `fromAccount` hai)
- **Body:**
```json
{
  "fromAccount": "4821009377451180",
  "toAccount": "5530771148820094",
  "amount": 300
}
```
- **Response (201):** `"status": "COMPLETED"`. Same account = 400, balance kam = 422.
> Iske baad **dono parties ko notification khud** chali jati hai, aur 2 transactions (TRANSFER_OUT/IN) ban jati hain.

### Step 5.2 — My transfers
- `GET http://localhost:8090/api/transfers/me`  · Token: customer

### Step 5.3 — One transfer / (STAFF) all
- `GET http://localhost:8090/api/transfers/<transferId>`  · Token: customer (owner) ya admin
- `GET http://localhost:8090/api/transfers`  · Token: **admin**

---

# 6️⃣ LOAN SERVICE — "Loan lena"
**Role:** Customer loan apply karta hai (PENDING), **staff approve** karta hai (paisa account mein aata hai, ACTIVE), phir customer **repay** karta hai.
**Pehle chahiye:** customer token + ek accountNumber.

### Step 6.1 — Apply for loan *(customer)*
- `POST http://localhost:8090/api/loans`  · Token: customer
- **Body:**
```json
{
  "accountNumber": "4821009377451180",
  "principal": 50000,
  "termMonths": 12,
  "interestRate": 10,
  "purpose": "Home renovation"
}
```
> `principal` ≥ 1000, `termMonths` 1–360, `interestRate` optional (default 10%).
- **Response (201):** `loanId`, `totalPayable`, `"status": "PENDING"`. 👉 **`loanId` copy karo.**

### Step 6.2 — (STAFF) Approve + disburse
- `PATCH http://localhost:8090/api/loans/<loanId>/approve`  · Token: **admin** · Body: nahi
- Status → ACTIVE, principal account mein aa jata hai.

### Step 6.3 — (STAFF) Reject
- `PATCH http://localhost:8090/api/loans/<loanId>/reject`  · Token: **admin**
- **Body (optional):** `{ "reason": "Income proof kam hai" }`

### Step 6.4 — Repay *(customer)*
- `POST http://localhost:8090/api/loans/<loanId>/repay`  · Token: customer
- **Body:** `{ "amount": 5000 }`  → outstanding kam; 0 ho to CLOSED.

### Step 6.5 — My loans / one / (STAFF) all
- `GET http://localhost:8090/api/loans/me`  · customer
- `GET http://localhost:8090/api/loans/<loanId>`  · owner/admin
- `GET http://localhost:8090/api/loans`  · **admin**

---

# 7️⃣ CARD SERVICE — "Debit/Credit card"
**Role:** Card issue karti hai. Pura number + **CVV sirf ek baar** dikhta hai (jaise asli card milta hai). Baad mein number **masked** (last 4 digits).
**Pehle chahiye:** customer token (DEBIT ke liye apna accountNumber bhi).

### Step 7.1 — Issue card
- `POST http://localhost:8090/api/cards`  · Token: customer
- **Body — DEBIT (apna account chahiye):**
```json
{ "cardType": "DEBIT", "accountNumber": "4821009377451180", "cardholderName": "ALI KHAN" }
```
- **Body — CREDIT (account nahi; limit optional):**
```json
{ "cardType": "CREDIT", "cardholderName": "ALI KHAN", "creditLimit": 200000 }
```
- **Response (201):** `cardId`, `cardNumber` (pura), `cvv` (**sirf abhi!**). 👉 `cardId` copy karo, `cvv` safe rakho (dobara nahi milega).

### Step 7.2 — My cards (masked)
- `GET http://localhost:8090/api/cards/me`  · Token: customer

### Step 7.3 — Block / Unblock
- `POST http://localhost:8090/api/cards/<cardId>/block`  · Token: customer · Body: nahi
- `POST http://localhost:8090/api/cards/<cardId>/unblock`  · Token: customer · Body: nahi

### Step 7.4 — One card / (STAFF) all
- `GET http://localhost:8090/api/cards/<cardId>`  · owner/admin
- `GET http://localhost:8090/api/cards`  · **admin**

---

# 8️⃣ NOTIFICATION SERVICE — "Alerts"
**Role:** User ko notifications/alerts deti hai. Transfer/loan/card karte hi **khud** ban jati hain; staff manually bhi bhej sakta hai.
**Pehle chahiye:** customer token (apni notifications), admin token (send).

### Step 8.1 — My notifications
- `GET http://localhost:8090/api/notifications/me`  · Token: customer

### Step 8.2 — Unread / count
- `GET http://localhost:8090/api/notifications/me/unread`  · customer
- `GET http://localhost:8090/api/notifications/me/unread-count`  · customer → `{ "unread": 3 }`

### Step 8.3 — Mark read
- `PATCH http://localhost:8090/api/notifications/<notificationId>/read`  · customer · Body: nahi
- `PATCH http://localhost:8090/api/notifications/read-all`  · customer · Body: nahi

### Step 8.4 — (STAFF) Send notification
- `POST http://localhost:8090/api/notifications/send`  · Token: **admin**
- **Body:**
```json
{ "recipientUserId": "<kisi user ka userId>", "type": "GENERAL",
  "title": "Welcome", "message": "Aapka account ready hai." }
```
> `type` = TRANSACTION/TRANSFER/LOAN/CARD/SECURITY/GENERAL. Rate limit: 20/min per user.

---

# 🎬 PURA SAFAR — ek customer (sab jod kar)

Postman mein isi order se chalao:

1. **Register** (1.1) → `accessToken` + `userId` copy → ye **customer token**.
2. **Create profile** (2.1) → profile ban gayi (KYC PENDING).
3. **Open account A** (3.1) → `accountNumber A` copy.
4. **Open account B** (3.1 dobara) → `accountNumber B` copy.
5. **Deposit** A mein (3.4) → balance badha.
6. **Transactions dekho** (4.1) → DEPOSIT dikh raha.
7. **Transfer** A→B (5.1) → COMPLETED.
8. **Apply loan** (6.1) → `loanId` copy.
9. **Admin login** (1.3) → **admin token** copy.
10. **Approve loan** (6.2, admin token) → ACTIVE, paisa A mein.
11. **Repay loan** (6.4) → outstanding kam.
12. **Issue card** (7.1) → `cardId` + `cvv` (ek baar).
13. **Notifications dekho** (8.1) → transfer/loan/card ke alerts khud aa gaye.

✅ Pura bank chal gaya!

---

# 🚨 Common errors — turant fix

| Error | Matlab | Fix |
|---|---|---|
| `ECONNREFUSED 8090` | gateway band hai | gateway start karo (PART 0). 8080 mat karo! |
| `Invalid protocol: post http:` | URL box mein "POST" likh diya | Method left dropdown se; URL box mein sirf `http://...` |
| `401 Unauthorized` (gateway) | token nahi / galat / expire | Login dobara, naya token Headers mein lagao |
| `401` "Direct access not allowed" | seedha service-port (8082 etc.) hit kiya | Hamesha gateway `8090` se jao |
| `403` "Requires MANAGER or ADMIN" | staff endpoint, customer token | **admin token** lo (1.3) ya `/me` wali API use karo |
| `409` | email/account pehle se | dusra email, ya login karo |
| `422` | balance kam | kam amount, ya pehle deposit |
| `400` "Malformed JSON" | body galat | Body → raw → JSON, sahi `{ }` |
| "main class not found" | ▶ Run button se chalaya | terminal se `java -jar` (PART 0) |

---

## 🎫 Token cheat-sheet
- **customer token** = Register/Login (`ali@example.com`) ka `accessToken`. → apne kaam (profile, account, deposit, transfer, loan apply/repay, card, apni notifications).
- **admin token** = Login `admin@bank.local` / `Admin@12345` ka `accessToken`. → staff kaam (all lists, KYC, loan approve/reject, notification send).
- Lagana: **Headers tab → Authorization → `Bearer <token>`**.

---

*Sab `http://localhost:8090` se. `<...>` ki jagah pichli response se asli value daalo. Ready collection: `postman/Banking-Auth.postman_collection.json`. Project detail: `PROJECT_GUIDE.md`.*
