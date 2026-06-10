# 🏦 Banking System — Microservices Project Guide

> **One-line pitch:** A production-style online banking backend built as **10 independent Spring Boot microservices** (Java 17 / Spring Boot 3.3.4 / Spring Cloud), talking through an API Gateway, discovered via Eureka, secured end-to-end with JWT + a gateway-only trust model, each owning its own MySQL database.

This single document explains the **whole project** — architecture, security, every service, and **every API with its request/response JSON** — so it can be presented and defended end to end.

---

## Table of Contents
1. [What this project is](#1-what-this-project-is)
2. [Technology stack](#2-technology-stack)
3. [The 10 services at a glance](#3-the-10-services-at-a-glance)
4. [Architecture diagram](#4-architecture-diagram)
5. [How a request flows (step by step)](#5-how-a-request-flows-step-by-step)
6. [Security model (the important part)](#6-security-model)
7. [Inter-service communication](#7-inter-service-communication)
8. [COMPLETE API REFERENCE (every endpoint + JSON)](#8-complete-api-reference)
9. [Data model (databases & tables)](#9-data-model)
10. [How to build & run](#10-how-to-build--run)
11. [How to test (Postman + smoke test)](#11-how-to-test)
12. [Q&A — likely boss questions](#12-qa--likely-boss-questions)
13. [Status & roadmap](#13-status--roadmap)

---

## 1. What this project is

An online banking platform broken into small, independently deployable services. A customer can:
- **register / log in** (gets a JWT),
- **create a profile** and go through **KYC**,
- **open accounts**, **deposit / withdraw**, see **balance**,
- **transfer money** between accounts (with automatic refund if it half-fails),
- view a full **transaction history / ledger**,
- **apply for loans** (staff approve/reject, then repay),
- **issue debit/credit cards** (block/unblock),
- receive **notifications** for important events.

Staff (MANAGER / ADMIN) get extra powers: approve loans, update KYC, list all records, send notifications.

**Why microservices?** Each business area (accounts, loans, cards…) is its own service with its own database. They can be developed, deployed, and scaled independently, and one service failing doesn't take the whole bank down.

---

## 2. Technology stack

| Layer | Technology |
|---|---|
| Language / runtime | **Java 17** |
| Framework | **Spring Boot 3.3.4** (Web, Data JPA, Validation, Security on auth) |
| Cloud / microservices | **Spring Cloud 2023.0.3** — Eureka (discovery), Spring Cloud Gateway, Spring Cloud LoadBalancer |
| Auth | **JWT** (JJWT / HS256), BCrypt password hashing |
| Database | **MySQL 8** (one schema per service) |
| ORM | Spring Data JPA + Hibernate |
| Inter-service calls | `RestClient` (load-balanced via Eureka) |
| API docs | springdoc OpenAPI / Swagger UI |
| Build | **Maven** (multi-module, one module per service) |
| Boilerplate | Lombok |

**Design patterns used:** API Gateway, Service Discovery, Database-per-Service, Saga with compensating transaction (money transfer), DTO/Record pattern, centralized exception handling, gateway-injected identity (token relay).

---

## 3. The 10 services at a glance

| # | Service | Port | Database | Responsibility |
|---|---|---|---|---|
| 1 | **discovery-server** (Eureka) | 8761 | — | Service registry — every service registers here; gateway finds services by name |
| 2 | **api-gateway** | **8090** | — | Single entry point. Validates JWT, injects identity headers, stamps internal secret, routes to services |
| 3 | **auth-service** | 8081 | `auth_db` | Register / login, issues & validates JWT, seeds the admin |
| 4 | **customer-service** | 8082 | `user_db` | Customer profile + KYC |
| 5 | **account-service** | 8083 | `account_db` | Open accounts, balance, deposit, withdraw |
| 6 | **transaction-service** | 8084 | `transaction_db` | Transaction history / ledger |
| 7 | **transfer-service** | 8085 | `transfer_db` | Money transfer between accounts (saga) |
| 8 | **loan-service** | 8086 | `loan_db` | Loan apply / approve / disburse / repay |
| 9 | **card-service** | 8087 | `card_db` | Issue & manage debit/credit cards |
| 10 | **notification-service** | 8088 | `notification_db` | User alerts / notifications |

> The gateway runs on **8090** (not 8080) because Oracle's listener already uses 8080 on the host. **All client traffic goes to `http://localhost:8090`.**

---

## 4. Architecture diagram

```
                          ┌──────────────────────────────┐
                          │   CLIENT (Postman / Web / App) │
                          └───────────────┬───────────────┘
                                          │  JSON over HTTP
                                          │  Authorization: Bearer <JWT>
                                          ▼
        ┌──────────────────────────────────────────────────────────────┐
        │                  API GATEWAY  (port 8090)                       │
        │  1. validate JWT  2. inject X-User-Id / Email / Role            │
        │  3. stamp X-Internal-Secret + X-Request-Id                      │
        │  4. strip client-forged identity headers                       │
        │  5. route  lb://SERVICE-NAME  (resolved via Eureka)             │
        └───┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────────┘
            │      │      │      │      │      │      │      │
   ┌────────▼─┐ ┌──▼───┐ ┌▼─────┐ ┌▼────────┐ ┌▼──────┐ ┌▼────┐ ┌▼────┐ ┌▼──────────┐
   │  auth    │ │ cust │ │accnt │ │  txn    │ │transf │ │loan │ │card │ │notif      │
   │  8081    │ │ 8082 │ │ 8083 │ │  8084   │ │ 8085  │ │8086 │ │8087 │ │8088       │
   └────┬─────┘ └──┬───┘ └──┬───┘ └────┬────┘ └───┬───┘ └─┬───┘ └─┬───┘ └────┬──────┘
        │          │        │          │          │       │       │          │
     auth_db    user_db  account_db transaction_  transfer_ loan_  card_  notification_
                                       db          db       db     db        db
        │          │        │          │          │       │       │          │
        └──────────┴────────┴──────────┴────┬─────┴───────┴───────┴──────────┘
                                            │  (every service registers itself
                                            ▼   and looks up others by name)
                          ┌──────────────────────────────────┐
                          │  EUREKA DISCOVERY SERVER (8761)    │
                          └──────────────────────────────────┘
```

Each service is a **separate process** with its **own database**. Nothing reaches a service directly in production — everything comes through the gateway, which is the only component that trusts the outside world.

---

## 5. How a request flows (step by step)

Example: a customer deposits money — `POST /api/accounts/{accountNumber}/deposit`.

```
CLIENT ──(1)── POST http://localhost:8090/api/accounts/4821.../deposit
               Authorization: Bearer <JWT>
               { "amount": 500 }
                        │
                        ▼
API GATEWAY  (2) verify JWT signature & expiry
             (3) read claims → add headers:
                   X-User-Id: u-123   X-User-Role: CUSTOMER
                   X-Internal-Secret: ******   X-Request-Id: <uuid>
             (4) find ACCOUNT-SERVICE instance via Eureka
                        │
                        ▼
ACCOUNT-SERVICE  (5) InternalRequestFilter checks X-Internal-Secret  ✔
                 (6) controller trusts X-User-Id, updates balance in account_db
                 (7) calls TRANSACTION-SERVICE (best-effort) to record the deposit
                        │                         (secret auto-added by interceptor)
                        ▼
TRANSACTION-SERVICE  (8) stores a DEPOSIT row in transaction_db
                        │
                        ▼
            ◄── 200 OK { updated account JSON } ──
```

Key idea: **the gateway is the only thing that validates the JWT.** Downstream services trust the identity headers — *but only because the gateway also stamps a shared secret that the services check*, so the headers can't be forged by going around the gateway (see security below).

---

## 6. Security model

This is the part most likely to be questioned, so here it is in full.

### 6.1 Authentication (who are you?)
- `POST /api/auth/register` and `/login` return a **JWT** signed with HS256 (`JWT_SECRET`).
- The JWT carries `userId`, `email`, `role` and an expiry (24h default).
- The client sends it on every later request as `Authorization: Bearer <token>`.

### 6.2 Authorization (what can you do?)
- Roles: **CUSTOMER**, **MANAGER**, **ADMIN** (MANAGER/ADMIN = "staff").
- Customer endpoints need any valid token; staff endpoints check the role; owner-scoped endpoints check the resource belongs to the caller.

### 6.3 The trust model (gateway-only trust)

```
   EXTERNAL CLIENT                 GATEWAY (8090)                  SERVICE (8081-8088)
 ──────────────────      ───────────────────────────────     ─────────────────────────
  sends Bearer JWT  ──►   validates JWT                       InternalRequestFilter:
  (cannot set            strips any client X-User-* headers     • requires X-Internal-Secret
   X-Internal-Secret)    sets X-User-Id/Email/Role             • else  ⟶  401 Unauthorized
                         sets X-Internal-Secret (shared)       (only /actuator + swagger exempt)
                         sets X-Request-Id              ──►    trusts X-User-* (because the
                                                              secret proves it came via gateway)
```

- **Why services can trust headers:** the gateway adds a secret (`X-Internal-Secret`, env `INTERNAL_SHARED_SECRET`) that only the gateway and the services know. Every service runs an `InternalRequestFilter` that **rejects with 401 any request that doesn't carry the secret** — so hitting a service port directly (bypassing the gateway) with forged `X-User-Role: ADMIN` headers **fails**.
- **Service-to-service calls** (e.g. account → transaction) add the same secret automatically via a `RestClient` interceptor, so legitimate internal calls pass.

### 6.4 Registration is locked + admin is seeded
- `POST /api/auth/register` **always** creates a **CUSTOMER** (the `role` field was removed — you cannot self-register as ADMIN).
- The single **ADMIN is seeded at startup** from `ADMIN_EMAIL` / `ADMIN_PASSWORD` env (defaults `admin@bank.local` / `Admin@12345` locally). No insecure default admin is created if those are unset.

### 6.5 Other hardening
- **Rate limiting** on `POST /api/notifications/send` (per recipient, default 20/min) — stops notification spam / forged "security alert" phishing.
- **Secrets from environment** — JWT secret, internal secret, DB credentials, admin credentials are all env-driven (`.env.example` documents them); none are hard-coded.
- **No internal error leakage** — unhandled errors return a generic `"Internal server error"` (logged server-side), never raw exception text.
- **Resilience** — every inter-service call has a 2s connect / 5s read **timeout** so a hung dependency can't freeze the caller.
- **Tracing** — an `X-Request-Id` is generated at the gateway and propagated through every service log (MDC) and downstream call, so one user action can be traced across services.
- **Passwords** are stored only as **BCrypt** hashes; card **CVV is never stored** (shown once at issue); card **PAN is masked** to last-4 on every read.

---

## 7. Inter-service communication

Services mostly own their data, but a few call each other (always best-effort + resilient — a downstream outage never corrupts the core operation):

```
 account-service ──(records every deposit/withdraw)──►  transaction-service

 transfer-service ──(debit source, credit dest, refund on failure)──►  account-service
                  └─(notify sender + receiver)────────────────────►  notification-service

 loan-service     ──(disburse to / repay from account)──►  account-service
                  └─(notify on approve / reject)─────────►  notification-service

 card-service     ──(verify account ownership for DEBIT)──►  account-service
                  └─(notify on card issue)────────────────►  notification-service
```

- **Money transfer = Saga pattern.** transfer-service does: (1) debit source → (2) credit destination. If the credit fails, it **compensates** by refunding the source, then records the transfer as FAILED. There's no shared DB transaction across services, so a compensating action replaces a rollback.
- Calls are made to `http://SERVICE-NAME/...` and Eureka + Spring Cloud LoadBalancer resolve the name to a live instance.

---

## 8. COMPLETE API REFERENCE

**Base URL (all requests):** `http://localhost:8090`
**Auth:** send `Authorization: Bearer <token>` on everything except `register` / `login`.
**Note:** `X-User-Id`, `X-User-Email`, `X-User-Role` are **injected by the gateway from your JWT** — the client never sends them. Endpoints marked *Staff* require a MANAGER/ADMIN token; *Owner/Staff* require you to own the resource (or be staff).

Common error responses (same shape everywhere):
```json
{ "timestamp": "2026-06-09T10:15:30Z", "status": 403, "error": "Forbidden", "message": "Requires MANAGER or ADMIN role" }
```
| Code | When |
|---|---|
| 400 | validation failed / malformed JSON / missing header |
| 401 | no or invalid JWT (at gateway) / missing internal secret (direct-port hit) |
| 403 | not staff / not the owner |
| 404 | resource not found |
| 409 | conflict (e.g. email already registered, account not ACTIVE) |
| 422 | unprocessable (e.g. insufficient balance) |
| 429 | too many requests (notification send rate limit) |
| 500 | internal server error (generic) |

---

### 8.1 — Auth service  (`/api/auth`)  · port 8081 · public

#### `POST /api/auth/register` — create a CUSTOMER account *(public)*
Request:
```json
{ "email": "alice@bank.test", "password": "secret123" }
```
Response `201 Created`:
```json
{
  "userId": "7c9e1b34-7b1a-4f0e-9a2d-2c2b8a1f0e11",
  "email": "alice@bank.test",
  "role": "CUSTOMER",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI3Yzll... ",
  "tokenType": "Bearer"
}
```
Errors: `400` (bad email / password < 6 chars), `409` (email already registered).
> Even if you send `"role":"ADMIN"`, you still get **CUSTOMER** — registration is locked.

#### `POST /api/auth/login` — get a token *(public)*
Request:
```json
{ "email": "alice@bank.test", "password": "secret123" }
```
Response `200 OK`: same shape as register (`userId, email, role, accessToken, tokenType`).
Errors: `401` (wrong email/password), `403` (account not active).

#### `GET /api/auth/validate` — validate a token *(public)*
Header: `Authorization: Bearer <token>`
Response `200 OK`:
```json
{ "valid": true, "userId": "7c9e...", "email": "alice@bank.test", "role": "CUSTOMER" }
```

**Admin login (staff):** `POST /api/auth/login` with `{ "email": "admin@bank.local", "password": "Admin@12345" }` → token with `"role":"ADMIN"`.

---

### 8.2 — Customer service  (`/api/customers`)  · port 8082

| Method | Path | Access |
|---|---|---|
| POST | `/api/customers` | Customer (self) |
| GET | `/api/customers/me` | Customer (self) |
| PUT | `/api/customers/me` | Customer (self) |
| GET | `/api/customers` | Staff |
| GET | `/api/customers/{userId}` | Staff |
| PATCH | `/api/customers/{userId}/kyc` | Staff |

#### `POST /api/customers` — create my profile
```json
{ "firstName": "Alice", "lastName": "Khan", "phone": "03001234567",
  "address": "12 Mall Road, Lahore", "dateOfBirth": "1995-04-22" }
```
Response `201`:
```json
{
  "userId": "7c9e...", "email": "alice@bank.test",
  "firstName": "Alice", "lastName": "Khan", "phone": "03001234567",
  "address": "12 Mall Road, Lahore", "dateOfBirth": "1995-04-22",
  "kycStatus": "PENDING",
  "createdAt": "2026-06-09T10:00:00Z", "updatedAt": "2026-06-09T10:00:00Z"
}
```
#### `GET /api/customers/me` → `200` the same CustomerResponse.
#### `PUT /api/customers/me` — partial update (send only fields you change)
```json
{ "phone": "03009998888", "address": "New address" }
```
#### `GET /api/customers` *(staff)* → `200` array of CustomerResponse.
#### `GET /api/customers/{userId}` *(staff)* → `200` one CustomerResponse.
#### `PATCH /api/customers/{userId}/kyc` *(staff)* — set KYC
```json
{ "kycStatus": "VERIFIED" }
```
`kycStatus` ∈ `PENDING | VERIFIED | REJECTED`.

---

### 8.3 — Account service  (`/api/accounts`)  · port 8083

| Method | Path | Access |
|---|---|---|
| POST | `/api/accounts` | Customer |
| GET | `/api/accounts/me` | Customer |
| GET | `/api/accounts/{accountNumber}` | Owner/Staff |
| POST | `/api/accounts/{accountNumber}/deposit` | Owner/Staff |
| POST | `/api/accounts/{accountNumber}/withdraw` | Owner/Staff |
| GET | `/api/accounts` | Staff |

#### `POST /api/accounts` — open an account
```json
{ "accountType": "SAVINGS", "initialDeposit": 1000 }
```
`accountType` ∈ `SAVINGS | CURRENT`. `initialDeposit` optional (defaults 0).
Response `201`:
```json
{
  "accountNumber": "4821009377451180",
  "userId": "7c9e...", "accountType": "SAVINGS",
  "balance": 1000, "currency": "PKR", "status": "ACTIVE",
  "createdAt": "2026-06-09T10:05:00Z", "updatedAt": "2026-06-09T10:05:00Z"
}
```
#### `GET /api/accounts/me` → `200` array of AccountResponse.
#### `GET /api/accounts/{accountNumber}` *(owner/staff)* → `200` one AccountResponse.
#### `POST /api/accounts/{accountNumber}/deposit`
```json
{ "amount": 500 }
```
→ `200` AccountResponse with new balance. Errors: `400` (amount ≤ 0), `404`, `409` (account not ACTIVE).
#### `POST /api/accounts/{accountNumber}/withdraw`
```json
{ "amount": 200 }
```
→ `200`. Errors: `422` (insufficient balance), `400`, `404`, `409`.
#### `GET /api/accounts` *(staff)* → `200` all accounts.

---

### 8.4 — Transaction service  (`/api/transactions`)  · port 8084

| Method | Path | Access |
|---|---|---|
| POST | `/api/transactions/record` | Internal (account/transfer) |
| GET | `/api/transactions/me` | Customer |
| GET | `/api/transactions/account/{accountNumber}` | Owner/Staff |
| GET | `/api/transactions/{transactionId}` | Owner/Staff |
| GET | `/api/transactions` | Staff |

#### `GET /api/transactions/me` → `200` my history (newest first):
```json
[
  {
    "transactionId": "b1f2...", "accountNumber": "4821009377451180",
    "userId": "7c9e...", "type": "DEPOSIT", "amount": 500, "currency": "PKR",
    "balanceAfter": 1500, "counterpartyAccount": null, "description": "Deposit",
    "status": "SUCCESS", "createdAt": "2026-06-09T10:06:00Z"
  }
]
```
`type` ∈ `DEPOSIT | WITHDRAWAL | TRANSFER_IN | TRANSFER_OUT`; `status` ∈ `SUCCESS | FAILED | PENDING`.
#### `GET /api/transactions/account/{accountNumber}` *(owner/staff)* → `200` array.
#### `GET /api/transactions/{transactionId}` *(owner/staff)* → `200` one.
#### `GET /api/transactions` *(staff)* → `200` all.
#### `POST /api/transactions/record` *(internal)* — called by account/transfer services, not by clients:
```json
{ "accountNumber": "4821009377451180", "type": "DEPOSIT", "amount": 500,
  "balanceAfter": 1500, "counterpartyAccount": null, "description": "Deposit" }
```

---

### 8.5 — Transfer service  (`/api/transfers`)  · port 8085

| Method | Path | Access |
|---|---|---|
| POST | `/api/transfers` | Customer |
| GET | `/api/transfers/me` | Customer |
| GET | `/api/transfers/{transferId}` | Owner/Staff |
| GET | `/api/transfers` | Staff |

#### `POST /api/transfers` — send money (saga)
```json
{ "fromAccount": "4821009377451180", "toAccount": "5530771148820094", "amount": 300 }
```
Response `201`:
```json
{
  "transferId": "a7d3...", "fromAccount": "4821009377451180",
  "toAccount": "5530771148820094", "amount": 300, "currency": "PKR",
  "status": "COMPLETED", "initiatedBy": "7c9e...",
  "failureReason": null, "createdAt": "2026-06-09T10:10:00Z"
}
```
`status` ∈ `COMPLETED | FAILED`. Errors: `400` (same account / destination not found), `422` (insufficient balance), `502` (account-service unavailable → if credit step fails, source is auto-refunded and status is FAILED).
#### `GET /api/transfers/me` → `200` my transfers (newest first).
#### `GET /api/transfers/{transferId}` *(owner/staff)* → `200` one.
#### `GET /api/transfers` *(staff)* → `200` all.

---

### 8.6 — Loan service  (`/api/loans`)  · port 8086

| Method | Path | Access |
|---|---|---|
| POST | `/api/loans` | Customer |
| GET | `/api/loans/me` | Customer |
| GET | `/api/loans/{loanId}` | Owner/Staff |
| POST | `/api/loans/{loanId}/repay` | Customer (owner) |
| PATCH | `/api/loans/{loanId}/approve` | Staff |
| PATCH | `/api/loans/{loanId}/reject` | Staff |
| GET | `/api/loans` | Staff |

#### `POST /api/loans` — apply
```json
{ "accountNumber": "4821009377451180", "principal": 50000,
  "termMonths": 12, "interestRate": 10, "purpose": "Home renovation" }
```
`principal` ≥ 1000; `termMonths` 1–360; `interestRate` optional (defaults 10%). Interest is **simple**: `totalPayable = principal + principal*rate/100*termMonths/12`.
Response `201`:
```json
{
  "loanId": "ln-91a2...", "userId": "7c9e...", "accountNumber": "4821009377451180",
  "principal": 50000, "interestRate": 10, "termMonths": 12,
  "totalPayable": 55000, "outstanding": 55000, "purpose": "Home renovation",
  "status": "PENDING", "decisionReason": null,
  "createdAt": "2026-06-09T10:12:00Z", "updatedAt": "2026-06-09T10:12:00Z"
}
```
`status` ∈ `PENDING | REJECTED | ACTIVE | CLOSED`.
#### `GET /api/loans/me` → `200` my loans.
#### `GET /api/loans/{loanId}` *(owner/staff)* → `200` one.
#### `POST /api/loans/{loanId}/repay` — repay from your account
```json
{ "amount": 5000 }
```
→ `200` loan with reduced `outstanding` (status → CLOSED when it hits 0).
#### `PATCH /api/loans/{loanId}/approve` *(staff)* — approve + disburse principal to the account → status `ACTIVE`. (no body)
#### `PATCH /api/loans/{loanId}/reject` *(staff)* — optional reason:
```json
{ "reason": "Insufficient income proof" }
```
→ status `REJECTED`.
#### `GET /api/loans` *(staff)* → `200` all.

---

### 8.7 — Card service  (`/api/cards`)  · port 8087

| Method | Path | Access |
|---|---|---|
| POST | `/api/cards` | Customer |
| GET | `/api/cards/me` | Customer |
| GET | `/api/cards/{cardId}` | Owner/Staff |
| POST | `/api/cards/{cardId}/block` | Owner/Staff |
| POST | `/api/cards/{cardId}/unblock` | Owner/Staff |
| GET | `/api/cards` | Staff |

#### `POST /api/cards` — issue a card
DEBIT (needs your account):
```json
{ "cardType": "DEBIT", "accountNumber": "4821009377451180", "cardholderName": "ALICE KHAN" }
```
CREDIT (no account; optional limit):
```json
{ "cardType": "CREDIT", "cardholderName": "ALICE KHAN", "creditLimit": 200000 }
```
Response `201` — **full number + CVV shown ONCE** (never again, CVV never stored):
```json
{
  "cardId": "cd-55b1...", "cardNumber": "4517382946651180", "cvv": "418",
  "cardholderName": "ALICE KHAN", "cardType": "DEBIT",
  "status": "ACTIVE", "expiryDate": "2030-06-30"
}
```
#### `GET /api/cards/me` → `200` my cards (**masked**):
```json
[
  {
    "cardId": "cd-55b1...", "maskedNumber": "**** **** **** 1180",
    "userId": "7c9e...", "accountNumber": "4821009377451180",
    "cardType": "DEBIT", "cardholderName": "ALICE KHAN", "status": "ACTIVE",
    "expiryDate": "2030-06-30", "creditLimit": null,
    "createdAt": "2026-06-09T10:14:00Z", "updatedAt": "2026-06-09T10:14:00Z"
  }
]
```
`cardType` ∈ `DEBIT | CREDIT`; `status` ∈ `ACTIVE | BLOCKED | EXPIRED` (EXPIRED is derived at read time from `expiryDate`).
#### `GET /api/cards/{cardId}` *(owner/staff)* → `200` masked card.
#### `POST /api/cards/{cardId}/block` / `…/unblock` *(owner/staff)* → `200` updated card. (no body)
#### `GET /api/cards` *(staff)* → `200` all (masked).

---

### 8.8 — Notification service  (`/api/notifications`)  · port 8088

| Method | Path | Access |
|---|---|---|
| POST | `/api/notifications/send` | Staff / internal |
| GET | `/api/notifications/me` | Customer |
| GET | `/api/notifications/me/unread` | Customer |
| GET | `/api/notifications/me/unread-count` | Customer |
| PATCH | `/api/notifications/{notificationId}/read` | Owner |
| PATCH | `/api/notifications/read-all` | Owner |
| GET | `/api/notifications` | Staff |

#### `POST /api/notifications/send` *(staff/internal, rate-limited 20/min per recipient)*
```json
{ "recipientUserId": "7c9e...", "type": "GENERAL",
  "title": "Welcome", "message": "Your account is ready." }
```
`type` ∈ `TRANSACTION | TRANSFER | LOAN | CARD | SECURITY | GENERAL`.
Response `201`:
```json
{
  "notificationId": "nt-77c2...", "userId": "7c9e...", "type": "GENERAL",
  "title": "Welcome", "message": "Your account is ready.",
  "status": "UNREAD", "createdAt": "2026-06-09T10:16:00Z", "readAt": null
}
```
Errors: `429` (rate limit exceeded).
#### `GET /api/notifications/me` → `200` my notifications (newest first).
#### `GET /api/notifications/me/unread` → `200` only unread.
#### `GET /api/notifications/me/unread-count` → `200`:
```json
{ "unread": 3 }
```
#### `PATCH /api/notifications/{notificationId}/read` *(owner)* → `200` notification with `status:"READ"`.
#### `PATCH /api/notifications/read-all` *(owner)* → `200`:
```json
{ "markedRead": 3 }
```
#### `GET /api/notifications` *(staff)* → `200` all.

---

## 9. Data model

One database per service (database-per-service pattern). Main tables / key columns:

| Database | Table | Key columns |
|---|---|---|
| `auth_db` | `user_credential` | userId (PK), email (unique), passwordHash (BCrypt), role, status |
| `user_db` | `customer` | userId, email, firstName, lastName, phone, address, dateOfBirth, kycStatus |
| `account_db` | `account` | accountNumber (16-digit), userId, accountType, balance, currency, status |
| `transaction_db` | `transaction` | transactionId, accountNumber, userId, type, amount, balanceAfter, counterpartyAccount, status, createdAt |
| `transfer_db` | `transfer` | transferId, fromAccount, toAccount, amount, status, initiatedBy, failureReason |
| `loan_db` | `loan` | loanId, userId, accountNumber, principal, interestRate, termMonths, totalPayable, outstanding, status |
| `card_db` | `card` | cardId, cardNumber (unique, masked on read), userId, accountNumber, cardType, status, expiryDate, creditLimit |
| `notification_db` | `notification` | notificationId, userId, type, title, message, status, createdAt, readAt (indexed by user+createdAt and user+status) |

- Money is stored as `BigDecimal` (precision 19, scale 2). Currency is `PKR`.
- Schema is auto-created by Hibernate (`ddl-auto: update`) for development. *(Production would switch to Flyway migrations + `validate`.)*

**Enum cheat-sheet:**
`Role`: CUSTOMER, MANAGER, ADMIN · `AccountType`: SAVINGS, CURRENT · `AccountStatus`: ACTIVE, CLOSED, FROZEN ·
`TransactionType`: DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT · `TransferStatus`: COMPLETED, FAILED ·
`LoanStatus`: PENDING, REJECTED, ACTIVE, CLOSED · `CardType`: DEBIT, CREDIT · `CardStatus`: ACTIVE, BLOCKED, EXPIRED ·
`KycStatus`: PENDING, VERIFIED, REJECTED · `NotificationType`: TRANSACTION, TRANSFER, LOAN, CARD, SECURITY, GENERAL · `NotificationStatus`: UNREAD, READ.

---

## 10. How to build & run

**Prerequisites (this machine):** Java 17 (`C:\Program Files\Java\jdk-17`), Maven 3.9.9 (`E:\tools\apache-maven-3.9.9`), MySQL 8 running on `localhost:3306`.

**Build all services into runnable jars (once):**
```powershell
.\build-jars.ps1
```

**Run everything (memory-efficient, recommended on this 8 GB box):**
```powershell
.\run-jars.ps1
```
This starts each service as a single `java -Xmx256m -jar`, in order, waiting for each to come up. Gateway is at `http://localhost:8090`.

**Secrets (set as env vars in production; local-dev fallbacks otherwise — see `.env.example`):**
`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `INTERNAL_SHARED_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`.

> The 8 GB host runs MySQL + Oracle + IDE, so running all 10 services as `mvn spring-boot:run` (two JVMs each) exhausts RAM. The lean jar runner fits the whole system in memory. To free more room, stop Oracle if unused, or set `$env:SERVICE_MAX_HEAP='384m'`.

---

## 11. How to test

**Smoke test (one command, 36 checks):**
```powershell
.\smoke-test-core.ps1
```
It exercises auth + accounts + transfers + all the security rules (register-lock, seeded-admin staff access, direct-port bypass → 401, etc.) and prints PASS/FAIL with a summary. Current result: **36/36 pass**.

**Postman collection:** `postman/Banking-Auth.postman_collection.json`
- Import it, set the collection/environment variable `baseUrl = http://localhost:8090`.
- Folders: **1. Auth · 2. Customer · 3. Account · 4. Transaction · 5. Transfer · 6. Loan · 7. Card · 8. Notification** — one request per endpoint above.
- It auto-saves the token, userId, accountNumber etc. between requests so you can run the whole flow top-to-bottom.
- For staff/admin requests, first run **Auth → Login as admin** (`admin@bank.local` / `Admin@12345`) to capture the admin token.

**Swagger UI** per service, e.g. `http://localhost:8081/swagger-ui.html` (auth), 8082, 8083, …

---

## 12. Q&A — likely boss questions

**Q: Why microservices and not one application?**
Each business domain (accounts, loans, cards…) is independent — separately deployable, separately scalable, and isolated failures. It mirrors how real banks structure systems.

**Q: How do services find each other?**
Netflix Eureka (discovery-server, 8761). Each service registers on startup; callers use the logical name (`lb://ACCOUNT-SERVICE`) and Spring Cloud LoadBalancer picks a live instance.

**Q: How is it secured?**
JWT at the edge (gateway validates once and injects identity). Services don't re-validate the JWT; instead they trust gateway-injected headers **only because the gateway also stamps a shared secret that each service verifies** — so you can't bypass the gateway and forge an admin role. Registration is locked to CUSTOMER; the admin is seeded from env. Passwords are BCrypt; card CVV is never stored; PAN is masked. (See section 6.)

**Q: What happens if a transfer half-fails?**
Saga with compensation: debit source → credit destination; if the credit fails, the source is **automatically refunded** and the transfer is recorded as FAILED. No money is lost.

**Q: What if a downstream service is down?**
Inter-service calls are best-effort with 2s/5s timeouts. A notification or transaction-record failure logs a warning but never fails the core banking operation.

**Q: Where's the data?**
Database-per-service — 8 MySQL schemas, one per service, so no service reaches into another's tables.

**Q: Is it production-ready?**
The application layer is hardened and verified (36/36 automated checks). The remaining items are infrastructure-level (containers, message broker, HTTPS/mTLS, DB migrations, central metrics) — see the roadmap.

**Q: How do you trace one user action across services?**
An `X-Request-Id` is generated at the gateway and propagated through every service's logs and onward calls.

---

## 13. Status & roadmap

**Done (application level):**
✅ 10 services (8 business + discovery + gateway), all running and tested
✅ JWT auth + role-based access + gateway-only trust (shared secret)
✅ Registration lock + seeded admin
✅ Saga money transfer with compensation
✅ Auto transaction ledger, event notifications
✅ Rate limiting, secrets-from-env, no error leakage, request timeouts, request-id tracing
✅ 36/36 automated end-to-end + security checks passing

**Roadmap (infrastructure level — needs Docker/more RAM):**
⬜ Containerize with Docker Compose
⬜ Replace synchronous notify calls with Kafka/RabbitMQ events
⬜ Redis (caching + multi-instance rate limiting)
⬜ Centralized config server
⬜ Observability: Prometheus + Grafana + Zipkin (distributed tracing)
⬜ HTTPS/TLS termination + service-to-service mTLS
⬜ Flyway DB migrations with `ddl-auto: validate`
⬜ CI/CD pipeline

---

*Generated for the Banking System microservices project. Base URL `http://localhost:8090`. For the resume/run guide see `START_HERE.md`; for the API collection see `postman/Banking-Auth.postman_collection.json`.*
