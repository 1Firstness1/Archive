DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS document_requests CASCADE;
DROP TABLE IF EXISTS app_accounts CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Таблица пользователей архива
CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    full_name   VARCHAR(200) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(30),
    department  VARCHAR(150),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица запросов на выдачу архивных документов
CREATE TABLE document_requests (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_title  VARCHAR(300) NOT NULL,
    description     TEXT,
    category        VARCHAR(100) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'NEW'
                        CHECK (status IN ('NEW', 'IN_PROGRESS', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED')),
    priority        VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица учетных записей работников архива (для входа в систему)
CREATE TABLE app_accounts (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE, -- логин работника
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(200),                 -- ФИО работника
    email         VARCHAR(150),
    role          VARCHAR(50) DEFAULT 'USER',   -- роль (ADMIN, USER)
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Таблица журнала аудита действий работников
CREATE TABLE audit_logs (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(100) NOT NULL,          -- работник (логин)
    event       VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Начальные пользователи
INSERT INTO users (full_name, email, phone, department) VALUES
    ('Иванов Иван Иванович',     'ivanov@mirea.ru',    '+7-900-111-11-11', 'Исторический отдел'),
    ('Петрова Мария Сергеевна',   'petrova@mirea.ru',   '+7-900-222-22-22', 'Юридический отдел'),
    ('Сидоров Алексей Петрович',  'sidorov@mirea.ru',   '+7-900-333-33-33', 'Бухгалтерия'),
    ('Козлова Елена Дмитриевна',  'kozlova@mirea.ru',   '+7-900-444-44-44', 'Отдел кадров'),
    ('Новиков Дмитрий Андреевич', 'novikov@mirea.ru',   '+7-900-555-55-55', 'Научный отдел'),
    ('Федорова Анна Олеговна',    'fedorova@mirea.ru',  '+7-900-666-66-66', 'Канцелярия');

-- Начальные архивные запросы
INSERT INTO document_requests (user_id, document_title, description, category, status, priority, created_at, updated_at) VALUES
    (1, 'Приказ №142 от 12.03.1998',       'Приказ о реорганизации отдела',                    'Приказы',          'NEW',         'HIGH',     '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
    (2, 'Договор поставки №55/2005',        'Договор с ООО «Ресурс» на поставку оборудования',  'Договоры',         'IN_PROGRESS', 'MEDIUM',   '2026-09-02 11:30:00', '2026-09-03 09:00:00'),
    (1, 'Протокол собрания от 20.01.2010',  'Протокол общего собрания сотрудников',             'Протоколы',        'APPROVED',    'LOW',      '2026-09-03 14:00:00', '2026-09-04 16:00:00'),
    (3, 'Акт инвентаризации №78/2015',      'Инвентаризация основных средств',                  'Акты',             'COMPLETED',   'MEDIUM',   '2026-09-04 09:15:00', '2026-09-06 12:00:00'),
    (4, 'Личное дело сотрудника №1023',     'Архивное личное дело Смирнова А.В.',               'Личные дела',      'NEW',         'HIGH',     '2026-09-05 08:00:00', '2026-09-05 08:00:00'),
    (5, 'Научный отчет за 2020 год',        'Годовой отчет научного отдела',                    'Отчеты',           'IN_PROGRESS', 'CRITICAL', '2026-09-06 13:45:00', '2026-09-07 10:30:00'),
    (2, 'Справка о доходах №340',           'Справка 2-НДФЛ за 2019 год',                      'Справки',          'REJECTED',    'LOW',      '2026-09-07 15:20:00', '2026-09-08 11:00:00'),
    (6, 'Входящее письмо №892 от 05.06.2022', 'Письмо из Министерства образования',             'Корреспонденция',  'NEW',         'MEDIUM',   '2026-09-08 10:00:00', '2026-09-08 10:00:00'),
    (3, 'Платежное поручение №445',         'Оплата за аренду помещения',                       'Финансовые',       'CANCELLED',   'LOW',      '2026-09-09 16:30:00', '2026-09-10 09:00:00'),
    (5, 'Патент на изобретение №RU2345678', 'Патент на способ обработки данных',                'Патенты',          'APPROVED',    'HIGH',     '2026-09-10 11:00:00', '2026-09-11 14:00:00'),
    (4, 'Трудовой договор №567/2018',       'Договор с Кузнецовой Е.И.',                       'Договоры',         'COMPLETED',   'MEDIUM',   '2026-09-11 09:30:00', '2026-09-13 15:00:00'),
    (1, 'Устав организации (ред. 2021)',    'Действующая редакция устава',                      'Учредительные',    'IN_PROGRESS', 'CRITICAL', '2026-09-12 08:00:00', '2026-09-12 17:00:00');

-- Начальный администратор (хэш SHA-256 пароля admin123: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9)
INSERT INTO app_accounts (username, password_hash, full_name, email, role) VALUES
    ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Администратор Архива', 'admin@mirea.ru', 'ADMIN');

-- Начальные записи журнала аудита
INSERT INTO audit_logs (username, event, entity_type, details, created_at) VALUES
    ('system', 'SYSTEM_INIT', 'SYSTEM', 'Инициализация базы данных и таблиц архива', '2026-09-01 09:00:00'),
    ('admin', 'USER_CREATE', 'USER', 'Создан пользователь Иванов Иван Иванович', '2026-09-01 09:10:00'),
    ('admin', 'REQUEST_CREATE', 'REQUEST', 'Создан запрос на документ: Приказ №142 от 12.03.1998', '2026-09-01 10:00:00'),
    ('admin', 'LOGIN', 'AUTH', 'Вход администратора в систему', '2026-09-01 10:05:00'),
    ('admin', 'STATUS_CHANGE', 'REQUEST', 'Статус запроса #3 изменен на APPROVED', '2026-09-04 16:00:00');
