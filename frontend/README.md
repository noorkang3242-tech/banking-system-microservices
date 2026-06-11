# ☀️ NOOR Bank — Web App (Frontend)

A production-style **online banking** web app built with React. It is the
front end for the [NOOR Bank microservices backend](https://github.com/noorkang3242-tech/banking-system-microservices)
(Spring Boot + Spring Cloud).

## ✨ Features

- 🔐 **JWT auth** — multi-step professional registration (personal → account → security → review) and login
- 🏦 **Accounts** — open savings/current accounts, deposit, withdraw, balances
- 💸 **Transfers** — beneficiary verification before sending + downloadable/printable receipt
- 📜 **Transactions** — full history with filtering & sorting
- 💰 **Loans** — State Bank of Pakistan–aligned products with auto interest by type & term
- 💳 **Cards** — issue debit/credit cards, one-time number/CVV reveal, block/unblock
- 🔔 **Notifications** — in-app alerts
- 🛡️ **Admin panel** — overview stats, customer KYC, account block/unblock, account lookup, loan approve/reject, send notifications
- 🌐 **Multi-language** — English, اردو, العربية, हिन्दी (with RTL) — switchable from the login page
- ☀️ **Live UI** — animated weather header (clock, season), bank head-office branding, policies & contact

## 🧱 Tech stack

React 19 · Vite · Ant Design · Axios · React Router

## 🚀 Getting started

```bash
npm install
npm run dev        # http://localhost:5173
```

The app talks to the API gateway. Configure the base URL in `.env`:

```
VITE_API_BASE_URL=http://localhost:8090
```

Make sure the [backend](https://github.com/noorkang3242-tech/banking-system-microservices)
(gateway on `:8090` + services) is running first.

## 🏗️ Build

```bash
npm run build      # outputs to dist/
npm run preview
```

A `Dockerfile` (build + nginx) is included for container deployment — see the
backend repo's `docker-compose.yml` and `DEPLOYMENT.md` for the full stack.

## 📞 Contact

NOOR Bank — Head Office: Chak No. 199P, Sadiqabad, Punjab, Pakistan
📧 noorkang3242@gmail.com · 💚 WhatsApp +92 323 3522940
