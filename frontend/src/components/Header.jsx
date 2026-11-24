import React from 'react';

export function Header({ user, currentTab, onSelectTab, onOpenChangePassword, onLogout, onExportCsv, onExportXlsx }) {
  const role = user?.role || 'VIEWER';
  const isAdmin = role === 'ADMIN';

  return (
    <header style={{ position: 'sticky', top: 0, zIndex: 40, boxShadow: '0 2px 8px rgba(0, 0, 0, 0.35)' }}>
      {/* Top Toolbar matching JavaFX .toolbar-panel */}
      <div className="toolbar-panel" style={{ justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span className="title-label">Архив документов</span>
          <span className="status-badge-online">
            <span style={{ display: 'inline-block', width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#11111b' }}></span>
            Подключено
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ color: 'var(--fx-text)', fontWeight: '600', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>{user?.fullName || user?.username}</span>
            <span style={{
              fontSize: '10px',
              padding: '1px 6px',
              borderRadius: '8px',
              backgroundColor: role === 'ADMIN' ? 'rgba(243,139,168,0.2)' : role === 'EDITOR' ? 'rgba(250,179,135,0.2)' : 'rgba(137,180,250,0.2)',
              color: role === 'ADMIN' ? 'var(--fx-danger)' : role === 'EDITOR' ? 'var(--fx-warning)' : 'var(--fx-accent)',
              border: `1px solid ${role === 'ADMIN' ? 'var(--fx-danger)' : role === 'EDITOR' ? 'var(--fx-warning)' : 'var(--fx-accent)'}`
            }}>
              {role}
            </span>
          </div>

          <button className="button btn-secondary btn-sm" onClick={onOpenChangePassword} title="Сменить постоянный пароль">
            Сменить пароль
          </button>

          <button className="button btn-danger btn-sm" onClick={onLogout} title="Завершить сессию">
            Выйти
          </button>

          <div className="separator-v"></div>

          <button
            className="button btn-warning btn-sm"
            onClick={() => onSelectTab('analytics')}
            title="Открыть статистику системы"
          >
            Статистика
          </button>

          {onExportCsv && (
            <button className="button btn-export btn-sm" onClick={onExportCsv} title="Экспортировать запросы в CSV">
              Экспорт CSV
            </button>
          )}

          {onExportXlsx && (
            <button className="button btn-export btn-sm" onClick={onExportXlsx} title="Экспортировать запросы в Excel">
              Экспорт XLSX
            </button>
          )}
        </div>
      </div>

      {/* TabPane Header matching JavaFX TabPane */}
      <div className="tab-header-area">
        <button
          className={`fx-tab ${currentTab === 'requests' ? 'selected' : ''}`}
          onClick={() => onSelectTab('requests')}
        >
          Запросы на документы
        </button>

        <button
          className={`fx-tab ${currentTab === 'dictionary' ? 'selected' : ''}`}
          onClick={() => onSelectTab('dictionary')}
        >
          Пользователи
        </button>

        {isAdmin && (
          <button
            className={`fx-tab ${currentTab === 'admin-users' ? 'selected' : ''}`}
            onClick={() => onSelectTab('admin-users')}
          >
            Работники
          </button>
        )}

        {isAdmin && (
          <button
            className={`fx-tab ${currentTab === 'audit' ? 'selected' : ''}`}
            onClick={() => onSelectTab('audit')}
          >
            Аудит логов
          </button>
        )}

        <button
          className={`fx-tab ${currentTab === 'analytics' ? 'selected' : ''}`}
          onClick={() => onSelectTab('analytics')}
        >
          Аналитика
        </button>
      </div>
    </header>
  );
}
