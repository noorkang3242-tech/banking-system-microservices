# 📮 Postman Guide — Har API, uska kaam, JSON data + Postman mein kahan dalein

> Ye file practical hai: **har API** ka kaam, uska **JSON body**, aur Postman mein **kahan paste karna hai** (URL bar / Headers / Body).
> Saari requests gateway se jaati hain: **`http://localhost:8090`**

---

## 🔧 PEHLE YE PADHO — Postman mein kaise dalein (ek baar samajh lo)

Har request mein 3 jagah cheezein dalti hain:

1. **Method + URL** → Postman ke upar: left dropdown se method (GET/POST/PUT/PATCH) choose karo, aur URL bar mein URL paste karo.
   - Example: method `POST`, URL `http://localhost:8090/api/auth/register`

2. **Token (Authorization)** → jin APIs pe likha hai "Auth chahiye":
   - Request ke **Headers** tab mein ek row add karo:
     - **Key:** `Authorization`   **Value:** `Bearer <yaha token paste karo>`
   - (Ya **Authorization** tab → Type: **Bearer Token** → token paste karo — dono same.)

3. **JSON body** → jin APIs ka "Body" diya hai:
   - **Body** tab → **raw** select karo → right side dropdown **JSON** choose karo → neeche box mein JSON paste karo.
   - (JSON choose karte hi Postman khud `Content-Type: application/json` laga deta hai.)

### Token kahan se aata hai?
- **Customer token:** `Register` ya `Login` ki response mein `accessToken` aata hai — usko copy karke baaki customer requests ke Authorization mein lagao.
- **Admin token (staff kaam ke liye):** `Login` karo `admin@bank.local` / `Admin@12345` se — uska `accessToken` admin token hai (role = ADMIN).

### Run order (sasta tareeka)
1. Register / Login (customer token milega) → 2. Customer profile → 3. Account → 4. Transaction → 5. Transfer → 6. Loan → 7. Card → 8. Notification.
Staff (admin) wali requests ke liye admin token use karo.

> **Tip:** Tokens, accountNumber waghaira baar baar copy na karna pade — iske liye ready collection import karo:
> `postman/Banking-Auth.postman_collection.json` (variables khud save hote hain). Ye file usko samajhne ke liye hai.

---

# 1️⃣ AUTH (login / register) — port 8081 — token yahin se milta hai

### 1.1 Register (naya customer banao)
- **Kaam:** Naya customer account banata hai aur turant ek JWT token deta hai. (Note: register hamesha CUSTOMER banata hai.)
- **Method + URL:** `POST  http://localhost:8090/api/auth/register`
- **Auth:** nahi chahiye (public)
- **Body (raw → JSON):**
```json
{
  "email": "ali@example.com",
  "password": "secret123"
}
```
- **Response (201):** `accessToken` (= token), `userId`, `email`, `role: CUSTOMER`. → **Is `accessToken` ko copy karo, aage use hoga.**

### 1.2 Login (token lo)
- **Kaam:** Email + password se login, token wapas. (Customer ya admin — dono isi se.)
- **Method + URL:** `POST  http://localhost:8090/api/auth/login`
- **Auth:** nahi chahiye
- **Body (raw → JSON):**
```json
{
  "email": "ali@example.com",
  "password": "secret123"
}
```
- **Admin token ke liye** body:
```json
{
  "email": "admin@bank.local",
  "password": "Admin@12345"
}
```
- **Response (200):** `accessToken` + `role`. Galat password = `401`.

### 1.3 Validate token
- **Kaam:** Token sahi hai ya nahi check karta hai.
- **Method + URL:** `GET  http://localhost:8090/api/auth/validate`
- **Auth (Headers):** `Authorization: Bearer <token>`
- **Body:** nahi
- **Response (200):** `{ "valid": true, "userId": "...", "email": "...", "role": "CUSTOMER" }`

---

# 2️⃣ CUSTOMER (profile + KYC) — port 8082

### 2.1 Create my profile
- **Kaam:** Apni customer profile banata hai (naam, phone, address). KYC PENDING se shuru.
- **Method + URL:** `POST  http://localhost:8090/api/customers`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
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
- **Response (201):** profile + `kycStatus: PENDING`.

### 2.2 Get my profile
- **Kaam:** Apni profile dekho.
- **Method + URL:** `GET  http://localhost:8090/api/customers/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body:** nahi

### 2.3 Update my profile
- **Kaam:** Profile update (sirf wo fields bhejo jo badalni hain).
- **Method + URL:** `PUT  http://localhost:8090/api/customers/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body (raw → JSON):**
```json
{
  "phone": "03009998888",
  "address": "Naya address, Karachi"
}
```

