package ru.mirea.archive.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ru.mirea.archive.model.DocumentRequest;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.User;

import java.util.List;

public class RequestDialogController {

    @FXML private ComboBox<User> userCombo;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<Priority> priorityCombo;

    @FXML private Label userErrorLabel;
    @FXML private Label titleErrorLabel;
    @FXML private Label categoryErrorLabel;
    @FXML private Label priorityErrorLabel;

    @FXML
    public void initialize() {
        userCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getId() + " — " + user.getFullName() : "";
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });

        priorityCombo.getItems().addAll(Priority.values());
        priorityCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Priority p) {
                return p != null ? p.getDisplayName() : "";
            }

            @Override
            public Priority fromString(String s) {
                return null;
            }
        });
        priorityCombo.getSelectionModel().select(Priority.MEDIUM);
    }

    public void initNew(List<User> users, List<String> categories) {
        userCombo.getItems().addAll(users);
        categoryCombo.getItems().addAll(categories);
    }

    public void initEdit(DocumentRequest req, List<User> users, List<String> categories) {
        userCombo.getItems().addAll(users);
        categoryCombo.getItems().addAll(categories);

        for (User u : users) {
            if (u.getId() == req.getUserId()) {
                userCombo.getSelectionModel().select(u);
                break;
            }
        }

        titleField.setText(req.getDocumentTitle());
        descriptionArea.setText(req.getDescription() != null ? req.getDescription() : "");

        if (req.getCategory() != null) {
            categoryCombo.getEditor().setText(req.getCategory());
            categoryCombo.setValue(req.getCategory());
        }

        if (req.getPriority() != null) {
            priorityCombo.getSelectionModel().select(req.getPriority());
        }
    }

    public int getSelectedUserId() {
        User selected = userCombo.getSelectionModel().getSelectedItem();
        return selected != null ? selected.getId() : -1;
    }

    public String getTitle() {
        return titleField.getText();
    }

    public String getDescription() {
        String desc = descriptionArea.getText();
        return (desc != null && !desc.trim().isEmpty()) ? desc.trim() : null;
    }

    public String getCategory() {
        String cat = categoryCombo.getEditor().getText();
        if (cat == null || cat.trim().isEmpty()) {
            cat = categoryCombo.getValue();
        }
        return cat;
    }

    public Priority getPriority() {
        return priorityCombo.getSelectionModel().getSelectedItem();
    }
}
