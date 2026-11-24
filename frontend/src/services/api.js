const BASE_URL = '/api';

class ApiService {
  constructor() {
    this.accessToken = localStorage.getItem('access_token') || null;
    this.refreshToken = localStorage.getItem('refresh_token') || null;
    this.user = JSON.parse(localStorage.getItem('user_info') || 'null');
    this.onAuthFailure = null;
  }

  setAuthFailureHandler(handler) {
    this.onAuthFailure = handler;
  }

  setTokens(accessToken, refreshToken, user) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.user = user;
    if (accessToken) localStorage.setItem('access_token', accessToken);
    else localStorage.removeItem('access_token');
    if (refreshToken) localStorage.setItem('refresh_token', refreshToken);
    else localStorage.removeItem('refresh_token');
    if (user) localStorage.setItem('user_info', JSON.stringify(user));
    else localStorage.removeItem('user_info');
  }

  clearAuth() {
    this.setTokens(null, null, null);
  }

  isAuthenticated() {
    return !!this.accessToken;
  }

  getUser() {
    return this.user;
  }

  async request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;
    const headers = {
      ...(options.headers || {}),
    };

    if (!(options.body instanceof FormData) && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    if (this.accessToken) {
      headers['Authorization'] = `Bearer ${this.accessToken}`;
    }

    let response;
    try {
      response = await fetch(url, { ...options, headers });
    } catch (err) {
      throw new Error('Сетевая ошибка подключения к серверу: ' + err.message);
    }

    // Обработка 401 Unauthorized с попыткой автоматического обновления access токена
    if (response.status === 401 && this.refreshToken && !endpoint.includes('/auth/refresh') && !endpoint.includes('/auth/login')) {
      try {
        const refreshRes = await fetch(`${BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: this.refreshToken }),
        });

        if (refreshRes.ok) {
          const authData = await refreshRes.json();
          this.setTokens(authData.accessToken, authData.refreshToken, authData.user);
          // Повторяем исходный запрос с новым токеном
          headers['Authorization'] = `Bearer ${this.accessToken}`;
          response = await fetch(url, { ...options, headers });
        } else {
          this.clearAuth();
          if (this.onAuthFailure) this.onAuthFailure();
        }
      } catch (e) {
        this.clearAuth();
        if (this.onAuthFailure) this.onAuthFailure();
      }
    }

    if (response.status === 204) {
      return null;
    }

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch (e) {
        errorData = { message: `Ошибка сервера (${response.status}: ${response.statusText})` };
      }

      const err = new Error(errorData.message || 'Произошла непредвиденная ошибка');
      err.status = response.status;
      err.correlationId = errorData.correlationId;
      err.validationErrors = errorData.validationErrors;
      throw err;
    }

    // Если ответ blob (например CSV экспорт)
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('text/csv')) {
      return await response.blob();
    }

    return await response.json();
  }

  // --- Аутентификация ---
  async login(username, password) {
    const data = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    this.setTokens(data.accessToken, data.refreshToken, data.user);
    return data;
  }

  async logout() {
    try {
      if (this.refreshToken) {
        await this.request('/auth/logout', {
          method: 'POST',
          body: JSON.stringify({ refreshToken: this.refreshToken }),
        });
      }
    } finally {
      this.clearAuth();
    }
  }

  async getMe() {
    const user = await this.request('/auth/me');
    this.user = user;
    localStorage.setItem('user_info', JSON.stringify(user));
    return user;
  }

  async changePassword(oldPassword, newPassword) {
    return await this.request('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ oldPassword, newPassword }),
    });
  }

  // --- Основная сущность: Document Requests ---
  async getDocumentRequests(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        query.append(k, v);
      }
    });
    return await this.request(`/document-requests?${query.toString()}`);
  }

  async getDocumentRequest(id) {
    return await this.request(`/document-requests/${id}`);
  }

  async createDocumentRequest(data) {
    return await this.request('/document-requests', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateDocumentRequest(id, data) {
    return await this.request(`/document-requests/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async archiveDocumentRequest(id) {
    return await this.request(`/document-requests/${id}`, {
      method: 'DELETE',
    });
  }

  async restoreDocumentRequest(id) {
    return await this.request(`/document-requests/${id}/restore`, {
      method: 'POST',
    });
  }

  async permanentDeleteDocumentRequest(id) {
    return await this.request(`/document-requests/${id}/permanent`, {
      method: 'DELETE',
    });
  }

  async bulkOperation(uuids, action, targetStatus, tag) {
    return await this.request('/document-requests/bulk', {
      method: 'POST',
      body: JSON.stringify({ uuids, action, targetStatus, tag }),
    });
  }

  async exportCsv(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        query.append(k, v);
      }
    });
    return await this.request(`/document-requests/export?${query.toString()}`);
  }

  async previewImportCsv(formData) {
    return await this.request('/document-requests/import/preview', {
      method: 'POST',
      body: formData,
    });
  }

  async previewImportCsvText(text) {
    return await this.request('/document-requests/import/preview', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: text,
    });
  }

  async applyImportCsv(mode, rows) {
    return await this.request('/document-requests/import/apply', {
      method: 'POST',
      body: JSON.stringify({ mode, rows }),
    });
  }

  // --- Заметки ---
  async getNotes(requestId) {
    return await this.request(`/document-requests/${requestId}/notes`);
  }

  async createNote(requestId, content, isPinned) {
    return await this.request(`/document-requests/${requestId}/notes`, {
      method: 'POST',
      body: JSON.stringify({ content, pinned: isPinned }),
    });
  }

  async updateNote(requestId, noteId, content, isPinned) {
    return await this.request(`/document-requests/${requestId}/notes/${noteId}`, {
      method: 'PUT',
      body: JSON.stringify({ content, pinned: isPinned }),
    });
  }

  async deleteNote(requestId, noteId) {
    return await this.request(`/document-requests/${requestId}/notes/${noteId}`, {
      method: 'DELETE',
    });
  }

  async togglePinNote(requestId, noteId) {
    return await this.request(`/document-requests/${requestId}/notes/${noteId}/pin`, {
      method: 'PATCH',
    });
  }

  // --- Справочник заявителей: /api/users ---
  async getArchiveUsers(includeInactive = false) {
    return await this.request(`/users?includeInactive=${includeInactive}`);
  }

  async getArchiveUser(id) {
    return await this.request(`/users/${id}`);
  }

  async createArchiveUser(data) {
    return await this.request('/users', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateArchiveUser(id, data) {
    return await this.request(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async toggleArchiveUserActive(id) {
    return await this.request(`/users/${id}/toggle`, {
      method: 'PATCH',
    });
  }

  // --- Аналитика ---
  async getAnalyticsSummary() {
    return await this.request('/analytics/summary');
  }

  // --- Администрирование пользователей ---
  async getAdminUsers(page = 0, size = 20) {
    return await this.request(`/admin/users?page=${page}&size=${size}`);
  }

  async createAdminUser(data) {
    return await this.request('/admin/users', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateAdminUser(id, data) {
    return await this.request(`/admin/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async resetAdminUserPassword(id) {
    return await this.request(`/admin/users/${id}/reset-password`, {
      method: 'POST',
    });
  }

  // --- Журнал аудита ---
  async getAuditLogs(params = {}) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') {
        query.append(k, v);
      }
    });
    return await this.request(`/admin/audit?${query.toString()}`);
  }
}

export const api = new ApiService();
