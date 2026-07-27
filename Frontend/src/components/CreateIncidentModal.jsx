import { useState } from 'react';
import { commandApi } from '../api/client';
import { X, AlertOctagon, Zap } from 'lucide-react';

export default function CreateIncidentModal({ onClose, onCreated }) {
  const [serviceName, setServiceName] = useState('FoodRush-Orders');
  const [severity, setSeverity] = useState('CRITICAL');
  const [customService, setCustomService] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    const targetService = customService.trim() || serviceName;
    if (!targetService) {
      setError('Service name is required');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const result = await commandApi.createIncident({
        serviceName: targetService,
        severity,
      });
      onCreated(result);
      onClose();
    } catch (err) {
      setError(err.response?.data?.error || err.message || 'Failed to simulate alert');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <AlertOctagon size={20} color="var(--color-warning)" />
            <h2>Simulate Monitored Service Alert</h2>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={onClose}>
            <X size={16} />
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Incidents are automatically detected when service metrics breach thresholds. Use this trigger to simulate a live alert POST to <code>/api/incidents</code>.
            </p>

            <div className="form-group">
              <label className="form-label">Target Monitored Service</label>
              <select
                className="form-select"
                value={serviceName}
                onChange={(e) => setServiceName(e.target.value)}
              >
                <option value="FoodRush-Orders">FoodRush-Orders (High Latency / Lag)</option>
                <option value="Payment-Gateway">Payment-Gateway (Elevated 5xx Rate)</option>
                <option value="DistributedJobForge">DistributedJobForge (Worker Deadlock)</option>
                <option value="Inventory-Worker">Inventory-Worker (DLQ Spillover)</option>
                <option value="custom">Custom Service...</option>
              </select>
            </div>

            {serviceName === 'custom' && (
              <div className="form-group">
                <label className="form-label">Custom Service Name</label>
                <input
                  className="form-input"
                  type="text"
                  placeholder="e.g. auth-service"
                  value={customService}
                  onChange={(e) => setCustomService(e.target.value)}
                  autoFocus
                />
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Alert Severity Level</label>
              <select
                className="form-select"
                value={severity}
                onChange={(e) => setSeverity(e.target.value)}
              >
                <option value="CRITICAL">CRITICAL (P1 — Service Down)</option>
                <option value="HIGH">HIGH (P2 — Latency Breach)</option>
                <option value="MEDIUM">MEDIUM (P3 — Degraded)</option>
                <option value="LOW">LOW (P4 — Warning)</option>
              </select>
            </div>

            {error && (
              <p style={{ color: 'var(--color-critical)', fontSize: '12px', marginTop: '8px' }}>
                {error}
              </p>
            )}
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-warning" disabled={loading}>
              <Zap size={14} />
              {loading ? 'Emitting Alert Event...' : 'Fire Alert Event'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
