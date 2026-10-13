package ru.mirea.archive.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.mirea.archive.exception.BusinessException;
import ru.mirea.archive.exception.DatabaseException;
import ru.mirea.archive.exception.EntityNotFoundException;
import ru.mirea.archive.model.DocumentRequest;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.RequestStatus;
import ru.mirea.archive.model.User;
import ru.mirea.archive.repository.DocumentRequestRepository;
import ru.mirea.archive.repository.UserRepository;
import ru.mirea.archive.service.DocumentRequestService;
import ru.mirea.archive.service.UserService;
import ru.mirea.archive.util.ExcelExporter;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private TabPane tabPane;

    @FXML private TableView<DocumentRequest> requestTable;
    @FXML private TableColumn<DocumentRequest, String> reqIdCol;
    @FXML private TableColumn<DocumentRequest, String> reqTitleCol;
    @FXML private TableColumn<DocumentRequest, String> reqCategoryCol;
    @FXML private TableColumn<DocumentRequest, String> reqStatusCol;
    @FXML private TableColumn<DocumentRequest, String> reqPriorityCol;
    @FXML private TableColumn<DocumentRequest, String> reqUserCol;
    @FXML private TableColumn<DocumentRequest, String> reqCreatedCol;
    @FXML private TableColumn<DocumentRequest, String> reqUpdatedCol;

    @FXML private TextField searchRequestField;
    @FXML private ComboBox<String> filterStatusCombo;
    @FXML private ComboBox<String> filterPriorityCombo;
    @FXML private ComboBox<String> sortRequestCombo;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userIdCol;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userEmailCol;
    @FXML private TableColumn<User, String> userPhoneCol;
    @FXML private TableColumn<User, String> userDeptCol;
    @FXML private TableColumn<User, String> userCreatedCol;

    @FXML private TextField searchUserField;

    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    private UserService userService;
    private DocumentRequestService requestService;

    private ObservableList<DocumentRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<User> userData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        UserRepository userRepo = new UserRepository();
        DocumentRequestRepository requestRepo = new DocumentRequestRepository();
        userService = new UserService(userRepo);
        requestService = new DocumentRequestService(requestRepo, userService);

        setupRequestTable();
        setupUserTable();
        setupCombos();

        loadRequests();
        loadUsers();

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().contains("Запросы")) {
                    updateRequestCount();
                } else {
                    updateUserCount();
                }
            }
        });
    }

    private void setupRequestTable() {
        reqIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        reqTitleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDocumentTitle()));
        reqCategoryCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        reqStatusCol.setCellValueFactory(d -> {
            RequestStatus s = d.getValue().getStatus();
            return new SimpleStringProperty(s != null ? s.getDisplayName() : "-");
        });
        reqPriorityCol.setCellValueFactory(d -> {
            Priority p = d.getValue().getPriority();
            return new SimpleStringProperty(p != null ? p.getDisplayName() : "-");
        });
        reqUserCol.setCellValueFactory(d -> {
            String name = d.getValue().getUserName();
            return new SimpleStringProperty(name != null ? name : String.valueOf(d.getValue().getUserId()));
        });
        reqCreatedCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCreatedAt() != null
                        ? d.getValue().getCreatedAt().format(FMT) : "-"));
        reqUpdatedCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUpdatedAt() != null
                        ? d.getValue().getUpdatedAt().format(FMT) : "-"));

        requestTable.setItems(requestData);

        requestTable.setRowFactory(tv -> {
            TableRow<DocumentRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showRequestDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void setupUserTable() {
        userIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        userNameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        userEmailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        userPhoneCol.setCellValueFactory(d -> {
            String phone = d.getValue().getPhone();
            return new SimpleStringProperty(phone != null && !phone.isBlank() ? phone : "-");
        });
        userDeptCol.setCellValueFactory(d -> {
            String dept = d.getValue().getDepartment();
            return new SimpleStringProperty(dept != null && !dept.isBlank() ? dept : "-");
        });
        userCreatedCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCreatedAt() != null
                        ? d.getValue().getCreatedAt().format(FMT) : "-"));

        userTable.setItems(userData);
    }

    private void setupCombos() {
        filterStatusCombo.getItems().add("Все статусы");
        for (RequestStatus s : RequestStatus.values()) {
            filterStatusCombo.getItems().add(s.getDisplayName());
        }
        filterStatusCombo.getSelectionModel().selectFirst();

        filterPriorityCombo.getItems().add("Все приоритеты");
        for (Priority p : Priority.values()) {
            filterPriorityCombo.getItems().add(p.getDisplayName());
        }
        filterPriorityCombo.getSelectionModel().selectFirst();

        sortRequestCombo.getItems().addAll(
                "По умолчанию (ID)",
                "По дате (новые)",
                "По приоритету (высокий)",
                "По названию (А-Я)"
        );
        sortRequestCombo.getSelectionModel().selectFirst();
    }

    private void loadRequests() {
        try {
            List<DocumentRequest> list = requestService.findAll();
            requestData.setAll(list);
            updateRequestCount();
            setStatus("Запросы загружены");
        } catch (DatabaseException e) {
            showError("Ошибка загрузки запросов", e.getMessage());
        }
    }

    private void loadUsers() {
        try {
            List<User> list = userService.findAll();
            userData.setAll(list);
            updateUserCount();
        } catch (DatabaseException e) {
            showError("Ошибка загрузки пользователей", e.getMessage());
        }
    }

    @FXML
    private void onAddRequest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/request_dialog.fxml"));
            DialogPane pane = loader.load();
            RequestDialogController ctrl = loader.getController();
            ctrl.initNew(userService.findAll(), requestService.findAllCategories());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Новый запрос");
            dialog.setDialogPane(pane);
            applyDialogStyle(dialog);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    requestService.create(
                            ctrl.getSelectedUserId(),
                            ctrl.getTitle(),
                            ctrl.getDescription(),
                            ctrl.getCategory(),
                            ctrl.getPriority()
                    );
                    loadRequests();
                    setStatus("Запрос успешно создан");
                } catch (BusinessException e) {
                    showWarning("Ошибка валидации", e.getMessage());
                }
            }
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEditRequest() {
        DocumentRequest selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите запрос", "Для редактирования выберите запрос в таблице.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/request_dialog.fxml"));
            DialogPane pane = loader.load();
            RequestDialogController ctrl = loader.getController();
            ctrl.initEdit(selected, userService.findAll(), requestService.findAllCategories());

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Редактирование запроса #" + selected.getId());
            dialog.setDialogPane(pane);
            applyDialogStyle(dialog);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    requestService.update(
                            selected.getId(),
                            ctrl.getSelectedUserId(),
                            ctrl.getTitle(),
                            ctrl.getDescription(),
                            ctrl.getCategory(),
                            ctrl.getPriority()
                    );
                    loadRequests();
                    setStatus("Запрос #" + selected.getId() + " обновлён");
                } catch (BusinessException e) {
                    showWarning("Ошибка валидации", e.getMessage());
                }
            }
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteRequest() {
        DocumentRequest selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите запрос", "Для удаления выберите запрос в таблице.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText("Удалить запрос #" + selected.getId() + "?");
        confirm.setContentText("Документ: " + selected.getDocumentTitle() + "\nЭто действие нельзя отменить.");
        applyAlertStyle(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                requestService.delete(selected.getId());
                loadRequests();
                setStatus("Запрос #" + selected.getId() + " удалён");
            } catch (EntityNotFoundException e) {
                showWarning("Запрос не найден", e.getMessage());
            }
        }
    }

    @FXML
    private void onChangeStatus() {
        DocumentRequest selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите запрос", "Для изменения статуса выберите запрос в таблице.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Изменить статус");
        dialog.setHeaderText("Запрос #" + selected.getId() + " — " + selected.getDocumentTitle());
        dialog.setContentText("Текущий: " + selected.getStatus().getDisplayName() + "\nНовый статус:");

        for (RequestStatus s : RequestStatus.values()) {
            dialog.getItems().add(s.getDisplayName());
        }
        dialog.setSelectedItem(selected.getStatus().getDisplayName());

        DialogPane dp = dialog.getDialogPane();
        dp.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                RequestStatus newStatus = RequestStatus.fromString(result.get());
                requestService.changeStatus(selected.getId(), newStatus);
                loadRequests();
                setStatus("Статус запроса #" + selected.getId() + " изменён на " + newStatus.getDisplayName());
            } catch (BusinessException e) {
                showWarning("Невозможно изменить статус", e.getMessage());
            }
        }
    }

    @FXML
    private void onSearchRequests() {
        String query = searchRequestField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadRequests();
            return;
        }
        try {
            List<DocumentRequest> found = requestService.searchByTitle(query.trim());
            requestData.setAll(found);
            updateRequestCount();
            setStatus("Найдено запросов: " + found.size());
        } catch (BusinessException e) {
            showWarning("Поиск", e.getMessage());
        }
    }

    @FXML
    private void onFilterRequests() {
        try {
            List<DocumentRequest> filtered = requestService.findAll();

            String statusFilter = filterStatusCombo.getValue();
            if (statusFilter != null && !statusFilter.equals("Все статусы")) {
                RequestStatus status = RequestStatus.fromString(statusFilter);
                filtered = filtered.stream()
                        .filter(r -> r.getStatus() == status)
                        .toList();
            }

            String priorityFilter = filterPriorityCombo.getValue();
            if (priorityFilter != null && !priorityFilter.equals("Все приоритеты")) {
                Priority priority = Priority.fromString(priorityFilter);
                filtered = filtered.stream()
                        .filter(r -> r.getPriority() == priority)
                        .toList();
            }

            String searchText = searchRequestField.getText();
            if (searchText != null && !searchText.trim().isEmpty()) {
                String lowerQuery = searchText.trim().toLowerCase();
                filtered = filtered.stream()
                        .filter(r -> r.getDocumentTitle().toLowerCase().contains(lowerQuery))
                        .toList();
            }

            requestData.setAll(filtered);
            applySorting();
            updateRequestCount();
            setStatus("Отфильтровано запросов: " + filtered.size());
        } catch (Exception e) {
            showError("Ошибка фильтрации", e.getMessage());
        }
    }

    @FXML
    private void onSortRequests() {
        applySorting();
    }

    private void applySorting() {
        String sort = sortRequestCombo.getValue();
        if (sort == null) return;

        List<DocumentRequest> sorted = new java.util.ArrayList<>(requestData);
        switch (sort) {
            case "По дате (новые)" -> sorted = requestService.sortByDateDesc(sorted);
            case "По приоритету (высокий)" -> sorted = requestService.sortByPriorityDesc(sorted);
            case "По названию (А-Я)" -> sorted = requestService.sortByTitle(sorted);
            default -> sorted.sort(java.util.Comparator.comparingInt(DocumentRequest::getId));
        }
        requestData.setAll(sorted);
    }

    @FXML
    private void onResetRequestFilters() {
        searchRequestField.clear();
        filterStatusCombo.getSelectionModel().selectFirst();
        filterPriorityCombo.getSelectionModel().selectFirst();
        sortRequestCombo.getSelectionModel().selectFirst();
        loadRequests();
        setStatus("Фильтры сброшены");
    }

    @FXML
    private void onRefreshRequests() {
        onResetRequestFilters();
    }

    @FXML
    private void onAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_dialog.fxml"));
            DialogPane pane = loader.load();
            UserDialogController ctrl = loader.getController();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Новый пользователь");
            dialog.setDialogPane(pane);
            applyDialogStyle(dialog);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    userService.create(
                            ctrl.getFullName(),
                            ctrl.getEmail(),
                            ctrl.getPhone(),
                            ctrl.getDepartment()
                    );
                    loadUsers();
                    setStatus("Пользователь успешно создан");
                } catch (BusinessException e) {
                    showWarning("Ошибка валидации", e.getMessage());
                }
            }
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите пользователя", "Для редактирования выберите пользователя в таблице.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_dialog.fxml"));
            DialogPane pane = loader.load();
            UserDialogController ctrl = loader.getController();
            ctrl.initEdit(selected);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Редактирование пользователя #" + selected.getId());
            dialog.setDialogPane(pane);
            applyDialogStyle(dialog);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    userService.update(
                            selected.getId(),
                            ctrl.getFullName(),
                            ctrl.getEmail(),
                            ctrl.getPhone(),
                            ctrl.getDepartment()
                    );
                    loadUsers();
                    setStatus("Пользователь #" + selected.getId() + " обновлён");
                } catch (BusinessException e) {
                    showWarning("Ошибка валидации", e.getMessage());
                }
            }
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть форму: " + e.getMessage());
        }
    }

    @FXML
    private void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Выберите пользователя", "Для удаления выберите пользователя в таблице.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText("Удалить пользователя #" + selected.getId() + "?");
        confirm.setContentText("ФИО: " + selected.getFullName() + "\nЭто действие нельзя отменить.\n"
                + "Все связанные запросы также будут удалены!");
        applyAlertStyle(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                userService.delete(selected.getId());
                loadUsers();
                loadRequests();
                setStatus("Пользователь #" + selected.getId() + " удалён");
            } catch (EntityNotFoundException e) {
                showWarning("Пользователь не найден", e.getMessage());
            } catch (DatabaseException e) {
                showError("Ошибка удаления", "Возможно, у пользователя есть связанные запросы.\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void onSearchUsers() {
        String query = searchUserField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadUsers();
            return;
        }
        try {
            List<User> found = userService.searchByName(query.trim());
            userData.setAll(found);
            updateUserCount();
            setStatus("Найдено пользователей: " + found.size());
        } catch (BusinessException e) {
            showWarning("Поиск", e.getMessage());
        }
    }

    @FXML
    private void onResetUserFilters() {
        searchUserField.clear();
        loadUsers();
        setStatus("Фильтры сброшены");
    }

    @FXML
    private void onRefreshUsers() {
        onResetUserFilters();
    }

    @FXML
    private void onShowStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics_view.fxml"));
            DialogPane pane = loader.load();
            StatisticsController ctrl = loader.getController();
            ctrl.loadStatistics(requestService, userService);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Статистика");
            dialog.setDialogPane(pane);
            applyDialogStyle(dialog);
            dialog.showAndWait();
        } catch (IOException e) {
            showError("Ошибка", "Не удалось открыть статистику: " + e.getMessage());
        }
    }

    @FXML
    private void onExportXlsx() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Экспорт в XLSX");
        fc.setInitialFileName("archive_export.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel файлы (*.xlsx)", "*.xlsx"));

        Stage stage = (Stage) requestTable.getScene().getWindow();
        File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                ExcelExporter.exportToExcel(userService.findAll(), requestService.findAll(), file.getAbsolutePath());
                setStatus("Экспорт в XLSX завершён: " + file.getName());
                showInfo("Экспорт успешен", "Данные сохранены в:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showError("Ошибка экспорта", e.getMessage());
            }
        }
    }

    @FXML
    private void onExportCsv() {
        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("Обе таблицы (по очереди 2 файла)",
                "Обе таблицы (по очереди 2 файла)", "Запросы на документы", "Пользователи");
        choiceDialog.setTitle("Экспорт в CSV");
        choiceDialog.setHeaderText("Выберите вариант экспорта в CSV");
        choiceDialog.setContentText("Экспорт:");
        choiceDialog.getDialogPane().getStylesheets()
                .add(getClass().getResource("/css/styles.css").toExternalForm());

        Optional<String> tableChoice = choiceDialog.showAndWait();
        if (tableChoice.isEmpty()) return;

        Stage stage = (Stage) requestTable.getScene().getWindow();
        String choice = tableChoice.get();

        if ("Обе таблицы (по очереди 2 файла)".equals(choice)) {
            FileChooser fcReq = new FileChooser();
            fcReq.setTitle("Экспорт запросов в CSV (файл 1 из 2)");
            fcReq.setInitialFileName("export_requests.csv");
            fcReq.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));
            File fileReq = fcReq.showSaveDialog(stage);
            if (fileReq == null) return;

            FileChooser fcUsers = new FileChooser();
            fcUsers.setTitle("Экспорт пользователей в CSV (файл 2 из 2)");
            fcUsers.setInitialFileName("export_users.csv");
            fcUsers.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));
            File fileUsers = fcUsers.showSaveDialog(stage);
            if (fileUsers == null) return;

            try {
                ExcelExporter.exportRequestsToCsv(requestService.findAll(), fileReq.getAbsolutePath());
                ExcelExporter.exportUsersToCsv(userService.findAll(), fileUsers.getAbsolutePath());
                setStatus("Обе таблицы экспортированы в CSV");
                showInfo("Экспорт успешен", "Обе таблицы успешно сохранены:\n• Запросы: "
                        + fileReq.getAbsolutePath() + "\n• Пользователи: " + fileUsers.getAbsolutePath());
            } catch (IOException e) {
                showError("Ошибка экспорта", e.getMessage());
            }
        } else if ("Пользователи".equals(choice)) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Экспорт пользователей в CSV");
            fc.setInitialFileName("export_users.csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));
            File file = fc.showSaveDialog(stage);
            if (file != null) {
                try {
                    ExcelExporter.exportUsersToCsv(userService.findAll(), file.getAbsolutePath());
                    setStatus("Экспорт в CSV завершён: " + file.getName());
                    showInfo("Экспорт успешен", "Данные пользователей сохранены в:\n" + file.getAbsolutePath());
                } catch (IOException e) {
                    showError("Ошибка экспорта", e.getMessage());
                }
            }
        } else {
            FileChooser fc = new FileChooser();
            fc.setTitle("Экспорт запросов в CSV");
            fc.setInitialFileName("export_requests.csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV файлы (*.csv)", "*.csv"));
            File file = fc.showSaveDialog(stage);
            if (file != null) {
                try {
                    ExcelExporter.exportRequestsToCsv(requestService.findAll(), file.getAbsolutePath());
                    setStatus("Экспорт в CSV завершён: " + file.getName());
                    showInfo("Экспорт успешен", "Данные запросов сохранены в:\n" + file.getAbsolutePath());
                } catch (IOException e) {
                    showError("Ошибка экспорта", e.getMessage());
                }
            }
        }
    }

    private void showRequestDetails(DocumentRequest req) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Детали запроса #" + req.getId());
        alert.setHeaderText(req.getDocumentTitle());

        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(req.getId()).append("\n");
        sb.append("Документ: ").append(req.getDocumentTitle()).append("\n");
        sb.append("Описание: ").append(req.getDescription() != null ? req.getDescription() : "-").append("\n");
        sb.append("Категория: ").append(req.getCategory()).append("\n");
        sb.append("Статус: ").append(req.getStatus() != null ? req.getStatus().getDisplayName() : "-").append("\n");
        sb.append("Приоритет: ").append(req.getPriority() != null ? req.getPriority().getDisplayName() : "-").append("\n");
        sb.append("Пользователь: ").append(req.getUserName() != null ? req.getUserName() : req.getUserId()).append("\n");
        sb.append("Создан: ").append(req.getCreatedAt() != null ? req.getCreatedAt().format(FMT) : "-").append("\n");
        sb.append("Обновлён: ").append(req.getUpdatedAt() != null ? req.getUpdatedAt().format(FMT) : "-");

        alert.setContentText(sb.toString());
        applyAlertStyle(alert);
        alert.showAndWait();
    }

    private void updateRequestCount() {
        countLabel.setText("Запросов: " + requestData.size());
    }

    private void updateUserCount() {
        countLabel.setText("Пользователей: " + userData.size());
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        applyAlertStyle(alert);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        applyAlertStyle(alert);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        applyAlertStyle(alert);
        alert.showAndWait();
    }

    private void applyDialogStyle(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets()
                .add(getClass().getResource("/css/styles.css").toExternalForm());
    }

    private void applyAlertStyle(Alert alert) {
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/css/styles.css").toExternalForm());
    }
}
