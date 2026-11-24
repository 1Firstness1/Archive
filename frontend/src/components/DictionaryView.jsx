import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';

export function DictionaryView({ user, addToast, setStatusMessage }) {
  const isAdmin = user?.role === 'ADMIN';

  const [applicants, setApplicants] = useState([]);
  const [includeInactive, setIncludeInactive] = useState(true);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  // Модальные окна
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);

  const loadApplicants = useCallback(async () => {
    setLoading(true);
    if (setStatusMessage) setStatusMessage('Загрузка справочника заявителей...');
    try {
      const data = await api.getArchiveUsers(includeInactive);
      setApplicants(data || []);
      if (setStatusMessage) {
        setStatusMessage(`Загружено заявителей: ${data?.length || 0}`);
      }
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
      if (setStatusMessage) setStatusMessage('Ошибка при загрузке данных');
    } finally {
      setLoading(false);
    }
  }, [includeInactive, addToast, setStatusMessage]);

  useEffect(() => {
    loadApplicants();
  }, [loadApplicants]);

  const handleToggleActive = async (id) => {
    try {
      await api.toggleArchiveUserActive(id);
      addToast({ type: 'success', message: 'Статус активности заявителя изменён' });
      loadApplicants();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const filtered = applicants.filter((a) => {
    if (!search.trim()) return true;
    const s = search.toLowerCase();
    return (
      a.fullName.toLowerCase().includes(s) ||
      a.email.toLowerCase().includes(s) ||
      (a.department && a.department.toLowerCase().includes(s)) ||
      (a.phone && a.phone.includes(s))
    );
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
      
      {/* 1. Действия matching JavaFX */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        {isAdmin && (
          <button className="button btn-success" onClick={() => setCreateModalOpen(true)}>
            + Добавить
          </button>
        )}
        <div style={{ flex: 1 }}></div>
        <button className="button btn-secondary" onClick={loadApplicants} disabled={loading}>
          Обновить
        </button>
      </div>

      {/* 2. Filter Panel matching JavaFX .filter-panel */}
      <div className="filter-panel">
        <span style={{ fontWeight: '600', color: 'var(--fx-accent)', fontSize: '12px' }}>Поиск по ФИО:</span>
        <input
          type="text"
          className="text-field"
          placeholder="Введите имя, отдел или email..."
          style={{ width: '280px' }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button className="button btn-secondary btn-sm" onClick={() => {}}>
          Найти
        </button>

        <div className="separator-v"></div>

        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={includeInactive}
            onChange={(e) => setIncludeInactive(e.target.checked)}
          />
          <span>Показывать отключённые</span>
        </label>

        <div style={{ flex: 1 }}></div>

        <button className="button btn-secondary btn-sm" onClick={() => setSearch('')}>
          Сбросить
        </button>
      </div>

      {/* 3. TableView matching JavaFX userTable */}
      <div className="table-view-container" style={{ flex: 1, minHeight: '340px' }}>
        <table className="table-view">
          <thead>
            <tr>
              <th style={{ width: '50px', whiteSpace: 'nowrap' }}>ID</th>
              <th style={{ minWidth: '200px' }}>ФИО</th>
              <th style={{ width: '180px', whiteSpace: 'nowrap' }}>Email</th>
              <th style={{ width: '140px', whiteSpace: 'nowrap' }}>Телефон</th>
              <th style={{ width: '160px', whiteSpace: 'nowrap' }}>Отдел</th>
              <th style={{ width: '100px', whiteSpace: 'nowrap', textAlign: 'center' }}>Статус</th>
              <th style={{ width: '140px', whiteSpace: 'nowrap' }}>Дата регистрации</th>
              {isAdmin && <th style={{ width: '130px', textAlign: 'right', whiteSpace: 'nowrap' }}>Действия</th>}
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={isAdmin ? 8 : 7} style={{ textAlign: 'center', padding: '36px', color: 'var(--fx-overlay0)' }}>
                  {loading ? 'Загрузка данных...' : 'Пользователи не найдены'}
                </td>
              </tr>
            ) : (
              filtered.map((a) => (
                <tr key={a.id}>
                  <td style={{ color: 'var(--fx-accent)', fontWeight: '600', fontFamily: 'monospace' }}>{a.id}</td>
                  <td style={{ fontWeight: '600', color: 'var(--fx-text)', whiteSpace: 'nowrap' }}>{a.fullName}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{a.email}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{a.phone || '-'}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>{a.department || '-'}</td>
                  <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                    {a.active ? (
                      <span className="badge badge-approved">Активен</span>
                    ) : (
                      <span className="badge badge-rejected">Отключен</span>
                    )}
                  </td>
                  <td style={{ color: 'var(--fx-subtext0)', whiteSpace: 'nowrap' }}>
                    {a.createdAt ? new Date(a.createdAt).toLocaleDateString() : '-'}
                  </td>
                  {isAdmin && (
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button
                        className="button btn-primary btn-sm"
                        onClick={() => setEditingUser(a)}
                        style={{ marginRight: '4px' }}
                        title="Редактировать"
                      >
                        Ред.
                      </button>
                      <button
                        className={`button ${a.active ? 'btn-danger' : 'btn-success'} btn-sm`}
                        onClick={() => handleToggleActive(a.id)}
                        title={a.active ? 'Отключить' : 'Активировать'}
                      >
                        {a.active ? 'Блок' : 'Вкл'}
                      </button>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Модальное окно создания заявителя (matching user_dialog.fxml) */}
      {createModalOpen && (
        <UserDialogModal
          title="Новый пользователь (заявитель) архива"
          onClose={() => setCreateModalOpen(false)}
          onSubmit={async (payload) => {
            try {
              await api.createArchiveUser(payload);
              addToast({ type: 'success', message: 'Заявитель успешно добавлен' });
              setCreateModalOpen(false);
              loadApplicants();
            } catch (err) {
              addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
            }
          }}
        />
      )}

      {/* Модальное окно редактирования заявителя */}
      {editingUser && (
        <UserDialogModal
          title={`Редактирование: ${editingUser.fullName}`}
          initialData={editingUser}
          onClose={() => setEditingUser(null)}
          onSubmit={async (payload) => {
            try {
              await api.updateArchiveUser(editingUser.id, payload);
              addToast({ type: 'success', message: 'Данные заявителя успешно сохранены' });
              setEditingUser(null);
              loadApplicants();
            } catch (err) {
              addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
            }
          }}
        />
      )}

    </div>
  );
}

/**
 * Модальное окно добавления / редактирования пользователя
 * Повторяет JavaFX user_dialog.fxml
 */
function UserDialogModal({ title, initialData, onClose, onSubmit }) {
  const [fullName, setFullName] = useState(initialData?.fullName || '');
  const [email, setEmail] = useState(initialData?.email || '');
  const [phone, setPhone] = useState(initialData?.phone || '');
  const [department, setDepartment] = useState(initialData?.department || '');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!fullName.trim() || !email.trim()) return;

    onSubmit({
      fullName: fullName.trim(),
      email: email.trim(),
      phone: phone.trim() || null,
      department: department.trim() || null,
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
                ФИО:
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="Иванов Иван Иванович"
                style={{ width: '100%' }}
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                autoFocus
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Email:
              </label>
              <input
                type="email"
                className="text-field"
                placeholder="user@mirea.ru"
                style={{ width: '100%' }}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Телефон:
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="+7 (999) 123-45-67 (необязательно)"
                style={{ width: '100%' }}
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Отдел:
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="Название отдела (необязательно)"
                style={{ width: '100%' }}
                value={department}
                onChange={(e) => setDepartment(e.target.value)}
              />
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
