-- ============================================================================
-- V1__init_schema.sql: Схема базы данных ИС «Архив документов» (КР-3)
-- Поддерживает как чистую установку, так и расширение существующей схемы КР1/КР2
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Справочник пользователей архива (заявители)
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(200) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(30),
    department  VARCHAR(150),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Расширение существующей таблицы users (если создана в КР1/КР2)
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. Основная сущность: Запросы на выдачу архивных документов
CREATE TABLE IF NOT EXISTS document_requests (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    request_number  VARCHAR(64) UNIQUE,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    document_title  VARCHAR(300) NOT NULL,
    description     TEXT,
    category        VARCHAR(100) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'NEW'
                        CHECK (status IN ('NEW', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED')),
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    page_count      INTEGER NOT NULL DEFAULT 1 CHECK (page_count >= 1),
    is_confidential BOOLEAN NOT NULL DEFAULT FALSE,
    estimated_days  INTEGER NOT NULL DEFAULT 3,
    is_archived     BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Расширение существующей таблицы document_requests (если создана в КР1/КР2)
ALTER TABLE document_requests ALTER COLUMN id TYPE BIGINT;
ALTER TABLE document_requests ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid();
UPDATE document_requests SET uuid = gen_random_uuid() WHERE uuid IS NULL;
ALTER TABLE document_requests ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS request_number VARCHAR(64);
UPDATE document_requests SET request_number = 'REQ-2026-' || LPAD(id::text, 4, '0') WHERE request_number IS NULL;
ALTER TABLE document_requests ALTER COLUMN request_number SET NOT NULL;

ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS page_count INTEGER NOT NULL DEFAULT 1;
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS is_confidential BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS estimated_days INTEGER NOT NULL DEFAULT 3;
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE document_requests ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Теги для основной сущности
CREATE TABLE IF NOT EXISTS document_request_tags (
    request_uuid    UUID NOT NULL REFERENCES document_requests(uuid) ON DELETE CASCADE,
    tag             VARCHAR(50) NOT NULL,
    PRIMARY KEY (request_uuid, tag)
);

-- 4. Заметки для основной сущности
CREATE TABLE IF NOT EXISTS document_notes (
    id              UUID PRIMARY KEY,
    request_uuid    UUID NOT NULL REFERENCES document_requests(uuid) ON DELETE CASCADE,
    author_username VARCHAR(100) NOT NULL,
    content         TEXT NOT NULL,
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Учетные записи работников / пользователей системы
CREATE TABLE IF NOT EXISTS app_accounts (
    id                     BIGSERIAL PRIMARY KEY,
    username               VARCHAR(100) NOT NULL UNIQUE,
    password_hash          VARCHAR(255) NOT NULL,
    full_name              VARCHAR(200),
    email                  VARCHAR(150),
    role                   VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
    is_active              BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password   BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts  INTEGER NOT NULL DEFAULT 0,
    locked_until           TIMESTAMP,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE app_accounts ALTER COLUMN id TYPE BIGINT;
ALTER TABLE app_accounts ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE app_accounts ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app_accounts ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE app_accounts ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE app_accounts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 6. Сессии Refresh-токенов
CREATE TABLE IF NOT EXISTS refresh_sessions (
    id          UUID PRIMARY KEY,
    account_id  BIGINT NOT NULL REFERENCES app_accounts(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    user_agent  VARCHAR(500),
    ip_address  VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE refresh_sessions ALTER COLUMN account_id TYPE BIGINT;

-- 7. Журнал аудита
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100) NOT NULL,
    event           VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(100),
    correlation_id  VARCHAR(100),
    details         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS entity_id VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

-- Индексы для ускорения поиска и фильтрации
CREATE INDEX IF NOT EXISTS idx_users_full_name ON users(full_name);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

CREATE INDEX IF NOT EXISTS idx_doc_req_uuid ON document_requests(uuid);
CREATE INDEX IF NOT EXISTS idx_doc_req_number ON document_requests(request_number);
CREATE INDEX IF NOT EXISTS idx_doc_req_user_id ON document_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_doc_req_status ON document_requests(status);
CREATE INDEX IF NOT EXISTS idx_doc_req_priority ON document_requests(priority);
CREATE INDEX IF NOT EXISTS idx_doc_req_category ON document_requests(category);
CREATE INDEX IF NOT EXISTS idx_doc_req_archived ON document_requests(is_archived);
CREATE INDEX IF NOT EXISTS idx_doc_req_confidential ON document_requests(is_confidential);
CREATE INDEX IF NOT EXISTS idx_doc_req_est_days ON document_requests(estimated_days);
CREATE INDEX IF NOT EXISTS idx_doc_req_page_count ON document_requests(page_count);
CREATE INDEX IF NOT EXISTS idx_doc_req_created_at ON document_requests(created_at);

CREATE INDEX IF NOT EXISTS idx_doc_tags_tag ON document_request_tags(tag);
CREATE INDEX IF NOT EXISTS idx_doc_notes_req ON document_notes(request_uuid);
CREATE INDEX IF NOT EXISTS idx_refresh_acc ON refresh_sessions(account_id);
CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_logs(event);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
