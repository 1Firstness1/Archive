package ru.mirea.archive.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.archive.model.User;

public class UserDialogController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField departmentField;

    @FXML private Label nameErrorLabel;
    @FXML private Label emailErrorLabel;

    public void initEdit(User user) {
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
        departmentField.setText(user.getDepartment() != null ? user.getDepartment() : "");
    }

    public String getFullName() {
        return fullNameField.getText();
    }

    public String getEmail() {
        return emailField.getText();
    }

    public String getPhone() {
        String phone = phoneField.getText();
        return (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
    }

    public String getDepartment() {
        String dept = departmentField.getText();
        return (dept != null && !dept.trim().isEmpty()) ? dept.trim() : null;
    }
}
