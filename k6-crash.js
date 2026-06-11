// k6-crash.js - MAX stress to actually CRASH the services.
// DB is now full (8000+ transactions, 600 accounts). We hammer the staff
// "get all" endpoints (which load thousands of rows into memory and serialize
// huge JSON) at up to 2550 VUs with a sustained peak -> memory pressure -> OOM.
// Run:  E:\tools\k6\k6.exe run k6-crash.js
import http from 'k6/http';
import { check } from 'k6';

const GW = 'http://localhost:8090';
const JH = { 'Content-Type': 'application/json' };

export const options = {
  discardResponseBodies: true,           // protect k6 itself from the huge payloads
  stages: [
    { duration: '10s', target: 300 },
    { duration: '15s', target: 1000 },
    { duration: '15s', target: 2000 },
    { duration: '15s', target: 2550 },   // 70% more than the previous 1500
    { duration: '40s', target: 2550 },   // HOLD at peak - crash zone
    { duration: '10s', target: 0 },
  ],
};

export function setup() {
  const txt = { headers: JH, responseType: 'text' };
  const adm = http.post(`${GW}/api/auth/login`, JSON.stringify({ email: 'admin@bank.local', password: 'Admin@12345' }), txt);
  const adminTok = adm.json('accessToken');
  const users = [];
  for (let i = 0; i < 5; i++) {
    const em = `crash${i}_${Date.now()}_${Math.floor(Math.random() * 99999)}@bank.test`;
    const r = http.post(`${GW}/api/auth/register`, JSON.stringify({ email: em, password: 'secret123' }), txt);
    if (r.status !== 201) continue;
    const tok = r.json('accessToken');
    const a = http.post(`${GW}/api/accounts`, JSON.stringify({ accountType: 'SAVINGS', initialDeposit: 100000000 }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` }, responseType: 'text' });
    users.push({ tok: tok, acct: a.json('accountNumber') });
  }
  return { adminTok: adminTok, users: users };
}

export default function (data) {
  const adminH = { headers: { 'Authorization': `Bearer ${data.adminTok}` } };
  const pick = Math.floor(Math.random() * 10);
  let res;
  if (pick < 5) {
    res = http.get(`${GW}/api/transactions`, adminH);     // ALL ~8200 transactions (huge JSON)
  } else if (pick < 7) {
    res = http.get(`${GW}/api/accounts`, adminH);         // ALL 600 accounts
  } else if (pick === 7) {
    res = http.get(`${GW}/api/loans`, adminH);            // ALL loans
  } else if (pick === 8) {
    res = http.get(`${GW}/api/cards`, adminH);            // ALL cards
  } else {
    const u = data.users[Math.floor(Math.random() * data.users.length)];
    if (u) res = http.post(`${GW}/api/accounts/${u.acct}/deposit`, JSON.stringify({ amount: 10 }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${u.tok}` } });
  }
  if (res) check(res, { 'ok (status < 500)': (r) => r.status > 0 && r.status < 500 });
}
