import { statusClass, statusLabel } from '../utils/helpers';

export default function StatusBadge({ status }) {
  const cls = statusClass(status);
  return (
    <span className={`badge badge-${cls}`}>
      <span className="badge-dot" />
      {statusLabel(status)}
    </span>
  );
}

export function SeverityBadge({ severity }) {
  if (!severity) return null;
  const s = severity.toLowerCase();
  let cls = 'low';
  if (s === 'critical' || s === 'p1') cls = 'critical';
  else if (s === 'high' || s === 'p2') cls = 'high';
  else if (s === 'medium' || s === 'p3') cls = 'medium';
  return (
    <span className={`badge badge-severity-${cls}`}>
      {severity.toUpperCase()}
    </span>
  );
}

export function ActionStatusBadge({ status }) {
  const cls = statusClass(status);
  return (
    <span className={`badge badge-${cls}`}>
      <span className="badge-dot" />
      {statusLabel(status)}
    </span>
  );
}