### 2.4 List all customers — STAFF
- **Kaam:** Saare customers ki list (sirf staff/admin).
- **Method + URL:** `GET  http://localhost:8090/api/customers`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`
- **Body:** nahi  · Customer token = `403`.

### 2.5 Get customer by userId — STAFF
- **Kaam:** Kisi ek customer ko userId se dekho.
- **Method + URL:** `GET  http://localhost:8090/api/customers/<userId>`  (URL mein userId daalo)
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

### 2.6 Update KYC — STAFF
- **Kaam:** Customer ka KYC status badlo.
- **Method + URL:** `PATCH  http://localhost:8090/api/customers/<userId>/kyc`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`
- **Body (raw → JSON):**
```json
{ "kycStatus": "VERIFIED" }
```
> `kycStatus` = `PENDING` ya `VERIFIED` ya `REJECTED`.

---

# 3️⃣ ACCOUNT (account khulna, balance, deposit, withdraw) — port 8083

### 3.1 Open account
- **Kaam:** Naya bank account kholta hai. `initialDeposit` optional.
- **Method + URL:** `POST  http://localhost:8090/api/accounts`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body (raw → JSON):**
```json
{
  "accountType": "SAVINGS",
  "initialDeposit": 1000
}
```
> `accountType` = `SAVINGS` ya `CURRENT`.
- **Response (201):** `accountNumber` (16 digits), `balance`, `status: ACTIVE`. → **`accountNumber` copy karo.**

### 3.2 My accounts
- **Kaam:** Apne saare accounts dekho.
- **Method + URL:** `GET  http://localhost:8090/api/accounts/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 3.3 Get one account (balance)
- **Kaam:** Ek account ka detail + balance.
- **Method + URL:** `GET  http://localhost:8090/api/accounts/<accountNumber>`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 3.4 Deposit (paisa daalo)
- **Kaam:** Account mein paisa add karta hai.
- **Method + URL:** `POST  http://localhost:8090/api/accounts/<accountNumber>/deposit`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body (raw → JSON):**
```json
{ "amount": 500 }
```

### 3.5 Withdraw (paisa nikalo)
- **Kaam:** Account se paisa nikalta hai (balance kam ho to `422`).
- **Method + URL:** `POST  http://localhost:8090/api/accounts/<accountNumber>/withdraw`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body (raw → JSON):**
```json
{ "amount": 200 }
```

### 3.6 List all accounts — STAFF
- **Kaam:** Bank ke saare accounts (admin only).
- **Method + URL:** `GET  http://localhost:8090/api/accounts`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

---

# 4️⃣ TRANSACTION (history / ledger) — port 8084

> Note: deposit/withdraw/transfer karte hi transaction **khud record** ho jaati hai. Yahan sirf dekhte hain.

### 4.1 My transactions
- **Kaam:** Apni saari transactions (nayi pehle).
- **Method + URL:** `GET  http://localhost:8090/api/transactions/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 4.2 Transactions of an account
- **Kaam:** Ek account ki transactions.
- **Method + URL:** `GET  http://localhost:8090/api/transactions/account/<accountNumber>`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 4.3 Get one transaction
- **Kaam:** Ek transaction id se.
- **Method + URL:** `GET  http://localhost:8090/api/transactions/<transactionId>`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 4.4 List all transactions — STAFF
- **Method + URL:** `GET  http://localhost:8090/api/transactions`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

---

# 5️⃣ TRANSFER (paisa bhejna) — port 8085

### 5.1 Transfer money
- **Kaam:** Ek account se doosre mein paisa bhejta hai (agar beech mein fail ho to source ko paisa wapas).
- **Method + URL:** `POST  http://localhost:8090/api/transfers`
- **Auth (Headers):** `Authorization: Bearer <customer token>` (jiske paas `fromAccount` hai)
- **Body (raw → JSON):**
```json
{
  "fromAccount": "4821009377451180",
  "toAccount": "5530771148820094",
  "amount": 300
}
```
- **Response (201):** `status: COMPLETED`. Same account = `400`, balance kam = `422`.

### 5.2 My transfers
- **Method + URL:** `GET  http://localhost:8090/api/transfers/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 5.3 Get one transfer
- **Method + URL:** `GET  http://localhost:8090/api/transfers/<transferId>`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 5.4 List all transfers — STAFF
- **Method + URL:** `GET  http://localhost:8090/api/transfers`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

