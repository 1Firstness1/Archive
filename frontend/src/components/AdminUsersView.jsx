import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';

export function AdminUsersView({ addToast, setStatusMessage }) {
  const [users, setUsers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  // Модальные окна
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [tempPasswordModal, setTempPasswordModal] = useState(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    if (setStatusMessage) setStatusMessage('Загрузка списка работников...');
    try {
      const data = await api.getAdminUsers(page, size);
      setUsers(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
      if (setStatusMessage) {
        setStatusMessage(`Загружено учетных записей работников: ${data.totalElements || 0}`);
      }
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
      if (setStatusMessage) setStatusMessage('Ошибка при загрузке данных');
    } finally {
      setLoading(false);
    }
  }, [page, size, addToast, setStatusMessage]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleResetPassword = async (id, username) => {
    if (!window.confirm(`Сбросить пароль для пользователя ${username}? Будет сгенерирован новый временный пароль.`)) {
      return;
    }
    try {
      const res = await api.resetAdminUserPassword(id);
      setTempPasswordModal(res);
      addToast({ type: 'success', message: `Пароль пользователя ${username} сброшен` });
      loadUsers();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const handleToggleActive = async (id) => {
    try {
      await api.toggleAdminUserActive(id);
      addToast({ type: 'success', message: 'Статус активности учетной записи изменен' });
      loadUsers();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const filteredUsers = users.filter((u) => {
    if (!search.trim()) return true;
    const s = search.toLowerCase();
    return (
      u.username.toLowerCase().includes(s) ||
      (u.fullName && u.fullName.toLowerCase().includes(s)) ||
      (u.email && u.email.toLowerCase().includes(s))
    );
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
      
      {/* 1. Действия */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <button className="button btn-success" onClick={() => setCreateModalOpen(true)}>
          + Добавить работника
        </button>
        <div style={{ flex: 1 }}></div>
        <button className="button btn-secondary" onClick={loadUsers} disabled={loading}>
          Обновить список
        </button>
      </div>

      {/* 2. Filter Panel matching JavaFX .filter-panel */}
      <div className="filter-panel">
        <span style={{ fontWeight: '600', color: 'var(--fx-accent)', fontSize: '12px' }}>Поиск работника:</span>
        <input
          type="text"
          className="text-field"
          placeholder="Логин, ФИО или email..."
          style={{ width: '280px' }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button className="button btn-secondary btn-sm" onClick={() => {}}>
          Найти
        </button>
        <button className="button btn-secondary btn-sm" onClick={() => setSearch('')}>
          Сбросить
        </button>
      </div>

      {/* 3. TableView matching JavaFX workerTable */}
      <div className="table-view-container" style={{ flex: 1, minHeight: '340px' }}>
        <table className="table-view">
          <thead>
            <tr>
              <th style={{ width: '50px', whiteSpace: 'nowrap' }}>ID</th>
              <th style={{ width: '150px', whiteSpace: 'nowrap' }}>Работник (Логин)</th>
              <th style={{ minWidth: '180px' }}>ФИО</th>
              <th style={{ width: '180px', whiteSpace: 'nowrap' }}>Email</th>
              <th style={{ width: '110px', whiteSpace: 'nowrap', textAlign: 'center' }}>Роль</th>
              <th style={{ width: '120px', whiteSpace: 'nowrap', textAlign: 'center' }}>Статус</th>
              <th style={{ width: '130px', whiteSpace: 'nowrap' }}>Дата регистрации</th>
              <th style={{ width: '180px', textAlign: 'right', whiteSpace: 'nowrap' }}>Действия</th>
            </tr>
          </thead>
          <tbody>
            {filteredUsers.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--fx-overlay0)' }}>
                  {loading ? 'Загрузка...' : 'Работники не найдены'}
                </td>
              </tr>
            ) : (
              filteredUsers.map((u) => (
                <tr key={u.id}>
                  <td style={{ color: 'var(--fx-accent)', fontWeight: '600', fontFamily: 'monospace' }}>{u.id}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <span style={{ fontWeight: '600', color: 'var(--fx-text)' }}>{u.username}</span>
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>{u.fullName || '-'}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{u.email || '-'}</td>
                  <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                    <span style={{
                      padding: '2px 8px',
                      borderRadius: '10px',
                      fontSize: '11px',
                      fontWeight: '600',
                      backgroundColor: u.role === 'ADMIN' ? 'rgba(243,139,168,0.2)' : u.role === 'EDITOR' ? 'rgba(250,179,135,0.2)' : 'rgba(137,180,250,0.2)',
                      color: u.role === 'ADMIN' ? 'var(--fx-danger)' : u.role === 'EDITOR' ? 'var(--fx-warning)' : 'var(--fx-accent)',
                      border: `1px solid ${u.role === 'ADMIN' ? 'var(--fx-danger)' : u.role === 'EDITOR' ? 'var(--fx-warning)' : 'var(--fx-accent)'}`
                    }}>
                      {u.role}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                    {!u.active ? (
                      <span className="badge badge-rejected">Отключен</span>
                    ) : u.mustChangePassword ? (
                      <span className="badge badge-progress">Смена пароля</span>
                    ) : (
                      <span className="badge badge-approved">Активен</span>
                    )}
                  </td>
                  <td style={{ color: 'var(--fx-subtext0)', whiteSpace: 'nowrap' }}>
                    {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}
                  </td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    <button
                      className="button btn-warning btn-sm"
                      onClick={() => handleResetPassword(u.id, u.username)}
                      title="Сбросить пароль и выдать временный"
                      style={{ marginRight: '4px' }}
                    >
                      Пароль
                    </button>
                    <button
                      className="button btn-primary btn-sm"
                      onClick={() => setEditingUser(u)}
                      title="Редактировать"
                      style={{ marginRight: '4px' }}
                    >
                      Ред.
                    </button>
                    <button
                      className={`button ${u.active ? 'btn-danger' : 'btn-success'} btn-sm`}
                      onClick={() => handleToggleActive(u.id)}
                      title={u.active ? 'Отключить учетную запись' : 'Включить'}
                    >
                      {u.active ? 'Блок' : 'Вкл'}
                    </button>
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
          Всего работников: <b>{totalElements}</b>
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

      {/* Модальное окно: Создание работника */}
      {createModalOpen && (
        <WorkerFormModal
          title="Регистрация нового работника архива"
          onClose={() => setCreateModalOpen(false)}
          onSubmit={async (payload) => {
            try {
              const res = await api.createAdminUser(payload);
              setCreateModalOpen(false);
              setTempPasswordModal(res);
              addToast({ type: 'success', message: 'Работник успешно зарегистрирован' });
              loadUsers();
            } catch (err) {
              addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
            }
          }}
        />
      )}

      {/* Модальное окно: Редактирование работника */}
      {editingUser && (
        <WorkerFormModal
          title={`Редактирование: ${editingUser.username}`}
          initialData={editingUser}
          onClose={() => setEditingUser(null)}
          onSubmit={async (payload) => {
            try {
              await api.updateAdminUser(editingUser.id, payload);
              setEditingUser(null);
              addToast({ type: 'success', message: 'Данные работника обновлены' });
              loadUsers();
            } catch (err) {
              addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
            }
          }}
        />
      )}

      {/* Модальное окно: Выдача временного пароля */}
      {tempPasswordModal && (
        <div className="modal-overlay" onClick={() => setTempPasswordModal(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px' }}>
            <div className="modal-header">
              <h3 style={{ color: 'var(--fx-warning)' }}>Временный пароль для работника</h3>
              <button className="close-btn" onClick={() => setTempPasswordModal(null)}>x</button>
            </div>
            <div className="modal-body" style={{ gap: '12px' }}>
              <p style={{ margin: 0, fontSize: '13px' }}>
                Пользователь <b style={{ color: 'var(--fx-accent)' }}>{tempPasswordModal.username}</b> должен использовать этот пароль для первого входа, после чего система обяжет его задать постоянный пароль:
              </p>

              <div style={{
                backgroundColor: 'var(--fx-surface0)',
                border: '1px solid var(--fx-warning)',
                borderRadius: '6px',
                padding: '12px',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '11px', color: 'var(--fx-subtext0)', marginBottom: '4px' }}>ВРЕМЕННЫЙ ПАРОЛЬ:</div>
                <div style={{ fontSize: '18px', fontWeight: 'bold', letterSpacing: '1px', color: 'var(--fx-yellow)', fontFamily: 'monospace' }}>
                  {tempPasswordModal.temporaryPassword}
                </div>
              </div>

              <button
                className="button btn-primary"
                onClick={() => {
                  navigator.clipboard.writeText(tempPasswordModal.temporaryPassword);
                  addToast({ type: 'success', message: 'Пароль скопирован в буфер обмена' });
                }}
              >
                Скопировать пароль
              </button>
            </div>
            <div className="modal-footer">
              <button className="button btn-secondary" onClick={() => setTempPasswordModal(null)}>
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

function WorkerFormModal({ title, initialData, onClose, onSubmit }) {
  const [username, setUsername] = useState(initialData?.username || '');
  const [fullName, setFullName] = useState(initialData?.fullName || '');
  const [email, setEmail] = useState(initialData?.email || '');
  const [role, setRole] = useState(initialData?.role || 'VIEWER');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!username.trim()) return;

    onSubmit({
      username: username.trim(),
      fullName: fullName.trim() || null,
      email: email.trim() || null,
      role,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '440px' }}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="close-btn" onClick={onClose}>x</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ gap: '10px' }}>
            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Логин работника:
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="ivanov"
                style={{ width: '100%' }}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={Boolean(initialData)}
                required
                autoFocus
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                ФИО:
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="Иванов Иван Иванович"
                style={{ width: '100%' }}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Email:
              </label>
              <input
                type="email"
                className="text-field"
                placeholder="ivanov@mirea.ru"
                style={{ width: '100%' }}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Роль доступа:
              </label>
              <select
                className="text-field"
                style={{ width: '100%' }}
                value={role}
                onChange={(e) => setRole(e.target.value)}
              >
                <option value="VIEWER">VIEWER (Только просмотр и поиск)</option>
                <option value="EDITOR">EDITOR (Редактор запросов, заметок и CSV)</option>
                <option value="ADMIN">ADMIN (Полный административный доступ)</option>
              </select>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="button btn-secondary" onClick={onClose}>
              Отмена
            </button>
            <button type="submit" className="button btn-success">
              Сохранить
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
