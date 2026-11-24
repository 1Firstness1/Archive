import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';

export function AuditView({ addToast, setStatusMessage }) {
  const [logs, setLogs] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState('');
  const [eventFilter, setEventFilter] = useState('');
  const [loading, setLoading] = useState(false);

  const loadAuditLogs = useCallback(async () => {
    setLoading(true);
    if (setStatusMessage) setStatusMessage('Загрузка журнала аудита...');
    try {
      const data = await api.getAuditLogs({
        page,
        size,
        search: search.trim() || undefined,
        event: eventFilter || undefined,
        sortBy: 'createdAt',
        sortDir: 'desc',
      });
      setLogs(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
      if (setStatusMessage) {
        setStatusMessage(`Загружено записей аудита: ${data.totalElements || 0}`);
      }
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
      if (setStatusMessage) setStatusMessage('Ошибка при загрузке данных');
    } finally {
      setLoading(false);
    }
  }, [page, size, search, eventFilter, addToast, setStatusMessage]);

  useEffect(() => {
    loadAuditLogs();
  }, [loadAuditLogs]);

  const renderEventBadge = (event) => {
    let colorClass = 'badge-new';
    if (event.includes('FAILED') || event.includes('BLOCKED') || event.includes('DELETE')) {
      colorClass = 'badge-rejected';
    } else if (event.includes('SUCCESS') || event.includes('CREATE') || event.includes('RESTORE')) {
      colorClass = 'badge-approved';
    } else if (event.includes('UPDATE') || event.includes('CHANGE') || event.includes('RESET')) {
      colorClass = 'badge-progress';
    }

    return <span className={`badge ${colorClass}`}>{event}</span>;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
      
      {/* 1. Действия */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
        <button className="button btn-secondary" onClick={loadAuditLogs} disabled={loading}>
          Обновить журнал
        </button>
      </div>

      {/* 2. Filter Panel matching JavaFX .filter-panel */}
      <div className="filter-panel">
        <span style={{ fontWeight: '600', color: 'var(--fx-accent)', fontSize: '12px' }}>Поиск в журнале:</span>
        <input
          type="text"
          className="text-field"
          placeholder="Работник, событие, ID или детали..."
          style={{ width: '280px' }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') { setPage(0); loadAuditLogs(); } }}
        />
        <button className="button btn-secondary btn-sm" onClick={() => { setPage(0); loadAuditLogs(); }}>
          Найти
        </button>

        <div className="separator-v"></div>

        <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Событие:</span>
        <select
          className="text-field"
          style={{ width: '180px' }}
          value={eventFilter}
          onChange={(e) => { setEventFilter(e.target.value); setPage(0); }}
        >
          <option value="">(Все события)</option>
          <option value="LOGIN_SUCCESS">LOGIN_SUCCESS</option>
          <option value="LOGIN_FAILED">LOGIN_FAILED</option>
          <option value="LOGIN_BLOCKED">LOGIN_BLOCKED</option>
          <option value="LOGOUT">LOGOUT</option>
          <option value="PASSWORD_CHANGED">PASSWORD_CHANGED</option>
          <option value="PASSWORD_RESET">PASSWORD_RESET</option>
          <option value="CREATE_REQUEST">CREATE_REQUEST</option>
          <option value="UPDATE_REQUEST">UPDATE_REQUEST</option>
          <option value="ARCHIVE_REQUEST">ARCHIVE_REQUEST</option>
          <option value="RESTORE_REQUEST">RESTORE_REQUEST</option>
          <option value="PERMANENT_DELETE_REQUEST">PERMANENT_DELETE_REQUEST</option>
          <option value="BULK_OPERATION">BULK_OPERATION</option>
          <option value="CSV_IMPORT">CSV_IMPORT</option>
        </select>

        <div style={{ flex: 1 }}></div>

        <button
          className="button btn-secondary btn-sm"
          onClick={() => {
            setSearch('');
            setEventFilter('');
            setPage(0);
          }}
        >
          Сбросить
        </button>
      </div>

      {/* 3. TableView matching JavaFX auditTable */}
      <div className="table-view-container" style={{ flex: 1, minHeight: '340px' }}>
        <table className="table-view">
          <thead>
            <tr>
              <th style={{ width: '50px', whiteSpace: 'nowrap' }}>ID</th>
              <th style={{ width: '150px', whiteSpace: 'nowrap' }}>Дата и время</th>
              <th style={{ width: '140px', whiteSpace: 'nowrap' }}>Работник</th>
              <th style={{ width: '150px', whiteSpace: 'nowrap' }}>Событие</th>
              <th style={{ width: '120px', whiteSpace: 'nowrap' }}>Сущность</th>
              <th style={{ width: '140px', whiteSpace: 'nowrap' }}>Correlation ID</th>
              <th>Детали операции</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--fx-overlay0)' }}>
                  {loading ? 'Загрузка журнала...' : 'Записи аудита отсутствуют'}
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id}>
                  <td style={{ color: 'var(--fx-subtext0)', fontFamily: 'monospace' }}>{log.id}</td>
                  <td style={{ color: 'var(--fx-text)', whiteSpace: 'nowrap' }}>
                    {log.createdAt ? new Date(log.createdAt).toLocaleString() : '-'}
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <span style={{ fontWeight: '600', color: 'var(--fx-accent)' }}>{log.username}</span>
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>{renderEventBadge(log.event)}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <span style={{ color: 'var(--fx-export)' }}>
                      {log.entityType ? `${log.entityType} ${log.entityId ? `#${log.entityId}` : ''}` : '-'}
                    </span>
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    {log.correlationId ? (
                      <span
                        className="badge-tag"
                        style={{ cursor: 'pointer', fontFamily: 'monospace', fontSize: '10px' }}
                        title="Нажмите, чтобы скопировать Correlation ID"
                        onClick={() => {
                          navigator.clipboard.writeText(log.correlationId);
                          addToast({ type: 'success', message: `Correlation ID скопирован: ${log.correlationId}` });
                        }}
                      >
                        {log.correlationId.slice(0, 8)}... [Копия]
                      </span>
                    ) : (
                      <span style={{ color: 'var(--fx-overlay0)' }}>-</span>
                    )}
                  </td>
                  <td style={{ color: 'var(--fx-text)' }}>
                    {log.details || '-'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Пагинация */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 6px' }}>
        <div style={{ color: 'var(--fx-subtext0)', fontSize: '12px' }}>
          Всего записей журнала: <b>{totalElements}</b>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            className="button btn-secondary btn-sm"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Назад
          </button>
          <span style={{ color: 'var(--fx-text)', fontSize: '12px', fontWeight: '600' }}>
            Страница {page + 1} из {totalPages || 1}
          </span>
          <button
            className="button btn-secondary btn-sm"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage(page + 1)}
          >
            Вперёд
          </button>
        </div>
      </div>

    </div>
  );
}
