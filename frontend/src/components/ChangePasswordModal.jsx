import React, { useState } from 'react';
import { api } from '../services/api';

export function ChangePasswordModal({ onClose, addToast }) {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!oldPassword) {
      addToast({ type: 'warning', message: 'Введите текущий пароль' });
      return;
    }
    if (newPassword.length < 6) {
      addToast({ type: 'warning', message: 'Новый пароль должен содержать минимум 6 символов' });
      return;
    }
    if (newPassword !== confirmPassword) {
      addToast({ type: 'warning', message: 'Новый пароль и подтверждение не совпадают' });
      return;
    }

    setLoading(true);
    try {
      await api.changePassword(oldPassword, newPassword);
      addToast({ type: 'success', title: 'Пароль изменён', message: 'Ваш постоянный пароль успешно обновлен' });
      onClose();
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
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '420px' }}>
        <div className="modal-header">
          <h3>Смена постоянного пароля</h3>
          <button className="close-btn" onClick={onClose}>x</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ gap: '10px' }}>
            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Текущий пароль:
              </label>
              <input
                type="password"
                className="text-field"
                placeholder="Введите текущий пароль"
                style={{ width: '100%' }}
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                required
                autoFocus
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Новый пароль (мин. 6 символов):
              </label>
              <input
                type="password"
                className="text-field"
                placeholder="Введите новый пароль"
                style={{ width: '100%' }}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Подтверждение нового пароля:
              </label>
              <input
                type="password"
                className="text-field"
                placeholder="Повторите новый пароль"
                style={{ width: '100%' }}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="button btn-secondary" onClick={onClose}>
              Отмена
            </button>
            <button type="submit" className="button btn-success" disabled={loading}>
              {loading ? 'Сохранение...' : 'Обновить пароль'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
