import axios from 'axios';

const api = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 5000,
});

import { authStorage } from './auth';

// attach token to every outgoing request
api.interceptors.request.use((config) => {
  const token = authStorage.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// on 401, drop the stale token and force re-login.
// on 429, the gateway is rate-limiting us — back off for the window it
// tells us about and retry once, rather than surfacing it as "backend down".
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      authStorage.clearToken();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    if (error.response?.status === 429 && !error.config._retriedAfterRateLimit) {
      const retryAfterHeader = error.response.headers?.['retry-after'];
      const waitSeconds = Math.min(Number(retryAfterHeader) || 2, 10); // cap the wait
      error.config._retriedAfterRateLimit = true;
      await sleep(waitSeconds * 1000);
      return api.request(error.config);
    }

    return Promise.reject(error);
  }
);

// ── Query Service (CQRS Read Side — Port 8082 / Gateway 8080) ───
export const queryApi = {
  getAllIncidents: () =>
    api.get('/api/query/incidents').then(r => r.data),

  getActiveIncidents: () =>
    api.get('/api/query/incidents/active').then(r => r.data),

  getIncidentById: (id) =>
    api.get(`/api/query/incidents/${id}`).then(r => r.data),

  getIncidentDetail: (id) =>
    api.get(`/api/query/incidents/${id}/detail`).then(r => r.data),

  getIncidentsByStatus: (status) =>
    api.get(`/api/query/incidents/status/${status}`).then(r => r.data),

  getIncidentsBySeverity: (severity) =>
    api.get(`/api/query/incidents/severity/${severity}`).then(r => r.data),

  getIncidentsByService: (serviceName) =>
    api.get(`/api/query/incidents/service/${serviceName}`).then(r => r.data),

  getActionsByIncident: (incidentId) =>
    api.get(`/api/query/incidents/${incidentId}/actions`).then(r => r.data),

  getActionById: (id) =>
    api.get(`/api/query/actions/${id}`).then(r => r.data),
};

const generateIdempotencyKey = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return 'key-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);
};

// ── Command Service (CQRS Write Side — Port 8081 / Gateway 8080) ──
export const commandApi = {
  createIncident: (data) =>
    api.post('/api/incidents', data).then(r => r.data),

  proposeAction: (incidentId, data) =>
    api.post(`/api/incidents/${incidentId}/actions`, data).then(r => r.data),

  approveAction: (incidentId, actionId, approvedBy) =>
    api.post(
      `/api/incidents/${incidentId}/actions/${actionId}/approve`,
      { approvedBy: approvedBy || 'OpsEngineer' },
      { headers: { 'X-Idempotency-Key': generateIdempotencyKey() } }
    ).then(r => r.data),

  rejectAction: (incidentId, actionId, reason) =>
    api.post(
      `/api/incidents/${incidentId}/actions/${actionId}/reject`,
      { reason: reason || 'Action rejected' }
    ).then(r => r.data),

  executeAction: (incidentId, actionId) =>
    api.post(
      `/api/incidents/${incidentId}/actions/${actionId}/execute`,
      {},
      { headers: { 'X-Idempotency-Key': generateIdempotencyKey() } }
    ).then(r => r.data),

  rollbackAction: (incidentId, actionId) =>
    api.post(
      `/api/incidents/${incidentId}/actions/${actionId}/rollback`
    ).then(r => r.data),

  updateStatus: (incidentId, targetStatus, reason) =>
    api.patch(
      `/api/incidents/${incidentId}/status`,
      { targetStatus, reason }
    ).then(r => r.data),
};

// ── Admin DLQ API (Kafka Dead-Letter Queue — Port 8082) ────────
export const adminDlqApi = {
  getUnreplayed: () =>
    api.get('/api/admin/dlq').then(r => r.data),

  getAll: () =>
    api.get('/api/admin/dlq/all').then(r => r.data),

  replay: (id) =>
    api.post(`/api/admin/dlq/${id}/replay`).then(r => r.data),
};

// ── Agent & Telemetry API (Port 8083) ───────────────────────────
export const agentApi = {
  testAi: (prompt) =>
    api.get('/test/ai', { params: { prompt } }).then(r => r.data),

  getMetrics: (serviceName) =>
    api.get('/test/metrics', { params: { serviceName } }).then(r => r.data),
};

export default api;
