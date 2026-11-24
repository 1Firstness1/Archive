import React, { useState } from 'react';
import { api } from '../services/api';

export function LoginModal({ onLoginSuccess, addToast }) {
  const [tab, setTab] = useState('login'); // 'login' | 'quick'
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);

  // Принудительная смена временного пароля
  const [mustChange, setMustChange] = useState(false);
  const [tempUser, setTempUser] = useState(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const handleLogin = async (userToLogin, passToLogin) => {
    const u = userToLogin || username;
    const p = passToLogin || password;

    if (!u.trim() || !p) {
      addToast({ type: 'warning', message: 'Введите логин и пароль' });
      return;
    }

    setLoading(true);
    try {
      const data = await api.login(u.trim(), p);
      if (data.user?.mustChangePassword) {
        setMustChange(true);
        setTempUser(data.user);
        addToast({
          type: 'warning',
          title: 'Требуется сменить пароль',
          message: 'Вы используете временный пароль. Для продолжения работы задайте постоянный пароль.'
        });
      } else {
        addToast({ type: 'success', title: 'Добро пожаловать', message: `Вы успешно вошли как ${data.user?.fullName || data.user?.username}` });
        onLoginSuccess(data.user);
      }
    } catch (err) {
      addToast({
        type: 'error',
        title: 'Ошибка авторизации',
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordChangeSubmit = async (e) => {
    e.preventDefault();
    if (newPassword.length < 6) {
      addToast({ type: 'warning', message: 'Новый пароль должен содержать минимум 6 символов' });
      return;
    }
    if (newPassword !== confirmPassword) {
      addToast({ type: 'warning', message: 'Пароли не совпадают' });
      return;
    }

    setLoading(true);
    try {
      await api.changePassword(password, newPassword);
      addToast({ type: 'success', title: 'Пароль обновлён', message: 'Постоянный пароль успешно сохранён' });
      onLoginSuccess(tempUser);
    } catch (err) {
      addToast({
        type: 'error',
        title: 'Ошибка смены пароля',
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px' }}>
      <div className="card" style={{ maxWidth: '440px', width: '100%', padding: '24px' }}>
        
        {/* Card Header matching JavaFX */}
        <div style={{ textAlign: 'center', marginBottom: '18px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 'bold', color: 'var(--fx-accent)', margin: '0 0 4px' }}>
            {mustChange ? 'Смена временного пароля' : 'Вход в «Архив документов»'}
          </h2>
          <p style={{ color: 'var(--fx-subtext0)', fontSize: '12px', margin: 0 }}>
            {mustChange ? 'Задайте постоянный пароль для вашей учетной записи' : 'Авторизация через Spring Boot REST API'}
          </p>
        </div>

        {mustChange ? (
          <form onSubmit={handlePasswordChangeSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', color: 'var(--fx-subtext0)', marginBottom: '3px' }}>
                Текущий временный пароль:
              </label>
              <input
                type="password"
                className="input-field"
                value={password}
                disabled
                style={{ width: '100%', opacity: 0.7 }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', color: 'var(--fx-subtext0)', marginBottom: '3px' }}>
                Новый постоянный пароль (мин. 6 символов):
              </label>
              <input
                type="password"
                className="input-field"
                placeholder="Введите новый пароль"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoFocus
                required
                style={{ width: '100%' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', color: 'var(--fx-subtext0)', marginBottom: '3px' }}>
                Подтверждение пароля:
              </label>
              <input
                type="password"
                className="input-field"
                placeholder="Повторите новый пароль"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                style={{ width: '100%' }}
              />
            </div>

            <button type="submit" className="button btn-success" disabled={loading} style={{ width: '100%', marginTop: '6px' }}>
              {loading ? 'Сохранение...' : 'Сохранить и продолжить'}
            </button>
          </form>
        ) : (
          <div>
            {/* Tabs matching JavaFX */}
            <div style={{ display: 'flex', gap: '4px', borderBottom: '1px solid var(--fx-border)', marginBottom: '14px' }}>
              <button
                className={`fx-tab ${tab === 'login' ? 'selected' : ''}`}
                style={{ flex: 1, justifyContent: 'center' }}
                onClick={() => setTab('login')}
              >
                Вход
              </button>
              <button
                className={`fx-tab ${tab === 'quick' ? 'selected' : ''}`}
                style={{ flex: 1, justifyContent: 'center' }}
                onClick={() => setTab('quick')}
              >
                Быстрый выбор
              </button>
            </div>

            {tab === 'login' ? (
              <form onSubmit={(e) => { e.preventDefault(); handleLogin(); }} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', color: 'var(--fx-subtext0)', marginBottom: '3px' }}>
                    Логин:
                  </label>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="admin"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 'bold', color: 'var(--fx-subtext0)', marginBottom: '3px' }}>
                    Пароль:
                  </label>
                  <input
                    type="password"
                    className="input-field"
                    placeholder="admin123"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    style={{ width: '100%' }}
                  />
                </div>

                <div style={{
                  backgroundColor: 'var(--fx-surface0)',
                  padding: '8px 12px',
                  borderRadius: '5px',
                  border: '1px solid var(--fx-border-subtle)',
                  fontSize: '11px',
                  lineHeight: '1.4'
                }}>
                  <div style={{ color: 'var(--fx-yellow)', fontWeight: 'bold', marginBottom: '2px' }}>
                    Данные для старта:
                  </div>
                  <div style={{ color: 'var(--fx-text)' }}>
                    admin / admin123 | editor / editor123 | viewer / viewer123
                  </div>
                </div>

                <button
                  type="submit"
                  className="button btn-primary"
                  disabled={loading}
                  style={{ width: '100%', marginTop: '4px' }}
                >
                  {loading ? 'Вход в систему...' : 'Войти в систему'}
                </button>
              </form>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <p style={{ fontSize: '12px', color: 'var(--fx-subtext0)', margin: '0 0 4px' }}>
                  Выберите стандартную роль для быстрого входа:
                </p>

                <button
                  className="button btn-danger"
                  style={{ width: '100%', justifyContent: 'space-between' }}
                  onClick={() => {
                    setUsername('admin');
                    setPassword('admin123');
                    handleLogin('admin', 'admin123');
                  }}
                  disabled={loading}
                >
                  <span>Администратор (admin)</span>
                  <span style={{ fontSize: '11px', opacity: 0.85 }}>Полный доступ</span>
                </button>

                <button
                  className="button btn-warning"
                  style={{ width: '100%', justifyContent: 'space-between' }}
                  onClick={() => {
                    setUsername('editor');
                    setPassword('editor123');
                    handleLogin('editor', 'editor123');
                  }}
                  disabled={loading}
                >
                  <span>Редактор (editor)</span>
                  <span style={{ fontSize: '11px', opacity: 0.85 }}>Запросы, Заметки, CSV</span>
                </button>

                <button
                  className="button btn-primary"
                  style={{ width: '100%', justifyContent: 'space-between' }}
                  onClick={() => {
                    setUsername('viewer');
                    setPassword('viewer123');
                    handleLogin('viewer', 'viewer123');
                  }}
                  disabled={loading}
                >
                  <span>Читатель (viewer)</span>
                  <span style={{ fontSize: '11px', opacity: 0.85 }}>Просмотр и поиск</span>
                </button>
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
}
