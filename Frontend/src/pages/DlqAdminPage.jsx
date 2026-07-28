import { useState, useEffect, useCallback } from 'react';
import { adminDlqApi } from '../api/client';
import {
  Database,
  RefreshCw,
  Play,
  CheckCircle2,
  AlertTriangle,
  FileCode,
  Check
} from 'lucide-react';
import { formatTime } from '../utils/helpers';

export default function DlqAdminPage({ onDlqUpdate }) {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('unreplayed'); // 'unreplayed' | 'all'
  const [replayingId, setReplayingId] = useState(null);
  const [selectedRecord, setSelectedRecord] = useState(null);

  const fetchDlq = useCallback(async () => {
    try {
      setLoading(true);
      const data = filter === 'unreplayed'
        ? await adminDlqApi.getUnreplayed()
        : await adminDlqApi.getAll();
      setRecords(data || []);
      if (onDlqUpdate && Array.isArray(data)) {
        onDlqUpdate(data.filter(r => !r.replayed).length);
      }
    } catch (err) {
      console.error('Failed to fetch DLQ records', err);
    } finally {
      setLoading(false);
    }
  }, [filter, onDlqUpdate]);

  useEffect(() => {
    fetchDlq();
  }, [fetchDlq]);

  const handleReplay = async (id) => {
    try {
      setReplayingId(id);
      await adminDlqApi.replay(id);
      await fetchDlq();
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to replay DLQ record to Kafka');
    } finally {
      setReplayingId(null);
    }
  };

  return (
    <>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <h1 style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Database size={24} color="var(--color-warning)" />
              Kafka Dead-Letter Queue (DLQ) Admin
            </h1>
            <p>Inspect unprocessable incident lifecycle events and trigger manual Kafka topic replay</p>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={fetchDlq}>
            <RefreshCw size={14} /> Refresh DLQ
          </button>
        </div>
      </div>

      <div className="page-body">
        {/* Filters */}
        <div className="filters-bar">
          <button
            className={`filter-btn${filter === 'unreplayed' ? ' active' : ''}`}
            onClick={() => setFilter('unreplayed')}
          >
            Pending Replay ({records.filter(r => !r.replayed).length})
          </button>
          <button
            className={`filter-btn${filter === 'all' ? ' active' : ''}`}
            onClick={() => setFilter('all')}
          >
            All DLQ Log History
          </button>
        </div>

        {/* DLQ Records Table */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Dead-Letter Queue Events</span>
          </div>

          {loading ? (
            <div className="empty-state">
              <div className="spinner" />
            </div>
          ) : records.length === 0 ? (
            <div className="empty-state">
              <CheckCircle2 size={48} color="var(--color-success)" />
              <h3>DLQ Clean & Healthy</h3>
              <p>No failed or malformed Kafka messages currently resting in the dead-letter queue.</p>
            </div>
          ) : (
            <table className="incident-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Original Kafka Topic</th>
                  <th>Status</th>
                  <th>Recorded Time</th>
                  <th>Payload Preview</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {records.map((rec) => (
                  <tr key={rec.id}>
                    <td>
                      <span className="incident-id">#{rec.id}</span>
                    </td>
                    <td>
                      <strong style={{ fontFamily: 'var(--font-mono)', color: 'var(--accent-text)' }}>
                        {rec.originalTopic}
                      </strong>
                    </td>
                    <td>
                      {rec.replayed ? (
                        <span className="badge badge-resolved">
                          <Check size={12} /> Replayed
                        </span>
                      ) : (
                        <span className="badge badge-proposed">
                          <AlertTriangle size={12} /> Pending Replay
                        </span>
                      )}
                    </td>
                    <td>
                      <span className="time-ago">{formatTime(rec.failedAt || rec.createdAt)}</span>
                    </td>
                    <td>
                      <button
                        className="btn btn-ghost btn-sm"
                        style={{ fontSize: '11px', gap: '4px' }}
                        onClick={() => setSelectedRecord(rec)}
                      >
                        <FileCode size={13} /> View Payload
                      </button>
                    </td>
                    <td>
                      {!rec.replayed ? (
                        <button
                          className="btn btn-warning btn-sm"
                          disabled={replayingId === rec.id}
                          onClick={() => handleReplay(rec.id)}
                        >
                          <Play size={12} />
                          {replayingId === rec.id ? 'Replaying...' : 'Replay to Kafka'}
                        </button>
                      ) : (
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          Replayed at {formatTime(rec.replayedAt)}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Payload Inspection Modal */}
      {selectedRecord && (
        <div className="modal-overlay" onClick={() => setSelectedRecord(null)}>
          <div className="modal" style={{ maxWidth: '650px' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>DLQ Record #{selectedRecord.id} Details</h2>
            </div>
            <div className="modal-body">
              <div style={{ marginBottom: '12px' }}>
                <span className="form-label">Topic:</span>
                <code>{selectedRecord.originalTopic}</code>
              </div>
              {selectedRecord.errorMessage && (
                <div style={{ marginBottom: '12px' }}>
                  <span className="form-label">Failure Exception:</span>
                  <div className="action-rationale" style={{ borderLeftColor: 'var(--color-critical)' }}>
                    {selectedRecord.errorMessage}
                  </div>
                </div>
              )}
              <div style={{ marginBottom: '12px' }}>
                <span className="form-label">Stored JSON Payload:</span>
                <pre style={{
                  background: 'var(--bg-elevated)',
                  padding: '12px',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '12px',
                  fontFamily: 'var(--font-mono)',
                  overflowX: 'auto',
                  maxHeight: '250px'
                }}>
                  {typeof selectedRecord.payload === 'string'
                    ? selectedRecord.payload
                    : JSON.stringify(selectedRecord.payload, null, 2)}
                </pre>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setSelectedRecord(null)}>
                Close
              </button>
              {!selectedRecord.replayed && (
                <button
                  className="btn btn-warning"
                  onClick={() => {
                    handleReplay(selectedRecord.id);
                    setSelectedRecord(null);
                  }}
                >
                  <Play size={14} /> Replay Now
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
