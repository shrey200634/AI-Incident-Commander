import { NavLink, useLocation } from 'react-router-dom';
import {
  Activity,
  LayoutDashboard,
  AlertTriangle,
  CheckCircle2,
  Cpu,
  Zap,
  Database,
  Wrench
} from 'lucide-react';

export default function Sidebar({ connected, activeCount, dlqCount }) {
  const location = useLocation();

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-brand">
          <div className="sidebar-logo">
            <Zap size={22} />
          </div>
          <div>
            <div className="sidebar-title">AI Incident Commander</div>
            <div className="sidebar-subtitle">Autonomous Ops & CQRS</div>
          </div>
        </div>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section-label">Operations Center</div>
        <NavLink
          to="/"
          end
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <LayoutDashboard size={18} />
          Live Command Center
          {activeCount > 0 && <span className="nav-badge">{activeCount}</span>}
        </NavLink>

        <div className="nav-section-label">Incident Management</div>
        <NavLink
          to="/incidents"
          end
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <AlertTriangle size={18} />
          All Incidents
        </NavLink>
        <NavLink
          to="/incidents?filter=active"
          className={() =>
            `nav-item${location.pathname === '/incidents' && location.search.includes('active') ? ' active' : ''}`
          }
        >
          <Activity size={18} />
          Active Incidents
        </NavLink>
        <NavLink
          to="/incidents?filter=resolved"
          className={() =>
            `nav-item${location.pathname === '/incidents' && location.search.includes('resolved') ? ' active' : ''}`
          }
        >
          <CheckCircle2 size={18} />
          Resolved History
        </NavLink>

        <div className="nav-section-label">Resilience & Telemetry</div>
        <NavLink
          to="/dlq"
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <Database size={18} />
          Kafka DLQ Admin
          {dlqCount > 0 && <span className="nav-badge" style={{ background: 'var(--color-warning)', color: '#000' }}>{dlqCount}</span>}
        </NavLink>
        <NavLink
          to="/telemetry"
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <Cpu size={18} />
          Telemetry & Circuit Breaker
        </NavLink>
        <NavLink
          to="/diagnostics"
          className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
        >
          <Wrench size={18} />
          AI Diagnostics
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <div className="connection-status">
          <span className={`connection-dot${connected ? ' connected' : ''}`} />
          {connected ? 'STOMP WebSocket Connected' : 'Disconnected / Polling'}
        </div>
      </div>
    </aside>
  );
}
