import React from 'react';

export function ToastContainer({ toasts, onClose }) {
  if (!toasts || toasts.length === 0) return null;

  return (
    <div style={{ position: 'fixed', bottom: '28px', right: '16px', zIndex: 10000, display: 'flex', flexDirection: 'column', gap: '6px', maxWidth: '400px' }}>
      {toasts.map((toast) => {
        const isError = toast.type === 'error';
        const isSuccess = toast.type === 'success';

        const borderColor = isError ? 'var(--fx-danger)' : isSuccess ? 'var(--fx-success)' : 'var(--fx-warning)';
        const textColor = isError ? 'var(--fx-danger)' : isSuccess ? 'var(--fx-success)' : 'var(--fx-warning)';

        return (
          <div
            key={toast.id}
            style={{
              backgroundColor: '#24273a',
              border: `1px solid ${borderColor}`,
              color: 'var(--fx-text)',
              borderRadius: '6px',
              padding: '10px 14px',
              boxShadow: '0 6px 20px rgba(0, 0, 0, 0.5)',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '10px',
              animation: 'scaleIn 0.15s ease-out',
            }}
          >
            <div style={{ flex: 1, fontSize: '12px' }}>
              <div style={{ fontWeight: '600', color: textColor, marginBottom: '2px' }}>
                {toast.title || (isError ? 'Ошибка' : isSuccess ? 'Успешно' : 'Внимание')}
              </div>
              <div style={{ whiteSpace: 'pre-wrap', color: 'var(--fx-text)' }}>
                {toast.message}
              </div>
              {toast.correlationId && (
                <div style={{ marginTop: '4px', fontSize: '10px', color: 'var(--fx-subtext0)', fontFamily: 'monospace' }}>
                  Correlation ID: {toast.correlationId}
                </div>
              )}
              {toast.validationErrors && (
                <ul style={{ margin: '4px 0 0 14px', padding: 0, fontSize: '11px', color: 'var(--fx-danger)' }}>
                  {Object.entries(toast.validationErrors).map(([field, msg]) => (
                    <li key={field}><b>{field}</b>: {msg}</li>
                  ))}
                </ul>
              )}
            </div>
            <button
              onClick={() => onClose(toast.id)}
              style={{ background: 'none', border: 'none', color: 'var(--fx-subtext0)', cursor: 'pointer', fontSize: '14px', padding: 0, lineHeight: 1 }}
            >
              x
            </button>
          </div>
        );
      })}
    </div>
  );
}
