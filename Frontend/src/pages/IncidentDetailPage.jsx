import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Server,
  Clock,
  User,
  CheckCircle2,
  XCircle,
  Play,
  RotateCcw,
  AlertTriangle,
  Shield,
  Bot,
  Loader2,
  Check,
  AlertOctagon,
  ArrowRight
} from 'lucide-react';
import { queryApi, commandApi } from '../api/client';
import StatusBadge, { SeverityBadge, ActionStatusBadge } from '../components/StatusBadge';
import { formatTime, timeAgo, statusLabel } from '../utils/helpers';

const STATE_MACHINE_STEPS = [
  'NEW',
  'INVESTIGATING',
  'ACTION_PROPOSED',
  'WAITING_APPROVAL',
  'EXECUTING',
  'MONITORING',
  'RESOLVED'
];

export default function IncidentDetailPage({ wsSubscribe, refreshActiveCount }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [incident, setIncident] = useState(null);
  const [actions, setActions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState({});
  const [error, setError] = useState('');

  // Approval & Rejection modals
  const [approvalModalAction, setApprovalModalAction] = useState(null);
  const [approvedByInput, setApprovedByInput] = useState('OpsEngineer');

  const [rejectModalAction, setRejectModalAction] = useState(null);
  const [rejectReasonInput, setRejectReasonInput] = useState('Requires further metrics evaluation');

  // Propose action modal
  const [showProposeModal, setShowProposeModal] = useState(false);
  const [proposeTypeInput, setProposeTypeInput] = useState('RESTART_SERVICE');
  const [proposeRationaleInput, setProposeRationaleInput] = useState('High error rate detected on microservice pod');

  const fetchDetail = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      let detail = null;

      try {
        detail = await queryApi.getIncidentDetail(id);
      } catch (err) {
        console.warn(`getIncidentDetail(${id}) failed, trying fallback getIncidentById...`, err);
        const inc = await queryApi.getIncidentById(id).catch(() => null);
        if (inc) {
          const acts = await queryApi.getActionsByIncident(id).catch(() => []);
          detail = { incident: inc, actions: acts };
        }
      }

      if (detail) {
        const incData = detail.incident || (detail.id ? detail : null);
        const actsData = Array.isArray(detail.actions) ? detail.actions : [];

        if (incData) {
          setIncident(incData);
          setActions(actsData);
        } else {
          setError(`Incident #${id} not found in database.`);
        }
      } else {
        setError(`Incident #${id} not found in database.`);
      }
    } catch (err) {
      console.error('Failed to fetch incident detail', err);
      setError(err.response?.data?.error || `Incident #${id} not found or backend unreachable.`);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  // STOMP WebSocket live updates for this incident
  useEffect(() => {
    const unsub = wsSubscribe(`/topic/incidents/${id}`, () => {
      fetchDetail();
    });
    return unsub;
  }, [wsSubscribe, id, fetchDetail]);

  const executeActionCall = async (actionId, callFn) => {
    setActionLoading((prev) => ({ ...prev, [actionId]: true }));
    try {
      await callFn();
      await fetchDetail();
      refreshActiveCount();
    } catch (err) {
      const msg = err.response?.data?.error || err.message || 'Operation failed';
      alert(`Command Execution Error: ${msg}`);
    } finally {
      setActionLoading((prev) => ({ ...prev, [actionId]: false }));
    }
  };

  const handleApprove = (actionId) => {
    executeActionCall(actionId, () =>
      commandApi.approveAction(id, actionId, approvedByInput || 'OpsCommander')
    );
  };

  const handleReject = (actionId) => {
    executeActionCall(actionId, () =>
      commandApi.rejectAction(id, actionId, rejectReasonInput || 'Rejected by commander')
    );
  };

  const confirmApprove = () => {
    if (!approvalModalAction) return;
    const actionId = approvalModalAction.id;
    setApprovalModalAction(null);
    handleApprove(actionId);
  };

  const confirmReject = () => {
    if (!rejectModalAction) return;
    const actionId = rejectModalAction.id;
    setRejectModalAction(null);
    handleReject(actionId);
  };

  const confirmPropose = async () => {
    setShowProposeModal(false);
    try {
      await commandApi.proposeAction(id, {
        actionType: proposeTypeInput,
        rationale: proposeRationaleInput,
      });
      setTimeout(fetchDetail, 300);
      await fetchDetail();
      refreshActiveCount();
    } catch (err) {
      alert(err.response?.data?.error || err.message || 'Failed to propose action');
    }
  };

  const handleExecute = (actionId) => {
    executeActionCall(actionId, () =>
      commandApi.executeAction(id, actionId)
    );
  };

  const handleRollback = (actionId) => {
    executeActionCall(actionId, () =>
      commandApi.rollbackAction(id, actionId)
    );
  };

  const handleResolve = () => {
    commandApi.updateStatus(id, 'RESOLVED').then(() => {
      fetchDetail();
      refreshActiveCount();
    }).catch(err => alert(err.response?.data?.error || 'Resolution failed'));
  };

  const handleEscalate = () => {
    const reason = prompt('Enter escalation rationale:', 'Automated remediation exhausted');
    if (!reason) return;
    commandApi.updateStatus(id, 'ESCALATED', reason).then(() => {
      fetchDetail();
      refreshActiveCount();
    }).catch(err => alert(err.response?.data?.error || 'Escalation failed'));
  };

  if (loading) {
    return (
      <div className="page-body">
        <div className="empty-state">
          <div className="spinner" />
        </div>
      </div>
    );
  }

  if (error || !incident) {
    return (
      <div className="page-body">
        <div className="empty-state">
          <AlertTriangle size={48} color="var(--color-critical)" />
          <h3 style={{ marginTop: '12px' }}>{error || 'Incident context not found'}</h3>
          <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
            Ensure backend services are running and an incident was created via API.
          </p>
          <button className="btn btn-ghost" style={{ marginTop: '14px' }} onClick={() => navigate('/incidents')}>
            <ArrowLeft size={14} /> Back to Incidents
          </button>
        </div>
      </div>
    );
  }

  const isTerminal = incident.status === 'RESOLVED' || incident.status === 'ESCALATED';
  const timelineEvents = buildTimeline(incident, actions);

  return (
    <>
      <div className="page-header">
        <button
          className="btn btn-ghost btn-sm"
          onClick={() => navigate('/incidents')}
          style={{ marginBottom: '12px' }}
        >
          <ArrowLeft size={14} /> Back to Incidents
        </button>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '16px', flexWrap: 'wrap' }}>
          <div>
            <h1 style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              Incident #{incident.id} — {incident.serviceName}
              <StatusBadge status={incident.status} />
            </h1>
            <p>Created {timeAgo(incident.createdAt)} &middot; CQRS Real-Time Synced</p>
          </div>
          {!isTerminal && (
            <div style={{ display: 'flex', gap: '8px' }}>
              <button className="btn btn-success btn-sm" onClick={handleResolve}>
                <CheckCircle2 size={14} /> Mark Resolved
              </button>
              <button className="btn btn-danger btn-sm" onClick={handleEscalate}>
                <AlertOctagon size={14} /> Escalate Incident
              </button>
            </div>
          )}
        </div>
      </div>

      <div className="page-body">
        {/* Incident State Machine Visualizer */}
        <div className="card" style={{ marginBottom: '20px', padding: '16px 20px' }}>
          <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '12px', letterSpacing: '0.5px' }}>
            CQRS Formal State Machine Pipeline
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', overflowX: 'auto', paddingBottom: '4px' }}>
            {STATE_MACHINE_STEPS.map((st, idx) => {
              const isActive = incident.status === st;
              const isPast = STATE_MACHINE_STEPS.indexOf(incident.status) > idx;
              return (
                <div key={st} style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
                  <div style={{
                    padding: '6px 12px',
                    borderRadius: '20px',
                    fontSize: '11px',
                    fontWeight: 700,
                    background: isActive ? 'var(--accent)' : isPast ? 'var(--accent-muted)' : 'var(--bg-elevated)',
                    color: isActive ? '#fff' : isPast ? 'var(--accent-text)' : 'var(--text-muted)',
                    border: isActive ? 'none' : '1px solid var(--border-subtle)',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                  }}>
                    {isPast && <Check size={12} />}
                    {statusLabel(st)}
                  </div>
                  {idx < STATE_MACHINE_STEPS.length - 1 && (
                    <ArrowRight size={12} color="var(--border-strong)" />
                  )}
                </div>
              );
            })}

            {incident.status === 'ROLLBACK' && (
              <div style={{ padding: '6px 12px', borderRadius: '20px', fontSize: '11px', fontWeight: 700, background: 'var(--color-critical-muted)', color: 'var(--color-critical)' }}>
                ROLLBACK IN PROGRESS
              </div>
            )}
            {incident.status === 'ESCALATED' && (
              <div style={{ padding: '6px 12px', borderRadius: '20px', fontSize: '11px', fontWeight: 700, background: 'var(--color-critical-muted)', color: 'var(--color-critical)' }}>
                ESCALATED TO HUMAN COMMANDER
              </div>
            )}
          </div>
        </div>

        {/* Info Chips */}
        <div className="info-row" style={{ marginBottom: '20px' }}>
          <div className="info-chip">
            <Server size={14} />
            Service: <strong>{incident.serviceName}</strong>
          </div>
          <div className="info-chip">
            <Shield size={14} />
            Severity: <strong><SeverityBadge severity={incident.severity} /></strong>
          </div>
          <div className="info-chip">
            <Clock size={14} />
            Created: <strong>{formatTime(incident.createdAt)}</strong>
          </div>
          {incident.resolvedAt && (
            <div className="info-chip">
              <CheckCircle2 size={14} />
              Resolved: <strong>{formatTime(incident.resolvedAt)}</strong>
            </div>
          )}
          {incident.escalationReason && (
            <div className="info-chip" style={{ borderLeft: '3px solid var(--color-critical)' }}>
              <AlertTriangle size={14} />
              Escalation Rationale: <strong>{incident.escalationReason}</strong>
            </div>
          )}
        </div>

        <div className="detail-grid">
          {/* Left: Interactive Timeline & Events */}
          <div className="detail-main">
            <div className="card">
              <div className="card-header">
                <span className="card-title">Incident Audit & Event Stream</span>
                <span className="live-indicator">
                  <span className="live-pulse" />
                  Kafka Real-Time Sync
                </span>
              </div>
              <div className="card-body">
                {timelineEvents.length === 0 ? (
                  <div className="empty-state" style={{ padding: '30px' }}>
                    <Clock size={32} />
                    <p style={{ marginTop: '8px' }}>Awaiting event emissions...</p>
                  </div>
                ) : (
                  <div className="timeline">
                    {timelineEvents.map((evt, idx) => (
                      <div className="timeline-item" key={idx}>
                        <div className={`timeline-dot ${evt.dotClass}`} />
                        <div className="timeline-content">
                          <div className="timeline-header">
                            <span className="timeline-title">{evt.title}</span>
                            <span className="timeline-time">{formatTime(evt.time)}</span>
                          </div>
                          {evt.description && (
                            <div className="timeline-body">{evt.description}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right: AI Remediation Actions Panel */}
          <div className="detail-sidebar-panel">
            <div className="card">
              <div className="card-header" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Bot size={18} color="var(--accent)" />
                  <span className="card-title">Remediation Proposals</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span className="badge badge-new">{actions.length} Actions</span>
                  {!isTerminal && (
                    <button className="btn btn-ghost btn-sm" style={{ fontSize: '11px', padding: '2px 8px' }} onClick={() => setShowProposeModal(true)}>
                      + Propose Action
                    </button>
                  )}
                </div>
              </div>
              <div className="card-body" style={{ padding: actions.length === 0 ? '20px' : '12px' }}>
                {actions.length === 0 ? (
                  <div className="empty-state" style={{ padding: '24px' }}>
                    <Bot size={36} color="var(--text-muted)" />
                    <h3 style={{ marginTop: '10px', fontSize: '13px' }}>AI Agent Investigating...</h3>
                    <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      Evaluating diagnostic tools (`getServiceHealth`, `checkDatabaseDeadlocks`, `getKafkaConsumerLag`).
                    </p>
                  </div>
                ) : (
                  actions.map((action) => {
                    const rationaleText = action.rationals || action.rationale || 'AI proposal';
                    const rollbackTarget = action.rollBackOf || action.rollbackOf;

                    return (
                      <div className="action-card" key={action.id}>
                        <div className="action-card-header">
                          <span className="action-type">{action.actionType}</span>
                          <ActionStatusBadge status={action.status} />
                        </div>

                        <div className="action-rationale">
                          <Bot size={13} style={{ marginRight: '6px', verticalAlign: 'middle', color: 'var(--accent)' }} />
                          {rationaleText}
                        </div>

                        <div className="action-meta">
                          {action.approvedBy && (
                            <span>
                              <User size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                              {action.approvedBy}
                            </span>
                          )}
                          {action.executedAt && (
                            <span>
                              <Clock size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                              {formatTime(action.executedAt)}
                            </span>
                          )}
                          {rollbackTarget && (
                            <span style={{ color: 'var(--color-critical)' }}>
                              <RotateCcw size={11} style={{ verticalAlign: 'middle', marginRight: '3px' }} />
                              Rollback of Action #{rollbackTarget}
                            </span>
                          )}
                        </div>

                        {/* Control Buttons */}
                        <div className="action-buttons">
                          {action.status === 'PROPOSED' && !isTerminal && (
                            <>
                              <button
                                className="btn btn-success btn-sm"
                                onClick={() => handleApprove(action.id)}
                                disabled={actionLoading[action.id]}
                              >
                                {actionLoading[action.id] ? (
                                  <Loader2 size={13} className="spinning" />
                                ) : (
                                  <CheckCircle2 size={13} />
                                )}
                                Approve Proposal
                              </button>
                              <button
                                className="btn btn-danger btn-sm"
                                onClick={() => handleReject(action.id)}
                                disabled={actionLoading[action.id]}
                              >
                                {actionLoading[action.id] ? (
                                  <Loader2 size={13} className="spinning" />
                                ) : (
                                  <XCircle size={13} />
                                )}
                                Reject
                              </button>
                            </>
                          )}

                          {action.status === 'APPROVED' && !isTerminal && (
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => handleExecute(action.id)}
                              disabled={actionLoading[action.id]}
                            >
                              {actionLoading[action.id] ? (
                                <Loader2 size={13} className="spinning" />
                              ) : (
                                <Play size={13} />
                              )}
                              Execute Remediation
                            </button>
                          )}

                          {action.status === 'EXECUTED' && !rollbackTarget && !isTerminal && (
                            <button
                              className="btn btn-warning btn-sm"
                              onClick={() => handleRollback(action.id)}
                              disabled={actionLoading[action.id]}
                            >
                              {actionLoading[action.id] ? (
                                <Loader2 size={13} className="spinning" />
                              ) : (
                                <RotateCcw size={13} />
                              )}
                              Saga Compensating Rollback
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Approval Modal */}
      {approvalModalAction && (
        <div className="modal-overlay" onClick={() => setApprovalModalAction(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Approve Remediation Proposal #{approvalModalAction.id}</h2>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '14px' }}>
                Action: <strong style={{ color: 'var(--accent)' }}>{approvalModalAction.actionType}</strong>
              </p>
              <div className="form-group">
                <label className="form-label">Approving Commander ID / Name</label>
                <input
                  className="form-input"
                  type="text"
                  value={approvedByInput}
                  onChange={(e) => setApprovedByInput(e.target.value)}
                  autoFocus
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setApprovalModalAction(null)}>
                Cancel
              </button>
              <button className="btn btn-success" onClick={confirmApprove}>
                <CheckCircle2 size={14} /> Approve Action
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Rejection Modal */}
      {rejectModalAction && (
        <div className="modal-overlay" onClick={() => setRejectModalAction(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Reject Remediation Proposal #{rejectModalAction.id}</h2>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Rejection Reason</label>
                <textarea
                  className="form-textarea"
                  value={rejectReasonInput}
                  onChange={(e) => setRejectReasonInput(e.target.value)}
                  autoFocus
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setRejectModalAction(null)}>
                Cancel
              </button>
              <button className="btn btn-danger" onClick={confirmReject}>
                <XCircle size={14} /> Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Propose Action Modal */}
      {showProposeModal && (
        <div className="modal-overlay" onClick={() => setShowProposeModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Propose Remediation Action</h2>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Action Type</label>
                <select
                  className="form-select"
                  value={proposeTypeInput}
                  onChange={(e) => setProposeTypeInput(e.target.value)}
                >
                  <option value="RESTART_SERVICE">RESTART_SERVICE (Restart Container)</option>
                  <option value="SCALE_WORKER_PODS">SCALE_WORKER_PODS (Scale Replicas)</option>
                  <option value="CLEAR_DEAD_LETTER_QUEUE">CLEAR_DEAD_LETTER_QUEUE (Purge Kafka DLQ)</option>
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Rationale</label>
                <textarea
                  className="form-textarea"
                  value={proposeRationaleInput}
                  onChange={(e) => setProposeRationaleInput(e.target.value)}
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setShowProposeModal(false)}>
                Cancel
              </button>
              <button className="btn btn-primary" onClick={confirmPropose}>
                Submit Proposal
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function buildTimeline(incident, actions) {
  const events = [];

  events.push({
    title: 'Incident Created & Alert Ingested',
    description: `Service "${incident.serviceName}" initialized with ${incident.severity} severity status.`,
    time: incident.createdAt,
    dotClass: 'created',
  });

  actions.forEach((a) => {
    const rationaleText = a.rationals || a.rationale || 'AI proposal';
    const rollbackTarget = a.rollBackOf || a.rollbackOf;

    events.push({
      title: `AI Agent Proposed: ${a.actionType}`,
      description: rationaleText,
      time: a.createdAt,
      dotClass: 'proposed',
    });

    if (a.status === 'APPROVED' || a.status === 'EXECUTED' || a.status === 'ROLLED_BACK') {
      events.push({
        title: `Human Approval Granted`,
        description: a.approvedBy ? `Approved by ${a.approvedBy}` : 'Approved by Operations Commander',
        time: a.createdAt,
        dotClass: 'approved',
      });
    }

    if (a.status === 'REJECTED') {
      events.push({
        title: `Action Proposal Rejected`,
        description: 'Proposal rejected. Agent returning to INVESTIGATING status.',
        time: a.lastUpdatedAt || a.createdAt,
        dotClass: 'rejected',
      });
    }

    if (a.status === 'EXECUTED' || a.status === 'ROLLED_BACK') {
      events.push({
        title: rollbackTarget ? `Compensating Rollback Executed` : `Remediation Executed: ${a.actionType}`,
        description: rollbackTarget
          ? `Saga rollback of Action #${rollbackTarget} executed.`
          : `Remediation side effect executed on ${incident.serviceName}.`,
        time: a.executedAt || a.lastUpdatedAt,
        dotClass: rollbackTarget ? 'rollback' : 'executed',
      });
    }

    if (a.status === 'ROLLED_BACK') {
      events.push({
        title: 'Action Triggered Compensating Rollback',
        description: `Action #${a.id} rolled back.`,
        time: a.lastUpdatedAt,
        dotClass: 'rollback',
      });
    }
  });

  if (incident.status === 'RESOLVED') {
    events.push({
      title: 'Incident Resolved',
      description: 'Incident status marked RESOLVED.',
      time: incident.resolvedAt || incident.lastUpdated,
      dotClass: 'resolved',
    });
  }

  if (incident.status === 'ESCALATED') {
    events.push({
      title: 'Incident Escalated',
      description: incident.escalationReason || 'Escalated to lead site reliability engineer.',
      time: incident.lastUpdated,
      dotClass: 'escalated',
    });
  }

  return events;
}
