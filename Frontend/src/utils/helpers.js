/**
 * Format a LocalDateTime array or ISO string into a readable time.
 */
export function formatTime(value) {
  if (!value) return '—';
  let date;
  if (Array.isArray(value)) {
    // Java LocalDateTime serializes as [2026, 7, 27, 18, 30, 0]
    const [y, mo, d, h = 0, m = 0, s = 0] = value;
    date = new Date(y, mo - 1, d, h, m, s);
  } else {
    date = new Date(value);
  }
  if (isNaN(date.getTime())) return '—';
  return date.toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  });
}

/**
 * Relative time (e.g. "3m ago", "2h ago")
 */
export function timeAgo(value) {
  if (!value) return '';
  let date;
  if (Array.isArray(value)) {
    const [y, mo, d, h = 0, m = 0, s = 0] = value;
    date = new Date(y, mo - 1, d, h, m, s);
  } else {
    date = new Date(value);
  }
  if (isNaN(date.getTime())) return '';
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

/**
 * Normalize status string for CSS class matching.
 * e.g. "WAITING_APPROVAL" -> "waiting_approval"
 */
export function statusClass(status) {
  if (!status) return '';
  return status.toLowerCase().replace(/\s+/g, '_');
}

/**
 * Human-readable status label
 */
export function statusLabel(status) {
  if (!status) return '';
  return status.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

/**
 * Severity CSS class
 */
export function severityClass(severity) {
  if (!severity) return '';
  const s = severity.toLowerCase();
  if (s === 'critical' || s === 'p1') return 'critical';
  if (s === 'high' || s === 'p2') return 'high';
  if (s === 'medium' || s === 'p3') return 'medium';
  return 'low';
}
