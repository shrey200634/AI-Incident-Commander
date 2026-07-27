import { useState } from 'react';
import { agentApi } from '../api/client';
import { Cpu, Bot, Activity, RefreshCw, Send, ShieldAlert } from 'lucide-react';

export default function DiagnosticsPage() {
  const [prompt, setPrompt] = useState('Analyze FoodRush-Orders latency spike and check database deadlocks');
  const [aiResponse, setAiResponse] = useState('');
  const [aiLoading, setAiLoading] = useState(false);

  const [metricsService, setMetricsService] = useState('FoodRush-Orders');
  const [metricSnapshot, setMetricSnapshot] = useState(null);
  const [metricsLoading, setMetricsLoading] = useState(false);

  const handleTestAi = async (e) => {
    e.preventDefault();
    if (!prompt.trim()) return;
    setAiLoading(true);
    setAiResponse('');
    try {
      const res = await agentApi.testAi(prompt);
      setAiResponse(typeof res === 'string' ? res : JSON.stringify(res, null, 2));
    } catch (err) {
      setAiResponse(`Error testing AI agent: ${err.message}`);
    } finally {
      setAiLoading(false);
    }
  };

  const handleFetchMetrics = async () => {
    setMetricsLoading(true);
    try {
      const data = await agentApi.getMetrics(metricsService);
      setMetricSnapshot(data);
    } catch (err) {
      alert(`Metrics call failed: ${err.message}`);
    } finally {
      setMetricsLoading(false);
    }
  };

  return (
    <>
      <div className="page-header">
        <h1>AI Agent & Resilience Diagnostics</h1>
        <p>Direct testing interface for Spring AI model fallbacks (Gemini → Groq) and Resilience4j Prometheus circuit breaker</p>
      </div>

      <div className="page-body" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        {/* Left: Spring AI Model Fallback Tester */}
        <div className="card">
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Bot size={18} color="var(--accent)" />
              <span className="card-title">Spring AI Reasoning Engine</span>
            </div>
            <span className="badge badge-executing">Gemini 2.5 Flash / Groq Llama 3.3</span>
          </div>
          <div className="card-body">
            <form onSubmit={handleTestAi}>
              <div className="form-group">
                <label className="form-label">Diagnostic Prompt</label>
                <textarea
                  className="form-textarea"
                  rows={3}
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                />
              </div>
              <button className="btn btn-primary btn-sm" disabled={aiLoading} type="submit">
                <Send size={13} /> {aiLoading ? 'Reasoning...' : 'Submit to AI Agent'}
              </button>
            </form>

            {aiResponse && (
              <div style={{ marginTop: '16px' }}>
                <span className="form-label">Agent Output:</span>
                <div className="action-rationale" style={{
                  maxHeight: '300px',
                  overflowY: 'auto',
                  whiteSpace: 'pre-wrap',
                  fontSize: '12px',
                  fontFamily: 'var(--font-mono)'
                }}>
                  {aiResponse}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right: Resilience4j Circuit Breaker & Metrics */}
        <div className="card">
          <div className="card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldAlert size={18} color="var(--color-warning)" />
              <span className="card-title">Prometheus Metrics Circuit Breaker</span>
            </div>
            <span className="badge badge-monitoring">Resilience4j Protected</span>
          </div>
          <div className="card-body">
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '14px' }}>
              Simulates Prometheus metrics fetch wrapped with a 50% failure rate threshold circuit breaker and Redis cache fallback.
            </p>

            <div className="form-group">
              <label className="form-label">Target Microservice</label>
              <select
                className="form-select"
                value={metricsService}
                onChange={(e) => setMetricsService(e.target.value)}
              >
                <option value="FoodRush-Orders">FoodRush-Orders</option>
                <option value="Payment-Gateway">Payment-Gateway</option>
                <option value="DistributedJobForge">DistributedJobForge</option>
              </select>
            </div>

            <button className="btn btn-warning btn-sm" onClick={handleFetchMetrics} disabled={metricsLoading}>
              <RefreshCw size={13} className={metricsLoading ? 'spinning' : ''} />
              {metricsLoading ? 'Querying Metrics...' : 'Test Metrics Endpoint'}
            </button>

            {metricSnapshot && (
              <div style={{ marginTop: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                  <span className="form-label">Telemetry Snapshot:</span>
                  {metricSnapshot.stale ? (
                    <span className="badge badge-proposed">
                      <Activity size={10} /> Circuit Open — Redis Cache Fallback (Stale)
                    </span>
                  ) : (
                    <span className="badge badge-resolved">
                      <Activity size={10} /> Live Prometheus Fetch (Healthy)
                    </span>
                  )}
                </div>

                <div className="stats-grid" style={{ gridTemplateColumns: '1fr 1fr', gap: '10px', marginBottom: 0 }}>
                  <div className="stat-card" style={{ padding: '12px' }}>
                    <div>
                      <div className="stat-value" style={{ fontSize: '18px' }}>
                        {metricSnapshot.errorRate?.toFixed(2)}%
                      </div>
                      <div className="stat-label">Error Rate</div>
                    </div>
                  </div>
                  <div className="stat-card" style={{ padding: '12px' }}>
                    <div>
                      <div className="stat-value" style={{ fontSize: '18px' }}>
                        {metricSnapshot.avgLatencyMs?.toFixed(0)} ms
                      </div>
                      <div className="stat-label">Avg Latency</div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
