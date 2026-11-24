import React, { useState, useEffect, useCallback } from 'react';
import { api } from './services/api';
import { Header } from './components/Header';
import { LoginModal } from './components/LoginModal';
import { ChangePasswordModal } from './components/ChangePasswordModal';
import { RequestsView } from './components/RequestsView';
import { DictionaryView } from './components/DictionaryView';
import { AnalyticsView } from './components/AnalyticsView';
import { AdminUsersView } from './components/AdminUsersView';
import { AuditView } from './components/AuditView';
import { ToastContainer } from './components/Toast';

export function App() {
  const [currentUser, setCurrentUser] = useState(api.getUser());
  const [currentTab, setCurrentTab] = useState('requests');
  const [loginModalOpen, setLoginModalOpen] = useState(!api.isAuthenticated());
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [statusMessage, setStatusMessage] = useState('Готово');

  const addToast = useCallback((toast) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 6);
    setToasts((prev) => [...prev, { ...toast, id }]);
    if (toast.message) {
      setStatusMessage(toast.message);
    }
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 6000);
  }, []);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  useEffect(() => {
    api.setAuthFailureHandler(() => {
      setCurrentUser(null);
      setLoginModalOpen(true);
      addToast({ type: 'warning', title: 'Сессия истекла', message: 'Пожалуйста, выполните вход снова' });
    });

    if (api.isAuthenticated()) {
      api.getMe()
        .then((user) => {
          setCurrentUser(user);
          setStatusMessage(`Авторизован: ${user.fullName || user.username} (${user.role})`);
        })
        .catch(() => {
          setLoginModalOpen(true);
        });
    }
  }, [addToast]);

  const handleLoginSuccess = (user) => {
    setCurrentUser(user);
    setLoginModalOpen(false);
    setStatusMessage(`Выполнен вход: ${user.fullName || user.username}`);
  };

  const handleLogout = async () => {
    await api.logout();
    setCurrentUser(null);
    setLoginModalOpen(true);
    setStatusMessage('Сессия завершена');
    addToast({ type: 'success', message: 'Вы успешно вышли из системы' });
  };

  const handleExportCsv = async () => {
    try {
      setStatusMessage('Экспорт CSV...');
      const csvData = await api.exportCsv();
      const blob = new Blob([csvData], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `document_requests_${new Date().toISOString().slice(0, 10)}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      addToast({ type: 'success', message: 'CSV файл успешно выгружен' });
      setStatusMessage('CSV файл успешно выгружен');
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--fx-bg)' }}>
      {currentUser && (
        <Header
          user={currentUser}
          currentTab={currentTab}
          onSelectTab={setCurrentTab}
          onOpenChangePassword={() => setChangePasswordOpen(true)}
          onLogout={handleLogout}
          onExportCsv={handleExportCsv}
        />
      )}

      <main style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {loginModalOpen ? (
          <LoginModal onLoginSuccess={handleLoginSuccess} addToast={addToast} />
        ) : (
          <div style={{ flex: 1, padding: '16px', display: 'flex', flexDirection: 'column' }}>
            {currentTab === 'requests' && (
              <RequestsView
                user={currentUser}
                addToast={addToast}
                setStatusMessage={setStatusMessage}
              />
            )}
            {currentTab === 'dictionary' && (
              <DictionaryView
                user={currentUser}
                addToast={addToast}
                setStatusMessage={setStatusMessage}
              />
            )}
            {currentTab === 'analytics' && (
              <AnalyticsView
                addToast={addToast}
                setStatusMessage={setStatusMessage}
              />
            )}
            {currentTab === 'admin-users' && currentUser?.role === 'ADMIN' && (
              <AdminUsersView
                addToast={addToast}
                setStatusMessage={setStatusMessage}
              />
            )}
            {currentTab === 'audit' && currentUser?.role === 'ADMIN' && (
              <AuditView
                addToast={addToast}
                setStatusMessage={setStatusMessage}
              />
            )}
          </div>
        )}
      </main>

      {/* Status Bar matching JavaFX .status-bar */}
      {currentUser && (
        <footer className="status-bar">
          <div>
            <span>Статус: </span>
            <span style={{ color: 'var(--fx-text)', fontWeight: 'bold' }}>{statusMessage}</span>
          </div>
          <div>
            <span>Сервер: </span>
            <span style={{ color: 'var(--fx-success)' }}>● localhost:8080 (REST API)</span>
            <span style={{ margin: '0 8px' }}>|</span>
            <span>Пользователь: </span>
            <span style={{ color: 'var(--fx-accent)' }}>{currentUser.username} [{currentUser.role}]</span>
          </div>
        </footer>
      )}

      {changePasswordOpen && (
        <ChangePasswordModal
          onClose={() => setChangePasswordOpen(false)}
          addToast={addToast}
        />
      )}

      <ToastContainer toasts={toasts} onClose={removeToast} />
    </div>
  );
}

export default App;