---

# 6️⃣ LOAN (loan apply / approve / repay) — port 8086

### 6.1 Apply for loan
- **Kaam:** Loan ke liye apply (PENDING banta hai, staff approve karega).
- **Method + URL:** `POST  http://localhost:8090/api/loans`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body (raw → JSON):**
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
- **Response (201):** `loanId`, `totalPayable`, `status: PENDING`. → **`loanId` copy karo.**

### 6.2 My loans
- **Method + URL:** `GET  http://localhost:8090/api/loans/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 6.3 Get one loan
- **Method + URL:** `GET  http://localhost:8090/api/loans/<loanId>`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 6.4 Approve + disburse — STAFF
- **Kaam:** Loan approve karke principal account mein daal deta hai (status ACTIVE).
- **Method + URL:** `PATCH  http://localhost:8090/api/loans/<loanId>/approve`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`
- **Body:** nahi

### 6.5 Reject — STAFF
- **Kaam:** Loan reject (reason optional).
- **Method + URL:** `PATCH  http://localhost:8090/api/loans/<loanId>/reject`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`
- **Body (raw → JSON, optional):**
```json
{ "reason": "Income proof kam hai" }
```

### 6.6 Repay loan
- **Kaam:** Active loan ki repayment account se (outstanding 0 ho to CLOSED).
- **Method + URL:** `POST  http://localhost:8090/api/loans/<loanId>/repay`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body (raw → JSON):**
```json
{ "amount": 5000 }
```

### 6.7 List all loans — STAFF
- **Method + URL:** `GET  http://localhost:8090/api/loans`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

---

# 7️⃣ CARD (debit/credit card) — port 8087

### 7.1 Issue card
- **Kaam:** Naya card banata hai. Pura number + CVV **sirf ek baar** milta hai (dobara nahi).
- **Method + URL:** `POST  http://localhost:8090/api/cards`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Body — DEBIT (apna account chahiye):**
```json
{
  "cardType": "DEBIT",
  "accountNumber": "4821009377451180",
  "cardholderName": "ALI KHAN"
}
```
- **Body — CREDIT (account nahi; limit optional):**
```json
{
  "cardType": "CREDIT",
  "cardholderName": "ALI KHAN",
  "creditLimit": 200000
}
```
- **Response (201):** `cardId`, `cardNumber` (pura), `cvv` (sirf abhi). → **`cardId` copy karo.**

### 7.2 My cards (masked)
- **Kaam:** Apne cards (number masked — sirf last 4 digits).
- **Method + URL:** `GET  http://localhost:8090/api/cards/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 7.3 Get one card
- **Method + URL:** `GET  http://localhost:8090/api/cards/<cardId>`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 7.4 Block card
- **Kaam:** Card block.
- **Method + URL:** `POST  http://localhost:8090/api/cards/<cardId>/block`
- **Auth (Headers):** `Authorization: Bearer <customer token>`  · **Body:** nahi

### 7.5 Unblock card
- **Method + URL:** `POST  http://localhost:8090/api/cards/<cardId>/unblock`
- **Auth (Headers):** `Authorization: Bearer <customer token>`  · **Body:** nahi

### 7.6 List all cards — STAFF
- **Method + URL:** `GET  http://localhost:8090/api/cards`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

---

# 8️⃣ NOTIFICATION (alerts) — port 8088

> Transfer / loan-approve / card-issue pe notifications **khud** ban jaati hain. `send` sirf staff ke liye.

### 8.1 Send notification — STAFF
- **Kaam:** Kisi user ko notification bhejta hai (rate limit: 20/min per user).
- **Method + URL:** `POST  http://localhost:8090/api/notifications/send`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`
- **Body (raw → JSON):**
```json
{
  "recipientUserId": "<kisi user ka userId>",
  "type": "GENERAL",
  "title": "Welcome",
  "message": "Aapka account ready hai."
}
```
> `type` = `TRANSACTION` / `TRANSFER` / `LOAN` / `CARD` / `SECURITY` / `GENERAL`.

### 8.2 My notifications
- **Method + URL:** `GET  http://localhost:8090/api/notifications/me`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 8.3 My unread
- **Method + URL:** `GET  http://localhost:8090/api/notifications/me/unread`
- **Auth (Headers):** `Authorization: Bearer <customer token>`

