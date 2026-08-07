import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  ArrowRight,
  Plus,
  Server,
  Database,
  WifiOff,
  RefreshCw
} from 'lucide-react';
import { queryApi, agentApi, adminDlqApi } from '../api/client';
import StatusBadge, { SeverityBadge } from '../components/StatusBadge';
import CreateIncidentModal from '../components/CreateIncidentModal';
import { timeAgo } from '../utils/helpers';

export default function DashboardPage({ wsSubscribe, refreshActiveCount }) {
  const navigate = useNavigate();
  const [incidents, setIncidents] = useState([]);
  const [allIncidents, setAllIncidents] = useState([]);
  const [dlqCount, setDlqCount] = useState(0);
  const [serviceMetrics, setServiceMetrics] = useState({});
  const [loading, setLoading] = useState(true);
  const [backendError, setBackendError] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const fetchData = useCallback(async (isBackground = false) => {
    try {
      if (!isBackground && incidents.length === 0) {
        setLoading(true);
      }
      setBackendError(false);

      const [active, all, dlq] = await Promise.all([
        queryApi.getActiveIncidents().catch(() => null),
        queryApi.getAllIncidents().catch(() => null),
        adminDlqApi.getUnreplayed().catch(() => null),
      ]);

      if (active === null && all === null) {
        setBackendError(true);
      } else {
        const allList = all || [];
        const activeList = active !== null && Array.isArray(active)
          ? active
          : allList.filter((i) => i.status !== 'RESOLVED' && i.status !== 'ESCALATED');

        setIncidents(activeList);
        setAllIncidents(allList);
        setDlqCount(dlq?.length || 0);
      }

      // Asynchronously fetch Prometheus metrics in background without blocking active incidents
      agentApi.getMetrics('api-service').then((m) => {
        if (m) setServiceMetrics((prev) => ({ ...prev, 'api-service': m }));
      }).catch(() => null);

      agentApi.getMetrics('scheduler-service').then((m) => {
        if (m) setServiceMetrics((prev) => ({ ...prev, 'scheduler-service': m }));
      }).catch(() => null);

      agentApi.getMetrics('worker-service').then((m) => {
        if (m) setServiceMetrics((prev) => ({ ...prev, 'worker-service': m }));
      }).catch(() => null);

    } catch (err) {
      console.error('Failed to fetch backend metrics', err);
      setBackendError(true);
    } finally {
      setLoading(false);
    }
  }, [incidents.length]);

  useEffect(() => {
    fetchData();
    const interval = setInterval(() => {
      fetchData(true);
    }, 3000);
    return () => clearInterval(interval);
  }, [fetchData]);

  // STOMP WebSocket real-time event listener
  useEffect(() => {
    const unsub = wsSubscribe('/topic/incidents/active', () => {
      fetchData(true);
    });
    return unsub;
  }, [wsSubscribe, fetchData]);

  const activeCount = incidents.length;
  const resolvedCount = allIncidents.filter((i) => i.status === 'RESOLVED').length;
  const awaitingApproval = incidents.filter(
    (i) => i.status === 'WAITING_APPROVAL' || i.status === 'ACTION_PROPOSED'
  ).length;

  return (
    <>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <h1>Real-Time Operations Command Center</h1>
            <p>Live CQRS Read Model & Prometheus Telemetry Stream</p>
          </div>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button className="btn btn-ghost btn-sm" onClick={fetchData}>
              <RefreshCw size={14} className={loading ? 'spinning' : ''} /> Refresh Data
            </button>
            <button className="btn btn-primary btn-sm" onClick={() => setShowCreateModal(true)}>
              <Plus size={14} /> Create / Trigger Incident
            </button>
          </div>
        </div>
      </div>

      <div className="page-body">
        {/* Backend Connection Error Banner */}
        {backendError && (
          <div className="card" style={{
            marginBottom: '20px',
            borderColor: 'var(--color-warning)',
            background: 'rgba(240, 169, 69, 0.08)',
            padding: '14px 20px',
            display: 'flex',
            alignItems: 'center',
            justify: 'space-between'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <WifiOff size={20} color="var(--color-warning)" />
              <div>
                <strong style={{ color: 'var(--color-warning)', fontSize: '13px' }}>
                  Backend Microservices Offline or Unreachable
                </strong>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', margin: 0 }}>
                  Cannot connect to Query Service (:18082) or Command Service (:18081). Ensure Docker containers are running (<code>docker compose up</code>).
                </p>
              </div>
            </div>
            <button className="btn btn-warning btn-sm" onClick={fetchData}>
              Retry Connection
            </button>
          </div>
        )}

        {/* Operational Metrics Grid */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon critical">
              <AlertTriangle size={20} />
            </div>
            <div>
              <div className="stat-value">{activeCount}</div>
              <div className="stat-label">Active Incidents (Live DB)</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon warning">
              <Clock size={20} />
            </div>
            <div>
              <div className="stat-value">{awaitingApproval}</div>
              <div className="stat-label">Awaiting Approval</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon success">
              <CheckCircle2 size={20} />
            </div>
            <div>
              <div className="stat-value">{resolvedCount}</div>
              <div className="stat-label">Resolved History</div>
            </div>
          </div>
          <div className="stat-card">
            <div className="stat-icon info">
              <Database size={20} />
            </div>
            <div>
              <div className="stat-value">{dlqCount}</div>
              <div className="stat-label">Kafka DLQ Messages</div>
            </div>
          </div>
        </div>

        {/* Monitored Microservices Prometheus Status */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '20px' }}>
          {['api-service', 'scheduler-service', 'worker-service'].map((service) => {
            const m = serviceMetrics[service];
            const isOffline = m === null || m?.stale === true;

            return (
              <div className="card" key={service} style={{ padding: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
                    <Server size={16} color={isOffline ? 'var(--text-muted)' : 'var(--accent)'} />
                    {service}
                  </div>
                  {m === null ? (
                    <span className="badge badge-rejected">Service Offline</span>
                  ) : m?.stale ? (
                    <span className="badge badge-rollback">Circuit Open (Unreachable)</span>
                  ) : (
                    <span className="badge badge-resolved">Live Telemetry</span>
                  )}
                </div>
                <div style={{ display: 'flex', gap: '20px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                  <div>
                    Error Rate: <strong style={{ color: isOffline ? 'var(--text-muted)' : 'var(--text-primary)' }}>
                      {!isOffline && m?.errorRate != null ? `${m.errorRate.toFixed(1)}%` : '— (Offline)'}
                    </strong>
                  </div>
                  <div>
                    Avg Latency: <strong style={{ color: isOffline ? 'var(--text-muted)' : 'var(--text-primary)' }}>
                      {!isOffline && m?.avgLatencyMs != null ? `${m.avgLatencyMs.toFixed(0)}ms` : '— (Offline)'}
                    </strong>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Active Incident Stream Table */}
        <div className="card">
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span className="card-title">Active Incidents (CQRS Read Model)</span>
              {activeCount > 0 && (
                <span className="live-indicator">
                  <span className="live-pulse" />
                  STOMP WebSocket Connected
                </span>
              )}
            </div>
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => navigate('/incidents')}
            >
              View All History <ArrowRight size={14} />
            </button>
          </div>

          {loading ? (
            <div className="empty-state">
              <div className="spinner" />
            </div>
          ) : incidents.length === 0 ? (
            <div className="empty-state">
              <CheckCircle2 size={48} color="var(--color-success)" />
              <h3>No Active Incidents</h3>
              <p style={{ marginTop: '4px' }}>
                {backendError
                  ? 'Connect the backend microservices to see real-time incident events.'
                  : 'All monitored services are operating within normal Prometheus thresholds.'}
              </p>
            </div>
          ) : (
            <table className="incident-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Monitored Service</th>
                  <th>Severity</th>
                  <th>Lifecycle Status</th>
                  <th>Detected Time</th>
                  <th>War Room</th>
                </tr>
              </thead>
              <tbody>
                {incidents.map((inc) => {
                  const incId = inc.id || inc.incidentId;
                  return (
                    <tr key={incId || Math.random()} onClick={() => incId && navigate(`/incidents/${incId}`)} style={{ cursor: 'pointer' }}>
                      <td>
                        <span className="incident-id">#{incId}</span>
                      </td>
                      <td>
                        <span className="service-name">{inc.serviceName}</span>
                      </td>
                      <td>
                        <SeverityBadge severity={inc.severity} />
                      </td>
                      <td>
                        <StatusBadge status={inc.status} />
                      </td>
                      <td>
                        <span className="time-ago">{timeAgo(inc.createdAt)}</span>
                      </td>
                      <td>
                        <button
                          className="btn btn-primary btn-sm"
                          style={{ fontSize: '11px', padding: '4px 10px' }}
                          onClick={(e) => {
                            e.stopPropagation();
                            if (incId) navigate(`/incidents/${incId}`);
                          }}
                        >
                          Enter War Room <ArrowRight size={12} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showCreateModal && (
        <CreateIncidentModal
          onClose={() => setShowCreateModal(false)}
          onCreated={() => {
            fetchData(true);
            refreshActiveCount();
          }}
        />
      )}
    </>
  );
}
