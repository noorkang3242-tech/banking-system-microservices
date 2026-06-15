# 🔐 Keycloak Security — Implementation Guide

This document explains the **Keycloak (OAuth2 / OpenID Connect) integration** for
NOOR Bank: what was built, what each piece does, how to configure it, and how to
run it. The system keeps the original custom-JWT login too — it is **mode-switchable**.

---

## 1. The big picture

The app supports **two authentication modes**, chosen by one flag:

| Mode | Who issues the token | When it is used | Login screen |
|---|---|---|---|
| `jwt` (default) | **auth-service** (custom HS256) | local development | the built-in NOOR Bank login form |
| `keycloak` | **Keycloak** (OIDC, RS256) | cloud deployment | Keycloak's hosted login page (redirect) |

Both modes end up forwarding the **same headers** (`X-User-Id`, `X-User-Email`,
`X-User-Role`) to the 8 microservices, so **the business services never change** —
they keep trusting the gateway via the shared internal secret.

```
                       MODE = keycloak (cloud)                 MODE = jwt (local)
Browser ──login──▶ Keycloak (issues RS256)            Browser ──login──▶ auth-service (HS256)
   │  access token                                       │  access token
   ▼                                                     ▼
API Gateway  ──validates token, injects X-User-* + X-Internal-Secret──▶  8 microservices
```

---

## 2. Which piece does what

| Piece | File / location | Job |
|---|---|---|
| **Mode flag (backend)** | `api-gateway` property `gateway.auth-mode` (env `GATEWAY_AUTH_MODE`) | Picks which gateway filter is active: `jwt` or `keycloak`. Default `jwt`. |
| **JWT filter** | `api-gateway/.../filter/JwtAuthenticationFilter.java` | Active in `jwt` mode. Validates the custom HS256 token from auth-service, forwards `X-User-*`. `@ConditionalOnProperty(... havingValue="jwt", matchIfMissing=true)`. |
| **Keycloak filter** | `api-gateway/.../filter/KeycloakAuthenticationFilter.java` | Active in `keycloak` mode. Validates Keycloak's **RS256** token against the realm **JWKS** (public keys) using `nimbus-jose-jwt`. Extracts `sub`→userId, `email`, `realm_access.roles`→role. `@ConditionalOnProperty(... havingValue="keycloak")`. |
| **Gateway config** | `api-gateway/src/main/resources/application.yml` | Holds `gateway.auth-mode`, `jwt.secret` (jwt mode), and `keycloak.issuer-uri` / `keycloak.jwks-uri` (keycloak mode). |
| **Realm definition** | `keycloak/realm-noor-bank.json` | Auto-imported by Keycloak on first start. Defines the realm, roles, the frontend client, and the seeded admin user. |
| **Keycloak server** | `docker-compose.yml` → `keycloak` service | Runs Keycloak, imports the realm, serves under `/auth`. |
| **Reverse proxy** | `Caddyfile` | Routes `/auth/*` → Keycloak, `/api/*` → gateway, everything else → frontend (one HTTPS origin). |
| **Mode flag (frontend)** | env `VITE_AUTH_MODE` | `jwt` = custom login form, `keycloak` = OIDC redirect. Default `jwt`. |
| **Keycloak adapter** | `frontend/src/auth/keycloak.js` | Creates the `keycloak-js` instance from env, maps realm roles → one role. |
| **Auth state** | `frontend/src/auth/AuthContext.jsx` | Branches on mode. In keycloak mode: `init({onLoad:'check-sso', pkceMethod:'S256'})`, login/register/logout via Keycloak, token auto-refresh, a `ready` flag. |
| **HTTP client** | `frontend/src/api/client.js` | Attaches the right token (Keycloak token + `updateToken` refresh, or the stored JWT). On 401 → Keycloak login (keycloak mode) or `/login` (jwt mode). |
| **App gate** | `frontend/src/App.jsx` | Shows a spinner until `ready` (waits for the Keycloak session check). |
| **Login page** | `frontend/src/pages/Login.jsx` | In keycloak mode shows a **Sign in** button (redirects to Keycloak); in jwt mode shows the email/password form. |

---

## 3. The Keycloak realm

Realm **`noor-bank`** (defined in `keycloak/realm-noor-bank.json`):

- **Registration:** self-registration **allowed**; email used as the username.
- **Roles:** `CUSTOMER`, `MANAGER`, `ADMIN`. New sign-ups get **CUSTOMER** by default.
- **Client `noor-bank-frontend`:** public client, **Authorization Code + PKCE (S256)**,
  standard flow on, direct grants off. Redirect URIs include `http://localhost:5173/*`
  and `https://*/*`; web origins `+`.
- **Seeded staff user:** `admin@bank.local` / `Admin@12345` with the **ADMIN** role.
- **Issuer:** `http://localhost:8180/realms/noor-bank` (local) or
  `${PUBLIC_URL}/auth/realms/noor-bank` (cloud).

---

## 4. Configuration reference

