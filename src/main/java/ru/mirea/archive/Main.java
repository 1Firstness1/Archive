package ru.mirea.archive;

import ru.mirea.archive.exception.DatabaseException;
import ru.mirea.archive.repository.DocumentRequestRepository;
import ru.mirea.archive.repository.UserRepository;
import ru.mirea.archive.service.DocumentRequestService;
import ru.mirea.archive.service.UserService;
import ru.mirea.archive.ui.ConsoleUI;
import ru.mirea.archive.util.DatabaseManager;

public class Main {

    public static void main(String[] args) {
        System.out.println("Подключение к базе данных...");

        try {
            if (!DatabaseManager.testConnection()) {
                System.out.println("Не удалось подключиться к базе данных.");
                System.out.println("Убедитесь, что PostgreSQL запущен и база 'archive_db' создана.");
                System.out.println("Параметры подключения: localhost:5432/archive_db (artem/1234)");
                return;
            }
            System.out.println("Подключение установлено.\n");
        } catch (DatabaseException e) {
            System.out.println("Ошибка подключения: " + e.getMessage());
            return;
        }

        UserRepository userRepository = new UserRepository();
        DocumentRequestRepository requestRepository = new DocumentRequestRepository();

        UserService userService = new UserService(userRepository);
        DocumentRequestService requestService = new DocumentRequestService(requestRepository, userService);

        ConsoleUI ui = new ConsoleUI(userService, requestService);
        ui.start();
    }
}
