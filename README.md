# 🏦 Banking System — Microservices Platform

> A production-style online banking backend built as **10 independent Spring Boot microservices** with an API Gateway, service discovery, JWT security, the saga pattern, and a database-per-service. Fully tested (functional + security + load).

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Maven](https://img.shields.io/badge/Build-Maven-red)
![Tests](https://img.shields.io/badge/Smoke%20Tests-36%2F36%20passing-success)

---

## 📖 Overview

A digital banking platform decomposed into small, independently deployable services. Customers can register, manage a KYC profile, open accounts, deposit/withdraw, transfer money (with automatic refund on failure), apply for loans, issue debit/credit cards, and receive notifications. Staff (MANAGER/ADMIN) get extra capabilities such as loan approval, KYC verification, and system-wide listing.

**Why microservices?** Each business domain (accounts, loans, cards…) is its own service with its own database, so they can be developed, deployed, and scaled independently, and a single failure does not take down the whole bank.

---

## ✨ Features

- 🔐 **JWT authentication** + role-based access (CUSTOMER / MANAGER / ADMIN)
- 🏦 **Accounts** — open, balance, deposit, withdraw
- 💸 **Money transfer** — saga pattern with automatic compensation (refund on partial failure)
- 📒 **Transaction ledger** — every money movement auto-recorded
- 💳 **Cards** — debit/credit issuance (CVV shown once, PAN masked), block/unblock
- 🧾 **Loans** — apply → staff approve/reject → disburse → repay
- 🔔 **Notifications** — event-driven alerts from transfers, loans, and cards
- 🛡️ **Production hardening** — gateway-only trust (shared secret), locked registration + seeded admin, rate limiting, externalized secrets, request tracing, resilience timeouts

---

## 🏗️ Architecture

```
                          ┌──────────────────────────────┐
                          │   Client (Postman / Web / App) │
                          └───────────────┬───────────────┘
                                          │  JSON + Bearer JWT
                                          ▼
        ┌──────────────────────────────────────────────────────────────┐
        │                  API GATEWAY  (port 8090)                       │
        │  validate JWT · inject X-User-* · stamp shared secret + req-id │
        │  route  lb://SERVICE-NAME  (resolved via Eureka)               │
        └───┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬───────────┘
            ▼      ▼      ▼      ▼      ▼      ▼      ▼      ▼
          auth   cust  account  txn  transfer loan  card  notification
          8081   8082   8083   8084   8085   8086  8087     8088
            │      │      │      │      │      │      │        │
         auth_db user_db acct_db txn_db xfer_db loan_db card_db notif_db
                                          │
                          ┌───────────────▼───────────────┐
                          │  EUREKA DISCOVERY SERVER (8761) │
                          └────────────────────────────────┘
```

Each service is a separate process with its own MySQL schema. Nothing reaches a service directly in production — everything flows through the gateway, the only component that trusts the outside world.

---

## 🧩 Services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| discovery-server (Eureka) | 8761 | — | Service registry |
| api-gateway | **8090** | — | Single entry point: JWT validation, identity injection, routing |
| auth-service | 8081 | `auth_db` | Register / login, issue & validate JWT, seed admin |
| customer-service | 8082 | `user_db` | Customer profile + KYC |
| account-service | 8083 | `account_db` | Accounts, balance, deposit, withdraw |
| transaction-service | 8084 | `transaction_db` | Transaction history / ledger |
| transfer-service | 8085 | `transfer_db` | Money transfer (saga) |
| loan-service | 8086 | `loan_db` | Loan apply / approve / disburse / repay |
| card-service | 8087 | `card_db` | Debit/credit card issuance & management |
| notification-service | 8088 | `notification_db` | User alerts |

**Inter-service flows:** account → transaction (auto-record); transfer/loan/card → account; transfer/loan/card → notification (events). Calls are load-balanced via Eureka and resilient (2 s connect / 5 s read timeouts, best-effort).

---

## 🛠️ Tech Stack

**Java 17** · **Spring Boot 3.3.4** · **Spring Cloud 2023.0.3** (Eureka, Gateway, LoadBalancer) · Spring Security · **JWT** (jjwt, HS256) · Spring Data JPA / Hibernate · **MySQL 8** · Lombok · springdoc OpenAPI / Swagger · **Maven** · load-tested with **k6**.

**Patterns:** API Gateway · Service Discovery · Database-per-Service · Saga with compensation · DTO/Record · Centralized exception handling · Gateway-injected identity (token relay).

---

## 🔐 Security Highlights

- **Gateway-only trust:** the gateway stamps a shared secret (`X-Internal-Secret`) on every forwarded request and strips client-supplied identity headers. Each service rejects (401) any direct-port request lacking the secret — so the gateway cannot be bypassed with forged `X-User-Role: ADMIN` headers.
- **Locked registration + seeded admin:** `/api/auth/register` always creates a CUSTOMER. The admin is seeded at startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD` — no self-registration as ADMIN.
- **Rate limiting** on notification send · **BCrypt** password hashing · **CVV never stored** (shown once) · **PAN masked** to last-4 · **secrets from environment** (`.env.example`) · **request-id tracing** across services.

---

## 🚀 Getting Started

### Prerequisites
- Java 17, Maven 3.9+, MySQL 8 (running on `localhost:3306`)
- Set your DB password and other secrets via environment variables (see [`.env.example`](.env.example)) — the scripts use placeholders by default.

### Build
```bash
# build every service into a runnable jar
./build-jars.ps1      # Windows PowerShell
# or per module:  mvn -DskipTests package
```

### Run
```powershell
# memory-friendly: each service as one lean JVM, started in order
$env:DB_PASSWORD = "your_mysql_password"
.\run-jars.ps1
```
Gateway comes up at **http://localhost:8090**. Default seeded admin: `admin@bank.local` / `Admin@12345` (override via env).

> Full run/troubleshooting guide: [`START_HERE.md`](START_HERE.md). Step-by-step usage of every service: [`STEP_BY_STEP_GUIDE.md`](STEP_BY_STEP_GUIDE.md).

---

## 📡 API (quick taste)

```bash
# 1) Register -> returns a JWT
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"ali@example.com","password":"secret123"}'

# 2) Open an account (use the token from step 1)
curl -X POST http://localhost:8090/api/accounts \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"accountType":"SAVINGS","initialDeposit":1000}'

# 3) Transfer money
curl -X POST http://localhost:8090/api/transfers \
  -H "Authorization: Bearer <token>" -H "Content-Type: application/json" \
  -d '{"fromAccount":"<A>","toAccount":"<B>","amount":300}'
```

**Every endpoint** (request + response JSON) is documented in [`POSTMAN_GUIDE.md`](POSTMAN_GUIDE.md), and a ready Postman collection is in [`postman/`](postman/). Per-service Swagger UI: `http://localhost:8081/swagger-ui.html` (and 8082, 8083, …).

---

## 🧪 Testing & Load

- **Functional + security:** `smoke-test-core.ps1` — **36 / 36 checks pass** (auth, accounts, transfers, security, hardening).
- **Data seeding:** `seed-data.ps1` — populates 1,000+ transactions across all services.
- **Load testing (k6):** `k6-stress.js` — ramped to **1,500 virtual users**.

**Headline load result:** ~**243 req/s** sustained, **19,178 requests**, **no service crashed** (fully recovered), with latency degrading gracefully under saturation. Full analysis in [`LOAD_TEST_REPORT.md`](LOAD_TEST_REPORT.md).

---

## 📚 Documentation

| Document | What it covers |
|---|---|
| [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md) | Full project: architecture, security, every API with JSON, diagrams |
| [`STEP_BY_STEP_GUIDE.md`](STEP_BY_STEP_GUIDE.md) | Hands-on walkthrough of every service in order |
| [`POSTMAN_GUIDE.md`](POSTMAN_GUIDE.md) | Every API + where to put what in Postman |
| [`LOAD_TEST_REPORT.md`](LOAD_TEST_REPORT.md) | Performance / stress test report |
| [`START_HERE.md`](START_HERE.md) | Build & run / troubleshooting |

---

## 🗺️ Roadmap (infrastructure scaling)

Application layer is complete and hardened. For heavy production traffic the next steps are infrastructure-level:

- [ ] Docker Compose + Kubernetes (horizontal scaling)
- [ ] Kafka/RabbitMQ for async events
- [ ] Redis (caching + distributed rate limiting)
- [ ] Prometheus + Grafana + Zipkin (observability)
- [ ] HTTPS/TLS + service-to-service mTLS
- [ ] Flyway migrations with `ddl-auto: validate`

---

## 👤 Author

Built by **noorkang3242-tech**. Educational / portfolio project demonstrating production-style microservices architecture, security, and testing.

> ⚠️ This is a learning/portfolio project. The committed configuration uses placeholder secrets for local development — set real values via environment variables and never commit production credentials.
