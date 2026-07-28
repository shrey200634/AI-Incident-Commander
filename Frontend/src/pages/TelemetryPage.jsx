import { useState, useEffect, useCallback } from 'react';
import { agentApi } from '../api/client';
import { ShieldAlert, Activity, Database, Server, Cpu, CheckCircle2, AlertTriangle, RefreshCw } from 'lucide-react';

export default function TelemetryPage() {
  const [selectedService, setSelectedService] = useState('FoodRush-Orders');
  const [metricSnapshot, setMetricSnapshot] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchMetrics = useCallback(async () => {
    setLoading(true);
    try {
      const data = await agentApi.getMetrics(selectedService);
      setMetricSnapshot(data);
    } catch (err) {
      console.error('Failed to query metrics endpoint', err);
      setMetricSnapshot(null);
    } finally {
      setLoading(false);
    }
  }, [selectedService]);

  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  const isOffline = metricSnapshot === null || metricSnapshot?.stale === true;

  return (
    <>
      <div className="page-header">
        <h1>System Telemetry & Resilience Monitor</h1>
        <p>Real-time metrics protection powered by Resilience4j Circuit Breaker & Redis cache-aside</p>
      </div>

      <div className="page-body">
        {/* Architecture Summary Card */}
        <div className="card" style={{ marginBottom: '20px' }}>
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Cpu size={20} color="var(--accent)" />
              <span className="card-title">Agent Service & Telemetry Flow Architecture</span>
            </div>
          </div>
          <div className="card-body" style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            <p style={{ marginBottom: '10px' }}>
              The <strong>Agent Service</strong> automatically listens to <code>incident.created</code> Kafka events. When an incident occurs:
            </p>
            <ol style={{ paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <li>The Agent executes read-only diagnostic tools: <code>getServiceHealth</code>, <code>getKafkaConsumerLag</code>, <code>getRedisMemoryStats</code>, <code>checkDatabaseDeadlocks</code>, and <code>getRecentDeployments</code>.</li>
              <li>Spring AI invokes <strong>Gemini 2.5 Flash</strong> (with automatic fallback to <strong>Groq Llama 3.3</strong> if Gemini is unavailable).</li>
              <li>The Agent calls <code>proposeAction</code>, sending a remediation command to the Command Service where it awaits human approval.</li>
              <li>Prometheus telemetry calls are wrapped by a <strong>Resilience4j Circuit Breaker</strong> (50% failure threshold over 10 calls, 30s open duration). On failure/timeout, it falls back to Redis cached metrics marked with <code>stale: true</code>.</li>
            </ol>
          </div>
        </div>

        {/* Circuit Breaker Live Inspection */}
        <div className="card">
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldAlert size={18} color="var(--color-warning)" />
              <span className="card-title">Prometheus Telemetry & Circuit Breaker Inspector</span>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={fetchMetrics} disabled={loading}>
              <RefreshCw size={13} className={loading ? 'spinning' : ''} /> Refresh Telemetry
            </button>
          </div>

          <div className="card-body">
            <div className="form-group" style={{ maxWidth: '360px', marginBottom: '20px' }}>
              <label className="form-label">Target Service Under Observation</label>
              <select
                className="form-select"
                value={selectedService}
                onChange={(e) => setSelectedService(e.target.value)}
              >
                <option value="FoodRush-Orders">FoodRush-Orders (Order Microservice)</option>
                <option value="Payment-Gateway">Payment-Gateway (Payment Processing)</option>
                <option value="DistributedJobForge">DistributedJobForge (Worker Cluster)</option>
              </select>
            </div>

            {loading ? (
              <div className="empty-state" style={{ padding: '30px' }}>
                <div className="spinner" />
              </div>
            ) : metricSnapshot ? (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
                  <span className="form-label" style={{ margin: 0 }}>Telemetry Status:</span>
                  {metricSnapshot.stale ? (
                    <span className="badge badge-rollback">
                      <AlertTriangle size={12} /> Circuit Open / Prometheus Unreachable (Offline)
                    </span>
                  ) : (
                    <span className="badge badge-resolved">
                      <CheckCircle2 size={12} /> Circuit Closed — Live Prometheus Query Healthy
                    </span>
                  )}
                </div>

                <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: 0 }}>
                  <div className="stat-card">
                    <div className={`stat-icon ${isOffline ? 'warning' : metricSnapshot.errorRate > 10 ? 'critical' : 'success'}`}>
                      <Activity size={20} />
                    </div>
                    <div>
                      <div className="stat-value">
                        {!isOffline && metricSnapshot.errorRate != null ? `${metricSnapshot.errorRate.toFixed(2)}%` : '—'}
                      </div>
                      <div className="stat-label">Error Rate {!isOffline ? '' : '(Offline)'}</div>
                    </div>
                  </div>

                  <div className="stat-card">
                    <div className={`stat-icon ${isOffline ? 'warning' : metricSnapshot.avgLatencyMs > 300 ? 'warning' : 'info'}`}>
                      <Server size={20} />
                    </div>
                    <div>
                      <div className="stat-value">
                        {!isOffline && metricSnapshot.avgLatencyMs != null ? `${metricSnapshot.avgLatencyMs.toFixed(0)} ms` : '—'}
                      </div>
                      <div className="stat-label">Avg Latency {!isOffline ? '' : '(Offline)'}</div>
                    </div>
                  </div>

                  <div className="stat-card">
                    <div className="stat-icon info">
                      <Database size={20} />
                    </div>
                    <div>
                      <div className="stat-value" style={{ fontSize: '18px' }}>
                        {metricSnapshot.stale ? 'Circuit Open' : 'Prometheus Direct'}
                      </div>
                      <div className="stat-label">Telemetry Pipeline</div>
                    </div>
                  </div>
                </div>
              </div>
            ) : (
              <div className="empty-state">
                <AlertTriangle size={32} color="var(--color-warning)" />
                <h3 style={{ marginTop: '8px' }}>Service Unreachable</h3>
                <p>No Prometheus metrics data available for {selectedService}.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
