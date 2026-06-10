# Setup & Run — Banking System (Foundation)

This guide gets the **foundation** (discovery-server + api-gateway + auth-service) running on your
machine and shows you how to test it. Follow it top to bottom.

---

## 1. Install the tools

| Tool | Why | Check |
|---|---|---|
| **JDK 17** | run/compile the services | `java -version` shows 17 |
| **Maven** | build the projects | `mvn -version` |
| **Docker Desktop** | run MySQL easily | `docker --version` |

> You do NOT have to install MySQL separately — the included `docker-compose.yml` runs it for you.
> (If you prefer your own local MySQL Server + Workbench, that's fine too — see step 2, Option B.)

If you don't have Maven, you can instead run each service from VS Code (the Spring Boot / Java
extensions let you click ▶ Run on each `*Application.java`).

---

## 2. Start MySQL

**Option A — Docker (recommended, simplest):**
```bash
cd banking-system-microservices
docker compose up -d
docker compose ps        # mysql should be "healthy"
```
This gives you MySQL on `localhost:3306` with user `root` / password `root`.
The `auth_db` database is created automatically on first run (the JDBC URL has
`createDatabaseIfNotExist=true`).

**Option B — Your own MySQL Server:**
- Make sure MySQL Server 8 is running on `localhost:3306`.
- If your root password is NOT `root`, either:
  - set env vars before running auth-service: `DB_USERNAME` and `DB_PASSWORD`, or
  - edit `auth-service/src/main/resources/application.yml`.
- You can watch the data in **MySQL Workbench** (Workbench is just the viewer/GUI).

---

## 3. Run the services (in 3 separate terminals)

Start them **in this order** and wait for each to finish booting before the next.

**Terminal 1 — Discovery (Eureka):**
```bash
cd discovery-server
mvn spring-boot:run
```
Wait until you see "Started DiscoveryServerApplication". Open http://localhost:8761 — the Eureka
dashboard should load.

**Terminal 2 — Auth Service:**
```bash
cd auth-service
mvn spring-boot:run
```
Wait for "Started AuthServiceApplication". Refresh http://localhost:8761 — **AUTH-SERVICE** should now
appear under "Instances currently registered with Eureka".

**Terminal 3 — API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```
Wait for "Started ApiGatewayApplication".

> Running from VS Code instead of Maven? Just open each module and click ▶ Run on
> `DiscoveryServerApplication`, then `AuthServiceApplication`, then `ApiGatewayApplication`.

---

## 4. Test it

All requests go through the **gateway on port 8080**.

### 4.1 Register a user
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"noor@example.com","password":"secret123"}'
```
Expected: `201 Created` with `userId`, `email`, `role: CUSTOMER`, and an `accessToken`.

### 4.2 Log in
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"noor@example.com","password":"secret123"}'
```
Expected: `200 OK` with an `accessToken`. **Copy that token.**

### 4.3 Validate the token
```bash
curl http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer PASTE_YOUR_TOKEN_HERE"
```
Expected: `{"valid":true, "userId":"...", "email":"noor@example.com", "role":"CUSTOMER"}`.

### 4.4 Prove the gateway's JWT guard works
Call a protected (not-yet-built) route **without** a token:
```bash
curl -i http://localhost:8080/api/customers/me
```
Expected: **`401 Unauthorized`** — the gateway blocked it because there was no token.
(Once you build `customer-service` in the next phase, the same call **with** a valid token will reach it.)

### 4.5 Swagger UI
Open the auth-service API docs directly: http://localhost:8081/swagger-ui.html

---

## 5. Common issues

| Problem | Fix |
|---|---|
| `Communications link failure` / can't connect to DB | MySQL isn't running. Run `docker compose up -d` (or start your local MySQL). |
| `Access denied for user 'root'` | Your MySQL password isn't `root`. Set `DB_PASSWORD` env var or edit `application.yml`. |
| `Port 8081 already in use` | Another process is using it. Stop it, or change `server.port` in that service. |
| AUTH-SERVICE not showing in Eureka | Start Eureka first and give it ~30s; check `eureka.client.service-url.defaultZone`. |
| `WeakKeyException` on startup | The `jwt.secret` is too short. It must be at least 32 characters (the default is long enough). |
| 401 even with a token | The gateway and auth-service `jwt.secret` values must be **identical**. |

---

## 6. What's next (build order)

Use the phase prompts in `banking-microservices-master-prompt.md`, one at a time:

```
✅ Foundation: discovery + gateway + auth   (this repo)
→ Phase: customer-service        (then uncomment its route in api-gateway/application.yml)
→ Phase: account-service
→ Phase: transaction + transfer  (the fund-transfer saga — needs Kafka/RabbitMQ)
→ Phase: loan + card
→ Phase: notification
→ Phase: monitoring / logging / tracing
→ Phase: Flyway migrations, full Dockerfiles, CI
```

After building each new service: start it, confirm it registers in Eureka, **uncomment its route** in
`api-gateway/src/main/resources/application.yml`, then test through the gateway on port 8080.
