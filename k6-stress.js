// k6-stress.js - REAL heavy-load stress test for the banking system.
// Ramps up to 1500 virtual users hammering every service's endpoints.
// Run:  E:\tools\k6\k6.exe run k6-stress.js
import http from 'k6/http';
import { check } from 'k6';

const GW = 'http://localhost:8090';
const JH = { 'Content-Type': 'application/json' };

export const options = {
  stages: [
    { duration: '15s', target: 100 },   // warm up
    { duration: '15s', target: 400 },
    { duration: '20s', target: 1000 },
    { duration: '15s', target: 1500 },  // peak - yahan crash expected
    { duration: '10s', target: 0 },     // ramp down
  ],
};

// setup(): test users + accounts + profiles banao (ek baar), VUs ko pass karo
export function setup() {
  const users = [];
  for (let i = 0; i < 8; i++) {
    const em = `k6u${i}_${Date.now()}_${Math.floor(Math.random() * 99999)}@bank.test`;
    const r = http.post(`${GW}/api/auth/register`, JSON.stringify({ email: em, password: 'secret123' }), { headers: JH });
    if (r.status !== 201) continue;
    const tok = r.json('accessToken');
    const ah = { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tok}` } };
    const a = http.post(`${GW}/api/accounts`, JSON.stringify({ accountType: 'SAVINGS', initialDeposit: 100000000 }), ah);
    http.post(`${GW}/api/customers`, JSON.stringify({ firstName: 'K6', lastName: 'User', phone: '03001112222', address: 'City', dateOfBirth: '1996-01-01' }), ah);
    users.push({ tok: tok, uid: r.json('userId'), acct: a.json('accountNumber') });
  }
  return { users: users };
}

// har VU iteration: random endpoint (saari services cover)
export default function (data) {
  const users = data.users;
  if (!users || users.length < 2) return;
  const u = users[Math.floor(Math.random() * users.length)];
  const ah = { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${u.tok}` } };
  const pick = Math.floor(Math.random() * 10);
  let res;
  if (pick < 3) {
    res = http.post(`${GW}/api/accounts/${u.acct}/deposit`, JSON.stringify({ amount: 10 }), ah);
  } else if (pick === 3) {
    res = http.post(`${GW}/api/accounts/${u.acct}/withdraw`, JSON.stringify({ amount: 5 }), ah);
  } else if (pick === 4) {
    let d = users[Math.floor(Math.random() * users.length)];
    res = http.post(`${GW}/api/transfers`, JSON.stringify({ fromAccount: u.acct, toAccount: d.acct, amount: 1 }), ah);
  } else if (pick === 5) {
    res = http.post(`${GW}/api/loans`, JSON.stringify({ accountNumber: u.acct, principal: 2000, termMonths: 6, purpose: 'k6' }), ah);
  } else if (pick === 6) {
    res = http.post(`${GW}/api/cards`, JSON.stringify({ cardType: 'CREDIT', cardholderName: 'K6', creditLimit: 1000 }), ah);
  } else if (pick === 7) {
    res = http.get(`${GW}/api/transactions/me`, ah);
  } else if (pick === 8) {
    res = http.get(`${GW}/api/accounts/me`, ah);
  } else {
    res = http.get(`${GW}/api/notifications/me`, ah);
  }
  check(res, { 'ok (status < 500)': (r) => r.status > 0 && r.status < 500 });
}
