import React, { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';

export function RequestsView({ user, addToast, setStatusMessage }) {
  const role = user?.role || 'VIEWER';
  const isViewer = role === 'VIEWER';
  const isAdmin = role === 'ADMIN';

  // Состояние списка и пагинации
  const [requests, setRequests] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(15);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [loading, setLoading] = useState(false);

  // Справочник заявителей для фильтра и форм
  const [applicants, setApplicants] = useState([]);

  // Фильтры
  const [search, setSearch] = useState('');
  const [userId, setUserId] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [priority, setPriority] = useState('');
  const [confidential, setConfidential] = useState('');
  const [archived, setArchived] = useState('false'); // только активные по умолчанию
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  // Выбранные строки
  const [selectedUuids, setSelectedUuids] = useState([]);

  // Модальные окна
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editDoc, setEditDoc] = useState(null);
  const [detailDoc, setDetailDoc] = useState(null);
  const [notesModalDoc, setNotesModalDoc] = useState(null);
  const [notes, setNotes] = useState([]);
  const [newNoteContent, setNewNoteContent] = useState('');
  const [newNotePinned, setNewNotePinned] = useState(false);

  // Массовые операции
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkAction, setBulkAction] = useState('CHANGE_STATUS');
  const [bulkStatus, setBulkStatus] = useState('IN_PROGRESS');
  const [bulkTag, setBulkTag] = useState('');

  // Двухэтапный импорт CSV
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importStep, setImportStep] = useState(1);
  const [importFile, setImportFile] = useState(null);
  const [importText, setImportText] = useState('');
  const [importPreview, setImportPreview] = useState(null);
  const [importMode, setImportMode] = useState('CREATE_ONLY');
  const [importLoading, setImportLoading] = useState(false);

  // Удаление
  const [deleteConfirmDoc, setDeleteConfirmDoc] = useState(null);

  // Загрузка справочника заявителей
  useEffect(() => {
    api.getArchiveUsers(true)
      .then(setApplicants)
      .catch((err) => console.error('Ошибка загрузки заявителей:', err));
  }, []);

  // Загрузка списка документов
  const loadRequests = useCallback(async () => {
    setLoading(true);
    if (setStatusMessage) setStatusMessage('Загрузка списка запросов...');
    try {
      const params = {
        page,
        size,
        sortBy,
        sortDir,
        search: search.trim() || undefined,
        userId: userId || undefined,
        category: category.trim() || undefined,
        status: status || undefined,
        priority: priority || undefined,
        confidential: confidential === '' ? undefined : confidential === 'true',
        archived: archived === 'all' ? undefined : archived === 'true',
      };

      const data = await api.getDocumentRequests(params);
      setRequests(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
      setSelectedUuids([]);
      if (setStatusMessage) {
        setStatusMessage(`Загружено запросов: ${data.totalElements || 0} (страница ${page + 1} из ${data.totalPages || 1})`);
      }
    } catch (err) {
      addToast({
        type: 'error',
        title: 'Ошибка загрузки записей',
        message: err.message,
        correlationId: err.correlationId,
      });
      if (setStatusMessage) setStatusMessage('Ошибка при загрузке данных');
    } finally {
      setLoading(false);
    }
  }, [page, size, sortBy, sortDir, search, userId, category, status, priority, confidential, archived, addToast, setStatusMessage]);

  useEffect(() => {
    loadRequests();
  }, [loadRequests]);

  // Выбор всех строк
  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedUuids(requests.map((r) => r.uuid));
    } else {
      setSelectedUuids([]);
    }
  };

  const handleToggleSelect = (uuid) => {
    setSelectedUuids((prev) =>
      prev.includes(uuid) ? prev.filter((id) => id !== uuid) : [...prev, uuid]
    );
  };

  // Архивирование / Восстановление
  const handleArchive = async (uuid) => {
    try {
      await api.archiveDocumentRequest(uuid);
      addToast({ type: 'success', message: 'Запрос перемещён в архив' });
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const handleRestore = async (uuid) => {
    try {
      await api.restoreDocumentRequest(uuid);
      addToast({ type: 'success', message: 'Запрос восстановлен из архива' });
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  // Окончательное удаление
  const handlePermanentDelete = async (uuid) => {
    try {
      await api.permanentDeleteDocumentRequest(uuid);
      addToast({ type: 'success', message: 'Запрос окончательно удален из базы данных' });
      setDeleteConfirmDoc(null);
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  // Запуск массовой операции
  const executeBulk = async () => {
    if (selectedUuids.length === 0) return;
    try {
      const res = await api.bulkOperation(
        selectedUuids,
        bulkAction,
        bulkAction === 'CHANGE_STATUS' ? bulkStatus : undefined,
        bulkAction === 'ADD_TAG' || bulkAction === 'REMOVE_TAG' ? bulkTag.trim() : undefined
      );
      addToast({
        type: 'success',
        title: 'Массовая операция выполнена',
        message: `Успешно: ${res.successCount}, Ошибок: ${res.failureCount}`
      });
      setBulkModalOpen(false);
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  // Загрузка заметок
  const openNotesModal = async (doc) => {
    setNotesModalDoc(doc);
    try {
      const data = await api.getNotes(doc.uuid);
      setNotes(data || []);
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const handleAddNote = async (e) => {
    e.preventDefault();
    if (!newNoteContent.trim()) return;
    try {
      await api.createNote(notesModalDoc.uuid, newNoteContent.trim(), newNotePinned);
      setNewNoteContent('');
      setNewNotePinned(false);
      const updatedNotes = await api.getNotes(notesModalDoc.uuid);
      setNotes(updatedNotes || []);
      addToast({ type: 'success', message: 'Заметка добавлена' });
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const handleDeleteNote = async (noteId) => {
    try {
      await api.deleteNote(notesModalDoc.uuid, noteId);
      const updatedNotes = await api.getNotes(notesModalDoc.uuid);
      setNotes(updatedNotes || []);
      addToast({ type: 'success', message: 'Заметка удалена' });
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  const handleTogglePinNote = async (noteId) => {
    try {
      await api.togglePinNote(notesModalDoc.uuid, noteId);
      const updatedNotes = await api.getNotes(notesModalDoc.uuid);
      setNotes(updatedNotes || []);
    } catch (err) {
      addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
    }
  };

  // CSV Импорт: Этап 1 (Preview)
  const handlePreviewImport = async (e) => {
    e.preventDefault();
    setImportLoading(true);
    try {
      let preview;
      if (importFile) {
        const formData = new FormData();
        formData.append('file', importFile);
        preview = await api.previewImportCsv(formData);
      } else if (importText.trim()) {
        preview = await api.previewImportCsvText(importText.trim());
      } else {
        addToast({ type: 'warning', message: 'Выберите файл или вставьте текст CSV' });
        setImportLoading(false);
        return;
      }
      setImportPreview(preview);
      setImportStep(2);
    } catch (err) {
      addToast({ type: 'error', title: 'Ошибка анализа CSV', message: err.message, correlationId: err.correlationId });
    } finally {
      setImportLoading(false);
    }
  };

  // CSV Импорт: Этап 2 (Apply)
  const handleApplyImport = async () => {
    if (!importPreview?.previewData?.length) return;
    setImportLoading(true);
    try {
      const res = await api.applyImportCsv(importMode, importPreview.previewData);
      addToast({
        type: 'success',
        title: 'Импорт завершён',
        message: `Создано: ${res.createdCount}, Обновлено: ${res.updatedCount}, Пропущено: ${res.skippedCount}`
      });
      setImportModalOpen(false);
      loadRequests();
    } catch (err) {
      addToast({ type: 'error', title: 'Ошибка импорта', message: err.message, correlationId: err.correlationId });
    } finally {
      setImportLoading(false);
    }
  };

  // Бейджи статуса (строго без смайликов и без переноса)
  const renderStatusBadge = (st) => {
    switch (st) {
      case 'NEW':
        return <span className="badge badge-new">Новый</span>;
      case 'IN_PROGRESS':
        return <span className="badge badge-progress">В обработке</span>;
      case 'APPROVED':
        return <span className="badge badge-approved">Одобрен</span>;
      case 'COMPLETED':
        return <span className="badge badge-completed">Завершён</span>;
      case 'REJECTED':
        return <span className="badge badge-rejected">Отклонён</span>;
      case 'CANCELLED':
        return <span className="badge badge-cancelled">Отменён</span>;
      default:
        return <span className="badge">{st}</span>;
    }
  };

  // Бейджи приоритета (строго без смайликов и без переноса)
  const renderPriorityBadge = (pr) => {
    switch (pr) {
      case 'CRITICAL':
        return <span className="badge badge-critical">Критический</span>;
      case 'HIGH':
        return <span className="badge badge-high">Высокий</span>;
      case 'MEDIUM':
        return <span className="badge badge-medium">Обычный</span>;
      case 'LOW':
        return <span className="badge badge-low">Низкий</span>;
      default:
        return <span className="badge">{pr}</span>;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', flex: 1 }}>
      
      {/* 1. Верхняя панель действий matching JavaFX action buttons */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
        {!isViewer && (
          <button
            className="button btn-success"
            onClick={() => setCreateModalOpen(true)}
            title="Создать новый запрос"
          >
            + Добавить
          </button>
        )}

        {!isViewer && (
          <button
            className="button btn-primary"
            disabled={selectedUuids.length !== 1}
            onClick={() => {
              const doc = requests.find((r) => r.uuid === selectedUuids[0]);
              if (doc) setEditDoc(doc);
            }}
            title="Редактировать выбранную запись"
          >
            Редактировать
          </button>
        )}

        {!isViewer && (
          <button
            className="button btn-danger"
            disabled={selectedUuids.length === 0}
            onClick={() => {
              if (selectedUuids.length === 1) {
                const doc = requests.find((r) => r.uuid === selectedUuids[0]);
                if (doc) setDeleteConfirmDoc(doc);
              } else {
                setBulkAction('ARCHIVE');
                setBulkModalOpen(true);
              }
            }}
            title="Удалить или архивировать выбранные"
          >
            Удалить
          </button>
        )}

        {!isViewer && (
          <button
            className="button btn-warning"
            disabled={selectedUuids.length === 0}
            onClick={() => {
              setBulkAction('CHANGE_STATUS');
              setBulkModalOpen(true);
            }}
            title="Массово изменить статус выбранных запросов"
          >
            Изменить статус ({selectedUuids.length})
          </button>
        )}

        {!isViewer && (
          <button
            className="button btn-export"
            onClick={() => {
              setImportStep(1);
              setImportFile(null);
              setImportText('');
              setImportPreview(null);
              setImportModalOpen(true);
            }}
            title="Двухэтапный импорт CSV"
          >
            Импорт CSV
          </button>
        )}

        <div style={{ flex: 1 }}></div>

        <button
          className="button btn-secondary"
          onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
          title="Расширенные параметры фильтрации"
        >
          {showAdvancedFilters ? 'Скрыть фильтры' : 'Фильтры'}
        </button>

        <button
          className="button btn-secondary"
          onClick={loadRequests}
          disabled={loading}
          title="Перезагрузить таблицу"
        >
          Обновить
        </button>
      </div>

      {/* 2. Filter Panel matching JavaFX .filter-panel */}
      <div className="filter-panel">
        <span style={{ fontWeight: '600', color: 'var(--fx-accent)', fontSize: '12px' }}>Поиск:</span>
        <input
          type="text"
          className="text-field"
          placeholder="Название, код документа..."
          style={{ width: '220px' }}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') { setPage(0); loadRequests(); } }}
        />
        <button className="button btn-secondary btn-sm" onClick={() => { setPage(0); loadRequests(); }}>
          Найти
        </button>

        <div className="separator-v"></div>

        <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Статус:</span>
        <select
          className="text-field"
          style={{ width: '140px' }}
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0); }}
        >
          <option value="">(Все статусы)</option>
          <option value="NEW">Новый</option>
          <option value="IN_PROGRESS">В обработке</option>
          <option value="APPROVED">Одобрен</option>
          <option value="COMPLETED">Завершён</option>
          <option value="REJECTED">Отклонён</option>
          <option value="CANCELLED">Отменён</option>
        </select>

        <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Приоритет:</span>
        <select
          className="text-field"
          style={{ width: '130px' }}
          value={priority}
          onChange={(e) => { setPriority(e.target.value); setPage(0); }}
        >
          <option value="">(Все)</option>
          <option value="LOW">Низкий</option>
          <option value="MEDIUM">Обычный</option>
          <option value="HIGH">Высокий</option>
          <option value="CRITICAL">Критический</option>
        </select>

        <div className="separator-v"></div>

        <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Сортировка:</span>
        <select
          className="text-field"
          style={{ minWidth: '220px' }}
          value={`${sortBy}:${sortDir}`}
          onChange={(e) => {
            const [f, d] = e.target.value.split(':');
            setSortBy(f);
            setSortDir(d);
            setPage(0);
          }}
        >
          <option value="createdAt:desc">Дата создания (новые)</option>
          <option value="createdAt:asc">Дата создания (старые)</option>
          <option value="documentTitle:asc">Название (А-Я)</option>
          <option value="documentTitle:desc">Название (Я-А)</option>
          <option value="estimatedDays:asc">Срок (по возрастанию)</option>
          <option value="estimatedDays:desc">Срок (по убыванию)</option>
          <option value="pageCount:desc">Страницы (убывание)</option>
        </select>

        <div style={{ flex: 1 }}></div>

        <button
          className="button btn-secondary btn-sm"
          onClick={() => {
            setSearch('');
            setStatus('');
            setPriority('');
            setUserId('');
            setCategory('');
            setConfidential('');
            setArchived('false');
            setSortBy('createdAt');
            setSortDir('desc');
            setPage(0);
          }}
        >
          Сбросить
        </button>
      </div>

      {/* Расширенные фильтры */}
      {showAdvancedFilters && (
        <div className="filter-panel" style={{ backgroundColor: '#1f2030' }}>
          <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Заявитель:</span>
          <select
            className="text-field"
            style={{ width: '220px' }}
            value={userId}
            onChange={(e) => { setUserId(e.target.value); setPage(0); }}
          >
            <option value="">(Все заявители)</option>
            {applicants.map((a) => (
              <option key={a.id} value={a.id}>
                {a.fullName} ({a.department || 'Без отдела'})
              </option>
            ))}
          </select>

          <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Категория:</span>
          <input
            type="text"
            className="text-field"
            placeholder="Категория документа"
            style={{ width: '160px' }}
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />

          <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Доступ:</span>
          <select
            className="text-field"
            style={{ width: '140px' }}
            value={confidential}
            onChange={(e) => { setConfidential(e.target.value); setPage(0); }}
          >
            <option value="">(Все)</option>
            <option value="true">Конфиденциально</option>
            <option value="false">Открытый</option>
          </select>

          <span style={{ fontWeight: '600', color: 'var(--fx-subtext0)', fontSize: '12px' }}>Архив:</span>
          <select
            className="text-field"
            style={{ width: '140px' }}
            value={archived}
            onChange={(e) => { setArchived(e.target.value); setPage(0); }}
          >
            <option value="false">Только активные</option>
            <option value="true">Только архивные</option>
            <option value="all">Все записи</option>
          </select>
        </div>
      )}

      {/* 3. Table matching JavaFX TableView */}
      <div className="table-view-container" style={{ flex: 1, minHeight: '340px' }}>
        <table className="table-view">
          <thead>
            <tr>
              <th style={{ width: '36px', textAlign: 'center' }}>
                <input
                  type="checkbox"
                  checked={requests.length > 0 && selectedUuids.length === requests.length}
                  onChange={handleSelectAll}
                />
              </th>
              <th style={{ width: '130px', whiteSpace: 'nowrap' }}>Код запроса</th>
              <th style={{ minWidth: '220px' }}>Документ</th>
              <th style={{ width: '130px', whiteSpace: 'nowrap' }}>Категория</th>
              <th style={{ width: '110px', whiteSpace: 'nowrap', textAlign: 'center' }}>Статус</th>
              <th style={{ width: '110px', whiteSpace: 'nowrap', textAlign: 'center' }}>Приоритет</th>
              <th style={{ width: '80px', whiteSpace: 'nowrap', textAlign: 'center' }}>Срок</th>
              <th style={{ width: '220px', minWidth: '200px' }}>Заявитель</th>
              <th style={{ width: '50px', textAlign: 'center', whiteSpace: 'nowrap' }}>Стр.</th>
              <th style={{ width: '100px' }}>Теги</th>
              <th style={{ width: '80px', textAlign: 'center', whiteSpace: 'nowrap' }}>Заметки</th>
              <th style={{ width: '180px', textAlign: 'right', whiteSpace: 'nowrap' }}>Действия</th>
            </tr>
          </thead>
          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan={12} style={{ textAlign: 'center', padding: '36px', color: 'var(--fx-overlay0)' }}>
                  {loading ? 'Загрузка данных...' : 'Нет данных для отображения'}
                </td>
              </tr>
            ) : (
              requests.map((r) => {
                const isSelected = selectedUuids.includes(r.uuid);
                return (
                  <tr key={r.uuid} className={isSelected ? 'selected' : ''}>
                    <td style={{ textAlign: 'center' }}>
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => handleToggleSelect(r.uuid)}
                      />
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <span style={{ fontWeight: '600', color: 'var(--fx-accent)', fontFamily: 'monospace' }}>
                        {r.requestNumber}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontWeight: '600', color: 'var(--fx-text)' }}>
                        {r.documentTitle}
                      </div>
                      {r.description && (
                        <div style={{ fontSize: '11px', color: 'var(--fx-subtext0)', maxWidth: '320px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {r.description}
                        </div>
                      )}
                      {r.confidential && (
                        <span style={{ fontSize: '10px', color: 'var(--fx-danger)', fontWeight: '600' }}>
                          [Конфиденциально]
                        </span>
                      )}
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <span style={{ color: 'var(--fx-export)' }}>{r.category}</span>
                    </td>
                    <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                      {renderStatusBadge(r.status)}
                    </td>
                    <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                      {renderPriorityBadge(r.priority)}
                    </td>
                    <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                      <span style={{
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '11px',
                        fontWeight: '600',
                        whiteSpace: 'nowrap',
                        display: 'inline-block',
                        backgroundColor: r.estimatedDays <= 2 ? 'rgba(166,227,161,0.2)' : r.estimatedDays <= 4 ? 'rgba(137,180,250,0.2)' : 'rgba(250,179,135,0.2)',
                        color: r.estimatedDays <= 2 ? 'var(--fx-success)' : r.estimatedDays <= 4 ? 'var(--fx-accent)' : 'var(--fx-warning)'
                      }}>
                        {r.estimatedDays} дн.
                      </span>
                    </td>
                    <td style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      <div style={{ color: 'var(--fx-text)', fontWeight: '500', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {r.userName}
                      </div>
                      <div style={{ fontSize: '11px', color: 'var(--fx-subtext0)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {r.userDepartment || 'Без отдела'}
                      </div>
                    </td>
                    <td style={{ textAlign: 'center', color: 'var(--fx-subtext0)', whiteSpace: 'nowrap' }}>
                      {r.pageCount}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                        {r.tags && r.tags.map((t) => (
                          <span key={t} className="badge-tag">#{t}</span>
                        ))}
                      </div>
                    </td>
                    <td style={{ textAlign: 'center', whiteSpace: 'nowrap' }}>
                      <button
                        className="button btn-secondary btn-sm"
                        onClick={() => openNotesModal(r)}
                        title="Открыть историю заметок"
                        style={{ padding: '2px 8px' }}
                      >
                        Заметки ({r.notesCount || 0})
                      </button>
                    </td>
                    <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                      <button
                        className="button btn-secondary btn-sm"
                        onClick={() => setDetailDoc(r)}
                        title="Подробная информация"
                        style={{ marginRight: '4px' }}
                      >
                        Инфо
                      </button>
                      {!isViewer && (
                        <button
                          className="button btn-primary btn-sm"
                          onClick={() => setEditDoc(r)}
                          title="Редактировать"
                          style={{ marginRight: '4px' }}
                        >
                          Ред.
                        </button>
                      )}
                      {!isViewer && (
                        r.archived ? (
                          <button
                            className="button btn-success btn-sm"
                            onClick={() => handleRestore(r.uuid)}
                            title="Восстановить из архива"
                            style={{ marginRight: '4px' }}
                          >
                            Восст.
                          </button>
                        ) : (
                          <button
                            className="button btn-warning btn-sm"
                            onClick={() => handleArchive(r.uuid)}
                            title="Отправить в архив"
                            style={{ marginRight: '4px' }}
                          >
                            В архив
                          </button>
                        )
                      )}
                      {isAdmin && (
                        <button
                          className="button btn-danger btn-sm"
                          onClick={() => setDeleteConfirmDoc(r)}
                          title="Окончательно удалить"
                        >
                          Удалить
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Пагинация */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '4px 6px' }}>
        <div style={{ color: 'var(--fx-subtext0)', fontSize: '12px' }}>
          Выбрано: <b>{selectedUuids.length}</b> из <b>{totalElements}</b> записей
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

      {/* =========================================================================
          МОДАЛЬНЫЕ ОКНА БЕЗ СМАЙЛИКОВ
          ========================================================================= */}

      {/* 1. Создание запроса (matching request_dialog.fxml) */}
      {createModalOpen && (
        <RequestFormModal
          title="Новый запрос на архивный документ"
          applicants={applicants}
          onClose={() => setCreateModalOpen(false)}
          onSubmit={async (payload) => {
            try {
              await api.createDocumentRequest(payload);
              addToast({ type: 'success', message: 'Запрос на документ успешно создан' });
              setCreateModalOpen(false);
              loadRequests();
            } catch (err) {
              addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
            }
          }}
        />
      )}

      {/* 2. Редактирование запроса */}
      {editDoc && (
        <RequestFormModal
          title={`Редактирование запроса: ${editDoc.requestNumber}`}
          initialData={editDoc}
          applicants={applicants}
          onClose={() => setEditDoc(null)}
          onSubmit={async (payload) => {
            try {
              await api.updateDocumentRequest(editDoc.uuid, { ...payload, version: editDoc.version });
              addToast({ type: 'success', message: 'Данные запроса успешно обновлены' });
              setEditDoc(null);
              loadRequests();
            } catch (err) {
              addToast({ type: 'error', message: err.message, correlationId: err.correlationId });
            }
          }}
        />
      )}

      {/* 3. Просмотр деталей запроса */}
      {detailDoc && (
        <div className="modal-overlay" onClick={() => setDetailDoc(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '620px' }}>
            <div className="modal-header">
              <h3>Карточка архивного запроса: {detailDoc.requestNumber}</h3>
              <button className="close-btn" onClick={() => setDetailDoc(null)}>x</button>
            </div>
            <div className="modal-body" style={{ gap: '12px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <span className="stat-label">Документ:</span>
                  <div style={{ fontWeight: '600', fontSize: '14px' }}>{detailDoc.documentTitle}</div>
                </div>
                <div>
                  <span className="stat-label">Категория:</span>
                  <div style={{ color: 'var(--fx-export)', fontWeight: '600' }}>{detailDoc.category}</div>
                </div>
                <div>
                  <span className="stat-label">Статус:</span>
                  <div>{renderStatusBadge(detailDoc.status)}</div>
                </div>
                <div>
                  <span className="stat-label">Приоритет:</span>
                  <div>{renderPriorityBadge(detailDoc.priority)}</div>
                </div>
                <div>
                  <span className="stat-label">Заявитель:</span>
                  <div style={{ fontWeight: '500' }}>{detailDoc.userName} ({detailDoc.userDepartment})</div>
                </div>
                <div>
                  <span className="stat-label">Расчетный срок исполнения:</span>
                  <div style={{ color: 'var(--fx-accent)', fontWeight: '600' }}>{detailDoc.estimatedDays} рабочих дней</div>
                </div>
                <div>
                  <span className="stat-label">Количество страниц:</span>
                  <div>{detailDoc.pageCount} стр.</div>
                </div>
                <div>
                  <span className="stat-label">Режим доступа:</span>
                  <div>{detailDoc.confidential ? 'Конфиденциальный' : 'Открытый'}</div>
                </div>
              </div>

              {detailDoc.description && (
                <div>
                  <span className="stat-label">Описание:</span>
                  <div style={{ backgroundColor: 'var(--fx-surface0)', padding: '8px 12px', borderRadius: '5px', marginTop: '4px' }}>
                    {detailDoc.description}
                  </div>
                </div>
              )}

              {detailDoc.tags && detailDoc.tags.length > 0 && (
                <div>
                  <span className="stat-label">Теги:</span>
                  <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '4px' }}>
                    {detailDoc.tags.map((t) => (
                      <span key={t} className="badge-tag">#{t}</span>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="button btn-secondary" onClick={() => setDetailDoc(null)}>
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 4. Заметки (Notes Modal) */}
      {notesModalDoc && (
        <div className="modal-overlay" onClick={() => setNotesModalDoc(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '620px' }}>
            <div className="modal-header">
              <h3>История заметок: {notesModalDoc.requestNumber}</h3>
              <button className="close-btn" onClick={() => setNotesModalDoc(null)}>x</button>
            </div>
            <div className="modal-body">
              {/* Список заметок */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '280px', overflowY: 'auto' }}>
                {notes.length === 0 ? (
                  <div style={{ textAlign: 'center', color: 'var(--fx-overlay0)', padding: '20px' }}>
                    Заметки отсутствуют. Добавьте первую заметку ниже.
                  </div>
                ) : (
                  notes.map((n) => (
                    <div
                      key={n.id}
                      style={{
                        backgroundColor: n.pinned ? 'rgba(250,179,135,0.08)' : 'var(--fx-surface0)',
                        border: `1px solid ${n.pinned ? 'var(--fx-warning)' : 'var(--fx-border)'}`,
                        borderRadius: '5px',
                        padding: '8px 12px'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                        <div style={{ fontSize: '12px', color: 'var(--fx-accent)', fontWeight: '600' }}>
                          {n.pinned && <span style={{ color: 'var(--fx-warning)', marginRight: '6px', fontSize: '11px' }}>[Закреплено]</span>}
                          <span>{n.authorUsername}</span>
                          <span style={{ color: 'var(--fx-subtext0)', fontWeight: 'normal', marginLeft: '8px' }}>
                            {new Date(n.createdAt).toLocaleString()}
                          </span>
                        </div>
                        {!isViewer && (
                          <div style={{ display: 'flex', gap: '4px' }}>
                            <button
                              className="button btn-secondary btn-sm"
                              onClick={() => handleTogglePinNote(n.id)}
                              title={n.pinned ? 'Открепить' : 'Закрепить'}
                            >
                              {n.pinned ? 'Открепить' : 'Закрепить'}
                            </button>
                            <button
                              className="button btn-danger btn-sm"
                              onClick={() => handleDeleteNote(n.id)}
                              title="Удалить заметку"
                            >
                              Удалить
                            </button>
                          </div>
                        )}
                      </div>
                      <div style={{ fontSize: '12px', color: 'var(--fx-text)', whiteSpace: 'pre-wrap' }}>
                        {n.content}
                      </div>
                    </div>
                  ))
                )}
              </div>

              <div className="separator-h"></div>

              {/* Форма добавления заметки */}
              {!isViewer && (
                <form onSubmit={handleAddNote} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <textarea
                    placeholder="Напишите новую заметку по документу..."
                    value={newNoteContent}
                    onChange={(e) => setNewNoteContent(e.target.value)}
                    required
                  />
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={newNotePinned}
                        onChange={(e) => setNewNotePinned(e.target.checked)}
                      />
                      <span>Закрепить заметку в начале списка</span>
                    </label>
                    <button type="submit" className="button btn-primary btn-sm">
                      Добавить заметку
                    </button>
                  </div>
                </form>
              )}
            </div>
            <div className="modal-footer">
              <button className="button btn-secondary" onClick={() => setNotesModalDoc(null)}>
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 5. Массовые операции (Bulk Modal) */}
      {bulkModalOpen && (
        <div className="modal-overlay" onClick={() => setBulkModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '460px' }}>
            <div className="modal-header">
              <h3>Массовая операция ({selectedUuids.length} записей)</h3>
              <button className="close-btn" onClick={() => setBulkModalOpen(false)}>x</button>
            </div>
            <div className="modal-body">
              <div>
                <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>
                  Действие:
                </label>
                <select
                  className="text-field"
                  style={{ width: '100%' }}
                  value={bulkAction}
                  onChange={(e) => setBulkAction(e.target.value)}
                >
                  <option value="CHANGE_STATUS">Изменить статус</option>
                  <option value="ADD_TAG">Добавить тег</option>
                  <option value="REMOVE_TAG">Удалить тег</option>
                  <option value="ARCHIVE">Переместить в архив</option>
                  <option value="RESTORE">Восстановить из архива</option>
                </select>
              </div>

              {bulkAction === 'CHANGE_STATUS' && (
                <div>
                  <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>
                    Новый статус:
                  </label>
                  <select
                    className="text-field"
                    style={{ width: '100%' }}
                    value={bulkStatus}
                    onChange={(e) => setBulkStatus(e.target.value)}
                  >
                    <option value="NEW">Новый</option>
                    <option value="IN_PROGRESS">В обработке</option>
                    <option value="APPROVED">Одобрен</option>
                    <option value="COMPLETED">Завершён</option>
                    <option value="REJECTED">Отклонён</option>
                    <option value="CANCELLED">Отменён</option>
                  </select>
                </div>
              )}

              {(bulkAction === 'ADD_TAG' || bulkAction === 'REMOVE_TAG') && (
                <div>
                  <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>
                    Тег:
                  </label>
                  <input
                    type="text"
                    className="text-field"
                    placeholder="Например: срочно"
                    style={{ width: '100%' }}
                    value={bulkTag}
                    onChange={(e) => setBulkTag(e.target.value)}
                  />
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="button btn-secondary" onClick={() => setBulkModalOpen(false)}>
                Отмена
              </button>
              <button className="button btn-warning" onClick={executeBulk}>
                Применить
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 6. Импорт CSV (2-stage modal) */}
      {importModalOpen && (
        <div className="modal-overlay" onClick={() => setImportModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '680px' }}>
            <div className="modal-header">
              <h3>Импорт запросов из CSV (Этап {importStep} из 2)</h3>
              <button className="close-btn" onClick={() => setImportModalOpen(false)}>x</button>
            </div>
            <div className="modal-body">
              {importStep === 1 ? (
                <form onSubmit={handlePreviewImport} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div>
                    <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '4px' }}>
                      Выберите CSV-файл на компьютере:
                    </label>
                    <input
                      type="file"
                      accept=".csv,text/csv"
                      className="text-field"
                      style={{ width: '100%' }}
                      onChange={(e) => setImportFile(e.target.files[0] || null)}
                    />
                  </div>

                  <div style={{ textAlign: 'center', color: 'var(--fx-subtext0)', fontSize: '11px' }}>
                    — или вставьте содержимое CSV ниже —
                  </div>

                  <div>
                    <textarea
                      placeholder={`document_title;category;status;priority;page_count;is_confidential;user_id\nПриказ №142;Приказы;NEW;HIGH;15;true;1`}
                      value={importText}
                      onChange={(e) => setImportText(e.target.value)}
                      style={{ height: '120px', fontFamily: 'monospace', fontSize: '11px' }}
                    />
                  </div>

                  <button type="submit" className="button btn-primary" disabled={importLoading}>
                    {importLoading ? 'Анализ...' : 'Далее: Предварительный просмотр'}
                  </button>
                </form>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <span className="stat-label">Всего строк: </span>
                      <b style={{ color: 'var(--fx-accent)' }}>{importPreview?.totalRows || 0}</b>
                      <span style={{ margin: '0 8px' }}>|</span>
                      <span className="stat-label">Валидных: </span>
                      <b style={{ color: 'var(--fx-success)' }}>{importPreview?.validRows || 0}</b>
                      <span style={{ margin: '0 8px' }}>|</span>
                      <span className="stat-label">Ошибочных: </span>
                      <b style={{ color: 'var(--fx-danger)' }}>{importPreview?.invalidRows || 0}</b>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span className="stat-label">Режим:</span>
                      <select
                        className="text-field"
                        value={importMode}
                        onChange={(e) => setImportMode(e.target.value)}
                      >
                        <option value="CREATE_ONLY">Только создание новых</option>
                        <option value="UPDATE_EXISTING">Создание и обновление существующих</option>
                      </select>
                    </div>
                  </div>

                  {/* Таблица предпросмотра */}
                  <div className="table-view-container" style={{ maxHeight: '200px', overflowY: 'auto' }}>
                    <table className="table-view" style={{ fontSize: '11px' }}>
                      <thead>
                        <tr>
                          <th>Код</th>
                          <th>Документ</th>
                          <th>Категория</th>
                          <th>Статус</th>
                          <th>Заявитель</th>
                          <th>Стр.</th>
                        </tr>
                      </thead>
                      <tbody>
                        {importPreview?.previewData?.map((row, idx) => (
                          <tr key={idx}>
                            <td>{row.requestCode || '-'}</td>
                            <td>{row.documentTitle}</td>
                            <td>{row.category}</td>
                            <td>{row.status}</td>
                            <td>{row.userId}</td>
                            <td>{row.pageCount}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '6px' }}>
                    <button className="button btn-secondary" onClick={() => setImportStep(1)}>
                      Назад к выбору файла
                    </button>
                    <button className="button btn-success" onClick={handleApplyImport} disabled={importLoading}>
                      {importLoading ? 'Импортирование...' : 'Применить импорт'}
                    </button>
                  </div>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="button btn-secondary" onClick={() => setImportModalOpen(false)}>
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 7. Подтверждение удаления */}
      {deleteConfirmDoc && (
        <div className="modal-overlay" onClick={() => setDeleteConfirmDoc(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '420px' }}>
            <div className="modal-header">
              <h3 style={{ color: 'var(--fx-danger)' }}>Подтверждение удаления</h3>
              <button className="close-btn" onClick={() => setDeleteConfirmDoc(null)}>x</button>
            </div>
            <div className="modal-body">
              <p style={{ margin: 0 }}>
                Вы действительно хотите навсегда удалить запрос{' '}
                <b style={{ color: 'var(--fx-accent)' }}>{deleteConfirmDoc.requestNumber}</b> ({deleteConfirmDoc.documentTitle})?
              </p>
              <p style={{ margin: 0, fontSize: '12px', color: 'var(--fx-danger)' }}>
                Это действие необратимо и удалит все связанные заметки.
              </p>
            </div>
            <div className="modal-footer">
              <button className="button btn-secondary" onClick={() => setDeleteConfirmDoc(null)}>
                Отмена
              </button>
              <button className="button btn-danger" onClick={() => handlePermanentDelete(deleteConfirmDoc.uuid)}>
                Окончательно удалить
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

/**
 * Модальное окно формы создания/редактирования запроса
 * Полностью повторяет раскладку JavaFX request_dialog.fxml
 */
function RequestFormModal({ title, initialData, applicants, onClose, onSubmit }) {
  const [userId, setUserId] = useState(initialData?.userId || (applicants[0]?.id || ''));
  const [documentTitle, setDocumentTitle] = useState(initialData?.documentTitle || '');
  const [description, setDescription] = useState(initialData?.description || '');
  const [category, setCategory] = useState(initialData?.category || 'Приказы');
  const [priority, setPriority] = useState(initialData?.priority || 'MEDIUM');
  const [pageCount, setPageCount] = useState(initialData?.pageCount || 1);
  const [confidential, setConfidential] = useState(initialData?.confidential || false);
  const [tagsStr, setTagsStr] = useState(initialData?.tags ? initialData.tags.join(', ') : '');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!documentTitle.trim()) return;

    const tags = tagsStr
      .split(',')
      .map((t) => t.trim())
      .filter((t) => t.length > 0);

    onSubmit({
      userId: Number(userId),
      documentTitle: documentTitle.trim(),
      description: description.trim() || null,
      category: category.trim(),
      priority,
      pageCount: Number(pageCount) || 1,
      confidential,
      tags,
    });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="close-btn" onClick={onClose}>x</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body" style={{ gap: '10px' }}>
            
            {/* Пользователь matching userCombo */}
            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Пользователь:
              </label>
              <select
                className="text-field"
                style={{ width: '100%' }}
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                required
              >
                {applicants.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.fullName} ({a.department || 'Без отдела'})
                  </option>
                ))}
              </select>
            </div>

            {/* Название документа matching titleField */}
            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Название документа:
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="Введите название документа"
                style={{ width: '100%' }}
                value={documentTitle}
                onChange={(e) => setDocumentTitle(e.target.value)}
                required
                autoFocus
              />
            </div>

            {/* Описание matching descriptionArea */}
            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Описание:
              </label>
              <textarea
                placeholder="Описание запроса (необязательно)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                style={{ minHeight: '55px' }}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              {/* Категория matching categoryCombo */}
              <div>
                <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                  Категория:
                </label>
                <select
                  className="text-field"
                  style={{ width: '100%' }}
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                >
                  <option value="Приказы">Приказы</option>
                  <option value="Договоры">Договоры</option>
                  <option value="Протоколы">Протоколы</option>
                  <option value="Акты">Акты</option>
                  <option value="Личные дела">Личные дела</option>
                  <option value="Отчеты">Отчеты</option>
                  <option value="Справки">Справки</option>
                  <option value="Учредительные">Учредительные</option>
                </select>
              </div>

              {/* Приоритет matching priorityCombo */}
              <div>
                <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                  Приоритет:
                </label>
                <select
                  className="text-field"
                  style={{ width: '100%' }}
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                >
                  <option value="LOW">Низкий</option>
                  <option value="MEDIUM">Обычный</option>
                  <option value="HIGH">Высокий</option>
                  <option value="CRITICAL">Критический</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', alignItems: 'center' }}>
              <div>
                <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                  Количество страниц:
                </label>
                <input
                  type="number"
                  min="1"
                  className="text-field"
                  style={{ width: '100%' }}
                  value={pageCount}
                  onChange={(e) => setPageCount(e.target.value)}
                  required
                />
              </div>

              <div style={{ paddingTop: '16px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', fontSize: '12px' }}>
                  <input
                    type="checkbox"
                    checked={confidential}
                    onChange={(e) => setConfidential(e.target.checked)}
                  />
                  <span>Конфиденциальный документ</span>
                </label>
              </div>
            </div>

            {/* Теги */}
            <div>
              <label className="section-label" style={{ fontSize: '12px', display: 'block', marginBottom: '3px' }}>
                Теги (через запятую):
              </label>
              <input
                type="text"
                className="text-field"
                placeholder="важно, архив, грант"
                style={{ width: '100%' }}
                value={tagsStr}
                onChange={(e) => setTagsStr(e.target.value)}
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
