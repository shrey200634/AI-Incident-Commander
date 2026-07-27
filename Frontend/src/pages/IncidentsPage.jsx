import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Search,
  Plus,
  ArrowRight,
  ArrowUpDown,
  Inbox,
} from 'lucide-react';
import { queryApi } from '../api/client';
import StatusBadge, { SeverityBadge } from '../components/StatusBadge';
import CreateIncidentModal from '../components/CreateIncidentModal';
import { timeAgo, formatTime } from '../utils/helpers';

const FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'active', label: 'Active' },
  { key: 'resolved', label: 'Resolved' },
  { key: 'escalated', label: 'Escalated' },
];

export default function IncidentsPage({ wsSubscribe, refreshActiveCount }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [sortField, setSortField] = useState('id');
  const [sortDir, setSortDir] = useState('desc');

  const activeFilter = searchParams.get('filter') || 'all';

  const fetchIncidents = useCallback(async () => {
    try {
      let data;
      if (activeFilter === 'active') {
        data = await queryApi.getActiveIncidents();
      } else if (activeFilter === 'resolved') {
        data = await queryApi.getIncidentsByStatus('RESOLVED');
      } else if (activeFilter === 'escalated') {
        data = await queryApi.getIncidentsByStatus('ESCALATED');
      } else {
        data = await queryApi.getAllIncidents();
      }
      setIncidents(data);
    } catch (err) {
      console.error('Failed to fetch incidents', err);
    } finally {
      setLoading(false);
    }
  }, [activeFilter]);

  useEffect(() => {
    setLoading(true);
    fetchIncidents();
  }, [fetchIncidents]);

  useEffect(() => {
    const unsub = wsSubscribe('/topic/incidents/active', () => {
      fetchIncidents();
    });
    return unsub;
  }, [wsSubscribe, fetchIncidents]);

  const handleSort = (field) => {
    if (sortField === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDir('desc');
    }
  };

  const filtered = incidents
    .filter((inc) => {
      if (!search) return true;
      const s = search.toLowerCase();
      return (
        inc.serviceName?.toLowerCase().includes(s) ||
        inc.severity?.toLowerCase().includes(s) ||
        inc.status?.toLowerCase().includes(s) ||
        String(inc.id).includes(s)
      );
    })
    .sort((a, b) => {
      let aVal = a[sortField];
      let bVal = b[sortField];
      if (sortField === 'id') {
        aVal = Number(aVal);
        bVal = Number(bVal);
      }
      if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
      return 0;
    });

  return (
    <>
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <h1>Incidents</h1>
            <p>Browse and manage all incidents</p>
          </div>
          <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
            <Plus size={15} /> New Incident
          </button>
        </div>
      </div>

      <div className="page-body">
        {/* Filters */}
        <div className="filters-bar">
          {FILTERS.map((f) => (
            <button
              key={f.key}
              className={`filter-btn${activeFilter === f.key ? ' active' : ''}`}
              onClick={() => {
                if (f.key === 'all') {
                  setSearchParams({});
                } else {
                  setSearchParams({ filter: f.key });
                }
              }}
            >
              {f.label}
            </button>
          ))}
          <div className="search-wrapper">
            <Search size={15} />
            <input
              className="search-input"
              placeholder="Search by service, severity, status..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {/* Table */}
        <div className="card">
          {loading ? (
            <div className="empty-state">
              <div className="spinner" />
            </div>
          ) : filtered.length === 0 ? (
            <div className="empty-state">
              <Inbox size={48} />
              <h3>No incidents found</h3>
              <p>
                {search
                  ? 'Try adjusting your search terms.'
                  : 'Create a new incident to get started.'}
              </p>
            </div>
          ) : (
            <table className="incident-table">
              <thead>
                <tr>
                  <th
                    onClick={() => handleSort('id')}
                    style={{ cursor: 'pointer', userSelect: 'none' }}
                  >
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                      ID <ArrowUpDown size={12} />
                    </span>
                  </th>
                  <th
                    onClick={() => handleSort('serviceName')}
                    style={{ cursor: 'pointer', userSelect: 'none' }}
                  >
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                      Service <ArrowUpDown size={12} />
                    </span>
                  </th>
                  <th>Severity</th>
                  <th
                    onClick={() => handleSort('status')}
                    style={{ cursor: 'pointer', userSelect: 'none' }}
                  >
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                      Status <ArrowUpDown size={12} />
                    </span>
                  </th>
                  <th>Created</th>
                  <th>Last Update</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((inc) => (
                  <tr key={inc.id} onClick={() => navigate(`/incidents/${inc.id}`)}>
                    <td>
                      <span className="incident-id">#{inc.id}</span>
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
                      <span className="time-ago">{formatTime(inc.createdAt)}</span>
                    </td>
                    <td>
                      <span className="time-ago">{timeAgo(inc.lastUpdated || inc.createdAt)}</span>
                    </td>
                    <td>
                      <ArrowRight size={14} style={{ color: 'var(--text-muted)' }} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {showCreate && (
        <CreateIncidentModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            fetchIncidents();
            refreshActiveCount();
          }}
        />
      )}
    </>
  );
}
