# 🚀 NOOR Bank — Cloud Deployment Guide

Goal: put the app on the internet so **anyone can open it on their mobile**, even
when your PC is off. The whole stack (10 services + MySQL + React) runs on **one
server** with a single command.

---

## 0. What you need

| Thing | Recommended (free) | Notes |
|---|---|---|
| A server | **Oracle Cloud "Always Free" ARM VM** (4 cores, **24 GB RAM**, free forever) | Any Ubuntu VPS with **≥ 4 GB RAM** also works (Hetzner/Contabo ~ $5/mo) |
| A domain (for HTTPS) | **DuckDNS** (free subdomain) | Optional — you can first test over plain http with the server IP |

> ⚠️ Honest note: this is the most advanced option. It needs a server and ~30–45
> min of setup. If you just want to show it on the **same Wi‑Fi**, that is 2 minutes
> instead (ask me for the LAN option).

---

## 1. Get a server (Oracle Cloud Free, one time)

1. Sign up at **cloud.oracle.com** → create an **Always Free** Compute instance.
2. Shape: **Ampere (ARM) A1.Flex** → give it **4 OCPU + 24 GB RAM** (all free).
3. Image: **Ubuntu 22.04**.
4. Save the **SSH key**, note the **public IP**.
5. In the instance's **VCN → Security List**, add **Ingress** rules to allow
   **TCP 80** and **TCP 443** from `0.0.0.0/0`.

SSH in:
```bash
ssh -i your-key.key ubuntu@YOUR.SERVER.IP
```

---

## 2. Install Docker on the server (one time)

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
# also open the OS firewall (Oracle images block ports by default):
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```
Log out and back in (so the docker group applies).

---

## 3. Copy the project to the server

Everything lives in **one repo** now — the React app is in the `frontend/`
folder, and `docker-compose.yml` builds it from there. Just clone it:

```bash
git clone https://github.com/noorkang3242-tech/banking-system-microservices.git
cd banking-system-microservices
```
(Or copy it up with `scp -r` / WinSCP.)

---

## 4. (Optional but recommended) Free domain + HTTPS with DuckDNS

1. Go to **duckdns.org**, sign in, create a subdomain e.g. `noorbank`.
2. Point it to your server IP (paste the IP, click *update ip*).
   You now have `noorbank.duckdns.org`.

Caddy will automatically get a free HTTPS certificate for it.

---

## 5. Configure secrets

```bash
cp .env.example .env
nano .env
```
Set strong values:
```
DB_PASSWORD=<long-random>
INTERNAL_SHARED_SECRET=<long-random-32+>
JWT_SECRET=<long-random-64+>
ADMIN_EMAIL=admin@bank.local
ADMIN_PASSWORD=<your-strong-admin-password>

# with a domain (HTTPS):
DOMAIN=noorbank.duckdns.org
PUBLIC_URL=https://noorbank.duckdns.org

# OR just to test by IP first (plain http, no domain):
# DOMAIN=:80
# PUBLIC_URL=http://YOUR.SERVER.IP
```

---

## 6. Launch everything 🚀

```bash
docker compose up -d --build
```
First run builds all 10 services — **give it 5–15 minutes** (it downloads Maven
deps and compiles). Watch progress / health:
```bash
docker compose ps
docker compose logs -f api-gateway      # Ctrl+C to stop watching
```

When the gateway and services are up, open:
- **App:** `https://noorbank.duckdns.org` (or `http://YOUR.SERVER.IP`)
- Share that link — it opens on **any mobile, anywhere**. 🎉

Log in as admin with the `ADMIN_EMAIL` / `ADMIN_PASSWORD` you set.

---

## 7. Day-to-day commands

```bash
docker compose ps                 # status
docker compose logs -f <service>  # logs (e.g. loan-service)
docker compose restart <service>  # restart one service
docker compose down               # stop everything (data is kept)
docker compose up -d --build      # apply code changes / restart
docker compose down -v            # ⚠️ also DELETES the database
```

---

## 8. Troubleshooting

| Problem | Fix |
|---|---|
| Build runs out of memory | Use the 24 GB ARM shape; or build one at a time: `docker compose build auth-service` then `up -d` |
| A service keeps restarting | `docker compose logs <service>` — usually it is waiting for MySQL/Eureka; give it a minute |
| Page loads but API calls fail | `PUBLIC_URL` must EXACTLY match the address in the browser (http vs https, domain vs IP). Rebuild frontend: `docker compose up -d --build frontend` |
| HTTPS not working | DNS must point to the server and ports 80/443 must be open in **both** the Oracle Security List **and** the OS iptables |
| Can't reach the site at all | Re-check the firewall rules from steps 1 & 2 |

---

## 9. Simpler alternatives (if cloud is too much for now)

- **Same Wi‑Fi (LAN):** run `npm run dev -- --host`, open `http://192.168.1.188:5173`
  on phones on your Wi‑Fi (point the frontend API at your PC IP + open gateway CORS).
- **Temporary public link (tunnel):** install `cloudflared` and run
  `cloudflared tunnel --url http://localhost:5173` for a throwaway public URL while
  your PC stays on.

Ask me and I'll set either of these up in a couple of minutes.