### Gateway (backend)
| Property / env | Default | Meaning |
|---|---|---|
| `gateway.auth-mode` / `GATEWAY_AUTH_MODE` | `jwt` | `jwt` or `keycloak` |
| `jwt.secret` / `JWT_SECRET` | (dev value) | HS256 secret (jwt mode), must match auth-service |
| `keycloak.issuer-uri` / `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/noor-bank` | Must equal the token's `iss` |
| `keycloak.jwks-uri` / `KEYCLOAK_JWKS_URI` | `…/realms/noor-bank/protocol/openid-connect/certs` | Where the gateway fetches public keys |
| `internal.shared-secret` / `INTERNAL_SHARED_SECRET` | (dev value) | Gateway→service trust header |

### Frontend
| Build env | Default | Meaning |
|---|---|---|
| `VITE_AUTH_MODE` | `jwt` | `jwt` or `keycloak` |
| `VITE_API_BASE_URL` | `http://localhost:8090` | Gateway URL |
| `VITE_KEYCLOAK_URL` | `http://localhost:8180/auth` | Keycloak base URL |
| `VITE_KEYCLOAK_REALM` | `noor-bank` | Realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `noor-bank-frontend` | Public client id |

### Keycloak container (cloud, `docker-compose.yml`)
| Env | Value | Meaning |
|---|---|---|
| `KC_BOOTSTRAP_ADMIN_USERNAME/PASSWORD` | from `.env` | Keycloak admin console login |
| `KC_HTTP_RELATIVE_PATH` | `/auth` | Serve Keycloak under `/auth` |
| `KC_PROXY_HEADERS` | `xforwarded` | Trust Caddy's forwarded headers |
| `KC_HOSTNAME` | `${PUBLIC_URL}/auth` | Public address (so `iss` is correct) |

---

## 5. How to run

### Local (jwt mode — Keycloak NOT needed)
This is the default. Just start the stack:
```
start-all.ps1          # services + gateway (jwt) + frontend
```
Open http://localhost:5173 and log in with the custom form. Nothing else required.

### Local, trying Keycloak by hand (needs free RAM)
1. Start Keycloak: `E:\tools\keycloak-26.6.3\bin\kc.bat start-dev --http-port=8180`
   (admin/Admin@12345). Import the realm or create it.
2. Run the gateway with `GATEWAY_AUTH_MODE=keycloak`.
3. Build the frontend with `VITE_AUTH_MODE=keycloak` (+ `VITE_KEYCLOAK_*`).

> ⚠️ On an 8 GB machine Keycloak + all services + MySQL usually don't fit together —
> use the cloud for the full Keycloak run.

### Cloud (keycloak mode — everything via Docker)
Already wired in `docker-compose.yml`. On the server:
```
cp .env.example .env        # set DB_PASSWORD, secrets, DOMAIN, PUBLIC_URL
docker compose up -d --build
```
This starts MySQL, Eureka, the 8 services, the gateway (**keycloak mode**), **Keycloak**
(imports the realm), the frontend (**OIDC login**), and Caddy (HTTPS). Open the site;
"Sign in" redirects to Keycloak. See `DEPLOYMENT.md` for the full server walkthrough.

---

## 6. Token → headers mapping

The Keycloak access token carries:
```json
{ "sub": "<user-uuid>", "email": "...", "realm_access": { "roles": ["CUSTOMER","ADMIN",...] } }
```
The gateway turns that into downstream headers:
| Token claim | Header sent to services |
|---|---|
| `sub` | `X-User-Id` |
| `email` (or `preferred_username`) | `X-User-Email` |
| highest of `realm_access.roles` (ADMIN > MANAGER > CUSTOMER) | `X-User-Role` |
| — | `X-Internal-Secret` (proves it came through the gateway) |

---

## 7. Gotchas we hit (and the fixes)

- **`admin-cli` returns a "lightweight" token** with no `sub`/roles — use the
  **`noor-bank-frontend`** client for real tokens. The gateway correctly rejects a
  token without `sub`.
- **Issuer must match exactly.** The token's `iss` must equal `keycloak.issuer-uri`.
  Behind a proxy, set `KC_HOSTNAME` so Keycloak stamps the public URL.
- **Don't wrap downstream calls in the auth error handler.** The gateway only treats
  *token-validation* failures as 401; routing/5xx errors stay as themselves.
- **RAM:** Keycloak (~500 MB) + the full stack doesn't fit on 8 GB — run the full
  Keycloak flow on the cloud (24 GB Oracle free tier is plenty).

---

## 8. File checklist

```
keycloak/realm-noor-bank.json                         realm + roles + client + admin (auto-import)
api-gateway/src/main/resources/application.yml        auth-mode + jwt + keycloak settings
api-gateway/.../filter/JwtAuthenticationFilter.java   HS256 validation (jwt mode)
api-gateway/.../filter/KeycloakAuthenticationFilter.java  RS256/JWKS validation (keycloak mode)
api-gateway/pom.xml                                   jjwt + nimbus-jose-jwt
docker-compose.yml                                    keycloak service + gateway/frontend wiring
Caddyfile                                             /auth → Keycloak, /api → gateway, / → app
frontend/src/auth/keycloak.js                         keycloak-js instance + role mapping
frontend/src/auth/AuthContext.jsx                     mode-aware auth state
frontend/src/api/client.js                            token attach + refresh + 401 handling
frontend/src/App.jsx                                  spinner until OIDC session check
frontend/src/pages/Login.jsx                          Sign-in button (keycloak) vs form (jwt)
frontend/Dockerfile                                   VITE_AUTH_MODE + VITE_KEYCLOAK_* build args
```
