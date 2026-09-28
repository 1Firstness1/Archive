# Архив документов — Система запросов на архивные документы

Консольная информационная система для управления запросами на архивные документы.

## Требования

- **Java 17+**
- **Maven 3.8+**
- **PostgreSQL 14+**

## Настройка базы данных

1. Установите и запустите PostgreSQL.

2. Создайте базу данных:
   ```sql
   CREATE DATABASE archive_db;
   ```

3. Выполните SQL-скрипт инициализации:
   ```bash
   psql -U postgres -d archive_db -f sql/init.sql
   ```

   Скрипт создаст таблицы `users` и `document_requests`, а также заполнит их тестовыми данными.

4. При необходимости измените параметры подключения в файле:
   ```
   src/main/java/ru/mirea/archive/util/DatabaseManager.java
   ```
   По умолчанию:
   - URL: `jdbc:postgresql://localhost:5432/archive_db`
   - User: `postgres`
   - Password: `postgres`

## Сборка и запуск

### Способ 1: Прямо из IntelliJ IDEA (самый удобный)
1. Откройте класс [`src/main/java/ru/mirea/archive/Main.java`](file:///Users/artem/IdeaProjects/Archive/src/main/java/ru/mirea/archive/Main.java).
2. Нажмите зелёную стрелку **Run ▶** рядом с методом `public static void main(...)` (или сочетание `Ctrl+Shift+R` / `Shift+F10`).
3. Работайте с интерактивным меню прямо во встроенном терминале/консоли Run IntelliJ IDEA.

---

### Способ 2: Через Maven (exec:java)
Плагин `exec-maven-plugin` уже настроен в `pom.xml`, поэтому имя главного класса указывать вручную не нужно:
```bash
mvn exec:java
```

---

### Способ 3: Через готовый JAR-архив с зависимостями
```bash
# Сборка fat JAR со всеми зависимостями (PostgreSQL, Apache POI):
mvn clean package -DskipTests

# Запуск:
java -jar target/archive-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Структура проекта

```
src/main/java/ru/mirea/archive/
├── Main.java                          — Точка входа
├── model/
│   ├── User.java                      — Модель пользователя
│   ├── DocumentRequest.java           — Модель запроса на документ
│   ├── RequestStatus.java             — Перечисление статусов (enum)
│   └── Priority.java                  — Перечисление приоритетов (enum)
├── repository/
│   ├── CrudRepository.java            — Интерфейс репозитория (полиморфизм)
│   ├── UserRepository.java            — JDBC-репозиторий пользователей
│   └── DocumentRequestRepository.java — JDBC-репозиторий запросов
├── service/
│   ├── UserService.java               — Бизнес-логика пользователей
│   └── DocumentRequestService.java    — Бизнес-логика запросов
├── exception/
│   ├── BusinessException.java         — Нарушение бизнес-правил
│   ├── EntityNotFoundException.java   — Сущность не найдена
│   └── DatabaseException.java         — Ошибка базы данных
├── util/
│   ├── DatabaseManager.java           — Управление подключением к PostgreSQL
│   └── ExcelExporter.java             — Экспорт в Excel/CSV
└── ui/
    └── ConsoleUI.java                 — Консольный интерфейс
```

## Архитектура

```
Console UI  →  Service  →  Repository / JDBC  →  PostgreSQL
```

- **Console UI** — принимает ввод, отображает меню и данные
- **Service** — содержит бизнес-логику и валидацию
- **Repository** — выполняет SQL-запросы через JDBC
- **Database** — хранит данные, обеспечивает ограничения

## Бизнес-правила

1. Нельзя создать запрос без обязательного названия документа
2. Нельзя указать несуществующего пользователя
3. Нельзя установить некорректный статус
4. Нельзя выполнить запрещённый переход между статусами
5. Нельзя сохранить запрос без категории

### Допустимые переходы статусов

| Текущий статус | Допустимые переходы |
|---|---|
| Новый | В обработке, Отменён |
| В обработке | Одобрен, Отклонён, Отменён |
| Одобрен | Выполнен, Отменён |
| Отклонён | Новый |
| Выполнен | — |
| Отменён | Новый |

## Функциональность

- **CRUD** для пользователей и запросов
- **Поиск** по названию документа, описанию, пользователю, ФИО
- **Фильтрация** по статусу, приоритету, категории, диапазону дат
- **Сортировка** по дате, приоритету, названию (Stream API)
- **Статистика** — сводка по количеству записей, статусам, приоритетам, категориям
- **Экспорт** в Excel (.xlsx) и CSV (.csv)