### 8.4 Unread count
- **Method + URL:** `GET  http://localhost:8090/api/notifications/me/unread-count`
- **Auth (Headers):** `Authorization: Bearer <customer token>`
- **Response:** `{ "unread": 3 }`

### 8.5 Mark one read
- **Method + URL:** `PATCH  http://localhost:8090/api/notifications/<notificationId>/read`
- **Auth (Headers):** `Authorization: Bearer <customer token>`  · **Body:** nahi

### 8.6 Mark all read
- **Method + URL:** `PATCH  http://localhost:8090/api/notifications/read-all`
- **Auth (Headers):** `Authorization: Bearer <customer token>`  · **Body:** nahi

### 8.7 List all — STAFF
- **Method + URL:** `GET  http://localhost:8090/api/notifications`
- **Auth (Headers):** `Authorization: Bearer <ADMIN token>`

---

## ✅ Quick reference — saari APIs ek nazar mein

| # | Kaam | Method | URL (baad mein) | Auth | Body? |
|---|---|---|---|---|---|
| 1 | Register | POST | /api/auth/register | — | ✅ email,password |
| 2 | Login | POST | /api/auth/login | — | ✅ email,password |
| 3 | Validate | GET | /api/auth/validate | token | — |
| 4 | Create profile | POST | /api/customers | token | ✅ |
| 5 | My profile | GET | /api/customers/me | token | — |
| 6 | Update profile | PUT | /api/customers/me | token | ✅ |
| 7 | All customers | GET | /api/customers | admin | — |
| 8 | Customer by id | GET | /api/customers/{userId} | admin | — |
| 9 | Update KYC | PATCH | /api/customers/{userId}/kyc | admin | ✅ kycStatus |
| 10 | Open account | POST | /api/accounts | token | ✅ accountType |
| 11 | My accounts | GET | /api/accounts/me | token | — |
| 12 | One account | GET | /api/accounts/{accountNumber} | token | — |
| 13 | Deposit | POST | /api/accounts/{accountNumber}/deposit | token | ✅ amount |
| 14 | Withdraw | POST | /api/accounts/{accountNumber}/withdraw | token | ✅ amount |
| 15 | All accounts | GET | /api/accounts | admin | — |
| 16 | My transactions | GET | /api/transactions/me | token | — |
| 17 | Account txns | GET | /api/transactions/account/{accountNumber} | token | — |
| 18 | One txn | GET | /api/transactions/{transactionId} | token | — |
| 19 | All txns | GET | /api/transactions | admin | — |
| 20 | Transfer | POST | /api/transfers | token | ✅ from,to,amount |
| 21 | My transfers | GET | /api/transfers/me | token | — |
| 22 | One transfer | GET | /api/transfers/{transferId} | token | — |
| 23 | All transfers | GET | /api/transfers | admin | — |
| 24 | Apply loan | POST | /api/loans | token | ✅ |
| 25 | My loans | GET | /api/loans/me | token | — |
| 26 | One loan | GET | /api/loans/{loanId} | token | — |
| 27 | Approve loan | PATCH | /api/loans/{loanId}/approve | admin | — |
| 28 | Reject loan | PATCH | /api/loans/{loanId}/reject | admin | (optional) |
| 29 | Repay loan | POST | /api/loans/{loanId}/repay | token | ✅ amount |
| 30 | All loans | GET | /api/loans | admin | — |
| 31 | Issue card | POST | /api/cards | token | ✅ cardType |
| 32 | My cards | GET | /api/cards/me | token | — |
| 33 | One card | GET | /api/cards/{cardId} | token | — |
| 34 | Block card | POST | /api/cards/{cardId}/block | token | — |
| 35 | Unblock card | POST | /api/cards/{cardId}/unblock | token | — |
| 36 | All cards | GET | /api/cards | admin | — |
| 37 | Send notif | POST | /api/notifications/send | admin | ✅ |
| 38 | My notifs | GET | /api/notifications/me | token | — |
| 39 | My unread | GET | /api/notifications/me/unread | token | — |
| 40 | Unread count | GET | /api/notifications/me/unread-count | token | — |
| 41 | Mark read | PATCH | /api/notifications/{notificationId}/read | token | — |
| 42 | Mark all read | PATCH | /api/notifications/read-all | token | — |
| 43 | All notifs | GET | /api/notifications | admin | — |

> **"token"** = customer ka accessToken (Register/Login se).  **"admin"** = `admin@bank.local` / `Admin@12345` login ka accessToken.
> Har URL ke aage `http://localhost:8090` lagao. `{...}` waali jagah par asli id/number daalo.
