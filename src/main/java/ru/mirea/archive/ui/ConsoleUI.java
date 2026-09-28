package ru.mirea.archive.ui;

import ru.mirea.archive.exception.BusinessException;
import ru.mirea.archive.exception.DatabaseException;
import ru.mirea.archive.exception.EntityNotFoundException;
import ru.mirea.archive.model.DocumentRequest;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.RequestStatus;
import ru.mirea.archive.model.User;
import ru.mirea.archive.service.DocumentRequestService;
import ru.mirea.archive.service.UserService;
import ru.mirea.archive.util.ExcelExporter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private final Scanner scanner;
    private final UserService userService;
    private final DocumentRequestService requestService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ConsoleUI(UserService userService, DocumentRequestService requestService) {
        this.scanner = new Scanner(System.in);
        this.userService = userService;
        this.requestService = requestService;
    }

    public void start() {
        while (true) {
            printMainMenu();
            int choice = readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> usersMenu();
                    case 2 -> requestsMenu();
                    case 3 -> searchMenu();
                    case 4 -> filterMenu();
                    case 5 -> statisticsMenu();
                    case 6 -> exportMenu();
                    case 7 -> showDatabaseTables();
                    case 0 -> {
                        System.out.println("\nРабота с программой завершена. До свидания!");
                        return;
                    }
                    default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
                }
            } catch (DatabaseException e) {
                System.out.println("\n[ Ошибка БД ] " + e.getMessage() + "\n");
            } catch (Exception e) {
                System.out.println("\n[ Непредвиденная ошибка ] " + e.getMessage() + "\n");
            }
        }
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("            АРХИВ ДОКУМЕНТОВ — СИСТЕМА ЗАПРОСОВ             ");
        System.out.println("============================================================");
        System.out.println("  1. Пользователи");
        System.out.println("  2. Запросы на документы");
        System.out.println("  3. Поиск");
        System.out.println("  4. Фильтрация");
        System.out.println("  5. Статистика");
        System.out.println("  6. Экспорт данных");
        System.out.println("  7. Вывести таблицы базы данных");
        System.out.println("  0. Выход");
        System.out.println("============================================================");
    }

    private void usersMenu() {
        while (true) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("                        ПОЛЬЗОВАТЕЛИ                        ");
            System.out.println("============================================================");
            System.out.println("  1. Список всех пользователей");
            System.out.println("  2. Найти пользователя по ID");
            System.out.println("  3. Добавить пользователя");
            System.out.println("  4. Редактировать пользователя");
            System.out.println("  5. Удалить пользователя");
            System.out.println("  0. Назад");
            System.out.println("============================================================");

            int choice = readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> listUsers();
                    case 2 -> findUserById();
                    case 3 -> createUser();
                    case 4 -> updateUser();
                    case 5 -> deleteUser();
                    case 0 -> { return; }
                    default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("\n[ Внимание ] " + e.getMessage() + "\n");
            } catch (DatabaseException e) {
                System.out.println("\n[ Ошибка БД ] " + e.getMessage() + "\n");
            }
        }
    }

    private void listUsers() {
        List<User> users = userService.findAll();
        if (users.isEmpty()) {
            System.out.println("\nСписок пользователей пуст.");
            return;
        }
        System.out.println();
        System.out.println(User.tableSeparator());
        System.out.println(User.tableHeader());
        System.out.println(User.tableSeparator());
        for (User u : users) {
            System.out.println(u);
        }
        System.out.println(User.tableSeparator());
        System.out.printf("Всего пользователей: %d%n", users.size());
    }

    private void findUserById() {
        int id = readInt("Введите ID пользователя: ");
        User user = userService.findById(id);
        System.out.println();
        System.out.println(User.tableSeparator());
        System.out.println(User.tableHeader());
        System.out.println(User.tableSeparator());
        System.out.println(user);
        System.out.println(User.tableSeparator());
    }

    private void createUser() {
        System.out.println("\n--- Добавление пользователя ---");
        String fullName = readLine("ФИО: ");
        String email = readLine("Email: ");
        String phone = readLine("Телефон (Enter — пропустить): ");
        String department = readLine("Отдел (Enter — пропустить): ");

        User user = userService.create(fullName, email,
                phone.isBlank() ? null : phone,
                department.isBlank() ? null : department);
        System.out.println("\n[ Успешно ] Пользователь создан с ID: " + user.getId());
    }

    private void updateUser() {
        int id = readInt("Введите ID пользователя для редактирования: ");
        User existing = userService.findById(id);
        System.out.println("Текущие данные:");
        System.out.println(User.tableSeparator());
        System.out.println(User.tableHeader());
        System.out.println(User.tableSeparator());
        System.out.println(existing);
        System.out.println(User.tableSeparator());

        String fullName = readLine("Новое ФИО [" + existing.getFullName() + "]: ");
        String email = readLine("Новый Email [" + existing.getEmail() + "]: ");
        String phone = readLine("Новый телефон [" + (existing.getPhone() != null ? existing.getPhone() : "") + "]: ");
        String department = readLine("Новый отдел [" + (existing.getDepartment() != null ? existing.getDepartment() : "") + "]: ");

        userService.update(id,
                fullName.isBlank() ? existing.getFullName() : fullName,
                email.isBlank() ? existing.getEmail() : email,
                phone.isBlank() ? existing.getPhone() : phone,
                department.isBlank() ? existing.getDepartment() : department);
        System.out.println("\n[ Успешно ] Данные пользователя обновлены.");
    }

    private void deleteUser() {
        int id = readInt("Введите ID пользователя для удаления: ");
        User user = userService.findById(id);
        System.out.println("Удалить пользователя: " + user.getFullName() + "?");
        String confirm = readLine("Введите 'да' для подтверждения: ");
        if ("да".equalsIgnoreCase(confirm.trim())) {
            userService.delete(id);
            System.out.println("\n[ Успешно ] Пользователь удалён.");
        } else {
            System.out.println("\nУдаление отменено.");
        }
    }

    private void requestsMenu() {
        while (true) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("                   ЗАПРОСЫ НА ДОКУМЕНТЫ                     ");
            System.out.println("============================================================");
            System.out.println("  1. Список всех запросов");
            System.out.println("  2. Просмотр запроса по ID");
            System.out.println("  3. Создать запрос");
            System.out.println("  4. Редактировать запрос");
            System.out.println("  5. Изменить статус запроса");
            System.out.println("  6. Удалить запрос");
            System.out.println("  7. Сортировка запросов");
            System.out.println("  0. Назад");
            System.out.println("============================================================");

            int choice = readInt("Выберите действие: ");
            try {
                switch (choice) {
                    case 1 -> listRequests();
                    case 2 -> findRequestById();
                    case 3 -> createRequest();
                    case 4 -> updateRequest();
                    case 5 -> changeRequestStatus();
                    case 6 -> deleteRequest();
                    case 7 -> sortMenu();
                    case 0 -> { return; }
                    default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("\n[ Внимание ] " + e.getMessage() + "\n");
            } catch (DatabaseException e) {
                System.out.println("\n[ Ошибка БД ] " + e.getMessage() + "\n");
            }
        }
    }

    private void listRequests() {
        List<DocumentRequest> requests = requestService.findAll();
        printRequestTable(requests);
    }

    private void findRequestById() {
        int id = readInt("Введите ID запроса: ");
        DocumentRequest req = requestService.findById(id);
        System.out.println();
        System.out.println(req.toDetailedString());
    }

    private void createRequest() {
        System.out.println("\n--- Создание запроса на документ ---");
        listUsers();

        int userId = readInt("ID пользователя: ");
        String title = readLine("Название документа: ");
        String description = readLine("Описание (Enter — пропустить): ");
        String category = readLine("Категория: ");

        System.out.println("Приоритеты: LOW (Низкий), MEDIUM (Средний), HIGH (Высокий), CRITICAL (Критический)");
        String priorityStr = readLine("Приоритет [MEDIUM]: ");
        Priority priority;
        try {
            priority = priorityStr.isBlank() ? Priority.MEDIUM : Priority.fromString(priorityStr.trim());
        } catch (IllegalArgumentException e) {
            System.out.println("[ Внимание ] " + e.getMessage() + " Установлен MEDIUM.");
            priority = Priority.MEDIUM;
        }

        DocumentRequest req = requestService.create(userId, title,
                description.isBlank() ? null : description, category, priority);
        System.out.println("\n[ Успешно ] Запрос создан с ID: " + req.getId());
    }

    private void updateRequest() {
        int id = readInt("Введите ID запроса для редактирования: ");
        DocumentRequest existing = requestService.findById(id);
        System.out.println();
        System.out.println(existing.toDetailedString());

        int userId = readIntOrDefault("Новый ID пользователя [" + existing.getUserId() + "]: ", existing.getUserId());
        String title = readLine("Новое название [" + existing.getDocumentTitle() + "]: ");
        String description = readLine("Новое описание [" + (existing.getDescription() != null ? existing.getDescription() : "") + "]: ");
        String category = readLine("Новая категория [" + existing.getCategory() + "]: ");

        System.out.println("Приоритеты: LOW, MEDIUM, HIGH, CRITICAL");
        String priorityStr = readLine("Новый приоритет [" + existing.getPriority().name() + "]: ");
        Priority priority;
        try {
            priority = priorityStr.isBlank() ? existing.getPriority() : Priority.fromString(priorityStr.trim());
        } catch (IllegalArgumentException e) {
            System.out.println("[ Внимание ] " + e.getMessage() + " Оставлен прежний.");
            priority = existing.getPriority();
        }

        requestService.update(id,
                userId,
                title.isBlank() ? existing.getDocumentTitle() : title,
                description.isBlank() ? existing.getDescription() : description,
                category.isBlank() ? existing.getCategory() : category,
                priority);
        System.out.println("\n[ Успешно ] Запрос обновлён.");
    }

    private void changeRequestStatus() {
        int id = readInt("Введите ID запроса: ");
        DocumentRequest req = requestService.findById(id);
        System.out.println("Текущий статус: " + req.getStatus().getDisplayName());

        System.out.println("Доступные статусы:");
        for (RequestStatus s : RequestStatus.values()) {
            System.out.println("  - " + s.name() + " (" + s.getDisplayName() + ")");
        }

        String statusStr = readLine("Новый статус: ");
        try {
            RequestStatus newStatus = RequestStatus.fromString(statusStr.trim());
            requestService.changeStatus(id, newStatus);
            System.out.println("\n[ Успешно ] Статус изменён на: " + newStatus.getDisplayName());
        } catch (IllegalArgumentException e) {
            System.out.println("[ Внимание ] " + e.getMessage());
        }
    }

    private void deleteRequest() {
        int id = readInt("Введите ID запроса для удаления: ");
        DocumentRequest req = requestService.findById(id);
        System.out.println("Удалить запрос: " + req.getDocumentTitle() + "?");
        String confirm = readLine("Введите 'да' для подтверждения: ");
        if ("да".equalsIgnoreCase(confirm.trim())) {
            requestService.delete(id);
            System.out.println("\n[ Успешно ] Запрос удалён.");
        } else {
            System.out.println("\nУдаление отменено.");
        }
    }

    private void sortMenu() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("                    СОРТИРОВКА ЗАПРОСОВ                     ");
        System.out.println("============================================================");
        System.out.println("  1. По дате (от новых к старым)");
        System.out.println("  2. По приоритету (от критического к низкому)");
        System.out.println("  3. По названию документа (А-Я)");
        System.out.println("  0. Назад");
        System.out.println("============================================================");

        int choice = readInt("Выберите: ");
        List<DocumentRequest> all = requestService.findAll();
        switch (choice) {
            case 1 -> printRequestTable(requestService.sortByDateDesc(all));
            case 2 -> printRequestTable(requestService.sortByPriorityDesc(all));
            case 3 -> printRequestTable(requestService.sortByTitle(all));
            case 0 -> {}
            default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
        }
    }

    private void searchMenu() {
        while (true) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("                           ПОИСК                            ");
            System.out.println("============================================================");
            System.out.println("  1. Поиск запроса по названию документа");
            System.out.println("  2. Поиск запроса по описанию");
            System.out.println("  3. Поиск запросов по пользователю");
            System.out.println("  4. Поиск пользователя по ФИО");
            System.out.println("  0. Назад");
            System.out.println("============================================================");

            int choice = readInt("Выберите: ");
            try {
                switch (choice) {
                    case 1 -> {
                        String query = readLine("Название документа: ");
                        printRequestTable(requestService.searchByTitle(query));
                    }
                    case 2 -> {
                        String query = readLine("Ключевое слово в описании: ");
                        printRequestTable(requestService.searchByDescription(query));
                    }
                    case 3 -> {
                        int userId = readInt("ID пользователя: ");
                        printRequestTable(requestService.findByUserId(userId));
                    }
                    case 4 -> {
                        String name = readLine("ФИО (частичное): ");
                        List<User> users = userService.searchByName(name);
                        if (users.isEmpty()) {
                            System.out.println("\nПользователи не найдены.");
                        } else {
                            System.out.println();
                            System.out.println(User.tableSeparator());
                            System.out.println(User.tableHeader());
                            System.out.println(User.tableSeparator());
                            for (User u : users) {
                                System.out.println(u);
                            }
                            System.out.println(User.tableSeparator());
                            System.out.printf("Всего найдено: %d%n", users.size());
                        }
                    }
                    case 0 -> { return; }
                    default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("\n[ Внимание ] " + e.getMessage() + "\n");
            }
        }
    }

    private void filterMenu() {
        while (true) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("                        ФИЛЬТРАЦИЯ                          ");
            System.out.println("============================================================");
            System.out.println("  1. По статусу");
            System.out.println("  2. По приоритету");
            System.out.println("  3. По категории");
            System.out.println("  4. По диапазону дат создания");
            System.out.println("  0. Назад");
            System.out.println("============================================================");

            int choice = readInt("Выберите: ");
            try {
                switch (choice) {
                    case 1 -> {
                        System.out.println("Доступные статусы:");
                        for (RequestStatus s : RequestStatus.values()) {
                            System.out.println("  - " + s.name() + " (" + s.getDisplayName() + ")");
                        }
                        String statusStr = readLine("Введите статус: ");
                        RequestStatus status = RequestStatus.fromString(statusStr.trim());
                        printRequestTable(requestService.filterByStatus(status));
                    }
                    case 2 -> {
                        System.out.println("Доступные приоритеты:");
                        for (Priority p : Priority.values()) {
                            System.out.println("  - " + p.name() + " (" + p.getDisplayName() + ")");
                        }
                        String prStr = readLine("Введите приоритет: ");
                        Priority pr = Priority.fromString(prStr.trim());
                        printRequestTable(requestService.filterByPriority(pr));
                    }
                    case 3 -> {
                        List<String> categories = requestService.findAllCategories();
                        System.out.println("Доступные категории:");
                        for (String c : categories) {
                            System.out.println("  - " + c);
                        }
                        String cat = readLine("Введите категорию: ");
                        printRequestTable(requestService.filterByCategory(cat));
                    }
                    case 4 -> {
                        System.out.println("Введите даты в формате дд.мм.гггг");
                        LocalDate from = readDate("Дата с: ");
                        LocalDate to = readDate("Дата по: ");
                        if (from != null && to != null) {
                            printRequestTable(requestService.filterByDateRange(from, to));
                        }
                    }
                    case 0 -> { return; }
                    default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
                }
            } catch (IllegalArgumentException | BusinessException e) {
                System.out.println("\n[ Внимание ] " + e.getMessage() + "\n");
            }
        }
    }

    private void statisticsMenu() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("                    СТАТИСТИКА СИСТЕМЫ                      ");
        System.out.println("============================================================");
        System.out.printf("  Всего пользователей:            %d%n", userService.count());
        System.out.printf("  Всего запросов:                 %d%n", requestService.count());
        System.out.println("------------------------------------------------------------");
        System.out.println("  По статусам:");
        for (RequestStatus s : RequestStatus.values()) {
            System.out.printf("    %-28s  %d%n", s.getDisplayName() + ":", requestService.countByStatus(s));
        }
        System.out.println("------------------------------------------------------------");
        System.out.println("  По приоритетам:");
        for (Priority p : Priority.values()) {
            System.out.printf("    %-28s  %d%n", p.getDisplayName() + ":", requestService.countByPriority(p));
        }
        System.out.println("------------------------------------------------------------");
        System.out.printf("  Уникальных категорий:           %d%n", requestService.countCategories());
        System.out.println("============================================================");
    }

    private void exportMenu() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("                       ЭКСПОРТ ДАННЫХ                       ");
        System.out.println("============================================================");
        System.out.println("  1. Экспорт в Excel (.xlsx)");
        System.out.println("  2. Экспорт в CSV (.csv)");
        System.out.println("  0. Назад");
        System.out.println("============================================================");

        int choice = readInt("Выберите: ");
        try {
            switch (choice) {
                case 1 -> {
                    String path = readLine("Путь к файлу [export.xlsx]: ");
                    if (path.isBlank()) path = "export.xlsx";
                    List<User> users = userService.findAll();
                    List<DocumentRequest> requests = requestService.findAll();
                    ExcelExporter.exportToExcel(users, requests, path);
                    System.out.println("\n[ Успешно ] Данные экспортированы в: " + path);
                }
                case 2 -> {
                    String path = readLine("Путь к файлу [export.csv]: ");
                    if (path.isBlank()) path = "export.csv";
                    List<DocumentRequest> requests = requestService.findAll();
                    ExcelExporter.exportToCsv(requests, path);
                    System.out.println("\n[ Успешно ] Данные экспортированы в: " + path);
                }
                case 0 -> {}
                default -> System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
            }
        } catch (IOException e) {
            System.out.println("\n[ Ошибка экспорта ] " + e.getMessage());
        }
    }

    private void showDatabaseTables() {
        System.out.println("\n================ ТАБЛИЦА: ПОЛЬЗОВАТЕЛИ ================");
        listUsers();
        System.out.println("\n=========== ТАБЛИЦА: ЗАПРОСЫ НА ДОКУМЕНТЫ ============");
        listRequests();
    }

    private void printRequestTable(List<DocumentRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("\nЗаписи не найдены.");
            return;
        }
        System.out.println();
        System.out.println(DocumentRequest.tableSeparator());
        System.out.println(DocumentRequest.tableHeader());
        System.out.println(DocumentRequest.tableSeparator());
        for (DocumentRequest r : requests) {
            System.out.println(r);
        }
        System.out.println(DocumentRequest.tableSeparator());
        System.out.printf("Всего записей: %d%n", requests.size());
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[ Ошибка ] Значение должно быть целым числом.\n");
            }
        }
    }

    private int readIntOrDefault(String prompt, int defaultValue) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("[ Внимание ] Некорректное число. Используется значение: " + defaultValue);
            return defaultValue;
        }
    }

    private LocalDate readDate(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return LocalDate.parse(input, DATE_FMT);
        } catch (DateTimeParseException e) {
            System.out.println("[ Ошибка ] Некорректный формат даты. Используйте дд.мм.гггг");
            return null;
        }
    }
}
