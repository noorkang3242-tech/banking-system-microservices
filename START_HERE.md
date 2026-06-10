# START HERE — Resume Guide (jahan se chhoda)

> Ye file is liye hai ke system band/restart hone ke baad sab kuch wapas chalu kaise karein.
> **Saara code disk pe safe hai** — sirf running services band hoti hain, unhe dobara start karna hota hai.

## Abhi tak kya bana hai (10 services, sab tested ✅ + production-hardened 🔒)

| Service | Port | Database | Folder |
|---|---|---|---|
| discovery-server (Eureka) | 8761 | — | `discovery-server` |
| auth-service | 8081 | `auth_db` | `auth-service` |
| customer-service | 8082 | `user_db` | `customer-service` |
| account-service | 8083 | `account_db` | `account-service` |
| transaction-service | 8084 | `transaction_db` | `transaction-service` |
| transfer-service | 8085 | `transfer_db` | `transfer-service` |
| loan-service | 8086 | `loan_db` | `loan-service` |
| card-service | 8087 | `card_db` | `card-service` |
| notification-service | 8088 | `notification_db` | `notification-service` |
| api-gateway | **8090** | — | `api-gateway` |

> transfer/loan/card services emit events to notification-service (best-effort) on transfer/loan-decision/card-issue.

> account-service har deposit/withdraw par transaction-service ko **automatically** call karke transaction record karta hai (Eureka load-balanced, best-effort).
> transfer-service account-service ko call karke paisa move karta hai (saga: credit fail ho to source ko refund).

> Gateway **8090** par hai (8080 nahi) kyunki Oracle ka `TNSLSNR` 8080 le raha hai.
> Saari API requests **http://localhost:8090** se jaati hain.

## Zaroori environment facts (is machine par)

- **Java 17**: `C:\Program Files\Java\jdk-17` (installed)
- **Maven 3.9.9**: `E:\tools\apache-maven-3.9.9\bin\mvn.cmd` (C: full tha is liye E: par)
  - Maven Central DNS broken → mirror `repo1.maven.org` settings.xml mein set hai (`E:\tools\apache-maven-3.9.9\conf\settings.xml`), local repo `E:\.m2`.
- **MySQL 8** service `MySQL80` (localhost:3306), root password = `your_mysql_password`.
  - 10 databases bani hui hain: auth_db, user_db, account_db, transaction_db, ledger_db, card_db, loan_db, notification_db, audit_db, fraud_db.
- ⚠️ **C: drive lगभग full tha (~82 MB)** — jab ho sake C: par jagah banayein.

## Sab services start karne ka tareeka

### ⭐ RECOMMENDED (kam RAM machine ke liye): lean jars
Ye machine 8 GB RAM ki hai (MySQL/Oracle/IDE ke saath). 10 services ko `mvn spring-boot:run`
se chalane par har service do JVM (Maven + forked app) banati hai aur RAM khatam ho jaati
hai → Eureka unresponsive. Is liye services ko **executable jars** ki tarah, ek-ek JVM mein,
capped heap ke saath, **sequentially** chalayein:
```powershell
.\build-jars.ps1   # ek baar: sab jars banaye (mvn -DskipTests package)
.\run-jars.ps1     # har service = ek `java -Xmx256m -jar`, port-wait ke saath
```
Zyada RAM ho to:  `$env:SERVICE_MAX_HEAP='512m'; .\run-jars.ps1`

### Alternative: start-all.ps1 (mvn spring-boot:run, fork=false + capped heap)
```powershell
C:\Users\Lenovo\Desktop\cash\banking-system-microservices\start-all.ps1
```
Ye 10 windows kholkar sahi order + env ke saath start karta hai (ab fork=false + -Xmx384m
taake RAM na bhare). Phir bhi run-jars.ps1 zyada reliable hai is machine par.

> Secrets (JWT_SECRET, INTERNAL_SHARED_SECRET, ADMIN_EMAIL/PASSWORD, DB creds) dono scripts
> environment se uthate hain; na ho to local-dev fallback. `.env.example` reference hai.

### Manual (agar script na chale) — har ek alag terminal mein, isi order mein:
Pehle ye dono har terminal mein set karein:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$mvn = "E:\tools\apache-maven-3.9.9\bin\mvn.cmd"
```
1. **discovery** (wait ~30s): `cd discovery-server; & $mvn spring-boot:run`
2. **auth**: `cd auth-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
3. **customer**: `cd customer-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
4. **account**: `cd account-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
5. **transaction**: `cd transaction-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
6. **transfer**: `cd transfer-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
7. **loan**: `cd loan-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
8. **card**: `cd card-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
9. **notification**: `cd notification-service; $env:DB_USERNAME='root'; $env:DB_PASSWORD='your_mysql_password'; & $mvn spring-boot:run`
10. **gateway**: `cd api-gateway; $env:SERVER_PORT='8090'; & $mvn spring-boot:run`

## Test karna
- **Postman**: `postman/Banking-Auth.postman_collection.json` import karein → Run collection (21 requests, 3 folders, auto-token).
- **Swagger**: http://localhost:8081/swagger-ui.html (auth), 8082, 8083 bhi.

## Status: SAARE README phases COMPLETE ✅
8 business services (auth, customer, account, transaction, transfer, loan, card, notification) + discovery + gateway. Sab adversarially reviewed (card + notification multi-agent workflow se).

Malformed-JSON ab **sab** services mein 400 deta hai (fleet cleanup done).

## 🔒 Security hardening (DONE — production pass)

1. **Registration locked to CUSTOMER**: `/api/auth/register` ab hamesha CUSTOMER banata hai (role field hata diya gaya). Staff/ADMIN sirf **startup pe seed** hote hain `ADMIN_EMAIL`/`ADMIN_PASSWORD` env se (`AdminSeeder`). Local-dev admin: `admin@bank.local` / `Admin@12345`.
2. **Gateway-only trust (shared secret)**: gateway har forwarded request par `X-Internal-Secret` (env `INTERNAL_SHARED_SECRET`) + `X-Request-Id` stamp karta hai aur client ke `X-User-*` headers strip karta hai. Har service ka `InternalRequestFilter` bina secret aaye direct-port request ko **401** deta hai (`/actuator`,`/swagger-ui`,`/v3/api-docs` exempt). Internal service-to-service calls secret RestClient interceptor se auto bhejte hain → gateway bypass / header forging band.
3. **`/notifications/send` rate-limited**: per-recipient fixed-window limiter (default 20/min) → forged "SECURITY" alert spam band.
4. **Secrets out of code**: JWT/internal/DB/admin sab env se (`.env.example`). `handleGeneric` ab exception message leak nahi karta (sab services).
5. **Resilience + tracing**: har inter-service RestClient par connect 2s / read 5s timeouts; `X-Request-Id` MDC + logs + downstream propagate; actuator `health,info,metrics`.

> Bache hue items infra-level hain (mTLS, HTTPS/TLS, Flyway + `ddl-auto: validate`, Redis-backed rate-limit for multi-instance, Kafka events, Docker/observability stack). Is machine par Docker/Kafka/Redis installed nahi, is liye abhi scope se bahar.

## Ab tak ke 2 bug fixes (yaad rahein)
1. `auth-service` SecurityConfig: `return http;` → `return http.build();` (compile fix)
2. `auth-service` GlobalExceptionHandler: missing header ab 500 ki bajaye **400** (added `MissingRequestHeaderException` handler). customer/account mein ye shuru se hai.
