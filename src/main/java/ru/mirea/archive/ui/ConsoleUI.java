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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private final Scanner scanner;
    private final UserService userService;
    private final DocumentRequestService requestService;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter FILE_TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public ConsoleUI(UserService userService, DocumentRequestService requestService) {
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
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
        String fullName = readNonEmptyLine("ФИО: ", "ФИО не может быть пустым.");
        String email = readNonEmptyLine("Email: ", "Email не может быть пустым.");
        String phone = readLine("Телефон (Enter — пропустить): ");
        String department = readLine("Отдел (Enter — пропустить): ");

        User user = userService.create(fullName, email, phone, department);
        System.out.println("\n[ Успешно ] Пользователь создан с ID: " + user.getId());
    }

    private void updateUser() {
        int id = readInt("Введите ID пользователя для редактирования: ");
        User existing = userService.findById(id);
        System.out.println("\nТекущие данные:");
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
                fullName.isEmpty() ? existing.getFullName() : fullName,
                email.isEmpty() ? existing.getEmail() : email,
                phone.isEmpty() ? existing.getPhone() : phone,
                department.isEmpty() ? existing.getDepartment() : department);
        System.out.println("\n[ Успешно ] Данные пользователя обновлены.");
    }

    private void deleteUser() {
        int id = readInt("Введите ID пользователя для удаления: ");
        User user = userService.findById(id);
        System.out.println("Удалить пользователя: " + user.getFullName() + "?");
        String confirm = readLine("Введите 'да' для подтверждения: ");
        if ("да".equalsIgnoreCase(confirm)) {
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
        String title = readNonEmptyLine("Название документа: ", "Название документа не может быть пустым.");
        String description = readLine("Описание (Enter — пропустить): ");
        String category = chooseCategory("Категория документа:", null);
        Priority priority = choosePriority("Приоритет запроса:", Priority.MEDIUM);

        DocumentRequest req = requestService.create(userId, title, description, category, priority);
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
        String category = chooseCategory("Новая категория [" + existing.getCategory() + "]:", existing.getCategory());
        Priority priority = choosePriority("Новый приоритет [" + existing.getPriority().getDisplayName() + "]:", existing.getPriority());

        requestService.update(id,
                userId,
                title.isEmpty() ? existing.getDocumentTitle() : title,
                description.isEmpty() ? existing.getDescription() : description,
                category.isEmpty() ? existing.getCategory() : category,
                priority);
        System.out.println("\n[ Успешно ] Запрос обновлён.");
    }

    private void changeRequestStatus() {
        int id = readInt("Введите ID запроса: ");
        DocumentRequest req = requestService.findById(id);
        System.out.println("Текущий статус: " + req.getStatus().getDisplayName());

        RequestStatus newStatus = chooseStatus("Выберите новый статус:", req.getStatus());
        requestService.changeStatus(id, newStatus);
        System.out.println("\n[ Успешно ] Статус изменён на: " + newStatus.getDisplayName());
    }

    private void deleteRequest() {
        int id = readInt("Введите ID запроса для удаления: ");
        DocumentRequest req = requestService.findById(id);
        System.out.println("Удалить запрос: " + req.getDocumentTitle() + "?");
        String confirm = readLine("Введите 'да' для подтверждения: ");
        if ("да".equalsIgnoreCase(confirm)) {
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
                        String query = readNonEmptyLine("Название документа для поиска: ", "Строка поиска не может быть пустой.");
                        printRequestTable(requestService.searchByTitle(query));
                    }
                    case 2 -> {
                        String query = readNonEmptyLine("Ключевое слово в описании: ", "Строка поиска не может быть пустой.");
                        printRequestTable(requestService.searchByDescription(query));
                    }
                    case 3 -> {
                        int userId = readInt("ID пользователя: ");
                        printRequestTable(requestService.findByUserId(userId));
                    }
                    case 4 -> {
                        String name = readNonEmptyLine("ФИО (частичное): ", "Строка поиска не может быть пустой.");
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
                        RequestStatus status = chooseStatus("Выберите статус для фильтрации:", null);
                        printRequestTable(requestService.filterByStatus(status));
                    }
                    case 2 -> {
                        Priority priority = choosePriority("Выберите приоритет для фильтрации:", null);
                        printRequestTable(requestService.filterByPriority(priority));
                    }
                    case 3 -> {
                        String category = chooseCategory("Выберите категорию для фильтрации:", null);
                        printRequestTable(requestService.filterByCategory(category));
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
        while (true) {
            System.out.println();
            System.out.println("============================================================");
            System.out.println("                       ЭКСПОРТ ДАННЫХ                       ");
            System.out.println("============================================================");
            System.out.println("  1. Экспорт в Excel (.xlsx)");
            System.out.println("  2. Экспорт в CSV (.csv)");
            System.out.println("  0. Назад");
            System.out.println("============================================================");

            int formatChoice = readInt("Выберите формат: ");
            if (formatChoice == 0) return;
            if (formatChoice != 1 && formatChoice != 2) {
                System.out.println("\n[ Ошибка ] Неверный пункт меню.\n");
                continue;
            }

            if (formatChoice == 1) {
                String defaultFileName = "export_" + LocalDateTime.now().format(FILE_TS_FMT) + ".xlsx";
                System.out.println("\nКуда экспортировать:");
                System.out.println("  1. Создать новый файл (" + defaultFileName + " или ввести своё имя)");
                System.out.println("  2. Записать в существующий файл (указать путь)");
                System.out.println("  0. Отмена");

                int targetChoice = readInt("Выберите действие: ");
                if (targetChoice == 0) continue;

                String filePath;
                if (targetChoice == 1) {
                    String customName = readLine("Имя нового файла [Enter для '" + defaultFileName + "']: ");
                    if (customName.isEmpty()) {
                        filePath = defaultFileName;
                    } else {
                        filePath = customName.endsWith(".xlsx") ? customName : customName + ".xlsx";
                    }
                } else if (targetChoice == 2) {
                    filePath = readNonEmptyLine("Введите путь к файлу: ", "Путь не может быть пустым.");
                    File f = new File(filePath);
                    if (!f.exists()) {
                        System.out.println("[ Инфо ] Файл '" + filePath + "' не найден, он будет создан автоматически.");
                    }
                } else {
                    System.out.println("\n[ Ошибка ] Неверный пункт.\n");
                    continue;
                }

                try {
                    List<User> users = userService.findAll();
                    List<DocumentRequest> requests = requestService.findAll();
                    String savedPath = ExcelExporter.exportToExcel(users, requests, filePath);
                    System.out.println("\n[ Успешно ] Данные экспортированы в файл: " + savedPath);
                } catch (IOException e) {
                    System.out.println("\n[ Ошибка экспорта ] " + e.getMessage());
                }

            } else {
                System.out.println("\nКакую таблицу экспортировать в CSV:");
                System.out.println("  1. Запросы на документы");
                System.out.println("  2. Пользователи");
                System.out.println("  3. Обе таблицы (в два отдельных файла)");
                System.out.println("  0. Отмена");

                int tableChoice = readInt("Выберите таблицу: ");
                if (tableChoice == 0) continue;
                if (tableChoice < 1 || tableChoice > 3) {
                    System.out.println("\n[ Ошибка ] Неверный пункт.\n");
                    continue;
                }

                String timestamp = LocalDateTime.now().format(FILE_TS_FMT);
                try {
                    if (tableChoice == 1) {
                        String defaultName = "export_requests_" + timestamp + ".csv";
                        String targetPath = askCsvFilePath(defaultName);
                        if (targetPath == null) continue;
                        List<DocumentRequest> requests = requestService.findAll();
                        String saved = ExcelExporter.exportRequestsToCsv(requests, targetPath);
                        System.out.println("\n[ Успешно ] Запросы экспортированы в: " + saved);
                    } else if (tableChoice == 2) {
                        String defaultName = "export_users_" + timestamp + ".csv";
                        String targetPath = askCsvFilePath(defaultName);
                        if (targetPath == null) continue;
                        List<User> users = userService.findAll();
                        String saved = ExcelExporter.exportUsersToCsv(users, targetPath);
                        System.out.println("\n[ Успешно ] Пользователи экспортированы в: " + saved);
                    } else {
                        String reqName = "export_requests_" + timestamp + ".csv";
                        String userName = "export_users_" + timestamp + ".csv";

                        String reqPath = askCsvFilePath(reqName);
                        if (reqPath == null) continue;
                        String userPath = askCsvFilePath(userName);
                        if (userPath == null) continue;

                        List<DocumentRequest> requests = requestService.findAll();
                        List<User> users = userService.findAll();
                        String savedReq = ExcelExporter.exportRequestsToCsv(requests, reqPath);
                        String savedUsers = ExcelExporter.exportUsersToCsv(users, userPath);

                        System.out.println("\n[ Успешно ] Обе таблицы экспортированы:");
                        System.out.println("  - Запросы: " + savedReq);
                        System.out.println("  - Пользователи: " + savedUsers);
                    }
                } catch (IOException e) {
                    System.out.println("\n[ Ошибка экспорта ] " + e.getMessage());
                }
            }
        }
    }

    private String askCsvFilePath(String defaultFileName) {
        System.out.println("\nКуда экспортировать:");
        System.out.println("  1. Создать новый файл (" + defaultFileName + " или ввести своё имя)");
        System.out.println("  2. Записать в существующий файл (указать путь)");
        System.out.println("  0. Отмена");

        int choice = readInt("Выберите действие: ");
        if (choice == 0) return null;

        if (choice == 1) {
            String customName = readLine("Имя нового файла [Enter для '" + defaultFileName + "']: ");
            if (customName.isEmpty()) {
                return defaultFileName;
            }
            return customName.endsWith(".csv") ? customName : customName + ".csv";
        } else if (choice == 2) {
            String path = readNonEmptyLine("Введите путь к файлу: ", "Путь не может быть пустым.");
            File f = new File(path);
            if (!f.exists()) {
                System.out.println("[ Инфо ] Файл '" + path + "' не найден, он будет создан автоматически.");
            }
            return path;
        } else {
            System.out.println("\n[ Ошибка ] Неверный пункт.\n");
            return null;
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

    private RequestStatus chooseStatus(String prompt, RequestStatus defaultStatus) {
        System.out.println("\n" + prompt);
        RequestStatus[] statuses = RequestStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.printf("  %d. %s (%s)%n", i + 1, statuses[i].name(), statuses[i].getDisplayName());
        }
        while (true) {
            String defHint = defaultStatus != null ? " [Enter для " + defaultStatus.getDisplayName() + "]: " : ": ";
            System.out.print("Выберите номер (1-" + statuses.length + ") или введите статус" + defHint);
            String input = readLine("");
            if (input.isEmpty() && defaultStatus != null) {
                return defaultStatus;
            }
            try {
                return RequestStatus.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("[ Ошибка ] " + e.getMessage() + ". Повторите ввод.");
            }
        }
    }

    private Priority choosePriority(String prompt, Priority defaultPriority) {
        System.out.println("\n" + prompt);
        Priority[] priorities = Priority.values();
        for (int i = 0; i < priorities.length; i++) {
            System.out.printf("  %d. %s (%s)%n", i + 1, priorities[i].name(), priorities[i].getDisplayName());
        }
        while (true) {
            String defHint = defaultPriority != null ? " [Enter для " + defaultPriority.getDisplayName() + "]: " : ": ";
            System.out.print("Выберите номер (1-" + priorities.length + ") или введите приоритет" + defHint);
            String input = readLine("");
            if (input.isEmpty() && defaultPriority != null) {
                return defaultPriority;
            }
            try {
                return Priority.fromString(input);
            } catch (IllegalArgumentException e) {
                System.out.println("[ Ошибка ] " + e.getMessage() + ". Повторите ввод.");
            }
        }
    }

    private String chooseCategory(String prompt, String defaultCategory) {
        List<String> categories = requestService.findAllCategories();
        System.out.println("\n" + prompt);
        if (!categories.isEmpty()) {
            System.out.println("Существующие категории:");
            for (int i = 0; i < categories.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, categories.get(i));
            }
            System.out.println("  0. Ввести другую категорию вручную");
        }
        while (true) {
            String defHint = defaultCategory != null && !defaultCategory.isEmpty()
                    ? " [Enter для '" + defaultCategory + "']: "
                    : ": ";

            if (!categories.isEmpty()) {
                System.out.print("Выберите номер (1-" + categories.size() + ") или 0 для ввода" + defHint);
            } else {
                System.out.print("Введите категорию" + defHint);
            }

            String input = readLine("");
            if (input.isEmpty() && defaultCategory != null && !defaultCategory.isEmpty()) {
                return defaultCategory;
            }

            if (!categories.isEmpty()) {
                try {
                    int num = Integer.parseInt(input);
                    if (num >= 1 && num <= categories.size()) {
                        return categories.get(num - 1);
                    }
                    if (num == 0) {
                        return readNonEmptyLine("Введите название новой категории: ", "Категория не может быть пустой.");
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[ Ошибка ] Категория не может быть пустой.");
        }
    }

    private String readLine(String prompt) {
        if (!prompt.isEmpty()) {
            System.out.print(prompt);
        }
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    private String readNonEmptyLine(String prompt, String errorMsg) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("[ Ошибка ] " + errorMsg);
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[ Ошибка ] Значение должно быть целым числом.\n");
            }
        }
    }

    private int readIntOrDefault(String prompt, int defaultValue) {
        System.out.print(prompt);
        String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        if (input.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("[ Внимание ] Некорректное число. Используется значение: " + defaultValue);
            return defaultValue;
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
            if (input.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(input, DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("[ Ошибка ] Некорректный формат даты. Используйте дд.мм.гггг");
            }
        }
    }
}
