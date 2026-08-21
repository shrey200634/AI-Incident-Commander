import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useState, useEffect, useCallback } from 'react';
import Sidebar from './components/Sidebar';
import DashboardPage from './pages/DashboardPage';
import IncidentsPage from './pages/IncidentsPage';
import IncidentDetailPage from './pages/IncidentDetailPage';
import DlqAdminPage from './pages/DlqAdminPage';
import TelemetryPage from './pages/TelemetryPage';
import LoginPage from './pages/LoginPage';
import { useWebSocket } from './hooks/useWebSocket';
import { queryApi, adminDlqApi } from './api/client';
import { authStorage } from './api/auth';

function AuthenticatedApp() {
  const { connected, subscribe } = useWebSocket();
  const [activeCount, setActiveCount] = useState(0);
  const [dlqCount, setDlqCount] = useState(0);

  const refreshCounts = useCallback(() => {
    queryApi.getActiveIncidents()
      .then((list) => setActiveCount(list?.length || 0))
      .catch(() => setActiveCount(0));

    adminDlqApi.getUnreplayed()
      .then((list) => setDlqCount(list?.length || 0))
      .catch(() => setDlqCount(0));
  }, []);

  useEffect(() => {
    refreshCounts();
    const interval = setInterval(refreshCounts, 10000);
    return () => clearInterval(interval);
  }, [refreshCounts]);

  // STOMP WebSocket real-time event listener
  useEffect(() => {
    const unsub = subscribe('/topic/incidents/active', () => {
      refreshCounts();
    });
    return unsub;
  }, [subscribe, refreshCounts]);

  return (
    <div className="app-layout">
      <Sidebar
        connected={connected}
        activeCount={activeCount}
        dlqCount={dlqCount}
      />
      <main className="main-content">
        <Routes>
          <Route
            path="/"
            element={
              <DashboardPage
                wsSubscribe={subscribe}
                refreshActiveCount={refreshCounts}
              />
            }
          />
          <Route
            path="/incidents"
            element={
              <IncidentsPage
                wsSubscribe={subscribe}
                refreshActiveCount={refreshCounts}
              />
            }
          />
          <Route
            path="/incidents/:id"
            element={
              <IncidentDetailPage
                wsSubscribe={subscribe}
                refreshActiveCount={refreshCounts}
              />
            }
          />
          <Route
            path="/dlq"
            element={
              <DlqAdminPage
                onDlqUpdate={(count) => setDlqCount(count)}
              />
            }
          />
          <Route
            path="/telemetry"
            element={<TelemetryPage />}
          />
        </Routes>
      </main>
    </div>
  );
}

// Subscribing to useLocation() forces this component to re-render on every
// navigation, so authStorage.isLoggedIn() is re-checked each time instead of
// once at first mount.
function ProtectedRoute() {
  useLocation();
  return authStorage.isLoggedIn() ? (
    <AuthenticatedApp />
  ) : (
    <Navigate to="/login" replace />
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/*" element={<ProtectedRoute />} />
    </Routes>
  );
}