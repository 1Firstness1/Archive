import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export function AnalyticsView({ addToast, setStatusMessage }) {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    if (setStatusMessage) setStatusMessage('Загрузка аналитической сводки...');
    try {
      const data = await api.getAnalyticsSummary();
      setSummary(data);
      if (setStatusMessage) setStatusMessage('Аналитическая сводка обновлена');
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
      if (setStatusMessage) setStatusMessage('Ошибка при загрузке аналитики');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: 'var(--fx-subtext0)' }}>
        Загрузка статистических показателей...
      </div>
    );
  }

  if (!summary) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: 'var(--fx-subtext0)' }}>
        Данные статистики недоступны
      </div>
    );
  }

  const categoryEntries = summary.byCategory ? Object.entries(summary.byCategory) : [];
  const statusEntries = summary.byStatus ? Object.entries(summary.byStatus) : [];
  const metricEntries = summary.byCalculatedMetric ? Object.entries(summary.byCalculatedMetric) : [];
  const userEntries = summary.byUser ? Object.entries(summary.byUser) : [];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', flex: 1, overflowY: 'auto' }}>
      
      {/* 1. Верхняя строка: Заголовок и кнопка обновления */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span className="title-label">Статистика и аналитика системы</span>
        <button className="button btn-secondary" onClick={loadData}>
          Обновить данные
        </button>
      </div>

      {/* 2. Общие показатели matching JavaFX stat-card */}
      <div>
        <div className="section-label" style={{ marginBottom: '8px' }}>Общие показатели</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
          
          <div className="stat-card">
            <span className="stat-value">{summary.totalCount || 0}</span>
            <span className="stat-label">Всего запросов</span>
          </div>

          <div className="stat-card">
            <span className="stat-value" style={{ color: 'var(--fx-accent)' }}>{summary.activeCount || 0}</span>
            <span className="stat-label">Активных запросов</span>
          </div>

          <div className="stat-card">
            <span className="stat-value" style={{ color: 'var(--fx-warning)' }}>{summary.archivedCount || 0}</span>
            <span className="stat-label">В архиве</span>
          </div>

          <div className="stat-card">
            <span className="stat-value" style={{ color: 'var(--fx-danger)' }}>{summary.confidentialCount || 0}</span>
            <span className="stat-label">Конфиденциальных</span>
          </div>

          <div className="stat-card">
            <span className="stat-value" style={{ color: 'var(--fx-export)' }}>{summary.averagePageCount || 0}</span>
            <span className="stat-label">Средний объём (стр.)</span>
          </div>

        </div>
      </div>

      <div className="separator-h"></div>

      {/* 3. Распределение по статусам */}
      <div>
        <div className="section-label" style={{ marginBottom: '8px' }}>По статусам выполнения</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '10px' }}>
          {statusEntries.map(([st, cnt]) => (
            <div key={st} className="card" style={{ padding: '12px', textAlign: 'center' }}>
              <div style={{ fontSize: '22px', fontWeight: 'bold', color: 'var(--fx-accent)', marginBottom: '2px' }}>
                {cnt}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--fx-subtext0)' }}>{st}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="separator-h"></div>

      {/* 4. Расчетная метрика: Сроки исполнения */}
      <div>
        <div className="section-label" style={{ marginBottom: '8px' }}>
          Расчетный срок исполнения (вычисляемая метрика КР-3)
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '12px' }}>
          {metricEntries.map(([metric, cnt]) => (
            <div key={metric} className="card" style={{ padding: '14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontWeight: '600', color: 'var(--fx-text)' }}>{metric}</div>
                <div style={{ fontSize: '11px', color: 'var(--fx-subtext0)' }}>
                  {metric.includes('Экспресс') ? 'Приоритет CRITICAL/HIGH (1-2 дня)' : metric.includes('Стандарт') ? 'Стандартный объём (3-4 дня)' : 'Большой объём страниц (5+ дней)'}
                </div>
              </div>
              <div style={{ fontSize: '22px', fontWeight: 'bold', color: 'var(--fx-success)' }}>
                {cnt}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="separator-h"></div>

      {/* 5. Категории документов и заявители */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
        
        {/* Категории */}
        <div className="card" style={{ padding: '14px' }}>
          <div className="section-label" style={{ marginBottom: '10px', fontSize: '14px' }}>
            Категории документов
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {categoryEntries.length === 0 ? (
              <div style={{ color: 'var(--fx-overlay0)', fontSize: '12px' }}>Нет данных</div>
            ) : (
              categoryEntries.map(([cat, cnt]) => (
                <div key={cat} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '5px 0', borderBottom: '1px solid var(--fx-border-subtle)' }}>
                  <span style={{ color: 'var(--fx-text)', fontSize: '12px' }}>{cat}</span>
                  <span style={{ color: 'var(--fx-export)', fontWeight: 'bold', fontSize: '12px' }}>{cnt}</span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Заявители */}
        <div className="card" style={{ padding: '14px' }}>
          <div className="section-label" style={{ marginBottom: '10px', fontSize: '14px' }}>
            Топ заявителей архива
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {userEntries.length === 0 ? (
              <div style={{ color: 'var(--fx-overlay0)', fontSize: '12px' }}>Нет данных</div>
            ) : (
              userEntries.map(([usr, cnt]) => (
                <div key={usr} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '5px 0', borderBottom: '1px solid var(--fx-border-subtle)' }}>
                  <span style={{ color: 'var(--fx-text)', fontSize: '12px' }}>{usr}</span>
                  <span style={{ color: 'var(--fx-accent)', fontWeight: 'bold', fontSize: '12px' }}>{cnt} запросов</span>
                </div>
              ))
            )}
          </div>
        </div>

      </div>

    </div>
  );
}
