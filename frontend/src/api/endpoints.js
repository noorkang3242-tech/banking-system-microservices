import api from './client'

// One function per backend endpoint, grouped by service.

export const authApi = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data) => api.post('/api/auth/login', data),
}

export const customerApi = {
  createProfile: (data) => api.post('/api/customers', data),
  me: () => api.get('/api/customers/me'),
  update: (data) => api.put('/api/customers/me', data),
  all: () => api.get('/api/customers'),
  updateKyc: (userId, kycStatus) => api.patch(`/api/customers/${userId}/kyc`, { kycStatus }),
}

export const accountApi = {
  open: (data) => api.post('/api/accounts', data),
  me: () => api.get('/api/accounts/me'),
  one: (num) => api.get(`/api/accounts/${num}`),
  beneficiary: (num) => api.get(`/api/accounts/${num}/beneficiary`),
  deposit: (num, amount) => api.post(`/api/accounts/${num}/deposit`, { amount }),
  withdraw: (num, amount) => api.post(`/api/accounts/${num}/withdraw`, { amount }),
  all: () => api.get('/api/accounts'),
  setStatus: (num, status) => api.patch(`/api/accounts/${num}/status`, { status }),
}

export const txnApi = {
  me: () => api.get('/api/transactions/me'),
  byAccount: (num) => api.get(`/api/transactions/account/${num}`),
  all: () => api.get('/api/transactions'),
}

export const transferApi = {
  create: (data) => api.post('/api/transfers', data),
  me: () => api.get('/api/transfers/me'),
  all: () => api.get('/api/transfers'),
}

export const loanApi = {
  apply: (data) => api.post('/api/loans', data),
  me: () => api.get('/api/loans/me'),
  repay: (id, amount) => api.post(`/api/loans/${id}/repay`, { amount }),
  approve: (id) => api.patch(`/api/loans/${id}/approve`),
  reject: (id, reason) => api.patch(`/api/loans/${id}/reject`, { reason }),
  all: () => api.get('/api/loans'),
}

export const cardApi = {
  issue: (data) => api.post('/api/cards', data),
  me: () => api.get('/api/cards/me'),
  block: (id) => api.post(`/api/cards/${id}/block`),
  unblock: (id) => api.post(`/api/cards/${id}/unblock`),
  all: () => api.get('/api/cards'),
}

export const notifApi = {
  me: () => api.get('/api/notifications/me'),
  unreadCount: () => api.get('/api/notifications/me/unread-count'),
  read: (id) => api.patch(`/api/notifications/${id}/read`),
  readAll: () => api.patch('/api/notifications/read-all'),
  send: (data) => api.post('/api/notifications/send', data),
  all: () => api.get('/api/notifications'),
}

// Pull a friendly message out of an axios error.
export const errMsg = (e) =>
  e?.response?.data?.message ||
  e?.response?.data?.error ||
  e?.message ||
  'Something went wrong'
