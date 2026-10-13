module ru.mirea.archive {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.poi.ooxml;
    requires org.apache.logging.log4j;

    opens ru.mirea.archive to javafx.fxml;
    opens ru.mirea.archive.controller to javafx.fxml;
    opens ru.mirea.archive.model to javafx.base;

    exports ru.mirea.archive;
    exports ru.mirea.archive.model;
    exports ru.mirea.archive.service;
    exports ru.mirea.archive.repository;
    exports ru.mirea.archive.exception;
    exports ru.mirea.archive.util;
    exports ru.mirea.archive.controller;
}
