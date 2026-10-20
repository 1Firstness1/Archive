# Полный теоретический и архитектурный справочник проекта (Backend + JavaFX)

Данный документ представляет собой исчерпывающий справочник по архитектуре, используемым технологиям, паттернам проектирования и особенностям реализации проекта **«Архив документов»**. Документ разобран **пофайлово**: для каждого компонента описаны его назначение, внутренняя структура, теоретические концепции и нестандартные инженерные решения.

---

## Оглавление
1. [Базовая теория ООП и полиморфизма](#1-базовая-теория-ооп-и-полиморфизма)
2. [Общая архитектура системы](#2-общая-архитектура-системы)
3. [Разбор файлов серверной части (Backend)](#3-разбор-файлов-серверной-части-backend)
   - [Конфигурация и инфраструктура](#31-конфигурация-и-инфраструктура-backend)
   - [Точка входа и контроллеры](#32-точка-входа-и-контроллеры)
   - [Слой сервисов и бизнес-логики](#33-слой-сервисов-и-бизнес-логики)
   - [Слой репозиториев (Spring Data JPA)](#34-слой-репозиториев-spring-data-jpa)
   - [Сущности базы данных (ORM Entities)](#35-сущности-базы-данных-orm-entities)
   - [Объекты передачи данных (DTO)](#36-объекты-передачи-данных-dto)
4. [Разбор файлов клиентской части (JavaFX Desktop Client)](#4-разбор-файлов-клиентской-части-javafx-desktop-client)
   - [Модульная система и запуск](#41-модульная-система-и-запуск)
   - [Пользовательский интерфейс (FXML и CSS)](#42-пользовательский-интерфейс-fxml-и-css)
   - [Контроллеры представления](#43-контроллеры-представления)
   - [Сетевой слой и сервисы данных](#44-сетевой-слой-и-сервисы-данных)
   - [Репозитории прямого доступа к БД](#45-репозитории-прямого-доступа-к-бд)
   - [Модели данных клиента](#46-модели-данных-клиента)
   - [Утилиты (Экспорт, База данных)](#47-утилиты-экспорт-база-данных)
5. [Сводная таблица паттернов проектирования](#5-сводная-таблица-паттернов-проектирования)

---

## 1. Базовая теория ООП и полиморфизма

### 1.1. К какому типу ООП относятся Generics (Обобщения)
В теории объектно-ориентированного программирования и теории систем типов (классификация Луки Карделли и Питера Вегнера) **Generics (обобщения / дженерики)** относятся к **параметрическому полиморфизму (Parametric Polymorphism)**.

#### Четыре вида полиморфизма в ООП:
1. **Параметрический полиморфизм (Parametric Polymorphism) — ДЖЕНЕРИКИ:**
   * Позволяет писать код, алгоритмы и структуры данных безотносительно к конкретным типам данных. Тип передается в виде параметра (например, `List<T>`, `Optional<T>`, `TableView<Worker>`, `ResponseEntity<T>`).
   * **Главная цель в Java:** обеспечение **строгой типобезопасности во время компиляции (Compile-Time Type Safety)**. Дженерики избавляют от необходимости выполнять небезопасное ручное приведение типов `(Worker) obj` и предотвращают ошибки времени выполнения `ClassCastException`.
   * **Стирание типов (Type Erasure):** в целях обратной совместимости со старыми версиями Java (до Java 5), компилятор стирает информацию о типах дженериков при компиляции в байт-код, заменяя `T` на `Object` (или границу `extends Number`).
2. **Подтиповый полиморфизм (Subtyping / Inclusion Polymorphism):**
   * Классический полиморфизм наследования. Переменная базового класса или интерфейса может ссылаться на объект любого класса-потомка (`JpaRepository<Account, Long> repo = new AccountRepositoryImpl()`). Реализуется через динамическую диспетчеризацию и переопределение методов (`@Override`).
3. **Ad-hoc полиморфизм (Перегрузка / Overloading):**
   * Методы с одинаковым именем, но разными наборами и типами параметров внутри одного класса (например, конструкторы `Worker()` и `Worker(id, username, ...)`).
4. **Полиморфизм приведения типов (Coercion):**
   * Автоматическое неявное (Widening) или явное (Narrowing) преобразование типов в языке (например, приведение `int` к `double` или `Long` к `String.valueOf(...)`).

### 1.2. Парадигма MVC (Model-View-Controller)
Архитектурный шаблон, разделяющий приложение на три независимые роли:
* **Model (Модель):** Инкапсулирует состояние данных и бизнес-логику (классы `Account`, `Worker`, `DocumentRequest`). Не знает, как эти данные рисуются на экране.
* **View (Представление):** Отвечает за визуальное отображение (XML-файлы `main_view.fxml`, таблицы, стили `styles.css`).
* **Controller (Контроллер):** Принимает сигналы и события от пользователя (нажатия кнопок, ввод текста), вызывает методы моделей/сервисов и обновляет представление (`MainController`).

---

## 2. Общая архитектура системы

Система построена по принципу **распределенного клиент-серверного приложения**:

```
 ┌──────────────────────────────────────────────────────────────────┐
 │                        PostgreSQL 15                             │
 │      Таблицы: app_accounts, audit_logs, document_requests, users │
 └──────────────▲──────────────────────────────────▲────────────────┘
                │                                  │
    JDBC (Hibernate / Data JPA)           Direct JDBC Fallback
                │                                  │
 ┌──────────────┴─────────────────┐                │
 │    Spring Boot 3 REST API      │                │
 │       (Порт 8080 в Docker)     │                │
 └──────────────▲─────────────────┘                │
                │                                  │
          REST HTTP / JSON                         │
                │                                  │
 ┌──────────────┴──────────────────────────────────┴────────────────┐
 │                      JavaFX Desktop Client                       │
 │      GUI (FXML/CSS) + Асинхронный Heartbeat + HTTP Client        │
 └──────────────────────────────────────────────────────────────────┘
```

---

## 3. Разбор файлов серверной части (Backend)

### 3.1. Конфигурация и инфраструктура (Backend)

#### `backend/pom.xml`
* **Назначение:** Файл дескриптора проекта Maven для серверной части.
* **Структура:**
  - Родительский стартер `spring-boot-starter-parent` (версия 3.2.5) — задает версионирование зависимостей и плагинов.
  - Зависимости: `spring-boot-starter-web` (REST API, embedded Tomcat), `spring-boot-starter-data-jpa` (Hibernate, Spring Data), `postgresql` (JDBC драйвер для работы с СУБД).
* **Теория:**
  - **Управление зависимостями (Dependency Management):** транзитивное разрешение библиотек (Jackson, HikariCP, Hibernate Core подтягиваются автоматически).
  - **Сборка «Fat JAR» (Uber-JAR):** плагин `spring-boot-maven-plugin:repackage` упаковывает скомпилированный код вместе со всеми сторонними JAR-библиотеками и встроенным веб-сервером в один исполняемый файл.
* **Особенность:** Сборка таргетирована строго на **Java 21** (`<java.version>21</java.version>`), что позволяет использовать современные возможности платформы (виртуальные потоки, сопоставление с образцом, `record`, `Stream.toList()`).

#### `backend/Dockerfile`
* **Назначение:** Инструкция по сборке Docker-образа бэкенд-сервиса.
* **Структура:**
  ```dockerfile
  FROM eclipse-temurin:21-jre-alpine
  WORKDIR /app
  COPY target/*.jar app.jar
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
* **Теория:**
  - **Контейнеризация:** изоляция исполняемого процесса и его среды на уровне ядра ОС (Linux namespaces, cgroups).
  - **Базовый образ JRE vs JDK:** используется минималистичный образ `eclipse-temurin:21-jre-alpine` (только среда выполнения Java Runtime Environment без тяжелых инструментов компиляции), что уменьшает размер итогового образа в 4 раза (до ~160 МБ) и сокращает поверхность атак.
* **Особенность (Alpine DNS Workaround):**
  - Во многих корпоративных сетях и на macOS при попытке выполнить `RUN mvn package` прямо внутри Alpine Linux сборка зависает из-за сбоев Alpine DNS (особенности библиотеки musl при обращении к `repo.maven.apache.org`).
  - **Решение:** Проект собирается на хост-машине с использованием локального кэша `~/.m2` за 1-2 секунды, а Dockerfile лишь копирует готовый JAR.

#### `backend/docker-compose.yml`
* **Назначение:** Мультиконтейнерная декларативная оркестрация сервисов.
* **Структура:**
  - Сервис `postgres`: образ `postgres:15`, маппинг порта `5432:5432`, монтирование тома `postgres_data`, монтирование SQL-скрипта `./sql/init.sql:/docker-entrypoint-initdb.d/init.sql`.
  - Сервис `backend`: контекст сборки `.`, зависимость `depends_on: postgres`, маппинг порта `8080:8080`, проброс переменных окружения из файла `.env`.
* **Теория:**
  - **Service Discovery & Внутренний DNS Docker:** контейнеры в одной сети обращаются друг к другу по именам сервисов (`DB_HOST=postgres`).
  - **Volumes (Персистентность):** именованный том `postgres_data` сохраняет данные на физическом диске хоста между остановками и пересозданиями контейнеров.
  - **Автоинициализация Postgres:** файлы из `/docker-entrypoint-initdb.d/` автоматически выполняются контейнером PostgreSQL в алфавитном порядке строго при первом создании базы данных.

#### `backend/.env`
* **Назначение:** Хранение параметров среды и секретов конфигурации (12-Factor App methodology).
* **Структура:** Содержит переменные `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `INIT_ADMIN_LOGIN`, `INIT_ADMIN_PASSWORD`.
* **Теория:** Принцип «Конфигурация во внешней среде» позволяет менять параметры подключения к БД и реквизиты администратора без перекомпиляции исходного Java-кода.

#### `backend/sql/init.sql`
* **Назначение:** SQL-скрипт инициализации схемы БД и начальных записей.
* **Структура:**
  - Создание таблиц: `users` (заявители), `document_requests` (запросы документов), `app_accounts` (учетные записи работников), `audit_logs` (журнал аудита).
  - Первичные ключи (`SERIAL PRIMARY KEY`), внешние ключи (`REFERENCES users(id) ON DELETE CASCADE`), ограничения целостности (`CHECK (status IN (...))`).
* **Особенность:** Для учетной записи `admin` сохранен предрассчитанный хэш SHA-256 пароля `admin123`:  
  `240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9`.

#### `backend/src/main/resources/application.properties`
* **Назначение:** Основной конфигурационный файл фреймворка Spring Boot.
* **Структура:**
  - `spring.datasource.url`: динамическая интерполяция `${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:archive_db}`.
  - `spring.jpa.hibernate.ddl-auto=none`: запрещает Hibernate автоматически менять структуру таблиц, отдавая полный контроль проверенному скрипту `init.sql`.
  - `spring.jpa.show-sql=true`: логирование всех SQL-запросов Hibernate в консоль для отладки.

---

### 3.2. Точка входа и контроллеры

#### `BackendApplication.java`
* **Назначение:** Главный запускающий класс бэкенда.
* **Структура:**
  ```java
  @SpringBootApplication
  public class BackendApplication {
      public static void main(String[] args) {
          SpringApplication.run(BackendApplication.class, args);
      }
  }
  ```
* **Теория:**
  - Создает и запускает **ApplicationContext** — контейнер Spring IoC, который сканирует classpath, регистрирует бины и настраивает зависимости.
  - Запускает встроенный HTTP-сервер Tomcat на порту 8080.

#### `controller/AuthController.java`
* **Назначение:** REST-контроллер авторизации, аудита и управления работниками.
* **Структура и эндпоинты:**
  - `@RestController`, `@RequestMapping("/api")`, `@CrossOrigin(origins = "*")`.
  - `GET /api/health` — проверка доступности бэкенда и связи с БД.
  - `POST /api/auth/login` — аутентификация пользователя.
  - `POST /api/auth/register` — регистрация нового работника.
  - `POST /api/auth/change-password` — смена пароля текущего пользователя.
  - `GET /api/workers` (и `/api/accounts`) — получение списка всех работников (без паролей!).
  - `GET /api/audit-logs` и `POST /api/audit-logs` — чтение и добавление записей аудита.
* **Теория и паттерны:**
  - **Front Controller & MVC:** Контроллер не выполняет вычислений над базой, а делегирует работу в `AuthService`.
  - **HTTP Status Codes:** успешные операции возвращают `200 OK`, ошибки валидации — `400 Bad Request`, сбой базы данных в health check — `503 Service Unavailable`.
* **Особенность:**  
  Эндпоинт `/api/workers` выполняет безопасную проекцию сущностей `Account` в `WorkerDto` через `Stream API`:
  ```java
  List<WorkerDto> workers = accountRepository.findAll().stream()
      .map(acc -> new WorkerDto(acc.getId(), acc.getUsername(), acc.getFullName(), acc.getEmail(), acc.getRole(), acc.getCreatedAt()))
      .toList();
  ```
  Это гарантирует, что `password_hash` никогда не будет отправлен по сети клиенту.

---

### 3.3. Слой сервисов и бизнес-логики

#### `service/AuthService.java`
* **Назначение:** Ядро бизнес-логики авторизации, регистрации, хэширования паролей и аудита.
* **Структура:**
  - Поля: инъекция `AccountRepository`, `AuditLogRepository`, `@Value` для параметров администратора из `.env`.
  - Метод `login(AuthRequest request)`: проверка логина/пароля, генерация токена сессии.
  - Метод `register(RegisterRequest request)`: валидация уникальности логина/email, хэширование, сохранение аккаунта с ролью `USER`.
  - Метод `changePassword(ChangePasswordRequest request)`: сверка старого пароля и обновление хэша.
  - Метод `run(String... args)`: реализация интерфейса `CommandLineRunner`.
  - Метод `hashPassword(String rawPassword)`: SHA-256 хэширование через `MessageDigest`.
* **Теория и паттерны:**
  - **Service Layer Pattern:** изоляция бизнес-правил.
  - **Декларативные транзакции (`@Transactional`):** операции регистрации и смены пароля атомарны.
  - **Хэширование vs Шифрование:** используется односторонний алгоритм SHA-256 с переводом байт в шестнадцатеричную строку (Hex-представление через `HexFormat.of().formatHex(...)`).

---

### 3.4. Слой репозиториев (Spring Data JPA)

#### `repository/AccountRepository.java`
* **Назначение:** Интерфейс доступа к таблице `app_accounts`.
* **Структура:**
  ```java
  public interface AccountRepository extends JpaRepository<Account, Long> {
      Optional<Account> findByUsername(String username);
      boolean existsByUsername(String username);
      boolean existsByEmail(String email);
  }
  ```
* **Теория:**
  - **Паттерн Data Mapper / Repository:** скрывает детали SQL за интерфейсом Java.
  - **Query Derivation:** Spring Data анализирует имена методов и динамически создает SQL-запросы на основе ключевых слов (`findBy`, `existsBy`).
  - **`Optional<T>`:** Защита от `NullPointerException`. Заставляет вызывающий код явно обрабатывать случай отсутствия записи (`if (accountOpt.isEmpty())`).

#### `repository/AuditLogRepository.java`
* **Назначение:** Интерфейс доступа к таблице `audit_logs`.
* **Структура:** Содержит метод `List<AuditLog> findAllByOrderByCreatedAtDesc()`.
* **Теория:** Генерация сортировки на уровне SQL (`ORDER BY created_at DESC`), предотвращающая загрузку неотсортированных данных в оперативную память.

---

### 3.5. Сущности базы данных (ORM Entities)

#### `model/Account.java`
* **Назначение:** Сущность JPA, представляющая учетную запись работника в таблице `app_accounts`.
* **Структура:** Поля `id` (Long), `username` (String), `passwordHash` (String), `fullName` (String), `email` (String), `role` (String), `createdAt` (LocalDateTime).
* **Теория:**
  - Аннотации `@Entity`, `@Table(name = "app_accounts")`, `@Column(name = "password_hash")`.
  - Стратегия генерации ключа `GenerationType.IDENTITY` — делегирует автоинкремент столбцу `SERIAL` PostgreSQL.
* **Особенность:** Аннотация `@SuppressWarnings("JpaDataSourceORMInspection")` отключает ложные предупреждения IDE об отсутствии локальной схемы БД.

#### `model/AuditLog.java`
* **Назначение:** Сущность JPA для хранения истории событий в таблице `audit_logs`.
* **Структура:** Поля `id`, `username` (логин работника), `event` (`LOGIN`, `REGISTER`, `PASSWORD_CHANGE`, `STATUS_CHANGE`), `entityType` (`AUTH`, `REQUEST`), `details` (текстовое описание), `createdAt`.
* **Теория:** Аудит безопасности (Security Auditing) — фиксация критических действий пользователей с отметкой точного серверного времени.

---

### 3.6. Объекты передачи данных (DTO)

#### `dto/AuthRequest.java`
* **Назначение:** Тело запроса на авторизацию.
* **Структура:** Поля `username`, `password`, геттеры, сеттеры, пустой конструктор.
* **Особенность:** Добавлены методы `setLogin(String login)` и `getLogin()`. Это создает псевдоним (алиас) для поля `username`, благодаря чему парсер Jackson принимает и `{"username":"admin"}`, и `{"login":"admin"}` без сбоев.

#### `dto/RegisterRequest.java`
* **Назначение:** Тело запроса на регистрацию нового работника.
* **Структура:** Поля `username`, `password`, `fullName`, `email`, методы-алиасы для `login`.

#### `dto/ChangePasswordRequest.java`
* **Назначение:** Тело запроса на смену пароля.
* **Структура:** Поля `username`, `oldPassword`, `newPassword`.

#### `dto/WorkerDto.java`
* **Назначение:** DTO для отображения работников в таблице клиентов.
* **Структура:** Поля `id`, `username`, `fullName`, `email`, `role`, `createdAt`.
* **Особенность:** **Не содержит поля пароля**. Защита от раскрытия чувствительной информации (Information Disclosure Vulnerability).

#### `dto/AuthResponse.java`
* **Назначение:** Унифицированный ответ сервиса авторизации.
* **Структура:** Поля `success` (boolean), `message`, `username`, `fullName`, `email`, `role`, `token`. Статические фабричные методы `AuthResponse.success(...)` и `AuthResponse.error(...)`.
* **Паттерн:** **Static Factory Method** — упрощает создание объектов ответов и делает код сервиса более читаемым.

#### `dto/HealthResponse.java`
* **Назначение:** DTO ответа проверки работоспособности сервиса (`/api/health`).
* **Структура:** Поля `status` (`UP`/`DOWN`), `service` (`archive-backend`), `database` (`CONNECTED`/`DISCONNECTED`), `message`, `timestamp`.
* **Особенность:** Поле `service = "archive-backend"` служит цифровой сигнатурой нашего бэкенда, защищая JavaFX-клиент от ложных подключений к чужим серверам на порту 8080.

---

## 4. Разбор файлов клиентской части (JavaFX Desktop Client)

### 4.1. Модульная система и запуск

#### `javafx-client/pom.xml`
* **Назначение:** Конфигурация Maven для сборки десктопного JavaFX-приложения.
* **Структура:** Зависимости: `javafx-controls`, `javafx-fxml`, `postgresql` (JDBC драйвер), `poi-ooxml` (экспорт в XLSX). Плагин `javafx-maven-plugin` с указанием главного класса `ru.mirea.archive.MainApp`.

#### `javafx-client/src/main/java/module-info.java`
* **Назначение:** Дескриптор модульной системы Java (Project Jigsaw, Java 9+).
* **Структура:**
  - `requires javafx.controls;`, `requires javafx.fxml;`, `requires java.sql;`, `requires java.net.http;`, `requires org.apache.poi.ooxml;`.
  - Директивы `opens ... to javafx.fxml`: открывают пакеты с контроллерами для рефлексивного доступа загрузчику `FXMLLoader`.
* **Теория:** **Инкапсуляция на уровне модулей.** Без явного `opens` рефлексивный доступ JavaFX к приватным полям `@FXML` будет заблокирован `IllegalAccessException`.

#### `MainApp.java`
* **Назначение:** Главный класс запуска настольного приложения JavaFX.
* **Структура:**
  - Наследует `javafx.application.Application`.
  - Метод `start(Stage primaryStage)`: загружает `main_view.fxml`, подключает `styles.css`, настраивает заголовок, минимальные размеры (950x650) и отображает окно.
* **Теория:** **JavaFX Application Thread** — однопоточная модель пользовательского интерфейса (UI Toolkit Single-Thread Rule). Все изменения графических компонентов должны происходить строго в главном потоке отрисовки.

---

### 4.2. Пользовательский интерфейс (FXML и CSS)

#### `main_view.fxml`
* **Назначение:** Декларативная разметка главного окна приложения.
* **Структура:**
  - Корневой контейнер `BorderPane`:
    - `top`: Верхняя панель управления (`HBox` с заголовком, статусом сети `serverStatusBadge`, кнопкой смены пароля, кнопкой выхода, экспортом) + баннер ошибки `offlineBanner`.
    - `center`: Контейнер `StackPane`, накладывающий экран входа `authContainer` и рабочие вкладки `tabPane`.
    - `tabPane` содержит 4 вкладки:
      1. **Запросы на документы** (поиск, фильтрация по статусу/приоритету, таблица `requestTable`).
      2. **Пользователи** (заявители архива: ФИО, email, телефон, отдел, таблица `userTable`).
      3. **Работники** (учетные записи сотрудников: логин, ФИО, email, роль, таблица `workerTable`).
      4. **Аудит логов** (история действий работников, таблица `auditTable`).
    - `bottom`: Статусная строка (`HBox` со `statusLabel` и счетчиком записей `countLabel`).

#### `styles.css`
* **Назначение:** Стилизация графического интерфейса в темной палитре (Catppuccin Mocha).
* **Теория:** Использование селекторов классов (`.button`, `.table-view`, `.status-badge-online`, `.offline-banner`) с кастомными свойствами CSS (`-fx-background-color`, `-fx-text-fill`, `-fx-border-radius`).

---

### 4.3. Контроллеры представления

#### `controller/MainController.java`
* **Назначение:** Главный контроллер, управляющий всей логикой интерфейса программы.
* **Структура (более 1200 строк кода):**
  - Привязка компонентов `@FXML`: таблицы, колонки, поля поиска, комбобоксы, метки статуса.
  - Метод `initialize()`: инициализирует таблицы, выпадающие списки, регистрирует слушатели смены вкладок, запускает Heartbeat.
  - Метод `checkSystemHealth()`: опрашивает `AuthClientService` и возвращает статус доступности.
  - Метод `setOnlineState(boolean online)`: динамически блокирует или активирует кнопки и вкладки при потере/восстановлении связи.
  - Методы авторизации: `onInlineLogin()`, `onInlineRegister()`, `onLogout()`, `onChangePasswordDialog()`.
  - Методы управления запросами: `onAddRequest()`, `onEditRequest()`, `onDeleteRequest()`, `onSearchRequests()`, `onExportCsv()`, `onExportXlsx()`.
  - Методы управления работниками: `setupWorkerTable()`, `loadWorkers()`, `onSearchWorkers()`, `onRefreshWorkers()`.
  - Методы управления аудитом: `setupAuditTable()`, `loadAuditLogs()`, `onSearchAudit()`, `onRefreshAudit()`.
* **Теория и ключевые особенности:**
  - **Асинхронный Heartbeat:** Запущен `Timeline` с вызовом каждые 4 секунды. Он проверяет связь и обновляет UI прямо на лету без зависания интерфейса.
  - **Observer (Наблюдатель):** Данные хранятся в `ObservableList<T>`. Таблицы автоматически подхватывают добавление и удаление записей.
  - **Кастомный CellFactory:** Колонка роли работника (`workerRoleCol`) динамически генерирует цветные графические бейджи для ролей `ADMIN` и `USER`.

#### `controller/AuthDialogController.java`
* **Назначение:** Контроллер модального диалога входа и регистрации (использовался на раннем этапе до встраивания формы в главное окно; сохранен для обратной совместимости).

---

### 4.4. Сетевой слой и сервисы данных

#### `service/AuthClientService.java`
* **Назначение:** Клиентский сервис для взаимодействия со Spring Boot REST API по протоколу HTTP и резервного доступа к БД.
* **Структура:**
  - Поля: `baseUrl` (по умолчанию `http://localhost:8080`), `HttpClient` (настроен с таймаутом соединения).
  - Внутренний статический класс `SystemHealth`: хранит детальные флаги доступности компонентов (`backendAvailable`, `databaseAvailable`, `fullyOnline`, текстовые сообщения).
  - Метод `checkSystemHealth()`: выполняет запрос к `/api/health`, валидирует ответ, параллельно проверяет доступность БД через `DatabaseManager.testConnection()`.
  - Методы REST: `login()`, `register()`, `changePassword()`.
  - Метод `fetchWorkers()`: запрашивает `/api/workers`, при сбое сети вызывает резервный `fetchWorkersFromDb()`.
  - Метод `fetchAuditLogs()`: запрашивает `/api/audit-logs`, при сбое вызывает `fetchAuditLogsFromDb()`.
  - Метод `logAuditEvent()`: отправляет событие на бэкенд, а при сбое сети пишет напрямую в PostgreSQL через JDBC `INSERT INTO audit_logs ...`.
* **Теория и паттерны:**
  - **Graceful Degradation / Fallback Pattern:** Архитектурная устойчивость к сбоям. Приложение сохраняет базовую функциональность просмотра работников и аудита даже при отказе бэкенд-контейнера.
  - **JSON парсинг без внешних библиотек:** Реализованы легковесные утилитные парсеры `extractString()`, `extractLong()`, `extractBoolean()`, что позволило клиенту не зависеть от сторонних тяжелых библиотек Jackson в classpath клиента.

#### `service/DocumentRequestService.java`
* **Назначение:** Бизнес-логика работы с архивными запросами документов (валидация статусов, дат, связей с заявителями).
* **Теория:** Валидация инвариантов бизнес-модели перед передачей в репозиторий.

#### `service/UserService.java`
* **Назначение:** Бизнес-логика управления заявителями архива (проверка уникальности email, формата телефона).

---

### 4.5. Репозитории прямого доступа к БД (JavaFX)

#### `repository/DocumentRequestRepository.java`
* **Назначение:** Прямой JDBC-доступ к таблице `document_requests`.
* **Теория:** Использование `PreparedStatement` с параметризованными плейсхолдерами `?`, защищающими от атак **SQL Injection**.

#### `repository/UserRepository.java`
* **Назначение:** Прямой JDBC-доступ к таблице `users` (клиенты архива).

---

### 4.6. Модели данных клиента

#### `model/Worker.java`
* **Назначение:** Модель работника архива для отображения в таблице (ID, логин, ФИО, email, роль, дата регистрации).
* **Особенность:** Полностью лишена поля пароля.

#### `model/AuditLogRecord.java`
* **Назначение:** Модель записи журнала аудита. Содержит методы-алиасы `getUsername()` и `getWorker()`.

#### `model/AuthSession.java`
* **Назначение:** Потокобезопасное хранение сессии вошедшего сотрудника.
* **Паттерн:** **Singleton (Одиночка):**
  ```java
  public class AuthSession {
      private static AuthSession instance;
      private AuthSession() {}
      public static synchronized AuthSession getInstance() {
          if (instance == null) instance = new AuthSession();
          return instance;
      }
      // ...
  }
  ```

#### `model/DocumentRequest.java` и `model/User.java`
* **Назначение:** Модели архивного документа и заявителя.

#### `model/Priority.java` и `model/RequestStatus.java`
* **Назначение:** Перечисления (Java `enum`) со строгой типизацией статусов (`NEW`, `IN_PROGRESS`, `APPROVED`, `REJECTED`, `COMPLETED`, `CANCELLED`) и приоритетов (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) с человекочитаемыми именами `getDisplayName()`.

---

### 4.7. Утилиты (Экспорт, База данных)

#### `util/DatabaseManager.java`
* **Назначение:** Управление пулом прямых JDBC-подключений клиента.
* **Структура:** Статический блок инициализации читает `db.properties`, метод `getConnection()`, метод `testConnection()`.
* **Теория:**
  - Идиома **`try-with-resources`** автоматически вызывает метод `close()` у соединений, statements и result sets.
* **Особенность:** Метод `testConnection()` выполняет проверочный запрос `SELECT 1 FROM document_requests LIMIT 1`, гарантируя проверку именно нашей архивной схемы.

#### `util/ExcelExporter.java`
* **Назначение:** Экспорт таблиц в бинарный формат Excel (OpenXML, `.xlsx`).
* **Теория:** Использование библиотеки Apache POI (`XSSFWorkbook`, `XSSFSheet`, `XSSFRow`, `XSSFCell`). Стилизация шапок таблиц, автоподбор ширины колонок (`autoSizeColumn`).

---

## 5. Сводная таблица паттернов проектирования

| Паттерн | Где реализован | Решаемая задача |
|---|---|---|
| **MVC** | `main_view.fxml` + `MainController` + `Worker`/`DocumentRequest` | Разделение визуального интерфейса и прикладной логики |
| **DTO (Data Transfer Object)** | `WorkerDto`, `AuthRequest`, `HealthResponse` | Безопасная передача данных по HTTP, изоляция схемы БД, сокрытие паролей |
| **Singleton (Одиночка)** | `AuthSession` | Единая точка хранения текущей авторизованной сессии в клиенте |
| **Repository / DAO** | `AccountRepository`, `DocumentRequestRepository` | Изоляция операций с базой данных от бизнес-логики |
| **Observer (Наблюдатель)** | `ObservableList`, `selectedItemProperty` | Автоматическая перерисовка UI при изменении данных в таблицах |
| **Factory / Delegate** | `setCellFactory` в `MainController` | Динамическое порождение визуальных узлов (цветных бейджей статусов и ролей) |
| **Fallback (Резервирование)** | `AuthClientService` | Автоматическое переключение на прямой JDBC при недоступности REST API |
| **Inversion of Control (IoC) / DI** | Spring Boot (`@RestController`, `@Service`, `@Repository`) | Инверсия управления и внедрение зависимостей через конструкторы |
| **Static Factory Method** | `AuthResponse.success(...)`, `AuthResponse.error(...)` | Удобная и выразительная инициализация типовых объектов ответов |
| **Parametric Polymorphism** | Generics в Java: `List<T>`, `TableView<S>`, `Optional<T>` | Строгая типобезопасность структур данных на этапе компиляции |
