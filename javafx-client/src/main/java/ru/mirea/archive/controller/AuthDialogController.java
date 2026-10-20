package ru.mirea.archive.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ru.mirea.archive.service.AuthClientService;

public class AuthDialogController {

    @FXML
    private TabPane authTabPane;

    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private TextField regUsernameField;

    @FXML
    private TextField regFullNameField;

    @FXML
    private TextField regEmailField;

    @FXML
    private PasswordField regPasswordField;

    @FXML
    private PasswordField regConfirmPasswordField;

    @FXML
    private Label statusLabel;

    private AuthClientService authClientService;
    private Stage dialogStage;
    private boolean authenticated = false;

    public void setAuthClientService(AuthClientService authClientService) {
        this.authClientService = authClientService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    @FXML
    private void onLogin() {
        statusLabel.setStyle("-fx-text-fill: #f38ba8;");
        String username = loginUsernameField.getText();
        String password = loginPasswordField.getText();

        if (username == null || username.isBlank()) {
            statusLabel.setText("Введите имя пользователя");
            return;
        }
        if (password == null || password.isBlank()) {
            statusLabel.setText("Введите пароль");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #89b4fa;");
        statusLabel.setText("Авторизация...");

        AuthClientService.AuthResult res = authClientService.login(username, password);
        if (res.isSuccess()) {
            authenticated = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        } else {
            statusLabel.setStyle("-fx-text-fill: #f38ba8;");
            statusLabel.setText(res.getMessage());
        }
    }

    @FXML
    private void onRegister() {
        statusLabel.setStyle("-fx-text-fill: #f38ba8;");
        String username = regUsernameField.getText();
        String fullName = regFullNameField.getText();
        String email = regEmailField.getText();
        String password = regPasswordField.getText();
        String confirm = regConfirmPasswordField.getText();

        if (username == null || username.trim().length() < 3) {
            statusLabel.setText("Логин должен содержать минимум 3 символа");
            return;
        }
        if (password == null || password.length() < 4) {
            statusLabel.setText("Пароль должен содержать минимум 4 символа");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("Пароли не совпадают");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #89b4fa;");
        statusLabel.setText("Регистрация...");

        AuthClientService.AuthResult res = authClientService.register(username, password, fullName, email);
        if (res.isSuccess()) {
            authenticated = true;
            if (dialogStage != null) {
                dialogStage.close();
            }
        } else {
            statusLabel.setStyle("-fx-text-fill: #f38ba8;");
            statusLabel.setText(res.getMessage());
        }
    }
}
