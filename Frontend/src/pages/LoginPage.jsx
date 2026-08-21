import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/auth';
import './LoginPage.css';

/* Decorative — mirrors the real event vocabulary from the platform
   (severities, action types) but is not live data. Purely atmospheric. */
const FEED_POOL = [
  { severity: 'critical', service: 'order-service', text: 'SEV1 · investigating root cause' },
  { severity: 'high', service: 'payment-service', text: 'AI proposed RESTART_SERVICE · awaiting approval' },
  { severity: 'medium', service: 'auth-service', text: 'SEV3 · consumer lag detected on Kafka' },
  { severity: 'low', service: 'checkout-service', text: 'resolved · metrics nominal for 5m' },
  { severity: 'high', service: 'inventory-service', text: 'SCALE_WORKER_PODS executed · monitoring' },
  { severity: 'critical', service: 'notification-service', text: 'rollback triggered · re-investigating' },
];

const STATUS_ITEMS = ['Kafka', 'MySQL', 'Redis', 'Docker', 'Prometheus'];

export default function LoginPage() {
  const [userName, setUserName] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [feed, setFeed] = useState(FEED_POOL.slice(0, 4));
  const navigate = useNavigate();
  const poolIndex = useRef(4);

  useEffect(() => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) return;

    const interval = setInterval(() => {
      const next = FEED_POOL[poolIndex.current % FEED_POOL.length];
      poolIndex.current += 1;
      setFeed((prev) => [{ ...next, _key: Date.now() }, ...prev.slice(0, 3)]);
    }, 3200);
    return () => clearInterval(interval);
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      await login(userName, password);
      navigate('/');
    } catch (err) {
      if (err.response?.status === 401) {
        setError('Invalid username or password');
      } else {
        setError(`Login failed: ${err.message}`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="login-screen">
      {/* ---------- Left: mission control panel ---------- */}
      <div className="login-brand-panel">
        <div className="login-brand-glow" aria-hidden="true" />

        <div className="login-brand-header">
          <div className="login-logo">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L3 6.5V12C3 17 6.8 21.4 12 22.5C17.2 21.4 21 17 21 12V6.5L12 2Z" stroke="white" strokeWidth="1.6" strokeLinejoin="round"/>
              <path d="M9 12.2L11.2 14.4L15.3 9.8" stroke="white" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <div>
            <div className="login-brand-title">AI Incident Commander</div>
            <div className="login-brand-subtitle">Ops Console</div>
          </div>
        </div>

        <div className="login-brand-copy">
          <h1>Every signal, one console.</h1>
          <p>
            AI agents investigate production incidents around the clock and
            propose remediations. Nothing executes without your approval.
          </p>
        </div>

        <div className="login-status-block">
          <div className="login-section-label">System status</div>
          <div className="login-status-row">
            {STATUS_ITEMS.map((name) => (
              <div className="login-status-chip" key={name}>
                <span className="connection-dot connected" />
                {name}
              </div>
            ))}
          </div>
        </div>

        <div className="login-feed-block">
          <div className="login-section-label">
            <span className="live-indicator">
              <span className="live-pulse" />
              Live feed
            </span>
          </div>
          <div className="login-feed">
            {feed.map((item, i) => (
              <div
                className="login-feed-item"
                key={item._key || `seed-${i}`}
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <span className={`badge badge-severity-${item.severity}`}>
                  <span className="badge-dot" />
                  {item.severity === 'critical' ? 'SEV1' : item.severity === 'high' ? 'SEV2' : item.severity === 'medium' ? 'SEV3' : 'OK'}
                </span>
                <span className="login-feed-text">
                  <strong>{item.service}</strong> · {item.text}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ---------- Right: sign-in form ---------- */}
      <div className="login-form-panel">
        <div className="login-form-card">
          <div className="login-form-header">
            <h2>Sign in</h2>
            <p>Access your incident dashboard</p>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <div className="form-group">
              <label className="form-label" htmlFor="login-username">Username</label>
              <div className="login-input-wrapper">
                <svg className="login-input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M12 12a4 4 0 100-8 4 4 0 000 8zM4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                <input
                  id="login-username"
                  className="form-input login-input"
                  placeholder="ops-engineer"
                  value={userName}
                  onChange={(e) => setUserName(e.target.value)}
                  autoComplete="username"
                  disabled={isLoading}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="login-password">Password</label>
              <div className="login-input-wrapper">
                <svg className="login-input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <rect x="5" y="11" width="14" height="9" rx="2" stroke="currentColor" strokeWidth="1.6"/>
                  <path d="M8 11V7a4 4 0 118 0v4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
                </svg>
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  className="form-input login-input"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                  disabled={isLoading}
                  required
                />
                <button
                  type="button"
                  className="login-visibility-toggle"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.9 5.1A9.4 9.4 0 0112 5c5 0 9 4 10 7-.4 1.2-1.2 2.5-2.3 3.6M6.5 6.7C4.6 8 3.3 9.8 2 12c1 3 5 7 10 7 1.3 0 2.5-.2 3.6-.6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/></svg>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round"/><circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.6"/></svg>
                  )}
                </button>
              </div>
            </div>

            {error && (
              <div className="login-error" role="alert">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.6"/><path d="M12 8v5M12 16h.01" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/></svg>
                {error}
              </div>
            )}

            <button type="submit" className="btn btn-primary login-submit" disabled={isLoading}>
              {isLoading ? (
                <>
                  <span className="login-btn-spinner" />
                  Signing in…
                </>
              ) : (
                <>
                  Sign in
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none"><path d="M5 12h14M13 6l6 6-6 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg>
                </>
              )}
            </button>
          </form>

          <p className="login-footnote">
            Every remediation still requires your explicit approval before it runs.
          </p>
        </div>
      </div>
    </div>
  );
}
