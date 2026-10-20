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
import ru.mirea.archive.model.AuthSession;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.RequestStatus;
import ru.mirea.archive.model.User;
import ru.mirea.archive.repository.DocumentRequestRepository;
import ru.mirea.archive.repository.UserRepository;
import ru.mirea.archive.service.AuthClientService;
import ru.mirea.archive.service.DocumentRequestService;
import ru.mirea.archive.service.UserService;
import ru.mirea.archive.util.DatabaseManager;
import ru.mirea.archive.util.ExcelExporter;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import ru.mirea.archive.model.AuditLogRecord;
import ru.mirea.archive.model.Worker;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private VBox authContainer;
    @FXML private TabPane authTabPane;
    @FXML private TextField inlineLoginUsernameField;
    @FXML private PasswordField inlineLoginPasswordField;
    @FXML private Button btnInlineLogin;
    @FXML private TextField inlineRegUsernameField;
    @FXML private TextField inlineRegFullNameField;
    @FXML private TextField inlineRegEmailField;
    @FXML private PasswordField inlineRegPasswordField;
    @FXML private PasswordField inlineRegConfirmPasswordField;
    @FXML private Button btnInlineRegister;
    @FXML private Label inlineAuthStatusLabel;

    @FXML private TabPane tabPane;

    @FXML private Label serverStatusBadge;
    @FXML private Label userStatusLabel;
    @FXML private Button btnChangePassword;
    @FXML private Button btnLogout;
    @FXML private Button btnStatistics;
    @FXML private Button btnExportXlsx;
    @FXML private Button btnExportCsv;
    @FXML private HBox offlineBanner;
    @FXML private Label offlineBannerLabel;

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

    @FXML private TableView<Worker> workerTable;
    @FXML private TableColumn<Worker, String> workerIdCol;
    @FXML private TableColumn<Worker, String> workerLoginCol;
    @FXML private TableColumn<Worker, String> workerNameCol;
    @FXML private TableColumn<Worker, String> workerEmailCol;
    @FXML private TableColumn<Worker, String> workerRoleCol;
    @FXML private TableColumn<Worker, String> workerCreatedCol;
    @FXML private TextField searchWorkerField;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userIdCol;
    @FXML private TableColumn<User, String> userNameCol;
    @FXML private TableColumn<User, String> userEmailCol;
    @FXML private TableColumn<User, String> userPhoneCol;
    @FXML private TableColumn<User, String> userDeptCol;
    @FXML private TableColumn<User, String> userCreatedCol;

    @FXML private TextField searchUserField;

    @FXML private TableView<AuditLogRecord> auditTable;
    @FXML private TableColumn<AuditLogRecord, String> auditIdCol;
    @FXML private TableColumn<AuditLogRecord, String> auditTimeCol;
    @FXML private TableColumn<AuditLogRecord, String> auditUserCol;
    @FXML private TableColumn<AuditLogRecord, String> auditEventCol;
    @FXML private TableColumn<AuditLogRecord, String> auditEntityCol;
    @FXML private TableColumn<AuditLogRecord, String> auditDetailsCol;
    @FXML private TextField searchAuditField;

    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    private UserService userService;
    private DocumentRequestService requestService;
    private final AuthClientService authClientService = new AuthClientService();
    private AuthClientService.SystemHealth currentHealth;

    private ObservableList<DocumentRequest> requestData = FXCollections.observableArrayList();
    private ObservableList<Worker> workerData = FXCollections.observableArrayList();
    private List<Worker> fullWorkerList = new java.util.ArrayList<>();
    private ObservableList<User> userData = FXCollections.observableArrayList();
    private ObservableList<AuditLogRecord> auditData = FXCollections.observableArrayList();
    private List<AuditLogRecord> fullAuditList = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        setupRequestTable();
        setupWorkerTable();
        setupUserTable();
        setupAuditTable();
        setupCombos();

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().contains("Запросы")) {
                    updateRequestCount();
                } else if (newTab.getText().contains("Работники")) {
                    updateWorkerCount();
                } else if (newTab.getText().contains("Заявители") || newTab.getText().contains("Пользователи")) {
                    updateUserCount();
                } else if (newTab.getText().contains("Аудит")) {
                    updateAuditCount();
                }
            }
        });

        boolean online = checkSystemHealth();
        setOnlineState(online);

        // Периодическая фоновая проверка состояния сервисов (каждые 4 секунды)
        Timeline heartbeat = new Timeline(new KeyFrame(Duration.seconds(4), event -> {
            boolean wasOnline = currentHealth != null && currentHealth.isFullyOnline();
            boolean isOnline = checkSystemHealth();
            if (wasOnline != isOnline) {
                setOnlineState(isOnline);
            }
        }));
        heartbeat.setCycleCount(Animation.INDEFINITE);
        heartbeat.play();
    }

    private boolean checkSystemHealth() {
        currentHealth = authClientService.checkSystemHealth();
        return currentHealth.isFullyOnline();
    }

    private void initServices() {
        if (userService == null) {
            try {
                UserRepository userRepo = new UserRepository();
                DocumentRequestRepository requestRepo = new DocumentRequestRepository();
                userService = new UserService(userRepo);
                requestService = new DocumentRequestService(requestRepo, userService);
            } catch (Exception e) {
                System.err.println("[MainController] Не удалось инициализировать репозитории: " + e.getMessage());
            }
        }
    }

    private void setOnlineState(boolean online) {
        if (!online) {
            if (serverStatusBadge != null) {
                if (currentHealth != null && currentHealth.isBackendAvailable() && !currentHealth.isDatabaseAvailable()) {
                    serverStatusBadge.setText("Ошибка БД");
                } else if (currentHealth != null && !currentHealth.isBackendAvailable() && currentHealth.isDatabaseAvailable()) {
                    serverStatusBadge.setText("Бэкенд выключен");
                } else {
                    serverStatusBadge.setText("Не в сети");
                }
                serverStatusBadge.getStyleClass().setAll("status-badge-offline");
            }
            if (offlineBanner != null) {
                offlineBanner.setVisible(true);
                offlineBanner.setManaged(true);
            }
            if (offlineBannerLabel != null && currentHealth != null) {
                offlineBannerLabel.setText(currentHealth.getMessage());
            }
            if (userStatusLabel != null) {
                userStatusLabel.setText("Сервер недоступен");
            }
            if (btnInlineLogin != null) btnInlineLogin.setDisable(true);
            if (btnInlineRegister != null) btnInlineRegister.setDisable(true);
            if (tabPane != null) {
                tabPane.setVisible(false);
                tabPane.setManaged(false);
            }
            if (authContainer != null) {
                authContainer.setVisible(true);
                authContainer.setManaged(true);
            }
            if (btnStatistics != null) btnStatistics.setDisable(true);
            if (btnExportXlsx != null) btnExportXlsx.setDisable(true);
            if (btnExportCsv != null) btnExportCsv.setDisable(true);
            if (currentHealth != null) {
                setStatus(currentHealth.getMessage());
            } else {
                setStatus("Система не подключена. Запустите docker-compose в папке backend.");
            }
        } else {
            if (serverStatusBadge != null) {
                serverStatusBadge.setText("В сети");
                serverStatusBadge.getStyleClass().setAll("status-badge-online");
            }
            if (offlineBanner != null) {
                offlineBanner.setVisible(false);
                offlineBanner.setManaged(false);
            }
            if (btnInlineLogin != null) btnInlineLogin.setDisable(false);
            if (btnInlineRegister != null) btnInlineRegister.setDisable(false);

            if (AuthSession.getInstance().isAuthenticated()) {
                updateUserSessionUI();
            } else {
                if (userStatusLabel != null) userStatusLabel.setText("Требуется вход");
                if (tabPane != null) {
                    tabPane.setVisible(false);
                    tabPane.setManaged(false);
                }
                if (authContainer != null) {
                    authContainer.setVisible(true);
                    authContainer.setManaged(true);
                }
                if (btnStatistics != null) btnStatistics.setDisable(true);
                if (btnExportXlsx != null) btnExportXlsx.setDisable(true);
                if (btnExportCsv != null) btnExportCsv.setDisable(true);
                setStatus("Сервер доступен. Войдите в систему.");
            }
        }
    }

    @FXML
    private void onRetryConnection() {
        boolean online = checkSystemHealth();
        setOnlineState(online);
        if (online) {
            showInfo("Подключение успешно",
                    "Сервер бэкенда (порт 8080) и база данных PostgreSQL (порт 5432) успешно обнаружены и готовы к работе!");
        } else {
            String details = (currentHealth != null)
                    ? "Результаты проверки компонентов системы:\n\n"
                    + "• Бэкенд REST API (localhost:8080): " + currentHealth.getBackendDetails() + "\n"
                    + "• База данных (localhost:5432): " + currentHealth.getDatabaseDetails() + "\n\n"
                    + "Диагноз: " + currentHealth.getMessage() + "\n\n"
                    + "Для запуска выполните в терминале:\n"
                    + "cd backend && docker compose up -d"
                    : "Не удалось подключиться к REST API (" + authClientService.getBaseUrl() + ") или PostgreSQL.";
            showError("Сервер недоступен", details);
        }
    }

    @FXML
    private void onInlineLogin() {
        if (inlineAuthStatusLabel != null) {
            inlineAuthStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
        }
        String username = inlineLoginUsernameField.getText();
        String password = inlineLoginPasswordField.getText();

        if (username == null || username.isBlank()) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("Введите имя пользователя");
            return;
        }
        if (password == null || password.isBlank()) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("Введите пароль");
            return;
        }

        if (inlineAuthStatusLabel != null) {
            inlineAuthStatusLabel.setStyle("-fx-text-fill: #89b4fa;");
            inlineAuthStatusLabel.setText("Авторизация...");
        }

        AuthClientService.AuthResult res = authClientService.login(username, password);
        if (res.isSuccess()) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("");
            initServices();
            updateUserSessionUI();
            loadRequests();
            loadWorkers();
            loadUsers();
            loadAuditLogs();
            setStatus("Выполнен вход: " + AuthSession.getInstance().getUsername());
        } else {
            if (inlineAuthStatusLabel != null) {
                inlineAuthStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
                inlineAuthStatusLabel.setText(res.getMessage());
            }
        }
    }

    @FXML
    private void onInlineRegister() {
        if (inlineAuthStatusLabel != null) {
            inlineAuthStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
        }
        String username = inlineRegUsernameField.getText();
        String fullName = inlineRegFullNameField.getText();
        String email = inlineRegEmailField.getText();
        String password = inlineRegPasswordField.getText();
        String confirm = inlineRegConfirmPasswordField.getText();

        if (username == null || username.trim().length() < 3) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("Логин должен содержать минимум 3 символа");
            return;
        }
        if (password == null || password.length() < 4) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("Пароль должен содержать минимум 4 символа");
            return;
        }
        if (!password.equals(confirm)) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("Пароли не совпадают");
            return;
        }

        if (inlineAuthStatusLabel != null) {
            inlineAuthStatusLabel.setStyle("-fx-text-fill: #89b4fa;");
            inlineAuthStatusLabel.setText("Регистрация...");
        }

        AuthClientService.AuthResult res = authClientService.register(username, password, fullName, email);
        if (res.isSuccess()) {
            if (inlineAuthStatusLabel != null) inlineAuthStatusLabel.setText("");
            initServices();
            updateUserSessionUI();
            loadRequests();
            loadWorkers();
            loadUsers();
            loadAuditLogs();
            setStatus("Успешная регистрация и вход: " + AuthSession.getInstance().getUsername());
        } else {
            if (inlineAuthStatusLabel != null) {
                inlineAuthStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
                inlineAuthStatusLabel.setText(res.getMessage());
            }
        }
    }

    private void updateUserSessionUI() {
        AuthSession session = AuthSession.getInstance();
        if (authContainer != null) {
            authContainer.setVisible(false);
            authContainer.setManaged(false);
        }
        if (tabPane != null) {
            tabPane.setVisible(true);
            tabPane.setManaged(true);
            tabPane.setDisable(false);
        }
        if (userStatusLabel != null) {
            userStatusLabel.setText(session.getUsername() + " (" + session.getFullName() + ")");
        }
        if (btnChangePassword != null) {
            btnChangePassword.setVisible(true);
            btnChangePassword.setManaged(true);
        }
        if (btnLogout != null) {
            btnLogout.setVisible(true);
            btnLogout.setManaged(true);
        }
        if (btnStatistics != null) btnStatistics.setDisable(false);
        if (btnExportXlsx != null) btnExportXlsx.setDisable(false);
        if (btnExportCsv != null) btnExportCsv.setDisable(false);
    }

    private void setupWorkerTable() {
        if (workerTable == null) return;
        workerIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        workerLoginCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        workerNameCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFullName() != null ? d.getValue().getFullName() : "-"));
        workerEmailCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEmail() != null ? d.getValue().getEmail() : "-"));
        workerRoleCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRole() != null ? d.getValue().getRole() : "USER"));
        workerCreatedCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(FMT) : "-"));

        workerRoleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(role);
                    badge.getStyleClass().add("badge");
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        badge.setStyle("-fx-background-color: rgba(243, 139, 168, 0.25); -fx-text-fill: #f38ba8; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle("-fx-background-color: rgba(137, 180, 250, 0.2); -fx-text-fill: #89b4fa; -fx-padding: 3 8; -fx-background-radius: 4;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        workerTable.setItems(workerData);
    }

    private void loadWorkers() {
        try {
            fullWorkerList = authClientService.fetchWorkers();
            workerData.setAll(fullWorkerList);
            updateWorkerCount();
        } catch (Exception e) {
            setStatus("Ошибка загрузки работников: " + e.getMessage());
        }
    }

    private void updateWorkerCount() {
        if (countLabel != null) {
            countLabel.setText("Работников: " + workerData.size());
        }
    }

    @FXML
    private void onSearchWorkers() {
        String q = searchWorkerField != null ? searchWorkerField.getText() : null;
        if (q == null || q.isBlank()) {
            workerData.setAll(fullWorkerList);
        } else {
            String lower = q.trim().toLowerCase();
            List<Worker> filtered = fullWorkerList.stream()
                    .filter(w -> (w.getUsername() != null && w.getUsername().toLowerCase().contains(lower))
                            || (w.getFullName() != null && w.getFullName().toLowerCase().contains(lower))
                            || (w.getEmail() != null && w.getEmail().toLowerCase().contains(lower))
                            || (w.getRole() != null && w.getRole().toLowerCase().contains(lower)))
                    .toList();
            workerData.setAll(filtered);
        }
        updateWorkerCount();
    }

    @FXML
    private void onResetWorkerFilter() {
        if (searchWorkerField != null) searchWorkerField.clear();
        workerData.setAll(fullWorkerList);
        updateWorkerCount();
    }

    @FXML
    private void onRefreshWorkers() {
        loadWorkers();
        setStatus("Список работников обновлен");
    }

    private void setupAuditTable() {
        if (auditTable == null) return;
        auditIdCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        auditTimeCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().format(FMT) : "-"));
        auditUserCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        auditEventCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEvent()));
        auditEntityCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEntityType() != null ? d.getValue().getEntityType() : "-"));
        auditDetailsCol.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDetails() != null ? d.getValue().getDetails() : "-"));

        auditTable.setItems(auditData);
    }

    private void loadAuditLogs() {
        try {
            fullAuditList = authClientService.fetchAuditLogs();
            auditData.setAll(fullAuditList);
            updateAuditCount();
        } catch (Exception e) {
            setStatus("Ошибка загрузки журнала аудита: " + e.getMessage());
        }
    }

    private void updateAuditCount() {
        if (countLabel != null) {
            countLabel.setText("Записей в журнале: " + auditData.size());
        }
    }

    @FXML
    private void onSearchAudit() {
        String q = searchAuditField.getText();
        if (q == null || q.isBlank()) {
            auditData.setAll(fullAuditList);
        } else {
            String lower = q.trim().toLowerCase();
            List<AuditLogRecord> filtered = fullAuditList.stream()
                    .filter(a -> (a.getUsername() != null && a.getUsername().toLowerCase().contains(lower))
                            || (a.getEvent() != null && a.getEvent().toLowerCase().contains(lower))
                            || (a.getDetails() != null && a.getDetails().toLowerCase().contains(lower)))
                    .toList();
            auditData.setAll(filtered);
        }
        updateAuditCount();
    }

    @FXML
    private void onResetAuditFilter() {
        searchAuditField.clear();
        auditData.setAll(fullAuditList);
        updateAuditCount();
    }

    @FXML
    private void onRefreshAudit() {
        loadAuditLogs();
        setStatus("Журнал аудита обновлён");
    }

    @FXML
    private void onChangePasswordDialog() {
        AuthSession session = AuthSession.getInstance();
        if (!session.isAuthenticated()) {
            showError("Ошибка", "Вы не авторизованы");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Смена пароля");
        dialog.setHeaderText("Смена пароля для пользователя: " + session.getUsername());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        ButtonType btnSave = new ButtonType("Сменить пароль", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Текущий пароль");
        PasswordField newPass = new PasswordField();
        newPass.setPromptText("Новый пароль");
        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Повторите новый пароль");

        grid.add(new Label("Старый пароль:"), 0, 0);
        grid.add(oldPass, 1, 0);
        grid.add(new Label("Новый пароль:"), 0, 1);
        grid.add(newPass, 1, 1);
        grid.add(new Label("Повтор пароля:"), 0, 2);
        grid.add(confirmPass, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnSave) {
            String oldP = oldPass.getText();
            String newP = newPass.getText();
            String confP = confirmPass.getText();

            if (oldP == null || oldP.isBlank()) {
                showError("Ошибка", "Введите старый пароль");
                return;
            }
            if (newP == null || newP.length() < 4) {
                showError("Ошибка", "Новый пароль должен содержать минимум 4 символа");
                return;
            }
            if (!newP.equals(confP)) {
                showError("Ошибка", "Новые пароли не совпадают");
                return;
            }

            AuthClientService.AuthResult res = authClientService.changePassword(session.getUsername(), oldP, newP);
            if (res.isSuccess()) {
                showInfo("Успешно", "Пароль успешно обновлён в системе!");
            } else {
                showError("Ошибка смены пароля", res.getMessage());
            }
        }
    }

    @FXML
    private void onLogout() {
        AuthSession.getInstance().clear();
        setOnlineState(checkSystemHealth());
        requestData.clear();
        workerData.clear();
        userData.clear();
        auditData.clear();
        setStatus("Вы вышли из системы.");
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
        if (requestService == null) return;
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
        if (userService == null) return;
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
