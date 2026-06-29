import axios from 'axios';

const API_BASE = '/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// Add JWT token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 responses
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('merchant');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const authAPI = {
  register: (data) => api.post('/merchants/register', data),
  login: (data) => api.post('/merchants/login', data),
};

// Merchant APIs
export const merchantAPI = {
  getById: (id) => api.get(`/merchants/${id}`),
  update: (id, data) => api.put(`/merchants/${id}`, data),
  regenerateKeys: (id) => api.post(`/merchants/${id}/regenerate-keys`),
  getDashboard: (id) => api.get(`/merchants/${id}/dashboard`),
};

// Transaction APIs
export const transactionAPI = {
  getAll: (params) => api.get('/transactions', { params }),
  getById: (id) => api.get(`/transactions/${id}`),
  getByMerchant: (merchantId, params) => api.get(`/transactions/merchant/${merchantId}`, { params }),
  getStats: () => api.get('/transactions/stats'),
  getStatsByMerchant: (merchantId) => api.get(`/transactions/stats/merchant/${merchantId}`),
  getDailyVolume: (days = 7) => api.get(`/transactions/volume/daily`, { params: { days } }),
  getPaymentMethodDistribution: () => api.get('/transactions/distribution/payment-method'),
};

// Fraud APIs
export const fraudAPI = {
  getCheck: (txnId) => api.get(`/fraud/check/${txnId}`),
  getRules: () => api.get('/fraud/rules'),
  getStats: () => api.get('/fraud/stats'),
  getFlagged: (params) => api.get('/fraud/flagged', { params }),
  approve: (txnId) => api.post(`/fraud/review/${txnId}/approve`),
  reject: (txnId) => api.post(`/fraud/review/${txnId}/reject`),
};

// Settlement APIs
export const settlementAPI = {
  getByMerchant: (merchantId) => api.get(`/settlements/merchant/${merchantId}`),
  getById: (id) => api.get(`/settlements/${id}`),
  getBalance: (merchantId) => api.get(`/settlements/balance/${merchantId}`),
  trigger: (merchantId) => api.post(`/settlements/trigger/${merchantId}`),
};

// Payment APIs
export const paymentAPI = {
  createOrder: (data) => api.post('/payments/orders', data),
  processPayment: (data) => api.post('/payments/process', data),
  getOrder: (orderId) => api.get(`/payments/orders/${orderId}`),
  retry: (txnId) => api.post(`/payments/${txnId}/retry`),
};

// Notification APIs
export const notificationAPI = {
  getAll: (params) => api.get('/notifications', { params }),
  getByMerchant: (merchantId, params) => api.get(`/notifications/merchant/${merchantId}`, { params }),
};

export default api;
