package ru.mirea.archive.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ru.mirea.archive.model.Priority;
import ru.mirea.archive.model.RequestStatus;
import ru.mirea.archive.service.DocumentRequestService;
import ru.mirea.archive.service.UserService;

import java.util.List;

public class StatisticsController {

    @FXML private Label totalRequestsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalCategoriesLabel;

    @FXML private HBox statusStatsBox;
    @FXML private HBox priorityStatsBox;
    @FXML private VBox categoryStatsBox;

    public void loadStatistics(DocumentRequestService requestService, UserService userService) {
        int totalRequests = requestService.count();
        int totalUsers = userService.count();
        int totalCategories = requestService.countCategories();

        totalRequestsLabel.setText(String.valueOf(totalRequests));
        totalUsersLabel.setText(String.valueOf(totalUsers));
        totalCategoriesLabel.setText(String.valueOf(totalCategories));

        statusStatsBox.getChildren().clear();
        for (RequestStatus status : RequestStatus.values()) {
            int count = requestService.countByStatus(status);
            VBox card = createStatCard(String.valueOf(count), status.getDisplayName());
            statusStatsBox.getChildren().add(card);
        }

        priorityStatsBox.getChildren().clear();
        for (Priority priority : Priority.values()) {
            int count = requestService.countByPriority(priority);
            VBox card = createStatCard(String.valueOf(count), priority.getDisplayName());
            priorityStatsBox.getChildren().add(card);
        }

        categoryStatsBox.getChildren().clear();
        List<String> categories = requestService.findAllCategories();
        if (categories.isEmpty()) {
            categoryStatsBox.getChildren().add(new Label("Нет категорий"));
        } else {
            for (int i = 0; i < categories.size(); i++) {
                Label label = new Label((i + 1) + ". " + categories.get(i));
                label.setStyle("-fx-font-size: 14px; -fx-text-fill: #cdd6f4;");
                categoryStatsBox.getChildren().add(label);
            }
        }
    }

    private VBox createStatCard(String value, String label) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-font-size: 24px;");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(valueLabel, nameLabel);
        return card;
    }
}
